package com.realityexpander.blitter.listeners

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.realityexpander.blitter.databinding.DialogConfirmLayoutBinding
import com.realityexpander.blitter.util.*

class BlitterListenerImpl(
    private val bleetListRv: RecyclerView,
    private val homeContextI: HomeContextI?,
    private val hostFragment: Fragment
) : BleetListener {

    override fun onLayoutClick(bleet: Bleet?) {
        val currentUserId = homeContextI!!.currentUserId!!
        val currentUser = homeContextI.currentUser!!
        val followUserIds = currentUser.followUserIds
        val bleetUserId = bleet?.rebleetUserIds!![0]

        // Show failure message
        fun onSaveFollowUserFailure(e: Exception) {
            e.printStackTrace()
            Toast.makeText(homeContextI as Context,
                "Follow User failed, please try again. ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
            bleetListRv.isClickable = true
        }

        fun saveFollowUserIdsForCurrentUser() {
            // Save updated followUserIds to firebase database
            homeContextI.firebaseDB.collection(DATA_USERS_COLLECTION)
                .document(currentUserId)
                .update(DATA_USERS_FOLLOW_USER_IDS, followUserIds)
                .addOnSuccessListener {
                    homeContextI.onRefreshUIForCurrentFragment()
                    bleetListRv.isClickable = true
                }
                .addOnFailureListener { e ->
                    onSaveFollowUserFailure(e)
                }
        }

        val followUserIdAction: (userIdToFollow: String) -> Unit = { userIdToFollow->
            followUserIds.add(userIdToFollow)
            saveFollowUserIdsForCurrentUser()
        }

        val unfollowUserIdAction: (userIdToUnFollow: String) -> Unit = { userIdToUnFollow ->
            followUserIds.remove(userIdToUnFollow)
            saveFollowUserIdsForCurrentUser()
        }

        bleet.let {
            bleetListRv.isClickable = false

            if (bleetUserId == currentUserId) return // cant follow yourself (you already are)

            if (followUserIds.contains(bleetUserId)) {
                confirmDialog("Unfollow ${bleet.username}?",
                    bleetUserId,
                    unfollowUserIdAction )
            } else {
                confirmDialog("Follow ${bleet.username}?",
                    bleetUserId,
                    followUserIdAction )
            }
        }
    }

    private fun confirmDialog(dialogMessage: String,
                              userId: String,
                              positiveAction: (userId: String) -> Unit ) {

        val dialog = BottomSheetDialog(homeContextI as Context)
        val bindDialog = DialogConfirmLayoutBinding.inflate(
            hostFragment.layoutInflater,
            null,
            false
        )

        bindDialog.dialogMessageTv.text = dialogMessage

        dialog.setCancelable(true)
        bindDialog.closeIv.setOnClickListener {
            dialog.dismiss()
            (bindDialog.root.parent as ViewGroup).removeView(bindDialog.root)
        }
        bindDialog.positiveActionTv.setOnClickListener {
            dialog.dismiss()
            (bindDialog.root.parent as ViewGroup).removeView(bindDialog.root)

            positiveAction(userId)
        }
        bindDialog.negativeActionTv.setOnClickListener {
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
                    bleetId = reBleetDocument.id,
                    originalBleetId = originalBleet.bleetId,
                    username = homeContextI.currentUser?.username,
                    likeUserIds = arrayListOf(),      // when reBleeting, only show likes for this rebleet.
                    rebleetUserIds = arrayListOf(currentUserId), // when reBleeting, only include this and original author
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
            // find the bleet from this user that matches the original bleetId
            homeContextI.firebaseDB
                .collection(DATA_BLEETS_COLLECTION)
                .whereEqualTo(DATA_BLEETS_ORIGINAL_BLEET_ID, originalBleet.bleetId) // matches original bleetId
                .whereArrayContains(DATA_BLEETS_REBLEET_USER_IDS, currentUserId) // and contains this users' reBleet
                .get()
                .addOnSuccessListener {
                    // Delete this reBleet only if reBleetsUserId element 0 is the currentUserId
                    //   (element 0 is the author of this bleet)
                    val documentToDelete = it.documents.filter { bleetDocument ->
                        val bleetElement = bleetDocument.toObject(Bleet::class.java)
                        bleetElement?.rebleetUserIds?.getOrNull(0) == currentUserId
                    } // Should only be one! (because each user can only rebleet each bleet one time)

                    // If no records are found, Bleet must have been deleted already
                    if (documentToDelete.isEmpty()) {
                        onRebleetFailure(java.lang.Exception("Bleet not found"))
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