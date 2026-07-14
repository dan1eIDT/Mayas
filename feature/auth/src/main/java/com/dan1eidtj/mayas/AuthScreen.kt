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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

@Composable
fun AuthScreen(vm: AuthVM, onAuthSuccess: () -> Unit = {}) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    val nameFocus = remember { FocusRequester() }
    val usernameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passFocus = remember { FocusRequester() }

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
        // Логотип-бейдж
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

        // Подзаголовок меняется вместе с режимом, с плавной анимацией
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

        // Карточка с формой
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Поля только для регистрации — плавно раскрываются/сворачиваются
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

                // Основное поле: Email
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

                // Основное поле: Пароль
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

                // Отображение ошибки — с плавным появлением
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

                Spacer(Modifier.height(20.dp))

                // Кнопка авторизации
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
                    enabled = !vm.isLoading
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

        // Кнопка переключения режима
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
}