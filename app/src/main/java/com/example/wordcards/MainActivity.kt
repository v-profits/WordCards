package com.example.wordcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.wordcards.data.AppSettings
import com.example.wordcards.ui.AppRoot
import com.example.wordcards.ui.WordCardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as WordApp
        setContent {
            val st by app.settings.flow.collectAsState(initial = AppSettings())
            WordCardsTheme(st.theme) { AppRoot() }
        }
    }
}
