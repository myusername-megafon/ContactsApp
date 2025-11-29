package com.example.contactsapp.data.dao

import androidx.room.*
import com.example.contactsapp.data.model.Reminder
import com.example.contactsapp.data.model.ReminderType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    
    @Query("SELECT * FROM reminders ORDER BY scheduledDate ASC")
    fun getAllReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE contactId = :contactId ORDER BY scheduledDate ASC")
    fun getRemindersForContact(contactId: Long): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): Reminder?
    
    @Query("SELECT * FROM reminders WHERE isCompleted = :isCompleted")
    fun getRemindersByStatus(isCompleted: Boolean): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE scheduledDate <= :currentTime AND isCompleted = 0")
    suspend fun getDueReminders(currentTime: Long): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE type = :type")
    fun getRemindersByType(type: ReminderType): Flow<List<Reminder>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<Reminder>)
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
    
    @Query("DELETE FROM reminders WHERE contactId = :contactId")
    suspend fun deleteRemindersForContact(contactId: Long)
    
    @Query("UPDATE reminders SET isCompleted = :isCompleted WHERE id = :reminderId")
    suspend fun updateReminderStatus(reminderId: Long, isCompleted: Boolean)
}

