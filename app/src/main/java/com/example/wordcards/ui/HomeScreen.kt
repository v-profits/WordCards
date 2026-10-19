package com.example.wordcards.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.data.*
import java.time.LocalDate

/** Главный экран: серия, цель дня, пояснение порядка занятия, выбор режима и уровни. */
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel(), onHelp: () -> Unit, onStart: (Int, StudyMode, SessionKind) -> Unit) {
    val levels by vm.levels.collectAsState()
    val recent by vm.recent.collectAsState()
    val st by vm.settings.collectAsState()
    val seeding by vm.seeding.collectAsState()
    var mode by rememberSaveable { mutableStateOf(StudyMode.CARDS) }
    var kind by rememberSaveable { mutableStateOf(SessionKind.LEARN) }

    val reviewedToday = recent.firstOrNull { it.date == LocalDate.now().toString() }?.reviewed ?: 0
    val streak = Repository.streak(recent)
    val totalDue = levels.sumOf { it.due }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.home_studying, langName(st.learning)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                    )
                }
                // Кнопка со справкой «Как это работает»
                FilledTonalButton(onClick = onHelp, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_help))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF6D00))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_streak, streak), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.home_goal, reviewedToday, st.dailyGoal))
                    LinearProgressIndicator(
                        progress = { (reviewedToday / st.dailyGoal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.home_due_now, totalDue), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (seeding) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.home_loading))
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
        item {
            Button(onClick = { onStart(0, mode, kind) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_start))
            }
            // Объясняем, что и в каком порядке появится в занятии
            Text(
                if (st.newPerDay == 0) stringResource(R.string.home_order_unlimited)
                else stringResource(R.string.home_order_limited, st.newPerDay),
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)
            )
        }
        item {
            Text(stringResource(R.string.home_what), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SessionKind.entries.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(stringResource(k.title)) })
                }
            }
            Text(stringResource(kind.hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Text(stringResource(R.string.home_mode), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyMode.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.title)) })
                }
            }
            Text(stringResource(mode.hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        items(Levels.all, key = { it.id }) { info ->
            LevelCard(info, levels.firstOrNull { it.level == info.id }) { onStart(info.id, mode, kind) }
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
                Text(stringResource(info.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if ((s?.due ?: 0) > 0) {
                    Badge { Text("${s?.due}") }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(info.desc), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else learned / total.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.level_progress, learned, total, s?.fresh ?: 0), style = MaterialTheme.typography.labelMedium)
        }
    }
}
