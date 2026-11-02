/* Copyright (C) 2026 dan1eIDT */

package com.dan1eidtj.mayas.feature.auth

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dan1eidtj.auth.R
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

private const val MAYAS_TOS_URL = "https://dan1eidt.github.io/mayas-site/tos.html"

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MayasTheme.RedAccent,
    unfocusedBorderColor = MayasTheme.TextGrey,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MayasTheme.RedAccent,
    unfocusedLeadingIconColor = MayasTheme.TextGrey,
    focusedTrailingIconColor = MayasTheme.RedAccent,
    unfocusedTrailingIconColor = MayasTheme.TextGrey
)

@Composable
fun AuthScreen(vm: AuthVM, onAuthSuccess: () -> Unit = {}, onReturnToAccount: () -> Unit = {}) {
    if (vm.showVerifyScreen) {
        VerifyEmailScreen(vm, onAuthSuccess)
        return
    }

    BackHandler(enabled = vm.isAddingAccount || vm.authStep != AuthStep.WELCOME) {
        if (!vm.stepBack()) {
            vm.cancelAddAccount()
            onReturnToAccount()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = vm.authStep,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(250)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(200)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(150)))
            },
            label = "authStep"
        ) { step ->
            when (step) {
                AuthStep.WELCOME -> WelcomeStep(vm)
                AuthStep.LOGIN -> LoginStep(vm, onAuthSuccess)
                AuthStep.REGISTER_EMAIL -> RegisterEmailStep(vm)
                AuthStep.PROFILE -> ProfileStep(vm, onAuthSuccess)
            }
        }

        AnimatedVisibility(
            visible = vm.isAddingAccount && (vm.authStep == AuthStep.LOGIN || vm.authStep == AuthStep.REGISTER_EMAIL),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            TextButton(
                onClick = {
                    vm.cancelAddAccount()
                    onReturnToAccount()
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MayasTheme.RedAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auth_return_to_account, vm.returnAccountName),
                    color = MayasTheme.RedAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StepLayout(
    icon: ImageVector,
    iconBoxSize: Dp = 80.dp,
    title: String,
    subtitle: String?,
    error: String?,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .background(MayasTheme.RedAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MayasTheme.RedAccent,
                    modifier = Modifier.size(iconBoxSize * 0.45f)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MayasTheme.TextGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            content()
            AnimatedVisibility(visible = error != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                Text(
                    text = error ?: "",
                    color = MayasTheme.ErrorRed,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = bottom
        )
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.RedAccent),
        enabled = enabled && !loading
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun TosRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = checked,
                onCheckedChange = onChange,
                colors = CheckboxDefaults.colors(checkedColor = MayasTheme.RedAccent)
            )
            val prefix = stringResource(R.string.tos_prefix)
            val link = stringResource(R.string.tos_link_text)
            val text = buildAnnotatedString {
                append(prefix)
                pushStringAnnotation(tag = "TOS", annotation = MAYAS_TOS_URL)
                withStyle(SpanStyle(color = MayasTheme.RedAccent, textDecoration = TextDecoration.Underline)) {
                    append(link)
                }
                pop()
            }
            ClickableText(
                text = text,
                style = TextStyle(fontSize = 13.sp, color = MayasTheme.TextGrey, lineHeight = 18.sp),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 14.dp),
                onClick = { offset ->
                    text.getStringAnnotations(tag = "TOS", start = offset, end = offset)
                        .firstOrNull()
                        ?.let { uriHandler.openUri(it.item) }
                }
            )
        }
        Text(
            text = stringResource(R.string.age_consent_notice),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MayasTheme.TextGrey,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
        )
    }
}

@Composable
private fun WelcomeStep(vm: AuthVM) {
    StepLayout(
        icon = Icons.Filled.Chat,
        iconBoxSize = 128.dp,
        title = stringResource(R.string.app_title_mixed_case),
        subtitle = stringResource(R.string.onb_welcome_subtitle),
        error = null,
        bottom = {
            PrimaryButton(stringResource(R.string.onb_start_messaging), enabled = true, loading = false) {
                vm.startRegistration()
            }
            TextButton(onClick = { vm.startLogin() }) {
                Text(stringResource(R.string.already_have_account), color = MayasTheme.TextGrey)
            }
        }
    ) {}
}

