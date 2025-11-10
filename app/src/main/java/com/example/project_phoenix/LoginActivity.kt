package com.example.project_phoenix

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.Switch
import androidx.fragment.app.commit
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var container: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_login)

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

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser

                        //this will be used in the future when we setup email verification
                        /*if (user?.isEmailVerified == false) {
                            Toast.makeText(
                                this,
                                "Please verify your email first.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnCompleteListener
                        }*/

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage ?: "Login failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
        findViewById<Button>(R.id.forgotPassButton).setOnClickListener {
            Toast.makeText(this, "THIS FEATURE IS COMING SOON!", Toast.LENGTH_SHORT).show()
        }
    }
}