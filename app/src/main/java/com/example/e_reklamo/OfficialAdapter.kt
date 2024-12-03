package com.example.e_reklamo

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.common.internal.AccountType
import com.google.firebase.database.FirebaseDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class OfficialAdapter(private val officials: List<Map<String, Any>>, private val supabase: SupabaseClient,private val AccountType: String) : RecyclerView.Adapter<OfficialAdapter.OfficialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfficialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.other_data_layout, parent, false)
        return OfficialViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfficialViewHolder, position: Int) {
        val official = officials[position]
        holder.nameTextView.text = official["name"] as? String ?: ""
        holder.otherTextView.text = official["other"] as? String ?: ""
        Glide.with(holder.itemView.context)
            .load(official["imageUrl"])
            .circleCrop()
            .placeholder(R.drawable.oval)
            .into(holder.otherImage)

        holder.deleteButton.visibility = if(AccountType != "user") { View.VISIBLE } else { View.GONE }
        holder.deleteButton.setOnClickListener {
            val Type = official["type"].toString()
            val context = holder.itemView.context
            val key = official["snapshotKey"].toString()
            val imageUrl = official["imageUrl"].toString()
            deletData(Type, context, key, imageUrl)
        }
    }

    override fun getItemCount(): Int = officials.size

    inner class OfficialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val otherTextView: TextView = itemView.findViewById(R.id.otherTextView)
        val otherImage: ImageView = itemView.findViewById(R.id.otherimage)
        val deleteButton: TextView = itemView.findViewById(R.id.deleteOfficials)
    }

    private fun deletData(Type: String, context: Context, userId: String, imageUrl: String) {
        val database = FirebaseDatabase.getInstance().getReference("$Type/$userId")

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
    }
}