package com.realityexpander.blitter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.realityexpander.blitter.databinding.ActivityLoginBinding
import com.realityexpander.blitter.util.ActivityUtil
import java.lang.Exception

// Firebase dashboard
// https://console.firebase.google.com/u/3/project/blitter-7f038/storage/blitter-7f038.appspot.com/rules

class LoginActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var bind: ActivityLoginBinding

    companion object {
        // navigate to this activity
        fun newIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }

    // Check for user already logged in
    private val firebaseAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser?.uid
        user?.let {
            startActivity(HomeActivity.newIntent(this))
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility") // suppress warning for the "no perform click" on the loginProgressLayout event capture
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)

        ActivityUtil.setTextChangeListener(bind.emailET, bind.emailTIL)
        ActivityUtil.setTextChangeListener(bind.passwordET, bind.passwordTIL)

        // After pressing Login button, loginProgressLayout obscures the login screen, this will eat the tap events:
        bind.loginProgressLayout.setOnTouchListener { _, _ ->
            true // this will block any tap events
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onLogin(unused: View) {

        // show failure message
        fun onLoginFailure(e: Exception? = Exception("unknown error")) {
            e?.printStackTrace()
            Toast.makeText(this, "Login failed, please try again. ${e?.localizedMessage}", Toast.LENGTH_LONG).show()
            bind.loginProgressLayout.visibility = View.GONE
        }

        var proceed = true

        if(bind.emailET.text.isNullOrEmpty()) {
            bind.emailTIL.error = "Email is required."
            bind.emailTIL.isErrorEnabled = true
            proceed = false
        }
        if(bind.passwordET.text.isNullOrEmpty()) {
            bind.passwordTIL.error = "Password is required."
            bind.passwordTIL.isErrorEnabled = true
            proceed = false
        }
        if(proceed) {
            bind.loginProgressLayout.visibility = View.VISIBLE
            firebaseAuth.signInWithEmailAndPassword(
                bind.emailET.text.toString(),
                bind.passwordET.text.toString()
            )
                .addOnCompleteListener { task ->
                    if(!task.isSuccessful) {
                        onLoginFailure(task.exception)
                    }
                }
                .addOnFailureListener { e ->
                    onLoginFailure(e)
                }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun goToSignUp(unused: View) {
        startActivity(SignupActivity.newIntent(this))
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