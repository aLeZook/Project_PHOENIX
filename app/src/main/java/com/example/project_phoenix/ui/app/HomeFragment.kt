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
import com.example.project_phoenix.BuildConfig
import com.example.project_phoenix.viewm.AffirmationUiState
import com.example.project_phoenix.viewm.AffirmationViewModel
import com.example.project_phoenix.viewm.AffirmationViewModelFactory
import androidx.fragment.app.viewModels
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import com.example.project_phoenix.data.LevelRepository
import com.example.project_phoenix.data.POINTS_PER_LEVEL
import com.example.project_phoenix.viewm.LevelViewModel
import com.example.project_phoenix.viewm.LevelViewModelFactory

class HomeFragment : Fragment(R.layout.fragment_home) {

    //we use activityViewModels so we can keep the vm without refreshing from other fragments
    private val vm: AuthViewModel by activityViewModels {
        val repo = firebaseRepo(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        AuthViewModelFactory(repo)
    }
    private val affirmationVm: AffirmationViewModel by viewModels {
        AffirmationViewModelFactory(BuildConfig.GEMINI_API_KEY, requireContext().applicationContext)
    }
    private val levelRepository by lazy { LevelRepository(FirebaseFirestore.getInstance()) }
    private var levelViewModel: LevelViewModel? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val levelCircle = view.findViewById<TextView>(R.id.levelCircle)
        val pointsText = view.findViewById<TextView>(R.id.pointsText)
        val progressText = view.findViewById<TextView>(R.id.progressText)
        val progressBar = view.findViewById<ProgressBar>(R.id.xpProgressBar)
        val userShown = view.findViewById<TextView>(R.id.usernameText)
        val imageView = view.findViewById<ImageView>(R.id.assistantImage)
        val messageBubble = view.findViewById<TextView>(R.id.messageBubble)

        Glide.with(this)
            .asGif()
            .load(R.drawable.sprite_idle)
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            levelCircle.text = getString(R.string.level_unknown_abbrev)
            pointsText.text = getString(R.string.points_unknown)
            progressText.text = getString(R.string.login_to_view_stats)
            progressBar.progress = 0
        } else {
            levelViewModel = ViewModelProvider(
                this,
                LevelViewModelFactory(levelRepository, uid)
            )[LevelViewModel::class.java]

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    levelViewModel?.state?.collectLatest { state ->
                        val pointsIntoLevel = state.points % POINTS_PER_LEVEL
                        val pointsRemaining = (POINTS_PER_LEVEL - pointsIntoLevel).coerceAtLeast(0)

                        levelCircle.text = getString(R.string.level_abbrev, state.level)
                        pointsText.text = getString(R.string.points_display, state.points)
                        progressText.text = getString(
                            R.string.points_to_next_level,
                            pointsIntoLevel,
                            POINTS_PER_LEVEL,
                            pointsRemaining
                        )
                        progressBar.max = POINTS_PER_LEVEL
                        progressBar.progress = pointsIntoLevel
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                affirmationVm.state.collectLatest { state ->
                    when (state) {
                        AffirmationUiState.Loading, AffirmationUiState.Idle -> messageBubble.text =
                            getString(R.string.loading_affirmation)

                        is AffirmationUiState.Success -> messageBubble.text = state.affirmation
                        is AffirmationUiState.Error -> messageBubble.text = state.message
                    }
                }
            }
        }
    }

}