package com.example.data

import com.example.security.CryptoHelper
import kotlinx.coroutines.flow.Flow

class AuthenticatorRepository(private val dao: AuthenticatorDao) {
    val allAccounts: Flow<List<AuthenticatorAccount>> = dao.getAllAccounts()

    suspend fun insertAccount(
        label: String,
        issuer: String,
        plainSecret: String,
        digits: Int = 6,
        period: Int = 30,
        algorithm: String = "SHA1"
    ): Long {
        val encrypted = CryptoHelper.encrypt(plainSecret.trim())
        val account = AuthenticatorAccount(
            label = label,
            issuer = issuer,
            encryptedSecret = encrypted,
            digits = digits,
            period = period,
            algorithm = algorithm
        )
        return dao.insertAccount(account)
    }

    suspend fun deleteAccountById(id: Int) {
        dao.deleteAccountById(id)
    }
}
