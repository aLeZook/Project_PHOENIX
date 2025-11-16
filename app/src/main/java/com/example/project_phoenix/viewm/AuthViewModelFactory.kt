package com.example.project_phoenix.viewm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.project_phoenix.data.AuthRepository

//implements the standard Android interface for constructing ViewModels
class AuthViewModelFactory(
    //factory receives dependency. This keeps construction outside the ViewModel so you can swap fakes in tests
    private val repo: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    //Android calls this when you do: by viewModels { ... }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        //This is a safety check so ensure we only create AuthViewModel here
        require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        //The actual construction, injecting the repository dependency
        return AuthViewModel(repo) as T
    }
}