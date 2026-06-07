package com.dan1eidtj.mayas.feature.auth

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthVM : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Слушатель Firestore (сохраняем, чтобы вовремя закрывать и избегать утечек)
    private var userDataListener: ListenerRegistration? = null

    var emailInput by mutableStateOf("")
    fun onEmailChange(newValue: String) {
        emailInput = newValue
    }

    var passInput by mutableStateOf("")
    fun onPassChange(newValue: String) {
        passInput = newValue
    }

    var nameInput by mutableStateOf("")
    fun onNameChange(newValue: String) {
        nameInput = newValue
    }

    var usernameInput by mutableStateOf("")
    fun onUsernameChange(newValue: String) {
        usernameInput = newValue
    }

    var isLoginMode by mutableStateOf(true)
    fun toggleAuthMode() {
        isLoginMode = !isLoginMode
        authError = null
    }

    var authError by mutableStateOf<String?>(null)

    // --- СОСТОЯНИЯ ДЛЯ UI ---
    var user: FirebaseUser? by mutableStateOf(auth.currentUser)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var userData by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var appTheme by mutableStateOf("dark")
        private set

    var backStack by mutableStateOf(listOf<Screen>(Screen.Chats))
        private set

    init {
        // Если пользователь уже авторизован, сразу загружаем данные
        user?.uid?.let { loadUserData(it) }
    }

    // --- СЛУШАТЕЛИ И ПРОФИЛЬ ---
    private fun loadUserData(uid: String) {
        userDataListener?.remove() // Удаляем старый слушатель, если он был

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
                }
            }
    }

    fun updateLastSeen() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(
            mapOf(
                "isOnline" to true,
                "lastSeen" to FieldValue.serverTimestamp()
            )
        )
    }

    fun updateLocalSettings(description: String, theme: String) {
        // Обновляем локально
        userData = userData + mapOf("description" to description, "theme" to theme)
        appTheme = theme

        // Синхронизируем с Firestore, чтобы настройки не пропадали
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(
            mapOf(
                "description" to description,
                "theme" to theme
            )
        )
    }

    // --- АВТОРИЗАЦИЯ (ВХОД / РЕГИСТРАЦИЯ / ВЫХОД) ---
    fun login(email: String, pass: String, onError: (String) -> Unit) {
        isLoading = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                user = result.user
                user?.uid?.let { loadUserData(it) }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                onError(it.localizedMessage ?: "Ошибка входа")
            }
    }

    fun register(
        email: String,
        pass: String,
        name: String,
        username: String,
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

                // Обновляем DisplayName в Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                u.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Создаем документ юзера в Firestore
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
                                    isLoading = false
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
                onError(e.localizedMessage ?: "Ошибка регистрации")
            }
    }

    fun logout() {
        val uid = auth.currentUser?.uid

        // 1. Отписываемся от Firestore обновлений перед выходом!
        userDataListener?.remove()
        userDataListener = null

        if (uid != null) {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
            )
        }

        // 2. Сбрасываем всё состояние
        auth.signOut()
        user = null
        userData = emptyMap()
        backStack = listOf(Screen.Chats)
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

        if (emailInput.isEmpty()) {
            authError = "Введите Email!"; return
        }
        if (!emailInput.contains("@")) {
            authError = "Некорректный Email"; return
        }
        if (passInput.length < 6) {
            authError = "Пароль мин. 6 символов"; return
        }

        if (!isLoginMode) {
            if (nameInput.isEmpty()) {
                authError = "Введите имя"; return
            }
            if (usernameInput.isEmpty() || usernameInput.contains(" ")) {
                authError = "Юзернейм без пробелов"; return
            }

            // Проверяем юзернейм
            checkUsername(usernameInput) { isAvailable ->
                if (isAvailable) {
                    register(emailInput, passInput, nameInput, usernameInput) { errMsg ->
                        authError = errMsg
                    }
                } else {
                    authError = "Юзернейм @$usernameInput уже занят!"
                }
            }
        } else {
            login(emailInput, passInput) { errMsg ->
                authError = errMsg
            }
        }
    }

    // --- РАБОТА С ЧАТАМИ И ПОИСКОМ (На корутинах) ---
    fun resolveAndStartChat(
        input: String,
        myUid: String,
        onStart: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        isLoading = true
        val query = input.trim().lowercase()

        // Запускаем асинхронную корутину, чтобы избавиться от ада колбэков
        viewModelScope.launch {
            try {
                // Ищем по Email
                var snap = db.collection("users")
                    .whereEqualTo("email", query)
                    .limit(1).get().await()

                // Если по Email не нашли, ищем по Username
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

    fun resolveUserByUsername(username: String, onResult: (String?) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username.lowercase().trim())
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val uid = snap.documents.firstOrNull()?.id
                onResult(uid)
            }
            .addOnFailureListener { onResult(null) }
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("chats").document(chatId)
            .update("typing.$uid", isTyping)
    }

    // --- ХРАНИЛИЩЕ (FIREBASE STORAGE) ---
    fun uploadAvatar(uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val ref = FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }
            .addOnFailureListener {
                onError(it.localizedMessage ?: "Ошибка загрузки аватара")
            }
    }

    // --- УВЕДОМЛЕНИЯ (FCM) ---
    fun getFcmToken() {
        val uid = auth.currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                db.collection("users").document(uid).update("fcmToken", token)
            }
            .addOnFailureListener { e ->
                Log.e("AuthVM", "Не удалось получить FCM токен", e)
            }
    }

    // --- НАВИГАЦИЯ ---
    fun navigateTo(screen: Screen) {
        backStack = backStack + screen
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    // --- ОПРЕДЕЛЕНИЕ ЭКРАНОВ ПРИЛОЖЕНИЯ ---
    sealed class Screen(val route: String) {
        object Auth : Screen("auth")
        object Chats : Screen("chats")
        object Credits : Screen("credits")
        object Profile : Screen("profile/{uid}") {
            fun create(uid: String) = "profile/$uid"
        }
        object Chat : Screen("chat/{chatId}") {
            fun create(chatId: String) = "chat/$chatId"
        }

        companion object {
            // Метод генерации ID чата перенесен внутрь companion-объекта Screen
            fun getChatId(uid1: String, uid2: String): String {
                return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
            }
        }
    }
    fun incrementUnreadCount(chatId: String, partnerUid: String) {
        db.collection("chats").document(chatId).update(
            "unreadCount_$partnerUid", FieldValue.increment(1)
        ).addOnFailureListener { e ->
            Log.e("AuthVM", "Не удалось увеличить счетчик для $partnerUid", e)
        }
    }
}

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Chats : Screen("chats")
    object Credits : Screen("credits")
    object Profile : Screen("profile/{uid}") {
        fun create(uid: String) = "profile/$uid"
    }
    object Chat : Screen("chat/{chatId}") {
        fun create(chatId: String) = "chat/$chatId"
    }

    companion object {
        fun getChatId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }
}