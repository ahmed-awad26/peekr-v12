package com.peekr.ui.settings.accounts

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.peekr.data.remote.whatsapp.WhatsappState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsappQrScreen(
    navController: NavController,
    viewModel: WhatsappViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state == WhatsappState.Connected) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ربط واتساب") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // أيقونة
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = androidx.compose.ui.graphics.Color(0xFF25D366).copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFF25D366),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            when (state) {
                WhatsappState.Idle -> {
                    Text("ربط واتساب", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "بيشتغل زي واتساب ويب بالظبط",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.connect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF25D366)
                        )
                    ) {
                        Text("توليد QR Code")
                    }
                }

                WhatsappState.Connecting -> {
                    Text("جاري الاتصال...", style = MaterialTheme.typography.titleMedium)
                    CircularProgressIndicator()
                }

                is WhatsappState.QrReady -> {
                    Text("امسح الـ QR Code", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "افتح واتساب ← النقاط الثلاث ← الأجهزة المرتبطة ← ربط جهاز",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // عرض QR Code
                    val qrData = (state as WhatsappState.QrReady).qrBase64
                    val qrBitmap = remember(qrData) { generateQrBitmap(qrData) }

                    qrBitmap?.let {
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(230.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.connect() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تجديد QR")
                    }
                }

                WhatsappState.Connected -> {
                    CircularProgressIndicator()
                    Text("تم الاتصال! جاري التوجيه...")
                }

                is WhatsappState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = (state as WhatsappState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "تأكد إن الـ WhatsApp Bridge شغال على الجهاز",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.connect() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إعادة المحاولة")
                    }
                }

                WhatsappState.Disconnected -> {
                    Text("انقطع الاتصال", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { viewModel.connect() }, modifier = Modifier.fillMaxWidth()) {
                        Text("إعادة الاتصال")
                    }
                }
            }

            // خطوات الاستخدام
            if (state is WhatsappState.QrReady || state == WhatsappState.Idle) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("خطوات الربط:", style = MaterialTheme.typography.labelLarge)
                        Text("1. افتح واتساب على موبايلك", style = MaterialTheme.typography.bodySmall)
                        Text("2. اضغط النقاط الثلاث (⋮) في الأعلى", style = MaterialTheme.typography.bodySmall)
                        Text("3. اختار الأجهزة المرتبطة", style = MaterialTheme.typography.bodySmall)
                        Text("4. اضغط ربط جهاز وامسح الـ QR", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
