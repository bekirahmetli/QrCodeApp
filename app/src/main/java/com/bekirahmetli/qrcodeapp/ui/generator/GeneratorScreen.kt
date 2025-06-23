package com.bekirahmetli.qrcodeapp.ui.generator

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen() {
    var input by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("text") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("QR Kod Oluştur") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tür: ")
                Spacer(modifier = Modifier.width(8.dp))
                DropdownMenuBox(type, onTypeChange = { type = it })
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(when(type) {
                    "text" -> "Metin"
                    "url" -> "Bağlantı"
                    "phone" -> "Telefon Numarası"
                    "wifi" -> "Wi-Fi: WIFI:T:WPA;S:SSID;P:password;;"
                    else -> "Metin"
                }) },
                keyboardOptions = KeyboardOptions(keyboardType = when(type) {
                    "phone" -> KeyboardType.Phone
                    else -> KeyboardType.Text
                }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val content = when(type) {
                    "url" -> input
                    "phone" -> "tel:$input"
                    "wifi" -> input
                    else -> input
                }
                qrBitmap = generateQrCode(content)
            }) {
                Text("QR Kod Oluştur")
            }
            Spacer(modifier = Modifier.height(24.dp))
            qrBitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = "QR Kod", modifier = Modifier.size(220.dp))
            }
        }
    }
}

@Composable
fun DropdownMenuBox(selected: String, onTypeChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(
                when(selected) {
                    "text" -> "Metin"
                    "url" -> "Bağlantı"
                    "phone" -> "Telefon"
                    "wifi" -> "Wi-Fi"
                    else -> "Metin"
                }
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Metin") }, onClick = { onTypeChange("text"); expanded = false })
            DropdownMenuItem(text = { Text("Bağlantı") }, onClick = { onTypeChange("url"); expanded = false })
            DropdownMenuItem(text = { Text("Telefon") }, onClick = { onTypeChange("phone"); expanded = false })
            DropdownMenuItem(text = { Text("Wi-Fi") }, onClick = { onTypeChange("wifi"); expanded = false })
        }
    }
}

fun generateQrCode(content: String): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
} 