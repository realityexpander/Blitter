package com.realityexpander.blitter.listeners

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.realityexpander.blitter.databinding.DialogFollowLayoutBinding
import com.realityexpander.blitter.util.*

class BlitterListenerImpl(
    private val bleetListRv: RecyclerView,
    private val homeContextI: HomeContextI?,
    private val fragment: Fragment,
) : BleetListener {

    override fun onLayoutClick(bleet: Bleet?) {

        val dialog = BottomSheetDialog(homeContextI as Context)
        val bindDialog = DialogFollowLayoutBinding.inflate(fragment.layoutInflater,
            null,
            false)
        dialog.setCancelable(true)
        bindDialog.closeIv.setOnClickListener {
            dialog.dismiss()
            (bindDialog.root.parent as ViewGroup).removeView(bindDialog.root)
        }
        bindDialog.yesTv.setOnClickListener {
            Toast.makeText(homeContextI as Context,
                "Clicked Yes!",
                Toast.LENGTH_LONG).show()
            dialog.dismiss()
            (bindDialog.root.parent as ViewGroup).removeView(bindDialog.root)
        }
        bindDialog.noTv.setOnClickListener {
            Toast.makeText(homeContextI as Context,
                "Clicked No!",
                Toast.LENGTH_LONG).show()
            dialog.dismiss()
            (bindDialog.root.parent as ViewGroup).removeView(bindDialog.root)
        }
        dialog.setContentView(bindDialog.root)
        dialog.show()

    }

    override fun onLike(bleet: Bleet?) {
        val currentUserId = homeContextI!!.currentUserId!!

        // Show failure message
        fun onLikeFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(homeContextI as Context,
                "Like failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bleetListRv.isClickable = true
        }

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
                    onLikeFailure(e)
                }

        }
    }

    private enum class ReBleetAction {
        REMOVE_REBLEET, // Remove the rebleet from the firebaseDB
        ADD_REBLEET     // Add the rebleet to the firebaseDB
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

        // Add the rebleet
        fun reBleetThisBleet(originalBleet: Bleet) {
            // Create a new bleet document as a copy of this bleet, this will be the reBleet
            val reBleetDocument = homeContextI.firebaseDB
                .collection(DATA_BLEETS_COLLECTION)
                .document()

            // Create a copy of the bleet that will be rebleeted
            val reBleet = originalBleet.deepCopy()
                .copy(
                    bleetId = originalBleet.bleetId,
                    username = homeContextI.currentUser?.username,
                    likeUserIds = arrayListOf(),      // when reBleeting, only show likes for this rebleet.
                    rebleetUserIds = arrayListOf(currentUserId,
                        originalBleet.rebleetUserIds!![0]), // when reBleeting, only include this and original author
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

        // Remove the rebleet
        fun removeTheReBleetOfThisBleet(originalBleet: Bleet) {
            // find the bleet from this user that matches the original bleet's id
            homeContextI.firebaseDB
                .collection(DATA_BLEETS_COLLECTION)
                .whereEqualTo(DATA_BLEETS_BLEET_ID,
                    originalBleet.bleetId) // matches original bleetId
                .whereArrayContains(DATA_BLEETS_REBLEET_USER_IDS,
                    currentUserId) // and contains this users' reBleet
                .get()
                .addOnSuccessListener {
                    // Delete this reBleet only if rebleetsUserId element 0 is the currentUserId
                    //   (element 0 is the author of this bleet)
                    val documentToDelete = it.documents.filter { bleetDocument ->
                        val bleetElement = bleetDocument.toObject(Bleet::class.java)
                        bleetElement?.rebleetUserIds?.getOrNull(0) == currentUserId
                    } // Should only be one!

                    // If no records are found, the database is corrupted
                    if (documentToDelete.isEmpty()) {
                        onRebleetFailure(java.lang.Exception("Record not found"))
                    }

                    homeContextI.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                        .document(documentToDelete[0].id)  // Should only be one rebleet from this userId
                        .delete()
                        .addOnSuccessListener {
                            homeContextI.onRefreshUIForCurrentFragment()
                            bleetListRv.isClickable = true
                        }
                        .addOnFailureListener { e ->
                            onRebleetFailure(e)
                        }

                    homeContextI.onRefreshUIForCurrentFragment()
                    bleetListRv.isClickable = true
                }
                .addOnFailureListener { e ->
                    onRebleetFailure(e)
                }
        }

        // PROCESS THE REBLEET TASK
        bleet?.let { originalBleet ->
            bleetListRv.isClickable = false
            val rebleetUserIds = originalBleet.rebleetUserIds
            var reBleetAction: ReBleetAction? = null

            if (rebleetUserIds?.contains(currentUserId) == true) {
                if (rebleetUserIds[0] == currentUserId) { // is this the currentUsers' bleet?
                    return // cant rebleet your own original bleet
                }
                rebleetUserIds.remove(currentUserId)
                reBleetAction = ReBleetAction.REMOVE_REBLEET
            } else {
                rebleetUserIds?.add(currentUserId)
                reBleetAction = ReBleetAction.ADD_REBLEET
            }

            // Save updated list of rebleetUserIds for this bleet
            homeContextI.firebaseDB.collection(DATA_BLEETS_COLLECTION)
                .document(originalBleet.bleetId!!)
                .update(DATA_BLEETS_REBLEET_USER_IDS, rebleetUserIds)
                .addOnSuccessListener {
                    when (reBleetAction) {
                        ReBleetAction.ADD_REBLEET -> reBleetThisBleet(originalBleet)
                        ReBleetAction.REMOVE_REBLEET -> removeTheReBleetOfThisBleet(originalBleet)
                    }
                }
                .addOnFailureListener { e ->
                    onRebleetFailure(e)
                }
        }

    }
}