package com.realityexpander.blitter.util

// ===== Firebase Database =====
const val DATA_USERS_COLLECTION        = "DATA_USERS"     // name of the collection
const val DATA_USERS_EMAIL             = "email"          // must match the object field names exactly
const val DATA_USERS_USERNAME          = "username"
const val DATA_USERS_IMAGE_URL         = "imageUrl"
const val DATA_USERS_FOLLOW_HASHTAGS   = "followHashtags"
const val DATA_USERS_FOLLOW_USER_IDS   = "followUserIds"
const val DATA_USERS_SIGNUP_TIMESTAMP  = "signupTimestamp"
const val DATA_USERS_UPDATED_TIMESTAMP = "updatedTimestamp"

const val DATA_BLEETS_COLLECTION         = "DATA_BLEETS"  // name of the collection
const val DATA_BLEETS_BLEET_ID           = "bleetId"      // must match the object field names exactly
const val DATA_BLEETS_ORIGINAL_BLEET_ID  = "originalBleetId"
const val DATA_BLEETS_USERNAME           = "username"
const val DATA_BLEETS_TEXT               = "text"
const val DATA_BLEETS_TEXT_WORDS         = "textWords"    // for basic text searching in firebase
const val DATA_BLEETS_IMAGE_URL          = "imageUrl"
const val DATA_BLEETS_HASHTAGS           = "hashtags"
const val DATA_BLEETS_REBLEET_USER_IDS   = "rebleetUserIds"
const val DATA_BLEETS_LIKE_USER_IDS      = "likeUserIds"
const val DATA_BLEETS_TIMESTAMP          = "timestamp"


// ===== Firebase Storage =====
const val DATA_PROFILE_IMAGES_STORAGE = "DATA_PROFILE_IMAGES" // storage location for profile images
const val DATA_BLEET_IMAGES_STORAGE = "DATA_BLEET_IMAGES" // storage location for bleet images


// ===== SavedInstanceState ====
const val HOME_ACTIVITY_SELECTED_TAB_POSITION = "homeActivity_selectedTabPosition"
const val SEARCH_FRAGMENT_SHOW_SEARCH_RESULTS = "searchFragment_showSearchResults"
const val SEARCH_FRAGMENT_CURRENT_HASHTAG_QUERY = "searchFragment_currentHashtagQuery"


// Deprecated way of getting image picker
val REQUEST_CODE_PHOTO = 1001