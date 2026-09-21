package com.dan1eidtj.mayas.storage

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun rememberResolvedAvatarUrl(rawUrl: String?, useCustomAvatar: Boolean): String? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resolved by androidx.compose.runtime.produceState<String?>(
        initialValue = if (useCustomAvatar) MediaFileCache.cachedModel(context, rawUrl) else null,
        rawUrl,
        useCustomAvatar
    ) {
        value = when {
            !useCustomAvatar || rawUrl.isNullOrBlank() -> null
            else -> MediaFileCache.resolveModel(context, rawUrl)
        }
    }
    return resolved
}

@Composable
fun B2Image(
    key: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    mediaClient: B2MediaClient = remember { B2MediaClient() },
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var model by remember(key) { mutableStateOf(MediaFileCache.cachedModel(context, key)) }
    var error by remember(key) { mutableStateOf(false) }
    var attempt by remember(key) { mutableStateOf(0) }
    var manual by remember(key) { mutableStateOf(false) }
    var blocked by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, attempt, manual) {
        if (key.isNullOrBlank() || model != null) return@LaunchedEffect
        error = false
        blocked = false
        var resolved = runCatching { MediaFileCache.resolveModel(context, key, auto = !manual) }.getOrNull()
        if (resolved == null && !manual && MediaCachePrefs.autoDownloadBlocked(context)) {
            blocked = true
            return@LaunchedEffect
        }
        if (resolved == null) {
            delay(1500)
            resolved = runCatching { MediaFileCache.resolveModel(context, key, auto = !manual) }.getOrNull()
        }
        if (resolved == null) error = true else model = resolved
    }

    Box(modifier = modifier.heightIn(min = 120.dp)) {
        when {
            model != null -> AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(220)
                    .listener(
                        onError = { _, result ->
                            Log.e("B2Image", "Coil не смог загрузить картинку key=$key", result.throwable)
                        }
                    )
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            blocked -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
                    .clickable { manual = true },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            error -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
                    .clickable { attempt++ }
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
