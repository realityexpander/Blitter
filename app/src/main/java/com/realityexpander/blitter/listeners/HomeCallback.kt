package com.realityexpander.blitter.listeners

import com.realityexpander.blitter.fragments.BlitterFragment
import com.realityexpander.blitter.fragments.SearchFragment
import com.realityexpander.blitter.util.User

interface HomeCallback {

    fun onUserUpdated()

    fun onRefreshList()

    // Pass back the fragment to the home activity when fragment is created after process death
    fun onSearchFragmentCreated(searchFragment: SearchFragment)

    // Update the fragments with the updated User
    fun updateFragmentsWithUpdatedUser(updatedUser: User?)
}