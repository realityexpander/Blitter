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

    abstract fun onUpdateUI()

    // Get the HomeActivity context
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context is HomeContextI) {
            homeContextI = context
        } else {
            throw RuntimeException("$context must implement HomeCallBack")
        }
    }

}