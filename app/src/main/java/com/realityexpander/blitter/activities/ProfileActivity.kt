package com.realityexpander.blitter.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.databinding.ActivityProfileBinding
import com.realityexpander.blitter.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var bind: ActivityProfileBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {

        // navigate to this activity
        fun newIntent(context: Context) = Intent(context, ProfileActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // user not logged in?
        if(userId == null) {
            finish()
        }

        bind.profileProgressLayout.setOnTouchListener{ v, event -> true}

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

        // Update the Firebase Authentication
        firebaseAuth.currentUser!!.updateEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Update Firebase database
                    firebaseDB.collection(DATA_USERS_COLLECTION).document(userId!!).update(map)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show()
                            bind.profileProgressLayout.visibility = View.GONE
                            finish()
                        }
                        .addOnFailureListener { e->
                            e.printStackTrace()
                            Toast.makeText(this, "Update failed, please try again.", Toast.LENGTH_LONG).show()
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

    fun onLogout(v: View) {
        firebaseAuth.signOut()
        startActivity(LoginActivity.newIntent(this))
        finish()
    }
}