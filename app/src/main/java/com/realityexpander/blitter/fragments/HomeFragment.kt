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
class HomeFragment : BaseFragment() {

//    private lateinit var bind: FragmentHomeBinding // alternate way
    private var _bind: FragmentHomeBinding? = null
    private val bind: FragmentHomeBinding
        get() = _bind!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _bind = FragmentHomeBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.apply {
            // After process death, pass this System-created fragment to HostContextI - (correct way to do this? SEEMS CLUNKY!)
            hostContextI?.onAndroidFragmentCreated(this@HomeFragment)

            // not needed yet
            // onViewStateRestored(savedInstanceState)
        }

        // Setup the RV listAdapter
        bleetListAdapter = BleetListAdapter(hostContextI!!.currentUserId!!, arrayListOf())
        bleetListener = BlitterListenerImpl(bind.bleetListRv, hostContextI, this)
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

    // not needed yet.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // outState.putBoolean(SEARCH_FRAGMENT_SHOW_SEARCH_RESULTS, showSearchResults)
    }
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Not Needed Yet
        // savedInstanceState?.apply {
        //    currentHashtagQuery = getString(SEARCH_FRAGMENT_CURRENT_HASHTAG_QUERY, "")
        //}
    }

    override fun onResume() {
        super.onResume()

        onUpdateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }

    override fun onUpdateUI() {
        if (!this.isResumed) return

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
            val followHashtags = hostContextI!!.currentUser?.followHashtags

            if(followHashtags?.isEmpty() == true) {
                sortBleetsByTimestampAndDisplayBleets()
                return
            }

            followHashtags?.let { hashtags ->
                var index = 0

                for (hashtag in hashtags) {
                    hostContextI!!.firebaseDB.collection(DATA_BLEETS_COLLECTION)
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
//             println("homeContext = $hostContextI")
//             println("currentUser = ${hostContextI!!.currentUser}")
//             println("currentUserId = ${hostContextI!!.currentUserId}")
//             println("followUserIds = ${hostContextI!!.currentUser?.followUserIds}")
//             println("followHashtags = ${hostContextI!!.currentUser?.followHashtags}")

            val followUserIds = hostContextI!!.currentUser?.followUserIds ?: arrayListOf()

            // Add the currentUserId to show the user their own bleets as well as others
            followUserIds.add(hostContextI!!.currentUserId!!)

            if(followUserIds.isEmpty()) {
                getBleetsForFollowHashtags()
                return
            }

            followUserIds.let { userIds ->
                var index = 0

                for (followUserId in userIds) {
                    hostContextI!!.firebaseDB.collection(DATA_BLEETS_COLLECTION)
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

