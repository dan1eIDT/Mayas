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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.dan1eidtj.data.SharedContentManager
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme.DarkColors
import com.dan1eidtj.mayas.core_ui.ui.components.*
import com.dan1eidtj.mayas.feature.auth.*
import com.dan1eidtj.mayas.feature.chat.ChatScreen
import com.dan1eidtj.mayas.feature.chats.ChatListScreen.ChatListScreen
import com.dan1eidtj.mayas.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlin.jvm.java


class MainActivity : ComponentActivity() {

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

        checkAndRequestNotifications()
        handleIncomingIntent(intent)

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
                    MayasApp()
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
fun MayasApp(vm: AuthVM = viewModel()) {

    val navController = rememberNavController()
    val user = vm.user
    var showUserSearchDialog by remember { mutableStateOf(false) }
    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0)
            }
        } else {
            if (navController.currentDestination?.route == Screen.Auth.route) {
                navController.navigate(Screen.Chats.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
            vm.getFcmToken()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (user == null) Screen.Auth.route else Screen.Chats.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(vm)
        }

        composable(Screen.Chats.route) {
            ChatListScreen(
                vm = vm,
                onStartChat = { chatId -> navController.navigate(Screen.Chat.create(chatId)) },
                onOpenProfile = { uid -> navController.navigate(Screen.Profile.create(uid)) },
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
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() },
                onOpenProfile = { uid -> navController.navigate(Screen.Profile.create(uid)) }
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
                onNavigate = { targetUid: String, targetIsGroup: Boolean ->
                    navController.navigate(Screen.Profile.create(targetUid, targetIsGroup))
                },
                onNavigateToCredits = {
                    navController.navigate(Screen.Credits.route)
                }
            )
        }

        composable(Screen.Credits.route) {
            CreditsScreen(onBack = { navController.popBackStack() })
        }
    }
}