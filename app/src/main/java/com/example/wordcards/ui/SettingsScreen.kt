package com.example.wordcards.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.AppLang
import com.example.wordcards.R
import com.example.wordcards.data.Direction
import com.example.wordcards.data.LearnLang
import com.example.wordcards.data.Levels
import kotlin.math.roundToInt

/** Настройки: язык интерфейса, изучаемый язык, направление, лимиты, звук, тема, напоминания, импорт, сброс. */
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel(), onHelp: () -> Unit) {
    val s by vm.settings.collectAsState()
    val msg by vm.message.collectAsState()
    var importLevel by remember { mutableIntStateOf(1) }
    var confirmReset by remember { mutableStateOf(false) }

    // Выбор CSV-файла через системный проводник
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCsv(uri, importLevel)
    }
    // Разрешение на уведомления (Android 13+)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminder(granted, s.reminderHour)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.set_help)) }

        Section(stringResource(R.string.set_lang_title)) {
            val tag = AppLang.currentTag()
            ChipRow {
                FilterChip(selected = tag == "", onClick = { vm.setLanguage("") }, label = { Text(stringResource(R.string.set_lang_system)) })
                FilterChip(selected = tag == "en", onClick = { vm.setLanguage("en") }, label = { Text(stringResource(R.string.set_lang_en)) })
                FilterChip(selected = tag == "ru", onClick = { vm.setLanguage("ru") }, label = { Text(stringResource(R.string.set_lang_ru)) })
            }
        }

        Section(stringResource(R.string.set_learn_title)) {
            Text(stringResource(R.string.set_learn_desc), style = MaterialTheme.typography.bodySmall)
            WideChip(s.learning == LearnLang.EN, stringResource(R.string.set_learn_en)) { vm.setLearning(LearnLang.EN) }
            WideChip(s.learning == LearnLang.RU, stringResource(R.string.set_learn_ru)) { vm.setLearning(LearnLang.RU) }
        }

        Section(stringResource(R.string.set_dir_title)) {
            Text(stringResource(R.string.set_dir_desc), style = MaterialTheme.typography.bodySmall)
            val target = langName(s.learning)
            val native = langName(s.learning.other())
            WideChip(s.direction == Direction.FORWARD, stringResource(R.string.dir_pair, target, native)) { vm.setDirection(Direction.FORWARD) }
            WideChip(s.direction == Direction.BACKWARD, stringResource(R.string.dir_pair, native, target)) { vm.setDirection(Direction.BACKWARD) }
            WideChip(s.direction == Direction.MIXED, stringResource(R.string.dir_mixed)) { vm.setDirection(Direction.MIXED) }
        }

        Section(stringResource(R.string.set_load_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_unlimited), Modifier.weight(1f))
                Switch(checked = s.newPerDay == 0, onCheckedChange = { vm.setNewPerDay(if (it) 0 else 10) })
            }
            if (s.newPerDay != 0) SliderRow({ stringResource(R.string.set_new_per_day, it) }, s.newPerDay, 5..100, 5) { vm.setNewPerDay(it) }
            else Text(stringResource(R.string.set_unlimited_desc), style = MaterialTheme.typography.bodySmall)
            SliderRow({ stringResource(R.string.set_goal, it) }, s.dailyGoal, 10..100, 10) { vm.setDailyGoal(it) }
        }

        Section(stringResource(R.string.set_sound_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_autospeak), Modifier.weight(1f))
                Switch(checked = s.autoSpeak, onCheckedChange = { vm.setAutoSpeak(it) })
            }
            ChipRow {
                listOf(0 to R.string.theme_system, 1 to R.string.theme_light, 2 to R.string.theme_dark).forEach { (v, t) ->
                    FilterChip(selected = s.theme == v, onClick = { vm.setTheme(v) }, label = { Text(stringResource(t)) })
                }
            }
        }

        Section(stringResource(R.string.set_reminder_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_reminder), Modifier.weight(1f))
                Switch(checked = s.reminderOn, onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.setReminder(on, s.reminderHour)
                })
            }
            if (s.reminderOn) SliderRow({ stringResource(R.string.set_reminder_hour, it) }, s.reminderHour, 6..23, 1) { vm.setReminder(true, it) }
        }

        Section(stringResource(R.string.set_import_title)) {
            Text(stringResource(R.string.set_import_desc), style = MaterialTheme.typography.bodySmall)
            // Ряд уровней не помещается в экран — его можно листать свайпом
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Levels.all.forEach { l ->
                    FilterChip(selected = importLevel == l.id, onClick = { importLevel = l.id }, label = { Text(l.code) })
                }
            }
            // Кнопка на всю ширину блока, минимальные боковые отступы, надпись по центру
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
            ) { Text(stringResource(R.string.set_import_btn), textAlign = TextAlign.Center) }
            msg?.let { m ->
                Text(
                    if (m.arg != null) stringResource(m.res, m.arg) else stringResource(m.res),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Section(stringResource(R.string.set_sources_title)) {
            Text(stringResource(R.string.set_sources_text), style = MaterialTheme.typography.bodySmall)
        }

        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.set_reset))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.set_reset_title)) },
            text = { Text(stringResource(R.string.set_reset_text)) },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text(stringResource(R.string.reset)) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** Чип на всю ширину строки — такие чипы идут друг под другом. */
@Composable
private fun WideChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) }, modifier = Modifier.fillMaxWidth())
}

/** Ползунок с локальным значением; в настройки пишем только при отпускании. label(v) — подпись с числом. */
@Composable
private fun SliderRow(label: @Composable (Int) -> String, value: Int, range: IntRange, step: Int, onCommit: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text(label(v.roundToInt()))
    Slider(
        value = v, onValueChange = { v = it },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
        onValueChangeFinished = { onCommit(v.roundToInt()) }
    )
}
