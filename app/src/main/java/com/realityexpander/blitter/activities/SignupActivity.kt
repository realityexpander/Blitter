package com.realityexpander.blitter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.databinding.ActivitySignupBinding
import com.realityexpander.blitter.util.ActivityUtil
import com.realityexpander.blitter.util.DATA_USERS_COLLECTION
import com.realityexpander.blitter.util.User

class SignupActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private lateinit var bind: ActivitySignupBinding

    companion object {
        // Navigate to the activity
        fun newIntent(context: Context) = Intent(context, SignupActivity::class.java)
    }

    // Check for user already logged in
    private val firebaseAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser?.uid
        user?.let {
            startActivity(HomeActivity.newIntent(this))
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility") // suppress warning for the "no perform click" on the signupProgressLayout event capture
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(bind.root)

        ActivityUtil.setTextChangeListener(bind.usernameET, bind.usernameTIL)
        ActivityUtil.setTextChangeListener(bind.emailET, bind.emailTIL)
        ActivityUtil.setTextChangeListener(bind.passwordET, bind.passwordTIL)

        // After pressing 'sign up' button, progress obscures the signup screen, this will eat any tap events:
        bind.signupProgressLayout.setOnTouchListener { v, event ->
            true // this will block any tap events
        }
    }

    fun onSignup(v: View) {
        var proceed = true

        if (bind.usernameET.text.isNullOrEmpty()) {
            bind.usernameTIL.error = "Username is required."
            bind.usernameTIL.isErrorEnabled = true
            proceed = false
        }
        if (bind.emailET.text.isNullOrEmpty()) {
            bind.emailTIL.error = "Email is required."
            bind.emailTIL.isErrorEnabled = true
            proceed = false
        }
        if (bind.passwordET.text.isNullOrEmpty()) {
            bind.passwordTIL.error = "Password is required."
            bind.passwordTIL.isErrorEnabled = true
            proceed = false
        }
        if (proceed) {
            bind.signupProgressLayout.visibility = View.VISIBLE
            val email = bind.emailET.text.toString()
            val password = bind.passwordET.text.toString()
            val username = bind.usernameET.text.toString()

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(
                            this@SignupActivity,
                            "Signup error: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Save the new user to the firebase DB
                        val user = User(
                            email,
                            username,
                            "",
                            arrayListOf(),
                            arrayListOf(),
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                        )
                        firebaseDB.collection(DATA_USERS_COLLECTION)
                            .document(firebaseAuth.uid!!) // id of the document matches id of the user, leave blank for fb to create id
                            .set(user) // document to save
                    }
                    bind.signupProgressLayout.visibility = View.GONE

                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    bind.signupProgressLayout.visibility = View.GONE
                }
        }
    }

    fun goToLogin(v: View) {
        startActivity(LoginActivity.newIntent(this))
        finish()
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(firebaseAuthListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(firebaseAuthListener)
    }

}