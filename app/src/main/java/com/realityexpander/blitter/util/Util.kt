package com.realityexpander.blitter.util

import android.content.Context
import android.widget.ImageView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.realityexpander.blitter.R
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

fun ImageView.loadUrl(url: String?, errorDrawable: Int = R.drawable.empty) {
    if(url.isNullOrEmpty()) return

    context?.let {
        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(progressDrawable(context))
            .fallback(progressDrawable(context))
            .error(errorDrawable)
            .override(1000,750)
            .fitCenter()

        Glide.with(context.applicationContext)
            .load(url)
            .thumbnail(0.5f)
            .apply(options)
            .into(this)
    }
}

fun progressDrawable(context: Context) : CircularProgressDrawable {
    return CircularProgressDrawable(context).apply {
        strokeWidth = 5f
        centerRadius = 30f
        start()
    }
}

fun Long?.getDateString(): String {
    var dateLong: Long? = this ?: return "unknown date"

    val sdf = SimpleDateFormat("EEE MMM dd, yyyy hh:mm a", Locale.US)
    val resultDate = Date(dateLong!!)

    return sdf.format(resultDate).lowercase(Locale.US)
}

fun ArrayList<String>?.deepCompare(other: ArrayList<String>?): Boolean {
    if(this == null && other == null) return true

    if (Json.encodeToString(this) == Json.encodeToString(other)) return true

    return false
}

fun Array<*>.deepEquals(other: Array<*>) = this.contentDeepEquals(other)

// example: previousFollowUserIds = homeContextI!!.currentUser?.followUserIds.deepCopy()
fun ArrayList<String>?.deepCopy(): ArrayList<String>? {
    if (this == null) return null

    return Json.decodeFromString<ArrayList<String>>(Json.encodeToString(this))
}


fun ViewPager2.reduceDragSensitivity(touchSlopFactor: Int = 4) {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView

    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int

    touchSlopField.set(recyclerView, touchSlop*touchSlopFactor)
}
