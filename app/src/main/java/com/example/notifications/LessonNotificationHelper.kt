package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AppSettings
import com.example.model.ScheduleNotificationSettings
import com.example.model.ScheduledClass
import com.example.model.WaadCalculator
import java.util.Calendar

object LessonNotificationHelper {
    const val CHANNEL_ID = "waad_smart_lesson_alerts"
    const val ACTION_LESSON_ALARM = "com.example.ACTION_LESSON_ALARM"

    const val EXTRA_FROM_NOTIFICATION = "extra_from_lesson_notification"
    const val EXTRA_DEPT_IDX = "extra_dept_idx"
    const val EXTRA_STAGE_IDX = "extra_stage_idx"
    const val EXTRA_DEPT_NAME = "extra_dept_name"
    const val EXTRA_STAGE_NAME = "extra_stage_name"
    const val EXTRA_SECTION = "extra_section"
    const val EXTRA_SUBJECT = "extra_subject"
    const val EXTRA_LESSON_TITLE = "extra_lesson_title"
    const val EXTRA_LESSON_NUM = "extra_lesson_num"
    const val EXTRA_DAY_NAME = "extra_day_name"
    const val EXTRA_START_TIME = "extra_start_time"
    const val EXTRA_END_TIME = "extra_end_time"
    const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات الحصص الدراسية",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات ذكية لتنبيه المدرس بمواعيد الحصص الدراسية والصفوف"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showLessonNotification(
        context: Context,
        dayName: String,
        lessonNum: Int,
        deptIdx: Int,
        stageIdx: Int,
        deptName: String,
        stageName: String,
        section: String,
        subject: String,
        lessonTitle: String,
        startTime: String,
        endTime: String,
        leadMinutes: Int = 0
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureNotificationChannel(context)

        val ordinalName = WaadCalculator.getLessonOrdinalName(lessonNum)
        val displayTitle = if (lessonTitle.isNotBlank()) {
            "$subject — $lessonTitle"
        } else {
            subject
        }

        val timingPrefix = if (leadMinutes > 0) {
            "⏰ تبدأ بعد $leadMinutes د: $ordinalName"
        } else {
            "🔔 حان موعد $ordinalName الآن"
        }

        val notificationTitle = "$timingPrefix • $displayTitle"
        val shortContent = "الصف: $stageName (شعبة $section) • الوقت: $startTime - $endTime"
        val bigText = buildString {
            append("📘 اسم الحصة: $ordinalName — $subject")
            if (lessonTitle.isNotBlank()) {
                append(" ($lessonTitle)")
            }
            append("\n")
            append("🏫 المرحلة والصف: $deptName - $stageName • شعبة ($section)\n")
            append("⏱️ توقيت الحصة: من $startTime إلى $endTime ($dayName)\n")
            append("اضغط على الإشعار للانتقال المباشر إلى طلاب الشعبة.")
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            putExtra(EXTRA_DEPT_IDX, deptIdx)
            putExtra(EXTRA_STAGE_IDX, stageIdx)
            putExtra(EXTRA_SECTION, section)
            putExtra(EXTRA_SUBJECT, subject)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1000 + lessonNum,
            tapIntent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notificationTitle)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(3000 + lessonNum, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun scheduleAllLessonAlarms(
        context: Context,
        schedule: Map<String, ScheduledClass>,
        settings: AppSettings,
        notifSettings: ScheduleNotificationSettings
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val dayToCalendarDow = mapOf(
            "الأحد" to Calendar.SUNDAY,
            "الإثنين" to Calendar.MONDAY,
            "الثلاثاء" to Calendar.TUESDAY,
            "الأربعاء" to Calendar.WEDNESDAY,
            "الخميس" to Calendar.THURSDAY
        )

        val now = System.currentTimeMillis()
        val struct = settings.structure

        WaadCalculator.WEEK_DAYS.forEachIndexed { dayIdx, dayName ->
            val targetDow = dayToCalendarDow[dayName] ?: Calendar.SUNDAY
            for (lessonNum in 1..6) {
                val requestCode = 2000 + dayIdx * 10 + lessonNum
                val key = "${dayName}_$lessonNum"
                val slot = schedule[key]

                val baseIntent = Intent(context, LessonAlarmReceiver::class.java).apply {
                    action = ACTION_LESSON_ALARM
                }
                val existingPi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    baseIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (existingPi != null) {
                    alarmManager.cancel(existingPi)
                }

                if (!notifSettings.enabled || slot == null || slot.subject.isBlank() || !slot.notifyEnabled) {
                    continue
                }

                val (startStr, endStr) = WaadCalculator.resolveLessonTimes(lessonNum, slot)
                val startMin = WaadCalculator.parseTimeMinutes(startStr) ?: continue
                val triggerMinutes = (startMin - notifSettings.leadMinutes).coerceAtLeast(0)
                val triggerHour = triggerMinutes / 60
                val triggerMin = triggerMinutes % 60

                val dept = struct.getOrElse(slot.deptIdx) { struct.firstOrNull() }
                val stage = dept?.stages?.getOrElse(slot.stageIdx) { dept.stages.firstOrNull() }

                val alarmIntent = Intent(context, LessonAlarmReceiver::class.java).apply {
                    action = ACTION_LESSON_ALARM
                    putExtra(EXTRA_DAY_NAME, dayName)
                    putExtra(EXTRA_LESSON_NUM, lessonNum)
                    putExtra(EXTRA_DEPT_IDX, slot.deptIdx)
                    putExtra(EXTRA_STAGE_IDX, slot.stageIdx)
                    putExtra(EXTRA_DEPT_NAME, dept?.name ?: "المرحلة")
                    putExtra(EXTRA_STAGE_NAME, stage?.name ?: "الصف")
                    putExtra(EXTRA_SECTION, slot.section)
                    putExtra(EXTRA_SUBJECT, slot.subject)
                    putExtra(EXTRA_LESSON_TITLE, slot.lessonTitle)
                    putExtra(EXTRA_START_TIME, startStr)
                    putExtra(EXTRA_END_TIME, endStr)
                    putExtra(EXTRA_LEAD_MINUTES, notifSettings.leadMinutes)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, targetDow)
                    set(Calendar.HOUR_OF_DAY, triggerHour)
                    set(Calendar.MINUTE, triggerMin)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                cal.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                cal.timeInMillis,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            cal.timeInMillis,
                            pendingIntent
                        )
                    }
                } catch (e: Exception) {
                    // Fallback if exact alarms are restricted
                    try {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}
