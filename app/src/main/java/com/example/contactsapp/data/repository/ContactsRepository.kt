package com.example.contactsapp.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.contactsapp.Contact
import com.example.contactsapp.data.AppDatabase
import com.example.contactsapp.data.dao.ContactTagDao
import com.example.contactsapp.data.dao.ExtendedContactDao
import com.example.contactsapp.data.dao.ReminderDao
import com.example.contactsapp.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ContactsRepository(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val extendedContactDao: ExtendedContactDao = database.extendedContactDao()
    private val contactTagDao: ContactTagDao = database.contactTagDao()
    private val reminderDao: ReminderDao = database.reminderDao()
    private val gson = Gson()
    
    // Extended Contact operations
    fun getAllExtendedContacts(): Flow<List<ExtendedContact>> = extendedContactDao.getAllContacts()
    
    suspend fun getContactById(contactId: Long): ExtendedContact? = extendedContactDao.getContactById(contactId)
    
    fun getLockedContacts(isLocked: Boolean): Flow<List<ExtendedContact>> = extendedContactDao.getLockedContacts(isLocked)
    
    fun searchContacts(searchQuery: String): Flow<List<ExtendedContact>> = extendedContactDao.searchContacts("%$searchQuery%")
    
    fun getContactsByTag(tag: String): Flow<List<ExtendedContact>> = extendedContactDao.getContactsByTag("%$tag%")
    
    suspend fun saveExtendedContact(contact: ExtendedContact) = extendedContactDao.insertContact(contact)
    
    suspend fun saveExtendedContacts(contacts: List<ExtendedContact>) = extendedContactDao.insertContacts(contacts)
    
    suspend fun updateExtendedContact(contact: ExtendedContact) {
        val updatedContact = contact.copy(dateModified = System.currentTimeMillis())
        extendedContactDao.updateContact(updatedContact)
    }
    
    suspend fun deleteExtendedContact(contactId: Long) = extendedContactDao.deleteContactById(contactId)
    
    suspend fun getContactsWithDueReminders(currentTime: Long): List<ExtendedContact> = 
        extendedContactDao.getContactsWithDueReminders(currentTime)
    
    suspend fun getContactsWithBirthday(): List<ExtendedContact> = extendedContactDao.getContactsWithBirthday()
    
    // Tag operations
    fun getTagsForContact(contactId: Long): Flow<List<ContactTag>> = contactTagDao.getTagsForContact(contactId)
    
    fun getAllTags(): Flow<List<String>> = contactTagDao.getAllTags()
    
    suspend fun addTag(contactId: Long, tagName: String) {
        contactTagDao.insertTag(ContactTag(contactId = contactId, tagName = tagName))
    }
    
    suspend fun removeTag(contactId: Long, tagName: String) {
        contactTagDao.deleteTagForContact(contactId, tagName)
    }
    
    suspend fun deleteAllTagsForContact(contactId: Long) {
        contactTagDao.deleteTagsForContact(contactId)
    }
    
    // Reminder operations
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()
    
    fun getRemindersForContact(contactId: Long): Flow<List<Reminder>> = reminderDao.getRemindersForContact(contactId)
    
    fun getDueReminders(): Flow<List<Reminder>> = flow {
        val currentTime = System.currentTimeMillis()
        val dueReminders = reminderDao.getDueReminders(currentTime)
        emit(dueReminders)
    }
    
    suspend fun createReminder(
        contactId: Long,
        type: ReminderType,
        title: String,
        description: String?,
        scheduledDate: Long
    ): Long {
        val reminder = Reminder(
            contactId = contactId,
            type = type,
            title = title,
            description = description,
            scheduledDate = scheduledDate
        )
        return reminderDao.insertReminder(reminder)
    }
    
    suspend fun updateReminderStatus(reminderId: Long, isCompleted: Boolean) {
        reminderDao.updateReminderStatus(reminderId, isCompleted)
    }
    
    suspend fun deleteReminder(reminderId: Long) {
        reminderDao.getReminderById(reminderId)?.let {
            reminderDao.deleteReminder(it)
        }
    }
    
    // Convert Contact to ExtendedContact
    fun convertToExtendedContact(contact: Contact): ExtendedContact {
        return ExtendedContact(
            contactId = contact.id,
            name = contact.name,
            phones = gson.toJson(contact.phones),
            emails = gson.toJson(emptyList<String>()),
            photoUri = contact.photoUri,
            birthday = null,
            socialNetworks = null,
            tags = null,
            biography = null,
            notes = null,
            lastCallDate = null,
            lastMessageDate = null,
            lastMeetingDate = null,
            reminderToCall = null,
            reminderToCongratulate = false,
            reminderReason = null,
            isLocked = false,
            dateAdded = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )
    }
    
    fun parseSocialNetworks(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun parseTags(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Load contacts from phone
    suspend fun loadSystemContacts(context: Context): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        val resolver = context.contentResolver
        
        val cursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI
            ),
            null, null,
            "${ContactsContract.Contacts.DISPLAY_NAME} COLLATE NOCASE ASC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: ""
                val photoUri = it.getString(photoIndex)
                val phones = getContactPhones(resolver, id)
                contactsList.add(Contact(id, name, phones, photoUri))
            }
        }
        
        return contactsList
    }
    
    private suspend fun getContactPhones(
        resolver: android.content.ContentResolver,
        contactId: Long
    ): List<String> {
        val phones = mutableListOf<String>()
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )
        
        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                it.getString(numberIndex)?.let { number ->
                    phones.add(number.replace("\\D+".toRegex(), ""))
                }
            }
        }
        
        return phones
    }
}

