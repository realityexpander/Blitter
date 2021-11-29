package com.realityexpander.blitter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.blitter.R
import com.realityexpander.blitter.listeners.BleetListener
import com.realityexpander.blitter.util.Bleet
import com.realityexpander.blitter.util.getDateString
import com.realityexpander.blitter.util.loadUrl
import kotlin.collections.ArrayList

class BleetListAdapter(val userId: String, val bleets: ArrayList<Bleet>) :
    RecyclerView.Adapter<BleetListAdapter.BleetViewHolder>() {


    private var listener: BleetListener? = null

    fun setListener(listener: BleetListener?) {
        this.listener = listener
    }

    fun updateBleets(newBleets: List<Bleet>) {
        bleets.clear()
        bleets.addAll(newBleets)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return bleets.size
    }

    override fun onBindViewHolder(holder: BleetViewHolder, position: Int) {
        holder.bind(userId, bleets[position], listener)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BleetListAdapter.BleetViewHolder = BleetViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_bleet, parent, false)
    )

    class BleetViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private val layout = v.findViewById<ViewGroup>(R.id.bleetItemLayout)
        private val userName = v.findViewById<TextView>(R.id.bleetUserName)
        private val text = v.findViewById<TextView>(R.id.bleetText)
        private val image = v.findViewById<ImageView>(R.id.bleetImage)
        private val date = v.findViewById<TextView>(R.id.bleetDate)
        private val likeButton = v.findViewById<ImageView>(R.id.bleetLike)
        private val likeCount = v.findViewById<TextView>(R.id.bleetLikeCount)
        private val rebleetButton = v.findViewById<ImageView>(R.id.bleetRebleet)
        private val rebleetCount = v.findViewById<TextView>(R.id.bleetRebleetCount)

        fun bind(userId: String, bleet: Bleet, listener: BleetListener?) {

            // Setup the bleet data
            userName.text = bleet.userName
            text.text = bleet.text

            if (bleet.imageUrl.isNullOrEmpty()) {
                image.visibility = View.INVISIBLE
            } else {
                image.visibility = View.VISIBLE
                image.loadUrl(bleet.imageUrl)
            }

            date.text = bleet.timeStamp.getDateString()
            likeCount.text = bleet.likesUserIds?.size.toString()
            rebleetCount.text = bleet.rebleetUserIds?.size?.minus(1).toString()

            // Setup buttons & actions
            layout.setOnClickListener { listener?.onLayoutClick(bleet) }
            likeButton.setOnClickListener { listener?.onLike(bleet) }
            rebleetButton.setOnClickListener { listener?.onRebleet(bleet) }

            // If the likes of this bleet contain this userId, set it to liked
            if (bleet.likesUserIds?.contains(userId) == true) {
                likeButton.setImageDrawable(ContextCompat.getDrawable(likeButton.context,
                    R.drawable.like))
            } else {
                likeButton.setImageDrawable(ContextCompat.getDrawable(likeButton.context,
                    R.drawable.like_inactive))
            }

            when {
                // If the bleet is from the userId, show "unRebleeted" icon
                bleet.rebleetUserIds?.getOrNull(0).equals(userId) -> {
                    rebleetButton.setImageDrawable(ContextCompat.getDrawable(rebleetButton.context,
                        R.drawable.original))
                    rebleetButton.isClickable = false
                }
                // If the rebleets of this bleet contain this userId, show "rebleeted" icon
                bleet.rebleetUserIds?.contains(userId) == true -> {
                    rebleetButton.setImageDrawable(ContextCompat.getDrawable(rebleetButton.context,
                        R.drawable.retweet))
                }
                else -> {
                    rebleetButton.setImageDrawable(ContextCompat.getDrawable(rebleetButton.context,
                        R.drawable.retweet_inactive))
                }
            }
        }

    }
}