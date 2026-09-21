/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dan1eidtj.data.UserSession
import com.dan1eidtj.data.AdminConfig
import com.dan1eidtj.data.cache.AppCacheManager
import com.dan1eidtj.data.cache.CacheBreakdown
import com.dan1eidtj.mayas.core_ui.emoji.EmojiGlyphStyled
import com.dan1eidtj.mayas.core_ui.emoji.EmojiStyle
import com.dan1eidtj.mayas.core_ui.emoji.EmojiStyleState
import com.dan1eidtj.mayas.storage.MediaCachePrefs
import com.dan1eidtj.mayas.storage.MediaFileCache
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AuthVM,
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToCredits: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onNavigateToAdminShop: () -> Unit,
    onNavigateToShop: () -> Unit = {},
    onNavigateToHomeScreenLayout: () -> Unit = {},
    onNavigateToSidebarLayout: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    var showChatSettings by remember { mutableStateOf(false) }
    var showInterfaceSettings by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showActiveSessions by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var accountToSwitch by remember { mutableStateOf<UserSession?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var switchPassword by remember { mutableStateOf("") }
    var switchError by remember { mutableStateOf<String?>(null) }
    var showStorage by remember { mutableStateOf(false) }

    if (showPasswordDialog && accountToSwitch != null) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                switchPassword = ""
                switchError = null
            },
            title = { Text(stringResource(R.string.switch_account_title), color = MayasTheme.TextPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.switch_password_prompt, accountToSwitch?.email.orEmpty()), color = MayasTheme.TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = switchPassword,
                        onValueChange = { switchPassword = it; switchError = null },
                        label = { Text(stringResource(R.string.password_label)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = switchError != null,
                        supportingText = { switchError?.let { Text(it, color = MayasTheme.ErrorRed) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MayasTheme.TextPrimary,
                            unfocusedTextColor = MayasTheme.TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.switchAccount(
                            accountToSwitch!!.email,
                            switchPassword,
                            onError = { switchError = it }
                        ) {
                            showPasswordDialog = false
                            switchPassword = ""
                            onBack()
                        }
                    },
                    enabled = switchPassword.isNotBlank() && !vm.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
                ) {
                    if (vm.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text(stringResource(R.string.sign_in_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text(stringResource(R.string.storage_cancel), color = MayasTheme.TextSecondary)
                }
            },
            containerColor = MayasTheme.Surface
        )
    }


    val currentUserName = vm.userData["name"] ?: vm.userData["username"] ?: stringResource(R.string.default_user_name)
    val currentUserAvatar = vm.userData["avatarUrl"] ?: vm.userData["photoUrl"] ?: ""

    if (showAccountSheet) {
        AccountSwitchSheet(
            sessions = vm.activeSessions,
            currentUid = vm.user?.uid,
            currentDeviceCount = vm.remoteSessions.size,
            onSelect = { session ->
                if (session.uid != vm.user?.uid) {
                    accountToSwitch = session
                    showAccountSheet = false
                    showPasswordDialog = true
                }
            },
            onDeleteSession = { uid ->
                vm.removeSession(uid)
            },
            onAddAccount = {
                showAccountSheet = false
                vm.addNewAccount(
                    onNavigateToAuth = onNavigateToAuth,
                    onLimitReached = {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(com.dan1eidtj.auth.R.string.auth_account_limit_reached),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = MayasTheme.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {

            item {
                UserProfileHeader(
                    name = currentUserName,
                    email = vm.user?.email ?: "",
                    avatarUrl = currentUserAvatar,
                    isPremium = vm.isPremium,
                    onHeaderClick = { showAccountSheet = true }
                )
                Spacer(Modifier.height(20.dp))
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_account)) }

            item {
                val premiumSubtitle = if (vm.isPremium) {
                    val dateStr = vm.premiumUntil?.let {
                        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        stringResource(R.string.settings_premium_until, sdf.format(it.toDate()))
                    } ?: ""
                    stringResource(R.string.settings_premium_active, dateStr)
                } else {
                    stringResource(R.string.settings_premium_get)
                }

                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.AutoAwesome,
                        iconBackground = MayasTheme.GlowGold,
                        title = "MAYAS+",
                        subtitle = premiumSubtitle,
                        showDivider = false,
                        onClick = onNavigateToPremium
                    )
                    SettingsRow(
                        icon = Icons.Default.ShoppingBag,
                        iconBackground = MayasTheme.GlowOrange,
                        title = stringResource(R.string.settings_shop_title),
                        subtitle = stringResource(R.string.settings_shop_subtitle),
                        onClick = onNavigateToShop
                    )
                    SettingsRow(
                        icon = Icons.Default.People,
                        iconBackground = MayasTheme.GlowSky,
                        title = stringResource(R.string.settings_accounts_title),
                        subtitle = stringResource(R.string.settings_accounts_subtitle, vm.activeSessions.size),
                        onClick = { showAccountSheet = true }
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_settings)) }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.Brush,
                        iconBackground = MayasTheme.GlowPink,
                        title = stringResource(R.string.settings_customization_title),
                        subtitle = stringResource(R.string.settings_customization_subtitle),
                        showDivider = false,
                        onClick = onNavigateToCustomization
                    )
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        iconBackground = MayasTheme.GlowPurple,
                        title = stringResource(R.string.settings_themes_title),
                        subtitle = stringResource(R.string.settings_themes_subtitle),
                        onClick = onNavigateToThemes
                    )
                    SettingsRow(
                        icon = Icons.Default.Dashboard,
                        iconBackground = Color(0xFF26A69A),
                        title = stringResource(R.string.settings_interface_title),
                        subtitle = stringResource(R.string.settings_interface_subtitle),
                        expanded = showInterfaceSettings,
                        onClick = { showInterfaceSettings = !showInterfaceSettings }
                    )
                    SettingsExpandable(showInterfaceSettings) {
                        InterfaceSettingsSubSection(
                            onNavigateToHomeScreenLayout = onNavigateToHomeScreenLayout,
                            onNavigateToSidebarLayout = onNavigateToSidebarLayout
                        )
                    }
                    if (AdminConfig.isAdmin(FirebaseAuth.getInstance().currentUser?.uid)) {
                        SettingsRow(
                            icon = Icons.Default.Storefront,
                            iconBackground = MayasTheme.GlowOrange,
                            title = stringResource(R.string.settings_admin_title),
                            subtitle = stringResource(R.string.settings_admin_subtitle),
                            onClick = onNavigateToAdminShop
                        )
                    }
                    SettingsRow(
                        icon = Icons.Default.Chat,
                        iconBackground = MayasTheme.Accent,
                        title = stringResource(R.string.settings_chats_title),
                        subtitle = stringResource(R.string.settings_chats_subtitle),
                        expanded = showChatSettings,
                        onClick = { showChatSettings = !showChatSettings }
                    )
                    SettingsExpandable(showChatSettings) { ChatSettingsSubSection(vm) }
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        iconBackground = Color(0xFF34C759),
                        title = stringResource(R.string.settings_privacy_title),
                        subtitle = stringResource(R.string.settings_privacy_subtitle),
                        expanded = showPrivacy,
                        onClick = { showPrivacy = !showPrivacy }
                    )
                    SettingsExpandable(showPrivacy) { PrivacySubSection(vm, onNavigateToPremium) }
                    SettingsRow(
                        icon = Icons.Default.PhoneAndroid,
                        iconBackground = MayasTheme.GlowOrange,
                        title = stringResource(R.string.settings_sessions_title),
                        subtitle = stringResource(R.string.settings_sessions_subtitle, vm.remoteSessions.size),
                        expanded = showActiveSessions,
                        onClick = { showActiveSessions = !showActiveSessions }
                    )
                    SettingsExpandable(showActiveSessions) { ActiveSessionsSubSection(vm) }
                    SettingsRow(
                        icon = Icons.Default.Security,
                        iconBackground = Color(0xFF8E8E93),
                        title = stringResource(R.string.settings_security_title),
                        subtitle = stringResource(R.string.settings_security_subtitle),
                        expanded = showSecurity,
                        onClick = { showSecurity = !showSecurity }
                    )
                    SettingsExpandable(showSecurity) { SecuritySubSection(vm, onNavigateToAuth) }
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        iconBackground = MayasTheme.ErrorRed,
                        title = stringResource(R.string.settings_notifications_title),
                        subtitle = stringResource(R.string.settings_notifications_subtitle),
                        onClick = onNavigateToNotificationSettings
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_app)) }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.Storage,
                        iconBackground = Color(0xFF30B0C7),
                        title = stringResource(R.string.settings_storage_title),
                        subtitle = stringResource(R.string.settings_storage_subtitle),
                        expanded = showStorage,
                        showDivider = false,
                        onClick = { showStorage = !showStorage }
                    )
                    SettingsExpandable(showStorage) { StorageSubSection() }
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconBackground = MayasTheme.GlowSky,
                        title = stringResource(R.string.settings_about_title),
                        subtitle = stringResource(R.string.settings_about_subtitle, versionName),
                        onClick = onNavigateToCredits
                    )
                    SettingsRow(
                        icon = Icons.Default.Gavel,
                        iconBackground = Color(0xFF8E8E93),
                        title = stringResource(R.string.settings_tos_title),
                        subtitle = stringResource(R.string.settings_tos_subtitle),
                        onClick = { uriHandler.openUri(MAYAS_TOS_URL) }
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.ExitToApp,
                        iconBackground = MayasTheme.ErrorRed,
                        title = stringResource(R.string.settings_logout_title),
                        subtitle = stringResource(R.string.settings_logout_subtitle),
                        showDivider = false,
                        titleColor = MayasTheme.ErrorRed,
                        showChevron = false,
                        onClick = {
                            vm.logout()
                            onBack()
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(28.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.settings_footer, versionName),
                        color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun UserProfileHeader(
    name: String,
    email: String,
    avatarUrl: String,
    isPremium: Boolean,
    onHeaderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onHeaderClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val ringModifier = if (isPremium) {
            Modifier
                .size(104.dp)
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(listOf(MayasTheme.GlowGold, MayasTheme.GlowOrange, MayasTheme.GlowGold)),
                    shape = CircleShape
                )
                .padding(5.dp)
        } else {
            Modifier
                .size(104.dp)
                .padding(4.dp)
        }
        Box(modifier = ringModifier, contentAlignment = Alignment.Center) {
            AsyncImage(
                model = avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=$name" },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                color = if (isPremium) MayasTheme.GlowGold else MayasTheme.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isPremium) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.WorkspacePremium, null, tint = MayasTheme.GlowGold, modifier = Modifier.size(20.dp))
            }
        }
        Text(text = email, color = MayasTheme.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MayasTheme.Accent.copy(alpha = 0.14f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SwapHoriz, null, tint = MayasTheme.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.settings_switch_account),
                color = MayasTheme.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MayasTheme.Surface),
        content = content
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    expanded: Boolean? = null,
    titleColor: Color = MayasTheme.TextPrimary,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            thickness = 0.5.dp,
            color = MayasTheme.TextSecondary.copy(alpha = 0.15f)
        )
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded == true) 90f else 0f,
        animationSpec = tween(200),
        label = "settingsChevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    subtitle,
                    color = MayasTheme.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MayasTheme.TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
            )
        }
    }
}

