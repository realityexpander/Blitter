package com.realityexpander.blitter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
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
import com.realityexpander.blitter.listeners.HomeContextI
import com.realityexpander.blitter.util.DATA_USERS_COLLECTION
import com.realityexpander.blitter.util.HOME_ACTIVITY_SELECTED_TAB_POSITION
import com.realityexpander.blitter.util.User
import com.realityexpander.blitter.util.loadUrl


class HomeActivity : AppCompatActivity(), HomeContextI {
    private lateinit var bind: ActivityHomeBinding

    private val firebaseAuth = FirebaseAuth.getInstance()

    // interface HomeContextI vars
    override val firebaseDB = FirebaseFirestore.getInstance()
    override val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    override var currentUser: User? = null

    // ViewPager / TabLayout
    private lateinit var onTabSelectedListener: TabLayout.OnTabSelectedListener
    private lateinit var sectionPageAdapter: SectionPageAdapter
    private lateinit var onPageChangeCallback: ViewPager2.OnPageChangeCallback

    // Hashtag Query
    private lateinit var textChangedListener: TextWatcher
    private lateinit var onEditorActionListener : TextView.OnEditorActionListener

    // Fragments
    private var homeFragment: HomeFragment? = null
    private var searchFragment: SearchFragment? = null
    private var myActivityFragment: MyActivityFragment? = null
    private var currentFragment: BlitterFragment? = null

    // TabsLayoutItems for the fragments
    private enum class TabLayoutItem {
        HOME,
        SEARCH,
        MYACTIVITY
    }

    companion object {
        // navigate to home activity
        fun newIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }

    @SuppressLint("ClickableViewAccessibility") // for progress tap event eater
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(bind.root)

         println("onCreate for HomeActivity")
         println("savedInstanceState = $savedInstanceState")

        // User not logged in?
        if (currentUserId == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        }

        // Setup progress tap-eater
        bind.homeProgressLayout.setOnTouchListener { _, _ ->
            true // this will block any tap events
        }

        // // example to Rename the tabs
        // bind.tabLayout.getTabAt(TabLayoutItem.HOME.ordinal)!!.text = "Home"

        // Nav to profile activity button
        bind.profileImageIv.setOnClickListener { _ ->
            startActivity(ProfileActivity.newIntent(this))
        }

        // Create a new Bleet FAB
        bind.fab.setOnClickListener {
            startActivity(BleetActivity.newIntent(this, currentUserId, currentUser?.username))
        }

