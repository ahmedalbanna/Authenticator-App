package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authenticator_accounts")
data class AuthenticatorAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val issuer: String,
    val encryptedSecret: String,
    val digits: Int = 6,
    val period: Int = 30,
    val algorithm: String = "SHA1",
    val timestamp: Long = System.currentTimeMillis()
)
