package com.example.contactsapp

import android.app.Application
import com.example.contactsapp.utils.NotificationManager

class ContactsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channel
        NotificationManager.createNotificationChannel(this)
    }
}

