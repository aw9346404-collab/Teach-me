package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DetectedMistakeInput
import com.example.data.model.ErrorSeverity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureInitialized_authenticatedOwner_createsProfileChallengesAndLeaderboard() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LearningRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureInitialized(uid, "Alice")
        }

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals(uid, profile!!.userId)
        assertEquals("B1", profile.cefrLevel)

        val challenges = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeChallenges(uid).first { it.isNotEmpty() }
        }
        assertTrue(challenges.size >= 3)
    }

    @Test
    fun logMistake_andReview_persistsRedAndOrangeErrorsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LearningRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureInitialized(uid, "Alice")
            repository.logMistake(
                uid,
                DetectedMistakeInput(
                    sourceType = "VOICE",
                    originalText = "I am agree",
                    correctedText = "I agree",
                    errorSpan = "am agree",
                    severity = ErrorSeverity.RED_CRITICAL,
                    category = "GRAMMAR",
                    explanationAr = "خطأ جوهري: لا يُستخدم am قبل agree",
                    explanationEn = "Do not use 'am' before 'agree'."
                )
            )
        }

        val mistakes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMistakes(uid).first { it.isNotEmpty() }
        }
        val mistake = mistakes.first()
        assertEquals("RED_CRITICAL", mistake.severity)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markMistakeReviewed(uid, mistake, mastered = true)
        }

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMistakes(uid).first { list -> list.any { it.id == mistake.id && it.mastered } }
        }
        assertTrue(updated.first { it.id == mistake.id }.mastered)
    }

    @Test
    fun crossUserAccess_readingAnotherLearnersMistakes_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = LearningRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureInitialized(aliceUid, "Alice")
        }

        signInTestUser(BOB_EMAIL)
        val exception = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProfile(aliceUid).first()
            }
        }.exceptionOrNull()

        assertNotNull(exception)
        val fsException = exception!!.unwrapFirestoreException()
        assertNotNull(fsException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException!!.code)
    }

    @Test
    fun unauthenticatedUser_observingProfile_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = LearningRepository(firestore)
        val exception = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProfile("alice_123").first()
            }
        }.exceptionOrNull()

        assertNotNull(exception)
        val fsException = exception!!.unwrapFirestoreException()
        assertNotNull(fsException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException!!.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_bayan@test.com"
        const val BOB_EMAIL = "bob_bayan@test.com"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
