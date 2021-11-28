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

data class Bleet(
    val bleetId: String? = "",
    val userName: String? = "",
    val text: String? = "",
    val imageUrl: String? = "",
    val hashTags: ArrayList<String>? = arrayListOf(),
    val userIds: ArrayList<String>? = arrayListOf(), // list of people who re-tweeted this tweet
    val likes: ArrayList<String>? = arrayListOf(),
    val timeStamp: Long? = 0,
)