package com.example.contactsapp

import android.app.Service
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log

class ContactCleanupService : Service() {
    private val binder = LocalBinder()
    private val service: IContactCleanupService = object : IContactCleanupService.Stub() {
        override fun cleanupDuplicateContacts(): Int {
            return try {
                removeDuplicateContacts(this@ContactCleanupService)
            } catch (e: Exception) {
                1
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): IContactCleanupService = service
    }

    private fun removeDuplicateContacts(context: Context): Int {
        val resolver = context.contentResolver
        val contacts = getContactList(resolver)

        if (contacts.isEmpty()) return 2

        val grouped = contacts.groupBy { contact ->
            val normalizedPhone =
                contact.phones.first().replace("\\D+".toRegex(), "").takeIf { it.isNotBlank() }
                    ?: this
            "${contact.name.lowercase().trim()}_$normalizedPhone"
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
            grouped.any { it.value.size == 1 } -> 2
            else -> 1
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
        var deletedCount = 0
        val uri = ContactsContract.Contacts.CONTENT_URI

        contactIds.forEach { id ->
            try {

                val contactUri = ContentUris.withAppendedId(uri, id)
                val cursor = resolver.query(
                    contactUri,
                    arrayOf(ContactsContract.Contacts._ID),
                    null, null, null
                )

                val exists = cursor?.moveToFirst() ?: false
                cursor?.close()

                if (!exists) {
                    return@forEach
                }

                val rowsDeleted = resolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                    arrayOf(id.toString())
                )
                if (rowsDeleted > 0) {
                    deletedCount++
                }
            } catch (e: Exception) {
                Log.e("CleanupService", "Error deleting contact $id", e)
            }
        }
        return deletedCount
    }

    override fun onBind(intent: Intent?): IBinder {
        removeDuplicateContacts(this@ContactCleanupService)
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean = super.onUnbind(intent)

}