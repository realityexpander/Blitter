package com.realityexpander.blitter.listeners

import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.fragments.BlitterFragment
import com.realityexpander.blitter.fragments.SearchFragment
import com.realityexpander.blitter.util.User

interface HomeContext {

    fun onUserUpdated()

    fun onRefreshList()

    // Pass back the fragment to the home activity when fragment is created after process death
    fun onSearchFragmentCreated(searchFragment: SearchFragment)

    // Update the currentUser & all fragments
    fun updateCurrentUser(updatedUser: User?)

    val firebaseDB: FirebaseFirestore
    val currentUserId: String?
    var currentUser: User?
}