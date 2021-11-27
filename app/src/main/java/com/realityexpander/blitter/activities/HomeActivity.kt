package com.realityexpander.blitter.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.realityexpander.blitter.databinding.ActivityHomeBinding


class HomeActivity : AppCompatActivity() {

    private lateinit var bind: ActivityHomeBinding
    private val firebaseAuth = FirebaseAuth.getInstance()

    enum class TAB(val index: Int){
        HOME(0),
        SEARCH(1),
        MYACTIVITY(2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val tabView: TabLayout = bind.tabLayout
        tabView.getTabAt(TAB.HOME.index)!!.text = "Home"
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }

    fun onLogout(v: View) {
        firebaseAuth.signOut()
        startActivity(LoginActivity.newIntent(this))
        finish()
    }
}