        setupViewPagerAdapter()
        setupBottomNavTabLayoutListeners()
    }
    override fun onStart() {
        super.onStart()
//         println("onStart for HomeActivity")
    }
    override fun onResume() {
        super.onResume()
//         println("onResume for HomeActivity")

        setupHashtagSearchQueryListeners()

        // Check if user if logged out
        if (firebaseAuth.currentUser?.uid == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        } else {
            // If we have a user, populate the latest user info
            updateUI()
        }
    }
    override fun onPause() {
        super.onPause()
//         println("onPause for HomeActivity")

        teardownHashtagSearchQueryListeners()
    }
    override fun onStop() {
        super.onStop()
//         println("onStop for HomeActivity")

//        teardownBottomNavTabLayoutListeners()
    }
    override fun onDestroy() {
        super.onDestroy()
        // println("onDestroy for HomeActivity")

        teardownViewPagerAdapter()
        teardownBottomNavTabLayoutListeners()
    }

    private fun updateUI() {
        bind.homeProgressLayout.visibility = View.VISIBLE

        // Show failure message
        fun onPopulateHomeActivityFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(bind.root.context,
                "Searching hashtag failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.homeProgressLayout.visibility = View.GONE
        }

        // Load the current user data from database
        firebaseDB.collection(DATA_USERS_COLLECTION)
            .document(currentUserId!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                currentUser = documentSnapshot.toObject(User::class.java) // load the user data

                // Load the profileImage URL for the User using Glide
                currentUser?.imageUrl.let { profileImageUrl ->
                    bind.profileImageIv.loadUrl(profileImageUrl, R.drawable.default_user)
                }
                updateCurrentUser(currentUser)
                bind.homeProgressLayout.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                onPopulateHomeActivityFailure(e)
                finish()
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //println("onSaveInstanceState for HomeActivity")

        outState.apply {
            putInt(HOME_ACTIVITY_SELECTED_TAB_POSITION, bind.tabLayout.selectedTabPosition)
        }
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // println("onRestoreInstanceState for HomeActivity")

        savedInstanceState.apply {
            val selectedTabPosition = getInt(HOME_ACTIVITY_SELECTED_TAB_POSITION)
            sectionPageAdapter.selectTabLayoutItem(selectedTabPosition)
        }

    }

    // After process death fragment creation, update the fragment vars
    override fun onBlitterFragmentCreated(newBlitterFragment: BlitterFragment) {
        when(newBlitterFragment) {
            is HomeFragment -> homeFragment = newBlitterFragment
            is SearchFragment -> searchFragment = newBlitterFragment
            is MyActivityFragment -> myActivityFragment = newBlitterFragment
        }

        if(currentFragment == null) {
            currentFragment = newBlitterFragment
        }

        // setup the fragment
        when(currentFragment) {
            is HomeFragment -> {}
            is SearchFragment -> setupHashtagSearchQueryListeners()
            is MyActivityFragment -> {}
        }

        // println("onBlitterFragmentCreated currentFragment=$currentFragment")
    }

    private fun updateCurrentUser(updatedUser: User?) {
        currentUser = updatedUser
        onRefreshUIForCurrentFragment()
    }
    override fun onRefreshUIForCurrentFragment() {
        currentFragment?.onUpdateUI()
    }

    override fun onUpdateHashtagSearchQueryTermEv(queryTerm: String) {
        bind.searchHashtagQueryEv.setText(queryTerm)
        bind.searchHashtagQueryEv.selectAll()
    }

    private fun setupViewPagerAdapter() {
//         println("  setupViewPagerAdapter")

        // Setup the Section ViewPager adapter
        sectionPageAdapter = SectionPageAdapter(this)
        bind.viewPager.adapter = sectionPageAdapter
        onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sectionPageAdapter.selectTabLayoutItem(position) // set the selected tab for the swiped-to fragment
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        }
        bind.viewPager.registerOnPageChangeCallback(onPageChangeCallback)
        // Add page transformer: https://developer.android.com/training/animation/screen-slide-2#pagetransformer
        bind.viewPager.setPageTransformer(ZoomOutPageTransformer())
    }
    inner class SectionPageAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = TabLayoutItem.values().size

        override fun createFragment(position: Int): Fragment {
//             println("  SectionPageAdapter createFragment, position=$position")

            val newFragment = when (TabLayoutItem.values()[position]) {
                TabLayoutItem.HOME -> {
                    homeFragment = HomeFragment()
                    homeFragment!!
                }
                TabLayoutItem.SEARCH -> {
                    searchFragment = SearchFragment()
                    teardownHashtagSearchQueryListeners()
                    setupHashtagSearchQueryListeners()
                    searchFragment!!
                }
                TabLayoutItem.MYACTIVITY -> {
                    myActivityFragment = MyActivityFragment()
                    myActivityFragment!!
                }
            }
            if(currentFragment == null) currentFragment = newFragment
//             println("    currentFragment=$currentFragment")

            return newFragment
        }

        // Utility to Select the tab at "position" in tabLayout
        fun selectTabLayoutItem(position: Int) {
            bind.tabLayout.selectTab(bind.tabLayout.getTabAt(position), true)
        }
    }
    private fun teardownViewPagerAdapter() {
        // println("  teardownViewPagerAdapter")

        bind.viewPager.adapter = null
        bind.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    private fun setupBottomNavTabLayoutListeners() {
//         println("  setupBottomNavTabLayoutListeners")

        // Nav to new page when bottom tab item is selected
        onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                println("  setupBottomNavTabLayout onTabSelected, position=${tab?.position}")

                tab?.position?.let { position ->
                    bind.viewPager.setCurrentItem(position, true)

                    when (TabLayoutItem.values()[position]) {
                        TabLayoutItem.HOME -> {
                            currentFragment = homeFragment
                            bind.searchBar.visibility = View.INVISIBLE
                            bind.titleBar.text = "Home"
                        }
                        TabLayoutItem.SEARCH -> {
                            currentFragment = searchFragment
                            bind.searchBar.visibility = View.VISIBLE
                        }
                        TabLayoutItem.MYACTIVITY -> {
                            currentFragment = myActivityFragment
                            bind.searchBar.visibility = View.INVISIBLE
                            bind.titleBar.text = "My Activity"
                        }
                    }

                    currentFragment?.onResume() // force refresh when tab is changed
                    println("    setupBottomNavTabLayout, currentFragment=$currentFragment")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        }
        bind.tabLayout.addOnTabSelectedListener(onTabSelectedListener)
    }
    private fun teardownBottomNavTabLayoutListeners() {
//         println("  teardownBottomNavTabLayoutListeners")
        bind.tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
    }

    // Setup "Search hashtag..." editText View
    private fun setupHashtagSearchQueryListeners() {
//         println("  setupHashtagQueryListeners searchFragment=$searchFragment")
//         println("                             currentFragment=$currentFragment")

        // Setup "Enter" & "Search" IME actions
        onEditorActionListener = TextView.OnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                -> {
                    // println("onEditorActionListener SearchFragment=$searchFragment")
                    searchFragment?.onHashtagQueryActionSearch(v?.text.toString())
                    true
                }
                else -> {
                    false
                }
            }
        }
        bind.searchHashtagQueryEv.setOnEditorActionListener(onEditorActionListener)

        // Setup single key-press to update the "Currently following" Star Icon
        textChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                val term = editable.toString()
                // println("  textChangedListener SearchFragment=$searchFragment")
                searchFragment?.onHashtagQueryKeyPress(term)
            }
        }
        bind.searchHashtagQueryEv.addTextChangedListener(textChangedListener)
    }
    private fun teardownHashtagSearchQueryListeners() {
//         println("  teardownHashtagQueryListeners, initialized=${::textChangedListener.isInitialized}")
        if(::textChangedListener.isInitialized)
            bind.searchHashtagQueryEv.removeTextChangedListener(textChangedListener)
        bind.searchHashtagQueryEv.setOnEditorActionListener(null)
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
                    rotation = position * 180.0f

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