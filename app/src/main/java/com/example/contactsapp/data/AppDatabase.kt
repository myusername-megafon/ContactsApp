package com.example.contactsapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.contactsapp.data.dao.ContactTagDao
import com.example.contactsapp.data.dao.ExtendedContactDao
import com.example.contactsapp.data.dao.ReminderDao
import com.example.contactsapp.data.model.ContactTag
import com.example.contactsapp.data.model.ExtendedContact
import com.example.contactsapp.data.model.Reminder

@Database(
    entities = [ExtendedContact::class, ContactTag::class, Reminder::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun extendedContactDao(): ExtendedContactDao
    abstract fun contactTagDao(): ContactTagDao
    abstract fun reminderDao(): ReminderDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "contacts_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

