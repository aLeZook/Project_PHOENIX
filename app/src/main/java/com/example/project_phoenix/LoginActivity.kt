package com.example.project_phoenix

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

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            //trim is used to get rid of tailing spaces and spaces before the word
            val username = findViewById<EditText>(R.id.username).text.toString().trim()
            val password = findViewById<EditText>(R.id.password).text.toString().trim()
            //we check if the fields are empty before proceeding. They both have to have words to proceed
            if(!password.isEmpty() && !username.isEmpty() && password.length >= 8) {
                val user: MutableMap<String, Any> = HashMap()
                user["username"] = username
                user["password"] = password
                val db = FirebaseFirestore.getInstance()
                db.collection("users").get().addOnCompleteListener {
                    val result: StringBuffer = StringBuffer()
                    if (it.isSuccessful) {
                        for (document in it.result!!) {
                            Log.d(
                                "dbfirebase", "retrieve: " +
                                        "${document.data.getValue("username")} " +
                                        "${document.data.getValue("password")}"
                            )
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
            //else we write something to let the user know
            else{
                if(username.isEmpty()) {
                    val editT: EditText = findViewById<EditText>(R.id.username)
                    editT.setBackgroundColor(Color.RED)
                }
                if(password.isEmpty() || password.length < 8) {
                    val editT2: EditText = findViewById<EditText>(R.id.password)
                    editT2.setBackgroundColor(Color.RED)
                }
            }
        }
        findViewById<Button>(R.id.signupbutton).setOnClickListener{
            val username = findViewById<EditText>(R.id.username).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()
            val db = FirebaseFirestore.getInstance()
            val user: MutableMap<String, Any> = HashMap()
            user["username"] = username
            user["password"] = password
            db.collection("users").add(user).addOnSuccessListener {
                Log.d("dbfirebase", "save: ${user}")
            }.addOnFailureListener {
                    Log.d("dbfirebase Failed", "${user}")
                }
        }

    }
}