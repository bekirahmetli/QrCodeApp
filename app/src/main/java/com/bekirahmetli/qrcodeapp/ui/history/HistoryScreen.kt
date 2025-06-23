package com.bekirahmetli.qrcodeapp.ui.history

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekirahmetli.qrcodeapp.data.db.AppDatabase
import com.bekirahmetli.qrcodeapp.data.model.ScanHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabaseSingleton.getInstance(context) }
    var history by remember { mutableStateOf(listOf<ScanHistoryItem>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            history = db.scanHistoryDao().getAll()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Geçmiş") })
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Henüz hiç QR kod okutulmadı.", fontSize = 18.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(history) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.content, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tarih: ${item.timestamp.toDateString()}", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Button(onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, item.content)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                }) { Text("Paylaş") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    val clipboard = androidx.core.content.ContextCompat.getSystemService(context, android.content.ClipboardManager::class.java)
                                    val clip = android.content.ClipData.newPlainText("QR Sonucu", item.content)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                                }) { Text("Kopyala") }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

object AppDatabaseSingleton {
    @Volatile
    private var INSTANCE: AppDatabase? = null
    fun getInstance(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: androidx.room.Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "scan_history_db"
            ).build().also { INSTANCE = it }
        }
    }
} 