@Composable
fun SettingsExpandable(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitchSheet(
    sessions: List<UserSession>,
    currentUid: String?,
    currentDeviceCount: Int? = null,
    onSelect: (UserSession) -> Unit,
    onDeleteSession: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MayasTheme.TextSecondary.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.accounts_sheet_title),
                color = MayasTheme.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn {
                items(sessions) { session: UserSession ->
                    val isCurrent = session.uid == currentUid
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(session.name, color = MayasTheme.TextPrimary)
                                if (!session.username.isNullOrBlank()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("@${session.username}", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                                }
                            }
                        },
                        supportingContent = {
                            Column {
                                Text(session.email, color = MayasTheme.TextSecondary, fontSize = 12.sp)
                                if (session.createdAt > 0L) {
                                    Text(
                                        stringResource(R.string.on_mayas_since, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(session.createdAt))),
                                        color = MayasTheme.TextSecondary.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                if (isCurrent && currentDeviceCount != null) {
                                    Text(
                                        stringResource(R.string.devices_count, currentDeviceCount),
                                        color = MayasTheme.Accent.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            AsyncImage(
                                model = session.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=${session.name}" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, null, tint = MayasTheme.Accent)
                                } else {
                                    IconButton(onClick = { onDeleteSession(session.uid) }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(session) }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.add_account_action), color = MayasTheme.Accent) },
                        leadingContent = {
                            Icon(Icons.Default.Add, null, tint = MayasTheme.Accent)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onAddAccount() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = MayasTheme.TextSecondary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MayasTheme.Surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    color = MayasTheme.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun ChatSettingsSubSection(vm: AuthVM) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.chat_text_size, vm.fontSize.toInt()), color = MayasTheme.TextPrimary, fontSize = 14.sp)
        Slider(
            value = vm.fontSize,
            onValueChange = {
                vm.updateLocalSettings(
                    description = vm.userData["description"] ?: "",
                    theme = vm.appTheme,
                    fontSize = it
                )
            },
            valueRange = 12f..24f,
            colors = SliderDefaults.colors(
                thumbColor = MayasTheme.Accent,
                activeTrackColor = MayasTheme.Accent
            )
        )

        HorizontalDivider(
            color = MayasTheme.TextSecondary.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        val autoDownloadMedia = vm.userData["autoDownloadMedia"] != "false"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_autoload_media), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.chat_autoload_media_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = autoDownloadMedia,
                onCheckedChange = { enabled ->
                    vm.updateUserData("autoDownloadMedia", enabled.toString())
                },
                colors = SwitchDefaults.colors(checkedThumbColor = MayasTheme.Accent)
            )
        }

        Spacer(Modifier.height(8.dp))

        val sendByEnter = vm.userData["sendByEnter"] == "true"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_send_on_enter), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.chat_send_on_enter_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = sendByEnter,
                onCheckedChange = { enabled ->
                    vm.updateUserData("sendByEnter", enabled.toString())
                },
                colors = SwitchDefaults.colors(checkedThumbColor = MayasTheme.Accent)
            )
        }
    }
}

