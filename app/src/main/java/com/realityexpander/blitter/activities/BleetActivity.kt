package com.realityexpander.blitter.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.R
import com.realityexpander.blitter.databinding.ActivityBleetBinding
import com.realityexpander.blitter.databinding.ActivityHomeBinding
import com.realityexpander.blitter.util.User

class BleetActivity : AppCompatActivity() {

    private lateinit var bind: ActivityBleetBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var user: User? = null

    companion object {
        private const val PARAM_USER_ID = "UserId"
        private const val PARAM_USER_NAME = "UserName"

        // navigate to Bleet activity
        fun newIntent(context: Context, userId: String?, userName: String?): Intent {
            val intent = Intent(context, BleetActivity::class.java)
            intent.putExtra(PARAM_USER_ID, userId)
            intent.putExtra(PARAM_USER_NAME, userName)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityBleetBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // user not logged in?
        if(userId == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if user if logged out
        if (firebaseAuth.currentUser?.uid == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        }
    }
}