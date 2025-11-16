package com.example.project_phoenix.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


//this class will hold our user properties
data class User(val uid: String, val email: String, val username: String?)


//this class will restrict the data class success and error to this file only (the repository)
sealed class ResultAuth{
    //this will be used to pass when a user successfully logs in
    data class Success(val user: User) : ResultAuth()

    //this will be used to pass when a user is unsuccessful in logging in, or signing up
    data class Error(val message: String) : ResultAuth()
}


//this will be our database repository
interface AuthRepository{
    //all 3 functions will return a Success or Error
    suspend fun login(email: String, password: String): ResultAuth
    suspend fun signup(email: String, username: String, password: String): ResultAuth
    suspend fun passwordReset(email: String): ResultAuth
}

//this creates an instance of our database repository
class firebaseRepo(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
): AuthRepository {

    //this function will be called everytime a user tries to login
    override suspend fun login(email: String, password: String): ResultAuth =
        //Dispatchers.IO is used when there needs to be a block to be finished before continuing (network calls/databases)
        withContext(Dispatchers.IO) {
            //this will make sure every error is caught so it doesn't crash the app
            runCatching {
                //using auth which is a firebase instance
                auth.signInWithEmailAndPassword(email, password).await()
                // auth.currentUser? is checking if the current user is null, if it is then returns Error
                val tUser = auth.currentUser ?: return@withContext ResultAuth.Error("No user")
                ResultAuth.Success(User(tUser.uid, tUser.email ?: email, null))
            }.getOrElse { t ->
                //this will throw the message error
                ResultAuth.Error(humanMessage(t))
            }
        }

    //this function will be called everytime a user tries to signup
    override suspend fun signup(email: String, username: String, password: String): ResultAuth =
        withContext(Dispatchers.IO) {
            runCatching {
                auth.createUserWithEmailAndPassword(email, password).await()
                val tUser = auth.currentUser ?: return@withContext ResultAuth.Error("No user")
                //creates a map of the profile with the following properties
                val profile = mapOf(
                    "uid" to tUser.uid,
                    "email" to (tUser.email ?: email),
                    "username" to username,
                    "createdAt" to Timestamp.now()
                )
                //adds the users profile to the database
                db.collection("users").document(tUser.uid).set(profile).await()
                //returns a Success
                ResultAuth.Success(User(tUser.uid, tUser.email ?: email, username))
            }.getOrElse { t ->
                //this will throw the message error
                ResultAuth.Error(humanMessage(t))
            }
        }

    //this function will be used for password resets
    override suspend fun passwordReset(email: String): ResultAuth =
        withContext(Dispatchers.IO) {
            runCatching {
                //will send a password reset to email
                auth.sendPasswordResetEmail(email).await()
                //returns success but with no userid because this isn't logging in or out
                ResultAuth.Success(User("", email, null))
            }.getOrElse { t ->
                //this will throw the message error
                ResultAuth.Error(humanMessage(t))
            }
        }

    private fun humanMessage(t: Throwable): String = when (t) {
        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found for this email."
        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password too weak."
        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Email already in use."
        else -> t.localizedMessage ?: "Something went wrong."
    }
}



