package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuthenticatorRepository
import com.example.security.CryptoHelper
import com.example.utils.TotpHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AuthenticatorRepository
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMs = _currentTimeMs.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AuthenticatorRepository(database.authenticatorDao())
        
        viewModelScope.launch {
            while (true) {
                _currentTimeMs.value = System.currentTimeMillis()
                delay(500)
            }
        }
    }

    val accountsUiState: StateFlow<List<AccountUiState>> = combine(
        repository.allAccounts,
        _searchQuery,
        _currentTimeMs
    ) { accounts, query, timeMs ->
        accounts
            .filter {
                it.issuer.contains(query, ignoreCase = true) ||
                it.label.contains(query, ignoreCase = true)
            }
            .map { account ->
                val plainSecret = CryptoHelper.decrypt(account.encryptedSecret)
                val code = TotpHelper.generateTotp(plainSecret, timeMs, account.period, account.digits)
                
                val currentSec = timeMs / 1000
                val elapsed = currentSec % account.period
                val remaining = account.period - elapsed
                val progress = remaining.toFloat() / account.period.toFloat()

                AccountUiState(
                    id = account.id,
                    issuer = account.issuer,
                    label = account.label,
                    totpCode = formatCode(code),
                    remainingSeconds = remaining.toInt(),
                    progress = progress,
                    period = account.period
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun formatCode(code: String): String {
        return if (code.length == 6) {
            "${code.substring(0, 3)} ${code.substring(3)}"
        } else {
            code
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addAccount(label: String, issuer: String, secret: String, period: Int = 30, digits: Int = 6) {
        viewModelScope.launch {
            repository.insertAccount(
                label = label,
                issuer = issuer,
                plainSecret = secret,
                period = period,
                digits = digits
            )
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            repository.deleteAccountById(id)
        }
    }

    // PIN LOCK CONFIGURATION
    private val prefs = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isAppLocked = MutableStateFlow(prefs.getBoolean("pin_lock_enabled", false))
    val isAppLocked = _isAppLocked.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(!_isAppLocked.value)
    val isAppUnlocked = _isAppUnlocked.asStateFlow()

    fun isPinSet(): Boolean {
        return prefs.getString("security_pin", null) != null
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = prefs.getString("security_pin", null)
        val success = savedPin == pin
        if (success) {
            _isAppUnlocked.value = true
        }
        return success
    }

    fun setPin(pin: String?) {
        if (pin == null) {
            prefs.edit()
                .remove("security_pin")
                .putBoolean("pin_lock_enabled", false)
                .apply()
            _isAppLocked.value = false
            _isAppUnlocked.value = true
        } else {
            prefs.edit()
                .putString("security_pin", pin)
                .putBoolean("pin_lock_enabled", true)
                .apply()
            _isAppLocked.value = true
            _isAppUnlocked.value = false
        }
    }
    
    fun lockApp() {
        if (isPinSet()) {
            _isAppUnlocked.value = false
        }
    }
}

data class AccountUiState(
    val id: Int,
    val issuer: String,
    val label: String,
    val totpCode: String,
    val remainingSeconds: Int,
    val progress: Float,
    val period: Int
)
