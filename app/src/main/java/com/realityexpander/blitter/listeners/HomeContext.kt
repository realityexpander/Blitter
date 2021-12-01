package com.realityexpander.blitter.listeners

import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.fragments.BlitterFragment
import com.realityexpander.blitter.fragments.SearchFragment
import com.realityexpander.blitter.util.User

// HomeContext shared with the fragments
interface HomeContext {
    val firebaseDB: FirebaseFirestore
    val currentUserId: String?
    var currentUser: User?

    fun onRefreshListForCurrentFragment()

    // Pass back the fragment to the home activity when fragment is created after process death
    fun onBlitterFragmentCreated(newBlitterFragment: BlitterFragment)
}