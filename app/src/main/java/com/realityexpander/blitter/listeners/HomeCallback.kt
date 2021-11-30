package com.realityexpander.blitter.listeners

import com.realityexpander.blitter.fragments.BlitterFragment
import com.realityexpander.blitter.fragments.SearchFragment

interface HomeCallback {

    fun onUserUpdated()

    fun onRefresh()

    // Pass back the fragment to the home activity when fragment is created after process death
    fun onSearchFragmentCreated(searchFragment: SearchFragment)
}