package com.realityexpander.blitter.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.adapters.BleetListAdapter
import com.realityexpander.blitter.databinding.FragmentSearchBinding
import com.realityexpander.blitter.listeners.BleetListener
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.DATA_BLEETS_COLLECTION
import com.realityexpander.blitter.util.DATA_BLEETS_HASHTAGS
import com.realityexpander.blitter.util.User
import java.lang.Exception
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : BlitterFragment() {

    private lateinit var bind: FragmentSearchBinding
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val userId =  FirebaseAuth.getInstance().currentUser?.uid

    private var bleetListAdapter: BleetListAdapter? = null  // why not late init?
    private val bleetListener: BleetListener? = null // why not late init?
    private var currentUser: User? = null

    private var currentSearchHashtag = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentSearchBinding.inflate(inflater, container, false);
        return bind.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the listAdapter
        bleetListAdapter = BleetListAdapter(userId!!, arrayListOf())
        bleetListAdapter?.setListener(bleetListener)
        bind.bleetList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bleetListAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        // Setup swipe-to-refresh
        bind.swipeRefresh.setOnRefreshListener {
            bind.swipeRefresh.isRefreshing = true
            updateList()
        }
    }

    fun newHashTagSearch(term: String) {
        currentSearchHashtag = term
        bind.followHashtagIv.visibility = View.VISIBLE

        updateList()
    }

    private fun updateList() {

        // Show failure message
        fun onSearchBleetHashtagsFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(bind.root.context,
                "Searching hashtag failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bind.swipeRefresh.isRefreshing = false
        }

        bind.bleetList.visibility = View.GONE
        bind.swipeRefresh.isRefreshing = true

        // Get bleets from firebaseDB that match the search hashtag & sort desc by timeStamp
        firebaseDB.collection(DATA_BLEETS_COLLECTION)
            .whereArrayContainsAny(DATA_BLEETS_HASHTAGS, arrayListOf("#$currentSearchHashtag", currentSearchHashtag))
            .get()
            .addOnSuccessListener { list ->
                val bleets = arrayListOf<Bleet>()

                // Convert firebaseDB Bleet documents to Bleet POKOs
                for(bleetDocument in list.documents) {
                    val bleet = (bleetDocument.toObject(Bleet::class.java))
                    bleet?.let {
                        bleets.add(it)
                    }
                }
                val sortedBleets = bleets.sortedWith(
                    compareByDescending { it.timeStamp }
                )

                println(sortedBleets)

                bind.bleetList.visibility = View.VISIBLE
                bleetListAdapter?.updateBleets(sortedBleets)
                bind.swipeRefresh.isRefreshing = false

            }
            .addOnFailureListener { e->
                onSearchBleetHashtagsFailure(e)
            }
    }


}