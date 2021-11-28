package com.realityexpander.blitter.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.realityexpander.blitter.databinding.ActivityBleetBinding
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.DATA_BLEETS_COLLECTION
import com.realityexpander.blitter.util.User
import java.lang.Exception

class BleetActivity : AppCompatActivity() {

    private lateinit var bind: ActivityBleetBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    private var user: User? = null
    private var userName: String? = null
    private var bleetImageUrl: String? = null

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

        // Set progress obscurity view
        bind.bleetProgressLayout.setOnTouchListener { _, _ ->
            true // this will block any tap events
        }

        if (intent.hasExtra(PARAM_USER_ID) && intent.hasExtra(PARAM_USER_NAME)) {
            userId = intent.getStringExtra(PARAM_USER_ID)
            userName = intent.getStringExtra(PARAM_USER_NAME)
        } else {
            Toast.makeText(this,
                "Error creating tweet",
                Toast.LENGTH_SHORT).show()
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

    fun addImage(view: View) {

    }

    fun postBleet(view: View) {

        // show failure message
        fun onPostBleetFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this,
                "Update failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.bleetProgressLayout.visibility = View.GONE
        }

        bind.bleetProgressLayout.visibility = View.VISIBLE
        val bleetText = bind.bleetText.text.toString()
        val hashTags = getHashTags(bleetText)

        // Prepare the firebase document and bleet
        val bleetDocument = firebaseDB.collection(DATA_BLEETS_COLLECTION).document()
        val bleet = Bleet(
            bleetDocument.id,
            userName,
            bleetText,
            bleetImageUrl,
            hashTags,
            userIds = arrayListOf(userId!!),
            likes = arrayListOf(),
            timeStamp = System.currentTimeMillis()
        )

        // Post the Bleet
        bleetDocument.set(bleet)
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener { e ->
                onPostBleetFailure(e)
            }

    }

    // #xxx          -> ["xxx"]
    // #xxx#yyy      -> ["xxx","yyy"]
    // ##            -> []
    // #abc xyz #123 -> ["abc","123"]
    // #abc xyz #abc -> ["abc"]
    private fun getHashTags(source: String): ArrayList<String> {
        val tags = Regex("(#\\w+)", RegexOption.IGNORE_CASE)
            .findAll(source)
            .map { match ->
                match.value
            }
            .toSet()

        return ArrayList(tags)
    }


}






















