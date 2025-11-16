package com.example.project_phoenix.viewm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_phoenix.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authed(val user: User) : AuthUiState() //on success login
    data class Error(val message: String) : AuthUiState() //on failed signup or login
    data class Info(val message: String) : AuthUiState() //on password reset sent
}

//this is our view model which holds state and will orchestrate calls
class AuthViewModel(
    //this makes it depend on our Repository
    private val repo: AuthRepository
) : ViewModel() {

    //this will expose StateFlow to the UI but we keep Mutable internal so the View can observe only and not change
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    //state will be the read-only for UI
    val state: StateFlow<AuthUiState> = _state

    //this will be called when a new View Model instance is needed
    fun getFromAuthIfNeeded(){
        if (_state.value is AuthUiState.Authed) return

        //we update the state value to send back the username of the authed user
        repo.currentUserCached()?.let { u ->
            _state.value = AuthUiState.Authed(u)
        }
    }

    //this will be the function the UI calls when user presses log in
    fun login(email: String, password: String) {
        //this will set the state to loading which can show spinner/disable buttons in the UI
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                //we call the repository and it will return a result in ResultAuth
                when (val res = repo.login(email, password)) {
                    //this will publish the Successful user to the UI
                    is ResultAuth.Success -> _state.value = AuthUiState.Authed(res.user)
                    //this will publish the error message to the UI
                    is ResultAuth.Error -> _state.value = AuthUiState.Error(res.message)
                }
            } catch(t:Throwable){
                _state.value = AuthUiState.Error(t.localizedMessage ?: "Unexpected error")
            }
        }
    }

    //this will be the function the UI calls when a user completes the sign up form
    fun signup(email: String, username: String, password: String) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val res = repo.signup(email, username, password)) {
                is ResultAuth.Success -> _state.value = AuthUiState.Authed(res.user)
                is ResultAuth.Error   -> _state.value = AuthUiState.Error(res.message)
            }
        }
    }

    //this will be the function the UI calls when a user clicks forgot password
    fun sendReset(email: String) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val res = repo.passwordReset(email)) {
                //for this special case the user is not logging in so we publish an info message instead
                is ResultAuth.Success -> _state.value = AuthUiState.Info("Password reset email sent.")
                is ResultAuth.Error   -> _state.value = AuthUiState.Error(res.message)
            }
        }
    }

    fun clearInfoOrError() { _state.value = AuthUiState.Idle }
}