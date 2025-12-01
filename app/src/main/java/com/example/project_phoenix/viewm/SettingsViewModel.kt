package com.example.project_phoenix.viewm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_phoenix.BuildConfig
import com.example.project_phoenix.data.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.project_phoenix.notifications.NotificationConstants
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import android.util.Log

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val email: String = "",
    val username: String = "",
    val appVersion: String = BuildConfig.VERSION_NAME
)

sealed class SettingsEvent {
    object RequestNotificationPermission : SettingsEvent()
    object ShowNotificationEnabled : SettingsEvent()
}

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
        loadUser()
        setMessagingAutoInit(_state.value.notificationsEnabled)
    }

    private fun loadSettings() {
        _state.update {
            it.copy(
                soundEnabled = repository.isSoundEnabled(),
                notificationsEnabled = repository.isNotificationsEnabled()
            )
        }
    }

    private fun loadUser() {
        val user = auth.currentUser
        val email = user?.email ?: "Unknown"
        val username = user?.displayName
            ?: user?.email?.substringBefore('@')
            ?: "Guest"

        _state.update { it.copy(email = email, username = username) }
    }

    fun onSoundToggled(enabled: Boolean) {
        repository.setSoundEnabled(enabled)
        _state.update { it.copy(soundEnabled = enabled) }
    }

    fun onNotificationsToggleRequested(enabled: Boolean, hasPermission: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (hasPermission) {
                    repository.setNotificationsEnabled(true)
                    _state.update { it.copy(notificationsEnabled = true) }
                    setMessagingAutoInit(true)
                    subscribeToPushTopic()
                    _events.emit(SettingsEvent.ShowNotificationEnabled)
                } else {
                    _state.update { it.copy(notificationsEnabled = false) }
                    _events.emit(SettingsEvent.RequestNotificationPermission)
                }
            } else {
                repository.setNotificationsEnabled(false)
                _state.update { it.copy(notificationsEnabled = false) }
                setMessagingAutoInit(false)
                runCatching {
                    messaging.unsubscribeFromTopic(NotificationConstants.GENERAL_TOPIC).await()
                }.onFailure { throwable ->
                    Log.w(TAG, "Failed to unsubscribe from topic", throwable)
                }
            }
        }
    }

    fun onNotificationPermissionGranted() {
        viewModelScope.launch {
            repository.setNotificationsEnabled(true)
            _state.update { it.copy(notificationsEnabled = true) }
            setMessagingAutoInit(true)
            subscribeToPushTopic()
            _events.emit(SettingsEvent.ShowNotificationEnabled)
        }
    }

    private suspend fun subscribeToPushTopic() {
        runCatching {
            messaging.subscribeToTopic(NotificationConstants.GENERAL_TOPIC).await()
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to subscribe to topic", throwable)
        }
    }

    private fun setMessagingAutoInit(enabled: Boolean) {
        messaging.isAutoInitEnabled = enabled
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, auth, messaging) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}