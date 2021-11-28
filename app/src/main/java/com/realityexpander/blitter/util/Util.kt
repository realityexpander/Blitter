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

fun ImageView.loadUrl(url: String?, errorDrawable: Int = R.drawable.empty) {
    println("***** Using loadUrl from Url: $url")
    context?.let {
        val options = RequestOptions()
            .placeholder(progressDrawable(context))
            .error(errorDrawable)

        Glide.with(context.applicationContext)
            .load(url)
//            .load("content://com.android.providers.media.documents/document/image:5141")
            .apply(options)
            .into(this)
    }
}

fun ImageView.loadUrl(urlFile: File, errorDrawable: Int = R.drawable.empty) {
    println("***** Using loadUrl from File: $urlFile")
    context?.let {
        val options = RequestOptions()
            .placeholder(progressDrawable(context))
            .error(errorDrawable)

        Glide.with(context.applicationContext)
            .load(Uri.fromFile(urlFile))
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