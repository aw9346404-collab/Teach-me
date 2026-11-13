package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LearnerProfile
import com.example.data.model.ReadingThemeMode
import com.example.domain.dashboard.StudentDashboardModel
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalBayanColors

@Composable
fun DashboardScreen(
    profile: LearnerProfile,
    dashboard: StudentDashboardModel,
    strings: AppStrings,
    onSelectTheme: (ReadingThemeMode) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onNavigateTab: (String) -> Unit
) {
    val semantic = LocalBayanColors.current
    val personalizedPath = dashboard.personalizedPath
    val badges = dashboard.badges
    val challenges = dashboard.weeklyChallenges
    val leaderboard = dashboard.leaderboard
    val pendingMistakes = dashboard.errorBalance.pendingCount
    val avgScore = dashboard.overallReadinessPercent.toFloat()
    val readinessProgress = (avgScore / 100f).coerceIn(0.05f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick calm reading mode & language switcher bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (strings.isArabic) "أهلاً، ${profile.displayName}" else "Welcome, ${profile.displayName}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.subtitleClassic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = profile.readingTheme == ReadingThemeMode.EYE_COMFORT.id,
                            onClick = {
                                val next = if (profile.readingTheme == ReadingThemeMode.EYE_COMFORT.id) {
                                    ReadingThemeMode.WARM_PAPER
                                } else {
                                    ReadingThemeMode.EYE_COMFORT
                                }
                                onSelectTheme(next)
                            },
                            label = {
                                Text(if (strings.isArabic) "راحة العين" else "Eye Care")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("toggle_eye_comfort_chip")
                        )
                        FilterChip(
                            selected = profile.preferredLang == "en",
                            onClick = {
                                onToggleLanguage(if (profile.preferredLang == "ar") "en" else "ar")
                            },
                            label = {
                                Text(if (profile.preferredLang == "ar") "EN" else "عربي")
                            },
                            modifier = Modifier.testTag("toggle_language_chip")
                        )
                    }
                }

                // Classic Hero Card with CEFR Readiness
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, semantic.cardBorder, RoundedCornerShape(22.dp))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_study),
                                contentDescription = "Study desk banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "CEFR ${profile.cefrLevel}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.cefrPathTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${avgScore.toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { readinessProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricBadgeBox(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Rounded.LocalFireDepartment,
                                    tint = semantic.amberAccent,
                                    bg = semantic.amberAccentBg,
                                    value = "${profile.streakDays}",
                                    label = strings.dailyStreakLabel
                                )
                                MetricBadgeBox(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Rounded.AutoAwesome,
                                    tint = MaterialTheme.colorScheme.primary,
                                    bg = MaterialTheme.colorScheme.primaryContainer,
                                    value = "${profile.xp} XP",
                                    label = strings.totalXpLabel
                                )
                                MetricBadgeBox(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Rounded.EmojiEvents,
                                    tint = if (pendingMistakes > 0) semantic.minorOrange else semantic.masteryGreen,
                                    bg = if (pendingMistakes > 0) semantic.minorOrangeBg else semantic.masteryGreenBg,
                                    value = "$pendingMistakes",
                                    label = if (strings.isArabic) "أخطاء للمراجعة" else "To Review"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personalized Learning Path Section
        item {
            Column(
                modifier = Modifier.widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.personalizedPathHeader,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                personalizedPath.forEach { step ->
                    Surface(
                        color = if (step.isRecommendedNext) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (step.isRecommendedNext) 1.5.dp else 1.dp,
                                color = if (step.isRecommendedNext) MaterialTheme.colorScheme.primary else semantic.cardBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onNavigateTab(step.targetTab) }
                            .testTag("path_step_${step.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (step.isCompleted) semantic.masteryGreenBg
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (step.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = semantic.masteryGreen
                                    )
                                } else {
                                    Text(
                                        text = step.stage,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                if (step.isRecommendedNext) {
                                    Text(
                                        text = if (strings.isArabic) "★ الخطوة الموصى بها لك الآن" else "★ Recommended Next Step",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (strings.isArabic) step.titleAr else step.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (strings.isArabic) step.subtitleAr else step.subtitleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${step.xpReward} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = semantic.amberAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Open step",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Continuous Linguistic Skill Assessment
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.skillsOverview,
                        style = MaterialTheme.typography.titleLarge
                    )
                    dashboard.skills.forEach { skill ->
                        val focusTag = if (skill.isFocusSkill) {
                            if (strings.isArabic) " • أولوية التطوير" else " • Focus Area"
                        } else ""
                        SkillBarRow(
                            label = (if (strings.isArabic) skill.labelAr else skill.labelEn) + focusTag,
                            score = skill.score
                        )
                    }
                }
            }
        }

        // Rewards & Badges
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (strings.isArabic) "الأوسمة والمكافآت" else "Academic Badges & Rewards",
                    style = MaterialTheme.typography.titleLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(badges) { badge ->
                        Surface(
                            color = if (badge.earned) semantic.amberAccentBg else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (badge.earned) semantic.amberAccent else semantic.cardBorder,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .widthIn(min = 135.dp, max = 165.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MilitaryTech,
                                    contentDescription = null,
                                    tint = if (badge.earned) semantic.amberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (strings.isArabic) badge.titleAr else badge.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (strings.isArabic) badge.descriptionAr else badge.descriptionEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weekly Interactive Challenges
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
                        text = strings.weeklyChallengesTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (challenges.isEmpty()) {
                        Text(
                            text = if (strings.isArabic) "جاري تجهيز تحديات الأسبوع..." else "Preparing weekly challenges...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        challenges.forEach { ch ->
                            val prog = (ch.currentCount.toFloat() / ch.targetCount.coerceAtLeast(1L)).coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ch.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (ch.completed) "✓ +${ch.xpReward} XP" else "${ch.currentCount}/${ch.targetCount} (+${ch.xpReward} XP)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (ch.completed) semantic.masteryGreen else semantic.amberAccent
                                    )
                                }
                                Text(
                                    text = ch.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearProgressIndicator(
                                    progress = { prog },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = if (ch.completed) semantic.masteryGreen else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Leaderboard
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.leaderboardTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (leaderboard.isEmpty()) {
                        Text(
                            text = if (strings.isArabic) "أكمل أول نشاط لتتصدر القائمة!" else "Complete your first activity to appear on the board.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        leaderboard.forEachIndexed { index, entry ->
                            val isMe = entry.userId == profile.userId
                            Surface(
                                color = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = if (isMe) "${entry.displayName} (${if (strings.isArabic) "أنت" else "You"})" else entry.displayName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${entry.cefrLevel} • ${entry.badgeTitle} • ${entry.streakDays}d",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${entry.xp} XP",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.amberAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadgeBox(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
    value: String,
    label: String
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SkillBarRow(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$score%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
