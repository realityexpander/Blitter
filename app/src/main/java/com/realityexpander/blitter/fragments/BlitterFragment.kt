package com.realityexpander.blitter.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import com.realityexpander.blitter.adapters.BleetListAdapter
import com.realityexpander.blitter.listeners.BleetListener
import com.realityexpander.blitter.listeners.HomeContextI

abstract class BlitterFragment: Fragment() {

    protected var bleetListAdapter: BleetListAdapter? = null
    protected var bleetListener: BleetListener? = null
    protected var homeContextI: HomeContextI? = null

    // Update the UI for the fragment
    abstract fun onUpdateUI()

    // Get the Host Activity context when fragment attaches
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (this.host is HomeContextI) {
            homeContextI = this.host as HomeContextI
        } else {
            throw RuntimeException("${this.host} must implement HomeContextI")
        }
    }

}