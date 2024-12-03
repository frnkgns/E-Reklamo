package com.example.e_reklamo

data class PostDataClass(
    val accountName: String,
    val accountPosition: String,
    val date: String,
    val content: String,
    val imageUrl: String? = null // Image URL for the post image (optional)
)