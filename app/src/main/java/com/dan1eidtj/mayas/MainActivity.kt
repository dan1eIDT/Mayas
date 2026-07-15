@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import com.dan1eidtj.mayas.core.ui.theme.MayasAppTheme
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.dan1eidtj.data.SharedContentManager
import com.dan1eidtj.mayas.core_ui.ui.components.*
import com.dan1eidtj.mayas.feature.auth.*
import com.dan1eidtj.mayas.feature.ChatScreen
import com.dan1eidtj.mayas.feature.chats.ChatListScreen.ChatListScreen
import com.dan1eidtj.mayas.ads.AdsManager
import com.dan1eidtj.mayas.CallManager
import com.dan1eidtj.mayas.CallType
import com.dan1eidtj.mayas.FirestoreCallRepository
import com.dan1eidtj.mayas.core_ui.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    private val callManager by lazy {
        CallManager(
            callRepository = FirestoreCallRepository(),
            webRtcClient = WebRtcClientImpl(applicationContext),
            audioController = SystemAudioController(applicationContext),
            currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() },
            callFeedbackController = CallFeedbackController(applicationContext),
            showError = { message ->
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            callPushNotifier = CallPushNotifier(),
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Уведомления включены! Ровного общения!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Без уведомлений можно пропустить важный движ", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotifications() // проверка уведомлений

        handleIncomingIntent(intent) // а хуй знает

        AdsManager.initialize(this) // реклама

        setContent {
            MayasAppTheme {
                var isSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1500)
                    isSplash = false
                }

                if (isSplash) {
                    SplashScreen()
                } else {
                    MayasApp(callManager = callManager)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                    SharedContentManager.sharedText = sharedText
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateOnlineStatus(true)
    }

    override fun onPause() {
        super.onPause()
        updateOnlineStatus(false)
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
            )
    }

    private fun checkAndRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showCustomExplanationDialog()
            }
        }
    }

    private fun showCustomExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Важное уведомление 📢")
            .setMessage("Включи уведомления, чтобы моментально узнавать, кто тебе чирканул в Маяс!")
            .setCancelable(false)
            .setPositiveButton("Включить") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Не сейчас") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

@Composable
fun MayasApp(vm: AuthVM = viewModel(), callManager: CallManager) {

    val navController = rememberNavController()
    val monetizationVm: MonetizationVM = viewModel()
    val user = vm.user
    var showUserSearchDialog by remember { mutableStateOf(false) }
    LaunchedEffect(user) {
        if (user == null) {
            callManager.stopListeningForIncomingCalls()
            navController.navigate(Screen.Auth.route) {
                popUpTo(0)
            }
        } else {
            callManager.startListeningForIncomingCalls()
            if (navController.currentDestination?.route == Screen.Auth.route) {
                navController.navigate(Screen.Chats.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
            vm.getFcmToken()
        }
    }

    CallHost(callManager = callManager, currentUserId = user?.uid.orEmpty()) {
        NavHost(
            navController = navController,
            startDestination = if (user == null) Screen.Auth.route else Screen.Chats.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400, easing = EaseInOutQuart)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400, easing = EaseInOutQuart)
                ) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400, easing = EaseInOutQuart)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400, easing = EaseInOutQuart)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(vm)
            }

            composable(Screen.Chats.route) {
                ChatListScreen(
                    vm = vm,
                    onStartChat = { chatId -> navController.navigate(Screen.Chat.create(chatId)) },
                    onOpenProfile = { uid -> navController.navigate(Screen.Profile.create(uid, isGroup = false)) },
                    onOpenSettings = { navController.navigate(Screen.Profile.create(vm.user?.uid ?: "")) },
                    onOpenCredits = { navController.navigate(Screen.Credits.route) },
                    onLogout = {
                        vm.logout()
                        navController.navigate(Screen.Auth.route) { popUpTo(0) }
                    },
                    onOpenUserSearch = { showUserSearchDialog = true },
                    onDismissUserSearch = { showUserSearchDialog = false }
                )
            }

            composable(
                Screen.Chat.route,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStack ->
                val chatId = backStack.arguments?.getString("chatId") ?: return@composable
                val context = LocalContext.current
                var pendingCall by remember { mutableStateOf<Pair<String, CallType>?>(null) }
                val recordAudioLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        pendingCall?.let { (peerId, type) -> callManager.startOutgoingCall(peerId, type) }
                    }
                    pendingCall = null
                }

                ChatScreen(
                    chatId = chatId,
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { uid, isGroup ->
                        navController.navigate(Screen.Profile.create(uid, isGroup)) },
                    onStartCall = { peerId, callType ->
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            callManager.startOutgoingCall(peerId, callType)
                        } else {
                            pendingCall = peerId to callType
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            composable(
                Screen.Profile.route,
                arguments = listOf(
                    navArgument("uid") { type = NavType.StringType },
                    navArgument("isGroup") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStack ->
                val uid = backStack.arguments?.getString("uid") ?: return@composable
                val isGroup = backStack.arguments?.getBoolean("isGroup") ?: false
                ProfileScreen(
                    targetId = uid,
                    isGroup = isGroup,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onNavigateToProfile = { targetUid, targetIsGroup ->
                        navController.navigate(Screen.Profile.create(targetUid, targetIsGroup))
                    },
                    onNavigateToChat = { chatId ->
                        // Переход непосредственно в чат
                        navController.navigate(Screen.Chat.create(chatId))
                    },
                    onNavigateToCredits = {
                        navController.navigate(Screen.Credits.route)
                    },
                    onNavigateToPremium = {
                        navController.navigate(Screen.Premium.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCustomization = {
                        navController.navigate(Screen.Customization.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                    onNavigateToCredits = { navController.navigate(Screen.Credits.route) },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0)
                        }
                    },
                    onNavigateToCustomization = {
                        navController.navigate(Screen.Customization.route)
                    }
                )
            }

            composable(Screen.Customization.route) {
                CustomizationScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Credits.route) {
                CreditsScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Premium.route) {
                PremiumScreen(vm = monetizationVm, onBack = { navController.popBackStack() })
            }

        }
    }
}