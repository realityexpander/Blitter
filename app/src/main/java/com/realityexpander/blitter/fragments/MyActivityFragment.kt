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
import com.realityexpander.blitter.databinding.FragmentMyActivityBinding
import com.realityexpander.blitter.listeners.BlitterListenerImpl
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.DATA_BLEETS_COLLECTION
import com.realityexpander.blitter.util.DATA_BLEETS_REBLEET_USER_IDS

/**
 * A simple [Fragment] subclass.
 * Use the [MyActivityFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyActivityFragment : BlitterFragment() {

    private lateinit var bind: FragmentMyActivityBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bind = FragmentMyActivityBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.apply {
            // After process death, pass this System-created fragment to HomeContextI - (correct way to do this? SEEMS CLUNKY!)
            homeContextI?.onBlitterFragmentCreated(this@MyActivityFragment)

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

    // not needed yet.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // outState.putBoolean(SEARCH_FRAGMENT_SHOW_SEARCH_RESULTS, showSearchResults)
    }

    override fun onResume() {
        super.onResume()
        onUpdateUI()
    }

    override fun onUpdateUI() {
        refreshHomeMyActivityFeed()
    }

    // Show original bleets and rebleets from the currentUserId
    private fun refreshHomeMyActivityFeed() {
        bind.swipeRefresh.isRefreshing = true
        val bleets = arrayListOf<Bleet>()

        fun sortBleetsByTimestampAndUpdateList() {
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

        fun getBleetsForCurrentUserId() {
//             println("homeContext = $homeContextI")
//             println("currentUser = ${homeContextI!!.currentUser}")
//             println("currentUserId = ${homeContextI!!.currentUserId}")
//             println("followUserIds = ${homeContextI!!.currentUser?.followUserIds}")
//             println("followHashtags = ${homeContextI!!.currentUser?.followHashtags}")

            val currentUserId = homeContextI!!.currentUserId

            currentUserId?.let { userId ->
                homeContextI!!.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                    .whereArrayContains(DATA_BLEETS_REBLEET_USER_IDS, userId)
                    .get()
                    .addOnSuccessListener { list ->

                        // Convert firebaseDB Bleet documents to Bleet POKOs
                        for (bleetDocument in list.documents) {
                            val bleet = (bleetDocument.toObject(Bleet::class.java))
                            bleet?.let {
                                // Check if the bleet is owned by the currentUserId
                                if (bleet.rebleetUserIds?.getOrNull(0) == userId)
                                    bleets.add(it)
                            }
                        }

                        sortBleetsByTimestampAndUpdateList()
                    }
                    .addOnFailureListener { e ->
                        onRefreshHomeFailure(e)
                    }
            }
        }

        // Get bleets for current user & sort desc by timeStamp
        getBleetsForCurrentUserId()

    }

}