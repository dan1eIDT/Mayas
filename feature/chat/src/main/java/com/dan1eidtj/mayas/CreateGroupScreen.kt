package com.dan1eidtj.mayas.feature.chat

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search // Добавлен импорт поиска
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.google.firebase.firestore.FirebaseFirestore

// Класс данных для отображения пользователя со статусом выбора
data class SelectableUser(
    val uid: String,
    val name: String,
    val username: String = "", // Добавлено поле username
    val isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val chatVM: ChatVM = viewModel()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Состояния (используем mutableIntStateOf для оптимизации)
    var step by remember { mutableIntStateOf(1) }
    var contacts by remember { mutableStateOf<List<SelectableUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Переносим настройки группы наверх (hoisting), чтобы они были видны везде
    var groupTitle by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) } // false = Приватная, true = Публичная

    val selectedUsers = contacts.filter { it.isSelected }

    // Фильтруем список контактов перед выводом в LazyColumn
    val filteredContacts = contacts.filter { contact ->
        contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.username.contains(searchQuery, ignoreCase = true)
    }

    // Подгрузка контактов (всех, кроме себя)
    // Подгрузка контактов (Только те, с кем есть чаты, и НЕ включаем себя)
    LaunchedEffect(Unit) {
        val myUid = chatVM.myUid ?: "" // Твой ID из ViewModel (или FirebaseAuth.getInstance().currentUser?.uid)

        if (myUid.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        // 1. Сначала ищем все чаты, где МЫ являемся участником (и это личные чаты, а не группы)
        db.collection("chats")
            .whereArrayContains("participants", myUid)
            .get()
            .addOnSuccessListener { chatsSnap ->

                // Собираем ID всех людей, с кем у нас есть чаты (исключая себя)
                val partnerIds = chatsSnap.documents.mapNotNull { doc ->
                    val isGroup = doc.getBoolean("isGroup") ?: false
                    if (!isGroup) {
                        val participants = doc.get("participants") as? List<*>
                        // Находим ID собеседника (тот элемент списка, который не равен моему uid)
                        participants?.firstOrNull { it != myUid } as? String
                    } else {
                        null // Пропускаем групповые чаты, нам нужны только личные контакты
                    }
                }.distinct() // Убираем дубликаты на всякий случай

                if (partnerIds.isEmpty()) {
                    contacts = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                // 2. Теперь делаем точечный запрос в "users", чтобы достать инфу только по этим ID
                // Firestore позволяет использовать 'whereIn', но максимум для 30 элементов за раз.
                // Для надежности прогоним загрузку документов:
                val loadedUsers = mutableListOf<SelectableUser>()
                var processedCount = 0

                partnerIds.forEach { partnerId ->
                    db.collection("users").document(partnerId).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val username = userDoc.getString("username") ?: ""
                                val name = userDoc.getString("name") ?: username.ifEmpty { "User" }

                                loadedUsers.add(
                                    SelectableUser(uid = partnerId, name = name, username = username)
                                )
                            }

                            processedCount++
                            // Когда проверили все ID, обновляем состояние экрана
                            if (processedCount == partnerIds.size) {
                                contacts = loadedUsers.sortedBy { it.name } // Сортируем по алфавиту
                                isLoading = false
                            }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == partnerIds.size) {
                                contacts = loadedUsers.sortedBy { it.name }
                                isLoading = false
                            }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Surface),
                title = {
                    Column {
                        Text(
                            text = if (step == 1) "Новая группа" else "Название группы",
                            color = MayasTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (step == 1 && selectedUsers.isNotEmpty()) {
                            Text(
                                text = "${selectedUsers.size} из ${contacts.size} выбрано",
                                color = MayasTheme.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (step == 2) step = 1 else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedUsers.isNotEmpty()) {
                FloatingActionButton(
                    containerColor = MayasTheme.GlowPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    onClick = {
                        if (step == 1) {
                            step = 2
                        } else {
                            if (groupTitle.isBlank()) {
                                Toast.makeText(context, "Введите название группы", Toast.LENGTH_SHORT).show()
                                return@FloatingActionButton
                            }
                            val selectedIds = selectedUsers.map { it.uid }

                            // Передаем все необходимые параметры в метод создания группы
                            chatVM.createGroupChat(
                                groupTitle,
                                groupDescription,
                                isPublic,
                                selectedIds
                            ) { newChatId ->
                                onGroupCreated(newChatId)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (step == 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                        contentDescription = null
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MayasTheme.Background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MayasTheme.GlowPurple)
            } else {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "StepAnimation"
                ) { currentStep ->
                    if (currentStep == 1) {
                        Column(modifier = Modifier.fillMaxSize()) {

                            // Поле поиска теперь внутри структуры шага 1
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                placeholder = { Text("Поиск участников...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                                singleLine = true
                            )

                            AnimatedVisibility(visible = selectedUsers.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(selectedUsers, key = { it.uid }) { user ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(55.dp)
                                        ) {
                                            Box(modifier = Modifier.size(45.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(MayasTheme.GlowPurple.copy(0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(user.name.take(1).uppercase(), color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Gray)
                                                        .align(Alignment.BottomEnd)
                                                        .clickable {
                                                            contacts = contacts.map {
                                                                if (it.uid == user.uid) it.copy(isSelected = false) else it
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = user.name,
                                                fontSize = 11.sp,
                                                color = MayasTheme.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedUsers.isNotEmpty()) {
                                HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Важно: используем filteredContacts вместо contacts для отображения результатов поиска
                                items(filteredContacts) { user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (user.isSelected) MayasTheme.Surface.copy(alpha = 0.5f) else Color.Transparent)
                                            .clickable {
                                                contacts = contacts.map {
                                                    if (it.uid == user.uid) it.copy(isSelected = !it.isSelected) else it
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(MayasTheme.GlowPurple.copy(0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(user.name.take(1).uppercase(), color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(Modifier.width(14.dp))

                                        Text(
                                            text = user.name,
                                            color = MayasTheme.TextPrimary,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Checkbox(
                                            checked = user.isSelected,
                                            onCheckedChange = { checked ->
                                                contacts = contacts.map {
                                                    if (it.uid == user.uid) it.copy(isSelected = checked) else it
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = MayasTheme.GlowPurple)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(MayasTheme.GlowPurple)
                                    .clickable { Toast.makeText(context, "Загрузка фото скоро!", Toast.LENGTH_SHORT).show() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }

                            Spacer(Modifier.height(24.dp))

                            // Объявление переменных groupTitle, groupDescription и isPublic удалено отсюда, так как они теперь на самом верху.

                            OutlinedTextField(
                                value = groupTitle,
                                onValueChange = { if (it.length <= 32) groupTitle = it },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                label = { Text("Название группы") },
                                singleLine = true,
                                supportingText = {
                                    Text(text = "${groupTitle.length}/32", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                                }
                            )

                            // Поле описания
                            OutlinedTextField(
                                value = groupDescription,
                                onValueChange = { groupDescription = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                label = { Text("Описание группы (необязательно)") },
                                maxLines = 3
                            )

                            // Переключатель приватности
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPublic) "Публичная группа 🌍" else "Приватная группа 🔒",
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = isPublic,
                                    onCheckedChange = { isPublic = it }
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Участников: ${selectedUsers.size}",
                                color = MayasTheme.TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }
                    }
                }
            }
        }
    }
}