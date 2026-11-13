package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.AssessmentLog
import com.example.data.model.DailyArticle
import com.example.data.model.DetectedMistakeInput
import com.example.data.model.ErrorSeverity
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LearnerProfile
import com.example.data.model.MistakeRecord
import com.example.data.model.ReadingThemeMode
import com.example.data.model.SpeechEvaluationResult
import com.example.data.model.TutorChatMessage
import com.example.data.model.WeeklyChallenge
import com.example.data.repository.AiTutorRepository
import com.example.data.repository.LearningRepository
import com.example.data.repository.SmartLibrarySearchResult
import com.example.domain.dashboard.FirestoreStudentDashboardRepository
import com.example.domain.dashboard.StudentDashboardModel
import com.example.domain.dashboard.StudentDashboardRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
}

sealed interface AiActionState<out T> {
    data object Idle : AiActionState<Nothing>
    data object Loading : AiActionState<Nothing>
    data class Success<T>(val data: T) : AiActionState<T>
    data class Error(val message: String) : AiActionState<Nothing>
}

class MainViewModel(
    private val repository: LearningRepository,
    private val aiRepository: AiTutorRepository,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val dashboardRepository: StudentDashboardRepository = FirestoreStudentDashboardRepository(repository)
) : ViewModel() {

    val studentDashboardState: StateFlow<StudentDashboardModel> =
        dashboardRepository.observeStudentDashboard(currentUserId)
            .catch { error ->
                val root = unwrapFirestore(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing student dashboard", root ?: error)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = StudentDashboardModel(
                    userId = currentUserId,
                    displayName = initialDisplayName
                )
            )

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    val profileState: StateFlow<DataState<LearnerProfile?>> =
        repository.observeProfile(currentUserId)
            .map<LearnerProfile?, DataState<LearnerProfile?>> { DataState.Success(it) }
            .catch { error ->
                val root = unwrapFirestore(error)
                if (root == null && error is CancellationException) throw error
                val err = root ?: error
                Log.w(TAG, "Error observing profile", err)
                emit(DataState.Error(err.localizedMessage ?: "Failed to load profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataState.Loading
            )

    val mistakesState: StateFlow<List<MistakeRecord>> =
        repository.observeMistakes(currentUserId)
            .catch { error ->
                val root = unwrapFirestore(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing mistakes", root ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    val assessmentsState: StateFlow<List<AssessmentLog>> =
        repository.observeAssessments(currentUserId)
            .catch { error ->
                val root = unwrapFirestore(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing assessments", root ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    val challengesState: StateFlow<List<WeeklyChallenge>> =
        repository.observeChallenges(currentUserId)
            .catch { error ->
                val root = unwrapFirestore(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing challenges", root ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    val leaderboardState: StateFlow<List<LeaderboardEntry>> =
        repository.observeLeaderboard()
            .catch { error ->
                val root = unwrapFirestore(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing leaderboard", root ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    private val _articles = MutableStateFlow(CurriculumCatalog.dailyArticles)
    val articles: StateFlow<List<DailyArticle>> = _articles.asStateFlow()

    private val _articleGenState = MutableStateFlow<AiActionState<DailyArticle>>(AiActionState.Idle)
    val articleGenState: StateFlow<AiActionState<DailyArticle>> = _articleGenState.asStateFlow()

    private val _speechEvalState = MutableStateFlow<AiActionState<SpeechEvaluationResult>>(AiActionState.Idle)
    val speechEvalState: StateFlow<AiActionState<SpeechEvaluationResult>> = _speechEvalState.asStateFlow()

    private val _smartSearchState = MutableStateFlow<AiActionState<SmartLibrarySearchResult>>(AiActionState.Idle)
    val smartSearchState: StateFlow<AiActionState<SmartLibrarySearchResult>> = _smartSearchState.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            TutorChatMessage(
                id = "welcome_ai",
                isUser = false,
                text = "أهلاً بك في Teach me! أنا معلمك الذكي بالذكاء الاصطناعي (24/7) للمساعدة في قواعد B1–B2، تحسين النطق، أو أي دعم تقني في استخدام التطبيق. كيف أساعدك اليوم؟"
            )
        )
    )
    val chatMessages: StateFlow<List<TutorChatMessage>> = _chatMessages.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureInitialized(currentUserId, initialDisplayName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize learner profile", e)
                _initError.value = e.localizedMessage ?: "تعذر تهيئة الملف السحابي."
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateTheme(mode: ReadingThemeMode) {
        viewModelScope.launch {
            runCatching {
                repository.updatePreferences(currentUserId, readingTheme = mode.id)
            }.onFailure {
                _statusBanner.value = it.localizedMessage
            }
        }
    }

    fun updateLanguage(preferredLang: String) {
        viewModelScope.launch {
            runCatching {
                repository.updatePreferences(currentUserId, preferredLang = preferredLang)
            }.onFailure {
                _statusBanner.value = it.localizedMessage
            }
        }
    }

    fun updateTranslationLanguage(translationLang: String) {
        viewModelScope.launch {
            runCatching {
                repository.updatePreferences(currentUserId, translationLang = translationLang)
            }.onFailure {
                _statusBanner.value = it.localizedMessage
            }
        }
    }

    fun updateGoalAndReminder(
        learningGoal: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updatePreferences(
                    uid = currentUserId,
                    learningGoal = learningGoal,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute
                )
                _statusBanner.value = "تم حفظ الهدف وإعدادات التنبيهات الذكية."
            }.onFailure {
                _statusBanner.value = it.localizedMessage
            }
        }
    }

    fun updateGuardianPin(pin: String) {
        viewModelScope.launch {
            runCatching {
                repository.updatePreferences(uid = currentUserId, guardianPin = pin)
                _statusBanner.value = "تم تحديث رمز بوابة المعلم وولي الأمر."
            }.onFailure {
                _statusBanner.value = it.localizedMessage
            }
        }
    }

    fun submitArticleQuiz(
        profile: LearnerProfile,
        article: DailyArticle,
        selectedAnswers: Map<String, Int>
    ) {
        viewModelScope.launch {
            var correctCount = 0
            var redCount = 0
            var orangeCount = 0

            article.quiz.forEach { q ->
                val chosen = selectedAnswers[q.id]
                if (chosen == q.correctIndex) {
                    correctCount++
                } else {
                    val sev = q.errorSeverityIfWrong
                    if (sev == ErrorSeverity.RED_CRITICAL) redCount++ else orangeCount++
                    val chosenText = chosen?.let { q.options.getOrNull(it) } ?: "No answer"
                    runCatching {
                        repository.logMistake(
                            currentUserId,
                            DetectedMistakeInput(
                                sourceType = "QUIZ",
                                originalText = "${q.promptEn} -> $chosenText",
                                correctedText = q.options.getOrElse(q.correctIndex) { "" },
                                errorSpan = chosenText,
                                severity = sev,
                                category = q.category,
                                explanationAr = q.explanationAr,
                                explanationEn = q.explanationEn
                            )
                        )
                    }
                }
            }

            val total = article.quiz.size.coerceAtLeast(1)
            val score = (correctCount * 100) / total
            val baseXp = 30 + (correctCount * 15)

            runCatching {
                val bonusXp = repository.advanceChallengeByPrefix(
                    currentUserId,
                    challengesState.value,
                    "ch_articles_"
                )
                repository.recordAssessmentAndProgress(
                    uid = currentUserId,
                    profile = profile,
                    activityType = "ARTICLE",
                    title = article.titleEn,
                    score = score,
                    xpEarned = baseXp + bonusXp.toInt(),
                    redErrors = redCount,
                    orangeErrors = orangeCount,
                    notes = "Quiz $correctCount/$total",
                    readingDelta = if (score >= 50) 3 else 1,
                    listeningDelta = 2,
                    grammarDelta = if (redCount == 0) 2 else 0,
                    vocabularyDelta = 2,
                    articleIncrement = 1,
                    quizIncrement = 1
                )
                _statusBanner.value = "تم تقييم المقال (+${baseXp + bonusXp} XP) وحفظ أي أخطاء في السجل."
            }.onFailure {
                _statusBanner.value = it.localizedMessage
            }
        }
    }

    fun generateAdaptiveArticle(profile: LearnerProfile) {
        viewModelScope.launch {
            _articleGenState.value = AiActionState.Loading
            val focus = when {
                profile.grammarScore <= profile.vocabularyScore -> "B1-B2 Grammar & Connectors"
                profile.vocabularyScore <= profile.readingScore -> "Academic & Workplace Vocabulary"
                else -> "Critical Reading & Nuance"
            }
            val result = aiRepository.generateAdaptiveArticle(profile.cefrLevel, focus)
            result.fold(
                onSuccess = { newArticle ->
                    _articles.value = listOf(newArticle) + _articles.value
                    _articleGenState.value = AiActionState.Success(newArticle)
                },
                onFailure = { err ->
                    _articleGenState.value = AiActionState.Error(err.localizedMessage ?: "فشل توليد المقال.")
                }
            )
        }
    }

    fun evaluateSpeechOrWriting(
        profile: LearnerProfile,
        promptTopic: String,
        learnerText: String,
        audioBytes: ByteArray? = null
    ) {
        if (learnerText.isBlank() && (audioBytes == null || audioBytes.isEmpty())) {
            _speechEvalState.value = AiActionState.Error("يرجى التحدث أو كتابة جملة بالإنكليزية أولاً.")
            return
        }
        viewModelScope.launch {
            _speechEvalState.value = AiActionState.Loading
            val result = aiRepository.evaluateSpeechOrText(
                promptTopic = promptTopic,
                learnerTranscript = learnerText,
                cefrLevel = profile.cefrLevel,
                audioBytes = audioBytes
            )
            result.fold(
                onSuccess = { eval ->
                    _speechEvalState.value = AiActionState.Success(eval)
                    var red = 0
                    var orange = 0
                    eval.mistakes.forEach { m ->
                        if (m.severity == ErrorSeverity.RED_CRITICAL) red++ else if (m.severity == ErrorSeverity.ORANGE_MINOR) orange++
                        runCatching { repository.logMistake(currentUserId, m) }
                    }
                    runCatching {
                        val bonusXp = repository.advanceChallengeByPrefix(
                            currentUserId,
                            challengesState.value,
                            "ch_voice_"
                        )
                        val xp = (25 + eval.overallScore / 4) + bonusXp.toInt()
                        repository.recordAssessmentAndProgress(
                            uid = currentUserId,
                            profile = profile,
                            activityType = "VOICE",
                            title = promptTopic.take(90),
                            score = eval.overallScore,
                            xpEarned = xp,
                            redErrors = red,
                            orangeErrors = orange,
                            notes = "Pronunciation ${eval.pronunciationScore}%, Grammar ${eval.grammarScore}%",
                            speakingDelta = if (eval.overallScore >= 65) 4 else 2,
                            listeningDelta = 1,
                            grammarDelta = if (red == 0) 3 else 1,
                            voiceIncrement = 1
                        )
                    }
                },
                onFailure = { err ->
                    _speechEvalState.value = AiActionState.Error(err.localizedMessage ?: "تعذر تحليل الصوت.")
                }
            )
        }
    }

    fun sendTutorMessage(profile: LearnerProfile, userText: String) {
        val clean = userText.trim()
        if (clean.isBlank()) return
        _chatError.value = null
        val userMsg = TutorChatMessage(id = "u_${System.currentTimeMillis()}", isUser = true, text = clean)
        val aiMsgId = "a_${System.currentTimeMillis() + 1}"
        val history = _chatMessages.value.map { it.isUser to it.text }
        _chatMessages.value = _chatMessages.value + userMsg + TutorChatMessage(
            id = aiMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        viewModelScope.launch {
            try {
                aiRepository.streamTutorReply(
                    userMessage = clean,
                    cefrLevel = profile.cefrLevel,
                    preferredLang = profile.preferredLang,
                    historyPairs = history
                ).collect { partial ->
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = partial, isStreaming = true) else msg
                    }
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) msg.copy(isStreaming = false) else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI tutor stream failed", e)
                val errText = e.localizedMessage ?: "تعذر الاتصال بالمعلم الذكي."
                _chatError.value = errText
                _chatMessages.value = _chatMessages.value.filterNot { it.id == aiMsgId && it.text.isBlank() }
                    .map { msg -> if (msg.id == aiMsgId) msg.copy(isStreaming = false) else msg }
            }
        }
    }

    fun runSmartLibrarySearch(query: String) {
        if (query.isBlank()) {
            _smartSearchState.value = AiActionState.Idle
            return
        }
        viewModelScope.launch {
            _smartSearchState.value = AiActionState.Loading
            val words = CurriculumCatalog.vocabularyLibrary.map { it.word }
            val result = aiRepository.smartSearchLibrary(query, words)
            result.fold(
                onSuccess = { _smartSearchState.value = AiActionState.Success(it) },
                onFailure = { _smartSearchState.value = AiActionState.Error(it.localizedMessage ?: "فشل البحث الذكي.") }
            )
        }
    }

    fun clearSmartSearch() {
        _smartSearchState.value = AiActionState.Idle
    }

    fun markMistakeReviewed(profile: LearnerProfile, mistake: MistakeRecord, mastered: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.markMistakeReviewed(currentUserId, mistake, mastered)
                val bonusXp = repository.advanceChallengeByPrefix(
                    currentUserId,
                    challengesState.value,
                    "ch_mistakes_"
                )
                if (mastered) {
                    repository.recordAssessmentAndProgress(
                        uid = currentUserId,
                        profile = profile,
                        activityType = "CHALLENGE",
                        title = "Reviewed: ${mistake.errorSpan}",
                        score = 100,
                        xpEarned = 15 + bonusXp.toInt(),
                        redErrors = 0,
                        orangeErrors = 0,
                        grammarDelta = 2,
                        vocabularyDelta = 2
                    )
                }
            }.onFailure {
                _statusBanner.value = it.localizedMessage
            }
        }
    }

    private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
