package com.dan1eidtj.mayas.core_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme


@Composable
fun EmailVerificationRequiredDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onVerifyClick: () -> Unit,
    title: String = "Подтвердите почту чтоб звонить!",
    message: String = "Мы отправили тебе письмо при регистрации. Подтверди почту по ссылке из письма — и звонки станут доступны."
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MayasTheme.RedAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    tint = MayasTheme.RedAccent
                )
            }
        },
        title = { Text(title, color = MayasTheme.TextPrimary) },
        text = { Text(message, color = MayasTheme.TextSecondary) },
        confirmButton = {
            TextButton(
                onClick = onVerifyClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MayasTheme.RedAccent)
            ) {
                Text("Подтвердить почту")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Позже", color = MayasTheme.TextSecondary)
            }
        },
        containerColor = MayasTheme.Surface
    )
}
