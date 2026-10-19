package com.example.wordcards.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.data.*

/** Словарь: поиск, фильтр по уровню, избранное, отметка «знаю», озвучивание слов и предложений. */
@Composable
fun WordsScreen(vm: WordsViewModel = viewModel()) {
    val words by vm.words.collectAsState()
    val q by vm.query.collectAsState()
    val level by vm.level.collectAsState()
    val fav by vm.favOnly.collectAsState()
    val app = rememberApp()
    val learn = app.settings.flow.collectAsState(initial = AppSettings()).value.learning
    val speaker = app.speaker

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = q, onValueChange = { vm.query.value = it },
            label = { Text(stringResource(R.string.words_search)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = level == 0, onClick = { vm.level.value = 0 }, label = { Text(stringResource(R.string.words_all)) })
            Levels.all.forEach { l ->
                FilterChip(selected = level == l.id, onClick = { vm.level.value = l.id }, label = { Text(l.code) })
            }
            FilterChip(selected = fav, onClick = { vm.favOnly.value = !fav }, label = { Text(stringResource(R.string.words_fav)) })
        }
        Text(
            if (words.size >= 2000) stringResource(R.string.words_limit) else stringResource(R.string.words_count, words.size),
            Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp), style = MaterialTheme.typography.labelMedium
        )
        // Короткое пояснение значков
        Text(stringResource(R.string.words_hint), Modifier.padding(16.dp, 2.dp, 16.dp, 8.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        LazyColumn(Modifier.fillMaxSize()) {
            items(words, key = { it.id }) { w ->
                WordRow(w, learn,
                    onSpeak = { speaker.speak(w.target(learn), learn) },
                    onSpeakExample = { speaker.speak(w.targetExample(learn), learn) },
                    onFav = { vm.toggleFav(w) }, onKnown = { vm.toggleKnown(w) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WordRow(w: Word, l: LearnLang, onSpeak: () -> Unit, onSpeakExample: () -> Unit, onFav: () -> Unit, onKnown: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            // Сверху — слово изучаемого языка, под ним — перевод
            Text(w.target(l), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (l == LearnLang.EN && w.transcription.isNotBlank())
                Text(w.transcription, style = MaterialTheme.typography.bodySmall)
            Text(w.native(l), style = MaterialTheme.typography.bodyLarge)
            if (w.targetExample(l).isNotBlank()) {
                // Нажатие на пример — озвучивание предложения
                Row(Modifier.clickable(onClick = onSpeakExample).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.study_speak_sentence_cd), Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(w.targetExample(l), style = MaterialTheme.typography.bodySmall)
                        if (w.nativeExample(l).isNotBlank())
                            Text(w.nativeExample(l), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.words_cd_speak)) }
        IconButton(onClick = onFav) {
            Icon(if (w.favorite) Icons.Default.Star else Icons.Default.StarBorder, stringResource(R.string.words_cd_fav),
                tint = if (w.favorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline)
        }
        IconButton(onClick = onKnown) {
            Icon(Icons.Default.CheckCircle, stringResource(R.string.words_cd_known),
                tint = if (w.known) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
