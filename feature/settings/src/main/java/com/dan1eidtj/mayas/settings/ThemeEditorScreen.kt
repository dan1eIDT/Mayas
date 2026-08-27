package com.dan1eidtj.mayas.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


private data class EditableField(
    val label: String,
    val get: (MayasColorScheme) -> Color,
    val set: (MayasColorScheme, Color) -> MayasColorScheme,
)

private val editableFields = listOf(
    EditableField("Фон чата", { it.background }, { s, c -> s.copy(background = c) }),
    EditableField("Моё сообщение", { it.bubbleMine }, { s, c -> s.copy(bubbleMine = c) }),
    EditableField("Чужое сообщение", { it.bubbleOther }, { s, c -> s.copy(bubbleOther = c) }),
    EditableField("Акцентный цвет", { it.accent }, { s, c -> s.copy(accent = c) }),
    EditableField("Текст (основной)", { it.textPrimary }, { s, c -> s.copy(textPrimary = c) }),
    EditableField("Текст (вторичный)", { it.textSecondary }, { s, c -> s.copy(textSecondary = c) }),
    EditableField("Поверхность (карточки, поле ввода)", { it.surface }, { s, c -> s.copy(surface = c) }),
    EditableField("Ссылки", { it.linkColor }, { s, c -> s.copy(linkColor = c) }),
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditorScreen(
    initialScheme: MayasColorScheme = DarkMayasColorScheme,
    onSave: (MayasColorScheme) -> Unit,
    onBack: () -> Unit,
) {
    var scheme by remember { mutableStateOf(initialScheme) }
    var editingField by remember { mutableStateOf<EditableField?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор темы") },
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { onSave(scheme) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить тему")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            ChatPreview(scheme = scheme)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(editableFields) { field ->
                    ColorFieldRow(
                        label = field.label,
                        color = field.get(scheme),
                        onClick = { editingField = field }
                    )
                }
                item {
                    Text(
                        text = "Темы между устройствами не синхронизируются.",
                        color = MayasTheme.TextSecondary,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    editingField?.let { field ->
        ColorPickerDialog(
            title = field.label,
            initialColor = field.get(scheme),
            onDismiss = { editingField = null },
            onConfirm = { newColor ->
                scheme = field.set(scheme, newColor)
                editingField = null
            }
        )
    }
}

@Composable
private fun ColorFieldRow(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MayasTheme.CardRadius),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}


@Composable
private fun ChatPreview(scheme: MayasColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                Brush.verticalGradient(listOf(scheme.background, scheme.surface))
            )
            .padding(12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(scheme.accent)
            )
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(
                text = "???",
                color = scheme.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier
                    .clip(MayasTheme.BubbleRadius)
                    .background(scheme.bubbleOther)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = "Скажите если не секрет..Кто ваш любимый персонаж?", color = scheme.textPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))


        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .clip(MayasTheme.BubbleRadius)
                    .background(scheme.bubbleMine)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
            Text(text = "Ооо.. Вы запишите , конешно это Фолли!", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier
                    .clip(MayasTheme.BubbleRadius)
                    .background(scheme.bubbleOther)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = "Емать. Справа пожалуйста остановите.", color = scheme.textPrimary)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MayasTheme.CardRadius)
                .background(scheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var previewText by remember { mutableStateOf("") }
            TextField(
                value = previewText,
                onValueChange = { previewText = it },
                placeholder = { Text("Сообщение", color = scheme.textSecondary) },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {  }) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Отправить",
                    tint = scheme.accent
                )
            }
        }
    }
}
