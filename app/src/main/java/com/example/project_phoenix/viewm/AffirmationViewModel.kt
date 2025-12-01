package com.example.project_phoenix.viewm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_phoenix.data.AffirmationRepository
import com.example.project_phoenix.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AffirmationUiState {
    data object Idle : AffirmationUiState
    data object Loading : AffirmationUiState
    data class Success(val affirmation: String) : AffirmationUiState
    data class Error(val message: String) : AffirmationUiState
}

class AffirmationViewModel(
    private val repo: AffirmationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AffirmationUiState>(AffirmationUiState.Idle)
    val state: StateFlow<AffirmationUiState> = _state

    init {
        loadAffirmation()
    }

    fun loadAffirmation() {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            _state.value = AffirmationUiState.Error("Add GEMINI_API_KEY to local.properties to enable affirmations.")
            return
        }
        _state.value = AffirmationUiState.Loading
        viewModelScope.launch {
            repo.fetchDailyAffirmation()
                .onSuccess { affirmation ->
                    _state.value = AffirmationUiState.Success(affirmation)
                }
                .onFailure { throwable ->
                    _state.value = AffirmationUiState.Error(
                        throwable.localizedMessage ?: "Failed to load affirmation"
                    )
                }
        }
    }
}

class AffirmationViewModelFactory(
    private val apiKey: String,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AffirmationViewModel::class.java)) {
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash-lite",
                apiKey = apiKey
            )
            val repo = AffirmationRepository.create(context.applicationContext, model)
            return AffirmationViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}