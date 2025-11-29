package com.example.project_phoenix.ui.app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.project_phoenix.R
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.project_phoenix.data.firebaseRepo
import com.example.project_phoenix.viewm.AuthUiState
import com.example.project_phoenix.viewm.AuthViewModel
import com.example.project_phoenix.viewm.AuthViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    //we use activityViewModels so we can keep the vm without refreshing from other fragments
    private val vm: AuthViewModel by activityViewModels {
        val repo = firebaseRepo(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        AuthViewModelFactory(repo)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userShown = view.findViewById<TextView>(R.id.usernameText)
        val imageView = view.findViewById<ImageView>(R.id.assistantImage)

        Glide.with(this)
            .asGif()
            .load(R.drawable.sprite_avatar)
            .override(1000, 2000)
            .into(imageView)


        //this will get the current user from FirebaseAuth (which is free)
        vm.getFromAuthIfNeeded()

        //this collects our states from view model
        viewLifecycleOwner.lifecycleScope.launch {
            //repeatOnLifecycle allows for good control on
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collectLatest { s ->
                    when (s) {
                        //if user logged in then proceeds
                        is AuthUiState.Authed -> {
                            //gets the users username but if theres no username, gets the name before the email
                            val name = s.user.username?.takeIf { it.isNotBlank() }
                                ?: s.user.email.substringBefore('@')
                            userShown.text = "$name"
                        }
                        is AuthUiState.Loading -> userShown.text = "Loading..."
                        is AuthUiState.Error -> {
                            userShown.text = ""
                            Toast.makeText(requireContext(), s.message, Toast.LENGTH_SHORT).show()
                        }
                        is AuthUiState.Idle, is AuthUiState.Info -> Unit
                    }
                }
            }
        }
    }

}