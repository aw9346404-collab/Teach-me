package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.ReportExporter
import com.example.core.SmartReminderScheduler
import com.example.data.model.AssessmentLog
import com.example.data.model.ErrorSeverity
import com.example.data.model.LearnerProfile
import com.example.data.model.MistakeRecord
import com.example.data.model.ReadingThemeMode
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalBayanColors

@Composable
fun ProgressAndPortalScreen(
    profile: LearnerProfile,
    mistakes: List<MistakeRecord>,
    assessments: List<AssessmentLog>,
    strings: AppStrings,
    onSpeakText: (String) -> Unit,
    onReviewMistake: (MistakeRecord, Boolean) -> Unit,
    onSelectTheme: (ReadingThemeMode) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSaveGoalAndReminder: (String, Boolean, Int, Int) -> Unit,
    onUpdateGuardianPin: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalBayanColors.current
    var activeSubTab by remember { mutableIntStateOf(0) }
    var mistakeFilter by remember { mutableStateOf("PENDING") }

    var goalDraft by remember(profile.learningGoal) { mutableStateOf(profile.learningGoal) }
    var reminderEnabled by remember(profile.reminderEnabled) { mutableStateOf(profile.reminderEnabled) }
    var reminderHour by remember(profile.reminderHour) { mutableIntStateOf(profile.reminderHour.toInt()) }
    var reminderMinute by remember(profile.reminderMinute) { mutableIntStateOf(profile.reminderMinute.toInt()) }
    var newPinDraft by remember { mutableStateOf("") }
    var portalUnlocked by remember(profile.guardianPin) { mutableStateOf(profile.guardianPin.isBlank()) }
    var pinInput by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            SmartReminderScheduler.scheduleDailyReminder(
                context = context,
                enabled = reminderEnabled,
                hour = reminderHour,
                minute = reminderMinute,
                pendingMistakesCount = mistakes.count { !it.mastered },
                streakDays = profile.streakDays.toInt()
            )
            SmartReminderScheduler.sendImmediatePreviewNotification(
                context = context,
                pendingMistakesCount = mistakes.count { !it.mastered },
                streakDays = profile.streakDays.toInt()
            )
        }
    }

    fun triggerReminderAction(previewNow: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            SmartReminderScheduler.scheduleDailyReminder(
                context = context,
                enabled = reminderEnabled,
                hour = reminderHour,
                minute = reminderMinute,
                pendingMistakesCount = mistakes.count { !it.mastered },
                streakDays = profile.streakDays.toInt()
            )
            if (previewNow) {
                SmartReminderScheduler.sendImmediatePreviewNotification(
                    context = context,
                    pendingMistakesCount = mistakes.count { !it.mastered },
                    streakDays = profile.streakDays.toInt()
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeSubTab) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text(strings.mistakesBankTitle) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text(if (strings.isArabic) "بوابة التقارير والإعدادات" else "Portal & Settings") }
            )
        }

        if (activeSubTab == 0) {
            val filteredMistakes = remember(mistakes, mistakeFilter) {
                when (mistakeFilter) {
                    "PENDING" -> mistakes.filter { !it.mastered }
                    "RED" -> mistakes.filter { it.severity == ErrorSeverity.RED_CRITICAL.id }
                    "ORANGE" -> mistakes.filter { it.severity == ErrorSeverity.ORANGE_MINOR.id }
                    "MASTERED" -> mistakes.filter { it.mastered }
                    else -> mistakes
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
                            text = strings.mistakesBankTitle,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val filters = listOf(
                                "PENDING" to (if (strings.isArabic) "بانتظار المراجعة" else "Pending"),
                                "RED" to (if (strings.isArabic) "أخطاء حمراء (جوهرية)" else "Red Critical"),
                                "ORANGE" to (if (strings.isArabic) "ملاحظات برتقالية (طفيفة)" else "Orange Minor"),
                                "MASTERED" to (if (strings.isArabic) "تم إتقانها" else "Mastered"),
                                "ALL" to (if (strings.isArabic) "الكل" else "All")
                            )
                            items(filters) { (key, label) ->
                                FilterChip(
                                    selected = mistakeFilter == key,
                                    onClick = { mistakeFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                if (filteredMistakes.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .widthIn(max = 760.dp)
                                .fillMaxWidth()
                                .border(1.dp, semantic.cardBorder, RoundedCornerShape(18.dp))
                        ) {
                            Text(
                                text = if (strings.isArabic) "لا توجد أخطاء في هذا التصنيف حالياً. واصل المقالات وجلسات النطق!" else "No mistakes in this filter yet. Keep practicing articles and voice sessions!",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(filteredMistakes, key = { it.id }) { m ->
                        val isRed = m.severity == ErrorSeverity.RED_CRITICAL.id
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isRed) semantic.criticalRedBg else semantic.minorOrangeBg,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (isRed) "أحمر • خطأ يغيّر المعنى (${m.category})" else "برتقالي • هفوة طفيفة (${m.category})",
                                            color = if (isRed) semantic.criticalRed else semantic.minorOrange,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(onClick = { onSpeakText(m.correctedText) }) {
                                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear correct form")
                                    }
                                }

                                Text(
                                    text = "الأصل: ${m.originalText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isRed) semantic.criticalRed else semantic.minorOrange
                                )
                                Text(
                                    text = "التصحيح: ${m.correctedText}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = semantic.masteryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (strings.isArabic) m.explanationAr else m.explanationEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!m.mastered) {
                                        Button(
                                            onClick = { onReviewMistake(m, true) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(strings.markMastered)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onReviewMistake(m, false) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(strings.practiceAgain)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Teacher & Parent Portal + Reports + Settings & Smart Reminders
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Teacher & Parent Portal
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .border(1.dp, semantic.cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = strings.teacherParentPortalTitle,
                                style = MaterialTheme.typography.titleLarge
                            )

                            if (!portalUnlocked) {
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { pinInput = it },
                                    label = { Text(if (strings.isArabic) "أدخل رمز المعلم / ولي الأمر" else "Enter Teacher/Parent PIN") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (pinInput == profile.guardianPin) portalUnlocked = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (strings.isArabic) "فتح بوابة التقارير" else "Unlock Portal")
                                }
                            } else {
                                val redTotal = mistakes.count { it.severity == ErrorSeverity.RED_CRITICAL.id }
                                val orangeTotal = mistakes.count { it.severity == ErrorSeverity.ORANGE_MINOR.id }
                                val masteredTotal = mistakes.count { it.mastered }

                                Text(
                                    text = if (strings.isArabic) {
                                        "ملخص المتابعة الدورية: المستوى ${profile.cefrLevel} • نقاط ${profile.xp} XP • المقالات ${profile.articlesRead} • جلسات النطق ${profile.voiceSessionsCount}\n" +
                                            "الأخطاء الجوهرية (أحمر): $redTotal | الطفيفة (برتقالي): $orangeTotal | المتقَنة: $masteredTotal"
                                    } else {
                                        "Periodic Overview: ${profile.cefrLevel} • ${profile.xp} XP • Articles ${profile.articlesRead} • Voice ${profile.voiceSessionsCount}\n" +
                                            "Red Critical: $redTotal | Orange Minor: $orangeTotal | Mastered: $masteredTotal"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            ReportExporter.exportAndSharePdfReport(
                                                context = context,
                                                profile = profile,
                                                mistakes = mistakes,
                                                assessments = assessments
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_pdf_button")
                                    ) {
                                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(strings.exportPdfReport)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            ReportExporter.shareTextSummaryForLms(context, profile, mistakes)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("share_lms_button")
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(strings.shareLmsSummary)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { ReportExporter.addStudySessionToCalendar(context, profile) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(strings.addCalendarStudy)
                                }

                                if (assessments.isNotEmpty()) {
                                    Text(
                                        text = if (strings.isArabic) "أحدث سجلات التقييم المستمر:" else "Recent Assessment Log:",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    assessments.take(5).forEach { log ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "[${log.activityType}] ${log.title.take(34)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "${log.score}% • +${log.xpEarned} XP",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newPinDraft,
                                        onValueChange = { newPinDraft = it },
                                        label = { Text(if (strings.isArabic) "رمز قفل البوابة (اختياري)" else "Set Portal PIN (optional)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = { onUpdateGuardianPin(newPinDraft) }) {
                                        Text(if (strings.isArabic) "حفظ" else "Save")
                                    }
                                }
                            }
                        }
                    }
                }

                // Preferences, Eye-Comfort Theme & Smart Reminders
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .border(1.dp, semantic.cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = strings.readingModeLabel,
                                style = MaterialTheme.typography.titleMedium
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val themes = listOf(
                                    ReadingThemeMode.WARM_PAPER to (if (strings.isArabic) "ورق دافئ" else "Warm Paper"),
                                    ReadingThemeMode.EYE_COMFORT to (if (strings.isArabic) "راحة العين" else "Eye-Comfort"),
                                    ReadingThemeMode.CALM_DARK to (if (strings.isArabic) "ليلي هادئ" else "Calm Dark")
                                )
                                items(themes) { (mode, label) ->
                                    FilterChip(
                                        selected = profile.readingTheme == mode.id,
                                        onClick = { onSelectTheme(mode) },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Text(
                                text = strings.languageSwitchLabel,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = profile.preferredLang == "ar",
                                    onClick = { onSelectLanguage("ar") },
                                    label = { Text("العربية") }
                                )
                                FilterChip(
                                    selected = profile.preferredLang == "en",
                                    onClick = { onSelectLanguage("en") },
                                    label = { Text("English") }
                                )
                            }

                            OutlinedTextField(
                                value = goalDraft,
                                onValueChange = { goalDraft = it },
                                label = { Text(if (strings.isArabic) "هدفك الشخصي" else "Personal Learning Goal") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(strings.smartRemindersLabel, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = reminderEnabled,
                                    onCheckedChange = { reminderEnabled = it }
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(8 to 30, 17 to 0, 19 to 30, 21 to 0)) { (h, m) ->
                                    FilterChip(
                                        selected = reminderHour == h && reminderMinute == m,
                                        onClick = {
                                            reminderHour = h
                                            reminderMinute = m
                                        },
                                        label = { Text("${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}") }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onSaveGoalAndReminder(goalDraft, reminderEnabled, reminderHour, reminderMinute)
                                        triggerReminderAction(previewNow = false)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (strings.isArabic) "حفظ الإعدادات" else "Save Preferences")
                                }
                                OutlinedButton(
                                    onClick = { triggerReminderAction(previewNow = true) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(strings.testReminderNow)
                                }
                            }

                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(strings.signOutLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}
