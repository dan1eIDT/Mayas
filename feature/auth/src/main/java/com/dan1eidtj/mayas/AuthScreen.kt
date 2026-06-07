package com.dan1eidtj.mayas.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

@Composable
fun AuthScreen(vm: AuthVM, onAuthSuccess: () -> Unit = {}) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MayasTheme.RedAccent,
        unfocusedBorderColor = MayasTheme.TextGrey,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        cursorColor = MaterialTheme.colorScheme.primary
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
        Text("Маяс", fontSize = 36.sp, color = MaterialTheme.colorScheme.onBackground)

        Spacer(Modifier.height(24.dp))

        // Поля только для регистрации
        if (!vm.isLoginMode) {
            OutlinedTextField(
                value = vm.nameInput,
                onValueChange = vm::onNameChange,
                label = { Text("Имя", color = MayasTheme.TextGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = vm.usernameInput,
                onValueChange = vm::onUsernameChange,
                label = { Text("Юзернейм (без @)", color = MayasTheme.TextGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(Modifier.height(12.dp))
        }

        // Основное поле: Email
        OutlinedTextField(
            value = vm.emailInput,
            onValueChange = vm::onEmailChange,
            label = { Text("Email", color = MayasTheme.TextGrey) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(12.dp))

        // Основное поле: Пароль
        OutlinedTextField(
            value = vm.passInput,
            onValueChange = vm::onPassChange,
            label = { Text("Пароль", color = MayasTheme.TextGrey) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        // Отображение ошибки
        vm.authError?.let { errorText ->
            Text(
                text = errorText,
                color = MayasTheme.ErrorRed,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        // Кнопка авторизации
        Button(
            onClick = { vm.handleAuthAction(onAuthSuccess) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
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
                Text(
                    text = if (vm.isLoginMode) "Войти" else "Зарегистрироваться",
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Кнопка переключения режима
        TextButton(onClick = { vm.toggleAuthMode() }) {
            Text(
                text = if (vm.isLoginMode) "Нет аккаунта? Создать" else "Уже есть? Войти",
                color = MayasTheme.TextGrey
            )
        }
    }
}