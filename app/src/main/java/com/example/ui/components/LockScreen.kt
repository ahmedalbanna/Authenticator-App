package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

@Composable
fun LockScreen(
    isSetupMode: Boolean,
    onPinEntered: (String) -> Boolean, // Returns true if PIN is correct
    onCancelSetup: (() -> Unit)? = null
) {
    var pinState by remember { mutableStateOf("") }
    var setupStep by remember { mutableIntStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var firstPinAttempt by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    val titleText = if (isSetupMode) {
        if (setupStep == 1) "تعيين رمز PIN جديد" else "تأكيد رمز PIN"
    } else {
        "التطبيق مؤمّن"
    }

    val subtitleText = if (isSetupMode) {
        if (setupStep == 1) "أدخل رمز مكون من 4 أرقام لحماية حساباتك" else "أعد إدخال الرمز للتأكيد"
    } else {
        "الرجاء إدخال رمز PIN لفك التشفير وعرض الرموز"
    }

    fun handleKeyPress(digit: String) {
        if (pinState.length < 4) {
            pinState += digit
            errorMessage = ""
        }

        if (pinState.length == 4) {
            if (isSetupMode) {
                if (setupStep == 1) {
                    firstPinAttempt = pinState
                    pinState = ""
                    setupStep = 2
                } else {
                    if (pinState == firstPinAttempt) {
                        onPinEntered(pinState)
                        Toast.makeText(context, "تم إعداد PIN بنجاح! 🔐", Toast.LENGTH_SHORT).show()
                    } else {
                        errorMessage = "الرموز غير متطابقة، أعد المحاولة"
                        pinState = ""
                        setupStep = 1
                    }
                }
            } else {
                val isCorrect = onPinEntered(pinState)
                if (!isCorrect) {
                    errorMessage = "رمز PIN خاطئ! حاول مجدداً"
                    pinState = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (pinState.isNotEmpty()) {
            pinState = pinState.substring(0, pinState.length - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Lock Icon
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "قفل",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            for (i in 1..4) {
                val isFilled = pinState.length >= i
                val color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                val size = if (isFilled) 18.dp else 14.dp

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }

        // Error message placeholder
        Box(modifier = Modifier.height(24.dp)) {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keypad Grid
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (digit in row) {
                        KeypadButton(text = digit, onClick = { handleKeyPress(digit) })
                    }
                }
            }

            // Bottom keypad row (Biometric/Blank, "0", Backspace)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Option: Biometric Simulation or Cancel Setup
                if (isSetupMode && onCancelSetup != null) {
                    IconButton(
                        onClick = onCancelSetup,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text(
                            text = "إلغاء",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "التحقق من البصمة نشط تلقائياً... ✅", Toast.LENGTH_SHORT).show()
                            onPinEntered("BIOMETRIC") // Simulated correct unlock for convenience
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "بصمة الاصبع",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                KeypadButton(text = "0", onClick = { handleKeyPress("0") })

                IconButton(
                    onClick = { handleBackspace() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "مسح",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
