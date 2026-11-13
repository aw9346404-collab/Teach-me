package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ReadingThemeMode(val id: String) {
    WARM_PAPER("WARM_PAPER"),
    EYE_COMFORT("EYE_COMFORT"),
    CALM_DARK("CALM_DARK");

    companion object {
        fun fromId(id: String): ReadingThemeMode =
            entries.firstOrNull { it.id == id } ?: WARM_PAPER
    }
}

enum class ErrorSeverity(val id: String) {
    RED_CRITICAL("RED_CRITICAL"),
    ORANGE_MINOR("ORANGE_MINOR"),
    NONE("NONE");

    companion object {
        fun fromId(id: String): ErrorSeverity =
            entries.firstOrNull { it.id == id } ?: ORANGE_MINOR
    }
}

@IgnoreExtraProperties
data class LearnerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val cefrLevel: String = "B1",
    val learningGoal: String = "Master B2 Academic & Everyday Fluency",
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    val articlesRead: Long = 0L,
    val quizzesCompleted: Long = 0L,
    val voiceSessionsCount: Long = 0L,
    val readingScore: Long = 55L,
    val listeningScore: Long = 52L,
    val speakingScore: Long = 50L,
    val grammarScore: Long = 54L,
    val vocabularyScore: Long = 58L,
    val preferredLang: String = "ar",
    val translationLang: String = "ar",
    val readingTheme: String = ReadingThemeMode.WARM_PAPER.id,
    val reminderEnabled: Boolean = true,
    val reminderHour: Long = 19L,
    val reminderMinute: Long = 30L,
    val guardianPin: String = "",
    val earnedBadges: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MistakeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceType: String = "VOICE",
    val originalText: String = "",
    val correctedText: String = "",
    val errorSpan: String = "",
    val severity: String = ErrorSeverity.ORANGE_MINOR.id,
    val category: String = "GRAMMAR",
    val explanationAr: String = "",
    val explanationEn: String = "",
    val mastered: Boolean = false,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AssessmentLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activityType: String = "QUIZ",
    val title: String = "",
    val score: Long = 0L,
    val xpEarned: Long = 0L,
    val redErrors: Long = 0L,
    val orangeErrors: Long = 0L,
    val cefrSnapshot: String = "B1",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WeeklyChallenge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weekKey: String = "",
    val title: String = "",
    val description: String = "",
    val targetCount: Long = 1L,
    val currentCount: Long = 0L,
    val xpReward: Long = 100L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val cefrLevel: String = "B1",
    val xp: Long = 0L,
    val weeklyPoints: Long = 0L,
    val streakDays: Long = 1L,
    val badgeTitle: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class QuizQuestion(
    val id: String,
    val promptEn: String,
    val promptAr: String,
    val options: List<String>,
    val correctIndex: Int,
    val errorSeverityIfWrong: ErrorSeverity,
    val explanationAr: String,
    val explanationEn: String,
    val category: String
)

data class DailyArticle(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val cefrLevel: String,
    val category: String,
    val readingMinutes: Int,
    val summaryAr: String,
    val paragraphs: List<String>,
    val keyPhrases: List<Pair<String, String>>,
    val quiz: List<QuizQuestion>,
    val isAiGenerated: Boolean = false
)

data class VocabWord(
    val id: String,
    val word: String,
    val ipa: String,
    val partOfSpeech: String,
    val cefrLevel: String,
    val definitionEn: String,
    val translations: Map<String, String>,
    val exampleEn: String,
    val collocations: List<String>
)

data class AnalyzedSegment(
    val text: String,
    val severity: ErrorSeverity,
    val correction: String = "",
    val category: String = "GRAMMAR",
    val explanationAr: String = "",
    val explanationEn: String = ""
)

data class DetectedMistakeInput(
    val sourceType: String,
    val originalText: String,
    val correctedText: String,
    val errorSpan: String,
    val severity: ErrorSeverity,
    val category: String,
    val explanationAr: String,
    val explanationEn: String
)

data class SpeechEvaluationResult(
    val promptUsed: String,
    val learnerText: String,
    val overallScore: Int,
    val pronunciationScore: Int,
    val grammarScore: Int,
    val segments: List<AnalyzedSegment>,
    val correctedFullText: String,
    val praiseAr: String,
    val praiseEn: String,
    val nextTipAr: String,
    val nextTipEn: String,
    val mistakes: List<DetectedMistakeInput>
)

data class TutorChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

data class LearningPathStep(
    val id: String,
    val stage: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val skillTag: String,
    val xpReward: Int,
    val targetTab: String,
    val isCompleted: Boolean,
    val isRecommendedNext: Boolean
)

data class BadgeInfo(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val earned: Boolean
)
