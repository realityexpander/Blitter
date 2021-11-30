package com.realityexpander.blitter.util

data class User(
    val email: String? = "",
    val username: String? = "",
    val imageUrl: String = "",
    val followHashtags: ArrayList<String> = arrayListOf(),
    val followUserIds: ArrayList<String> = arrayListOf(),
    val signupTimestamp: Long = 0,
    var updatedTimestamp: Long = 0
)

data class Bleet(
    val bleetId: String? = "",
    val username: String? = "",
    val text: String? = "",
    val textWords: ArrayList<String>? = arrayListOf(),
    val imageUrl: String? = "",
    val hashtags: ArrayList<String>? = arrayListOf(),
    val rebleetUserIds: ArrayList<String>? = arrayListOf(), // list of userIds who re-bleeted this tweet
    val likeUserIds: ArrayList<String>? = arrayListOf(),   // list of userIds who liked this tweet
    val timestamp: Long? = 0,
)