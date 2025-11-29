package com.example.contactsapp.data.dao

import androidx.room.*
import com.example.contactsapp.data.model.ExtendedContact
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtendedContactDao {
    
    @Query("SELECT * FROM extended_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ExtendedContact>>
    
    @Query("SELECT * FROM extended_contacts WHERE contactId = :contactId")
    suspend fun getContactById(contactId: Long): ExtendedContact?
    
    @Query("SELECT * FROM extended_contacts WHERE isLocked = :isLocked")
    fun getLockedContacts(isLocked: Boolean): Flow<List<ExtendedContact>>
    
    @Query("SELECT * FROM extended_contacts WHERE name LIKE :searchQuery OR phones LIKE :searchQuery")
    fun searchContacts(searchQuery: String): Flow<List<ExtendedContact>>
    
    @Query("SELECT * FROM extended_contacts WHERE tags LIKE :tag")
    fun getContactsByTag(tag: String): Flow<List<ExtendedContact>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ExtendedContact)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ExtendedContact>)
    
    @Update
    suspend fun updateContact(contact: ExtendedContact)
    
    @Delete
    suspend fun deleteContact(contact: ExtendedContact)
    
    @Query("DELETE FROM extended_contacts WHERE contactId = :contactId")
    suspend fun deleteContactById(contactId: Long)
    
    @Query("SELECT * FROM extended_contacts WHERE reminderToCall IS NOT NULL AND reminderToCall <= :currentTime")
    suspend fun getContactsWithDueReminders(currentTime: Long): List<ExtendedContact>
    
    @Query("SELECT * FROM extended_contacts WHERE birthday IS NOT NULL")
    suspend fun getContactsWithBirthday(): List<ExtendedContact>
}

