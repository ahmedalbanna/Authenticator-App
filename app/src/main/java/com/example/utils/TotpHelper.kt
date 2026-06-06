package com.example.utils

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpHelper {

    fun decodeBase32(base32: String): ByteArray {
        val uppercase = base32.uppercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("=", "")
            .trim()
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val bits = StringBuilder()
        for (char in uppercase) {
            val index = allowedChars.indexOf(char)
            if (index >= 0) {
                val binaryString = Integer.toBinaryString(index)
                // Pad left with zeros to ensure 5-bit width
                val padded = "00000".substring(binaryString.length) + binaryString
                bits.append(padded)
            }
        }
        
        val bytesCount = bits.length / 8
        val bytes = ByteArray(bytesCount)
        for (i in 0 until bytesCount) {
            val byteStr = bits.substring(i * 8, (i + 1) * 8)
            bytes[i] = byteStr.toInt(2).toByte()
        }
        return bytes
    }

    fun generateTotp(secretBase32: String, timeMs: Long, periodSecs: Int = 30, digits: Int = 6): String {
        return try {
            val decodedSecret = decodeBase32(secretBase32)
            if (decodedSecret.isEmpty()) return "000000"

            val timeStep = timeMs / 1000 / periodSecs
            val timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array()

            val mac = Mac.getInstance("HmacSHA1")
            val keySpec = SecretKeySpec(decodedSecret, "RAW")
            mac.init(keySpec)
            val hash = mac.doFinal(timeBytes)

            val offset = hash[hash.size - 1].toInt() and 0xf
            val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)

            val otp = binary % 10.0.pow(digits.toDouble()).toInt()
            val format = "%0${digits}d"
            String.format(format, otp)
        } catch (e: Exception) {
            e.printStackTrace()
            "000000"
        }
    }
    
    fun parseOtpAuthUri(uriString: String): OtpAuthData? {
        val uri = try {
            android.net.Uri.parse(uriString)
        } catch (e: Exception) {
            return null
        }
        if (uri.scheme != "otpauth" || uri.host != "totp") return null

        val path = uri.path?.trimStart('/') ?: return null
        
        var issuer = uri.getQueryParameter("issuer") ?: ""
        var label = path

        if (path.contains(":")) {
            val parts = path.split(":", limit = 2)
            val possibleIssuer = parts[0].trim()
            val possibleLabel = parts[1].trim()
            if (issuer.isEmpty()) {
                issuer = possibleIssuer
            }
            label = possibleLabel
        }

        val secret = uri.getQueryParameter("secret") ?: return null
        val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
        val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
        val algorithm = uri.getQueryParameter("algorithm") ?: "SHA1"

        return OtpAuthData(
            issuer = issuer.ifEmpty { "Default" },
            label = label,
            secret = secret,
            digits = digits,
            period = period,
            algorithm = algorithm
        )
    }
}

data class OtpAuthData(
    val issuer: String,
    val label: String,
    val secret: String,
    val digits: Int,
    val period: Int,
    val algorithm: String
)
