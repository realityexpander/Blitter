package com.realityexpander.blitter.listeners

import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.DATA_BLEETS_COLLECTION
import com.realityexpander.blitter.util.DATA_BLEETS_LIKE_USER_IDS

class BlitterListenerImpl(
    private val bleetListRv: RecyclerView,
    private val homeContext: HomeContext?
): BleetListener {
//    private val firebaseDB = FirebaseFirestore.getInstance()
//    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid



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

            homeContext.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                .document(bleet.bleetId!!)
                .update(DATA_BLEETS_LIKE_USER_IDS, likeUserIds)
                .addOnSuccessListener {
                    homeContext.onRefreshList()
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