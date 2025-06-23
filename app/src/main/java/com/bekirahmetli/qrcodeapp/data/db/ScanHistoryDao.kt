package com.bekirahmetli.qrcodeapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bekirahmetli.qrcodeapp.data.model.ScanHistoryItem

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scanhistoryitem ORDER BY timestamp DESC")
    suspend fun getAll(): List<ScanHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScanHistoryItem)

    @Query("DELETE FROM scanhistoryitem")
    suspend fun clear()
} 