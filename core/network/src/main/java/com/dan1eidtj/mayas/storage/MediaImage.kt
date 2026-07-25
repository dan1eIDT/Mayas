package com.dan1eidtj.mayas.storage

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
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
    val resolved by androidx.compose.runtime.produceState<String?>(
        initialValue = null,
        rawUrl,
        useCustomAvatar
    ) {
        value = when {
            !useCustomAvatar || rawUrl.isNullOrBlank() -> null
            rawUrl.startsWith("http") -> rawUrl
            else -> B2MediaClient.resolveDownloadUrl(rawUrl)
        }
    }
    return resolved
}

private object PresignedUrlCache {
    private data class Entry(val url: String, val expiresAtMs: Long)
    private val map = mutableMapOf<String, Entry>()

    private const val SAFETY_MARGIN_MS = 60_000L

    fun get(key: String): String? {
        val entry = map[key] ?: return null
        return if (System.currentTimeMillis() < entry.expiresAtMs - SAFETY_MARGIN_MS) entry.url else null
    }

    fun put(key: String, url: String, ttlMs: Long = 15 * 60_000L) {
        map[key] = Entry(url, System.currentTimeMillis() + ttlMs)
    }
}

@Composable
fun B2Image(
    key: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    mediaClient: B2MediaClient = remember { B2MediaClient() },
) {
    var resolvedUrl by remember(key) { mutableStateOf(key?.let { PresignedUrlCache.get(it) }) }
    var error by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key) {
        if (key.isNullOrBlank()) return@LaunchedEffect
        val cached = PresignedUrlCache.get(key)
        if (cached != null) {
            resolvedUrl = cached
            return@LaunchedEffect
        }
        runCatching { mediaClient.refreshDownloadUrl(key) }
            .onSuccess { url ->
                PresignedUrlCache.put(key, url)
                resolvedUrl = url
            }
            .onFailure { e ->



                if (e is kotlinx.coroutines.CancellationException) throw e

                delay(1500)
                runCatching { mediaClient.refreshDownloadUrl(key) }
                    .onSuccess { url -> PresignedUrlCache.put(key, url); resolvedUrl = url }
                    .onFailure { retryError ->
                        if (retryError is kotlinx.coroutines.CancellationException) throw retryError
                        error = true
                    }
            }
    }
    Box(modifier = modifier.heightIn(min = 120.dp)) {
        when {
            resolvedUrl != null -> AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(resolvedUrl)
                    .listener(
                        onError = { _, result ->
                            Log.e(
                                "B2Image",
                                "Coil не смог загрузить картинку по key=$key, url=$resolvedUrl",
                                result.throwable
                            )
                        }
                    )
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            error -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}