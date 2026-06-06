package com.dan1eidtj.mayas.feature.auth


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.*
import com.dan1eidtj.mayas.core_ui.ui.components.*
import com.dan1eidtj.mayas.feature.auth.*


@Composable
fun AuthScreen(vm: AuthVM) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MayasTheme.RedAccent, unfocusedBorderColor = MayasTheme.TextGrey,
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
        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя", color = MayasTheme.TextGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Юзернейм (без @)", color = MayasTheme.TextGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = MayasTheme.TextGrey) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Пароль", color = MayasTheme.TextGrey) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
        )

        if (error != null) Text(
            text = error!!,
            color = MayasTheme.ErrorRed,
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                error = null
                if (email.isEmpty()) {
                    error = "Введите Email!"; return@Button
                }
                if (!email.contains("@")) {
                    error = "Некорректный Email"; return@Button
                }
                if (pass.length < 6) {
                    error = "Пароль мин. 6 символов"; return@Button
                }
                if (!isLogin) {
                    if (name.isEmpty()) {
                        error = "Введите имя"; return@Button
                    }
                    if (username.isEmpty() || username.contains(" ")) {
                        error = "Юзернейм без пробелов"; return@Button
                    }
                    vm.checkUsername(username) { isAvailable: Boolean ->
                        if (isAvailable) vm.register(
                            email,
                            pass,
                            name,
                            username
                        ) { errorMsg: String -> error = errorMsg }
                        else error = "Юзернейм @$username уже занят!"
                    }
                } else {
                    vm.login(email, pass) { errorMsg: String -> error = errorMsg }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.RedAccent),
            enabled = !vm.isLoading
        ) {
            if (vm.isLoading) CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            else Text(
                text = if (isLogin) "Войти" else "Зарегистрироваться",
                color = Color.White
            )
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { isLogin = !isLogin; error = null }) {
            Text(
                text = if (isLogin) "Нет аккаунта? Создать" else "Уже есть? Войти",
                color = MayasTheme.TextGrey
                )
            }
        }
    }

