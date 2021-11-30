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
import com.realityexpander.blitter.listeners.HomeCallback
import com.realityexpander.blitter.util.DATA_USERS_COLLECTION
import com.realityexpander.blitter.util.User
import com.realityexpander.blitter.util.loadUrl


class HomeActivity : AppCompatActivity(), HomeCallback {

    private lateinit var bind: ActivityHomeBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var currentUser: User? = null

    // ViewPager / TabLayout
    private lateinit var onTabSelectedListener: TabLayout.OnTabSelectedListener
    private lateinit var sectionPageAdapter: SectionPageAdapter
    private lateinit var onPageChangeCallback: ViewPager2.OnPageChangeCallback

    // Hashtag Search
    private lateinit var textChangedListener: TextWatcher
    private lateinit var onEditorActionListener : TextView.OnEditorActionListener

    private var homeFragment: HomeFragment? = null
    private var searchFragment: SearchFragment? = null
    private var myActivityFragment: MyActivityFragment? = null
    private var currentFragment: BlitterFragment? = null

    private enum class TabLayoutItem {
        HOME,
        SEARCH,
        MYACTIVITY
    }

    companion object {
        // navigate to home activity
        fun newIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }

    @SuppressLint("ClickableViewAccessibility") // for progress event eater
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(bind.root)

        println("onCreate for HomeActivity searchFragment=$searchFragment")
        println("savedInstanceState = $savedInstanceState")

//        homeFragment = HomeFragment()
//        searchFragment = SearchFragment()
//        myActivityFragment = MyActivityFragment()
//        currentFragment  = homeFragment
//        println("  onCreate homeFragment=$homeFragment")
//        println("  onCreate searchFragment=$searchFragment")
//        println("  onCreate myActivityFragment=$myActivityFragment")
//        println("  onCreate currentFragment=$currentFragment")

        // user not logged in?
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

        // Nav to profile activity
        bind.profileImageIv.setOnClickListener { _ ->
            startActivity(ProfileActivity.newIntent(this))
        }