@Composable
private fun LoginStep(vm: AuthVM, onAuthSuccess: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val passFocus = remember { FocusRequester() }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    val fieldColors = authFieldColors()

    StepLayout(
        icon = Icons.Filled.Chat,
        title = stringResource(R.string.app_title_mixed_case),
        subtitle = stringResource(R.string.welcome_back),
        error = vm.authError,
        bottom = {
            PrimaryButton(stringResource(R.string.login), enabled = true, loading = vm.isLoading) {
                focusManager.clearFocus()
                vm.handleAuthAction(onAuthSuccess)
            }
            TextButton(onClick = { vm.toggleAuthMode() }) {
                Text(stringResource(R.string.no_account_create), color = MayasTheme.TextGrey)
            }
        }
    ) {
        OutlinedTextField(
            value = vm.emailInput,
            onValueChange = vm::onEmailChange,
            label = { Text(stringResource(R.string.email_label), color = MayasTheme.TextGrey) },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passFocus.requestFocus() })
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = vm.passInput,
            onValueChange = vm::onPassChange,
            label = { Text(stringResource(R.string.password), color = MayasTheme.TextGrey) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passFocus),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                vm.handleAuthAction(onAuthSuccess)
            })
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                resetEmailInput = vm.emailInput
                showResetDialog = true
            }) {
                Text(stringResource(R.string.forgot_password), color = MayasTheme.TextGrey, fontSize = 13.sp)
            }
        }
        AnimatedVisibility(visible = vm.resetMessage != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            Text(
                text = vm.resetMessage ?: "",
                color = MayasTheme.RedAccent,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.password_reset)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.reset_password_desc), color = MayasTheme.TextGrey, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text(stringResource(R.string.email_label), color = MayasTheme.TextGrey) },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            vm.sendForgotPasswordEmail(resetEmailInput)
                            showResetDialog = false
                        })
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.sendForgotPasswordEmail(resetEmailInput)
                        showResetDialog = false
                    },
                    enabled = !vm.isResetLoading
                ) {
                    Text(stringResource(com.dan1eidtj.mayas.ui.R.string.send), color = MayasTheme.RedAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = MayasTheme.TextGrey)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun RegisterEmailStep(vm: AuthVM) {
    val focusManager = LocalFocusManager.current
    val passFocus = remember { FocusRequester() }
    var passwordVisible by remember { mutableStateOf(false) }
    var tosAccepted by rememberSaveable { mutableStateOf(false) }
    val fieldColors = authFieldColors()

    StepLayout(
        icon = Icons.Filled.Email,
        title = stringResource(R.string.onb_register_email_title),
        subtitle = stringResource(R.string.onb_register_email_desc),
        error = vm.authError,
        bottom = {
            PrimaryButton(stringResource(R.string.onb_next), enabled = tosAccepted, loading = false) {
                focusManager.clearFocus()
                vm.continueToProfile()
            }
            TextButton(onClick = { vm.toggleAuthMode() }) {
                Text(stringResource(R.string.already_have_account), color = MayasTheme.TextGrey)
            }
        }
    ) {
        OutlinedTextField(
            value = vm.emailInput,
            onValueChange = vm::onEmailChange,
            label = { Text(stringResource(R.string.email_label), color = MayasTheme.TextGrey) },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passFocus.requestFocus() })
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = vm.passInput,
            onValueChange = vm::onPassChange,
            label = { Text(stringResource(R.string.password), color = MayasTheme.TextGrey) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passFocus),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (tosAccepted) vm.continueToProfile()
            })
        )
        Spacer(Modifier.height(12.dp))
        TosRow(checked = tosAccepted, onChange = { tosAccepted = it })
    }
}

