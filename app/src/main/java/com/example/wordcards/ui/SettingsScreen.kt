package com.example.wordcards.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.data.Direction
import com.example.wordcards.data.Levels
import kotlin.math.roundToInt

/** Настройки: направление, лимиты, озвучка, тема, напоминания, импорт CSV, сброс. */
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
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
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)

        Section("Направление перевода") {
            ChipRow {
                listOf(Direction.EN_RU to "EN → RU", Direction.RU_EN to "RU → EN", Direction.MIXED to "Вперемешку")
                    .forEach { (d, t) ->
                        FilterChip(selected = s.direction == d, onClick = { vm.setDirection(d) }, label = { Text(t) })
                    }
            }
        }

        Section("Нагрузка") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Без дневного лимита новых слов", Modifier.weight(1f))
                Switch(checked = s.newPerDay == 0, onCheckedChange = { vm.setNewPerDay(if (it) 0 else 10) })
            }
            if (s.newPerDay != 0) SliderRow("Новых слов в день", s.newPerDay, 5..100, 5) { vm.setNewPerDay(it) }
            else Text("Новые слова идут пачками по 100 за занятие — учите сколько хотите.",
                style = MaterialTheme.typography.bodySmall)
            SliderRow("Цель дня (ответов)", s.dailyGoal, 10..100, 10) { vm.setDailyGoal(it) }
        }

        Section("Звук и тема") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Автоозвучка английских слов", Modifier.weight(1f))
                Switch(checked = s.autoSpeak, onCheckedChange = { vm.setAutoSpeak(it) })
            }
            ChipRow {
                listOf(0 to "Системная", 1 to "Светлая", 2 to "Тёмная").forEach { (v, t) ->
                    FilterChip(selected = s.theme == v, onClick = { vm.setTheme(v) }, label = { Text(t) })
                }
            }
        }

        Section("Напоминание") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ежедневное напоминание", Modifier.weight(1f))
                Switch(checked = s.reminderOn, onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.setReminder(on, s.reminderHour)
                })
            }
            if (s.reminderOn) SliderRow("Время (час)", s.reminderHour, 6..23, 1) { vm.setReminder(true, it) }
        }

        Section("Свои слова (импорт CSV)") {
            Text(
                "Файл UTF-8, одна строка = одно слово:\nenglish;русский;транскрипция;пример EN;пример RU\n" +
                    "Обязательны только первые два поля. Строки с # игнорируются.",
                style = MaterialTheme.typography.bodySmall
            )
            ChipRow {
                Levels.all.forEach { l ->
                    FilterChip(selected = importLevel == l.id, onClick = { importLevel = l.id }, label = { Text(l.code) })
                }
            }
            Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Выбрать файл и добавить в уровень") }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        Section("Источники данных") {
            Text(
                "Переводы слов: OpenRussian.org (CC BY-SA 4.0).\n" +
                    "Примеры предложений: Tatoeba.org (CC BY 2.0 FR); часть примеров для редких слов — простые шаблонные фразы.\n" +
                    "Частотность: wordfreq. Транскрипция: CMU Pronouncing Dictionary.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Сбросить весь прогресс")
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить прогресс?") },
            text = { Text("Все слова вернутся в состояние «новые». Статистика дней сохранится.") },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text("Сбросить") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } }
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

/** Ползунок с локальным значением; в настройки пишем только при отпускании. */
@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, step: Int, onCommit: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text("$label: ${v.roundToInt()}")
    Slider(
        value = v, onValueChange = { v = it },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
        onValueChangeFinished = { onCommit(v.roundToInt()) }
    )
}