@Composable
fun InterfaceSettingsSubSection(
    onNavigateToHomeScreenLayout: () -> Unit,
    onNavigateToSidebarLayout: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EmojiStyleSelector()

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigateToHomeScreenLayout() }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Home, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.interface_home_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.interface_home_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(alpha = 0.3f))
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigateToSidebarLayout() }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Menu, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.interface_sidebar_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.interface_sidebar_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(alpha = 0.3f))
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Default.Info, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.interface_dpi_note),
                color = MayasTheme.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PrivacySubSection(vm: AuthVM, onNavigateToPremium: () -> Unit) {
    var showSelectorFor by remember { mutableStateOf<String?>(null) }

    val privacySettings = listOf(
        Triple("privacy_phone", stringResource(R.string.privacy_phone_label), Icons.Default.Phone),
        Triple("privacy_last_seen", stringResource(R.string.privacy_last_seen_label), Icons.Default.AccessTime),
        Triple("privacy_photo", stringResource(R.string.privacy_photo_label), Icons.Default.AccountCircle),
        Triple("privacy_groups", stringResource(R.string.privacy_groups_label), Icons.Default.Group)
    )

    if (showSelectorFor != null) {
        val currentKey = showSelectorFor!!
        val currentValue = vm.userData[currentKey] ?: "all"
        val title = privacySettings.find { it.first == currentKey }?.second ?: stringResource(R.string.privacy_setting_fallback)

        PrivacySelectorDialog(
            title = title,
            currentValue = currentValue,
            onSelect = { newValue ->
                vm.updateUserData(currentKey, newValue)
                showSelectorFor = null
            },
            onDismiss = { showSelectorFor = null }
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !vm.isPremium) { onNavigateToPremium() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.privacy_invisible_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.privacy_invisible_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = vm.isInvisible && vm.isPremium,
                onCheckedChange = { vm.updateInvisibleMode(it) },
                enabled = vm.isPremium,
                colors = SwitchDefaults.colors(checkedThumbColor = MayasTheme.GlowPurple)
            )
        }

        if (!vm.isPremium) {
            Text(
                stringResource(R.string.privacy_premium_only),
                color = MayasTheme.GlowGold,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        privacySettings.forEach { (key, label, icon) ->
            val value = vm.userData[key] ?: "all"
            val valueText = when (value) {
                "contacts" -> stringResource(R.string.audience_contacts)
                "none" -> stringResource(R.string.audience_nobody)
                else -> stringResource(R.string.audience_everyone)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSelectorFor = key },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                    Text(valueText, color = MayasTheme.Accent, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(0.3f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ActiveSessionsSubSection(vm: AuthVM) {
    var sessionToEnd by remember { mutableStateOf<AuthVM.RemoteSession?>(null) }

    sessionToEnd?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToEnd = null },
            title = { Text(if (session.isCurrent) stringResource(R.string.session_logout_question) else stringResource(R.string.session_end_question)) },
            text = {
                Text(
                    if (session.isCurrent) {
                        stringResource(R.string.session_logout_message)
                    } else {
                        stringResource(R.string.session_device_logout_message, session.deviceName)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.endSession(session.id)
                    sessionToEnd = null
                }) {
                    Text(if (session.isCurrent) stringResource(R.string.settings_logout_title) else stringResource(R.string.session_end_action), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToEnd = null }) { Text(stringResource(R.string.storage_cancel)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            stringResource(R.string.settings_sessions_title),
            color = MayasTheme.TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (vm.remoteSessions.isEmpty()) {
            Text(stringResource(R.string.session_loading), color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }

        vm.remoteSessions.forEach { session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sessionToEnd = session }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (session.platform) {
                        "android", "ios" -> Icons.Default.PhoneAndroid
                        "web" -> Icons.Default.Language
                        else -> Icons.Default.Computer
                    },
                    null,
                    tint = if (session.isCurrent) MayasTheme.Accent else MayasTheme.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(session.deviceName, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                        if (session.isCurrent) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.session_this_device),
                                color = MayasTheme.Accent,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MayasTheme.Accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        formatSessionLastActive(session.lastActiveAt),
                        color = MayasTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (!session.isCurrent) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (vm.remoteSessions.count { !it.isCurrent } > 0) {
            var showEndAllConfirm by remember { mutableStateOf(false) }

            if (showEndAllConfirm) {
                AlertDialog(
                    onDismissRequest = { showEndAllConfirm = false },
                    title = { Text(stringResource(R.string.session_end_others_question)) },
                    text = { Text(stringResource(R.string.session_end_others_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.endAllOtherSessions()
                            showEndAllConfirm = false
                        }) {
                            Text(stringResource(R.string.session_end_action), color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndAllConfirm = false }) { Text(stringResource(R.string.storage_cancel)) }
                    }
                )
            }

            TextButton(
                onClick = { showEndAllConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.session_end_others_action), color = Color.Red.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun formatSessionLastActive(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return stringResource(R.string.session_recently)
    val diffMs = System.currentTimeMillis() - timestamp.toDate().time
    val minutes = (diffMs / 60_000).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.session_active_now)
        minutes < 60 -> stringResource(R.string.session_seen_minutes, minutes)
        minutes < 24 * 60 -> stringResource(R.string.session_seen_hours, minutes / 60)
        else -> stringResource(R.string.session_seen_days, minutes / (24 * 60))
    }
}

@Composable
fun PrivacySelectorDialog(
    title: String,
    currentValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MayasTheme.TextPrimary) },
        text = {
            Column {
                val options = listOf(
                    "all" to stringResource(R.string.audience_everyone),
                    "contacts" to stringResource(R.string.audience_contacts),
                    "none" to stringResource(R.string.audience_nobody)
                )
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == value,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = MayasTheme.Accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = MayasTheme.TextPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.storage_cancel), color = MayasTheme.TextSecondary)
            }
        },
        containerColor = MayasTheme.Surface
    )
}

@Composable
fun SecuritySubSection(vm: AuthVM, onNavigateToAuth: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPassDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showEmailDialog) {
        var pass by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text(stringResource(R.string.change_email_title), color = MayasTheme.TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text(stringResource(R.string.new_email_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text(stringResource(R.string.current_password_label)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MayasTheme.ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        loading = true
                        vm.updateEmail(pass, newEmail) { err ->
                            loading = false
                            if (err == null) {
                                showEmailDialog = false
                                Toast.makeText(context, context.getString(R.string.change_email_toast), Toast.LENGTH_LONG).show()
                            } else {
                                error = err
                            }
                        }
                    },
                    enabled = pass.isNotBlank() && newEmail.contains("@") && !loading
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text(stringResource(R.string.update_action))
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    if (showPassDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPassDialog = false },
            title = { Text(stringResource(R.string.change_password_title), color = MayasTheme.TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text(stringResource(R.string.old_password_label)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text(stringResource(R.string.new_password_label)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MayasTheme.ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        loading = true
                        vm.updatePassword(oldPass, newPass) { err ->
                            loading = false
                            if (err == null) {
                                showPassDialog = false
                                Toast.makeText(context, context.getString(R.string.password_changed_toast), Toast.LENGTH_SHORT).show()
                            } else {
                                error = err
                            }
                        }
                    },
                    enabled = oldPass.isNotBlank() && newPass.length >= 6 && !loading
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text(stringResource(R.string.change_action))
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account_question), color = MayasTheme.ErrorRed) },
            text = { Text(stringResource(R.string.delete_account_message), color = MayasTheme.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteUserAccount(
                            onSuccess = { showDeleteDialog = false },
                            onError = {
                                showDeleteDialog = false
                                Toast.makeText(context, context.getString(R.string.delete_account_failed), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.delete_action), color = MayasTheme.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.storage_cancel), color = MayasTheme.TextPrimary)
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (vm.isEmailVerified) Modifier
                    else Modifier.clickable { vm.openEmailVerification(); onNavigateToAuth() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (vm.isEmailVerified) Icons.Default.CheckCircle else Icons.Default.Email,
                null,
                tint = if (vm.isEmailVerified) MayasTheme.TextSecondary.copy(alpha = 0.5f) else MayasTheme.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (vm.isEmailVerified) stringResource(R.string.email_verified) else stringResource(R.string.email_verify_action),
                    color = if (vm.isEmailVerified) MayasTheme.TextSecondary.copy(alpha = 0.6f) else MayasTheme.TextPrimary,
                    fontSize = 14.sp
                )
                if (!vm.isEmailVerified) {
                    Text(stringResource(R.string.email_verify_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEmailDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Email, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.change_email_action), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.change_email_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPassDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Password, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.change_password_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.change_password_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    vm.sendPasswordReset { error ->
                        val msg = error ?: context.getString(R.string.reset_link_sent)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LockReset, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.reset_password_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.reset_password_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PersonOff, null, tint = MayasTheme.ErrorRed, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.delete_account_title), color = MayasTheme.ErrorRed, fontSize = 14.sp)
                Text(stringResource(R.string.delete_account_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StorageSubSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var breakdown by remember { mutableStateOf<CacheBreakdown?>(null) }
    var isClearing by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearOfflineDialog by remember { mutableStateOf(false) }
    val clearedText = stringResource(R.string.storage_cleared)

    suspend fun refresh() {
        breakdown = AppCacheManager.breakdown(context)
    }

    LaunchedEffect(Unit) { refresh() }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClearing) showClearCacheDialog = false },
            title = { Text(stringResource(R.string.storage_clear_confirm_title), color = MayasTheme.TextPrimary) },
            text = { Text(stringResource(R.string.storage_clear_confirm_text), color = MayasTheme.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isClearing = true
                        scope.launch {
                            AppCacheManager.clearAll(context)
                            refresh()
                            isClearing = false
                            showClearCacheDialog = false
                            Toast.makeText(context, clearedText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isClearing
                ) {
                    if (isClearing) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else Text(stringResource(R.string.storage_clear_action), color = MayasTheme.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }, enabled = !isClearing) {
                    Text(stringResource(R.string.storage_cancel), color = MayasTheme.TextSecondary)
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    if (showClearOfflineDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClearing) showClearOfflineDialog = false },
            title = { Text(stringResource(R.string.storage_clear_offline_title), color = MayasTheme.TextPrimary) },
            text = { Text(stringResource(R.string.storage_clear_offline_text), color = MayasTheme.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isClearing = true
                        scope.launch {
                            AppCacheManager.clearOfflineData(context)
                            refresh()
                            isClearing = false
                            showClearOfflineDialog = false
                            Toast.makeText(context, clearedText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isClearing
                ) {
                    if (isClearing) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else Text(stringResource(R.string.storage_clear_action), color = MayasTheme.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearOfflineDialog = false }, enabled = !isClearing) {
                    Text(stringResource(R.string.storage_cancel), color = MayasTheme.TextSecondary)
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val current = breakdown

        if (current == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.storage_calculating), color = MayasTheme.TextSecondary, fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.storage_total), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                    Text(
                        AppCacheManager.format(context, current.totalBytes),
                        color = MayasTheme.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                val total = current.totalBytes.coerceAtLeast(1L).toFloat()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MayasTheme.TextSecondary.copy(alpha = 0.15f))
                ) {
                    listOf(
                        current.imagesBytes to MayasTheme.Accent,
                        current.mediaBytes to MayasTheme.GlowSky,
                        current.offlineDataBytes to MayasTheme.GlowLime,
                        current.firestoreBytes to MayasTheme.GlowGold,
                        current.tempBytes to MayasTheme.ErrorRed
                    ).forEach { (bytes, color) ->
                        if (bytes > 0L) {
                            Box(
                                modifier = Modifier
                                    .weight(bytes / total)
                                    .fillMaxHeight()
                                    .background(color)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

            StorageCategoryRow(
                icon = Icons.Default.Image,
                tint = MayasTheme.Accent,
                title = stringResource(R.string.storage_images),
                subtitle = stringResource(
                    R.string.storage_files_count,
                    current.imagesCount,
                    AppCacheManager.format(context, current.imagesBytes)
                )
            )
            StorageCategoryRow(
                icon = Icons.Default.PermMedia,
                tint = MayasTheme.GlowSky,
                title = stringResource(R.string.storage_media),
                subtitle = stringResource(
                    R.string.storage_files_count,
                    current.mediaCount,
                    AppCacheManager.format(context, current.mediaBytes)
                ),
                description = stringResource(R.string.storage_media_desc)
            )
            StorageCategoryRow(
                icon = Icons.Default.Storage,
                tint = MayasTheme.GlowLime,
                title = stringResource(R.string.storage_offline_data),
                subtitle = AppCacheManager.format(context, current.offlineDataBytes),
                description = stringResource(R.string.storage_offline_data_desc)
            )
            StorageCategoryRow(
                icon = Icons.Default.Sync,
                tint = MayasTheme.GlowGold,
                title = stringResource(R.string.storage_server_copy),
                subtitle = AppCacheManager.format(context, current.firestoreBytes),
                description = stringResource(R.string.storage_server_copy_desc)
            )
            StorageCategoryRow(
                icon = Icons.Default.Schedule,
                tint = MayasTheme.ErrorRed,
                title = stringResource(R.string.storage_temp),
                subtitle = stringResource(
                    R.string.storage_files_count,
                    current.tempCount,
                    AppCacheManager.format(context, current.tempBytes)
                ),
                description = stringResource(R.string.storage_temp_desc)
            )

            HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.storage_hint),
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

            CachePolicySettings(
                onPolicyChanged = {
                    scope.launch {
                        MediaFileCache.applyPolicy(context)
                        refresh()
                    }
                }
            )

            HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))

            val canClear = current.clearableBytes > 0L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = canClear && !isClearing) { showClearCacheDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    null,
                    tint = if (canClear) MayasTheme.ErrorRed else MayasTheme.TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.storage_clear_cache),
                        color = if (canClear) MayasTheme.ErrorRed else MayasTheme.TextSecondary.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                    Text(stringResource(R.string.storage_clear_cache_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                }
            }

            val canClearOffline = current.offlineDataBytes > 0L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = canClearOffline && !isClearing) { showClearOfflineDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage,
                    null,
                    tint = if (canClearOffline) MayasTheme.ErrorRed else MayasTheme.TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.storage_clear_offline),
                        color = if (canClearOffline) MayasTheme.ErrorRed else MayasTheme.TextSecondary.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                    Text(stringResource(R.string.storage_clear_offline_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CachePolicySettings(onPolicyChanged: () -> Unit) {
    val context = LocalContext.current
    var maxBytes by remember { mutableStateOf(MediaCachePrefs.maxBytes(context)) }
    var keepDays by remember { mutableStateOf(MediaCachePrefs.keepDays(context)) }
    var wifiOnly by remember { mutableStateOf(MediaCachePrefs.wifiOnly(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.cache_limit_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaCachePrefs.limitOptions.forEach { option ->
                    val label = when (option) {
                        MediaCachePrefs.UNLIMITED -> stringResource(R.string.cache_limit_unlimited)
                        else -> stringResource(
                            when {
                                option <= 1024L * 1024L * 1024L -> R.string.cache_limit_1gb
                                option <= 2L * 1024L * 1024L * 1024L -> R.string.cache_limit_2gb
                                else -> R.string.cache_limit_5gb
                            }
                        )
                    }
                    CacheChoiceChip(label, maxBytes == option) {
                        maxBytes = option
                        MediaCachePrefs.setMaxBytes(context, option)
                        onPolicyChanged()
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.cache_keep_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaCachePrefs.keepOptions.forEach { option ->
                    val label = stringResource(
                        when (option) {
                            3 -> R.string.cache_keep_3d
                            7 -> R.string.cache_keep_7d
                            30 -> R.string.cache_keep_30d
                            else -> R.string.cache_keep_forever
                        }
                    )
                    CacheChoiceChip(label, keepDays == option) {
                        keepDays = option
                        MediaCachePrefs.setKeepDays(context, option)
                        onPolicyChanged()
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cache_wifi_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.cache_wifi_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = wifiOnly,
                onCheckedChange = {
                    wifiOnly = it
                    MediaCachePrefs.setWifiOnly(context, it)
                }
            )
        }
    }
}

@Composable
private fun CacheChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MayasTheme.Accent.copy(alpha = 0.2f) else MayasTheme.TextSecondary.copy(alpha = 0.1f))
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) MayasTheme.Accent else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) MayasTheme.Accent else MayasTheme.TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun StorageCategoryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    description: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MayasTheme.TextPrimary, fontSize = 14.sp)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
            if (description != null) {
                Text(description, color = MayasTheme.TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmojiStyleSelector() {
    val context = LocalContext.current
    EmojiStyleState.ensureInit(context)
    val current = EmojiStyleState.style

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(Icons.Default.EmojiEmotions, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.emoji_style_title), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text(stringResource(R.string.emoji_style_subtitle), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                EmojiStyle.STANDARD to R.string.emoji_style_standard,
                EmojiStyle.MAYAS to R.string.emoji_style_mayas
            ).forEach { (style, label) ->
                val selected = current == style
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MayasTheme.Accent.copy(alpha = 0.18f) else MayasTheme.SurfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = if (selected) 1.5.dp else 0.dp,
                            color = if (selected) MayasTheme.Accent else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { EmojiStyleState.set(context, style) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("😍", "🔥", "❤️", "👍").forEach { emoji ->
                            EmojiGlyphStyled(emoji, fontSize = 20.sp, style = style)
                        }
                    }
                    Text(
                        stringResource(label),
                        color = if (selected) MayasTheme.Accent else MayasTheme.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

const val MAYAS_TOS_URL = "https://dan1eidt.github.io/mayas-site/tos.html"

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = MayasTheme.Accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}