package com.example.project_phoenix.ui.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.ImageViewTarget
import com.example.project_phoenix.BuildConfig
import com.example.project_phoenix.R
import com.example.project_phoenix.data.LevelRepository
import com.example.project_phoenix.data.POINTS_PER_LEVEL
import com.example.project_phoenix.data.firebaseRepo
import com.example.project_phoenix.viewm.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val vm: AuthViewModel by activityViewModels {
        val repo = firebaseRepo(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        AuthViewModelFactory(repo)
    }

    private val affirmationVm: AffirmationViewModel by viewModels {
        AffirmationViewModelFactory(BuildConfig.GEMINI_API_KEY, requireContext().applicationContext)
    }

    private val levelRepository by lazy { LevelRepository(FirebaseFirestore.getInstance()) }
    private var levelViewModel: LevelViewModel? = null

    private var lastLevel: Int? = null
    private lateinit var imageView: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val levelCircle = view.findViewById<TextView>(R.id.levelCircle)
        val pointsText = view.findViewById<TextView>(R.id.pointsText)
        val progressText = view.findViewById<TextView>(R.id.progressText)
        val progressBar = view.findViewById<ProgressBar>(R.id.xpProgressBar)
        val userShown = view.findViewById<TextView>(R.id.usernameText)
        imageView = view.findViewById(R.id.assistantImage)
        val messageBubble = view.findViewById<TextView>(R.id.messageBubble)

        playIdleGif()

        vm.getFromAuthIfNeeded()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collectLatest { s ->
                    when (s) {
                        is AuthUiState.Authed -> {
                            val name = s.user.username?.takeIf { it.isNotBlank() }
                                ?: s.user.email.substringBefore('@')
                            userShown.text = name
                        }
                        is AuthUiState.Loading -> userShown.text = "Loading..."
                        is AuthUiState.Error -> {
                            userShown.text = ""
                            Toast.makeText(requireContext(), s.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
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

                        // Detect level up
                        val currentLevel = state.level
                        if (lastLevel != null && currentLevel > lastLevel!!) {
                            playLevelUpGif()
                        }
                        lastLevel = currentLevel

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
                    messageBubble.text = when (state) {
                        AffirmationUiState.Loading, AffirmationUiState.Idle -> getString(R.string.loading_affirmation)
                        is AffirmationUiState.Success -> state.affirmation
                        is AffirmationUiState.Error -> state.message
                    }
                }
            }
        }
    }

    private fun playIdleGif() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.sprite_idle)
            .into(imageView)
    }

    private fun playIdleGif2() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.sprite_idlel_2)
            .into(imageView)
    }

    private fun playLevelUpGif() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.sprite_levelup)
            .into(object : ImageViewTarget<GifDrawable>(imageView) {
                override fun setResource(resource: GifDrawable?) {
                    if (resource == null) return
                    imageView.setImageDrawable(resource)
                    resource.setLoopCount(1)
                    resource.start()

                    val levelUpDurationMs = 3100L

                    Handler(Looper.getMainLooper()).postDelayed({
                        playIdleGif2()
                    }, levelUpDurationMs)
                }
            })
    }

}
