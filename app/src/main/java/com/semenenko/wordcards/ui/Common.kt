package com.semenenko.wordcards.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.semenenko.wordcards.R
import com.semenenko.wordcards.data.LearnLang

/** Название языка на текущем языке интерфейса. */
@Composable
fun langName(l: LearnLang): String =
    stringResource(if (l == LearnLang.EN) R.string.lang_name_en else R.string.lang_name_ru)
