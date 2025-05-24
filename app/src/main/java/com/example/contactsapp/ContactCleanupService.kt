package com.example.contactsapp

import android.app.Service
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.ContactsContract

class ContactCleanupService : Service() {

    private val binder = object : IContactCleanupService.Stub() {
        override fun cleanupDuplicateContacts(): Int {
            return removeDuplicateContacts(this@ContactCleanupService)
        }
    }

    private fun removeDuplicateContacts(context: Context): Int {
        val resolver = context.contentResolver
        val contacts = getContactList(resolver)

        if (contacts.isEmpty()) {
            return 2
        }

        val grouped = contacts.groupBy { contact ->
            "${contact.name.lowercase().trim()}_${contact.phones.first().replace("\\D+".toRegex(), "")}"
        }

        var deletedCount = 0
        grouped.values.forEach { group ->
            if (group.size > 1) {
                val toDelete = group.drop(1)
                deletedCount += deleteContacts(resolver, toDelete.map { it.id })
            }
        }

        return when {
            deletedCount > 0 -> 0
            grouped.any { it.value.size > 1 } -> 1
            else -> 2
        }
    }

    private fun getContactList(resolver: ContentResolver): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: ""
                val number = it.getString(numberIndex)?.replace("\\D+".toRegex(), "") ?: continue
                contacts.add(Contact(id, name, listOf(number)))
            }
        }

        return contacts
    }

    private fun deleteContacts(resolver: ContentResolver, contactIds: List<Long>): Int {
        var count = 0
        contactIds.forEach { id ->
                val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
                val rowsDeleted = resolver.delete(uri, null, null)
                if (rowsDeleted > 0) {
                    count++
                }
        }
        return count
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onUnbind(intent: Intent?) = super.onUnbind(intent)

}