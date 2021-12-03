package com.realityexpander.blitter.listeners

import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.fragments.BlitterFragment
import com.realityexpander.blitter.fragments.SearchFragment
import com.realityexpander.blitter.util.User

// HomeContextI shared with the fragments
interface HomeContextI {
    val firebaseDB: FirebaseFirestore
    val currentUserId: String?
    var currentUser: User?

    // Refresh the UI for the current fragment
    fun onRefreshUIForCurrentFragment()

    // Pass back the system-created fragment to the home Activity after process death restoration
    fun onBlitterFragmentCreated(newBlitterFragment: BlitterFragment)

    // Update the hashtag search query term from the fragment
    fun onUpdateHashtagSearchQueryTermEv(queryTerm: String)
}