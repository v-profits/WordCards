package com.example.wordcards.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.data.Levels
import com.example.wordcards.data.Word

/** Словарь: поиск, фильтр по уровню, избранное, отметка «знаю», озвучивание. */
@Composable
fun WordsScreen(vm: WordsViewModel = viewModel()) {
    val words by vm.words.collectAsState()
    val q by vm.query.collectAsState()
    val level by vm.level.collectAsState()
    val fav by vm.favOnly.collectAsState()
    val speaker = rememberApp().speaker

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = q, onValueChange = { vm.query.value = it },
            label = { Text("Поиск слова") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = level == 0, onClick = { vm.level.value = 0 }, label = { Text("Все") })
            Levels.all.forEach { l ->
                FilterChip(selected = level == l.id, onClick = { vm.level.value = l.id }, label = { Text(l.code) })
            }
            FilterChip(selected = fav, onClick = { vm.favOnly.value = !fav }, label = { Text("★ Избранное") })
        }
        Text(if (words.size >= 2000) "Показано 2000 — уточните поиск или уровень" else "Слов: ${words.size}", Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.fillMaxSize()) {
            items(words, key = { it.id }) { w ->
                WordRow(w, onSpeak = { speaker.speak(w.en) },
                    onFav = { vm.toggleFav(w) }, onKnown = { vm.toggleKnown(w) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WordRow(w: Word, onSpeak: () -> Unit, onFav: () -> Unit, onKnown: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(w.en, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (w.transcription.isNotBlank())
                Text(w.transcription, style = MaterialTheme.typography.bodySmall)
            Text(w.ru, style = MaterialTheme.typography.bodyLarge)
            if (w.exampleEn.isNotBlank())
                Text(w.exampleEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Озвучить") }
        IconButton(onClick = onFav) {
            Icon(if (w.favorite) Icons.Default.Star else Icons.Default.StarBorder, "Избранное",
                tint = if (w.favorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline)
        }
        IconButton(onClick = onKnown) {
            Icon(Icons.Default.CheckCircle, "Знаю",
                tint = if (w.known) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
