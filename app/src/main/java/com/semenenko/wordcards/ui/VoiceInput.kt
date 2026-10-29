package com.semenenko.wordcards.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.semenenko.wordcards.R

/**
 * Голосовой ввод через всплывающее окно Google с микрофоном (системное окно распознавания речи).
 *
 * Язык распознавания всегда совпадает с языком ожидаемого ответа: en-US для английского и
 * ru-RU для русского. Google в разных версиях читает язык из разных параметров, поэтому
 * язык передаётся несколькими способами сразу, а второй язык добавляется как дополнительный.
 */
class VoiceInput(private val ctx: Context) {
    /** Сообщение для пользователя (причина, если распознавание не удалось). */
    var message by mutableStateOf<String?>(null)

    var langTag = "en-US"
    var otherTag = "ru-RU"
    var langName = ""
    var prompt = ""
    var launchDialog: (Intent) -> Unit = {}

    private fun intent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        // Основной язык — тот, на котором ожидается ответ
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        // Второй язык как дополнительный: если распознаватель «слушает» не тот язык, он всё равно поймёт
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(otherTag))
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }

    /** Нажатие на микрофон: открыть окно Google (если его нет — любое системное окно распознавания). */
    fun start() {
        message = null
        try {
            launchDialog(intent().setPackage(GOOGLE_PKG))
        } catch (e: ActivityNotFoundException) {
            try { launchDialog(intent()) }
            catch (e2: ActivityNotFoundException) { message = ctx.getString(R.string.study_mic_unavailable) }
        }
    }

    /** Разбор ответа окна: текст — в поле ввода, иначе понятная причина. */
    fun handleResult(resultCode: Int, data: Intent?, onText: (String) -> Unit) {
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull { it.isNotBlank() }
        when {
            resultCode == Activity.RESULT_OK && text != null -> onText(text)
            resultCode == Activity.RESULT_CANCELED -> { /* пользователь закрыл окно */ }
            resultCode == RecognizerIntent.RESULT_NETWORK_ERROR || resultCode == RecognizerIntent.RESULT_SERVER_ERROR ->
                message = ctx.getString(R.string.voice_network)
            resultCode == RecognizerIntent.RESULT_NO_MATCH || resultCode == Activity.RESULT_OK ->
                message = ctx.getString(R.string.voice_no_match, langName)
            else -> message = ctx.getString(R.string.voice_error_code, resultCode)
        }
    }

    private companion object {
        const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"
    }
}

@Composable
fun rememberVoiceInput(
    langTag: String, otherTag: String, langName: String, prompt: String, onText: (String) -> Unit
): VoiceInput {
    val ctx = LocalContext.current
    val voice = remember { VoiceInput(ctx) }
    voice.langTag = langTag
    voice.otherTag = otherTag
    voice.langName = langName
    voice.prompt = prompt
    val currentOnText by rememberUpdatedState(onText)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        voice.handleResult(res.resultCode, res.data) { currentOnText(it) }
    }
    voice.launchDialog = { launcher.launch(it) }
    return voice
}
