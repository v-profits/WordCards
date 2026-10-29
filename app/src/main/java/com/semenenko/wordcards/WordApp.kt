package com.semenenko.wordcards

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.semenenko.wordcards.data.AppDatabase
import com.semenenko.wordcards.data.LearnLang
import com.semenenko.wordcards.data.Repository
import com.semenenko.wordcards.data.SettingsStore
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
        AppLang.restore(this)
        repo = Repository(this, AppDatabase.build(this))
        settings = SettingsStore(this)
        speaker = Speaker(this)
        Reminder.createChannel(this)
        appScope.launch { repo.seedIfNeeded() }
    }
}

/**
 * Язык интерфейса: "" — как в системе, "en" или "ru".
 * На Android 13+ выбор хранит система (в том числе в системных настройках «Язык приложения»),
 * на более старых версиях мы сами запоминаем его и применяем при каждом запуске.
 */
object AppLang {
    private const val PREFS = "lang"

    /** Язык, выбранный в данный момент ("" — системный). */
    fun currentTag(): String {
        val l = AppCompatDelegate.getApplicationLocales()
        return if (l.isEmpty()) "" else l.get(0)?.language ?: ""
    }

    private fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        )
    }

    /** Выбор пользователя: запоминаем и применяем (Activity пересоздастся сама). */
    fun set(ctx: Context, tag: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("tag", tag).apply()
        apply(tag)
    }

    fun restore(ctx: Context) {
        if (Build.VERSION.SDK_INT < 33) {
            apply(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tag", "") ?: "")
        }
    }

    /** Контекст с выбранным языком — для уведомлений и тостов вне Activity. */
    fun context(base: Context): Context {
        val tag = currentTag()
        if (tag.isEmpty()) return base
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(cfg)
    }
}

/** Озвучивание слов и предложений встроенным синтезатором речи Android (английский и русский). */
class Speaker(private val ctx: Context) {
    private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.setSpeechRate(0.9f)
                ready = true
            }
        }
    }

    fun speak(text: String, lang: LearnLang) {
        if (!ready || text.isBlank()) return
        val clean = text.replace("\u0301", "")   // знаки ударения не озвучиваем
        val locale = if (lang == LearnLang.EN) Locale.US else Locale("ru", "RU")
        val r = tts.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(AppLang.context(ctx), R.string.tts_missing, Toast.LENGTH_LONG).show()
            return
        }
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "word")
    }
}
