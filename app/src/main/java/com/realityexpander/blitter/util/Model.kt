package com.realityexpander.blitter.util

data class User(
    val email: String? = "",
    val username: String? = "",
    val imageUrl: String = "",
    val followHashtags: ArrayList<String> = arrayListOf(),
    val followUsers: ArrayList<String> = arrayListOf(),
    val signupTimestamp: Long = 0,
    var updatedTimestamp: Long = 0
)