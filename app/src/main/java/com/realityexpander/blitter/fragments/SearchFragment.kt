package com.realityexpander.blitter.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.realityexpander.blitter.R
import com.realityexpander.blitter.adapters.BleetListAdapter
import com.realityexpander.blitter.databinding.FragmentSearchBinding
import com.realityexpander.blitter.listeners.BlitterListenerImpl
import com.realityexpander.blitter.util.*
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : BlitterFragment() {
    private lateinit var bind: FragmentSearchBinding

    private var currentHashtagQuery = ""
    private var isSearchResultsDisplayed: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // println("onCreateView SearchFragment=$this, savedInstanceState=$savedInstanceState")

        bind = FragmentSearchBinding.inflate(inflater, container, false)
        return bind.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.apply {
            // After process death, pass this System-created fragment to HomeContextI - (correct way to do this? SEEMS CLUNKY!)
            homeContextI?.onBlitterFragmentCreated(this@SearchFragment)

            onViewStateRestored(savedInstanceState)
        }

        // Setup the RV listAdapter
        bleetListAdapter = BleetListAdapter(homeContextI!!.currentUserId!!, arrayListOf())
        bleetListener = BlitterListenerImpl(bind.bleetListRv, homeContextI, this)
        bleetListAdapter?.setListener(bleetListener)
        bind.bleetListRv.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bleetListAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        // Setup swipe-to-refresh
        bind.swipeRefresh.setOnRefreshListener {
            bind.swipeRefresh.isRefreshing = true
            onUpdateUI()
        }

        // Setup followHashtag button
        bind.followHashtagIv.setOnClickListener {
            if (currentHashtagQuery.isEmpty()) return@setOnClickListener

            bind.followHashtagIv.isClickable = false
            val followHashtags = homeContextI!!.currentUser?.followHashtags ?: arrayListOf()

            // Toggle "follow hashtag" for the current "search hashtag" query
            if (followHashtags.contains(currentHashtagQuery)) {
                followHashtags.remove(currentHashtagQuery)
            } else {
                followHashtags.add(currentHashtagQuery)
            }

            onSaveFollowHashtagsToDatabase(followHashtags)
        }
    }
    override fun onResume() {
        super.onResume()

        onUpdateUI()
    }

    override fun onUpdateUI() {
        if (!this.isResumed) return

        updateHashTagUiElements()

        if (isSearchResultsDisplayed) {
            onSearchHashtagsInDatabase()
        }
    }

    // Save & Restore process death/config change
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putBoolean(SEARCH_FRAGMENT_SHOW_SEARCH_RESULTS, isSearchResultsDisplayed)
            putString(SEARCH_FRAGMENT_CURRENT_HASHTAG_QUERY, currentHashtagQuery)
        }
    }
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore query search state
        savedInstanceState?.apply {
            isSearchResultsDisplayed = getBoolean(SEARCH_FRAGMENT_SHOW_SEARCH_RESULTS, false)
            currentHashtagQuery = getString(SEARCH_FRAGMENT_CURRENT_HASHTAG_QUERY, "")
        }
    }

    // Search for a new hashtag
    fun onSearchHashtagQueryAction(queryTerm: String) {
        currentHashtagQuery = queryTerm
        isSearchResultsDisplayed = true
        bind.followHashtagIv.visibility = View.VISIBLE

        onSearchHashtagsInDatabase()
    }
    private fun onSearchHashtagsInDatabase() {
        bind.swipeRefresh.isRefreshing = false
        if (currentHashtagQuery.isEmpty()) return

        // Show failure message
        fun onSearchBleetHashtagsFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(bind.root.context,
                "Searching hashtag failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.swipeRefresh.isRefreshing = false
        }

        bind.swipeRefresh.isRefreshing = true

        // Get bleets from firebaseDB that match the search hashtag & sort desc by timeStamp
        homeContextI!!.firebaseDB.collection(DATA_BLEETS_COLLECTION)
            .whereArrayContains(DATA_BLEETS_HASHTAGS, currentHashtagQuery)
            .get()
            .addOnSuccessListener { list ->
                val bleets = arrayListOf<Bleet>()

                // Convert firebaseDB Bleet documents to Bleet POKOs
                for (bleetDocument in list.documents) {
                    val bleet = (bleetDocument.toObject(Bleet::class.java))
                    bleet?.let {
                        if (bleet.text?.contains(currentHashtagQuery) == true) {
                            bleets.add(it)
                        }
                    }
                }
                val sortedBleets = bleets.sortedWith(
                    compareByDescending { it.timestamp }
                )

                bleetListAdapter?.updateBleets(sortedBleets)
                updateHashTagUiElements()
                bind.swipeRefresh.isRefreshing = false

                if(bleets.size > 0) {
                    bind.root.let { v ->
                        val imm = bind.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                }

            }
            .addOnFailureListener { e ->
                onSearchBleetHashtagsFailure(e)
            }

//        // To search text field, matches only the start. Need to separate out the words into Arrays to do text search on the words of text
//        // For full fuzzy text search: https://github.com/typesense/firestore-typesense-search
//            .orderBy(DATA_BLEETS_TEXT)
////            .startAt(currentSearchHashtag)  // #1 seems to work the same as #2
////            .endAt(currentSearchHashtag+"\uf8ff")
//            .whereGreaterThanOrEqualTo(DATA_BLEETS_TEXT, currentSearchHashtag)  // #2
//            .whereLessThan(DATA_BLEETS_TEXT, currentSearchHashtag + "\uf8ff")
//            .get()
//            ...
//               bleet?.let {
//                  if(bleet.text?.contains(currentSearchHashtag) == true) {
//                      bleets.add(it)
//                  }
//            }
    }

    // Update the "follow this hashtag" button icon & chips based on the query term for every keypress
    fun onSearchHashtagQueryKeyPress(term: String) {
        //println("onHashtagSearchTermKeyPress SearchFragment=$this")
        currentHashtagQuery = term
        bind.followHashtagIv.visibility = View.VISIBLE

        updateHashTagUiElements()
    }

    // Save the current followHashtags to the DB
    private fun onSaveFollowHashtagsToDatabase(followHashtags: ArrayList<String>) {
        updateFollowHashtagButton() // optimistically update the button for fast user response

        // Show failure message
        fun onUpdateFollowHashtagsFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(bind.root.context,
                "Adding hashtag failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.followHashtagIv.isClickable = true
        }

        // Save the updated list of hashtags for the User in the firebaseDB
        homeContextI!!.firebaseDB.collection(DATA_USERS_COLLECTION)
            .document(homeContextI?.currentUserId!!)
            .update(DATA_USERS_FOLLOW_HASHTAGS, followHashtags)
            .addOnSuccessListener {
                bind.followHashtagIv.isClickable = true
                updateFollowHashtagChipGroupView()
            }
            .addOnFailureListener { e ->
                onUpdateFollowHashtagsFailure(e)
            }
    }

    // Update all the hashtag related UI elements based on current hashtag query
    private fun updateHashTagUiElements() {
        updateFollowHashtagButton()
        updateFollowHashtagChipGroupView()
    }
    // Indicate current query hashtag is followed by the user
    private fun updateFollowHashtagButton() {
        val followHashtags = homeContextI!!.currentUser?.followHashtags

        if (followHashtags?.contains(currentHashtagQuery) == true) {
            bind.followHashtagIv.setImageDrawable(
                ContextCompat.getDrawable(bind.followHashtagIv.context,
                    R.drawable.follow))
        } else {
            bind.followHashtagIv.setImageDrawable(
                ContextCompat.getDrawable(bind.followHashtagIv.context,
                    R.drawable.follow_inactive))
        }
    }
    private fun updateFollowHashtagChipGroupView() {
        val followHashtags = homeContextI!!.currentUser?.followHashtags
        if (followHashtags.isNullOrEmpty()) return

        // Create the chipGroup chips for the followHashtags
        val chipGroup = bind.chipGroup
        chipGroup.removeAllViews()
        followHashtags.forEach { hashTag ->
            // val chip = Chip(bind.chipGroup.context) // this is ok if chip does not need to be
            //   clickable bc no resource ID is assigned. The closeIcon is still clickable tho.
            val chip = this.layoutInflater.inflate(R.layout.chip_hashtag,
                bind.chipGroup,
                false) as Chip // assigns an ID
            chip.text = hashTag
            chip.isCloseIconVisible = true
            chip.isCheckedIconVisible = true
            chipGroup.addView(chip)

            // Set checkmark if the search is for this hashtag
            if (chip.text == currentHashtagQuery) chip.isChecked = true
        }
        setupHashtagChipGroupClickListeners()
    }
    private fun setupHashtagChipGroupClickListeners() {

        // Setup ChipGroup for hashtags
        bind.chipGroup.forEach { chipView ->

            // Tap "X" will remove the hashtag from the set of followHashtags
            (chipView as Chip).setOnCloseIconClickListener { chip ->
                val chipText = (chip as Chip).text.toString()
                bind.chipGroup.removeView(chip)

                val followHashtags = homeContextI!!.currentUser?.followHashtags
                if (followHashtags?.contains(chipText) == true) { // just to safe we check if user is following the hashtag
                    followHashtags.remove(chipText)
                    onSaveFollowHashtagsToDatabase(followHashtags)
                }

                onUpdateUI()
            }

            // Tap on chip name initiates a new hashtag search, selects the chip
            (chipView as Chip).setOnCheckedChangeListener { buttonView, _ ->
                val chip = buttonView as Chip
                val chipText = chip.text.toString()

                // set the checkmark to the tapped hashtag chip
                bind.chipGroup.clearCheck()
                chip.isChecked = true

                // update the search query in the home activity
                homeContextI!!.onUpdateHashtagSearchQueryTermEv(chipText)

                onSearchHashtagQueryAction(chipText)
            }
        }

//        // Setup tap chip searches for that hashtag
//        bind.chipGroup.setOnCheckedChangeListener{ chipGroup, item ->
//            // Set new search term to the tapped Chip
//            val chip = chipGroup.findViewById<Chip>(item)
//            val chipText = chip.text.toString()
//            // println("chipGroup=$chipGroup, item=$item, text=$chipText")
//
//            homeContextI!!.onUpdateHashtagSearchQueryTermEv(chipText)
//            onHashtagQueryActionSearch(chipText)
//
//            // Cannot set check mark easily in singleSelection=true mode.
//            // This mode is good for just clicking on the on the chip.
//            // bind.chipGroup.clearCheck()
//            // bind.chipGroup.check(item) // i think this performs a click in SingleSelection=true mode
//            onUpdateUI() // endless loop here
//        }
//
//        // Setup tap to remove hashtag
//        bind.chipGroup.forEach { chipView ->
//            // Remove the hashtag from the set of followHashtags
//            (chipView as Chip).setOnCloseIconClickListener { chip ->
//                val chipText = (chip as Chip).text.toString()
//                bind.chipGroup.removeView(chip)
//
//                val followHashtags = homeContextI!!.currentUser?.followHashtags
//                if (followHashtags?.contains(chipText) == true) { // just to be safe, we check if user is following the hashtag
//                    followHashtags.remove(chipText)
//                    onUpdateFollowHashtagsToDatabase(followHashtags)
//                }
//
//                onUpdateUI()
//            }
//        }

    }

}