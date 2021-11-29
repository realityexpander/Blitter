package com.realityexpander.blitter.listeners

import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.DATA_BLEETS_COLLECTION
import com.realityexpander.blitter.util.DATA_BLEETS_LIKES_USERIDS
import com.realityexpander.blitter.util.User

class BlitterListenerImpl(val bleetListRv: RecyclerView,
                          val currentUser: User?,
                          val homeCallback: HomeCallback?
): BleetListener {
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onLayoutClick(bleet: Bleet?) {
//        TODO("Not yet implemented")
    }

    override fun onLike(bleet: Bleet?) {
        bleet?.let {
            bleetListRv.isClickable = false
            val likesUserIds = bleet.likesUserIds

            if(likesUserIds?.contains(currentUserId) == true) {
                likesUserIds.remove(currentUserId)
            } else {
                likesUserIds?.add(currentUserId!!)
            }

            firebaseDB.collection(DATA_BLEETS_COLLECTION)
                .document(bleet.bleetId!!)
                .update(DATA_BLEETS_LIKES_USERIDS, likesUserIds)
                .addOnSuccessListener {
                    bleetListRv.isClickable = true
                    homeCallback?.onRefresh()
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