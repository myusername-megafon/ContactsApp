package com.example.contactsapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "extended_contacts")
data class ExtendedContact(
    @PrimaryKey
    val contactId: Long,
    
    // Базовая информация
    val name: String,
    val phones: String, // JSON array of strings
    val emails: String, // JSON array of strings
    val photoUri: String? = null,
    val birthday: Long? = null, // timestamp
    
    // Социальные сети
    val socialNetworks: String? = null, // JSON object with platform -> URL
    
    // Теги
    val tags: String? = null, // JSON array of strings
    
    // Биография и заметки
    val biography: String? = null,
    val notes: String? = null,
    
    // Последние взаимодействия
    val lastCallDate: Long? = null, // timestamp
    val lastMessageDate: Long? = null, // timestamp
    val lastMeetingDate: Long? = null, // timestamp
    
    // Умные напоминания
    val reminderToCall: Long? = null, // timestamp when to call back
    val reminderToCongratulate: Boolean = false,
    val reminderReason: String? = null, // причина напоминания
    
    // Безопасность
    val isLocked: Boolean = false, // требует биометрическую аутентификацию
    
    // Метаданные
    val dateAdded: Long = Date().time,
    val dateModified: Long = Date().time
)

