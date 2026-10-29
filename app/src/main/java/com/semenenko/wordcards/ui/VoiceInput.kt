package com.semenenko.wordcards.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.semenenko.wordcards.R

/**
 * Голосовой ввод. Язык распознавания задаётся явно (en-US или ru-RU) — он всегда совпадает
 * с языком ожидаемого ответа, поэтому работает и для английского, и для русского.
 *
 * Порядок работы:
 *  1) распознаватель Google (если установлен) — на телефонах с «фирменным» распознаванием
 *     производителя оно часто не понимает английский и отвечает «Не слышно»;
 *  2) распознаватель по умолчанию;
 *  3) системное окно голосового ввода — если прямой вызов недоступен.
 * Все ошибки показываются понятным текстом с названием языка распознавания.
 */
class VoiceInput(private val ctx: Context) {
    /** Идёт прослушивание. */
    var listening by mutableStateOf(false)
        private set
    /** Сообщение об ошибке для пользователя. */
    var message by mutableStateOf<String?>(null)

    var langTag = "en-US"
    var langName = ""
    var prompt = ""
    var onText: (String) -> Unit = {}
    var requestPermission: () -> Unit = {}
    var launchSystemDialog: (Intent) -> Unit = {}

    private var recognizer: SpeechRecognizer? = null
    private var useGoogle = true

    /** Нажатие на микрофон: начать или остановить прослушивание. */
    fun start() {
        message = null
        if (listening) { recognizer?.stopListening(); return }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(); return
        }
        begin()
    }

    fun begin() {
        message = null
        destroyRecognizer()
        val r = createRecognizer()
        if (r == null) { fallbackToDialog(); return }
        recognizer = r
        r.setRecognitionListener(listener)
        listening = true
        r.startListening(recognizerIntent())
    }

    /** Отмена при смене карточки или уходе с экрана. */
    fun cancel() {
        recognizer?.cancel()
        destroyRecognizer()
        listening = false
        message = null
    }

    fun release() { destroyRecognizer(); listening = false }

    private fun recognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }

    private fun googleInstalled() =
        try { ctx.packageManager.getPackageInfo(GOOGLE_PKG, 0); true } catch (e: Exception) { false }

    private fun createRecognizer(): SpeechRecognizer? {
        if (useGoogle && googleInstalled()) {
            try {
                return SpeechRecognizer.createSpeechRecognizer(ctx, ComponentName(GOOGLE_PKG, GOOGLE_SERVICE))
            } catch (e: Exception) { /* пробуем распознаватель по умолчанию */ }
        }
        return if (SpeechRecognizer.isRecognitionAvailable(ctx)) SpeechRecognizer.createSpeechRecognizer(ctx) else null
    }

    private fun destroyRecognizer() {
        try { recognizer?.destroy() } catch (e: Exception) { }
        recognizer = null
    }

    private fun fallbackToDialog() {
        try { launchSystemDialog(recognizerIntent()) }
        catch (e: ActivityNotFoundException) { message = ctx.getString(R.string.study_mic_unavailable) }
    }

    private fun firstResult(b: Bundle?) =
        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        // Промежуточный результат сразу заменяет текст в поле — как и итоговый
        override fun onPartialResults(partialResults: Bundle?) { firstResult(partialResults)?.let(onText) }
        override fun onResults(results: Bundle?) {
            listening = false
            firstResult(results)?.let(onText)
            destroyRecognizer()
        }
        override fun onError(error: Int) {
            listening = false
            destroyRecognizer()
            handleError(error)
        }
    }

    private fun handleError(code: Int) {
        message = when (code) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                ctx.getString(R.string.voice_no_match, langName)
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                ctx.getString(R.string.voice_network)
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                ctx.getString(R.string.voice_lang_unsupported, langName)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                ctx.getString(R.string.voice_no_permission)
            SpeechRecognizer.ERROR_CLIENT -> {
                // Распознаватель Google не запустился — один раз пробуем другой, затем системное окно
                if (useGoogle) { useGoogle = false; begin() } else fallbackToDialog()
                null
            }
            else -> ctx.getString(R.string.voice_error_code, code)
        }
    }

    private companion object {
        const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"
        const val GOOGLE_SERVICE = "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
    }
}

@Composable
fun rememberVoiceInput(langTag: String, langName: String, prompt: String, onText: (String) -> Unit): VoiceInput {
    val ctx = LocalContext.current
    val voice = remember { VoiceInput(ctx) }
    voice.langTag = langTag
    voice.langName = langName
    voice.prompt = prompt
    voice.onText = onText
    val dialog = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(voice.onText)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.begin() else voice.message = ctx.getString(R.string.voice_no_permission)
    }
    voice.launchSystemDialog = { dialog.launch(it) }
    voice.requestPermission = { permission.launch(Manifest.permission.RECORD_AUDIO) }
    DisposableEffect(Unit) { onDispose { voice.release() } }
    return voice
}
