package com.bekirahmetli.qrcodeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ScanHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val type: String,
    val timestamp: Long
) 