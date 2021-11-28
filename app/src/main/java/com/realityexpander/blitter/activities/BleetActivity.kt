package com.realityexpander.blitter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.realityexpander.blitter.databinding.ActivityBleetBinding
import com.realityexpander.blitter.util.*
import java.lang.Exception

class BleetActivity : AppCompatActivity() {

    private lateinit var bind: ActivityBleetBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    private var user: User? = null
    private var userName: String? = null
    private var bleetImageUrl: String? = null // from the firebaseStorage
    private var bleetImageUri: Uri? = null // from the android system
    private lateinit var resultPhotoLauncher: ActivityResultLauncher<Array<out String>>

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

    @SuppressLint("ClickableViewAccessibility") // for the bleetProgressLayout event eater
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityBleetBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // Set progress obscurity view
        bind.bleetProgressLayout.setOnTouchListener { _, _ ->
            true // this will block any tap events
        }

        // Get Parameters for Bleetin'
        if (intent.hasExtra(PARAM_USER_ID) && intent.hasExtra(PARAM_USER_NAME)) {
            userId = intent.getStringExtra(PARAM_USER_ID)
            userName = intent.getStringExtra(PARAM_USER_NAME)
        } else {
            Toast.makeText(this,
                "Error creating tweet",
                Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup photo picker
        resultPhotoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .into(bind.imageIv)

                bleetImageUri = uri
            }
        }

        bind.imageIv.setOnClickListener { view ->
            addImage(view)
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
        resultPhotoLauncher.launch(arrayOf("image/*")) // Launch Image Picker
    }

    @Suppress("UNUSED_PARAMETER")
    fun postBleet(unused: View) {
        bind.bleetProgressLayout.visibility = View.VISIBLE
        val bleetText = bind.bleetText.text.toString()
        val hashTags = getHashTags(bleetText)

        // Prepare Bleet object
        val bleet = Bleet(
            bleetId = "",  // new bleet
            userName,
            bleetText,
            imageUrl = "",  // no image set yet
            hashTags,
            userIds = arrayListOf(userId!!),
            likes = arrayListOf(),
            timeStamp = System.currentTimeMillis()
        )

        // Post Bleet with or without Image
        if(bleetImageUri == null) {
            // post Bleet without image
            sendBleet(bleet)
        } else {
            // post Bleet with image
            storeBleetImageAndSendBleet(bleet, bleetImageUri)
        }
    }

    private fun sendBleet(bleet: Bleet) {

        // show failure message
        fun onSendBleetFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this,
                "Sending Bleet failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.bleetProgressLayout.visibility = View.GONE
        }

        // Create new firebaseDB document and Bleet id
        val bleetDocument = firebaseDB.collection(DATA_BLEETS_COLLECTION).document()
        val bleetWithId = bleet.copy(bleetId = bleetDocument.id)

        // Send the Bleet to the FirebaseDB
        bleetDocument.set(bleetWithId)
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener { e ->
                onSendBleetFailure(e)
            }
    }

    // Save the bleet image to the firebase Storage
    private fun storeBleetImageAndSendBleet(bleet: Bleet, bleetImageUri: Uri?) {

        // show failure message
        fun onUploadFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Bleet Image upload failed, please try again later. ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            bind.bleetProgressLayout.visibility = View.GONE
        }

        bleetImageUri?.let {
            Toast.makeText(this, "Uploading...", Toast.LENGTH_LONG).show()
            bind.bleetProgressLayout.visibility = View.VISIBLE

            // Upload the new bleet image to firebase Storage
            val bleetImageStorageRef = firebaseStorage.child(DATA_BLEET_IMAGES_STORAGE).child(userId!!)
            bleetImageStorageRef.putFile(bleetImageUri)
                .addOnSuccessListener {

                    // Get the new Bleet image URL from firebase Storage
                    bleetImageStorageRef.downloadUrl
                        .addOnSuccessListener { bleetImageUri->

                            // Update the Bleet with Image URL, and send the Bleet to FirebaseDB Bleet database
                            val bleetImageUrl = bleetImageUri.toString()
                            val bleetToSend = bleet.copy(imageUrl = bleetImageUrl)
                            sendBleet(bleetToSend)
                        }
                        .addOnFailureListener { e->
                            onUploadFailure(e)
                        }
                }
                .addOnFailureListener { e->
                    onUploadFailure(e)
                }
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






















