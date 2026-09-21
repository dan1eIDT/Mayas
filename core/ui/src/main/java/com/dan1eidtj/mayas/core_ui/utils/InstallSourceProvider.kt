/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.core_ui.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

enum class InstallSource {
    GITHUB,
    GOOGLE_PLAY,
    APP_GALLERY
}

object InstallSourceProvider {

    private const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
    private const val APP_GALLERY_PACKAGE = "com.huawei.appmarket"
    private const val APP_GALLERY_LISTING_URL = "https://appgallery.huawei.com/app/C118665147"

    fun detect(context: Context): InstallSource {
        val installerPackageName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }

        return when (installerPackageName) {
            GOOGLE_PLAY_PACKAGE -> InstallSource.GOOGLE_PLAY
            APP_GALLERY_PACKAGE -> InstallSource.APP_GALLERY
            else -> InstallSource.GITHUB
        }
    }

    fun openUpdate(context: Context, source: InstallSource, githubUpdateUrl: String) {
        val marketIntent = when (source) {
            InstallSource.GOOGLE_PLAY -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${context.packageName}")
            ).apply { setPackage(GOOGLE_PLAY_PACKAGE) }
            InstallSource.APP_GALLERY -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("appmarket://details?id=${context.packageName}")
            ).apply { setPackage(APP_GALLERY_PACKAGE) }
            InstallSource.GITHUB -> null
        }

        val fallbackUrl = when (source) {
            InstallSource.GOOGLE_PLAY -> "https://play.google.com/store/apps/details?id=${context.packageName}"
            InstallSource.APP_GALLERY -> APP_GALLERY_LISTING_URL
            InstallSource.GITHUB -> githubUpdateUrl
        }

        if (fallbackUrl.isEmpty() && marketIntent == null) return

        try {
            if (marketIntent != null) {
                context.startActivity(marketIntent)
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
            }
        } catch (e: Exception) {
            if (fallbackUrl.isNotEmpty()) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                } catch (e: Exception) {
                }
            }
        }
    }
}

fun InstallSource.label(): String = when (this) {
    InstallSource.GITHUB -> "GitHub"
    InstallSource.GOOGLE_PLAY -> "Google Play"
    InstallSource.APP_GALLERY -> "AppGallery"
}

val InstallSource.accentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        InstallSource.GITHUB -> MayasTheme.GlowWhite
        InstallSource.GOOGLE_PLAY -> MayasTheme.GlowGreen
        InstallSource.APP_GALLERY -> MayasTheme.GlowRed
    }

val InstallSource.onAccentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (this == InstallSource.GITHUB) Color.Black else Color.White
