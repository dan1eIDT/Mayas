package com.dan1eidtj.mayas.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

private const val MAYAS_TOS_URL = "https://dan1eidt.github.io/mayas-site/tos.html"

@Composable
fun AuthScreen(vm: AuthVM, onAuthSuccess: () -> Unit = {}) {
    if (vm.showVerifyScreen) {
        VerifyEmailScreen(vm, onAuthSuccess)
        return
    }

    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    var passwordVisible by remember { mutableStateOf(false) }
    var tosAccepted by remember { mutableStateOf(false) }

    val nameFocus = remember { FocusRequester() }
    val usernameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passFocus = remember { FocusRequester() }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
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
                imageVector = Icons.Filled.Chat,
                contentDescription = null,
                tint = MayasTheme.RedAccent,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text("Маяс", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        Spacer(Modifier.height(4.dp))


        AnimatedContent(
            targetState = vm.isLoginMode,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "subtitle"
        ) { isLogin ->
            Text(
                text = if (isLogin) "С возвращением!" else "Создайте новый аккаунт",
                fontSize = 14.sp,
                color = MayasTheme.TextGrey
            )
        }

        Spacer(Modifier.height(28.dp))


        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {


                AnimatedVisibility(
                    visible = !vm.isLoginMode,
                    enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                ) {
                    Column {
                        OutlinedTextField(
                            value = vm.nameInput,
                            onValueChange = vm::onNameChange,
                            label = { Text("Имя", color = MayasTheme.TextGrey) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocus),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { usernameFocus.requestFocus() })
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = vm.usernameInput,
                            onValueChange = vm::onUsernameChange,
                            label = { Text("Юзернейм (без @)", color = MayasTheme.TextGrey) },
                            leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(usernameFocus),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { emailFocus.requestFocus() })
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }


                OutlinedTextField(
                    value = vm.emailInput,
                    onValueChange = vm::onEmailChange,
                    label = { Text("Email", color = MayasTheme.TextGrey) },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocus),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { passFocus.requestFocus() })
                )

                Spacer(Modifier.height(12.dp))


                OutlinedTextField(
                    value = vm.passInput,
                    onValueChange = vm::onPassChange,
                    label = { Text("Пароль", color = MayasTheme.TextGrey) },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passFocus),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        vm.handleAuthAction(onAuthSuccess)
                    })
                )


                AnimatedVisibility(
                    visible = vm.isLoginMode,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            resetEmailInput = vm.emailInput
                            showResetDialog = true
                        }) {
                            Text(
                                text = "Забыли пароль?",
                                color = MayasTheme.TextGrey,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = vm.authError != null,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 2 }
                ) {
                    Text(
                        text = vm.authError ?: "",
                        color = MayasTheme.ErrorRed,
                        modifier = Modifier.padding(top = 12.dp),
                        fontSize = 14.sp
                    )
                }

                AnimatedVisibility(
                    visible = vm.resetMessage != null,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 2 }
                ) {
                    Text(
                        text = vm.resetMessage ?: "",
                        color = MayasTheme.RedAccent,
                        modifier = Modifier.padding(top = 12.dp),
                        fontSize = 14.sp
                    )
                }

                AnimatedVisibility(
                    visible = !vm.isLoginMode,
                    enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = tosAccepted,
                                onCheckedChange = { tosAccepted = it },
                                colors = CheckboxDefaults.colors(checkedColor = MayasTheme.RedAccent)
                            )

                            val tosText = buildAnnotatedString {
                                append("Я прочёл(а) ")
                                pushStringAnnotation(tag = "TOS", annotation = MAYAS_TOS_URL)
                                withStyle(
                                    SpanStyle(
                                        color = MayasTheme.RedAccent,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Пользовательское соглашение Маяс")
                                }
                                pop()
                            }

                            ClickableText(
                                text = tosText,
                                style = TextStyle(fontSize = 13.sp, color = MayasTheme.TextGrey, lineHeight = 18.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 14.dp),
                                onClick = { offset ->
                                    tosText.getStringAnnotations(tag = "TOS", start = offset, end = offset)
                                        .firstOrNull()
                                        ?.let { uriHandler.openUri(it.item) }
                                }
                            )
                        }

                        Text(
                            text = "Продолжая, вы подтверждаете, что у вас есть согласие законного представителя на использование приложения, либо что вам уже исполнилось 14 лет.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MayasTheme.TextGrey,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))


                Button(
                    onClick = {
                        focusManager.clearFocus()
                        vm.handleAuthAction(onAuthSuccess)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.RedAccent),
                    enabled = !vm.isLoading && (vm.isLoginMode || tosAccepted)
                ) {
                    if (vm.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        AnimatedContent(
                            targetState = vm.isLoginMode,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                            label = "buttonText"
                        ) { isLogin ->
                            Text(
                                text = if (isLogin) "Войти" else "Зарегистрироваться",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))


        TextButton(onClick = { vm.toggleAuthMode() }) {
            AnimatedContent(
                targetState = vm.isLoginMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "toggleText"
            ) { isLogin ->
                Text(
                    text = if (isLogin) "Нет аккаунта? Создать" else "Уже есть? Войти",
                    color = MayasTheme.TextGrey
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сброс пароля") },
            text = {
                Column {
                    Text(
                        text = "Введите Email, на который придёт письмо со ссылкой для сброса пароля.",
                        color = MayasTheme.TextGrey,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("Email", color = MayasTheme.TextGrey) },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
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
                    Text("Отправить", color = MayasTheme.RedAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена", color = MayasTheme.TextGrey)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun VerifyEmailScreen(vm: AuthVM, onVerified: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            "Подтверди почту",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Мы отправили письмо на ${vm.user?.email ?: "твой email"}. " +
                    "Перейди по ссылке в письме, потом нажми \"Я подтвердил\".",
            fontSize = 14.sp,
            color = MayasTheme.TextGrey,
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

        Button(
            onClick = { vm.refreshVerificationStatus(onVerified) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.RedAccent),
            enabled = !vm.isVerifyLoading
        ) {
            if (vm.isVerifyLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Я подтвердил", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = { vm.resendVerificationEmail() },
            enabled = !vm.isVerifyLoading
        ) {
            Text("Отправить письмо ещё раз", color = MayasTheme.TextGrey)
        }

        TextButton(onClick = { vm.cancelVerification() }) {
            Text("Выйти из аккаунта", color = MayasTheme.TextGrey, fontSize = 13.sp)
        }
    }
}