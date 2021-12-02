package com.realityexpander.blitter.util

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.realityexpander.blitter.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun ImageView.loadUrl(url: String?, errorDrawable: Int = R.drawable.empty) {
    context?.let {
        val options = RequestOptions()
            .placeholder(progressDrawable(context))
            .fallback(progressDrawable(context))
            .error(errorDrawable)
            .override(1000)

        Glide.with(context.applicationContext)
            .load(url)
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