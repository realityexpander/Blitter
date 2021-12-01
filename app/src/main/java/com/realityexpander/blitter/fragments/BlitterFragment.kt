package com.realityexpander.blitter.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import com.realityexpander.blitter.adapters.BleetListAdapter
import com.realityexpander.blitter.listeners.BleetListener
import com.realityexpander.blitter.listeners.HomeContext

abstract class BlitterFragment: Fragment() {

    protected var bleetListAdapter: BleetListAdapter? = null
    protected var bleetListener: BleetListener? = null
    protected var homeContext: HomeContext? = null

    abstract fun updateList()

    // Get the HomeActivity context
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context is HomeContext) {
            homeContext = context
        } else {
            throw RuntimeException("$context must implement HomeCallBack")
        }
    }

//    override fun onResume() {
//        super.onResume()
//
//        updateList() // forces all fragment lists to update after onResume
//    }
}