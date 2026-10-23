/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dan1eidtj.data.BackendApi
import com.dan1eidtj.data.SteamStats
import com.dan1eidtj.data.SteamStatus
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.settings.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteamScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<SteamStatus?>(null) }
    var stats by remember { mutableStateOf<SteamStats?>(null) }
    val recentGames = stats?.recentGames.orEmpty()
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }
    var profileDisplayMode by remember { mutableStateOf("CARD") }
    var profileDisplaySaving by remember { mutableStateOf(false) }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    LaunchedEffect(myUid) {
        if (myUid.isBlank()) return@LaunchedEffect
        runCatching {
            FirebaseFirestore.getInstance().collection("users").document(myUid).get().await()
                .getString("steamStatusDisplayMode")
        }.onSuccess { stored ->
            profileDisplayMode = when (stored) {
                "TEXT", "HIDDEN" -> stored
                else -> "CARD"
            }
        }.onFailure { error ->
            android.util.Log.w("SteamScreen", "Не удалось загрузить режим отображения Steam", error)
        }
    }

    fun saveProfileDisplayMode(mode: String) {
        if (mode == profileDisplayMode || profileDisplaySaving || myUid.isBlank()) return
        val previous = profileDisplayMode
        profileDisplayMode = mode
        profileDisplaySaving = true
        scope.launch {
            try {
                FirebaseFirestore.getInstance().collection("users").document(myUid)
                    .update("steamStatusDisplayMode", mode).await()
            } catch (e: CancellationException) {
                profileDisplayMode = previous
                throw e
            } catch (e: Exception) {
                profileDisplayMode = previous
                errorText = e.message
            } finally {
                profileDisplaySaving = false
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        // Первый ON_RESUME стреляет сразу при подписке, его пропускаем:
        // первую загрузку и так делает LaunchedEffect.
        var firstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) firstResume = false else refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshTick) {
        loading = true
        errorText = null
        try {
            status = BackendApi.steamStatus()
            stats = if (status?.linked == true) {
                try {
                    BackendApi.steamStats()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorText = e.message
        }
        loading = false
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.steam_title), color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MayasTheme.Surface)
                    .padding(16.dp)
            ) {
                val current = status
                when {
                    loading && current == null -> Text(stringResource(R.string.steam_loading), color = MayasTheme.TextSecondary)
                    current == null -> Text(
                        stringResource(R.string.steam_load_error, errorText.orEmpty()),
                        color = MayasTheme.TextSecondary
                    )
                    !current.configured -> Text(stringResource(R.string.steam_not_configured), color = MayasTheme.TextSecondary)
                    !current.linked -> {
                        Text(stringResource(R.string.steam_not_linked), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.steam_link_hint), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            scope.launch {
                                try {
                                    val url = BackendApi.steamLinkStart()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    errorText = e.message
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.steam_link_button))
                        }
                    }
                    else -> {
                        Text(
                            stringResource(R.string.steam_linked, current.steamIdSuffix),
                            color = MayasTheme.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.steam_show_game), color = MayasTheme.TextPrimary)
                                Text(stringResource(R.string.steam_show_game_hint), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                            }
                            Switch(checked = current.showGame, onCheckedChange = { checked ->
                                status = current.copy(showGame = checked)
                                scope.launch {
                                    try {
                                        BackendApi.steamSetShowGame(checked)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        status = current
                                        errorText = e.message
                                    }
                                }
                            })
                        }
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    BackendApi.steamUnlink()
                                    refreshTick++
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    errorText = e.message
                                }
                            }
                        }) { Text(stringResource(R.string.steam_unlink), color = MayasTheme.ErrorRed) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MayasTheme.Surface)
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.steam_profile_display_title), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.steam_profile_display_desc), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "CARD" to R.string.steam_profile_display_card,
                    "TEXT" to R.string.steam_profile_display_text,
                    "HIDDEN" to R.string.steam_profile_display_hidden
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = !profileDisplaySaving && myUid.isNotBlank()) { saveProfileDisplayMode(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = profileDisplayMode == mode,
                            onClick = null,
                            enabled = !profileDisplaySaving && myUid.isNotBlank()
                        )
                        Text(stringResource(label), color = MayasTheme.TextPrimary, fontSize = 14.sp)
                    }
                }
                if (profileDisplaySaving) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.steam_profile_display_saving), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                }
            }
            if (recentGames.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayasTheme.Surface)
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.steam_recent_games_title), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    recentGames.forEach { game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(game.name, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                                Text(
                                    stringResource(R.string.steam_playtime_forever, game.playtimeForeverMinutes / 60),
                                    color = MayasTheme.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (game.playtime2WeeksMinutes > 0) {
                                Text(
                                    stringResource(R.string.steam_playtime_2weeks, game.playtime2WeeksMinutes / 60f),
                                    color = MayasTheme.Accent,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            errorText?.let {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.steam_load_error, it), color = MayasTheme.ErrorRed, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.steam_details_hint), color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
    }
}