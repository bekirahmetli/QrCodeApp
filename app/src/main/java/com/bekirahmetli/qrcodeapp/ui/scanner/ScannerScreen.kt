package com.bekirahmetli.qrcodeapp.ui.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import android.graphics.BitmapFactory
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.bekirahmetli.qrcodeapp.data.db.AppDatabase
import com.bekirahmetli.qrcodeapp.data.model.ScanHistoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.Stroke
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var qrResult by remember { mutableStateOf<String?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    var galleryQrResult by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val db = remember { AppDatabaseSingleton.getInstance(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val result = barcodes.firstOrNull()?.rawValue
                        if (result != null) {
                            galleryQrResult = result
                            // Geçmişe kaydet
                            CoroutineScope(Dispatchers.IO).launch {
                                db.scanHistoryDao().insert(
                                    ScanHistoryItem(
                                        content = result,
                                        type = "gallery",
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        } else {
                            galleryQrResult = "QR kod bulunamadı."
                        }
                    }
                    .addOnFailureListener {
                        galleryQrResult = "Görselden QR okunamadı."
                    }
            } else {
                galleryQrResult = "Görsel açılamadı."
            }
        }
    }

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Kod Tara") },
                actions = {
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        enabled = CameraFlashStateHolder.hasFlash
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = if (flashEnabled) "Flaş Kapat" else "Flaş Aç"
                        )
                    }
                    IconButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                        Icon(
                            imageVector = Icons.Filled.Photo,
                            contentDescription = "Galeriden Seç"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                hasCameraPermission -> {
                    CameraPreview(
                        flashEnabled = flashEnabled,
                        onQrCodeScanned = { qr ->
                            qrResult = qr
                            // Titreşim
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            vibrator.vibrate(100)
                            // Geçmişe kaydet
                            CoroutineScope(Dispatchers.IO).launch {
                                db.scanHistoryDao().insert(
                                    ScanHistoryItem(
                                        content = qr,
                                        type = "scan",
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    )
                    qrResult?.let {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("QR Sonucu:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                getQrTypeActionButtons(context, it)()
                            }
                        }
                    }
                    galleryQrResult?.let {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 120.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Galeri QR Sonucu:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                getQrTypeActionButtons(context, it)()
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Kamera izni gerekli.", color = Color.Red, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("Kamera izni ver")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(flashEnabled: Boolean, onQrCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraHasFlash by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    LaunchedEffect(flashEnabled, cameraControl) {
        cameraControl?.let { control ->
            try {
                control.enableTorch(flashEnabled)
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val frameLayout = android.widget.FrameLayout(ctx)
                val previewView = androidx.camera.view.PreviewView(ctx)
                frameLayout.addView(previewView, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                ))
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also {
                            it.setAnalyzer(cameraExecutor, QRCodeAnalyzer(onQrCodeScanned))
                        }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageAnalyzer
                        )
                        cameraHasFlash = camera.cameraInfo.hasFlashUnit()
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                frameLayout
            },
            modifier = Modifier.fillMaxSize()
        )
        //çerçeve
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(240.dp)) {
                drawRect(
                    color = Color.Green,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
    }

    LaunchedEffect(cameraHasFlash) {
        CameraFlashStateHolder.hasFlash = cameraHasFlash
    }
}

class QRCodeAnalyzer(val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private var lastResult: String? = null
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.rawValue != null && barcode.rawValue != lastResult) {
                            lastResult = barcode.rawValue
                            onQrCodeScanned(barcode.rawValue!!)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
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

fun getQrTypeActionButtons(context: Context, qr: String): @Composable () -> Unit = {
    val isUrl = qr.startsWith("http://") || qr.startsWith("https://")
    val isEmail = qr.startsWith("mailto:") || qr.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    val isPhone = qr.startsWith("tel:") || qr.matches(Regex("^\\+?[0-9]{10,}")).also { }
    val isWifi = qr.startsWith("WIFI:")
    val isVCard = qr.startsWith("BEGIN:VCARD")
    Row {
        if (isUrl) {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qr))
                context.startActivity(intent)
            }) { Text("Siteyi Aç") }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (isEmail) {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(if (qr.startsWith("mailto:")) qr else "mailto:$qr"))
                context.startActivity(intent)
            }) { Text("E-posta Gönder") }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (isPhone) {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse(if (qr.startsWith("tel:")) qr else "tel:$qr"))
                context.startActivity(intent)
            }) { Text("Ara") }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (isWifi) {
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }) { Text("Wi-Fi Bağlan") }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (isVCard) {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_INSERT)
                intent.type = "vnd.android.cursor.dir/contact"
                intent.putExtra("vCard", qr)
                context.startActivity(intent)
            }) { Text("Rehbere Ekle") }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

object CameraFlashStateHolder {
    var hasFlash by mutableStateOf(false)
} 