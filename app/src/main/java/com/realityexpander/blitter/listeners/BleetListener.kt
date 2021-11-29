package com.realityexpander.blitter.listeners

import com.realityexpander.blitter.util.Bleet

interface BleetListener {

    fun onLayoutClick(bleet: Bleet?)
    fun onLike(bleet: Bleet?)
    fun onRebleet(bleet: Bleet?)

}