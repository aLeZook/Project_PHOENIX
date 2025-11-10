package com.example.project_phoenix

import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import com.google.android.material.button.MaterialButton
import android.text.TextWatcher
import androidx.appcompat.content.res.AppCompatResources
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast


class loginForm : Fragment() {

    private var newEmail: TextInputEditText? = null

    private var newUsername: TextInputEditText? = null
    private var etPassword: TextInputEditText? = null

    private var etPassword2: TextInputEditText? = null
    private var createButton: MaterialButton? = null

    private var reqLengthButton: Button? = null
    private var reqUpperCaseButton: Button? = null
    private var reqSpecialButton: Button? = null
    private var reqPasswordMatch: Button? = null

    private var passwordWatcher: TextWatcher? = null




    override fun onDestroyView() {
        // Avoid leaking the TextWatcher
        passwordWatcher?.let { etPassword?.removeTextChangedListener(it) }
        passwordWatcher = null

        etPassword = null
        etPassword2 = null
        createButton = null
        reqLengthButton = null
        reqUpperCaseButton = null
        reqSpecialButton = null
        reqPasswordMatch = null
        newEmail = null
        newUsername = null
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //we will show the login fragment
        return inflater.inflate(R.layout.fragment_login_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        etPassword2 = view.findViewById<TextInputEditText>(R.id.etPassword2)
        createButton = view.findViewById<MaterialButton>(R.id.btnCreateAccount)

        reqLengthButton = view.findViewById<Button>(R.id.reqButton1)
        reqSpecialButton = view.findViewById<Button>(R.id.reqButton2)
        reqUpperCaseButton = view.findViewById<Button>(R.id.reqButton3)
        reqPasswordMatch = view.findViewById<Button>(R.id.reqButton4)

        newEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        newUsername = view.findViewById<TextInputEditText>(R.id.etUsername)

        //Disabling the create account button till you add the requirements
        createButton?.isEnabled = false
        createButton?.alpha = 0.5f

        val closeButton = view.findViewById<Button>(R.id.btnClose)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }



        passwordWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }

            //everytime the password text is changed we check these conditions
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pwd = s?.toString().orEmpty()

                //booleans to check for requirements
                val hasLen = pwd.length >= 8
                val hasUpper = pwd.any{it.isUpperCase()}
                val hasSpecial = SPECIAL_REGEX.containsMatchIn(pwd)


                //we call the function setButtonImage() to change the icons to green check mark
                setButtonImage(reqLengthButton, hasLen)
                setButtonImage(reqUpperCaseButton,hasUpper)
                setButtonImage(reqSpecialButton,hasSpecial)

                val allMet = hasLen && hasUpper && hasSpecial
                createButton?.isEnabled = allMet
                createButton?.alpha = if (allMet) 1f else 0.5f
            }
        }
        etPassword?.addTextChangedListener(passwordWatcher)

        createButton?.setOnClickListener {
            val email = newEmail?.text?.toString()?.trim().orEmpty()
            val username = newUsername?.text?.toString()?.trim().orEmpty()
            val password = etPassword?.text?.toString()?.trim().orEmpty()
            val password2 = etPassword2?.text?.toString()?.trim().orEmpty()

            val match = password == password2
            setButtonImage(reqPasswordMatch,match)

            if (email.isBlank() || username.isBlank() || password.length < 8 || !match) {
                toast("Please complete all fields (password 8+ chars).")
                return@setOnClickListener
            }

            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()


            //this will prevent double-taps
            setBusy(true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser!!
                        //this might be implemented later on when we need to verify emails
                        //user.sendEmailVerification()

                        //creating the profile of a user - not including password
                        val profile = mapOf(
                            "uid" to user.uid,
                            "email" to email,
                            "username" to username,
                            "createdAt" to Timestamp.now()
                        )

                        db.collection("users").document(user.uid)
                            .set(profile)
                            .addOnSuccessListener {
                                toast("Account created!")
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                toast("Profile save failed: ${e.localizedMessage}")
                            }
                            .addOnCompleteListener {
                                setBusy(false)
                            }
                    } else {
                        setBusy(false)
                        val msg = when (val e = task.exception) {
                            is FirebaseAuthWeakPasswordException -> "Password too weak."
                            is FirebaseAuthUserCollisionException -> "Email already in use."
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                            else -> e?.localizedMessage ?: "Sign up failed."
                        }
                        toast(msg)
                    }
                }

        }

    }

    private fun setBusy(busy: Boolean) {
        createButton?.isEnabled = !busy
        createButton?.alpha = if (busy) 0.5f else 1f
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()


    private fun setButtonImage(button: Button?, passed: Boolean){
        if(button == null) return

        val iconImg = if(passed) R.drawable.check_button_image else R.drawable.error_button_image
        val icon = AppCompatResources.getDrawable(requireContext(),iconImg)

        button.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)

    }
    //This is used to find the special characters
    companion object{
        private val SPECIAL_REGEX = Regex("[^A-Za-z0-9]")
    }

}