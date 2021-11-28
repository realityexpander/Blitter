package com.realityexpander.blitter.activities

import android.annotation.SuppressLint
import android.app.Activity
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
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var bind: ActivityProfileBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var imageUrl: String? = null

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
        var resultPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .into(bind.photoIV)
            }
        }
        bind.photoIV.setOnClickListener {
            resultPhotoLauncher.launch("image/*")
        }

//        // Setup photo picker (deprecated way)
//        bind.photoIV.setOnClickListener {
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
                bind.profileProgressLayout.visibility = View.GONE
                imageUrl.let {
                    bind.photoIV.loadUrl(user?.imageUrl, R.drawable.logo)
                }
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

        // create the hashmap for firebaseDB
        val map = HashMap<String, Any>()
        map[DATA_USERS_USERNAME] = username
        map[DATA_USERS_EMAIL] = email
        map[DATA_USERS_UPDATED_TIMESTAMP] = System.currentTimeMillis()

        // Update the Firebase Authentication email
        firebaseAuth.currentUser!!.updateEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Update Firebase database user info
                    firebaseDB.collection(DATA_USERS_COLLECTION).document(userId!!).update(map)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show()
                            bind.profileProgressLayout.visibility = View.GONE
                            finish()
                        }
                        .addOnFailureListener { e->
                            e.printStackTrace()
                            Toast.makeText(this, "Update failed, please try again. ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            bind.profileProgressLayout.visibility = View.GONE
                        }
                }
            }
            .addOnFailureListener { e->
                e.printStackTrace()
                Toast.makeText(this, "Update failed, please try again. ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                bind.profileProgressLayout.visibility = View.GONE
            }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
//            storeImage(data?.data)
//        }
//    }

//    private fun storeImage(imageUri: Uri?) {
//        imageUri?.let {
//            Toast.makeText(this, "Uploading...", Toast.LENGTH_LONG).show()
//            bind.profileProgressLayout.visibility = View.VISIBLE
//            val filePath = firebaseStorage.child()
//        }
//    }

    fun onLogout(v: View) {
        firebaseAuth.signOut()
        startActivity(LoginActivity.newIntent(this))
        finish()
    }
}