@Composable
private fun ProfileStep(vm: AuthVM, onAuthSuccess: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val usernameFocus = remember { FocusRequester() }
    val fieldColors = authFieldColors()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.onAvatarPicked(uri)
    }
    val submit = {
        focusManager.clearFocus()
        vm.handleAuthAction(onAuthSuccess)
    }

    StepLayout(
        icon = Icons.Filled.Person,
        iconBoxSize = 0.dp,
        title = stringResource(R.string.onb_profile_title),
        subtitle = stringResource(R.string.onb_profile_desc),
        error = vm.authError,
        bottom = {
            PrimaryButton(
                stringResource(R.string.onb_done),
                enabled = vm.nameInput.isNotBlank() && vm.usernameStatus != UsernameStatus.TAKEN && vm.usernameStatus != UsernameStatus.INVALID,
                loading = vm.isLoading
            ) { submit() }
        }
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MayasTheme.RedAccent.copy(alpha = 0.15f))
                .clickable { picker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (vm.avatarUri != null) {
                AsyncImage(
                    model = vm.avatarUri,
                    contentDescription = stringResource(R.string.onb_add_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = stringResource(R.string.onb_add_photo),
                    tint = MayasTheme.RedAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = vm.nameInput,
            onValueChange = vm::onNameChange,
            label = { Text(stringResource(R.string.name_label), color = MayasTheme.TextGrey) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { usernameFocus.requestFocus() })
        )
        Spacer(Modifier.height(12.dp))
        val statusColor: Color
        val statusText: String
        when (vm.usernameStatus) {
            UsernameStatus.IDLE -> {
                statusColor = MayasTheme.TextGrey
                statusText = stringResource(R.string.onb_username_hint)
            }
            UsernameStatus.INVALID -> {
                statusColor = MayasTheme.ErrorRed
                statusText = stringResource(R.string.onb_username_hint)
            }
            UsernameStatus.CHECKING -> {
                statusColor = MayasTheme.TextGrey
                statusText = stringResource(R.string.onb_username_checking)
            }
            UsernameStatus.FREE -> {
                statusColor = Color(0xFF4CAF50)
                statusText = stringResource(R.string.onb_username_free, vm.usernameInput)
            }
            UsernameStatus.TAKEN -> {
                statusColor = MayasTheme.ErrorRed
                statusText = stringResource(R.string.onb_username_taken)
            }
        }
        OutlinedTextField(
            value = vm.usernameInput,
            onValueChange = vm::onUsernameChange,
            label = { Text(stringResource(R.string.username_no_at), color = MayasTheme.TextGrey) },
            leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
            supportingText = { Text(statusText, color = statusColor, fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(usernameFocus),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
    }
}

@Composable
fun VerifyEmailScreen(vm: AuthVM, onVerified: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MayasTheme.RedAccent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                tint = MayasTheme.RedAccent,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            stringResource(R.string.verify_email_screen_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.verify_email_instruction,
                vm.user?.email ?: stringResource(R.string.verify_email_fallback)
            ),
            fontSize = 14.sp,
            color = MayasTheme.TextGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(
            visible = vm.verifyMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            Text(
                text = vm.verifyMessage ?: "",
                color = MayasTheme.RedAccent,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        PrimaryButton(stringResource(R.string.i_confirmed_button), enabled = true, loading = vm.isVerifyLoading) {
            vm.refreshVerificationStatus(onVerified)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { vm.skipEmailVerification(onVerified) },
            enabled = !vm.isVerifyLoading
        ) {
            Text(stringResource(R.string.onb_verify_later), color = MayasTheme.RedAccent, fontWeight = FontWeight.SemiBold)
        }

        TextButton(
            onClick = { vm.resendVerificationEmail() },
            enabled = !vm.isVerifyLoading
        ) {
            Text(stringResource(R.string.resend_email_button), color = MayasTheme.TextGrey)
        }

        TextButton(onClick = { vm.cancelVerification() }) {
            Text(stringResource(R.string.logout_from_account_button), color = MayasTheme.TextGrey, fontSize = 13.sp)
        }
    }
}
