package com.semenenko.wordcards.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.semenenko.wordcards.R

/** Окно оценки: пять звёзд. Нажатие на звезду сразу отправляет оценку. */
@Composable
fun RatingDialog(onLater: () -> Unit, onRate: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.rating_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.rating_text, stringResource(R.string.app_name)),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { i ->
                        IconButton(onClick = { onRate(i) }) {
                            Icon(
                                Icons.Default.Star, stringResource(R.string.rating_star_cd, i),
                                Modifier.size(40.dp), tint = Color(0xFFFFB300)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.rating_later)) } }
    )
}

/** Куда ведут оценки: 4–5 звёзд — страница приложения в RuStore, 1–3 — письмо разработчику. */
object Feedback {
    const val EMAIL = "vl.profits@gmail.com"

    /** Страница приложения в RuStore (сначала через приложение RuStore, затем через сайт). */
    fun openStore(ctx: Context) {
        val pkg = ctx.packageName
        val links = listOf("rustore://apps.rustore.ru/app/$pkg", "https://apps.rustore.ru/app/$pkg")
        for (link in links) {
            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))); return }
            catch (e: ActivityNotFoundException) { /* пробуем следующую ссылку */ }
        }
    }

    /** Письмо разработчику с темой, в которой указана оценка. */
    fun sendMail(ctx: Context, stars: Int) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.mail_subject, stars))
            putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.mail_body))
        }
        try { ctx.startActivity(intent) }
        catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, ctx.getString(R.string.mail_no_client, EMAIL), Toast.LENGTH_LONG).show()
        }
    }
}
