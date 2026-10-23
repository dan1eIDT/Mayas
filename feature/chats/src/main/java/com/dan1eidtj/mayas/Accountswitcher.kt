/* Copyright (C) 2026 ProjectIDT */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.chats.R
import com.dan1eidtj.data.UserSession
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Stable
class AccountSwitcherState(
    private val vm: AuthVM,
    private val appContext: Context,
    private val navigateToAuth: () -> Unit
) {
    var sheetVisible by mutableStateOf(false)
    var targetSession by mutableStateOf<UserSession?>(null)
    var passwordDialogVisible by mutableStateOf(false)
    var password by mutableStateOf("")
    var error by mutableStateOf<String?>(null)

    val sessions: List<UserSession> get() = vm.activeSessions
    val currentUid: String? get() = vm.user?.uid
    val isLoading: Boolean get() = vm.isLoading

    fun openSheet() {
        sheetVisible = true
    }

    fun closeSheet() {
        sheetVisible = false
    }

    fun select(session: UserSession) {
        if (session.uid == vm.user?.uid) return
        targetSession = session
        sheetVisible = false
        vm.switchAccountByToken(
            targetUid = session.uid,
            onPasswordRequired = {
                password = ""
                error = null
                passwordDialogVisible = true
            },
            onError = { message ->
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            },
            onSuccess = {
                passwordDialogVisible = false
                password = ""
                error = null
            }
        )
    }

    fun confirmPassword(targetMissingText: String) {
        val account = targetSession
        if (account == null) {
            error = targetMissingText
            return
        }
        vm.switchAccount(
            account.email,
            password,
            onError = { error = it }
        ) {
            passwordDialogVisible = false
            password = ""
        }
    }

    fun dismissPasswordDialog() {
        passwordDialogVisible = false
        password = ""
        error = null
    }

    fun addAccount() {
        sheetVisible = false
        vm.addNewAccount(
            onNavigateToAuth = navigateToAuth,
            onLimitReached = {
                Toast.makeText(
                    appContext,
                    appContext.getString(com.dan1eidtj.auth.R.string.auth_account_limit_reached),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    fun removeSession(uid: String) {
        vm.removeSession(uid)
    }
}

@Composable
fun rememberAccountSwitcher(vm: AuthVM, onNavigateToAuth: () -> Unit): AccountSwitcherState {
    val appContext = LocalContext.current.applicationContext
    val navigateState = rememberUpdatedState(onNavigateToAuth)
    return remember(vm, appContext) {
        AccountSwitcherState(vm, appContext) { navigateState.value() }
    }
}

@Composable
fun rememberFirebaseUser(): State<FirebaseUser?> {
    val auth = remember { FirebaseAuth.getInstance() }
    val userState = remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { userState.value = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    return userState
}

@Composable
fun AccountSwitcherHost(state: AccountSwitcherState) {
    if (state.passwordDialogVisible && state.targetSession != null) {
        val targetMissingText = stringResource(R.string.chatlist_acc_target_missing)
        AlertDialog(
            onDismissRequest = { state.dismissPasswordDialog() },
            title = {
                Text(stringResource(R.string.chatlist_acc_switch_title), color = MayasTheme.TextPrimary)
            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.chatlist_acc_password_prompt, state.targetSession?.email.orEmpty()),
                        color = MayasTheme.TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = {
                            state.password = it
                            state.error = null
                        },
                        label = { Text(stringResource(R.string.chatlist_acc_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = state.error != null,
                        supportingText = { state.error?.let { Text(it, color = MayasTheme.ErrorRed) } },
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
                    onClick = { state.confirmPassword(targetMissingText) },
                    enabled = state.password.isNotBlank() && !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text(stringResource(R.string.chatlist_acc_sign_in))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { state.dismissPasswordDialog() }) {
                    Text(stringResource(R.string.chatlist_acc_cancel), color = MayasTheme.TextSecondary)
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    if (state.sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { state.closeSheet() },
            containerColor = MayasTheme.Surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MayasTheme.TextSecondary.copy(alpha = 0.3f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.chatlist_acc_title),
                    color = MayasTheme.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn {
                    items(state.sessions) { session: UserSession ->
                        val isCurrent = session.uid == state.currentUid
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(session.name, color = MayasTheme.TextPrimary)
                                    if (!session.username.isNullOrBlank()) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "@${session.username}",
                                            color = MayasTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            supportingContent = {
                                Text(session.email, color = MayasTheme.TextSecondary, fontSize = 12.sp)
                            },
                            leadingContent = { SessionAvatar(session = session, size = 40.dp) },
                            trailingContent = {
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, null, tint = MayasTheme.Accent)
                                } else {
                                    IconButton(onClick = { state.removeSession(session.uid) }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { state.select(session) }
                        )
                    }
                    item {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.chatlist_acc_add), color = MayasTheme.Accent)
                            },
                            leadingContent = {
                                Icon(Icons.Default.Add, null, tint = MayasTheme.Accent)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { state.addAccount() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionAvatar(session: UserSession, size: Dp) {
    val url = session.avatarUrl.orEmpty()
    UserAvatarView(
        avatarUrl = url.ifBlank { null },
        useCustomAvatar = url.isNotBlank(),
        profileIcon = "ghost",
        profileGlow = "purple",
        isPremium = false,
        frameType = "none",
        size = size
    )
}

@Composable
fun DrawerAccountsList(
    visible: Boolean,
    sessions: List<UserSession>,
    currentUid: String?,
    onSelect: (UserSession) -> Unit,
    onAddAccount: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MayasTheme.Surface.copy(alpha = 0.6f))
                .padding(vertical = 4.dp)
        ) {
            sessions.forEach { session ->
                val isCurrent = session.uid == currentUid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(enabled = !isCurrent) { onSelect(session) }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SessionAvatar(session = session, size = 36.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.name,
                            color = MayasTheme.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!session.username.isNullOrBlank()) {
                            Text(
                                text = "@${session.username}",
                                color = MayasTheme.TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MayasTheme.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onAddAccount() }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MayasTheme.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.chatlist_acc_add),
                    color = MayasTheme.Accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}