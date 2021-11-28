package com.realityexpander.blitter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.realityexpander.blitter.R
import com.realityexpander.blitter.databinding.ActivityProfileBinding
import com.realityexpander.blitter.util.*
import java.lang.Exception

class ProfileActivity : AppCompatActivity() {

    private lateinit var bind: ActivityProfileBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        // navigate to this activity
        fun newIntent(context: Context) = Intent(context, ProfileActivity::class.java)
    }

    @SuppressLint("ClickableViewAccessibility") // for the no-op profileProgressLayout.setOnTouchListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // user not logged in?
        if(userId == null) {
            finish()
        }

        bind.profileProgressLayout.setOnTouchListener{ v, event -> true}

        // Setup photo picker (new way)
        val resultPhotoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .into(bind.profileImageIv)

                storeProfileImage(uri)
            }
        }
        bind.profileImageIv.setOnClickListener {
            resultPhotoLauncher.launch(arrayOf("image/*")) // OpenDocument
        }

//        // Setup photo picker (deprecated way)
//        bind.profileImageIv.setOnClickListener {
//            val intent = Intent(Intent.ACTION_PICK) // open file/image picker
//            intent.type = "image/*"
//            startActivityForResult(intent, REQUEST_CODE_PHOTO)
//        }

        populateInfo()
    }

    private fun populateInfo() {
        bind.profileProgressLayout.visibility = View.VISIBLE

        firebaseDB.collection(DATA_USERS_COLLECTION).document(userId!!).get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)

                bind.usernameET.setText(user?.username)
                bind.emailET.setText(user?.email)
                user?.imageUrl.let {
                    bind.profileImageIv.loadUrl(user?.imageUrl, R.drawable.default_user)
                }
                bind.profileProgressLayout.visibility = View.GONE
            }
            .addOnFailureListener { e->
                e.printStackTrace()
                finish()
            }
    }

    fun onApply(v: View) {
        val username = bind.usernameET.text.toString()
        val email = bind.emailET.text.toString()
        bind.profileProgressLayout.visibility = View.VISIBLE

        // create the hashmap for firebaseDB update
        val map = HashMap<String, Any>()
        map[DATA_USERS_USERNAME] = username
        map[DATA_USERS_EMAIL] = email
        map[DATA_USERS_UPDATED_TIMESTAMP] = System.currentTimeMillis()

        // show failure message
        fun onApplyFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Update failed, please try again. ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            bind.profileProgressLayout.visibility = View.GONE
        }

        // Update the Firebase Authentication email
        firebaseAuth.currentUser!!
            .updateEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Update Firebase database user info
                    firebaseDB.collection(DATA_USERS_COLLECTION)
                        .document(userId!!)
                        .update(map)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show()
                            bind.profileProgressLayout.visibility = View.GONE
                            finish()
                        }
                        .addOnFailureListener { e->
                            onApplyFailure(e)
                        }
                }
            }
            .addOnFailureListener { e->
                onApplyFailure(e)
            }
    }

//    // Retrieve the image uri from the gallery photo picker (deprecated way)
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
//            storeProfileImage(data?.data)
//        }
//    }

    // Save the profile image to the firebase Storage
    private fun storeProfileImage(profileImageUri: Uri?) {

        // show failure message
        fun onUploadFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Profile Image upload failed, please try again later. ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            bind.profileProgressLayout.visibility = View.GONE
        }

        profileImageUri?.let {
            Toast.makeText(this, "Uploading...", Toast.LENGTH_LONG).show()
            bind.profileProgressLayout.visibility = View.VISIBLE

            // Upload the new profile image to firebase Storage
            val profileImageStorageRef = firebaseStorage.child(DATA_PROFILE_IMAGES_STORAGE).child(userId!!)
            profileImageStorageRef.putFile(profileImageUri)
                .addOnSuccessListener {

                    // Download the new profile image from firebase Storage
                    profileImageStorageRef.downloadUrl
                        .addOnSuccessListener { profileImageUri->

                            // Update the users' profile in the firebase user database with the new profileImageUrl
                            val profileImageUrl = profileImageUri.toString()

                            // create the hashmap for firebaseDB update
                            val map = HashMap<String, Any>()
                            map[DATA_USERS_IMAGE_URL] = profileImageUrl
                            map[DATA_USERS_UPDATED_TIMESTAMP] = System.currentTimeMillis()

                            firebaseDB.collection(DATA_USERS_COLLECTION)
                                .document(userId)
                                .update(map)
                                .addOnSuccessListener {
                                    // bind.profileImageIv.loadUrl(profileImageUrl, R.drawable.default_user) // we optimistically loaded the profile image after it was picked

                                    Toast.makeText(this, "Profile image update successful", Toast.LENGTH_SHORT).show()
                                    bind.profileProgressLayout.visibility = View.GONE
                                }
                                .addOnFailureListener { e->
                                    onUploadFailure(e)
                                }
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

    fun onLogout(v: View) {
        firebaseAuth.signOut()
        startActivity(LoginActivity.newIntent(this))
        finish()
    }
}