package com.example.contactsapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.contactsapp.R

object NotificationManager {
    
    private const val CHANNEL_ID = "reminder_channel"
    private const val CHANNEL_NAME = "Напоминания контактов"
    
    fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о напоминаниях контактов"
            }
            
            val notificationManager = context.getSystemService(
                AndroidNotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun createReminderNotification(
        context: Context,
        title: String,
        description: String,
        reminderId: Long
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(
            AndroidNotificationManager::class.java
        )
        notificationManager.notify(reminderId.toInt(), notification)
    }
}

