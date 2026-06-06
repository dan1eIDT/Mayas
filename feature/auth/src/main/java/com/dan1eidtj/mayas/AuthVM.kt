    package com.dan1eidtj.mayas.feature.auth

    import android.net.Uri
    import android.util.Log
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.setValue
    import androidx.lifecycle.ViewModel
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.FirebaseUser
    import com.google.firebase.auth.UserProfileChangeRequest
    import com.google.firebase.firestore.FieldValue
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.firestore.SetOptions
    import com.google.firebase.messaging.FirebaseMessaging
    import com.google.firebase.storage.FirebaseStorage
    import kotlin.collections.plus

    open class AuthVM : ViewModel() {
        private val auth = FirebaseAuth.getInstance()
        open val db: FirebaseFirestore = FirebaseFirestore.getInstance()

        var user: FirebaseUser? by mutableStateOf(auth.currentUser)
            private set
        var isLoading by mutableStateOf(false)
            private set
        var userData by mutableStateOf<Map<String, String>>(emptyMap())
            private set
        var appTheme by mutableStateOf("dark")
            private set

        // Стек экранов для кнопки "Назад"
        var backStack by mutableStateOf(listOf("chats"))
            private set


        init {
            if (user != null) loadUserData(user!!.uid)
        }

        private fun loadUserData(uid: String) {
            db.collection("users").document(uid).addSnapshotListener { snap, _ ->
                if (snap?.exists() == true) {
                    val data = snap.data?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
                    userData = data
                    appTheme = data["theme"] ?: "dark"
                }
            }
        }

        fun navigateTo(screen: String) {
            backStack = backStack + screen
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
        fun logout() {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "isOnline" to false,
                        "lastSeen" to FieldValue.serverTimestamp()
                    )
                )
            }
            auth.signOut()
            user = null
            userData = emptyMap()
            backStack = listOf("chats")
        }
        fun navigateBack() {
            if (backStack.size > 1) backStack = backStack.dropLast(1)
        }

        sealed class Screen(val route: String) {
            object Auth : Screen("auth")
            object Chats : Screen("chats")

            object Chat : Screen("chat/{chatId}") {
                fun create(chatId: String) = "chat/$chatId"
            }

            object Profile : Screen("profile/{uid}") {
                fun create(uid: String) = "profile/$uid"
            }

            object Credits : Screen("credits")
        }

        // 🔍 Резолв пользователя: email или username -> uid -> chatId
        fun resolveAndStartChat(
            input: String,
            myUid: String,
            onStart: (String) -> Unit,
            onError: (String) -> Unit
        ) {
            isLoading = true
            val query = input.trim().lowercase()
            db.collection("users")
                .whereEqualTo("email", query)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    if (!snap.isEmpty) startChatFromUid(snap.documents[0].id, myUid, onStart)
                    else db.collection("users").whereEqualTo("username", query).limit(1).get()
                        .addOnSuccessListener { snap2 ->
                            if (!snap2.isEmpty) startChatFromUid(snap2.documents[0].id, myUid, onStart)
                            else {
                                isLoading = false; onError("Пользователь не найден")
                            }
                        }
                }
                .addOnFailureListener { isLoading = false; onError("Ошибка поиска") }
        }
        fun setTyping(chatId: String, isTyping: Boolean) {
            val uid = auth.currentUser?.uid ?: return

            db.collection("chats").document(chatId)
                .update("typing.$uid", isTyping)
        }

        private fun startChatFromUid(targetUid: String, myUid: String, onStart: (String) -> Unit) {
            val chatId = listOf(myUid, targetUid).sorted().joinToString("_")
            isLoading = false
            onStart(chatId)
        }

        // 🖼️ Фикс аватарок: Storage -> Firestore -> Auth -> UI
        fun uploadAvatar(
            uri: Uri,
            onSuccess: (String) -> Unit,
            onError: (String) -> Unit
        ) {
            val ref = FirebaseStorage.getInstance()
                .reference
                .child("avatars/${auth.currentUser?.uid}.jpg")

            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString()) // 🔥 ВАЖНО
                }
                .addOnFailureListener {
                    onError(it.message ?: "error")
                }
        }

        fun login(email: String, pass: String, onError: (String) -> Unit) {
            isLoading = true
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    user = auth.currentUser; user?.uid?.let { loadUserData(it) }; isLoading = false
                }
                .addOnFailureListener { isLoading = false; onError(it.message ?: "Ошибка входа") }
        }
        fun getFcmToken() {

            val uid =
                FirebaseAuth.getInstance()
                    .currentUser?.uid ?: return

            FirebaseMessaging.getInstance()
                .token
                .addOnSuccessListener { token ->

                    db.collection("users")
                        .document(uid)
                        .update(
                            "fcmToken",
                            token
                        )
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
                .addOnSuccessListener { res ->
                    val u = res.user ?: run {
                        isLoading =
                            false; onError("Пользователь не создан"); return@addOnSuccessListener
                    }
                    u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                        .addOnCompleteListener { t ->
                            if (t.isSuccessful) {
                                db.collection("users").document(u.uid).set(
                                    mapOf(
                                        "username" to username.lowercase(),
                                        "email" to email,
                                        "name" to name,
                                        "isOnline" to true,
                                        "theme" to "dark",
                                        "description" to "",
                                        "avatarUrl" to "",
                                        "emojiStatus" to " "
                                    )
                                ).addOnSuccessListener {
                                    isLoading = false; user = auth.currentUser; loadUserData(u.uid)
                                }
                                    .addOnFailureListener { e ->
                                        isLoading = false; onError(
                                        e.message ?: "Ошибка профиля"
                                    )
                                    }
                            } else {
                                isLoading = false; onError(t.exception?.message ?: "Ошибка обновления")
                            }
                        }
                }.addOnFailureListener { e ->
                    isLoading = false; onError(
                    e.message ?: "Ошибка регистрации"
                )
                }
        }

        fun checkUsername(username: String, onResult: (Boolean) -> Unit) {
            db.collection("users").whereEqualTo("username", username.lowercase()).limit(1).get()
                .addOnSuccessListener { snap -> onResult(snap.isEmpty) }
                .addOnFailureListener { onResult(true) }
        }


        fun resolveUserByUsername(username: String, onResult: (String?) -> Unit) {
            db.collection("users")
                .whereEqualTo("username", username.lowercase())
                .limit(1)
                .get()
                .addOnSuccessListener {
                    val uid = it.documents.firstOrNull()?.id
                    onResult(uid)
                }
                .addOnFailureListener {
                    onResult(null)
                }
        }

        fun updateLocalSettings(description: String, theme: String) {
            userData = userData + mapOf("description" to description, "theme" to theme)
            appTheme = theme
        }
    }

    sealed class Screen(val route: String) {
        object Profile : Screen("profile")

        object Chat : Screen("chat/{chatId}") {
            fun create(chatId: String) = "chat/$chatId"
        }

        // Чтобы вызов AuthVM.Screen.getChatId работал, метод должен быть здесь:
        companion object {
            fun getChatId(uid1: String, uid2: String): String {
                return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
            }
        }
    }
