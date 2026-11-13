package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AssessmentLog
import com.example.data.model.DetectedMistakeInput
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LearnerProfile
import com.example.data.model.MistakeRecord
import com.example.data.model.WeeklyChallenge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class LearningRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeProfile(uid: String): Flow<LearnerProfile?> =
        userDoc(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        LearnerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val fsError = unwrapFirestoreError(error)
                if (fsError == null && error is CancellationException) throw error
                handleFirestoreError(fsError ?: error, OperationType.GET, userDoc(uid).path)
                throw error
            }

    fun observeMistakes(uid: String): Flow<List<MistakeRecord>> =
        userDoc(uid).collection("mistakes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MistakeRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = unwrapFirestoreError(error)
                if (fsError == null && error is CancellationException) throw error
                handleFirestoreError(fsError ?: error, OperationType.LIST, "users/$uid/mistakes")
                throw error
            }

    fun observeAssessments(uid: String): Flow<List<AssessmentLog>> =
        userDoc(uid).collection("assessments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AssessmentLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = unwrapFirestoreError(error)
                if (fsError == null && error is CancellationException) throw error
                handleFirestoreError(fsError ?: error, OperationType.LIST, "users/$uid/assessments")
                throw error
            }

    fun observeChallenges(uid: String): Flow<List<WeeklyChallenge>> =
        userDoc(uid).collection("challenges")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WeeklyChallenge::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = unwrapFirestoreError(error)
                if (fsError == null && error is CancellationException) throw error
                handleFirestoreError(fsError ?: error, OperationType.LIST, "users/$uid/challenges")
                throw error
            }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> =
        db.collection("leaderboard")
            .orderBy("xp", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = unwrapFirestoreError(error)
                if (fsError == null && error is CancellationException) throw error
                handleFirestoreError(fsError ?: error, OperationType.LIST, "leaderboard")
                throw error
            }

    suspend fun ensureInitialized(uid: String, displayName: String) {
        val safeName = displayName.trim().ifBlank { "Scholar" }.take(60)
        val docRef = userDoc(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "cefrLevel" to "B1",
                    "learningGoal" to "Reach B2 Academic & Fluent Communication",
                    "xp" to 20L,
                    "streakDays" to 1L,
                    "articlesRead" to 0L,
                    "quizzesCompleted" to 0L,
                    "voiceSessionsCount" to 0L,
                    "readingScore" to 55L,
                    "listeningScore" to 52L,
                    "speakingScore" to 50L,
                    "grammarScore" to 54L,
                    "vocabularyScore" to 58L,
                    "preferredLang" to "ar",
                    "translationLang" to "ar",
                    "readingTheme" to "WARM_PAPER",
                    "reminderEnabled" to true,
                    "reminderHour" to 19L,
                    "reminderMinute" to 30L,
                    "earnedBadges" to listOf("first_step"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                seedWeeklyChallenges(uid)
                syncLeaderboard(
                    uid = uid,
                    displayName = safeName,
                    cefrLevel = "B1",
                    xp = 20L,
                    weeklyPoints = 20L,
                    streakDays = 1L,
                    badgeTitle = "First Ink"
                )
            } else {
                val existingChallenges = userDoc(uid).collection("challenges").limit(1).get().await()
                if (existingChallenges.isEmpty) {
                    seedWeeklyChallenges(uid)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun seedWeeklyChallenges(uid: String) {
        val cal = Calendar.getInstance()
        val weekKey = "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
        val defaults = listOf(
            Triple("ch_articles_$weekKey", "تحدي القراءة السمعية (3 مقالات)", 3L) to Pair(
                "اقرأ واستمع إلى 3 مقالات B1-B2 وأجب عن اختباراتها.",
                120L
            ),
            Triple("ch_voice_$weekKey", "تحدي الفصاحة الصوتية (3 جلسات)", 3L) to Pair(
                "أكمل 3 جلسات تقييم صوتي مع تقليل الأخطاء الحمراء.",
                150L
            ),
            Triple("ch_mistakes_$weekKey", "تحدي إتقان الأخطاء (5 مراجعات)", 5L) to Pair(
                "راجع 5 أخطاء من سجل الأخطاء وحوّلها إلى نقاط قوة.",
                100L
            )
        )
        defaults.forEach { (idTitleTarget, descReward) ->
            val (id, title, target) = idTitleTarget
            val (desc, reward) = descReward
            val cRef = userDoc(uid).collection("challenges").document(id)
            val payload = mapOf(
                "userId" to uid,
                "weekKey" to weekKey,
                "title" to title,
                "description" to desc,
                "targetCount" to target,
                "currentCount" to 0L,
                "xpReward" to reward,
                "completed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            cRef.set(payload).await()
        }
    }

    suspend fun updatePreferences(
        uid: String,
        preferredLang: String? = null,
        translationLang: String? = null,
        readingTheme: String? = null,
        learningGoal: String? = null,
        reminderEnabled: Boolean? = null,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        guardianPin: String? = null
    ) {
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        preferredLang?.let { updates["preferredLang"] = it }
        translationLang?.let { updates["translationLang"] = it }
        readingTheme?.let { updates["readingTheme"] = it }
        learningGoal?.let { updates["learningGoal"] = it.take(120) }
        reminderEnabled?.let { updates["reminderEnabled"] = it }
        reminderHour?.let { updates["reminderHour"] = it.toLong() }
        reminderMinute?.let { updates["reminderMinute"] = it.toLong() }
        guardianPin?.let { updates["guardianPin"] = it.take(12) }

        val docRef = userDoc(uid)
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun logMistake(uid: String, input: DetectedMistakeInput): String {
        val docRef = userDoc(uid).collection("mistakes").document()
        val payload = mapOf(
            "userId" to uid,
            "sourceType" to input.sourceType,
            "originalText" to input.originalText.take(500).ifBlank { "—" },
            "correctedText" to input.correctedText.take(500).ifBlank { "—" },
            "errorSpan" to input.errorSpan.take(150).ifBlank { "—" },
            "severity" to input.severity.id,
            "category" to input.category,
            "explanationAr" to input.explanationAr.take(600).ifBlank { "ملاحظة لغوية" },
            "explanationEn" to input.explanationEn.take(600).ifBlank { "Language feedback" },
            "mastered" to false,
            "reviewCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markMistakeReviewed(
        uid: String,
        mistake: MistakeRecord,
        mastered: Boolean
    ) {
        require(mistake.id.isNotBlank()) { "Mistake ID cannot be blank" }
        val docRef = userDoc(uid).collection("mistakes").document(mistake.id)
        try {
            docRef.update(
                mapOf(
                    "mastered" to mastered,
                    "reviewCount" to (mistake.reviewCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordAssessmentAndProgress(
        uid: String,
        profile: LearnerProfile,
        activityType: String,
        title: String,
        score: Int,
        xpEarned: Int,
        redErrors: Int,
        orangeErrors: Int,
        notes: String = "",
        readingDelta: Int = 0,
        listeningDelta: Int = 0,
        speakingDelta: Int = 0,
        grammarDelta: Int = 0,
        vocabularyDelta: Int = 0,
        articleIncrement: Int = 0,
        quizIncrement: Int = 0,
        voiceIncrement: Int = 0
    ) {
        val newReading = (profile.readingScore + readingDelta).coerceIn(0L, 100L)
        val newListening = (profile.listeningScore + listeningDelta).coerceIn(0L, 100L)
        val newSpeaking = (profile.speakingScore + speakingDelta).coerceIn(0L, 100L)
        val newGrammar = (profile.grammarScore + grammarDelta).coerceIn(0L, 100L)
        val newVocab = (profile.vocabularyScore + vocabularyDelta).coerceIn(0L, 100L)

        val avgSkill = (newReading + newListening + newSpeaking + newGrammar + newVocab) / 5L
        val newCefr = when {
            avgSkill >= 80L -> "B2"
            avgSkill >= 65L -> "B1+"
            else -> "B1"
        }
        val newXp = profile.xp + xpEarned.toLong()
        val newArticles = profile.articlesRead + articleIncrement.toLong()
        val newQuizzes = profile.quizzesCompleted + quizIncrement.toLong()
        val newVoice = profile.voiceSessionsCount + voiceIncrement.toLong()

        val badges = profile.earnedBadges.toMutableSet()
        badges.add("first_step")
        if (newArticles >= 3L) badges.add("avid_reader")
        if (newVoice >= 3L) badges.add("eloquent_voice")
        if (profile.streakDays >= 7L) badges.add("streak_7")
        if (newCefr == "B2") badges.add("b2_vanguard")

        val assessmentRef = userDoc(uid).collection("assessments").document()
        val assessmentPayload = mapOf(
            "userId" to uid,
            "activityType" to activityType,
            "title" to title.take(180).ifBlank { "Learning Activity" },
            "score" to score.coerceIn(0, 100).toLong(),
            "xpEarned" to xpEarned.coerceAtLeast(0).toLong(),
            "redErrors" to redErrors.coerceAtLeast(0).toLong(),
            "orangeErrors" to orangeErrors.coerceAtLeast(0).toLong(),
            "cefrSnapshot" to newCefr,
            "notes" to notes.take(400),
            "createdAt" to FieldValue.serverTimestamp()
        )

        val profileUpdates = mapOf(
            "cefrLevel" to newCefr,
            "xp" to newXp,
            "articlesRead" to newArticles,
            "quizzesCompleted" to newQuizzes,
            "voiceSessionsCount" to newVoice,
            "readingScore" to newReading,
            "listeningScore" to newListening,
            "speakingScore" to newSpeaking,
            "grammarScore" to newGrammar,
            "vocabularyScore" to newVocab,
            "earnedBadges" to badges.take(30),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            assessmentRef.set(assessmentPayload).await()
            userDoc(uid).update(profileUpdates).await()
            syncLeaderboard(
                uid = uid,
                displayName = profile.displayName.ifBlank { "Scholar" },
                cefrLevel = newCefr,
                xp = newXp,
                weeklyPoints = newXp,
                streakDays = profile.streakDays,
                badgeTitle = when (newCefr) {
                    "B2" -> "B2 Vanguard"
                    "B1+" -> "Rising Scholar"
                    else -> "First Ink"
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, assessmentRef.path)
            throw e
        }
    }

    suspend fun advanceChallengeByPrefix(
        uid: String,
        challenges: List<WeeklyChallenge>,
        prefix: String
    ): Long {
        val target = challenges.firstOrNull { it.id.startsWith(prefix) && !it.completed } ?: return 0L
        val nextCount = (target.currentCount + 1L).coerceAtMost(target.targetCount)
        val nowCompleted = nextCount >= target.targetCount
        val docRef = userDoc(uid).collection("challenges").document(target.id)
        try {
            docRef.update(
                mapOf(
                    "currentCount" to nextCount,
                    "completed" to nowCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return if (nowCompleted) target.xpReward else 0L
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    private suspend fun syncLeaderboard(
        uid: String,
        displayName: String,
        cefrLevel: String,
        xp: Long,
        weeklyPoints: Long,
        streakDays: Long,
        badgeTitle: String
    ) {
        val ref = db.collection("leaderboard").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to displayName.take(60),
            "cefrLevel" to cefrLevel,
            "xp" to xp,
            "weeklyPoints" to weeklyPoints,
            "streakDays" to streakDays,
            "badgeTitle" to badgeTitle.take(60),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.set(payload).await()
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