        // Create a new Bleet
        bind.fab.setOnClickListener {
            startActivity(BleetActivity.newIntent(this, currentUserId, currentUser?.username))
        }

    }

    private fun populate() {
        bind.homeProgressLayout.visibility = View.VISIBLE

        // Load the current user data from firebaseDB
        firebaseDB.collection(DATA_USERS_COLLECTION).document(currentUserId!!).get()
            .addOnSuccessListener { documentSnapshot ->
                currentUser = documentSnapshot.toObject(User::class.java) // load the user data

                // Load the profileImage for the user from the firebase Storage
                currentUser?.imageUrl.let { profileImageUrl ->
                    bind.profileImageIv.loadUrl(profileImageUrl, R.drawable.default_user)
                }
                updateFragmentWithCurrentUser()
                bind.homeProgressLayout.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                finish()
            }
    }

    override fun onStart() {
        super.onStart()
        println("onStart for HomeActivity")

        setupViewPagerAdapter()
        setupBottomNavTabLayout()
    }

    override fun onResume() {
        super.onResume()
        println("onResume for HomeActivity")

        // Check if user if logged out
        if (firebaseAuth.currentUser?.uid == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        } else {
            // If we have a user, populate the latest user info
            populate()
        }
    }

    override fun onPause() {
        super.onPause()
        println("onPause for HomeActivity")

        teardownHashtagSearchEditText()
    }

    override fun onStop() {
        super.onStop()
        println("onStop for HomeActivity")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy for HomeActivity")

        teardownViewPagerAdapter()
        teardownBottomNavTabLayout()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        println("onSaveInstanceState for HomeActivity")

        outState.putInt("selectedTabPosition", bind.tabLayout.selectedTabPosition)
        println("  selectedTabPosition=${outState.getInt("selectedTabPosition")}")

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        println("onRestoreInstanceState for HomeActivity")

        val selectedTabPosition = savedInstanceState.getInt("selectedTabPosition")
        println("  selectedTabPosition=$selectedTabPosition")

        sectionPageAdapter.selectTabItem(selectedTabPosition)

    }

    override fun onUserUpdated() {
        populate()
    }

    override fun onRefresh() {
        currentFragment?.updateList()
    }

    override fun onSearchFragmentCreated(searchFragment: SearchFragment) {
        this.searchFragment = searchFragment

        if(currentFragment == null) {
            currentFragment = searchFragment
            setupHashtagSearchEditText()
        }

        println("onSearchFragmentCreated currentfragment=$currentFragment")
    }

    private fun updateFragmentWithCurrentUser() {
        homeFragment?.setUser(currentUser)
        searchFragment?.setUser(currentUser)
        myActivityFragment?.setUser(currentUser)

        currentFragment?.updateList()
    }

    private fun setupViewPagerAdapter() {
        println("  setupViewPagerAdapter")

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
                sectionPageAdapter.selectTabItem(position) // set the selected tab for the swiped-to fragment
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
            println("  SectionPageAdapter createFragment, position=$position")

            val newFragment = when (TabLayoutItem.values()[position]) {
                TabLayoutItem.HOME -> {
                    homeFragment = HomeFragment()
                    homeFragment!!
                }
                TabLayoutItem.SEARCH -> {
                    searchFragment = SearchFragment()
                    teardownHashtagSearchEditText()
                    setupHashtagSearchEditText()
                    searchFragment!!
                }
                TabLayoutItem.MYACTIVITY -> {
                    myActivityFragment = MyActivityFragment()
                    myActivityFragment!!
                }
            }
            if(currentFragment == null) currentFragment = newFragment
            println("  currentFragment=$currentFragment")
            return newFragment
        }

        // Utility to Select the tab at "position" in tabLayout
        fun selectTabItem(position: Int) {
            bind.tabLayout.selectTab(bind.tabLayout.getTabAt(position), true)
        }
    }
    private fun teardownViewPagerAdapter() {
        println("  teardownViewPagerAdapter")

        bind.viewPager.adapter = null
        bind.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    private fun setupBottomNavTabLayout() {
        println("  setupBottomNavTabLayout")

        // Nav to new page when bottom tab item is selected
        onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                println("  setupBottomNavTabLayout onTabSelected, position=${tab?.position}")

                tab?.position?.let { position ->
                    bind.viewPager.setCurrentItem(position, true)

                    when (TabLayoutItem.values()[position]) {
                        TabLayoutItem.HOME -> {
                            println("  onTabSelected currentFragment='homeFragment'")
                            currentFragment = homeFragment
                            bind.searchBar.visibility = View.INVISIBLE
                            bind.titleBar.text = "Home"
                        }
                        TabLayoutItem.SEARCH -> {
                            println("  onTabSelected currentFragment='searchFragment'")
                            currentFragment = searchFragment
                            bind.searchBar.visibility = View.VISIBLE
                        }
                        TabLayoutItem.MYACTIVITY -> {
                            println("  onTabSelected currentFragment='myActivityFragment'")
                            currentFragment = myActivityFragment
                            bind.searchBar.visibility = View.INVISIBLE
                            bind.titleBar.text = "My Activity"
                        }
                    }

                    println("  setupBottomNavTabLayout, currentFragment=$currentFragment")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        }
        bind.tabLayout.addOnTabSelectedListener(onTabSelectedListener)
    }
    private fun teardownBottomNavTabLayout() {
        println("teardownBottomNavTabLayout")
        bind.tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
    }

    // Setup "search hashtag..." editText View
    private fun setupHashtagSearchEditText() {
        println("  setupHashtagSearchEditText searchFragment=$searchFragment")

        // Setup "Enter" & "Search" IME
        onEditorActionListener = TextView.OnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                -> {
                    println("onEditorActionListener SearchFragment=$searchFragment")
                    searchFragment?.onHashtagSearchActionSearch(v?.text.toString())
                    true
                }
                else -> {
                    false
                }
            }
        }

        // Setup single key press to update the "Currently following" Star Icon
        bind.search.setOnEditorActionListener(onEditorActionListener)
        textChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                val term = editable.toString()
                println("  textChangedListener SearchFragment=$searchFragment")
                searchFragment?.onHashtagSearchTermKeyPress(term)
            }
        }
        bind.search.addTextChangedListener(textChangedListener)
    }
    private fun teardownHashtagSearchEditText() {
        println("  teardownHashtagSearchEditText")
        if(::textChangedListener.isInitialized)
            bind.search.removeTextChangedListener(textChangedListener)
        bind.search.setOnEditorActionListener(null)
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