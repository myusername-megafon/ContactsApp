package com.example.contactsapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val type: ReminderType,
    val title: String,
    val description: String? = null,
    val scheduledDate: Long, // timestamp
    val isCompleted: Boolean = false,
    val dateCreated: Long = Date().time
)

enum class ReminderType {
    CALL_BACK, // Позвонить через определенное время
    BIRTHDAY, // Поздравить с днем рождения
    ANNIVERSARY, // Важная годовщина
    CUSTOM // Произвольное напоминание
}

