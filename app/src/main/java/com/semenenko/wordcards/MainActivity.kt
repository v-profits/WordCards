package com.semenenko.wordcards

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.semenenko.wordcards.data.AppSettings
import com.semenenko.wordcards.data.RatingPrefs
import com.semenenko.wordcards.ui.AppRoot
import com.semenenko.wordcards.ui.WordCardsTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as WordApp
        // Новый запуск (не пересоздание экрана): учитываем для окна оценки
        val showRating = savedInstanceState == null && RatingPrefs.registerLaunch(this)
        setContent {
            val st by app.settings.flow.collectAsState(initial = AppSettings())
            WordCardsTheme(st.theme) { AppRoot(showRating) }
        }
    }
}
