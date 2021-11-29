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
import com.google.firebase.firestore.DocumentReference
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
    private var userName: String? = null
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

        // Setup progress tap-eater
        bind.bleetProgressLayout.setOnTouchListener { _, _ ->
            true // this will block any tap events
        }

        // Get passed-in Parameters for Bleetin'
        if (intent.hasExtra(PARAM_USER_ID) && intent.hasExtra(PARAM_USER_NAME)) {
            userId = intent.getStringExtra(PARAM_USER_ID)
            userName = intent.getStringExtra(PARAM_USER_NAME)
        } else {
            Toast.makeText(this,
                "Error creating tweet",
                Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup photo picker (must be setup before onResume/onStart)
        resultPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    Glide.with(this)
                        .load(uri)
                        .into(bind.imageIv)

                    bleetImageUri = uri
                }
            }

        // Click on image to replace the image
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

        // Create new firebaseDB document and id
        val bleetDocument = firebaseDB.collection(DATA_BLEETS_COLLECTION).document()

        // Create Bleet object
        val bleet = Bleet(
            bleetDocument.id,
            userName,
            bleetText,
            imageUrl = "",  // no image set yet
            hashTags,
            rebleetUserIds = arrayListOf(userId!!),
            likesUserIds = arrayListOf(),
            timeStamp = System.currentTimeMillis()
        )

        // Post Bleet with or without Image
        if (bleetImageUri == null) {
            // post Bleet without image
            sendBleet(bleet, bleetDocument)
        } else {
            // post Bleet with image
            storeBleetImageAndSendBleet(bleet, bleetImageUri, bleetDocument)
        }
    }

    // Send Bleet to FirebaseDB Bleet database
    private fun sendBleet(bleet: Bleet, bleetDocument: DocumentReference) {

        // Show failure message
        fun onSendBleetFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this,
                "Sending Bleet failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.bleetProgressLayout.visibility = View.GONE
        }

        // Send the Bleet to the FirebaseDB
        bleetDocument.set(bleet)
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener { e ->
                onSendBleetFailure(e)
            }
    }

    // Save the bleet image to the firebase Storage
    private fun storeBleetImageAndSendBleet(bleet: Bleet, bleetImageUri: Uri?, bleetDocument: DocumentReference) {

        // Show failure message
        fun onUploadFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this,
                "Bleet Image upload failed, please try again later. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.bleetProgressLayout.visibility = View.GONE
        }

        bleetImageUri?.let {
            Toast.makeText(this, "Uploading...", Toast.LENGTH_LONG).show()
            bind.bleetProgressLayout.visibility = View.VISIBLE

            // Upload the Bleet image to firebase Storage
            val bleetImageStorageRef =
                firebaseStorage.child(DATA_BLEET_IMAGES_STORAGE).child(bleet.bleetId!!) // Bleet.bleetId is the name and owner of this image
            bleetImageStorageRef.putFile(bleetImageUri)
                .addOnSuccessListener {

                    // Get the new Bleet image URL from firebase Storage
                    bleetImageStorageRef.downloadUrl
                        .addOnSuccessListener { bleetImageUri ->
                            // Update the Bleet with Image URL, then send the Bleet to FirebaseDB Bleet database
                            val bleetImageUrl = bleetImageUri.toString()
                            val bleetToSend = bleet.copy(imageUrl = bleetImageUrl)

                            sendBleet(bleetToSend, bleetDocument)
                        }
                        .addOnFailureListener { e ->
                            onUploadFailure(e)
                        }
                }
                .addOnFailureListener { e ->
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
        val tags = Regex("(#\\w+)")
            .findAll(source)
            .map { match ->
                match.value.split("#")[1]
            }
            .toSet()

        return ArrayList(tags)
    }


}






















