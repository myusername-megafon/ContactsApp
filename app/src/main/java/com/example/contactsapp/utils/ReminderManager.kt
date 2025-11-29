package com.example.contactsapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.contactsapp.data.model.Reminder
import com.example.contactsapp.data.model.ReminderType
import java.util.Calendar

class ReminderManager(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun scheduleReminder(reminder: Reminder) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_description", reminder.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        when (reminder.type) {
            ReminderType.BIRTHDAY -> {
                // Schedule for birthday
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledDate,
                    pendingIntent
                )
            }
            ReminderType.CALL_BACK -> {
                // Schedule call back reminder
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledDate,
                    pendingIntent
                )
            }
            ReminderType.ANNIVERSARY -> {
                // Schedule anniversary reminder
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledDate,
                    pendingIntent
                )
            }
            ReminderType.CUSTOM -> {
                // Schedule custom reminder
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledDate,
                    pendingIntent
                )
            }
        }
    }
    
    fun cancelReminder(reminder: Reminder) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    fun scheduleBirthdayReminder(contactId: Long, birthdayMillis: Long, contactName: String) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = birthdayMillis
        }
        
        // Schedule for next birthday
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        calendar.set(Calendar.YEAR, currentYear)
        
        if (calendar.timeInMillis <= now.timeInMillis) {
            calendar.add(Calendar.YEAR, 1)
        }
        
        val reminder = Reminder(
            contactId = contactId,
            type = ReminderType.BIRTHDAY,
            title = "День рождения $contactName",
            description = "Не забудьте поздравить!",
            scheduledDate = calendar.timeInMillis
        )
        
        scheduleReminder(reminder)
    }
    
    fun scheduleCallBackReminder(contactId: Long, contactName: String, daysFromNow: Int) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        
        val reminder = Reminder(
            contactId = contactId,
            type = ReminderType.CALL_BACK,
            title = "Позвонить $contactName",
            description = "Вернуться к звонку",
            scheduledDate = calendar.timeInMillis
        )
        
        scheduleReminder(reminder)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        val title = intent.getStringExtra("reminder_title") ?: "Напоминание"
        val description = intent.getStringExtra("reminder_description")
        
        // Create notification
        NotificationManager.createReminderNotification(
            context = context,
            title = title,
            description = description ?: "",
            reminderId = reminderId
        )
    }
}

