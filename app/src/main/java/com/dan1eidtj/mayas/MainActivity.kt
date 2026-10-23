/* Copyright (C) 2026 dan1eIDT */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import com.dan1eidtj.mayas.core.ui.theme.MayasAppTheme
import com.dan1eidtj.mayas.core.ui.theme.MayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.DarkMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.isStandardScheme
import com.dan1eidtj.mayas.core.ui.theme.LightMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.ThemeEditorScreen
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import android.net.Uri
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.dan1eidtj.data.SharedContentManager
import com.dan1eidtj.data.ShopBannerRepository
import com.dan1eidtj.data.ShopRepository
import com.dan1eidtj.data.ShopTabRepository
import com.dan1eidtj.mayas.core_ui.ui.components.*
import com.dan1eidtj.mayas.feature.auth.*
import com.dan1eidtj.mayas.feature.ChatScreen
import com.dan1eidtj.mayas.feature.ChatVM
import com.dan1eidtj.mayas.feature.JoinInviteFlow
import com.dan1eidtj.mayas.feature.chats.ChatListScreen.ChatListScreen
import com.dan1eidtj.mayas.feature.chats.ChatListScreen.ChatListViewModel
import com.dan1eidtj.mayas.feature.chats.ChatListScreen.GlobalSearchScreen
import com.dan1eidtj.mayas.NotificationsScreen
import com.dan1eidtj.mayas.ads.AdsManager
import com.dan1eidtj.mayas.core_ui.Screen
import com.dan1eidtj.mayas.mesh.MeshManager
import com.dan1eidtj.mayas.settings.CustomizationScreen
import com.dan1eidtj.mayas.settings.SettingsScreen
import com.dan1eidtj.mayas.settings.ThemesScreen
import com.dan1eidtj.mayas.settings.HomeScreenLayoutScreen
import com.dan1eidtj.mayas.settings.SidebarLayoutScreen
import com.dan1eidtj.mayas.settings.NotificationSettingsScreen
import com.dan1eidtj.mayas.core.ui.theme.LayoutPreferences
import com.dan1eidtj.mayas.ui.theme.ThemePreferences
import com.dan1eidtj.data.ItemType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale


