package com.realityexpander.blitter.util

// ===== Firebase Database =====
const val DATA_USERS_COLLECTION = "DATA_USERS" // name of the collection
const val DATA_USERS_EMAIL = "email"
const val DATA_USERS_USERNAME = "username"
const val DATA_USERS_IMAGE_URL = "imageUrl"
const val DATA_USERS_FOLLOW_HASHTAGS = "followHashtags"
const val DATA_USERS_FOLLOW_USERS = "followUsers"
const val DATA_USERS_SIGNUP_TIMESTAMP = "SignupTimestamp"
const val DATA_USERS_UPDATED_TIMESTAMP = "updatedTimestamp"

const val DATA_BLEETS_COLLECTION = "DATA_BLEETS"
const val DATA_BLEETS_TWEETID = "bleetId"
const val DATA_BLEETS_USERNAME = "userName"
const val DATA_BLEETS_TEXT = "text"
const val DATA_BLEETS_IMAGEURL = "imageUrl"
const val DATA_BLEETS_HASHTAGS = "hashTags"
const val DATA_BLEETS_USERIDS = "userIds"
const val DATA_BLEETS_LIKES = "likes"
const val DATA_BLEETS_TIMESTAMP = "timeStamp"


// ===== Firebase Storage =====
const val DATA_PROFILE_IMAGES_STORAGE = "DATA_PROFILE_IMAGES" // storage location for profile images
const val DATA_BLEET_IMAGES_STORAGE = "DATA_BLEET_IMAGES" // storage location for bleet images


// Old way of doing image picker
val REQUEST_CODE_PHOTO = 1001