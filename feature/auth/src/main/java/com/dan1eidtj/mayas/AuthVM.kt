package com.dan1eidtj.mayas.feature.auth

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dan1eidtj.data.FirestoreListenerCoordinator
import com.dan1eidtj.data.PhoneUtils
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
import com.dan1eidtj.data.buyShopItemViaBackend
import com.dan1eidtj.data.BuyItemResult

class AuthVM(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager = SessionManager(application)

    var activeSessions by mutableStateOf<List<UserSession>>(emptyList())
        private set

    /** Одна запись в списке "Активные сессии" — устройство, где залогинен этот аккаунт. */
    data class RemoteSession(
        val id: String,
        val deviceName: String,
        val platform: String,
        val lastActiveAt: com.google.firebase.Timestamp?,
        val isCurrent: Boolean
    )

    var remoteSessions by mutableStateOf<List<RemoteSession>>(emptyList())
        private set

    private var myDeviceId: String? = null
    private var selfSessionListener: ListenerRegistration? = null
    private var remoteSessionsListener: ListenerRegistration? = null

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

    var resetMessage by mutableStateOf<String?>(null)
        private set

    var isResetLoading by mutableStateOf(false)
        private set

    var user: FirebaseUser? by mutableStateOf(auth.currentUser)
        private set


    var showVerifyScreen by mutableStateOf(false)
        private set

    var isVerifyLoading by mutableStateOf(false)
        private set

    var verifyMessage by mutableStateOf<String?>(null)
        private set

    val isEmailVerified: Boolean
        get() = user?.isEmailVerified == true

    fun openEmailVerification() {
        showVerifyScreen = true
        verifyMessage = null
    }

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
        // Восстанавливаем трекинг сессии и для случая "юзер уже был залогинен
        // при прошлом запуске" (Firebase Auth сам восстанавливает currentUser).
        user?.uid?.let { bindSessionTracking(it) }

        user?.let { u ->
            showVerifyScreen = !u.isEmailVerified
            u.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val verified = u.isEmailVerified
                    showVerifyScreen = !verified
                    db.collection("users").document(u.uid).update("emailVerified", verified)
                        .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось обновить emailVerified", e) }
                }

            }
        }
    }
    private fun bindSessionTracking(uid: String) {
        viewModelScope.launch {
            val deviceId = sessionManager.getOrCreateDeviceId()
            myDeviceId = deviceId
            val ref = db.collection("users").document(uid)
                .collection("sessions").document(deviceId)

            ref.get().addOnSuccessListener { snap ->
                val write = if (snap.exists()) {
                    ref.update(
                        mapOf(
                            "lastActiveAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    ref.set(
                        mapOf(
                            "deviceName" to SessionManager.deviceDisplayName(),
                            "platform" to "android",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "lastActiveAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                write
                    .addOnSuccessListener {
                        // Включаем self-listener только ПОСЛЕ подтверждённой записи —
                        // иначе он поймает состояние "документа ещё нет" до первого
                        // set() и тут же ложно разлогинит нас самих.
                        startSelfSessionListener(uid, deviceId)
                        startRemoteSessionsListener(uid, deviceId)
                    }
                    .addOnFailureListener { e -> Log.e("AuthVM", "Ошибка записи сессии устройства", e) }
            }.addOnFailureListener { e -> Log.e("AuthVM", "Ошибка чтения сессии устройства", e) }
        }
    }

    private fun startSelfSessionListener(uid: String, deviceId: String) {
        selfSessionListener?.remove()
        selfSessionListener = db.collection("users").document(uid)
            .collection("sessions").document(deviceId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("AuthVM", "Ошибка self-session listener", error)
                    return@addSnapshotListener
                }
                if (snap != null && !snap.exists() && auth.currentUser?.uid == uid) {
                    Log.w("AuthVM", "Сессия завершена удалённо, выходим из аккаунта")
                    logout()
                }
            }
    }

    private fun startRemoteSessionsListener(uid: String, myDeviceId: String) {
        remoteSessionsListener?.remove()
        remoteSessionsListener = db.collection("users").document(uid)
            .collection("sessions")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("AuthVM", "Ошибка списка активных сессий", error)
                    return@addSnapshotListener
                }
                remoteSessions = snap?.documents.orEmpty().map { doc ->
                    RemoteSession(
                        id = doc.id,
                        deviceName = doc.getString("deviceName") ?: "Неизвестное устройство",
                        platform = doc.getString("platform") ?: "unknown",
                        lastActiveAt = doc.getTimestamp("lastActiveAt"),
                        isCurrent = doc.id == myDeviceId
                    )
                }.sortedByDescending { it.lastActiveAt?.toDate()?.time ?: 0L }
            }
    }

    private fun unbindSessionTracking() {
        selfSessionListener?.remove()
        selfSessionListener = null
        remoteSessionsListener?.remove()
        remoteSessionsListener = null
        remoteSessions = emptyList()
    }

    /**
     * Завершить сессию из списка "Активные сессии". Если это сессия ТЕКУЩЕГО
     * устройства — это просто обычный выход. Если чужая — удаляем её документ,
     * а то устройство само себя разлогинит через свой selfSessionListener,
     * как только увидит, что документ пропал (при следующем подключении к сети).
     */
    fun endSession(sessionId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (sessionId == myDeviceId) {
            logout()
            return
        }
        db.collection("users").document(uid)
            .collection("sessions").document(sessionId)
            .delete()
            .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось завершить сессию", e) }
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


    fun sendForgotPasswordEmail(email: String) {
        authError = null
        resetMessage = null

        if (email.isEmpty()) { authError = "Введите Email!"; return }
        if (!email.contains("@")) { authError = "Некорректный Email"; return }

        isResetLoading = true
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                isResetLoading = false
                resetMessage = "Письмо для сброса пароля отправлено на $email"
            }
            .addOnFailureListener { e ->
                isResetLoading = false
                authError = mapAuthError(e)
            }
    }

    fun clearResetMessage() { resetMessage = null }


    fun resendVerificationEmail() {
        val u = auth.currentUser ?: return
        isVerifyLoading = true
        verifyMessage = null
        u.sendEmailVerification()
            .addOnSuccessListener {
                isVerifyLoading = false
                verifyMessage = "Письмо отправлено на ${u.email}"
            }
            .addOnFailureListener { e ->
                isVerifyLoading = false
                verifyMessage = mapAuthError(e)
            }
    }


    fun refreshVerificationStatus(onSuccess: () -> Unit) {
        val u = auth.currentUser ?: return
        isVerifyLoading = true
        verifyMessage = null
        u.reload().addOnCompleteListener {
            isVerifyLoading = false
            if (u.isEmailVerified) {
                db.collection("users").document(u.uid).update("emailVerified", true)
                    .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось обновить emailVerified", e) }





                u.getIdToken(true).addOnCompleteListener {
                    showVerifyScreen = false
                    user = u
                    onSuccess()
                }
            } else {
                verifyMessage = "Почта ещё не подтверждена. Проверь письмо (и папку Спам)"
            }
        }
    }


    fun cancelVerification() {
        showVerifyScreen = false
        verifyMessage = null
        logout()
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit) {
        isLoading = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val loggedUser = result.user
                user = loggedUser
                val uid = loggedUser?.uid
                if (uid != null) {
                    loadUserData(uid)
                    bindSessionTracking(uid)
                    viewModelScope.launch {
                        sessionManager.saveSession(UserSession(
                            uid = uid,
                            email = email,
                            name = userData["name"] ?: loggedUser.displayName ?: "User",
                            avatarUrl = userData["avatarUrl"] ?: ""
                        ))
                    }
                }


                loggedUser?.reload()?.addOnCompleteListener {
                    isLoading = false
                    val verified = loggedUser.isEmailVerified
                    if (uid != null) {
                        db.collection("users").document(uid).update("emailVerified", verified)
                            .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось обновить emailVerified", e) }
                    }
                    if (verified) {


                        loggedUser.getIdToken(true).addOnCompleteListener {
                            showVerifyScreen = false
                            onSuccess()
                        }
                    } else {
                        showVerifyScreen = true
                    }
                }
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
                            "emojiStatus" to " ",
                            "emailVerified" to false,
                            // Верификация и роль — явные дефолты для новых юзеров.
                            // Firestore rules всё равно требуют rank=0 и verification=false
                            // на create, это просто делает документ сразу читаемым без
                            // дополнительных .get(..., default) на клиенте.
                            "rank" to 0,
                            "verification" to false
                        )

                        db.collection("users").document(u.uid).set(userMap)
                            .addOnSuccessListener {
                                user = auth.currentUser
                                loadUserData(u.uid)
                                bindSessionTracking(u.uid)
                                viewModelScope.launch {
                                    sessionManager.saveSession(UserSession(
                                        uid = u.uid,
                                        email = email,
                                        name = name,
                                        avatarUrl = ""
                                    ))
                                }


                                u.sendEmailVerification()
                                    .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось отправить письмо верификации", e) }

                                isLoading = false
                                showVerifyScreen = true


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
        // Снимаем self-listener ДО удаления документа сессии — иначе поймаем
        // своё же удаление как "сессию завершили с другого устройства" и
        // рекурсивно ещё раз вызовем logout().
        val uid = auth.currentUser?.uid
        val deviceId = myDeviceId
        unbindSessionTracking()
        if (uid != null && deviceId != null) {
            db.collection("users").document(uid)
                .collection("sessions").document(deviceId)
                .delete()
                .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось удалить сессию при выходе", e) }
        }
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





        FirestoreListenerCoordinator.tearDownAll()

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
        // Как и в logout() — снимаем слушателей сессии старого аккаунта ДО signOut().
        // Без этого selfSessionListener/remoteSessionsListener остаются висеть на
        // users/{oldUid}/sessions, и как только меняется auth-состояние, правило
        // request.auth.uid == userId перестаёт совпадать -> PERMISSION_DENIED.
        unbindSessionTracking()
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
        // Та же причина, что и в switchAccount() — иначе слушатели старого uid
        // переживают signOut() и ловят PERMISSION_DENIED.
        unbindSessionTracking()
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
        val clean = username.lowercase().trim()
        var userDone = false
        var channelDone = false
        var isTaken = false

        fun finish() {
            if (userDone && channelDone) onResult(!isTaken)
        }

        db.collection("users")
            .whereEqualTo("username", clean)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) isTaken = true
                userDone = true
                finish()
            }
            .addOnFailureListener { userDone = true; finish() }

        db.collection("chats")
            .whereEqualTo("username", clean)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) isTaken = true
                channelDone = true
                finish()
            }
            .addOnFailureListener { channelDone = true; finish() }
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


    fun ensureSavedMessagesChat(myUid: String) {
        val chatId = "saved_$myUid"
        val chatRef = db.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                chatRef.set(
                    mapOf(
                        "type" to "SAVED",
                        "isGroup" to true,
                        "participants" to listOf(myUid),
                        "ownerId" to myUid,
                        "groupName" to "Избранное",
                        "groupIcon" to "bookmark",
                        "lastMessage" to "",
                        "lastSenderId" to "",
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "pinned_$myUid" to true,
                        "unreadCount_$myUid" to 0
                    )
                )
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

    /**
     * Ищет пользователя по номеру телефона через phoneIndex/{номер} — это точечный
     * get() по известному ID документа, а не listing/query по полю. Firestore-правила
     * различают get и list: get на phoneIndex разрешён, а list по этой коллекции
     * запрещён навсегда (см. firestore.rules) — так что массово слить все номера
     * через клиент физически нельзя, а точечный поиск "у кого такой-то номер" работает.
     */
    fun resolveUserByPhone(phone: String, onResult: (Map<String, Any?>?) -> Unit) {
        val normalized = PhoneUtils.normalize(phone)
        if (normalized == null) { onResult(null); return }

        db.collection("phoneIndex").document(normalized).get()
            .addOnSuccessListener { indexDoc ->
                val uid = indexDoc.getString("uid")
                if (uid == null) { onResult(null); return@addOnSuccessListener }

                db.collection("users").document(uid).get()
                    .addOnSuccessListener { userDoc ->
                        if (!userDoc.exists()) { onResult(null); return@addOnSuccessListener }
                        val data = userDoc.data?.toMutableMap() ?: mutableMapOf()
                        data["uid"] = userDoc.id
                        onResult(data)
                    }
                    .addOnFailureListener { onResult(null) }
            }
            .addOnFailureListener { onResult(null) }
    }

    /**
     * Читает СВОЙ собственный номер из приватной подколлекции users/{uid}/private/contact.
     * Эта подколлекция читается только владельцем (см. firestore.rules) — даже другой
     * авторизованный юзер получит permission-denied, если попробует её запросить напрямую.
     * Используется, например, чтобы подставить текущий номер в поле редактирования профиля.
     */
    fun getMyPhone(onResult: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: run { onResult(""); return }
        db.collection("users").document(uid).collection("private").document("contact").get()
            .addOnSuccessListener { doc -> onResult(doc.getString("phone") ?: "") }
            .addOnFailureListener { onResult("") }
    }

    /**
     * Сохраняет номер телефона текущего юзера.
     * Номер НЕ пишется в users/{uid} (этот документ читают все signed-in юзеры) —
     * вместо этого:
     *  1) кладётся в приватную users/{uid}/private/contact, которую видит только сам юзер;
     *  2) кладётся в phoneIndex/{номер} = {uid} — это единственный способ найти юзера
     *     по номеру, и он доступен только через точечный get(), не через list().
     * Пустая строка отвязывает номер: удаляются оба документа.
     * Если юзер меняет номер на другой — старая запись в phoneIndex подчищается,
     * чтобы по старому номеру его больше нельзя было найти.
     */
    fun updatePhoneNumber(rawPhone: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return
        val trimmed = rawPhone.trim()
        val privateContactRef = db.collection("users").document(uid).collection("private").document("contact")

        privateContactRef.get().addOnSuccessListener { existingDoc ->
            val oldPhone = existingDoc.getString("phone")

            if (trimmed.isEmpty()) {
                val batch = db.batch()
                batch.delete(privateContactRef)
                if (oldPhone != null) batch.delete(db.collection("phoneIndex").document(oldPhone))
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.localizedMessage ?: "Ошибка сохранения номера") }
                return@addOnSuccessListener
            }

            val normalized = PhoneUtils.normalize(trimmed)
            if (normalized == null) {
                onError("Похоже, номер введён некорректно")
                return@addOnSuccessListener
            }

            if (normalized == oldPhone) {
                // Номер не поменялся, писать нечего
                onSuccess()
                return@addOnSuccessListener
            }

            val newIndexRef = db.collection("phoneIndex").document(normalized)
            newIndexRef.get().addOnSuccessListener { newIndexDoc ->
                val claimedByUid = newIndexDoc.getString("uid")
                if (claimedByUid != null && claimedByUid != uid) {
                    onError("Этот номер уже привязан к другому аккаунту")
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                if (oldPhone != null) batch.delete(db.collection("phoneIndex").document(oldPhone))
                batch.set(newIndexRef, mapOf("uid" to uid))
                batch.set(privateContactRef, mapOf("phone" to normalized))
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.localizedMessage ?: "Ошибка сохранения номера") }
            }.addOnFailureListener { e -> onError(e.localizedMessage ?: "Ошибка проверки номера") }
        }.addOnFailureListener { e -> onError(e.localizedMessage ?: "Ошибка чтения текущего номера") }
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
        if (auth.currentUser?.uid == null) return

        viewModelScope.launch {
            when (val result = buyShopItemViaBackend(id)) {
                is BuyItemResult.Success -> onSuccess()
                BuyItemResult.LowBalance -> onError("Недостаточно монет!")
                BuyItemResult.AlreadyOwned -> onError("Уже куплено")
                BuyItemResult.ItemNotFound -> onError("Товар не найден")
                BuyItemResult.OutOfSeason -> onError("Товар сейчас недоступен")
                is BuyItemResult.Error -> onError(result.message)
            }
        }
    }

    fun useItem(id: String, type: com.dan1eidtj.data.ItemType) {
        val uid = auth.currentUser?.uid ?: return
        val field = when (type) {
            com.dan1eidtj.data.ItemType.BUBBLE -> "messageStyle"
            com.dan1eidtj.data.ItemType.EMOJI_STATUS -> "emojiStatus"
            com.dan1eidtj.data.ItemType.FONT -> "fontId"
            com.dan1eidtj.data.ItemType.COLOR_SCHEME -> "colorSchemeId"
            com.dan1eidtj.data.ItemType.ANIMATION -> "animationId"
            com.dan1eidtj.data.ItemType.EFFECT -> "effectId"
        }



        val value: Any = if (id == "default" || id == "none") FieldValue.delete() else id
        db.collection("users").document(uid).update(field, value)
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