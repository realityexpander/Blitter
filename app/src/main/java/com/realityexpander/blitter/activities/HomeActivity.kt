package com.realityexpander.blitter.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.realityexpander.blitter.databinding.ActivityHomeBinding
import com.realityexpander.blitter.fragments.HomeFragment
import com.realityexpander.blitter.fragments.MyActivityFragment
import com.realityexpander.blitter.fragments.SearchFragment


class HomeActivity : AppCompatActivity() {

    private lateinit var bind: ActivityHomeBinding
    private val firebaseAuth = FirebaseAuth.getInstance()

    private lateinit var onTabSelectedListener: TabLayout.OnTabSelectedListener
    private lateinit var sectionPageAdapter : SectionPageAdapter

    private enum class TabLayoutItem {
        HOME,
        SEARCH,
        MYACTIVITY
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // Setup the Section ViewPager adapter
        sectionPageAdapter = SectionPageAdapter(this)
        bind.viewPager.adapter = sectionPageAdapter
        bind.viewPager.registerOnPageChangeCallback( object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sectionPageAdapter.selectTab(position) // update the tabs for the newly swiped-to fragment
            }
        })

       // // example to Rename the tabs
       // bind.tabLayout.getTabAt(TabLayoutItem.HOME.ordinal)!!.text = "Home"
    }

    fun onLogout(v: View) {
        firebaseAuth.signOut()
        startActivity(LoginActivity.newIntent(this))
        finish()
    }

    inner class SectionPageAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = TabLayoutItem.values().size

        override fun createFragment(position: Int): Fragment {
            return when(TabLayoutItem.values()[position]) {
              TabLayoutItem.HOME -> HomeFragment()
              TabLayoutItem.SEARCH -> SearchFragment()
              TabLayoutItem.MYACTIVITY -> MyActivityFragment()
          }
        }

        // Utility to Select the tab at position in tabLayout
        fun selectTab(position: Int) {
            bind.tabLayout.selectTab(bind.tabLayout.getTabAt(position), true)
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if user if logged out
        if(firebaseAuth.currentUser?.uid == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        }

        // Nav to new page when bottom tab item is selected
        onTabSelectedListener = object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { bind.viewPager.setCurrentItem(it, true) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // do nothing
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // do nothing
            }
        }
        bind.tabLayout.addOnTabSelectedListener(onTabSelectedListener)
    }

    override fun onPause() {
        super.onPause()

        bind.tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
    }
}