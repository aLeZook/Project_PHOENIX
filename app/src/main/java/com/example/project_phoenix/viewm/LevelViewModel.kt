package com.example.project_phoenix.viewm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_phoenix.data.LevelRepository
import com.example.project_phoenix.data.LevelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LevelViewModel(
    private val repo: LevelRepository,
    private val uid: String
) : ViewModel() {

    private val _state = MutableStateFlow(LevelState())
    val state: StateFlow<LevelState> = _state

    init {
        viewModelScope.launch {
            repo.getLevelState(uid).collect { levelState ->
                _state.value = levelState
            }
        }
    }
}

class LevelViewModelFactory(
    private val repo: LevelRepository,
    private val uid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LevelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LevelViewModel(repo, uid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}