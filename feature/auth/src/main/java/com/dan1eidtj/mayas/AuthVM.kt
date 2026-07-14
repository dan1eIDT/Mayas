package com.dan1eidtj.mayas.feature.auth

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dan1eidtj.data.SessionManager
import com.dan1eidtj.data.UserSession
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.dan1eidtj.mayas.core_ui.Screen
import kotlinx.coroutines.launch
import com.dan1eidtj.mayas.db.ChatRepository
import kotlinx.coroutines.tasks.await

class AuthVM(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager = SessionManager(application)

    var activeSessions by mutableStateOf<List<UserSession>>(emptyList())
        private set

    private var userDataListener: ListenerRegistration? = null

    var emailInput by mutableStateOf("")
    fun onEmailChange(newValue: String) { emailInput = newValue }

    var passInput by mutableStateOf("")
    fun onPassChange(newValue: String) { passInput = newValue }

    var nameInput by mutableStateOf("")
    fun onNameChange(newValue: String) { nameInput = newValue }

    var usernameInput by mutableStateOf("")
    fun onUsernameChange(newValue: String) { usernameInput = newValue }

    var isLoginMode by mutableStateOf(true)
    fun toggleAuthMode() {
        isLoginMode = !isLoginMode
        authError = null
    }

    var authError by mutableStateOf<String?>(null)

    var user: FirebaseUser? by mutableStateOf(auth.currentUser)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var userData by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var appTheme by mutableStateOf("dark")
        private set

    var fontSize by mutableStateOf(16f)
        private set

    var isPremium by mutableStateOf(false)
        private set

    var premiumUntil by mutableStateOf<com.google.firebase.Timestamp?>(null)
        private set

    var isInvisible by mutableStateOf(false)
        private set

    var ownedItems by mutableStateOf<List<String>>(emptyList())
        private set

    var backStack by mutableStateOf(listOf<Screen>(Screen.Chats))
        private set

    init {
        viewModelScope.launch {
            sessionManager.sessions.collect {
                activeSessions = it
            }
        }
        user?.uid?.let { loadUserData(it) }
    }

    private fun loadUserData(uid: String) {
        userDataListener?.remove()
        userDataListener = db.collection("users").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("AuthVM", "Ошибка прослушивания данных пользователя", error)
                    return@addSnapshotListener
                }
                if (snap?.exists() == true) {
                    val data = snap.data?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
                    userData = data
                    appTheme = data["theme"] ?: "dark"
                    fontSize = data["fontSize"]?.toFloatOrNull() ?: 16f
                    isPremium = snap.getBoolean("isPremium") ?: false
                    premiumUntil = snap.getTimestamp("premiumUntil")
                    isInvisible = snap.getBoolean("isInvisible") ?: false
                    @Suppress("UNCHECKED_CAST")
                    ownedItems = snap.get("ownedItems") as? List<String> ?: emptyList()
                }
            }
    }

    fun updateInvisibleMode(enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        if (!isPremium && enabled) return
        db.collection("users").document(uid).update("isInvisible", enabled)
            .addOnFailureListener { Log.e("AuthVM", "Failed to update invisible mode", it) }
    }

    fun updateLastSeen() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(mapOf(
            "isOnline" to true,
            "lastSeen" to FieldValue.serverTimestamp()
        ))
    }

    fun updateLocalSettings(description: String, theme: String, fontSize: Float = 16f) {
        userData = userData + mapOf(
            "description" to description,
            "theme" to theme,
            "fontSize" to fontSize.toString()
        )
        appTheme = theme
        this.fontSize = fontSize

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(mapOf(
            "description" to description,
            "theme" to theme,
            "fontSize" to fontSize.toString()
        ))
    }

    fun updateUserData(key: String, value: Any) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(key, value)
            .addOnFailureListener { Log.e("AuthVM", "Failed to update $key", it) }
    }
    private fun mapAuthError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Некорректный формат Email"
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Неверный Email или пароль"
                else -> "Неверный Email или пароль"
            }
            is FirebaseAuthInvalidUserException -> when (e.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "Пользователь с таким Email не найден"
                "ERROR_USER_DISABLED" -> "Этот аккаунт заблокирован"
                else -> "Аккаунт не найден"
            }
            is FirebaseAuthUserCollisionException -> "Этот Email уже используется"
            is FirebaseAuthWeakPasswordException -> "Пароль слишком простой, придумайте другой"
            is FirebaseTooManyRequestsException -> "Слишком много попыток. Попробуйте позже"
            is FirebaseNetworkException -> "Нет подключения к интернету"
            else -> "Что-то пошло не так. Попробуйте ещё раз"
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit) {
        isLoading = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                user = result.user
                val uid = user?.uid
                if (uid != null) {
                    loadUserData(uid)
                    viewModelScope.launch {
                        sessionManager.saveSession(UserSession(
                            uid = uid,
                            email = email,
                            name = userData["name"] ?: user?.displayName ?: "User",
                            avatarUrl = userData["avatarUrl"] ?: ""
                        ))
                    }
                }
                isLoading = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading = false
                onError(mapAuthError(e))
            }
    }

    fun register(
        email: String,
        pass: String,
        name: String,
        username: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        isLoading = true
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val u = result.user
                if (u == null) {
                    isLoading = false
                    onError("Пользователь не создан")
                    return@addOnSuccessListener
                }

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                u.updateProfile(profileUpdates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userMap = mapOf(
                            "username" to username.lowercase().trim(),
                            "email" to email,
                            "name" to name,
                            "isOnline" to true,
                            "theme" to "dark",
                            "description" to "",
                            "avatarUrl" to "",
                            "emojiStatus" to " "
                        )

                        db.collection("users").document(u.uid).set(userMap)
                            .addOnSuccessListener {
                                user = auth.currentUser
                                loadUserData(u.uid)
                                viewModelScope.launch {
                                    sessionManager.saveSession(UserSession(
                                        uid = u.uid,
                                        email = email,
                                        name = name,
                                        avatarUrl = ""
                                    ))
                                }
                                isLoading = false
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                onError(e.localizedMessage ?: "Ошибка сохранения профиля")
                            }
                    } else {
                        isLoading = false
                        onError(task.exception?.localizedMessage ?: "Ошибка обновления профиля")
                    }
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                onError(mapAuthError(e))
            }
    }

    fun logout() {
        logoutSilently {
            viewModelScope.launch {
                ChatRepository(getApplication()).clearAll()
            }
            auth.signOut()
            user = null
            userData = emptyMap()
            backStack = listOf(Screen.Chats)
        }
    }

    fun logoutSilently(onComplete: () -> Unit = {}) {
        val uid = auth.currentUser?.uid

        userDataListener?.remove()
        userDataListener = null

        if (uid != null) {
            viewModelScope.launch {
                sessionManager.saveSession(UserSession(
                    uid = uid,
                    email = auth.currentUser?.email ?: "",
                    name = userData["name"] ?: auth.currentUser?.displayName ?: "User",
                    avatarUrl = userData["avatarUrl"] ?: ""
                ))

                db.collection("users").document(uid).update(mapOf(
                    "isOnline" to false,
                    "lastSeen" to FieldValue.serverTimestamp()
                )).addOnCompleteListener {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    fun switchAccount(targetEmail: String, targetPass: String, onSuccess: () -> Unit) {
        isLoading = true
        logoutSilently {
            auth.signOut()
            login(targetEmail, targetPass, onSuccess = {
                isLoading = false
                onSuccess()
            }, onError = {
                isLoading = false
                authError = it
            })
        }
    }

    fun addNewAccount(onNavigateToAuth: () -> Unit) {
        logoutSilently {
            auth.signOut()
            user = null
            userData = emptyMap()
            onNavigateToAuth()
        }
    }

    fun removeSession(uid: String) {
        viewModelScope.launch { sessionManager.removeSession(uid) }
    }

    fun sendPasswordReset(onComplete: (String?) -> Unit) {
        val email = user?.email ?: return
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) onComplete(null)
            else onComplete(task.exception?.let { mapAuthError(it) } ?: "Ошибка")
        }
    }

    fun updateEmail(password: String, newEmail: String, onComplete: (String?) -> Unit) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        db.collection("users").document(user.uid).update("email", newEmail)
                        onComplete(null)
                    } else {
                        onComplete(updateTask.exception?.let { mapAuthError(it) } ?: "Ошибка обновления Email")
                    }
                }
            } else {
                onComplete("Неверный пароль")
            }
        }
    }

    fun updatePassword(oldPass: String, newPass: String, onComplete: (String?) -> Unit) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)

        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(newPass).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        onComplete(null)
                    } else {
                        onComplete(updateTask.exception?.localizedMessage ?: "Ошибка обновления пароля")
                    }
                }
            } else {
                onComplete("Неверный старый пароль")
            }
        }
    }

    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val u = auth.currentUser ?: return
        val uid = u.uid
        u.delete().addOnSuccessListener {
            db.collection("users").document(uid).delete()
            logout()
            onSuccess()
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "Ошибка удаления. Для этой операции может потребоваться недавний вход в аккаунт.")
        }
    }

    fun checkUsername(username: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username.lowercase().trim())
            .limit(1)
            .get()
            .addOnSuccessListener { snap -> onResult(snap.isEmpty) }
            .addOnFailureListener { onResult(true) }
    }

    fun handleAuthAction(onSuccess: () -> Unit) {
        authError = null

        if (emailInput.isEmpty()) { authError = "Введите Email!"; return }
        if (!emailInput.contains("@")) { authError = "Некорректный Email"; return }
        if (passInput.length < 6) { authError = "Пароль мин. 6 символов"; return }

        if (!isLoginMode) {
            if (nameInput.isEmpty()) { authError = "Введите имя"; return }
            if (usernameInput.isEmpty() || usernameInput.contains(" ")) {
                authError = "Юзернейм без пробелов"; return
            }

            checkUsername(usernameInput) { isAvailable ->
                if (isAvailable) {
                    register(emailInput, passInput, nameInput, usernameInput, onSuccess = onSuccess) { errMsg ->
                        authError = errMsg
                    }
                } else {
                    authError = "Юзернейм @$usernameInput уже занят!"
                }
            }
        } else {
            login(emailInput, passInput, onSuccess = onSuccess) { errMsg ->
                authError = errMsg
            }
        }
    }

    fun resolveAndStartChat(
        input: String,
        myUid: String,
        onStart: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        isLoading = true
        val query = input.trim().lowercase()

        viewModelScope.launch {
            try {
                var snap = db.collection("users")
                    .whereEqualTo("email", query)
                    .limit(1).get().await()

                if (snap.isEmpty) {
                    snap = db.collection("users")
                        .whereEqualTo("username", query)
                        .limit(1).get().await()
                }

                if (!snap.isEmpty) {
                    val targetUid = snap.documents[0].id
                    val chatId = Screen.getChatId(myUid, targetUid)
                    onStart(chatId)
                } else {
                    onError("Пользователь не найден")
                }
            } catch (e: Exception) {
                onError("Ошибка поиска: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    fun resolveUserByUsername(username: String, onResult: (Map<String, Any?>?) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username.lowercase().trim())
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc != null) {
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["uid"] = doc.id
                    onResult(data)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("chats").document(chatId)
            .update("typing.$uid", isTyping)
    }

    fun uploadAvatar(id: String, uri: Uri, isGroup: Boolean, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val folder = if (isGroup) "group_avatars" else "avatars"
        val ref = FirebaseStorage.getInstance().reference.child("$folder/$id.jpg")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                val collection = if (isGroup) "chats" else "users"
                val field = if (isGroup) "groupAvatar" else "avatarUrl"

                db.collection(collection).document(id).update(field, url)
                    .addOnSuccessListener { onSuccess(url) }
                    .addOnFailureListener { onError(it.localizedMessage ?: "Ошибка базы данных") }
            }
            .addOnFailureListener {
                Log.e("AuthVM", "Upload failed", it)
                onError(it.localizedMessage ?: "Ошибка загрузки")
            }
    }

    fun buyItem(id: String, price: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.runTransaction { transaction ->
            val userRef = db.collection("users").document(uid)
            val snapshot = transaction.get(userRef)
            val balance = snapshot.getLong("balance") ?: 0L

            if (balance < price) throw Exception("Недостаточно монет!")

            transaction.update(userRef, "balance", balance - price)
            transaction.update(userRef, "ownedItems", FieldValue.arrayUnion(id))
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Ошибка транзакции") }
    }

    fun useItem(id: String, type: com.dan1eidtj.data.ItemType) {
        val uid = auth.currentUser?.uid ?: return
        val field = when (type) {
            com.dan1eidtj.data.ItemType.BUBBLE -> "messageStyle"
            com.dan1eidtj.data.ItemType.EMOJI_STATUS -> "emojiStatus"
            com.dan1eidtj.data.ItemType.WALLPAPER -> "wallpaper"
            com.dan1eidtj.data.ItemType.FONT -> "fontId"
            com.dan1eidtj.data.ItemType.COLOR_SCHEME -> "colorSchemeId"
            com.dan1eidtj.data.ItemType.ANIMATION -> "animationId"
            com.dan1eidtj.data.ItemType.EFFECT -> "effectId"
        }
        db.collection("users").document(uid).update(field, id)
    }

    fun getFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                db.collection("users").document(uid).update("fcmToken", token)
            }
            .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось получить FCM токен", e) }
    }

    fun incrementUnreadCount(chatId: String, partnerUid: String) {
        db.collection("chats").document(chatId).update(
            "unreadCount_$partnerUid", FieldValue.increment(1)
        ).addOnFailureListener { e ->
            Log.e("AuthVM", "Не удалось увеличить счетчик для $partnerUid", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        userDataListener?.remove()
    }
}