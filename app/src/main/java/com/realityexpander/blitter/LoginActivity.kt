package com.realityexpander.blitter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.realityexpander.blitter.databinding.ActivityLoginBinding

// Firebase dashboard
// https://console.firebase.google.com/u/3/project/blitter-7f038/storage/blitter-7f038.appspot.com/rules

class LoginActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var bind: ActivityLoginBinding

    companion object {
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

    @SuppressLint("ClickableViewAccessibility") // suppress the "no perform click" on the loginProgressLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)

        setTextChangeListener(bind.emailET, bind.emailTIL)
        setTextChangeListener(bind.passwordET, bind.passwordTIL)

        // After pressing Login button, progress obscures the login screen, this will eat the tap events:
        bind.loginProgressLayout.setOnTouchListener { v, event ->
            true // this will block any tap events
        }
    }

    private fun setTextChangeListener(et: EditText, til: TextInputLayout) {
        et.addTextChangedListener( object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                til.isErrorEnabled = false
            }
            override fun afterTextChanged(s: Editable?) {
                // do nothing
            }
        })
    }

    fun onLogin(v: View) {
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
            firebaseAuth.signInWithEmailAndPassword(bind.emailET.text.toString(), bind.passwordET.text.toString())
                .addOnCompleteListener { task ->
                    if(!task.isSuccessful) {
                        bind.loginProgressLayout.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, "Login error: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    bind.loginProgressLayout.visibility = View.GONE
                }
        }
    }

    fun goToSignUp(v: View) {

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