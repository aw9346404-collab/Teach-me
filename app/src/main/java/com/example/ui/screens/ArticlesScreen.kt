package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyArticle
import com.example.data.model.ErrorSeverity
import com.example.data.model.LearnerProfile
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalBayanColors
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.AiActionState

@Composable
fun ArticlesScreen(
    profile: LearnerProfile,
    articles: List<DailyArticle>,
    articleGenState: AiActionState<DailyArticle>,
    isSpeaking: Boolean,
    speakingRate: Float,
    strings: AppStrings,
    onSpeakText: (String) -> Unit,
    onStopAudio: () -> Unit,
    onChangeRate: (Float) -> Unit,
    onGenerateAdaptiveArticle: () -> Unit,
    onSubmitQuiz: (DailyArticle, Map<String, Int>) -> Unit
) {
    val semantic = LocalBayanColors.current
    var selectedArticleId by remember(articles) {
        mutableStateOf(articles.firstOrNull()?.id ?: "")
    }
    val activeArticle = articles.firstOrNull { it.id == selectedArticleId } ?: articles.first()

    var readingFontSp by remember { mutableIntStateOf(17) }
    val selectedAnswers = remember(activeArticle.id) { mutableStateMapOf<String, Int>() }
    var quizSubmitted by remember(activeArticle.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.dailyArticlesTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    OutlinedButton(
                        onClick = onGenerateAdaptiveArticle,
                        enabled = articleGenState !is AiActionState.Loading,
                        modifier = Modifier.testTag("generate_adaptive_article_button")
                    ) {
                        if (articleGenState is AiActionState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.generateAdaptiveArticle)
                    }
                }

                if (articleGenState is AiActionState.Error) {
                    Surface(color = semantic.criticalRedBg, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = articleGenState.message,
                            color = semantic.criticalRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(articles) { art ->
                        FilterChip(
                            selected = art.id == activeArticle.id,
                            onClick = { selectedArticleId = art.id },
                            label = {
                                Text("${art.cefrLevel} • ${art.titleEn.take(24)}${if (art.titleEn.length > 24) "…" else ""}")
                            }
                        )
                    }
                }
            }
        }

        // Main Reading & Listening Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .border(1.dp, semantic.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${activeArticle.cefrLevel} • ${activeArticle.category} • ${activeArticle.readingMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 17, 20).forEach { sz ->
                                FilterChip(
                                    selected = readingFontSp == sz,
                                    onClick = { readingFontSp = sz },
                                    label = { Text("${sz}pt") }
                                )
                            }
                        }
                    }

                    Text(
                        text = activeArticle.titleEn,
                        fontFamily = PlayfairDisplayFontFamily,
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "${activeArticle.titleAr} — ${activeArticle.summaryAr}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Synchronized Audio Controls
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    if (isSpeaking) onStopAudio()
                                    else onSpeakText("${activeArticle.titleEn}. ${activeArticle.paragraphs.joinToString(" ")}")
                                },
                                modifier = Modifier.testTag("listen_article_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(if (isSpeaking) strings.stopAudio else strings.listenArticle)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(0.8f to "0.8x", 0.95f to "1.0x", 1.2f to "1.2x").forEach { (rate, label) ->
                                    FilterChip(
                                        selected = kotlin.math.abs(speakingRate - rate) < 0.08f,
                                        onClick = { onChangeRate(rate) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }

                    // Paragraphs (Tap any paragraph to hear it aloud)
                    activeArticle.paragraphs.forEachIndexed { idx, para ->
                        Surface(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSpeakText(para) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = para,
                                    fontSize = readingFontSp.sp,
                                    lineHeight = (readingFontSp + 9).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onSpeakText(para) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "Read paragraph",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Key Phrases
                    if (activeArticle.keyPhrases.isNotEmpty()) {
                        Text(
                            text = if (strings.isArabic) "تعابير مفتاحية من المقال:" else "Key Collocations & Phrases:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        activeArticle.keyPhrases.forEach { (phrase, meaningAr) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeakText(phrase) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• $phrase",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = meaningAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Quiz Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .border(1.dp, semantic.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (strings.isArabic) "اختبار الفهم والتطبيق التفاعلي" else "Interactive Comprehension Quiz",
                        style = MaterialTheme.typography.titleLarge
                    )

                    activeArticle.quiz.forEachIndexed { qIdx, q ->
                        val chosen = selectedAnswers[q.id]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${qIdx + 1}. ${q.promptEn}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = q.promptAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            q.options.forEachIndexed { optIdx, opt ->
                                val isSelected = chosen == optIdx
                                val isCorrectOpt = optIdx == q.correctIndex
                                val bgColor = when {
                                    !quizSubmitted -> MaterialTheme.colorScheme.surface
                                    isCorrectOpt -> semantic.masteryGreenBg
                                    isSelected && !isCorrectOpt -> {
                                        if (q.errorSeverityIfWrong == ErrorSeverity.RED_CRITICAL) semantic.criticalRedBg
                                        else semantic.minorOrangeBg
                                    }
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                Surface(
                                    color = bgColor,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !quizSubmitted) {
                                            selectedAnswers[q.id] = optIdx
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { if (!quizSubmitted) selectedAnswers[q.id] = optIdx }
                                        )
                                        Text(
                                            text = opt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            if (quizSubmitted) {
                                val isRight = chosen == q.correctIndex
                                Surface(
                                    color = if (isRight) semantic.masteryGreenBg
                                    else if (q.errorSeverityIfWrong == ErrorSeverity.RED_CRITICAL) semantic.criticalRedBg
                                    else semantic.minorOrangeBg,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (strings.isArabic) q.explanationAr else q.explanationEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isRight) semantic.masteryGreen
                                        else if (q.errorSeverityIfWrong == ErrorSeverity.RED_CRITICAL) semantic.criticalRed
                                        else semantic.minorOrange,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            quizSubmitted = true
                            onSubmitQuiz(activeArticle, selectedAnswers.toMap())
                        },
                        enabled = !quizSubmitted && selectedAnswers.size == activeArticle.quiz.size,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_quiz_button")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(strings.submitQuiz)
                    }
                }
            }
        }
    }
}
