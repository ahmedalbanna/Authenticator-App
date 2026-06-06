package com.example.ui.components

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.utils.TotpHelper
import com.example.utils.OtpAuthData

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onQrScanned: (OtpAuthData) -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.width(48.dp))
                        
                        Text(
                            text = "مسح الرمز الثنائي (QR Code)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black)
            ) {
                if (cameraPermissionState.status.isGranted) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onQrScanned = { uri ->
                            val parsed = TotpHelper.parseOtpAuthUri(uri)
                            if (parsed != null) {
                                onQrScanned(parsed)
                                Toast.makeText(context, "تم مسح الرمز بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "تنسيق الرمز غير صالح!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    // Custom fallback if permission denied
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "صلاحية الكاميرا مطلوبة لمسح الـ QR",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "يرجى منح الإذن للوصول إلى الكاميرا من إعدادات النظام للتمكن من مسح الرموز تلقائياً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("منح الإذن")
                        }
                    }
                }

                // Aesthetic QR Box Overlay on center
                ScannerOverlay(
                    modifier = Modifier.fillMaxSize(),
                    onSimulateScan = {
                        val simulatedUris = listOf(
                            "otpauth://totp/GitHub:ahmedalbannatech?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&period=30",
                            "otpauth://totp/Google:ahmed@gmail.com?secret=K67A7Z3WEXS56EZE&issuer=Google&period=30",
                            "otpauth://totp/Facebook:johndoe@email.com?secret=MZXW6YTBOI======&issuer=Facebook&period=30",
                            "otpauth://totp/CryptoBank:customer910?secret=NBSWY3DPEB3W64TBNQ======&issuer=CryptoBank&period=30&digits=6",
                            "otpauth://totp/Microsoft:work-system?secret=ORXW233SMV2G64TN&issuer=Microsoft&period=60"
                        )
                        val randomUri = simulatedUris.random()
                        val parsed = TotpHelper.parseOtpAuthUri(randomUri)
                        if (parsed != null) {
                            onQrScanned(parsed)
                            Toast.makeText(context, "تمت محاكاة مسح رمز بنجاح! 🧪", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    onSimulateScan: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animation for the scanning line
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Darkened surrounding mask with transparent middle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val boxSize = 260.dp.toPx()
            val left = (canvasWidth - boxSize) / 2
            val top = (canvasHeight - boxSize) / 2
            val right = left + boxSize
            val bottom = top + boxSize

            // Outer darkened layer
            // Top overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = androidx.compose.ui.geometry.Size(canvasWidth, top)
            )
            // Left overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(0f, top),
                size = androidx.compose.ui.geometry.Size(left, boxSize)
            )
            // Right overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(right, top),
                size = androidx.compose.ui.geometry.Size(canvasWidth - right, boxSize)
            )
            // Bottom overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(0f, bottom),
                size = androidx.compose.ui.geometry.Size(canvasWidth, canvasHeight - bottom)
            )
        }

        // Focused Scanning Box
        Box(
            modifier = Modifier
                .size(260.dp)
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            // Highlighting red or laser grid animation line
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = size.width
                val boxHeight = size.height
                val y = boxHeight * lineProgress

                // Laser horizontal line
                drawLine(
                    brush = SolidColor(Color(0xFF00FFCC)),
                    start = Offset(16.dp.toPx(), y),
                    end = Offset(boxWidth - 16.dp.toPx(), y),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        // Instructions and Simulation buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ضع رمز الـ QR داخل الإطار للمسح",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Test Simulation Trigger (MANDATORY for virtual/cloud testing environments)
            Button(
                onClick = onSimulateScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "محاكاة مسح رمز (بيانات تجريبية)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
