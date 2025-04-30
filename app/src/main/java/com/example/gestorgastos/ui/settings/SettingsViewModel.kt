package com.example.gestorgastos.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestorgastos.data.database.AppDatabase
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()

    private val _isDarkMode = MutableLiveData<Boolean>()
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    private val _isNotificationsEnabled = MutableLiveData<Boolean>()
    val isNotificationsEnabled: LiveData<Boolean> = _isNotificationsEnabled

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        // TODO: Implementar cambio de tema
    }

    fun setNotifications(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
        // TODO: Implementar notificaciones
    }

    fun exportData() {
        viewModelScope.launch {
            // TODO: Implementar exportación de datos
        }
    }

    fun clearData() {
        viewModelScope.launch {
            // TODO: Implementar limpieza de datos
        }
    }
} 