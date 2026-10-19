package com.example.wordcards.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.wordcards.R

/** Экран «Как это работает»: правила приложения, порядок появления слов, назначение кнопок. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    // Пары «заголовок — текст» из строковых ресурсов (переводятся вместе с интерфейсом)
    val sections = listOf(
        R.string.help_1_t to R.string.help_1_b, R.string.help_2_t to R.string.help_2_b,
        R.string.help_3_t to R.string.help_3_b, R.string.help_4_t to R.string.help_4_b,
        R.string.help_5_t to R.string.help_5_b, R.string.help_6_t to R.string.help_6_b,
        R.string.help_7_t to R.string.help_7_b, R.string.help_8_t to R.string.help_8_b,
        R.string.help_9_t to R.string.help_9_b, R.string.help_10_t to R.string.help_10_b
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.help_title), style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sections.forEachIndexed { i, (t, b) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${i + 1}. ${stringResource(t)}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(b), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
