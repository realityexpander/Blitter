package com.realityexpander.blitter.listeners

import android.content.Context
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.DATA_BLEETS_COLLECTION
import com.realityexpander.blitter.util.DATA_BLEETS_LIKE_USER_IDS
import com.realityexpander.blitter.util.DATA_BLEETS_REBLEET_USER_IDS
import java.util.ArrayList

class BlitterListenerImpl(
    private val bleetListRv: RecyclerView,
    private val homeContextI: HomeContextI?,
) : BleetListener {

    override fun onLayoutClick(bleet: Bleet?) {
//        TODO("Not yet implemented")
    }

    override fun onLike(bleet: Bleet?) {
        val currentUserId = homeContextI!!.currentUserId!!

        bleet?.let {
            bleetListRv.isClickable = false
            val likeUserIds = bleet.likeUserIds

            if (likeUserIds?.contains(currentUserId) == true) {
                likeUserIds.remove(currentUserId)
            } else {
                likeUserIds?.add(currentUserId)
            }

            // Save like to firebase database
            homeContextI.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                .document(bleet.bleetId!!)
                .update(DATA_BLEETS_LIKE_USER_IDS, likeUserIds)
                .addOnSuccessListener {
                    homeContextI.onRefreshUIForCurrentFragment()
                    bleetListRv.isClickable = true
                }
                .addOnFailureListener { e ->
                    println(e.localizedMessage)
                    bleetListRv.isClickable = true
                }

        }
    }

    override fun onRebleet(bleet: Bleet?) {
        val currentUserId = homeContextI!!.currentUserId!!

        // Show failure message
        fun onRebleetFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(homeContextI as Context,
                "Rebleeting failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bleetListRv.isClickable = true
        }

        fun reBleetThisBleet(bleet: Bleet) {
            // Create a new bleet document as a copy of this bleet, this will be the reBleet
            val reBleetDocument = homeContextI.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                .document()

            // Create a copy of the bleet that will be rebleeted
            val reBleet = bleet.deepCopy()
                .copy(
                    bleetId = reBleetDocument.id,
                    username = homeContextI.currentUser?.username,
                    timestamp = System.currentTimeMillis()
                )

            reBleetDocument.set(reBleet)
                .addOnSuccessListener {
                    homeContextI.onRefreshUIForCurrentFragment()
                    bleetListRv.isClickable = true
                }
                .addOnFailureListener { e ->
                    onRebleetFailure(e)
                }
        }

        bleet?.let { bleet ->
            bleetListRv.isClickable = false
            val rebleetUserIds = bleet.rebleetUserIds

            if (rebleetUserIds?.contains(currentUserId) == true) {
                return // can't rebleet a bleet that's already been bleeted
            } else {
                rebleetUserIds?.add(currentUserId)
            }

            // Save updated list of rebleetUserIds for this bleet
            homeContextI.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                .document(bleet.bleetId!!)
                .update(DATA_BLEETS_REBLEET_USER_IDS, rebleetUserIds)
                .addOnSuccessListener {
                    reBleetThisBleet(bleet)
                }
                .addOnFailureListener { e ->
                    onRebleetFailure(e)
                }
        }
    }
}