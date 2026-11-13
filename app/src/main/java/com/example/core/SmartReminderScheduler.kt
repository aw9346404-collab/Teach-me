package com.example.core

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

private const val CHANNEL_ID = "bayan_smart_reminders"
private const val NOTIFICATION_ID = 4021

class SmartReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        SmartReminderScheduler.ensureChannel(manager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingMistakes = intent.getIntExtra("pending_mistakes", 0)
        val streak = intent.getIntExtra("streak_days", 1)
        val body = if (pendingMistakes > 0) {
            "لديك $pendingMistakes أخطاء بانتظار المراجعة — حافظ على سلسلتك ($streak أيام) بجلسة هادئة لمدة 10 دقائق."
        } else {
            "مقالك اليومي وجلسة النطق في مستوى B1–B2 جاهزان — حافظ على سلسلتك ($streak أيام)."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("موعد التعلم الهادئ • Teach me")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}

object SmartReminderScheduler {

    fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "التنبيهات التعليمية الذكية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكيرات مخصصة لمراجعة الأخطاء والمقال اليومي"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        pendingMistakesCount: Int,
        streakDays: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SmartReminderReceiver::class.java).apply {
            putExtra("pending_mistakes", pendingMistakesCount)
            putExtra("streak_days", streakDays)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pending)
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    fun sendImmediatePreviewNotification(
        context: Context,
        pendingMistakesCount: Int,
        streakDays: Int
    ) {
        val receiver = SmartReminderReceiver()
        val intent = Intent(context, SmartReminderReceiver::class.java).apply {
            putExtra("pending_mistakes", pendingMistakesCount)
            putExtra("streak_days", streakDays)
        }
        receiver.onReceive(context, intent)
    }
}
