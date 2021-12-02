package com.realityexpander.blitter.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.realityexpander.blitter.adapters.BleetListAdapter
import com.realityexpander.blitter.databinding.FragmentHomeBinding
import com.realityexpander.blitter.listeners.BlitterListenerImpl
import com.realityexpander.blitter.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : BlitterFragment() {

    private lateinit var bind: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentHomeBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.apply {
            // After process death, pass this System-created fragment to HomeContextI - (correct way to do this? SEEMS CLUNKY!)
            homeContextI?.onBlitterFragmentCreated(this@HomeFragment)

            // Restore query search state
            // showSearchResults = getBoolean("search_showSearchResults", false)
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // outState.putBoolean(SEARCH_FRAGMENT_SHOW_SEARCH_RESULTS, showSearchResults)
    }

    override fun onResume() {
        super.onResume()
        onUpdateUI()
    }

    override fun onUpdateUI() {
        refreshHomeNewsfeed()
    }

    private fun refreshHomeNewsfeed() {
        bind.swipeRefresh.isRefreshing = true
        val bleets = arrayListOf<Bleet>()

        fun sortBleetsByTimestampAndDisplayBleets() {
            // sort the bleets by timestamp
            val sortedBleets = bleets.toSet()
                .sortedWith(
                    compareByDescending { it.timestamp }
                )
            bleetListAdapter?.updateBleets(sortedBleets)

            bind.swipeRefresh.isRefreshing = false
        }

        // Show failure message
        fun onRefreshHomeFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(bind.root.context,
                "Refresh news feed bleets failed, please try again later. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.swipeRefresh.isRefreshing = false
        }

        fun getBleetsForFollowHashtags() {
            val followHashtags = homeContextI!!.currentUser?.followHashtags

            if(followHashtags?.isEmpty() == true) {
                sortBleetsByTimestampAndDisplayBleets()
                return
            }

            followHashtags?.let { hashtags ->
                var index = 0

                for (hashtag in hashtags) {
                    homeContextI!!.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                        .whereArrayContains(DATA_BLEETS_HASHTAGS, hashtag)
                        .get()
                        .addOnSuccessListener { list ->

                            // Convert firebaseDB Bleet documents to Bleet POKOs
                            for (bleetDocument in list.documents) {
                                val bleet = (bleetDocument.toObject(Bleet::class.java))
                                bleet?.let {
                                    bleets.add(it)
                                }
                            }

                            // println("Finished hashtag ${index+1} of ${hashtags.size}")
                            if (++index == hashtags.size) // are we done yet?
                                sortBleetsByTimestampAndDisplayBleets()
                        }
                        .addOnFailureListener { e ->
                            onRefreshHomeFailure(e)
                        }
                }
            }
        }

        fun getBleetsForFollowUserIdsAndFollowHashtags() {
//             println("homeContext = $homeContextI")
//             println("currentUser = ${homeContextI!!.currentUser}")
//             println("currentUserId = ${homeContextI!!.currentUserId}")
//             println("followUserIds = ${homeContextI!!.currentUser?.followUserIds}")
//             println("followHashtags = ${homeContextI!!.currentUser?.followHashtags}")

            val followUserIds = homeContextI!!.currentUser?.followUserIds

            if(followUserIds?.isEmpty() == true) {
                getBleetsForFollowHashtags()
                return
            }

            followUserIds?.let { userIds ->
                var index = 0

                for (followUserId in userIds) {
                    homeContextI!!.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                        .whereArrayContains(DATA_BLEETS_REBLEET_USER_IDS, followUserId)
                        .get()
                        .addOnSuccessListener { list ->

                            // Convert firebaseDB Bleet documents to Bleet POKOs
                            for (bleetDocument in list.documents) {
                                val bleet = (bleetDocument.toObject(Bleet::class.java))
                                bleet?.let {
                                    bleets.add(it)
                                }
                            }

//                             println("Finished followUserId ${index+1} of ${followUserIds.size}")
                            if (++index == userIds.size) // are we done yet?
                                getBleetsForFollowHashtags()
                        }
                        .addOnFailureListener { e ->
                            onRefreshHomeFailure(e)
                        }
                }
            }
        }

        // Get bleets that currentUser has: followingUserId, followHashtags
        //   & sort desc by timeStamp
        getBleetsForFollowUserIdsAndFollowHashtags()

    }
}

