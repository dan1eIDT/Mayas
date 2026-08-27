@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import com.dan1eidtj.mayas.core.ui.theme.MayasAppTheme
import com.dan1eidtj.mayas.core.ui.theme.MayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.DarkMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.LightMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.ThemeEditorScreen
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.dan1eidtj.mayas.feature.ChatVM
import com.dan1eidtj.mayas.feature.JoinInviteFlow
import com.dan1eidtj.mayas.feature.chats.ChatListScreen.ChatListScreen
import com.dan1eidtj.mayas.NotificationsScreen
import com.dan1eidtj.mayas.ads.AdsManager
import com.dan1eidtj.mayas.core_ui.Screen
import com.dan1eidtj.mayas.settings.CustomizationScreen
import com.dan1eidtj.mayas.settings.SettingsScreen
import com.dan1eidtj.mayas.settings.ThemesScreen
import com.dan1eidtj.mayas.settings.HomeScreenLayoutScreen
import com.dan1eidtj.mayas.settings.SidebarLayoutScreen
import com.dan1eidtj.mayas.core.ui.theme.LayoutPreferences
import com.dan1eidtj.mayas.ui.theme.ThemePreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    private val callManager: CallManager
        get() = (application as CallManagerProvider).callManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Уведомления включены!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Уведомления можно включить позже.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotifications()

        handleIncomingIntent(intent)

        AdsManager.initialize(this)

        setContent {
            var isSplash by remember { mutableStateOf(true) }


            val themePrefsContext = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            var currentColorScheme by remember {
                mutableStateOf(
                    ThemePreferences.loadSelectedScheme(themePrefsContext)
                        ?: if (systemDark) DarkMayasColorScheme else LightMayasColorScheme
                )
            }
            var customThemes by remember {
                mutableStateOf(ThemePreferences.loadCustomThemes(themePrefsContext))
            }

            LaunchedEffect(currentColorScheme) {
                ThemePreferences.saveSelectedScheme(themePrefsContext, currentColorScheme)
            }
            LaunchedEffect(customThemes) {
                ThemePreferences.saveCustomThemes(themePrefsContext, customThemes)
            }

            MayasAppTheme(
                darkTheme = systemDark,
                colorScheme = currentColorScheme
            ) {
                LaunchedEffect(Unit) {
                    delay(1500)
                    isSplash = false
                }

                if (isSplash) {
                    SplashScreen()
                } else {
                    MayasApp(
                        callManager = callManager,
                        currentColorScheme = currentColorScheme,
                        onColorSchemeChange = { currentColorScheme = it },
                        customThemes = customThemes,
                        onCustomThemesChange = { customThemes = it }
                    )
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

        if (Intent.ACTION_VIEW == action) {
            val data = intent.data

            if (data?.scheme == "mayas" && data.host == "join") {
                val code = data.lastPathSegment
                if (!code.isNullOrBlank()) {
                    SharedContentManager.pendingInviteCode = code
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
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showCustomExplanationDialog()
            }
        }
    }

    private fun showCustomExplanationDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("!!!")
            .setMessage(getString(R.string.notif_text))
            .setCancelable(false)
            .setPositiveButton("Включить") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Не сейчас") { d, _ -> d.dismiss() }
            .create()

        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.show()
    }
}
@Composable
fun MayasApp(
    vm: AuthVM = viewModel(),
    callManager: CallManager,
    currentColorScheme: MayasColorScheme,
    onColorSchemeChange: (MayasColorScheme) -> Unit,
    customThemes: List<Pair<String, MayasColorScheme>>,
    onCustomThemesChange: (List<Pair<String, MayasColorScheme>>) -> Unit,
) {

    val navController = rememberNavController()
    val monetizationVm: MonetizationVM = viewModel()
    val user = vm.user
    var showUserSearchDialog by remember { mutableStateOf(false) }

    // Настройки расположения элементов интерфейса — локальные для устройства,
    // не синхронизируются между устройствами (см. LayoutPreferences)
    val layoutPrefsContext = LocalContext.current
    var homeScreenLayoutPrefs by remember {
        mutableStateOf(LayoutPreferences.loadHomeScreenLayoutPrefs(layoutPrefsContext))
    }
    var sidebarLayoutPrefs by remember {
        mutableStateOf(LayoutPreferences.loadSidebarLayoutPrefs(layoutPrefsContext))
    }

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
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onOpenSettings = { navController.navigate(Screen.Settings.route) },
                        onOpenNotifications = { navController.navigate(Screen.Notifications.route) },
                        onOpenCredits = { navController.navigate(Screen.Credits.route) },
                        onLogout = {
                            vm.logout()
                            navController.navigate(Screen.Auth.route) { popUpTo(0) }
                        },
                        onOpenUserSearch = { showUserSearchDialog = true },
                        onDismissUserSearch = { showUserSearchDialog = false },
                        homeLayoutPrefs = homeScreenLayoutPrefs,
                        sidebarLayoutPrefs = sidebarLayoutPrefs,
                        onUpdateSidebarPrefs = { newPrefs ->
                            sidebarLayoutPrefs = newPrefs
                            LayoutPreferences.saveSidebarLayoutPrefs(layoutPrefsContext, newPrefs)
                        }
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
                        },
                        onNavigateToThemes = {
                            navController.navigate(Screen.Themes.route)
                        },
                        onNavigateToAdminShop = {
                            navController.navigate(Screen.AdminShop.route)
                        },
                        onNavigateToHomeScreenLayout = {
                            navController.navigate(Screen.HomeScreenLayout.route)
                        },
                        onNavigateToSidebarLayout = {
                            navController.navigate(Screen.SidebarLayout.route)
                        }
                    )
                }

                composable(Screen.AdminShop.route) {
                    AdminShopScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.Customization.route) {
                    CustomizationScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Themes.route) {
                    ThemesScreen(
                        currentScheme = currentColorScheme,
                        customThemes = customThemes,
                        onSelectScheme = { scheme -> onColorSchemeChange(scheme) },
                        onNavigateToEditor = { navController.navigate(Screen.ThemeEditor.create()) },
                        onEditCustomTheme = { name -> navController.navigate(Screen.ThemeEditor.create(name)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.HomeScreenLayout.route) {
                    HomeScreenLayoutScreen(
                        initialPrefs = homeScreenLayoutPrefs,
                        onSave = { prefs ->
                            homeScreenLayoutPrefs = prefs
                            LayoutPreferences.saveHomeScreenLayoutPrefs(layoutPrefsContext, prefs)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SidebarLayout.route) {
                    SidebarLayoutScreen(
                        initialPrefs = sidebarLayoutPrefs,
                        onSave = { prefs ->
                            sidebarLayoutPrefs = prefs
                            LayoutPreferences.saveSidebarLayoutPrefs(layoutPrefsContext, prefs)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    Screen.ThemeEditor.route,
                    arguments = listOf(
                        navArgument("themeName") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStack ->
                    val themeName = backStack.arguments?.getString("themeName")
                    val initialScheme = themeName
                        ?.let { name -> customThemes.firstOrNull { it.first == name }?.second }
                        ?: currentColorScheme

                    ThemeEditorScreen(
                        initialScheme = initialScheme,
                        onSave = { scheme ->
                            val name = themeName ?: "Тема ${customThemes.size + 1}"
                            onCustomThemesChange(customThemes.filterNot { it.first == name } + (name to scheme))
                            onColorSchemeChange(scheme)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Credits.route) {
                    CreditsScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenChat = { chatId -> navController.navigate(Screen.Chat.create(chatId)) }
                    )
                }

                composable(Screen.Premium.route) {
                    PremiumScreen(vm = monetizationVm, onBack = { navController.popBackStack() })
                }

            }




            val pendingInviteCode = SharedContentManager.pendingInviteCode
            if (pendingInviteCode != null && user != null) {
                val chatVmForInvite: ChatVM = viewModel()
                JoinInviteFlow(
                    inviteCode = pendingInviteCode,
                    onLoadPreview = { code, onResult ->
                        chatVmForInvite.getInviteInfo(code, onResult)
                    },
                    onConfirmJoin = { code, onResult ->
                        chatVmForInvite.joinByInviteCode(
                            code = code,
                            onSuccess = { chatId -> onResult(chatId) },
                            onError = { onResult(null) },
                        )
                    },
                    onFinished = { chatId, _ ->
                        SharedContentManager.pendingInviteCode = null
                        navController.navigate(Screen.Chat.create(chatId))
                    },
                    onCancelled = {
                        SharedContentManager.pendingInviteCode = null
                    },
                )
            }
        }
    }
}