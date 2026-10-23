package com.dan1eidtj.mayas.feature.auth

import android.app.Application
import android.net.Uri
import android.util.Log
import com.dan1eidtj.auth.R
import com.google.firebase.FirebaseApp
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
import kotlinx.coroutines.CancellationException
import com.dan1eidtj.mayas.db.OfflineDataStore
import com.dan1eidtj.mayas.storage.MediaCachePrefs
import com.dan1eidtj.mayas.storage.AutoDownloadType
import com.dan1eidtj.mayas.mesh.MeshManager
import kotlinx.coroutines.tasks.await
import com.dan1eidtj.data.buyShopItemViaBackend
import com.dan1eidtj.data.refreshBalanceAndInventory
import com.dan1eidtj.data.BuyItemResult
import com.dan1eidtj.data.BackendApi

class AuthVM(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager = SessionManager(application)
    private val accountTokenStore = AccountTokenStore(application.applicationContext)

    var activeSessions by mutableStateOf<List<UserSession>>(emptyList())
        private set


    data class RemoteSession(
        val id: String,
        val deviceName: String,
        val platform: String,
        val lastActiveAt: com.google.firebase.Timestamp?,
        val isCurrent: Boolean
    )

    var remoteSessions by mutableStateOf<List<RemoteSession>>(emptyList())
        private set

    var isAddingAccount by mutableStateOf(false)
        private set

    val returnAccountName: String
        get() = userData["name"] ?: user?.displayName ?: user?.email.orEmpty()

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

    var appTheme by mutableStateOf("dark")
        private set

    var fontSize by mutableStateOf(16f)
        private set

    var isPremium by mutableStateOf(false)
        private set

    var premiumUntil by mutableStateOf<com.google.firebase.Timestamp?>(null)
        private set

    private var premiumExpiryJob: kotlinx.coroutines.Job? = null

    private fun applyPremium(flag: Boolean, until: com.google.firebase.Timestamp?) {
        premiumExpiryJob?.cancel()
        premiumExpiryJob = null
        premiumUntil = until
        val untilMillis = until?.toDate()?.time
        val now = System.currentTimeMillis()
        val active = flag && (untilMillis == null || untilMillis > now)
        isPremium = active
        if (active && untilMillis != null) {
            premiumExpiryJob = viewModelScope.launch {
                kotlinx.coroutines.delay((untilMillis - now).coerceAtMost(Int.MAX_VALUE.toLong()) + 500L)
                if (premiumUntil?.toDate()?.time == untilMillis) isPremium = false
            }
        }
    }

    private fun resetPremium() {
        premiumExpiryJob?.cancel()
        premiumExpiryJob = null
        isPremium = false
        premiumUntil = null
        ownedItems = emptyList()
        isInvisible = false
    }

    var isInvisible by mutableStateOf(false)
        private set

    var ownedItems by mutableStateOf<List<String>>(emptyList())
        private set

    var purchasingItemIds by mutableStateOf<Set<String>>(emptySet())
        private set

    val isPurchaseInProgress: Boolean
        get() = purchasingItemIds.isNotEmpty()

    var backStack by mutableStateOf(listOf<Screen>(Screen.Chats))
        private set

    init {
        viewModelScope.launch {
            sessionManager.sessions.collect {
                activeSessions = it
            }
        }
        user?.uid?.let { loadUserData(it) }


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

    fun endAllOtherSessions() {
        val uid = auth.currentUser?.uid ?: return
        val myId = myDeviceId ?: return
        db.collection("users").document(uid)
            .collection("sessions")
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    if (doc.id != myId) batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось завершить остальные сессии", e) }
            }
            .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось получить список сессий", e) }
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
                    MeshManager.refreshIdentity()
                    MeshManager.setBlockedUserIds(
                        (snap.get("blocked") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                    )
                    val data = snap.data?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
                    userData = data
                    val legacyAutoDownloadEnabled = data["autoDownloadMedia"] != "false"
                    MediaCachePrefs.setAutoDownloadEnabled(getApplication(), legacyAutoDownloadEnabled)
                    AutoDownloadType.values().forEach { type ->
                        val saved = data[type.userDataKey]
                        MediaCachePrefs.setAutoDownloadEnabled(
                            getApplication(), type, saved?.let { it != "false" } ?: legacyAutoDownloadEnabled
                        )
                    }
                    appTheme = data["theme"] ?: "dark"
                    fontSize = data["fontSize"]?.toFloatOrNull() ?: 16f
                    applyPremium(snap.getBoolean("isPremium") ?: false, snap.getTimestamp("premiumUntil"))
                    isInvisible = snap.getBoolean("isInvisible") ?: false
                    @Suppress("UNCHECKED_CAST")
                    ownedItems = snap.get("ownedItems") as? List<String> ?: emptyList()
                } else {
                    MeshManager.setBlockedUserIds(emptySet())
                    resetPremium()
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
        val stringValue = value.toString()
        val isAutoDownloadSetting = key == "autoDownloadMedia" || AutoDownloadType.values().any { it.userDataKey == key }
        val previousValue = userData[key]

        userData = userData + (key to stringValue)
        if (isAutoDownloadSetting) {
            if (key == "autoDownloadMedia") {
                MediaCachePrefs.setAutoDownloadEnabled(getApplication(), stringValue != "false")
            } else {
                AutoDownloadType.values().firstOrNull { it.userDataKey == key }?.let { type ->
                    MediaCachePrefs.setAutoDownloadEnabled(getApplication(), type, stringValue != "false")
                }
            }
        }
        db.collection("users").document(uid).update(key, value)
            .addOnFailureListener { error ->
                Log.e("AuthVM", "Failed to update $key", error)
                if (userData[key] == stringValue) {
                    userData = if (previousValue == null) userData - key else userData + (key to previousValue)
                    if (isAutoDownloadSetting) {
                        if (key == "autoDownloadMedia") {
                            MediaCachePrefs.setAutoDownloadEnabled(getApplication(), previousValue != "false")
                        } else {
                            AutoDownloadType.values().firstOrNull { it.userDataKey == key }?.let { type ->
                                val restoredEnabled = previousValue?.let { it != "false" }
                                    ?: (userData["autoDownloadMedia"] != "false")
                                MediaCachePrefs.setAutoDownloadEnabled(getApplication(), type, restoredEnabled)
                            }
                        }
                    }
                }
            }
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
        if (isAddingAccount) {
            loginAdditionalAccount(email, pass, onSuccess, onError)
            return
        }
        performLogin(email, pass, onSuccess, onError)
    }

    private fun secondaryAuth(): FirebaseAuth {
        val appName = "mayas_add_account"
        val app = runCatching { FirebaseApp.getInstance(appName) }.getOrNull()
            ?: FirebaseApp.initializeApp(getApplication(), FirebaseApp.getInstance().options, appName)
        return FirebaseAuth.getInstance(app)
    }

    private suspend fun validateOnSecondary(email: String, pass: String, create: Boolean): Exception? {
        val secondary = secondaryAuth()
        return try {
            if (create) {
                secondary.createUserWithEmailAndPassword(email, pass).await()
            } else {
                secondary.signInWithEmailAndPassword(email, pass).await()
            }
            null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        } finally {
            secondary.signOut()
        }
    }

    private fun leaveCurrentAccount(then: () -> Unit) {
        unbindSessionTracking()
        logoutSilently {
            auth.signOut()
            MeshManager.refreshIdentity()
            user = null
            userData = emptyMap()
            resetPremium()
            MeshManager.setBlockedUserIds(emptySet())
            then()
        }
    }

    private fun isCurrentAccount(email: String): Boolean =
        email.trim().equals(auth.currentUser?.email, ignoreCase = true)

    private fun loginAdditionalAccount(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isCurrentAccount(email)) {
            onError(getApplication<Application>().getString(R.string.auth_same_account))
            return
        }
        isLoading = true
        viewModelScope.launch {
            val failure = validateOnSecondary(email, pass, create = false)
            if (failure != null) {
                isLoading = false
                onError(mapAuthError(failure))
                return@launch
            }
            isAddingAccount = false
            leaveCurrentAccount {
                performLogin(email, pass, onSuccess) { message ->
                    isLoading = false
                    onError(message)
                }
            }
        }
    }

    private fun registerAdditionalAccount(
        email: String,
        pass: String,
        name: String,
        username: String,
        onError: (String) -> Unit
    ) {
        if (isCurrentAccount(email)) {
            onError(getApplication<Application>().getString(R.string.auth_same_account))
            return
        }
        isLoading = true
        viewModelScope.launch {
            val failure = validateOnSecondary(email, pass, create = true)
            if (failure != null) {
                isLoading = false
                onError(mapAuthError(failure))
                return@launch
            }
            isAddingAccount = false
            leaveCurrentAccount {
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        completeRegistration(result.user, email, pass, name, username, onError)
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        onError(mapAuthError(e))
                    }
            }
        }
    }

    private fun performLogin(email: String, pass: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit) {
        isLoading = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val loggedUser = result.user
                user = loggedUser
                val uid = loggedUser?.uid
                if (uid != null) {
                    // Keep a Keystore-encrypted local fallback so account switching does not
                    // require the user to retype the password when the optional token API is down.
                    accountTokenStore.putPassword(uid, pass)
                    loadUserData(uid)
                    bindSessionTracking(uid)
                    viewModelScope.launch {
                        sessionManager.saveSession(UserSession(
                            uid = uid,
                            email = email,
                            name = userData["name"] ?: loggedUser.displayName ?: "User",
                            avatarUrl = userData["avatarUrl"] ?: "",
                            username = userData["username"],
                            createdAt = loggedUser.metadata?.creationTimestamp ?: 0L
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
                            // Token issuance is best-effort: normal Firebase login must keep working
                            // even if the optional account-token backend is unavailable.
                            saveAccountTokenForCurrentUser()
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
        if (isAddingAccount) {
            registerAdditionalAccount(email, pass, name, username, onError)
            return
        }
        isLoading = true
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                completeRegistration(result.user, email, pass, name, username, onError)
            }
            .addOnFailureListener { e ->
                isLoading = false
                onError(mapAuthError(e))
            }
    }

    private fun completeRegistration(
        u: FirebaseUser?,
        email: String,
        pass: String,
        name: String,
        username: String,
        onError: (String) -> Unit
    ) {
        if (u == null) {
            isLoading = false
            onError("Пользователь не создан")
            return
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
                    "bannerUrl" to "",
                    "bannerStyle" to "none",
                    "statusTagDisplayMode" to "BOTH",
                    "emojiStatus" to " ",
                    "emailVerified" to false,
                    "createdAt" to com.google.firebase.Timestamp(
                        java.util.Date(u.metadata?.creationTimestamp?.takeIf { it > 0L } ?: System.currentTimeMillis())
                    ),
                    // Верификация и роль — явные дефолты для новых юзеров.
                    // Firestore rules всё равно требуют rank=0 и verification=false
                    // на create, это просто делает документ сразу читаемым без
                    // дополнительных .get(..., default) на клиенте.
                    "rank" to 0,
                    "verification" to false
                )

                viewModelScope.launch {
                    val cleanUsername = username.lowercase().trim()
                    var mainUsernameClaimed = false
                    var profileCreated = false
                    try {
                        // Сначала атомарно резервируем имя на сервере, затем создаём профиль.
                        // Это синхронизирует регистрацию с резервированием дополнительных username.
                        BackendApi.registerMainUsername(cleanUsername)
                        mainUsernameClaimed = true
                        db.collection("users").document(u.uid).set(userMap).await()
                        profileCreated = true
                        // Turns the short-lived PENDING_MAIN reservation into a permanent claim and
                        // keeps the profile username in sync with the shared username registry.
                        BackendApi.changeMainUsername(cleanUsername)

                        user = auth.currentUser
                        accountTokenStore.putPassword(u.uid, pass)
                        loadUserData(u.uid)
                        bindSessionTracking(u.uid)
                        sessionManager.saveSession(UserSession(
                            uid = u.uid,
                            email = email,
                            name = name,
                            avatarUrl = "",
                            username = cleanUsername,
                            createdAt = u.metadata?.creationTimestamp ?: System.currentTimeMillis()
                        ))

                        u.sendEmailVerification()
                            .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось отправить письмо верификации", e) }
                        isLoading = false
                        showVerifyScreen = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (mainUsernameClaimed && !profileCreated) {
                            runCatching { BackendApi.releaseMainUsernameClaim(cleanUsername) }
                                .onFailure { releaseError -> Log.e("AuthVM", "Не удалось освободить резерв username после ошибки регистрации", releaseError) }
                        }
                        isLoading = false
                        val errorMessage = when (e.message?.substringBefore(' ')) {
                            "USERNAME_TAKEN" -> "Этот username уже занят"
                            "INVALID_USERNAME" -> "Username должен содержать 5–32 символа: латинские буквы, цифры и _"
                            else -> e.localizedMessage ?: "Ошибка сохранения профиля"
                        }
                        Log.e("AuthVM", "Не удалось завершить регистрацию профиля", e)
                        onError(errorMessage)
                    }
                }
            } else {
                isLoading = false
                onError(task.exception?.localizedMessage ?: "Ошибка обновления профиля")
            }
        }
    }

    fun logout() {
        isAddingAccount = false
        // Снимаем self-listener ДО удаления документа сессии — иначе поймаем
        // своё же удаление как "сессию завершили с другого устройства" и
        // рекурсивно ещё раз вызовем logout().
        val uid = auth.currentUser?.uid
        val deviceId = myDeviceId
        if (uid != null) {
            val token = runCatching { accountTokenStore.get(uid) }.getOrNull()
            // Remove the local secret immediately; server revocation is best-effort and may
            // fail offline, but must never prevent logout.
            accountTokenStore.remove(uid)
            if (token != null && deviceId != null) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    AccountTokenApi.revoke(uid, deviceId, token)
                }
            }
        }
        unbindSessionTracking()
        if (uid != null && deviceId != null) {
            db.collection("users").document(uid)
                .collection("sessions").document(deviceId)
                .delete()
                .addOnFailureListener { e -> Log.e("AuthVM", "Не удалось удалить сессию при выходе", e) }
        }
        val appContext = getApplication<Application>()
        logoutSilently {
            if (uid != null) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    OfflineDataStore.deleteAccountData(appContext, uid)
                }
            }
            auth.signOut()
            MeshManager.refreshIdentity()
            user = null
            userData = emptyMap()
            resetPremium()
            MeshManager.setBlockedUserIds(emptySet())
            backStack = listOf(Screen.Chats)
        }
    }

    fun logoutSilently(onComplete: () -> Unit = {}) {





        FirestoreListenerCoordinator.tearDownAll()

        val uid = auth.currentUser?.uid

        userDataListener?.remove()
        userDataListener = null

        var completed = false
        val finish = {
            if (!completed) {
                completed = true
                onComplete()
            }
        }

        if (uid != null) {
            val snapshotName = userData["name"] ?: auth.currentUser?.displayName ?: "User"
            val snapshotAvatar = userData["avatarUrl"] ?: ""
            val snapshotUsername = userData["username"]
            val snapshotEmail = auth.currentUser?.email ?: ""
            val snapshotCreatedAt = auth.currentUser?.metadata?.creationTimestamp ?: 0L
            viewModelScope.launch {
                runCatching {
                    sessionManager.saveSession(UserSession(
                        uid = uid,
                        email = snapshotEmail,
                        name = snapshotName,
                        avatarUrl = snapshotAvatar,
                        username = snapshotUsername,
                        createdAt = snapshotCreatedAt
                    ))
                }.onFailure { Log.e("AuthVM", "Не удалось сохранить сессию", it) }

                db.collection("users").document(uid).update(mapOf(
                    "isOnline" to false,
                    "lastSeen" to FieldValue.serverTimestamp()
                )).addOnCompleteListener { finish() }

                kotlinx.coroutines.delay(3000)
                finish()
            }
        } else {
            finish()
        }
    }

    private fun saveAccountTokenForCurrentUser() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val current = auth.currentUser ?: return@launch
            val deviceId = runCatching { sessionManager.getOrCreateDeviceId() }.getOrNull() ?: return@launch
            val token = AccountTokenApi.issue(deviceId) ?: return@launch
            accountTokenStore.put(current.uid, token)
        }
    }


    fun switchAccountByToken(
        targetUid: String,
        onPasswordRequired: () -> Unit,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit
    ) {
        if (targetUid == auth.currentUser?.uid) {
            onSuccess()
            return
        }
        isLoading = true
        viewModelScope.launch {
            val deviceId = try {
                sessionManager.getOrCreateDeviceId()
            } catch (e: Exception) {
                isLoading = false
                onError(e.localizedMessage ?: "Не удалось определить устройство")
                return@launch
            }
            val token = runCatching { accountTokenStore.get(targetUid) }.getOrNull()
            if (token.isNullOrBlank()) {
                switchUsingSavedPassword(targetUid, onPasswordRequired, onError, onSuccess)
                return@launch
            }
            when (val exchange = AccountTokenApi.exchange(targetUid, deviceId, token)) {
                is AccountTokenApi.ExchangeResult.Invalid -> {
                    accountTokenStore.removeToken(targetUid)
                    switchUsingSavedPassword(targetUid, onPasswordRequired, onError, onSuccess)
                }
                AccountTokenApi.ExchangeResult.Unavailable -> {
                    // Token API may be unreachable in some networks; use the locally encrypted
                    // credential fallback before showing a password dialog.
                    switchUsingSavedPassword(targetUid, onPasswordRequired, onError, onSuccess)
                }
                is AccountTokenApi.ExchangeResult.Ok -> {
                    val session = activeSessions.firstOrNull { it.uid == targetUid }
                    if (session == null) {
                        accountTokenStore.removeToken(targetUid)
                        isLoading = false
                        onError("Сохранённый аккаунт не найден. Добавь его заново.")
                        return@launch
                    }
                    leaveCurrentAccount {
                        viewModelScope.launch {
                            try {
                                val result = auth.signInWithCustomToken(exchange.customToken).await()
                                val signedIn = result.user
                                if (signedIn == null || signedIn.uid != targetUid) {
                                    auth.signOut()
                                    accountTokenStore.removeToken(targetUid)
                                    user = null
                                    isLoading = false
                                    switchUsingSavedPassword(targetUid, onPasswordRequired, onError, onSuccess)
                                    return@launch
                                }
                                accountTokenStore.put(targetUid, exchange.newToken)
                                user = signedIn
                                showVerifyScreen = !signedIn.isEmailVerified
                                loadUserData(targetUid)
                                bindSessionTracking(targetUid)
                                sessionManager.saveSession(session)
                                MeshManager.refreshIdentity()
                                isLoading = false
                                if (showVerifyScreen) {
                                    onError("Для этого аккаунта нужно подтвердить почту.")
                                } else {
                                    onSuccess()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.w("AuthVM", "Быстрое переключение по токену не удалось", e)
                                accountTokenStore.removeToken(targetUid)
                                auth.signOut()
                                user = null
                                isLoading = false
                                switchUsingSavedPassword(targetUid, onPasswordRequired, onError, onSuccess)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun switchUsingSavedPassword(
        targetUid: String,
        onPasswordRequired: () -> Unit,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        val session = activeSessions.firstOrNull { it.uid == targetUid }
        if (session == null) {
            isLoading = false
            onError("Сохранённый аккаунт не найден. Добавь его заново.")
            return
        }
        val savedPassword = runCatching { accountTokenStore.getPassword(targetUid) }.getOrNull()
        if (savedPassword.isNullOrBlank()) {
            isLoading = false
            onPasswordRequired()
            return
        }
        isLoading = false
        switchAccount(
            targetEmail = session.email,
            targetPass = savedPassword,
            onError = onError,
            onSuccess = onSuccess
        )
    }

    fun switchAccount(
        targetEmail: String,
        targetPass: String,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit
    ) {
        if (isCurrentAccount(targetEmail)) {
            onSuccess()
            return
        }
        isLoading = true
        viewModelScope.launch {
            val failure = validateOnSecondary(targetEmail, targetPass, create = false)
            if (failure != null) {
                isLoading = false
                val message = mapAuthError(failure)
                authError = message
                onError(message)
                return@launch
            }
            leaveCurrentAccount {
                performLogin(targetEmail, targetPass, onSuccess = {
                    isLoading = false
                    onSuccess()
                }, onError = { message ->
                    isLoading = false
                    authError = message
                    onError(message)
                })
            }
        }
    }

    fun addNewAccount(onNavigateToAuth: () -> Unit, onLimitReached: () -> Unit = {}) {
        viewModelScope.launch {
            if (!sessionManager.canAddSession()) {
                onLimitReached()
                return@launch
            }
            resetAuthForm()
            isAddingAccount = true
            onNavigateToAuth()
        }
    }

    fun cancelAddAccount() {
        isAddingAccount = false
        isLoading = false
        resetAuthForm()
    }

    private fun resetAuthForm() {
        emailInput = ""
        passInput = ""
        nameInput = ""
        usernameInput = ""
        authError = null
        resetMessage = null
        isLoginMode = true
    }

    fun removeSession(uid: String) {
        viewModelScope.launch {
            val token = runCatching { accountTokenStore.get(uid) }.getOrNull()
            val deviceId = runCatching { sessionManager.getOrCreateDeviceId() }.getOrNull()
            accountTokenStore.remove(uid)
            sessionManager.removeSession(uid)
            if (token != null && deviceId != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    AccountTokenApi.revoke(uid, deviceId, token)
                }
            }
        }
    }

    fun sendPasswordReset(onComplete: (String?) -> Unit) {
        val email = user?.email ?: return
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) onComplete(null)
            else onComplete(task.exception?.let { mapAuthError(it) } ?: "Ошибка")
        }
    }

    fun updateEmail(password: String, newEmail: String, onComplete: (String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(getApplication<Application>().getString(R.string.error_reauthentication_required))
            return
        }
        val email = user.email?.takeIf { it.isNotBlank() }
        if (email == null) {
            onComplete(getApplication<Application>().getString(R.string.error_reauthentication_email_missing))
            return
        }
        val credential = EmailAuthProvider.getCredential(email, password)

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
        val user = auth.currentUser
        if (user == null) {
            onComplete(getApplication<Application>().getString(R.string.error_reauthentication_required))
            return
        }
        val email = user.email?.takeIf { it.isNotBlank() }
        if (email == null) {
            onComplete(getApplication<Application>().getString(R.string.error_reauthentication_email_missing))
            return
        }
        val credential = EmailAuthProvider.getCredential(email, oldPass)

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
        var claimDone = false
        var isTaken = false

        fun finish() {
            if (userDone && channelDone && claimDone) onResult(!isTaken)
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
            .addOnFailureListener { e ->
                Log.e("AuthVM", "Не удалось проверить занятость username в users", e)
                isTaken = true
                userDone = true
                finish()
            }

        db.collection("usernameClaims").document(clean).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) isTaken = true
                claimDone = true
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AuthVM", "Не удалось проверить username claim", e)
                // При недоступности реестра нельзя считать имя свободным.
                isTaken = true
                claimDone = true
                finish()
            }

        db.collection("chats")
            .whereEqualTo("username", clean)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) isTaken = true
                channelDone = true
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AuthVM", "Не удалось проверить занятость username в chats", e)
                isTaken = true
                channelDone = true
                finish()
            }
    }

    fun handleAuthAction(onSuccess: () -> Unit) {
        authError = null

        if (emailInput.isEmpty()) { authError = "Введите Email!"; return }
        if (!emailInput.contains("@")) { authError = "Некорректный Email"; return }
        if (passInput.length < 6) { authError = "Пароль мин. 6 символов"; return }

        if (!isLoginMode) {
            if (nameInput.isEmpty()) { authError = "Введите имя"; return }
            if (!usernameInput.matches(Regex("^[a-zA-Z0-9_]{5,32}$"))) {
                authError = "Username должен содержать 5–32 символа: латинские буквы, цифры и _"; return
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
        val usernameQuery = query.removePrefix("@")

        viewModelScope.launch {
            try {
                var snap = db.collection("users")
                    .whereEqualTo("email", query)
                    .limit(1).get().await()

                if (snap.isEmpty) {
                    snap = db.collection("users")
                        .whereEqualTo("username", usernameQuery)
                        .limit(1).get().await()
                }

                var targetUid = snap.documents.firstOrNull()?.id
                if (targetUid == null && usernameQuery.matches(Regex("^[a-z0-9_]{5,32}$"))) {
                    val claim = db.collection("usernameClaims").document(usernameQuery).get().await()
                    val claimKind = claim.getString("kind")
                    if (claimKind in setOf("MAIN", "ADDITIONAL")) {
                        val claimedUid = claim.getString("uid")?.takeIf { it.isNotBlank() }
                        targetUid = if (claimedUid != null && db.collection("users").document(claimedUid).get().await().exists()) {
                            claimedUid
                        } else {
                            null
                        }
                    }
                }

                val resolvedUid = targetUid?.takeIf { it.isNotBlank() }
                if (resolvedUid != null && resolvedUid != myUid) {
                    onStart(Screen.getChatId(myUid, resolvedUid))
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
                    db.collection("usernameClaims").document(username.lowercase().trim()).get()
                        .addOnSuccessListener { claim ->
                            val claimedUid = claim.getString("uid")
                            val kind = claim.getString("kind")
                            if (claimedUid.isNullOrBlank() || kind !in setOf("MAIN", "ADDITIONAL")) {
                                onResult(null)
                            } else {
                                db.collection("users").document(claimedUid).get()
                                    .addOnSuccessListener { userDoc ->
                                        if (!userDoc.exists()) {
                                            onResult(null)
                                        } else {
                                            val data = userDoc.data?.toMutableMap() ?: mutableMapOf()
                                            data["uid"] = userDoc.id
                                            onResult(data)
                                        }
                                    }
                                    .addOnFailureListener { onResult(null) }
                            }
                        }
                        .addOnFailureListener { onResult(null) }
                }
            }
            .addOnFailureListener { onResult(null) }
    }


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


    fun getMyPhone(onResult: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: run { onResult(""); return }
        db.collection("users").document(uid).collection("private").document("contact").get()
            .addOnSuccessListener { doc -> onResult(doc.getString("phone") ?: "") }
            .addOnFailureListener { onResult("") }
    }


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
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("Загрузка аватара завершилась без ошибки SDK")
                }
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

    @Suppress("UNUSED_PARAMETER")
    fun buyItem(id: String, price: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (auth.currentUser?.uid == null) {
            onError("Не авторизован")
            return
        }
        if (id.isBlank()) {
            onError("Некорректный идентификатор товара")
            return
        }
        if (id in purchasingItemIds) return

        purchasingItemIds = purchasingItemIds + id
        viewModelScope.launch {
            try {
                when (val result = buyShopItemViaBackend(id)) {
                    is BuyItemResult.Success -> {
                        // Reflect the authoritative backend result immediately; the Firestore
                        // listener will subsequently reconcile the profile snapshot.
                        userData = userData + ("balance" to result.newBalance.toString())
                        if (result.itemId !in ownedItems) ownedItems = ownedItems + result.itemId
                        onSuccess()
                    }
                    BuyItemResult.LowBalance -> onError("Недостаточно монет!")
                    BuyItemResult.AlreadyOwned -> onError("Уже куплено")
                    BuyItemResult.ItemNotFound -> onError("Товар не найден")
                    BuyItemResult.OutOfSeason -> onError("Товар сейчас недоступен")
                    is BuyItemResult.Error -> {
                        if (!result.outcomeUnknown) {
                            onError(result.message)
                        } else {
                            val fresh = refreshBalanceAndInventory()
                            if (fresh == null) {
                                onError("Не удалось подтвердить покупку. Проверь баланс и инвентарь, прежде чем пробовать снова.")
                            } else {
                                fresh.balance?.let { userData = userData + ("balance" to it.toString()) }
                                ownedItems = fresh.ownedItems
                                if (id in fresh.ownedItems) onSuccess() else onError(result.message)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AuthVM", "Ошибка покупки товара $id", e)
                onError(e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Не удалось выполнить покупку. Проверь подключение и попробуй ещё раз.")
            } finally {
                purchasingItemIds = purchasingItemIds - id
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

        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(uid)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    @Suppress("UNCHECKED_CAST")
                    val inventory = snapshot.get("ownedItems") as? List<Any?> ?: emptyList()
                    // Validate against the latest document inside the same transaction
                    // that updates the equipped field, not a potentially stale Compose state.
                    if (id != "default" && id != "none" && id !in inventory) {
                        throw IllegalStateException("ITEM_NOT_OWNED")
                    }
                    val value: Any = if (id == "default" || id == "none") FieldValue.delete() else id
                    transaction.update(userRef, field, value)
                    true
                }.await()
            } catch (e: Exception) {
                Log.e("AuthVM", "Не удалось применить предмет $id", e)
            }
        }
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