package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.WaadDatabase
import com.example.data.WaadRepository

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val db = WaadDatabase.getInstance(context)
            val prefs = context.getSharedPreferences("waad_prefs_v180", Context.MODE_PRIVATE)
            val repo = WaadRepository(db.waadDao(), prefs)
            val schedule = repo.loadSchedule()
            val settings = repo.loadSettings()
            val notifSettings = repo.loadNotificationSettings()
            LessonNotificationHelper.scheduleAllLessonAlarms(
                context = context,
                schedule = schedule,
                settings = settings,
                notifSettings = notifSettings
            )
            return
        }

        if (action == LessonNotificationHelper.ACTION_LESSON_ALARM) {
            val dayName = intent.getStringExtra(LessonNotificationHelper.EXTRA_DAY_NAME) ?: "اليوم"
            val lessonNum = intent.getIntExtra(LessonNotificationHelper.EXTRA_LESSON_NUM, 1)
            val deptIdx = intent.getIntExtra(LessonNotificationHelper.EXTRA_DEPT_IDX, 0)
            val stageIdx = intent.getIntExtra(LessonNotificationHelper.EXTRA_STAGE_IDX, 0)
            val deptName = intent.getStringExtra(LessonNotificationHelper.EXTRA_DEPT_NAME) ?: "المرحلة"
            val stageName = intent.getStringExtra(LessonNotificationHelper.EXTRA_STAGE_NAME) ?: "الصف"
            val section = intent.getStringExtra(LessonNotificationHelper.EXTRA_SECTION) ?: "أ"
            val subject = intent.getStringExtra(LessonNotificationHelper.EXTRA_SUBJECT) ?: ""
            val lessonTitle = intent.getStringExtra(LessonNotificationHelper.EXTRA_LESSON_TITLE) ?: ""
            val startTime = intent.getStringExtra(LessonNotificationHelper.EXTRA_START_TIME) ?: "08:00"
            val endTime = intent.getStringExtra(LessonNotificationHelper.EXTRA_END_TIME) ?: "08:45"
            val leadMinutes = intent.getIntExtra(LessonNotificationHelper.EXTRA_LEAD_MINUTES, 0)

            if (subject.isNotBlank()) {
                LessonNotificationHelper.showLessonNotification(
                    context = context,
                    dayName = dayName,
                    lessonNum = lessonNum,
                    deptIdx = deptIdx,
                    stageIdx = stageIdx,
                    deptName = deptName,
                    stageName = stageName,
                    section = section,
                    subject = subject,
                    lessonTitle = lessonTitle,
                    startTime = startTime,
                    endTime = endTime,
                    leadMinutes = leadMinutes
                )
            }

            // Reschedule for next week
            val db = WaadDatabase.getInstance(context)
            val prefs = context.getSharedPreferences("waad_prefs_v180", Context.MODE_PRIVATE)
            val repo = WaadRepository(db.waadDao(), prefs)
            LessonNotificationHelper.scheduleAllLessonAlarms(
                context = context,
                schedule = repo.loadSchedule(),
                settings = repo.loadSettings(),
                notifSettings = repo.loadNotificationSettings()
            )
        }
    }
}
