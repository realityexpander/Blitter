package com.realityexpander.blitter.listeners

import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.User

class BlitterListenerImpl(val bleetList: RecyclerView,
                          val currentUser: User?,
                          val homeCallback: HomeCallback
): BleetListener {
    override fun onLayoutClick(bleet: Bleet?) {
        TODO("Not yet implemented")
    }

    override fun onLike(bleet: Bleet?) {
        TODO("Not yet implemented")
    }

    override fun onRebleet(bleet: Bleet?) {
        TODO("Not yet implemented")
    }
}