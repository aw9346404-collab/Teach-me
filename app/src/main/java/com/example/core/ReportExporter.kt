package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.example.data.model.AssessmentLog
import com.example.data.model.LearnerProfile
import com.example.data.model.MistakeRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun exportAndSharePdfReport(
        context: Context,
        profile: LearnerProfile,
        mistakes: List<MistakeRecord>,
        assessments: List<AssessmentLog>
    ) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "Bayan_English_Progress_Report.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.parseColor("#FAF7F2") }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val headerPaint = Paint().apply {
            color = Color.parseColor("#46685B")
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, 595f, 96f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#E3ECE7")
            textSize = 12f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#46685B")
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#232825")
            textSize = 12f
            isAntiAlias = true
        }
        val redPaint = Paint().apply {
            color = Color.parseColor("#C62828")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val orangePaint = Paint().apply {
            color = Color.parseColor("#E65100")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("Teach me (B1 -> B2) • Periodic Report", 36f, 44f, titlePaint)
        canvas.drawText("Learner: ${profile.displayName}  |  Generated: $dateStr", 36f, 70f, subtitlePaint)

        var y = 132f
        canvas.drawText("1. Executive Summary & CEFR Stage", 36f, y, sectionPaint)
        y += 24f
        canvas.drawText("Current CEFR Stage: ${profile.cefrLevel}   |   Total XP: ${profile.xp}   |   Streak: ${profile.streakDays} days", 36f, y, bodyPaint)
        y += 20f
        canvas.drawText("Goal: ${profile.learningGoal}", 36f, y, bodyPaint)
        y += 20f
        canvas.drawText("Articles Completed: ${profile.articlesRead}  |  Quizzes: ${profile.quizzesCompleted}  |  Voice Sessions: ${profile.voiceSessionsCount}", 36f, y, bodyPaint)

        y += 36f
        canvas.drawText("2. Skill Mastery Breakdown (0 - 100)", 36f, y, sectionPaint)
        y += 24f
        canvas.drawText("• Reading: ${profile.readingScore}%    • Listening: ${profile.listeningScore}%    • Speaking: ${profile.speakingScore}%", 36f, y, bodyPaint)
        y += 20f
        canvas.drawText("• Grammar: ${profile.grammarScore}%    • Vocabulary: ${profile.vocabularyScore}%", 36f, y, bodyPaint)

        val redCount = mistakes.count { it.severity == "RED_CRITICAL" }
        val orangeCount = mistakes.count { it.severity == "ORANGE_MINOR" }
        val masteredCount = mistakes.count { it.mastered }

        y += 36f
        canvas.drawText("3. Linguistic Error Analytics (Teacher & Parent Review)", 36f, y, sectionPaint)
        y += 24f
        canvas.drawText("Critical Meaning-Changing Errors (Red): $redCount", 36f, y, redPaint)
        y += 20f
        canvas.drawText("Minor Non-Meaning Style/Accent Slips (Orange): $orangeCount", 36f, y, orangePaint)
        y += 20f
        canvas.drawText("Mistakes Reviewed & Mastered: $masteredCount / ${mistakes.size}", 36f, y, bodyPaint)

        y += 36f
        canvas.drawText("4. Recent Assessments & Activity Log", 36f, y, sectionPaint)
        y += 22f
        assessments.take(8).forEach { log ->
            canvas.drawText(
                "- [${log.activityType}] ${log.title.take(45)} | Score: ${log.score}% | +${log.xpEarned} XP",
                36f,
                y,
                bodyPaint
            )
            y += 18f
        }

        doc.finishPage(page)
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Teach me Periodic Report - ${profile.displayName} (${profile.cefrLevel})"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                buildLmsSummaryText(profile, mistakes)
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير مع المعلم / ولي الأمر / Google Classroom"))
    }

    fun shareTextSummaryForLms(
        context: Context,
        profile: LearnerProfile,
        mistakes: List<MistakeRecord>
    ) {
        val summary = buildLmsSummaryText(profile, mistakes)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Teach me LMS Progress - ${profile.displayName}")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        context.startActivity(Intent.createChooser(intent, "تصدير إلى منصات التعليم عن بعد"))
    }

    fun addStudySessionToCalendar(context: Context, profile: LearnerProfile) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, profile.reminderHour.toInt())
            set(Calendar.MINUTE, profile.reminderMinute.toInt())
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Teach me (${profile.cefrLevel} -> B2) Study Session")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Daily B1-B2 Article, AI Pronunciation Clinic & Mistake Bank Review"
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startCal.timeInMillis + 20 * 60 * 1000L)
        }
        context.startActivity(intent)
    }

    private fun buildLmsSummaryText(
        profile: LearnerProfile,
        mistakes: List<MistakeRecord>
    ): String {
        val red = mistakes.count { it.severity == "RED_CRITICAL" }
        val orange = mistakes.count { it.severity == "ORANGE_MINOR" }
        return """
            Teach me (B1–B2) • Learner Progress Report
            Student: ${profile.displayName}
            CEFR Level: ${profile.cefrLevel} | XP: ${profile.xp} | Streak: ${profile.streakDays} days
            Skills -> Reading: ${profile.readingScore}%, Listening: ${profile.listeningScore}%, Speaking: ${profile.speakingScore}%, Grammar: ${profile.grammarScore}%, Vocabulary: ${profile.vocabularyScore}%
            Error Breakdown -> Critical (Red): $red | Minor (Orange): $orange | Mastered: ${mistakes.count { it.mastered }}
        """.trimIndent()
    }
}
