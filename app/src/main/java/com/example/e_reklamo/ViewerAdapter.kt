package com.example.e_reklamo

import android.view.LayoutInflater
import com.example.e_reklamo.R
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ViewerAdapter(private val viewers: List<Map<String, Any>>) : RecyclerView.Adapter<ViewerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViews) // Assuming your ImageView has the ID "imageView"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_profile_layout, parent, false) // Replace with your layout file
        return ViewHolder(view) // Create and return a ViewHolder instance
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewer = viewers[position]
        val imageUrl = viewer["viewers"] as String // Assuming "viewers" key holds the image URL

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .circleCrop()
            .placeholder(R.drawable.oval)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return viewers.size
    }
}