package com.example.e_reklamo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class ComplainGetAdapter(
    private val posts: List<Map<String, Any>>,
    private val accountType: String,
    private val newcompaintBtn: TextView
) : RecyclerView.Adapter<ComplainGetAdapter.PostViewHolder>() {

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val accountName: TextView = view.findViewById(R.id.accountname)
        val accountPosition: TextView = view.findViewById(R.id.accountposition)
        val postContent: TextView = view.findViewById(R.id.postcontent)
        val postDate: TextView = view.findViewById(R.id.postdate)
        val Readcontent: TextView = view.findViewById(R.id.readcontentbtn)
        val Readindicator: ImageView = view.findViewById(R.id.viewcontentindicator)
        val postImageView: ImageView = view.findViewById(R.id.postImage)
        val showcontent: LinearLayout = view.findViewById(R.id.Contents)
        val StatusSpinner: Spinner = view.findViewById(R.id.complaintstatus)
        val SpinnerLayout: LinearLayout = view.findViewById(R.id.spinnerLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.complainlayout, parent, false)
        return PostViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val postKey = post["postkey"] as? String ?: ""
        val keySecret = post["keysecret"] as? String ?: ""
        val imageUrl = post["imageUrl"] as? String ?: ""

        holder.accountName.text = "User${(keySecret).take(5)}"

        if (accountType == "user") {
            holder.Readcontent.visibility = GONE
            holder.SpinnerLayout.visibility = GONE
            holder.showcontent.visibility = VISIBLE
        }

        val database2 = FirebaseDatabase.getInstance().getReference("Users/$keySecret/Post/$postKey/read")

        // Handle read status
        database2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hasRead = snapshot.getValue(String::class.java) ?: ""
                holder.Readindicator.visibility = if (hasRead == "yes") GONE else VISIBLE
                if (hasRead == "yes") {
                    newcompaintBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0) // This removes the drawable
                } else {
                    newcompaintBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_circle_241, 0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error reading data: ${error.message}")
            }
        })

        holder.accountPosition.text = post["position"] as? String ?: "Unknown"
        holder.postContent.text = post["content"] as? String ?: "No Content"

        if (imageUrl.isEmpty()) {
            holder.postImageView.visibility = GONE
        } else {
            holder.postImageView.visibility = VISIBLE
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .fitCenter()
                .into(holder.postImageView)
        }

        holder.postImageView.setOnClickListener {
            showImageDialog(holder.itemView.context, imageUrl)
        }

        val timestamp = post["timestamp"] as? Long ?: 0L
        holder.postDate.text = getTimeAgo(timestamp)

        var readButtonClicked = false
        holder.Readcontent.setOnClickListener {
            readButtonClicked = !readButtonClicked
            holder.showcontent.visibility = if (readButtonClicked) VISIBLE else GONE
            holder.Readindicator.visibility = GONE
            holder.Readcontent.setBackgroundResource(
                if (readButtonClicked) R.drawable.round_keyboard_arrow_up_24 else R.drawable.round_keyboard_arrow_down_24
            )
            database2.setValue("yes")
        }

        // Spinner setup
        val spinnerStatusList = listOf("Pending", "Processing", "Completed")
        val currentStatus = post["status"] as? String ?: "Pending"
        setupStatusSpinner(holder.itemView.context, holder.StatusSpinner, spinnerStatusList, currentStatus, postKey, keySecret)
    }
    private fun setupStatusSpinner(
        context: Context,
        spinner: Spinner,
        statusList: List<String>,
        initialStatus: String,
        postKey: String,
        keySecret: String
    ) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, statusList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter

        val initialIndex = statusList.indexOf(initialStatus).takeIf { it >= 0 } ?: 0
        spinner.setSelection(initialIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, selectedPosition: Int, id: Long) {
                val selectedStatus = statusList[selectedPosition]
                if (selectedStatus != initialStatus) {
                    val database = FirebaseDatabase.getInstance()
                        .getReference("Users/$keySecret/Post/$postKey/status")
                    database.setValue(selectedStatus)
                        .addOnSuccessListener {
                            Log.d("SpinnerUpdate", "Updated status for postKey: $postKey to $selectedStatus")
                        }
                        .addOnFailureListener { exception ->
                            Log.e("SpinnerUpdateError", "Failed to update status: ${exception.message}")
                        }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    override fun getItemCount(): Int = posts.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffInMillis = now - timestamp

        return when {
            diffInMillis < DateUtils.MINUTE_IN_MILLIS -> "Just now"
            diffInMillis > DateUtils.DAY_IN_MILLIS * 7 -> SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(timestamp))
            else -> DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS).toString()
        }
    }

    private fun showImageDialog(context: Context, imageUrl: String) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.view_post_image)

        val dialogImageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
        Glide.with(context)
            .load(imageUrl)
            .into(dialogImageView)
        dialog.show()
    }
}
