package com.bekirahmetli.qrcodeapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bekirahmetli.qrcodeapp.data.model.ScanHistoryItem

@Database(entities = [ScanHistoryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
} 