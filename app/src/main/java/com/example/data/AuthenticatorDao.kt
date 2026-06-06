package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthenticatorDao {
    @Query("SELECT * FROM authenticator_accounts ORDER BY timestamp DESC")
    fun getAllAccounts(): Flow<List<AuthenticatorAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AuthenticatorAccount): Long

    @Delete
    suspend fun deleteAccount(account: AuthenticatorAccount)

    @Query("DELETE FROM authenticator_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Int)
}
