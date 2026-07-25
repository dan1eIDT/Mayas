package com.dan1eidtj.mayas.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.DarkMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.LightMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.MayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    currentScheme: MayasColorScheme,
    customThemes: List<Pair<String, MayasColorScheme>> = emptyList(),
    onSelectScheme: (MayasColorScheme) -> Unit,
    onNavigateToEditor: () -> Unit,
    onEditCustomTheme: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text("Темы", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { SettingsSectionTitle("СТАНДАРТНЫЕ") }

            item {
                ThemePresetRow(
                    title = "Тёмная",
                    subtitle = "Классическая тёмная тема Mayas",
                    scheme = DarkMayasColorScheme,
                    isSelected = currentScheme == DarkMayasColorScheme,
                    onClick = { onSelectScheme(DarkMayasColorScheme) }
                )
            }

            item {
                ThemePresetRow(
                    title = "Светлая",
                    subtitle = "Классическая светлая тема Mayas",
                    scheme = LightMayasColorScheme,
                    isSelected = currentScheme == LightMayasColorScheme,
                    onClick = { onSelectScheme(LightMayasColorScheme) }
                )
            }

            if (customThemes.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)) }
                item { SettingsSectionTitle("МОИ ТЕМЫ") }

                items(customThemes) { (name, scheme) ->
                    ThemePresetRow(
                        title = name,
                        subtitle = "Своя тема",
                        scheme = scheme,
                        isSelected = currentScheme == scheme,
                        onClick = { onSelectScheme(scheme) },
                        onEditClick = { onEditCustomTheme(name) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Surface(
                    onClick = onNavigateToEditor,
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
                        Icon(Icons.Default.Add, null, tint = MayasTheme.Accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Создать свою тему",
                            color = MayasTheme.Accent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePresetRow(
    title: String,
    subtitle: String,
    scheme: MayasColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
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

            Row(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(scheme.background, scheme.bubbleMine)))
            ) {}

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MayasTheme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 13.sp)
            }

            if (onEditClick != null) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = MayasTheme.Accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}
