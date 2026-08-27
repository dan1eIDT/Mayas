@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.Intent
import androidx.compose.foundation.lazy.LazyListScope
import coil.compose.AsyncImage
import com.dan1eidtj.data.ContactsRepository
import com.dan1eidtj.data.ContactsSyncResult
import com.dan1eidtj.data.MatchedContact
import com.dan1eidtj.data.UnregisteredContact
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import kotlinx.coroutines.launch

private enum class ContactsSyncState { IDLE, NO_PERMISSION, LOADING, DONE, ERROR }

/**
 * Экран "Найти контакты в Маяс": запрашивает READ_CONTACTS, читает адресную
 * книгу устройства, матчит номера с зарегистрированными юзерами через
 * ContactsRepository и даёт сразу открыть с ними чат.
 *
 * Номера самих контактов на сервер не отправляются — сравнение идёт батчами
 * нормализованных номеров через whereIn, а не выгрузкой всей книги целиком.
 */
@Composable
fun ContactsSyncScreen(
    myUid: String,
    onBack: () -> Unit,
    onStartChat: (uid: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf(ContactsSyncState.IDLE) }
    var result by remember { mutableStateOf(ContactsSyncResult(emptyList(), emptyList())) }
    var errorMessage by remember { mutableStateOf("") }

    fun startSync() {
        state = ContactsSyncState.LOADING
        coroutineScope.launch {
            try {
                result = ContactsRepository.syncDeviceContacts(context, myUid)
                state = ContactsSyncState.DONE
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Не удалось прочитать контакты"
                state = ContactsSyncState.ERROR
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startSync() else state = ContactsSyncState.NO_PERMISSION
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startSync()
        } else {
            state = ContactsSyncState.NO_PERMISSION
        }
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Контакты в Маяс", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MayasTheme.Background,
                    titleContentColor = MayasTheme.TextPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MayasTheme.Background)
        ) {
            when (state) {
                ContactsSyncState.IDLE, ContactsSyncState.LOADING -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MayasTheme.GlowPurple)
                        Spacer(Modifier.height(12.dp))
                        Text("Ищем твоих друзей в Маяс…", color = MayasTheme.TextSecondary, fontSize = 13.sp)
                    }
                }

                ContactsSyncState.NO_PERMISSION -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Contacts,
                            contentDescription = null,
                            tint = MayasTheme.TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Нужен доступ к контактам",
                            color = MayasTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Маяс сравнит номера из твоей адресной книги с зарегистрированными юзерами — сами номера контактов никуда не выгружаются",
                            color = MayasTheme.TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                            Text("Разрешить доступ")
                        }
                    }
                }

                ContactsSyncState.ERROR -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Что-то пошло не так", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(errorMessage, color = MayasTheme.TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { startSync() }) { Text("Повторить") }
                    }
                }

                ContactsSyncState.DONE -> {
                    if (result.onMayas.isEmpty() && result.notOnMayas.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MayasTheme.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("В контактах никого с номером", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "В адресной книге не нашлось ни одного номера",
                                color = MayasTheme.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                        ) {
                            if (result.onMayas.isNotEmpty()) {
                                item(key = "header_on_mayas") {
                                    SectionHeader("Уже в Маяс — ${result.onMayas.size}")
                                }
                                items(result.onMayas, key = { "u_" + it.uid }) { contact ->
                                    ContactMatchRow(
                                        contact = contact,
                                        onClick = { onStartChat(contact.uid) }
                                    )
                                }
                            }
                            if (result.notOnMayas.isNotEmpty()) {
                                item(key = "header_not_on_mayas") {
                                    SectionHeader("Ещё не в Маяс — ${result.notOnMayas.size}")
                                }
                                items(result.notOnMayas, key = { "n_" + it.rawPhone }) { contact ->
                                    UnregisteredContactRow(
                                        contact = contact,
                                        onInvite = {
                                            try {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "Заходи в Маяс, го общаться - https://dan1eidt.github.io/mayas-site/"
                                                    )
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Пригласить"))
                                            } catch (e: Exception) {}
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = MayasTheme.TextSecondary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun UnregisteredContactRow(contact: UnregisteredContact, onInvite: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MayasTheme.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contact.deviceName.firstOrNull()?.uppercase() ?: "?",
                color = MayasTheme.TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.deviceName.ifBlank { contact.rawPhone },
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Text(
                text = "Не в Маяс",
                color = MayasTheme.TextSecondary,
                fontSize = 12.sp
            )
        }

        Text(
            text = "Пригласить",
            color = MayasTheme.GlowPurple,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.clickable { onInvite() }.padding(8.dp)
        )
    }
}

@Composable
private fun ContactMatchRow(contact: MatchedContact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MayasTheme.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (contact.avatarUrl.startsWith("http")) {
                AsyncImage(
                    model = contact.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                )
            } else {
                Text(
                    contact.deviceName.firstOrNull()?.uppercase() ?: "?",
                    color = MayasTheme.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.deviceName.ifBlank { contact.name },
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            if (contact.username.isNotBlank()) {
                Text(
                    text = "@${contact.username}",
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
