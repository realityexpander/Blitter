package com.realityexpander.blitter.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import com.realityexpander.blitter.adapters.BleetListAdapter
import com.realityexpander.blitter.listeners.BleetListener
import com.realityexpander.blitter.listeners.HostContextI

abstract class BaseFragment: Fragment() {

    protected var bleetListAdapter: BleetListAdapter? = null
    protected var bleetListener: BleetListener? = null
    protected var hostContextI: HostContextI? = null

    // Update the UI for the fragment
    abstract fun onUpdateUI()

    // Get the Host Activity context when fragment attaches
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (this.host is HostContextI) {
            hostContextI = this.host as HostContextI
        } else {
            throw RuntimeException("${this.host} must implement HostContextI")
        }
    }

}