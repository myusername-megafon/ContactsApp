package com.example.contactsapp

import android.Manifest
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.provider.ContactsContract
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenViewModel : ViewModel() {

    private val _state = mutableStateOf(MainScreenState())
    val state: State<MainScreenState> get() = _state

    private var isBound = false
    private var cleanupService: IContactCleanupService? = null

    fun checkPermissionAndLoadContacts(context: Context) {

        val hasRead = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasWrite = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        _state.value = _state.value.copy(permissionGranted = hasRead && hasWrite)
        if (hasRead && hasWrite) loadContacts(context)
        _state.value = _state.value.copy(isLoading = false)
    }

    fun updatePermission(granted: Boolean) {
        _state.value = _state.value.copy(permissionGranted = granted)
    }

    fun loadContacts(context: Context) {
        viewModelScope.launch {
            try {
                val contacts = withContext(Dispatchers.IO) {
                    getContacts(context)
                }
                _state.value = _state.value.copy(contactsList = contacts)
            } catch (e: Exception) {
                _state.value = _state.value.copy(cleanupStatus = CleanedStatusModel.ERROR)
            }
        }
    }

    private fun getContacts(context: Context): List<Contact> {
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

    private fun getContactPhones(resolver: ContentResolver, contactId: Long): List<String> {
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


    fun bindService(context: Context) {
        val intent = Intent(context, ContactCleanupService::class.java)
        context.startService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            cleanupService = IContactCleanupService.Stub.asInterface(service)
            isBound = true

            viewModelScope.launch {
                runCleanup()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            cleanupService = null
            isBound = false
        }
    }

    suspend fun runCleanup() {
        if (!isBound || cleanupService == null) {
            _state.value = _state.value.copy(cleanupStatus = CleanedStatusModel.ERROR)
            return
        }

        try {
            val result = withContext(Dispatchers.IO) {
                cleanupService!!.cleanupDuplicateContacts()
            }
            val status = when (result) {
                0 -> CleanedStatusModel.SUCCESS
                2 -> CleanedStatusModel.NO_DUPLICATES
                else -> CleanedStatusModel.ERROR
            }
            _state.value = _state.value.copy(cleanupStatus = status)
        } catch (e: Exception) {
            _state.value = _state.value.copy(cleanupStatus = CleanedStatusModel.ERROR)
        }
    }

    fun getGroupedContactsList(contacts: List<Contact>): Map<Char, List<Contact>> {
        return contacts.sortedBy { it.name }.groupBy { it.name.first().uppercaseChar() }
    }
}