package com.example.wordcards.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.data.*
import java.time.LocalDate

/** Главный экран: серия, цель дня, выбор режима и уровни. */
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel(), onStart: (Int, StudyMode) -> Unit) {
    val levels by vm.levels.collectAsState()
    val recent by vm.recent.collectAsState()
    val st by vm.settings.collectAsState()
    var mode by rememberSaveable { mutableStateOf(StudyMode.CARDS) }

    val reviewedToday = recent.firstOrNull { it.date == LocalDate.now().toString() }?.reviewed ?: 0
    val streak = Repository.streak(recent)
    val totalDue = levels.sumOf { it.due }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF6D00))
                        Spacer(Modifier.width(8.dp))
                        Text("Серия: $streak дн.", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Цель дня: $reviewedToday из ${st.dailyGoal}")
                    LinearProgressIndicator(
                        progress = { (reviewedToday / st.dailyGoal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("К повторению сейчас: $totalDue", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Button(onClick = { onStart(0, mode) }, modifier = Modifier.fillMaxWidth()) {
                Text("Начать занятие (все уровни)")
            }
        }
        item {
            Text("Режим тренировки", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyMode.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.title) })
                }
            }
        }
        items(Levels.all, key = { it.id }) { info ->
            LevelCard(info, levels.firstOrNull { it.level == info.id }) { onStart(info.id, mode) }
        }
    }
}

@Composable
private fun LevelCard(info: LevelInfo, s: LevelStat?, onClick: () -> Unit) {
    val total = s?.total ?: 0
    val learned = s?.learned ?: 0
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(info.code, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(10.dp))
                Text(info.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if ((s?.due ?: 0) > 0) {
                    Badge { Text("${s?.due}") }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(info.desc, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else learned / total.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text("Выучено $learned из $total · новых ${s?.fresh ?: 0}", style = MaterialTheme.typography.labelMedium)
        }
    }
}
