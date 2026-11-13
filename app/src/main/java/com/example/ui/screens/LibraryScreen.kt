package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.LearnerProfile
import com.example.data.repository.SmartLibrarySearchResult
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalBayanColors
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.AiActionState

@Composable
fun LibraryScreen(
    profile: LearnerProfile,
    smartSearchState: AiActionState<SmartLibrarySearchResult>,
    strings: AppStrings,
    onSpeakWord: (String) -> Unit,
    onSelectTranslationLang: (String) -> Unit,
    onRunSmartSearch: (String) -> Unit,
    onClearSmartSearch: () -> Unit
) {
    val semantic = LocalBayanColors.current
    var query by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf("ALL") }

    val aiMatchedIds = (smartSearchState as? AiActionState.Success)?.data?.matchedWordIds?.toSet()

    val filteredWords = remember(query, levelFilter, aiMatchedIds, profile.translationLang) {
        val base = CurriculumCatalog.vocabularyLibrary.filter { word ->
            levelFilter == "ALL" || word.cefrLevel == levelFilter
        }
        if (query.isBlank()) {
            base
        } else {
            val q = query.trim().lowercase()
            base.filter { w ->
                w.word.lowercase().contains(q) ||
                    w.definitionEn.lowercase().contains(q) ||
                    w.translations.values.any { it.lowercase().contains(q) } ||
                    (aiMatchedIds != null && w.id in aiMatchedIds)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (strings.isArabic) "مكتبة الكلمات الشائعة (B1–B2) والبحث الذكي" else "Common B1–B2 Vocabulary & Smart Search",
                    style = MaterialTheme.typography.headlineSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.isBlank()) onClearSmartSearch()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text(strings.smartSearchPlaceholder) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("library_search_input")
                    )
                    Button(
                        onClick = { onRunSmartSearch(query) },
                        enabled = query.isNotBlank() && smartSearchState !is AiActionState.Loading,
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("library_smart_search_button")
                    ) {
                        if (smartSearchState is AiActionState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(strings.smartSearchButton)
                        }
                    }
                }

                // CEFR filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ALL", "B1", "B1+", "B2")) { lvl ->
                        FilterChip(
                            selected = levelFilter == lvl,
                            onClick = { levelFilter = lvl },
                            label = { Text(if (lvl == "ALL") (if (strings.isArabic) "الكل" else "All") else lvl) }
                        )
                    }
                }

                // Helper translation language selector
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val langs = listOf(
                        "ar" to "العربية",
                        "fr" to "Français",
                        "es" to "Español",
                        "tr" to "Türkçe",
                        "de" to "Deutsch"
                    )
                    items(langs) { (code, name) ->
                        FilterChip(
                            selected = profile.translationLang == code,
                            onClick = { onSelectTranslationLang(code) },
                            label = { Text(name) }
                        )
                    }
                }

                // Smart Search AI Insight Card
                when (smartSearchState) {
                    is AiActionState.Error -> {
                        Surface(color = semantic.criticalRedBg, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                text = smartSearchState.message,
                                color = semantic.criticalRed,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    is AiActionState.Success -> {
                        val ai = smartSearchState.data
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (strings.isArabic) "تحليل البحث الذكي بالمعنى والسياق" else "AI Semantic Search Insight",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (strings.isArabic) ai.aiExplanationAr else ai.aiExplanationEn,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (ai.bonusWord.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "+ كلمة إضافية مقترحة: ${ai.bonusWord} ${ai.bonusIpa}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${ai.bonusMeaningAr} • ${ai.bonusExampleEn}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            IconButton(onClick = { onSpeakWord("${ai.bonusWord}. ${ai.bonusExampleEn}") }) {
                                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Speak bonus word")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        items(filteredWords, key = { it.id }) { word ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .border(1.dp, semantic.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = word.word,
                                fontFamily = PlayfairDisplayFontFamily,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${word.ipa}  •  ${word.partOfSpeech}  •  ${word.cefrLevel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onSpeakWord("${word.word}. ${word.exampleEn}") }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Pronounce ${word.word}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = word.translations[profile.translationLang] ?: word.translations["ar"].orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = word.definitionEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${word.exampleEn}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    if (word.collocations.isNotEmpty()) {
                        Text(
                            text = "Collocations: ${word.collocations.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
