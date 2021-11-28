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
import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.R
import com.realityexpander.blitter.databinding.ActivityHomeBinding
import com.realityexpander.blitter.fragments.BlitterFragment
import com.realityexpander.blitter.fragments.HomeFragment
import com.realityexpander.blitter.fragments.MyActivityFragment
import com.realityexpander.blitter.fragments.SearchFragment
import com.realityexpander.blitter.util.DATA_USERS_COLLECTION
import com.realityexpander.blitter.util.User
import com.realityexpander.blitter.util.loadUrl


class HomeActivity : AppCompatActivity() {

    private lateinit var bind: ActivityHomeBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var user: User? = null

    private lateinit var onTabSelectedListener: TabLayout.OnTabSelectedListener
    private lateinit var sectionPageAdapter : SectionPageAdapter

    private enum class TabLayoutItem {
        HOME,
        SEARCH,
        MYACTIVITY
    }

    companion object {
        // navigate to home activity
        fun newIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // user not logged in?
        if(userId == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        }

        // Set progress obscurity view
        bind.homeProgressLayout.setOnTouchListener { _, _ ->
            true // this will block any tap events
        }

        // Setup the Section ViewPager adapter
        sectionPageAdapter = SectionPageAdapter(this)
        bind.viewPager.adapter = sectionPageAdapter
        bind.viewPager.registerOnPageChangeCallback( object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sectionPageAdapter.selectTabItem(position) // set the selected tab for the swiped-to fragment
            }
        })
        // Add page transformer: https://developer.android.com/training/animation/screen-slide-2#pagetransformer
        bind.viewPager.setPageTransformer(ZoomOutPageTransformer())

       // // example to Rename the tabs
       // bind.tabLayout.getTabAt(TabLayoutItem.HOME.ordinal)!!.text = "Home"

        // Nav to profile activity
        bind.profileImageIv.setOnClickListener { _ ->
            startActivity(ProfileActivity.newIntent(this))
        }

        // Create a new Bleet
        bind.fab.setOnClickListener {
            startActivity(BleetActivity.newIntent(this, userId, user?.username))
        }
    }

    private fun populateInfo() {
        bind.homeProgressLayout.visibility = View.VISIBLE

        firebaseDB.collection(DATA_USERS_COLLECTION).document(userId!!).get()
            .addOnSuccessListener { documentSnapshot ->
                user = documentSnapshot.toObject(User::class.java) // load the user data

                // Load the profileImage for the user from the firebase Storage
                user?.imageUrl.let { profileImageUrl ->
                    bind.profileImageIv.loadUrl(profileImageUrl, R.drawable.default_user)
                }
                bind.homeProgressLayout.visibility = View.GONE
            }
            .addOnFailureListener { e->
                e.printStackTrace()
                finish()
            }
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
        fun selectTabItem(position: Int) {
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

        populateInfo()

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

private const val MIN_SCALE = 0.85f
private const val MIN_ALPHA = 0.5f

class ZoomOutPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        horzMargin - vertMargin / 2
                    } else {
                        horzMargin + vertMargin / 2
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    rotation = position * 360.0f

                    // Fade the page relative to its size.
                    alpha = (MIN_ALPHA +
                            (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}