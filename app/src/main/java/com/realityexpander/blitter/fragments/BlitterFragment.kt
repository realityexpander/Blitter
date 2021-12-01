package com.realityexpander.blitter.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import com.realityexpander.blitter.adapters.BleetListAdapter
import com.realityexpander.blitter.listeners.BleetListener
import com.realityexpander.blitter.listeners.HomeContext

abstract class BlitterFragment: Fragment() {

//    protected val firebaseDB = FirebaseFirestore.getInstance()
//    protected val currentUserId =  FirebaseAuth.getInstance().currentUser?.uid
//    protected var currentUser: User? = null

    protected var bleetListAdapter: BleetListAdapter? = null  // why not late init?
    protected var bleetListener: BleetListener? = null // why not late init?
    protected var homeContext: HomeContext? = null

//    fun setUser(user: User?) {
//        this.currentUser = user
//    }

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

    override fun onResume() {
        super.onResume()

        updateList()
    }
}