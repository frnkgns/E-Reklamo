package com.example.e_reklamo

import android.app.Dialog
import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class ComplainGetAdapter(private val posts: List<Map<String, Any>>,
                         private val Image: String, private val name: String,
                         private val key: String, private val newcompaintBtn: TextView) : RecyclerView.Adapter<ComplainGetAdapter.PostViewHolder>() {

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
        val Readcontent: TextView = view.findViewById(R.id.readcontentbtn)
        val Readindicator: ImageView = view.findViewById(R.id.viewcontentindicator)
        val postImageView: ImageView = view.findViewById(R.id.postImage)
        val postProfileImage: ImageView = view.findViewById(R.id.postprofile)
        val showcontent: LinearLayout = view.findViewById(R.id.Contents)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.complainlayout, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        val PostKey = post["postkey"] as String
        val KeySecret = post["keysecret"] as String

        val database = FirebaseDatabase.getInstance().getReference("Users/$KeySecret/Post")
        val database2 = FirebaseDatabase.getInstance().getReference("Users/$KeySecret/Post/$PostKey/Read")
        database2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hasRead = snapshot.child(key).exists() // Check if user has read the post
                holder.Readindicator.visibility = if (hasRead) View.GONE else View.VISIBLE
                if (!hasRead) {
                    newcompaintBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_circle_241, 0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error reading data: ${error.message}") // Log the error
                // Optionally display an error message to the user
            }
        })

        // Extract and bind the data to the views
        holder.accountName.text = post["name"] as String
        holder.accountPosition.text = post["position"] as String
        holder.postContent.text = post["content"] as String
        Glide.with(holder.itemView.context)
            .load(post["imageUrl"] as String)
            .fitCenter()
            .into(holder.postImageView)
        holder.postImageView.setOnClickListener { showImageDialog(holder.itemView.context, post["imageUrl"] as String) }
        Glide.with(holder.itemView.context)
            .load(post["profileImage"] as String)
            .circleCrop()
            .into(holder.postProfileImage)

        val timestamp = post["timestamp"] as Long
        val timeAgo = getTimeAgo(timestamp)
        holder.postDate.text = timeAgo

        var readButtonClicked = false
        holder.Readcontent.setOnClickListener {
            holder.showcontent.visibility =View.VISIBLE
            holder.Readindicator.visibility = View.GONE
            database.child("$PostKey/Read").child(key).child("name").setValue(name)
            database.child("$PostKey/Read").child(key).child("imageUrl").setValue(Image)

            readButtonClicked = !readButtonClicked // Toggle the flag
            if (readButtonClicked) {
                holder.Readcontent.setBackgroundResource(R.drawable.round_keyboard_arrow_up_24)
            } else {
                holder.Readcontent.setBackgroundResource(R.drawable.round_keyboard_arrow_down_24)
                holder.showcontent.visibility = View.GONE
            }
        }
//        checkifalreadyViewed
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
}