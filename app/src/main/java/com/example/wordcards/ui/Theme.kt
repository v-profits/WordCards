package com.example.wordcards.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.wordcards.WordApp

/** Тема: динамические цвета Material You (Android 12+), светлая/тёмная/системная. */
@Composable
fun WordCardsTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Доступ к общим объектам приложения из Compose. */
@Composable
fun rememberApp(): WordApp = LocalContext.current.applicationContext as WordApp
