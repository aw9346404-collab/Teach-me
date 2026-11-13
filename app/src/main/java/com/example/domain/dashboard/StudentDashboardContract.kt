package com.example.domain.dashboard

import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.AssessmentLog
import com.example.data.model.BadgeInfo
import com.example.data.model.ErrorSeverity
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LearnerProfile
import com.example.data.model.LearningPathStep
import com.example.data.model.MistakeRecord
import com.example.data.model.WeeklyChallenge
import com.example.data.repository.LearningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * نموذج بيانات المهارة الفردية ضمن لوحة تحكم الطالب.
 */
data class SkillProgressItem(
    val skillKey: String,
    val labelAr: String,
    val labelEn: String,
    val score: Int,
    val isFocusSkill: Boolean
)

/**
 * ملخص توازن الأخطاء المصنفة (الأحمر الجوهري والبرتقالي الطفيف).
 */
data class ErrorBalanceSummary(
    val pendingCount: Int = 0,
    val masteredCount: Int = 0,
    val criticalRedCount: Int = 0,
    val minorOrangeCount: Int = 0
)

/**
 * نموذج بيانات "لوحة تحكم الطالب" الموحد الذي يربط بيانات التقدم السحابي من Firestore
 * بمسارات التعلم الفردية والتحديات والأوسمة.
 */
data class StudentDashboardModel(
    val userId: String = "",
    val displayName: String = "",
    val cefrLevel: String = "B1",
    val nextTargetCefr: String = "B2",
    val overallReadinessPercent: Int = 54,
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    val learningGoal: String = "",
    val skills: List<SkillProgressItem> = emptyList(),
    val weakestSkillKey: String = "READING",
    val personalizedPath: List<LearningPathStep> = emptyList(),
    val recommendedNextStep: LearningPathStep? = null,
    val recentAssessments: List<AssessmentLog> = emptyList(),
    val errorBalance: ErrorBalanceSummary = ErrorBalanceSummary(),
    val weeklyChallenges: List<WeeklyChallenge> = emptyList(),
    val badges: List<BadgeInfo> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList()
)

/**
 * الواجهة البرمجية (Interface) المسؤولة عن ربط بيانات التقدم السحابي في Firestore
 * ببناء وتحديث مسارات التعلم الفردية في لوحة تحكم الطالب.
 */
interface StudentDashboardRepository {
    fun observeStudentDashboard(userId: String): Flow<StudentDashboardModel>

    suspend fun updateLearningGoal(userId: String, newGoal: String)

    fun buildTailoredPath(profile: LearnerProfile): List<LearningPathStep>
}

/**
 * التنفيذ السحابي للواجهة عبر تجميع تدفقات Cloud Firestore لحظياً
 * ومواءمتها مع خوارزمية المسار الفردي للمتعلم.
 */
class FirestoreStudentDashboardRepository(
    private val learningRepository: LearningRepository
) : StudentDashboardRepository {

    override fun observeStudentDashboard(userId: String): Flow<StudentDashboardModel> {
        return combine(
            learningRepository.observeProfile(userId),
            learningRepository.observeMistakes(userId),
            learningRepository.observeAssessments(userId),
            learningRepository.observeChallenges(userId),
            learningRepository.observeLeaderboard()
        ) { profile, mistakes, assessments, challenges, leaderboard ->
            val resolvedProfile = profile ?: LearnerProfile(userId = userId)
            mapToDashboardModel(
                profile = resolvedProfile,
                mistakes = mistakes,
                assessments = assessments,
                challenges = challenges,
                leaderboard = leaderboard
            )
        }
    }

    override suspend fun updateLearningGoal(userId: String, newGoal: String) {
        learningRepository.updatePreferences(uid = userId, learningGoal = newGoal)
    }

    override fun buildTailoredPath(profile: LearnerProfile): List<LearningPathStep> {
        return CurriculumCatalog.buildPersonalizedPath(profile)
    }

    private fun mapToDashboardModel(
        profile: LearnerProfile,
        mistakes: List<MistakeRecord>,
        assessments: List<AssessmentLog>,
        challenges: List<WeeklyChallenge>,
        leaderboard: List<LeaderboardEntry>
    ): StudentDashboardModel {
        val rawSkills = listOf(
            Triple("READING", "القراءة (Reading)", profile.readingScore.toInt()),
            Triple("LISTENING", "الاستماع (Listening)", profile.listeningScore.toInt()),
            Triple("SPEAKING", "النطق والتحدث (Speaking)", profile.speakingScore.toInt()),
            Triple("GRAMMAR", "القواعد (Grammar)", profile.grammarScore.toInt()),
            Triple("VOCABULARY", "المفردات (Vocabulary)", profile.vocabularyScore.toInt())
        )
        val weakestKey = rawSkills.minByOrNull { it.third }?.first ?: "READING"
        val skillItems = rawSkills.map { (key, labelAr, score) ->
            SkillProgressItem(
                skillKey = key,
                labelAr = labelAr,
                labelEn = key.lowercase().replaceFirstChar { it.uppercase() },
                score = score,
                isFocusSkill = key == weakestKey
            )
        }

        val overall = if (rawSkills.isNotEmpty()) {
            rawSkills.sumOf { it.third } / rawSkills.size
        } else {
            54
        }

        val path = buildTailoredPath(profile)
        val recommended = path.firstOrNull { it.isRecommendedNext } ?: path.firstOrNull()

        val errorSummary = ErrorBalanceSummary(
            pendingCount = mistakes.count { !it.mastered },
            masteredCount = mistakes.count { it.mastered },
            criticalRedCount = mistakes.count { it.severity == ErrorSeverity.RED_CRITICAL.id },
            minorOrangeCount = mistakes.count { it.severity == ErrorSeverity.ORANGE_MINOR.id }
        )

        val nextTarget = when (profile.cefrLevel) {
            "B1" -> "B1+"
            "B1+" -> "B2"
            else -> "B2 Mastery"
        }

        return StudentDashboardModel(
            userId = profile.userId,
            displayName = profile.displayName,
            cefrLevel = profile.cefrLevel,
            nextTargetCefr = nextTarget,
            overallReadinessPercent = overall.coerceIn(0, 100),
            xp = profile.xp,
            streakDays = profile.streakDays,
            learningGoal = profile.learningGoal,
            skills = skillItems,
            weakestSkillKey = weakestKey,
            personalizedPath = path,
            recommendedNextStep = recommended,
            recentAssessments = assessments.take(5),
            errorBalance = errorSummary,
            weeklyChallenges = challenges,
            badges = CurriculumCatalog.evaluateBadges(profile),
            leaderboard = leaderboard
        )
    }
}
