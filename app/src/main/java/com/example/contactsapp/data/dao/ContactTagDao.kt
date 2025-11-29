package com.example.contactsapp.data.dao

import androidx.room.*
import com.example.contactsapp.data.model.ContactTag
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactTagDao {
    
    @Query("SELECT * FROM contact_tags WHERE contactId = :contactId")
    fun getTagsForContact(contactId: Long): Flow<List<ContactTag>>
    
    @Query("SELECT DISTINCT tagName FROM contact_tags")
    fun getAllTags(): Flow<List<String>>
    
    @Query("SELECT COUNT(*) FROM contact_tags WHERE tagName = :tagName")
    suspend fun getTagCount(tagName: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: ContactTag)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<ContactTag>)
    
    @Delete
    suspend fun deleteTag(tag: ContactTag)
    
    @Query("DELETE FROM contact_tags WHERE contactId = :contactId")
    suspend fun deleteTagsForContact(contactId: Long)
    
    @Query("DELETE FROM contact_tags WHERE contactId = :contactId AND tagName = :tagName")
    suspend fun deleteTagForContact(contactId: Long, tagName: String)
}

