package com.example.wordcards

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.wordcards.data.AppDatabase
import com.example.wordcards.data.Repository
import com.example.wordcards.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

/** Приложение: простой «ручной» DI — общие объекты создаются один раз. */
class WordApp : Application() {
    lateinit var repo: Repository
    lateinit var settings: SettingsStore
    lateinit var speaker: Speaker
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repo = Repository(this, AppDatabase.build(this))
        settings = SettingsStore(this)
        speaker = Speaker(this)
        Reminder.createChannel(this)
        appScope.launch { repo.seedIfEmpty() }
    }
}

/** Озвучивание английских слов встроенным синтезатором речи Android. */
class Speaker(ctx: Context) {
    private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setSpeechRate(0.9f)
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word")
    }
}
