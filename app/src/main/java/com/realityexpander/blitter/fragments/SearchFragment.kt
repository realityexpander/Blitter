package com.realityexpander.blitter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.realityexpander.blitter.R
import com.realityexpander.blitter.activities.HomeActivity
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

    private var currentSearchHashtag = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        println("onCreateView SearchFragment=$this, savedInstanceState=$savedInstanceState")

        bind = FragmentSearchBinding.inflate(inflater, container, false)
        println("  onCreateView bind=$bind")

        return bind.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        println("onViewCreated SearchFragment bind=$bind")

        if (savedInstanceState != null) {
            homeCallback?.onSearchFragmentCreated(this) // pass back the fragment that the system created after process death
        }

        bleetListener = BlitterListenerImpl(bind.bleetListRv, currentUser, homeCallback)

        // Setup the listAdapter
        bleetListAdapter = BleetListAdapter(currentUserId!!, arrayListOf())
        bleetListAdapter?.setListener(bleetListener)
        bind.bleetListRv.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bleetListAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        // Setup swipe-to-refresh
        bind.swipeRefresh.setOnRefreshListener {
            bind.swipeRefresh.isRefreshing = true
            updateList()
        }

        // Setup followHashtag button
        bind.followHashtagIv.setOnClickListener {
            bind.followHashtagIv.isClickable = false
            val followHashtags = currentUser?.followHashtags ?: arrayListOf()

            // Toggle followed hashtag for the current "search" query
            if(followHashtags.contains(currentSearchHashtag)) {
                followHashtags.remove(currentSearchHashtag)
            } else {
                followHashtags.add(currentSearchHashtag)
            }

            // Show failure message
            fun onUpdateFollowHashtagsFailure(e: Exception) {
                e.printStackTrace()
                Toast.makeText(bind.root.context,
                    "Adding hashtag failed, please try again. ${e.localizedMessage}",
                    Toast.LENGTH_LONG).show()
                bind.followHashtagIv.isClickable = true
            }

            // Save the updated list of hashtags for the user in the firebaseDB
            firebaseDB.collection(DATA_USERS_COLLECTION)
                .document(currentUserId)
                .update(DATA_USERS_FOLLOW_HASHTAGS, followHashtags)
                .addOnSuccessListener {
                    homeCallback?.onUserUpdated()
                    bind.followHashtagIv.isClickable = true
                }
                .addOnFailureListener { e->
                    onUpdateFollowHashtagsFailure(e)
                }
        }
    }

    // Search for a new search tag
    fun onHashtagSearchActionSearch(term: String) {
        println("onHashtagSearchActionSearch SearchFragment=$this, term=$term")
        currentSearchHashtag = term
        bind.followHashtagIv.visibility = View.VISIBLE

        updateList()
    }

    // Update the hash tag follow button based on the search term each keypress
    fun onHashtagSearchTermKeyPress(term: String) {
        currentSearchHashtag = term
        println("onHashtagSearchTermKeyPress SearchFragment=$this")
        bind.followHashtagIv.visibility = View.VISIBLE

        updateFollowHashtagButton()
    }

    // Show if the current search tag is followed by the user
    private fun updateFollowHashtagButton() {
        val followHashtags = currentUser?.followHashtags

        if(followHashtags?.contains(currentSearchHashtag) == true) {
            bind.followHashtagIv.setImageDrawable(
                ContextCompat.getDrawable(bind.followHashtagIv.context,
                R.drawable.follow))
        } else {
            bind.followHashtagIv.setImageDrawable(
                ContextCompat.getDrawable(bind.followHashtagIv.context,
                    R.drawable.follow_inactive))
        }
    }

    override fun updateList() {
        if(currentSearchHashtag.toString().isEmpty()) return

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
        firebaseDB.collection(DATA_BLEETS_COLLECTION)
            .whereArrayContains(DATA_BLEETS_HASHTAGS, currentSearchHashtag)
            .get()
            .addOnSuccessListener { list ->
                val bleets = arrayListOf<Bleet>()

                // Convert firebaseDB Bleet documents to Bleet POKOs
                for(bleetDocument in list.documents) {
                    val bleet = (bleetDocument.toObject(Bleet::class.java))
                    bleet?.let {
                        if(bleet.text?.contains(currentSearchHashtag) == true) {
                            bleets.add(it)
                        }
                    }
                }
                val sortedBleets = bleets.sortedWith(
                    compareByDescending { it.timestamp }
                )

                bleetListAdapter?.updateBleets(sortedBleets)
                updateFollowHashtagButton()
                bind.swipeRefresh.isRefreshing = false

            }
            .addOnFailureListener { e->
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

}