class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageTag = newBase
            .getSharedPreferences("mayas_app_preferences", Context.MODE_PRIVATE)
            .getString("app_language", "")
            .orEmpty()
        if (languageTag.isBlank()) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    private val callManager: CallManager
        get() = (application as CallManagerProvider).callManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.notifications_enabled_toast), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.notifications_later_toast), Toast.LENGTH_LONG).show()
        }
    }

    @Volatile
    private var contentReady = false

    private fun applyLaunchTheme(scheme: MayasColorScheme) {
        val color = scheme.background.toArgb()
        val dark = scheme.background.luminance() < 0.5f

        window.setBackgroundDrawable(ColorDrawable(color))
        @Suppress("DEPRECATION")
        window.statusBarColor = color
        @Suppress("DEPRECATION")
        window.navigationBarColor = color
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            splashScreen.setSplashScreenTheme(
                if (dark) R.style.Theme_App_Starting_Dark else R.style.Theme_App_Starting_Light
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        val launchSplash = installSplashScreen()
        launchSplash.setKeepOnScreenCondition { !contentReady }
        super.onCreate(savedInstanceState)

        val systemNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val savedScheme = ThemePreferences.loadSelectedScheme(this)
        val followSystemAtLaunch = ThemePreferences.loadFollowSystem(this)
        val launchScheme = when {
            savedScheme == null -> if (systemNight) DarkMayasColorScheme else LightMayasColorScheme
            followSystemAtLaunch && savedScheme.isStandardScheme() ->
                if (systemNight) DarkMayasColorScheme else LightMayasColorScheme
            else -> savedScheme
        }
        applyLaunchTheme(launchScheme)

        checkAndRequestNotifications()

        handleIncomingIntent(intent)

        setContent {
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
            var followSystemTheme by remember {
                mutableStateOf(ThemePreferences.loadFollowSystem(themePrefsContext))
            }
            val effectiveColorScheme =
                if (followSystemTheme && currentColorScheme.isStandardScheme()) {
                    if (systemDark) DarkMayasColorScheme else LightMayasColorScheme
                } else {
                    currentColorScheme
                }
            LaunchedEffect(followSystemTheme) {
                ThemePreferences.saveFollowSystem(themePrefsContext, followSystemTheme)
            }
            LaunchedEffect(effectiveColorScheme) {
                applyLaunchTheme(effectiveColorScheme)
            }

            LaunchedEffect(currentColorScheme) {
                ThemePreferences.saveSelectedScheme(themePrefsContext, currentColorScheme)
            }
            LaunchedEffect(customThemes) {
                ThemePreferences.saveCustomThemes(themePrefsContext, customThemes)
            }

            MayasAppTheme(
                colorScheme = effectiveColorScheme
            ) {
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    contentReady = true
                    AdsManager.initialize(this@MainActivity)
                }

                MayasApp(
                    callManager = callManager,
                    currentColorScheme = effectiveColorScheme,
                    onColorSchemeChange = { currentColorScheme = it },
                    followSystemTheme = followSystemTheme,
                    onFollowSystemThemeChange = { followSystemTheme = it },
                    customThemes = customThemes,
                    onCustomThemesChange = { customThemes = it }
                )
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
            val data = intent.data ?: return
            when {
                data.scheme == "mayas" && data.host == "join" -> {
                    val code = data.getQueryParameter("code") ?: data.lastPathSegment
                    if (!code.isNullOrBlank() && code.length <= 256 && '/' !in code) {
                        SharedContentManager.pendingInviteCode = code
                    }
                }
                data.scheme == "https" && data.host == "dan1eidt.github.io" &&
                    data.path.orEmpty().startsWith("/mayas-site/join") -> {
                    val code = data.getQueryParameter("code")
                    if (!code.isNullOrBlank() && code.length <= 256 && '/' !in code) {
                        SharedContentManager.pendingInviteCode = code
                    }
                }
                data.scheme == "mayas" && data.host == "chat" -> {
                    val chatId = data.getQueryParameter("chatId")
                    val messageId = data.getQueryParameter("messageId")
                    val validId: (String?) -> Boolean = { value ->
                        !value.isNullOrBlank() && value.length <= 1500 && '/' !in value
                    }
                    if (validId(chatId) && validId(messageId)) {
                        SharedContentManager.pendingChatId = chatId
                        SharedContentManager.pendingMessageId = messageId
                    }
                }
            }
        }
    }
    // pay 2 win для тф2 2 функции ниже :
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
            .setPositiveButton(getString(R.string.enable_action)) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton(getString(R.string.not_now_action)) { d, _ -> d.dismiss() }
            .create()

        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.show()
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MayasApp(
    vm: AuthVM = viewModel(),
    callManager: CallManager,
    currentColorScheme: MayasColorScheme,
    onColorSchemeChange: (MayasColorScheme) -> Unit,
    followSystemTheme: Boolean = false,
    onFollowSystemThemeChange: (Boolean) -> Unit = {},
    customThemes: List<Pair<String, MayasColorScheme>>,
    onCustomThemesChange: (List<Pair<String, MayasColorScheme>>) -> Unit,
) {

    val navController = rememberNavController()
    val monetizationVm: MonetizationVM = viewModel()
    val user = vm.user
    var showUserSearchDialog by remember { mutableStateOf(false) }

    // ты че дебил?
    val layoutPrefsContext = LocalContext.current
    var homeScreenLayoutPrefs by remember {
        mutableStateOf(LayoutPreferences.loadHomeScreenLayoutPrefs(layoutPrefsContext))
    }
    var sidebarLayoutPrefs by remember {
        mutableStateOf(LayoutPreferences.loadSidebarLayoutPrefs(layoutPrefsContext))
    }
    var seasonalEffectsEnabled by remember {
        mutableStateOf(
            layoutPrefsContext.getSharedPreferences("mayas_app_preferences", Context.MODE_PRIVATE)
                .getBoolean("seasonal_effects_enabled", false)
        )
    }
    val updateSeasonalEffects: (Boolean) -> Unit = { enabled ->
        seasonalEffectsEnabled = enabled
        layoutPrefsContext.getSharedPreferences("mayas_app_preferences", Context.MODE_PRIVATE)
            .edit().putBoolean("seasonal_effects_enabled", enabled).apply()
    }

    LaunchedEffect(user) {
        if (user == null) {
            callManager.stopListeningForIncomingCalls()
            navController.navigate(Screen.Auth.route) {
                popUpTo(0)
            }
        } else {
            callManager.startListeningForIncomingCalls()
            // Стартуем эти три подписки только когда юзер точно авторизован — раньше они
            // запускались в onCreate() безусловно, до восстановления сессии FirebaseAuth
            // на холодном старте, из-за чего Firestore видел request.auth == null и отдавал
            // PERMISSION_DENIED даже на правила, которые сами по себе корректны.
            ShopRepository.startListening()
            ShopBannerRepository.startListening()
            ShopTabRepository.startListening()
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
                    val isChatListTransition =
                        (initialState.destination.route == Screen.Chats.route &&
                            targetState.destination.route == Screen.Chat.route) ||
                            (initialState.destination.route == Screen.Chat.route &&
                                targetState.destination.route == Screen.Chats.route)
                    if (isChatListTransition) {
                        EnterTransition.None
                    } else {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(260, easing = EaseInOutQuart)
                        ) + fadeIn(animationSpec = tween(200))
                    }
                },
                exitTransition = {
                    val isChatListTransition =
                        (initialState.destination.route == Screen.Chats.route &&
                            targetState.destination.route == Screen.Chat.route) ||
                            (initialState.destination.route == Screen.Chat.route &&
                                targetState.destination.route == Screen.Chats.route)
                    if (isChatListTransition) {
                        ExitTransition.None
                    } else {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(260, easing = EaseInOutQuart)
                        ) + fadeOut(animationSpec = tween(200))
                    }
                },
                popEnterTransition = {
                    val isChatListTransition =
                        (initialState.destination.route == Screen.Chats.route &&
                            targetState.destination.route == Screen.Chat.route) ||
                            (initialState.destination.route == Screen.Chat.route &&
                                targetState.destination.route == Screen.Chats.route)
                    if (isChatListTransition) {
                        EnterTransition.None
                    } else {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(260, easing = EaseInOutQuart)
                        ) + fadeIn(animationSpec = tween(200))
                    }
                },
                popExitTransition = {
                    val isChatListTransition =
                        (initialState.destination.route == Screen.Chats.route &&
                            targetState.destination.route == Screen.Chat.route) ||
                            (initialState.destination.route == Screen.Chat.route &&
                                targetState.destination.route == Screen.Chats.route)
                    if (isChatListTransition) {
                        ExitTransition.None
                    } else {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(260, easing = EaseInOutQuart)
                        ) + fadeOut(animationSpec = tween(200))
                    }
                }
            ) {
                composable(Screen.Auth.route) {
                    AuthScreen(
                        vm = vm,
                        onReturnToAccount = { navController.popBackStack() }
                    )
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
                        onOpenGlobalSearch = { navController.navigate(Screen.GlobalSearch.route) },
                        homeLayoutPrefs = homeScreenLayoutPrefs,
                        sidebarLayoutPrefs = sidebarLayoutPrefs,
                        onUpdateSidebarPrefs = { newPrefs ->
                            sidebarLayoutPrefs = newPrefs
                            LayoutPreferences.saveSidebarLayoutPrefs(layoutPrefsContext, newPrefs)
                        }
                    )
                }

                composable(Screen.GlobalSearch.route) {
                    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val chatsEntry = remember { navController.getBackStackEntry(Screen.Chats.route) }
                    val chatListVm: ChatListViewModel = viewModel(viewModelStoreOwner = chatsEntry)
                    val localChats by chatListVm.chats.collectAsState()
                    val mappedChats = remember(localChats) {
                        localChats.map { entity ->
                            mapOf(
                                "chatId" to entity.chatId,
                                "isGroup" to entity.isGroup,
                                "groupName" to (entity.groupName ?: ""),
                                "partnerName" to (entity.partnerName ?: ""),
                                "groupAvatarUrl" to (entity.groupAvatarUrl ?: ""),
                                "partnerAvatarUrl" to (entity.partnerAvatarUrl ?: ""),
                                "description" to (entity.description ?: "")
                            )
                        }
                    }
                    GlobalSearchScreen(
                        myUid = myUid,
                        localChats = mappedChats,
                        onBack = { navController.popBackStack() },
                        onOpenChat = { chatId, messageId ->
                            navController.navigate(Screen.Chat.create(chatId, messageId))
                        },
                        onOpenProfile = { uid ->
                            navController.navigate(Screen.Profile.create(uid, isGroup = false))
                        }
                    )
                }

                composable(
                    Screen.Chat.route,
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("messageId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStack ->
                    val chatId = backStack.arguments?.getString("chatId") ?: return@composable
                    val scrollToMessageId = backStack.arguments?.getString("messageId")
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
                        scrollToMessageId = scrollToMessageId,
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
                        onNavigateToChat = { chatId, messageId ->
                            navController.navigate(Screen.Chat.create(chatId, messageId))
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
                        seasonalEffectsEnabled = seasonalEffectsEnabled,
                        onSeasonalEffectsEnabledChange = updateSeasonalEffects,
                        onBack = { navController.popBackStack() },
                        onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                        onNavigateToCredits = { navController.navigate(Screen.Credits.route) },
                        onNavigateToAuth = {
                            navController.navigate(Screen.Auth.route)
                        },
                        onNavigateToCustomization = {
                            navController.navigate(Screen.Customization.route)
                        },
                        onNavigateToPlugins = {
                            navController.navigate(Screen.Plugins.route)
                        },
                        onNavigateToMesh = {
                            navController.navigate(Screen.Mesh.route)
                        },
                        onNavigateToSteam = {
                            navController.navigate(Screen.Steam.route)
                        },
                        onNavigateToThemes = {
                            navController.navigate(Screen.Themes.route)
                        },
                        onNavigateToAdminShop = {
                            navController.navigate(Screen.AdminShop.route)
                        },
                        onNavigateToShop = {
                            navController.navigate(Screen.Shop.route)
                        },
                        onNavigateToHomeScreenLayout = {
                            navController.navigate(Screen.HomeScreenLayout.route)
                        },
                        onNavigateToSidebarLayout = {
                            navController.navigate(Screen.SidebarLayout.route)
                        },
                        onNavigateToNotificationSettings = {
                            navController.navigate(Screen.NotificationSettings.route)
                        }
                    )
                }

                composable(Screen.NotificationSettings.route) {
                    NotificationSettingsScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AdminShop.route) {
                    AdminShopScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.Shop.route) {
                    val context = LocalContext.current
                    ShopScreen(
                        balance = vm.userData["balance"]?.toIntOrNull() ?: 0,
                        ownedItems = vm.ownedItems,
                        onBack = { navController.popBackStack() },
                        onBuyItem = { id, price, itemName ->
                            vm.buyItem(
                                id, price,
                                onSuccess = {
                                    Toast.makeText(context, "Куплено: $itemName", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        onSelectItem = { id, type ->
                            vm.useItem(id, type)
                        },
                        currentEmoji = vm.userData["emojiStatus"] ?: "",
                        messageStyle = vm.userData["messageStyle"] ?: "",
                        currentFont = vm.userData["fontId"] ?: "",
                        currentEffect = vm.userData["effectId"] ?: "",
                        isPurchaseInProgress = vm.isPurchaseInProgress
                    )
                }

                composable(Screen.Customization.route) {
                    CustomizationScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Steam.route) {
                    com.dan1eidtj.mayas.settings.SteamScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Mesh.route) {
                    com.dan1eidtj.mayas.settings.MeshScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Plugins.route) {
                    com.dan1eidtj.mayas.settings.PluginsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Themes.route) {
                    ThemesScreen(
                        currentScheme = currentColorScheme,
                        customThemes = customThemes,
                        onSelectScheme = { scheme -> onColorSchemeChange(scheme) },
                        followSystem = followSystemTheme,
                        onFollowSystemChange = onFollowSystemThemeChange,
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

                    val defaultThemeNameTemplate = stringResource(R.string.theme_default_name)

                    ThemeEditorScreen(
                        initialScheme = initialScheme,
                        onSave = { scheme ->
                            val name = themeName ?: String.format(defaultThemeNameTemplate, customThemes.size + 1)
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

            val meshUiState by MeshManager.state.collectAsState()
            LaunchedEffect(user?.uid, meshUiState.pendingConnections) {
                if (user == null) {
                    meshUiState.pendingConnections.forEach { pending ->
                        MeshManager.rejectPendingConnection(pending.endpointId)
                    }
                }
            }
            val pendingMeshConnection = meshUiState.pendingConnections.firstOrNull { !it.acceptedByUser }
            if (user != null && pendingMeshConnection != null) {
                AlertDialog(
                    onDismissRequest = {
                        MeshManager.rejectPendingConnection(pendingMeshConnection.endpointId)
                    },
                    title = {
                        Text(stringResource(com.dan1eidtj.mayas.settings.R.string.mesh_pairing_title))
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(
                                    com.dan1eidtj.mayas.settings.R.string.mesh_pairing_peer,
                                    pendingMeshConnection.name
                                )
                            )
                            Text(
                                pendingMeshConnection.authenticationDigits,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(stringResource(com.dan1eidtj.mayas.settings.R.string.mesh_pairing_compare))
                            pendingMeshConnection.mayasUid?.takeIf { it.isNotBlank() }?.let { claimedUid ->
                                Text(
                                    stringResource(
                                        com.dan1eidtj.mayas.settings.R.string.mesh_pairing_uid,
                                        claimedUid
                                    )
                                )
                            }
                            Text(stringResource(com.dan1eidtj.mayas.settings.R.string.mesh_pairing_uid_warning))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            MeshManager.acceptPendingConnection(pendingMeshConnection.endpointId)
                        }) {
                            Text(stringResource(com.dan1eidtj.mayas.settings.R.string.mesh_pairing_accept))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            MeshManager.rejectPendingConnection(pendingMeshConnection.endpointId)
                        }) {
                            Text(stringResource(com.dan1eidtj.mayas.settings.R.string.mesh_pairing_reject))
                        }
                    }
                )
            }

            val pendingLinkedChatId = SharedContentManager.pendingChatId
            val pendingLinkedMessageId = SharedContentManager.pendingMessageId
            LaunchedEffect(pendingLinkedChatId, pendingLinkedMessageId, user?.uid) {
                if (user != null && !pendingLinkedChatId.isNullOrBlank()) {
                    SharedContentManager.pendingChatId = null
                    SharedContentManager.pendingMessageId = null
                    navController.navigate(
                        Screen.Chat.create(pendingLinkedChatId, pendingLinkedMessageId)
                    ) {
                        launchSingleTop = true
                    }
                }
            }

            SeasonalEffectsOverlay(
                enabled = seasonalEffectsEnabled,
                modifier = Modifier.fillMaxSize()
            )

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