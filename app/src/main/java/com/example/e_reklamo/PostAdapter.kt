package com.example.e_reklamo

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(private val posts: List<Map<String, Any>>,private val accounttype: String) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val accountName: TextView = view.findViewById(R.id.accountname)
        val accountPosition: TextView = view.findViewById(R.id.accountposition)
        val postContent: TextView = view.findViewById(R.id.postcontent)
        val postDate: TextView = view.findViewById(R.id.postdate)
        val deletePost: TextView = view.findViewById(R.id.deletepost)
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

        // Get the timestamp from the post data (saved as long)
        val timestamp = post["timestamp"] as Long
        // Call the getTimeAgo function to get the formatted time
        val timeAgo = getTimeAgo(timestamp)
        // Set the time in the postDate TextView
        holder.postDate.text = timeAgo

        holder.deletePost.visibility = if(accounttype != "user") { View.VISIBLE } else { View.GONE }
        holder.deletePost.setOnClickListener {
            deletePost(post["postkey"].toString(), holder.itemView.context, post["keysecret"].toString()) // Call deletePost method with post id
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
    private fun deletePost(postKey: String, context: Context, userId: String) {
        val database = FirebaseDatabase.getInstance().getReference("Users/$userId/Post/$postKey")
        Log.d("ytr", "$database")

        // Try to find the post by its unique key
        database.removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener() { error ->
                Toast.makeText(context, "Error deleting post: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

}