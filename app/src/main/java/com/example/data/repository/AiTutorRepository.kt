package com.example.data.repository

import android.util.Log
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.AnalyzedSegment
import com.example.data.model.DailyArticle
import com.example.data.model.DetectedMistakeInput
import com.example.data.model.ErrorSeverity
import com.example.data.model.QuizQuestion
import com.example.data.model.SpeechEvaluationResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiTutorRepository"
private const val AI_TIMEOUT_MS = 60_000L

data class SmartLibrarySearchResult(
    val matchedWordIds: List<String>,
    val aiExplanationAr: String,
    val aiExplanationEn: String,
    val bonusWord: String,
    val bonusIpa: String,
    val bonusMeaningAr: String,
    val bonusExampleEn: String
)

class AiTutorRepository {

    private val speechEvalSchema = Schema.obj(
        properties = mapOf(
            "learnerText" to Schema.string("The exact English sentence spoken or written by the learner"),
            "correctedFullText" to Schema.string("The fully corrected B1-B2 English version"),
            "overallScore" to Schema.integer("Overall score 0-100"),
            "pronunciationScore" to Schema.integer("Pronunciation & fluency score 0-100"),
            "grammarScore" to Schema.integer("Grammar & vocabulary accuracy score 0-100"),
            "praiseAr" to Schema.string("Encouraging feedback in Arabic"),
            "praiseEn" to Schema.string("Encouraging feedback in English"),
            "nextTipAr" to Schema.string("Concrete improvement tip in Arabic"),
            "nextTipEn" to Schema.string("Concrete improvement tip in English"),
            "segments" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "text" to Schema.string("Word or phrase segment from learner's input"),
                        "severity" to Schema.enumeration(
                            listOf("NONE", "RED_CRITICAL", "ORANGE_MINOR"),
                            "RED_CRITICAL if word/letter/grammar mistake changes meaning or harms comprehension; ORANGE_MINOR if minor mistake that does NOT change meaning but slightly harms accuracy or style; NONE if accurate"
                        ),
                        "correction" to Schema.string("Suggested correction if severity is not NONE, else empty"),
                        "category" to Schema.enumeration(
                            listOf("PRONUNCIATION", "GRAMMAR", "SPELLING", "WORD_CHOICE"),
                            "Error category"
                        ),
                        "explanationAr" to Schema.string("Short explanation in Arabic"),
                        "explanationEn" to Schema.string("Short explanation in English")
                    )
                ),
                description = "Sequential segments covering the learner's text"
            )
        )
    )

    private val smartSearchSchema = Schema.obj(
        properties = mapOf(
            "matchedWords" to Schema.array(
                items = Schema.string(),
                description = "Up to 5 matching words from the provided list"
            ),
            "aiExplanationAr" to Schema.string("Concise Arabic answer explaining the word choice or nuance"),
            "aiExplanationEn" to Schema.string("Concise English explanation"),
            "bonusWord" to Schema.string("One additional helpful B1-B2 word related to the query"),
            "bonusIpa" to Schema.string("IPA phonetic transcription"),
            "bonusMeaningAr" to Schema.string("Arabic meaning of bonus word"),
            "bonusExampleEn" to Schema.string("Example sentence using bonus word")
        )
    )

    private val articleSchema = Schema.obj(
        properties = mapOf(
            "titleEn" to Schema.string(),
            "titleAr" to Schema.string(),
            "cefrLevel" to Schema.enumeration(listOf("B1", "B1+", "B2")),
            "category" to Schema.string(),
            "summaryAr" to Schema.string(),
            "paragraphs" to Schema.array(items = Schema.string()),
            "keyPhrases" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "phrase" to Schema.string(),
                        "meaningAr" to Schema.string()
                    )
                )
            ),
            "quiz" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "promptEn" to Schema.string(),
                        "promptAr" to Schema.string(),
                        "options" to Schema.array(items = Schema.string()),
                        "correctIndex" to Schema.integer(),
                        "severityIfWrong" to Schema.enumeration(listOf("RED_CRITICAL", "ORANGE_MINOR")),
                        "explanationAr" to Schema.string(),
                        "explanationEn" to Schema.string(),
                        "category" to Schema.enumeration(listOf("READING", "GRAMMAR", "WORD_CHOICE"))
                    )
                )
            )
        )
    )

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Bayan AI, a calm, encouraging 24/7 virtual English tutor and technical study support assistant for B1-to-B2 learners. " +
                        "Always be transparent that you are an AI tutor (not a real human teacher). " +
                        "Support Arabic and English seamlessly. When correcting mistakes, explicitly distinguish CRITICAL meaning-changing errors (Red) from MINOR non-meaning style/spelling/accent slips (Orange). " +
                        "Keep replies clear, scholarly, and well-structured."
                )
            }
        )
    }

    private val speechModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = speechEvalSchema
            }
        )
    }

    private val searchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = smartSearchSchema
            }
        )
    }

    private val articleModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = articleSchema
            }
        )
    }

    suspend fun evaluateSpeechOrText(
        promptTopic: String,
        learnerTranscript: String,
        cefrLevel: String,
        audioBytes: ByteArray? = null,
        audioMimeType: String = "audio/mp4"
    ): Result<SpeechEvaluationResult> {
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                val promptContent = content {
                    if (audioBytes != null && audioBytes.isNotEmpty() && audioBytes.size < 18 * 1024 * 1024) {
                        inlineData(audioBytes, audioMimeType)
                    }
                    text(
                        """
                        Evaluate this $cefrLevel English learner's spoken/written response to the prompt: "$promptTopic".
                        Learner transcript (may be empty if audio is attached): "$learnerTranscript".
                        Rules for segments:
                        1. Split the learner's response sequentially so joining all segment.text approximates the learner's sentence.
                        2. Mark severity = RED_CRITICAL ONLY when an error in a word, letter, tense, or pronunciation changes the meaning or makes it hard to understand.
                        3. Mark severity = ORANGE_MINOR when a minor error does NOT change the word's or sentence's meaning, but slightly harms style, accent, preposition choice, or minor spelling.
                        4. Mark severity = NONE for accurate words/phrases.
                        5. Provide clear Arabic and English explanations for every non-NONE segment.
                        """.trimIndent()
                    )
                }
                speechModel.generateContent(promptContent)
            }

            val rawJson = response.text ?: throw IllegalStateException("Empty AI response.")
            val json = JSONObject(rawJson)
            val resolvedText = json.optString("learnerText", learnerTranscript).ifBlank { learnerTranscript }
            val correctedFull = json.optString("correctedFullText", resolvedText)
            val segmentsJson = json.optJSONArray("segments")
            val segments = mutableListOf<AnalyzedSegment>()
            val mistakes = mutableListOf<DetectedMistakeInput>()

            if (segmentsJson != null) {
                for (i in 0 until segmentsJson.length()) {
                    val item = segmentsJson.getJSONObject(i)
                    val segText = item.optString("text", "")
                    val sev = ErrorSeverity.fromId(item.optString("severity", "NONE"))
                    val corr = item.optString("correction", "")
                    val cat = item.optString("category", "GRAMMAR")
                    val expAr = item.optString("explanationAr", "")
                    val expEn = item.optString("explanationEn", "")
                    if (segText.isNotEmpty()) {
                        segments.add(
                            AnalyzedSegment(
                                text = segText,
                                severity = sev,
                                correction = corr,
                                category = cat,
                                explanationAr = expAr,
                                explanationEn = expEn
                            )
                        )
                        if (sev != ErrorSeverity.NONE) {
                            mistakes.add(
                                DetectedMistakeInput(
                                    sourceType = if (audioBytes != null) "VOICE" else "WRITING",
                                    originalText = resolvedText,
                                    correctedText = corr.ifBlank { correctedFull },
                                    errorSpan = segText,
                                    severity = sev,
                                    category = cat,
                                    explanationAr = expAr.ifBlank { "ملاحظة لغوية للتصحيح" },
                                    explanationEn = expEn.ifBlank { "Suggested refinement" }
                                )
                            )
                        }
                    }
                }
            }

            Result.success(
                SpeechEvaluationResult(
                    promptUsed = promptTopic,
                    learnerText = resolvedText,
                    overallScore = json.optInt("overallScore", 75).coerceIn(0, 100),
                    pronunciationScore = json.optInt("pronunciationScore", 74).coerceIn(0, 100),
                    grammarScore = json.optInt("grammarScore", 76).coerceIn(0, 100),
                    segments = segments,
                    correctedFullText = correctedFull,
                    praiseAr = json.optString("praiseAr", "أحسنت! استمر في التمرن بثقة."),
                    praiseEn = json.optString("praiseEn", "Well done! Keep practicing consistently."),
                    nextTipAr = json.optString("nextTipAr", "ركّز على نطق المقاطع المشددة بوضوح."),
                    nextTipEn = json.optString("nextTipEn", "Focus on clear word stress and connectors."),
                    mistakes = mistakes
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Speech evaluation timed out", e)
            Result.failure(Exception("استغرق التحليل وقتاً أطول من المتوقع (60 ثانية). حاول مجدداً."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Speech evaluation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "تعذر تحليل الصوت والنص حالياً."))
        }
    }

    fun streamTutorReply(
        userMessage: String,
        cefrLevel: String,
        preferredLang: String,
        historyPairs: List<Pair<Boolean, String>>
    ): Flow<String> = flow {
        val historyContent = historyPairs.takeLast(8).map { (isUser, msg) ->
            content(if (isUser) "user" else "model") { text(msg) }
        }
        val chat = chatModel.startChat(history = historyContent)
        val prompt = "[Learner CEFR: $cefrLevel | UI Language: $preferredLang] $userMessage"
        val accumulated = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
            chat.sendMessageStream(prompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Empty AI tutor response.")
        }
    }

    suspend fun smartSearchLibrary(
        query: String,
        availableWords: List<String>
    ): Result<SmartLibrarySearchResult> {
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                searchModel.generateContent(
                    """
                    A B1-B2 English learner searched the vocabulary library with query: "$query".
                    Available library words: ${availableWords.joinToString(", ")}.
                    Pick up to 5 words from the available list that semantically match the query (by meaning, synonym, Arabic translation, or context), explain clearly in Arabic and English, and suggest 1 bonus B1-B2 word.
                    """.trimIndent()
                )
            }
            val raw = response.text ?: throw IllegalStateException("Empty smart search response.")
            val json = JSONObject(raw)
            val arr = json.optJSONArray("matchedWords")
            val matched = mutableListOf<String>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    matched.add(arr.getString(i))
                }
            }
            val idMap = CurriculumCatalog.vocabularyLibrary.associateBy { it.word.lowercase() }
            val matchedIds = matched.mapNotNull { idMap[it.lowercase()]?.id }
            Result.success(
                SmartLibrarySearchResult(
                    matchedWordIds = matchedIds,
                    aiExplanationAr = json.optString("aiExplanationAr", ""),
                    aiExplanationEn = json.optString("aiExplanationEn", ""),
                    bonusWord = json.optString("bonusWord", ""),
                    bonusIpa = json.optString("bonusIpa", ""),
                    bonusMeaningAr = json.optString("bonusMeaningAr", ""),
                    bonusExampleEn = json.optString("bonusExampleEn", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Smart search timed out", e)
            Result.failure(Exception("انتهت مهلة البحث الذكي."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Smart search failed", e)
            Result.failure(Exception(e.localizedMessage ?: "تعذر إتمام البحث الذكي."))
        }
    }

    suspend fun generateAdaptiveArticle(
        cefrLevel: String,
        focusSkill: String
    ): Result<DailyArticle> {
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                articleModel.generateContent(
                    """
                    Generate a calm, intellectually engaging English learning article for a learner at CEFR level $cefrLevel focusing on $focusSkill.
                    Include 3 clear paragraphs, 3 key phrases with Arabic translations, and 2 multiple-choice quiz questions (each with 4 options, correctIndex 0..3, severityIfWrong RED_CRITICAL or ORANGE_MINOR, and Arabic/English explanations).
                    """.trimIndent()
                )
            }
            val raw = response.text ?: throw IllegalStateException("Empty article response.")
            val json = JSONObject(raw)

            val paragraphs = mutableListOf<String>()
            val pArr = json.optJSONArray("paragraphs")
            if (pArr != null) {
                for (i in 0 until pArr.length()) paragraphs.add(pArr.getString(i))
            }

            val phrases = mutableListOf<Pair<String, String>>()
            val kpArr = json.optJSONArray("keyPhrases")
            if (kpArr != null) {
                for (i in 0 until kpArr.length()) {
                    val obj = kpArr.getJSONObject(i)
                    phrases.add(obj.optString("phrase") to obj.optString("meaningAr"))
                }
            }

            val quiz = mutableListOf<QuizQuestion>()
            val qArr = json.optJSONArray("quiz")
            if (qArr != null) {
                for (i in 0 until qArr.length()) {
                    val qObj = qArr.getJSONObject(i)
                    val optArr = qObj.optJSONArray("options")
                    val opts = mutableListOf<String>()
                    if (optArr != null) {
                        for (j in 0 until optArr.length()) opts.add(optArr.getString(j))
                    }
                    if (opts.size >= 2) {
                        quiz.add(
                            QuizQuestion(
                                id = "ai_q_${System.currentTimeMillis()}_$i",
                                promptEn = qObj.optString("promptEn"),
                                promptAr = qObj.optString("promptAr"),
                                options = opts,
                                correctIndex = qObj.optInt("correctIndex", 0).coerceIn(0, opts.lastIndex),
                                errorSeverityIfWrong = ErrorSeverity.fromId(
                                    qObj.optString("severityIfWrong", "RED_CRITICAL")
                                ),
                                explanationAr = qObj.optString("explanationAr"),
                                explanationEn = qObj.optString("explanationEn"),
                                category = qObj.optString("category", "READING")
                            )
                        )
                    }
                }
            }

            Result.success(
                DailyArticle(
                    id = "ai_art_${System.currentTimeMillis()}",
                    titleEn = json.optString("titleEn", "Adaptive Reading Practice"),
                    titleAr = json.optString("titleAr", "مقال تفاعلي مخصص لمستواك"),
                    cefrLevel = json.optString("cefrLevel", cefrLevel),
                    category = json.optString("category", "Adaptive AI"),
                    readingMinutes = 4,
                    summaryAr = json.optString("summaryAr", ""),
                    paragraphs = paragraphs.ifEmpty { listOf("Practice reading carefully and attentively.") },
                    keyPhrases = phrases,
                    quiz = quiz,
                    isAiGenerated = true
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Adaptive article generation timed out", e)
            Result.failure(Exception("استغرق توليد المقال وقتاً طويلاً. حاول مجدداً."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Adaptive article generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "تعذر توليد مقال جديد حالياً."))
        }
    }
}
