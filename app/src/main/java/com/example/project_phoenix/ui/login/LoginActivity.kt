package com.example.project_phoenix.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.project_phoenix.R
import com.example.project_phoenix.ui.app.MainActivity
import com.example.project_phoenix.viewm.AuthUiState
import com.example.project_phoenix.viewm.AuthViewModel
import com.example.project_phoenix.viewm.AuthViewModelFactory
import com.example.project_phoenix.data.firebaseRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {
    private val vm: AuthViewModel by viewModels {
        val repo = firebaseRepo(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        AuthViewModelFactory(repo)
    }
    private lateinit var container: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_login)
        //this will hold our viewmodel states
        lifecycleScope.launch {
            //automatically cancels the inner collection when the Activity goes to the background, and restarts it when you come back.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                //this is our view model with the latest state
                vm.state.collectLatest { s ->
                    when (s) {
                        is AuthUiState.Idle -> {
                            findViewById<Button>(R.id.loginButton).isEnabled = true
                        }

                        is AuthUiState.Loading -> {
                            //while its loading, we want to disable login button so we can't double tap
                            findViewById<Button>(R.id.loginButton).isEnabled = false
                        }
                        //this state is when a user successfully logged in
                        is AuthUiState.Authed -> {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                        //this is when the user successfully sends an email reset
                        is AuthUiState.Info -> {
                            Toast.makeText(this@LoginActivity, s.message, Toast.LENGTH_SHORT).show()
                            vm.clearInfoOrError()
                        }
                        //anytime a user fails a login or fails a signup
                        is AuthUiState.Error -> {
                            Toast.makeText(this@LoginActivity, s.message, Toast.LENGTH_SHORT).show()
                            findViewById<Button>(R.id.loginButton).isEnabled = true
                            vm.clearInfoOrError()
                        }
                    }
                }
            }
        }


        container = findViewById(R.id.fragmentContainerView)

        //this will show the signup form when the signup button is clicked
        findViewById<Button>(R.id.signupbutton).setOnClickListener {
            val tag = "signup_fragment"
            if (supportFragmentManager.findFragmentByTag(tag) == null) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                    )
                    add(R.id.fragmentContainerView, loginForm(), tag) // or LoginForm() if you rename
                    addToBackStack("signup")
                }
                container.visibility = View.VISIBLE
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                container.visibility = View.GONE
            }
        }

        //this will be username or email(whichever user wants to use)
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val email = findViewById<EditText>(R.id.username).text.toString().trim()
            val password = findViewById<EditText>(R.id.password).text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //this calls our view model and sends the data
            vm.login(email, password)
        }
        findViewById<Button>(R.id.forgotPassButton).setOnClickListener {
            val email = findViewById<EditText>(R.id.username).text.toString().trim()
            //this will give an error message
            if (email.isEmpty())
            {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
            }
            else //if theres an email, we call the reset function
            {
                vm.sendReset(email)
            }
        }
    }
}