package com.realityexpander.blitter.listeners

import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.fragments.BaseFragment
import com.realityexpander.blitter.util.User

// HostContextI shared with the fragments
interface HostContextI {
    val firebaseDB: FirebaseFirestore
    val currentUserId: String?
    var currentUser: User?

    // Pass back a Refresh the UI for the current fragment
    fun onUpdateUIForCurrentFragment()

    // Pass back the system-created fragment to the home Activity after process death restoration
    fun onAndroidFragmentCreated(androidCreatedBaseFragment: BaseFragment)

    // Pass back an update the hashtag search query term from the fragment
    fun onUpdateHashtagSearchQueryTermEv(queryTerm: String)
}