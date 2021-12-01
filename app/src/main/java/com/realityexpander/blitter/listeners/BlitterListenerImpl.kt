package com.realityexpander.blitter.listeners

import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.DATA_BLEETS_COLLECTION
import com.realityexpander.blitter.util.DATA_BLEETS_LIKE_USER_IDS

class BlitterListenerImpl(
    private val bleetListRv: RecyclerView,
    private val homeContext: HomeContext?
): BleetListener {

    override fun onLayoutClick(bleet: Bleet?) {
//        TODO("Not yet implemented")
    }

    override fun onLike(bleet: Bleet?) {
        val currentUserId = homeContext!!.currentUserId!!

        bleet?.let {
            bleetListRv.isClickable = false
            val likeUserIds = bleet.likeUserIds

            if(likeUserIds?.contains(currentUserId) == true) {
                likeUserIds.remove(currentUserId)
            } else {
                likeUserIds?.add(currentUserId)
            }

            // Save like to firebase database
            homeContext.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                .document(bleet.bleetId!!)
                .update(DATA_BLEETS_LIKE_USER_IDS, likeUserIds)
                .addOnSuccessListener {
                    homeContext.onRefreshListForCurrentFragment()
                    bleetListRv.isClickable = true
                }
                .addOnFailureListener { e->
                    println(e.localizedMessage)
                    bleetListRv.isClickable = true
                }

        }
    }

    override fun onRebleet(bleet: Bleet?) {
//        TODO("Not yet implemented")
    }
}