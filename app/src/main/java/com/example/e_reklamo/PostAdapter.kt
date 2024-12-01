package com.example.e_reklamo

import android.app.Dialog
import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(private val posts: List<Map<String, Any>>,private val accounttype: String) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkYWJxbWFvb2NxaXFqbGJqeW1pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI4NTQyODcsImV4cCI6MjA0ODQzMDI4N30.m0Mi4G4Henu9nt_E4P0TqJVKe_Q1S6ZhC7UkLRWpTsA"
    ) {
        install(Storage)
    }

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val accountName: TextView = view.findViewById(R.id.accountname)
        val accountPosition: TextView = view.findViewById(R.id.accountposition)
        val postContent: TextView = view.findViewById(R.id.postcontent)
        val postDate: TextView = view.findViewById(R.id.postdate)
        val deletePost: TextView = view.findViewById(R.id.deletepost)
        val postImageView: ImageView = view.findViewById(R.id.postImage)
        val postProfileImage: ImageView = view.findViewById(R.id.postprofile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.postlayout, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Extract and bind the data to the views
        holder.accountName.text = post["name"] as String
        holder.accountPosition.text = post["position"] as String
        holder.postContent.text = post["content"] as String
        Glide.with(holder.itemView.context)
            .load(post["imageUrl"] as String)
            .fitCenter()
            .into(holder.postImageView)
        holder.postImageView.setOnClickListener {
            showImageDialog(holder.itemView.context, post["imageUrl"] as String)
        }
        Glide.with(holder.itemView.context)
            .load(post["profileImage"] as String)
            .circleCrop()
            .into(holder.postProfileImage)

        // Get the timestamp from the post data (saved as long)
        val timestamp = post["timestamp"] as Long
        // Call the getTimeAgo function to get the formatted time
        val timeAgo = getTimeAgo(timestamp)
        // Set the time in the postDate TextView
        holder.postDate.text = timeAgo

//        holder.deletePost.visibility = if(accounttype == "admin") { View.VISIBLE } else { View.GONE }
        holder.deletePost.setOnClickListener {
            deletePost(post["postkey"].toString(), holder.itemView.context, post["keysecret"].toString(), post["imageUrl"].toString())
            //delete on my supbase using the post[imageUrl]
        }
    }

    override fun getItemCount(): Int = posts.size

    // The getTimeAgo function that formats the timestamp to relative time or date
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffInMillis = now - timestamp

        // Handle posts that are less than a minute old
        if (diffInMillis < DateUtils.MINUTE_IN_MILLIS) {
            return "Just now"
        }

        // If the post is older than 7 days, show the full date
        if (diffInMillis > DateUtils.DAY_IN_MILLIS * 7) {
            val format = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            return format.format(Date(timestamp))
        }

        // For posts within the last 7 days, show relative time like "1 minute ago"
        return DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
    private fun showImageDialog(context: Context, imageUrl: String) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.view_post_image) // Inflate your dialog layout

        val dialogImageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
        Glide.with(context)
            .load(imageUrl)
            .into(dialogImageView)

        dialog.show()
    }

    private fun deletePost(postKey: String, context: Context, userId: String, imageUrl: String) {
        val database = FirebaseDatabase.getInstance().getReference("Users/$userId/Post/$postKey")
        Log.d("ytr", "$database")

        // Delete from Firebase
        database.removeValue()
            .addOnSuccessListener {
                // Delete from Supabase
                val storage = supabase.storage.from("images")
                val path = imageUrl.substringAfterLast("/") // Extract theimage path from the URL

                runBlocking {
                    launch {
                        try {
                            val response = storage.delete(listOf(path)) // Use await() to get the result
                            Toast.makeText(context, "Post and image deleted successfully", Toast.LENGTH_SHORT).show()
                        } catch (error: Exception) {
                            Toast.makeText(context, "Error deleting image from Supabase: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error deleting post: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        notifyDataSetChanged()
    }
}