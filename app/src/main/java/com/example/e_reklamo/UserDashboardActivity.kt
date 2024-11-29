package com.example.e_reklamo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.*
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class UserDashboardActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var postContent: ImageView

    private lateinit var nametxt: TextView
    private lateinit var positiontxt: TextView
    private lateinit var agetxt: TextView
    private lateinit var phonetxt: TextView
    private lateinit var emailtext: TextView
    private lateinit var addresstxt: TextView

    private lateinit var NewsBtn: TextView
    private lateinit var NewUnderLine: View
    private lateinit var ComplainBtn: TextView
    private lateinit var ComplainUnderLine: View

    private lateinit var recycleriew: RecyclerView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        makeFullscreen()
        //region Retrieve Shared Preference
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val name = sharedPreferences.getString("name", "")
        val age = sharedPreferences.getString("age", "")
        val contact = sharedPreferences.getString("contact", "")
        val email = sharedPreferences.getString("email", "")
        val street = sharedPreferences.getString("street", "")
        val barangay = sharedPreferences.getString("barangay", "")
        val position = sharedPreferences.getString("position", "citizen")
        val accounttype = sharedPreferences.getString("accounttype", "user")
        //endregion
        //region Navbar Behavior
        val navbar = findViewById<ImageView>(R.id.navbar)
        navbar.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        //endregion
        //region ID Connection
        drawerLayout = findViewById(R.id.main)
        nametxt = findViewById(R.id.nametxtview)
        positiontxt = findViewById(R.id.positiontxtview)
        agetxt = findViewById(R.id.agetxtview)
        phonetxt = findViewById(R.id.contacttxtview)
        emailtext = findViewById(R.id.emailtxtview)
        addresstxt = findViewById(R.id.addresstxtview)
        //endregion
        progressBar = findViewById(R.id.progressbaR)
        recycleriew = findViewById(R.id.recyclerView)
        recycleriew.layoutManager = LinearLayoutManager(this)
        NewsBtn = findViewById(R.id.newsbtn)
        NewUnderLine = findViewById(R.id.newsunderline)
        ComplainBtn = findViewById(R.id.complainbtn)
        ComplainUnderLine = findViewById(R.id.complainsunderline)
        NewsBtn.setOnClickListener {
            getPostsFromFirebase("official")
            NewsBtn.setTextColor(android.graphics.Color.WHITE)
            ComplainBtn.setTextColor(android.graphics.Color.parseColor("#7393B3"))
            NewUnderLine.visibility = VISIBLE
            ComplainUnderLine.visibility = GONE
        }
        ComplainBtn.setOnClickListener {
            getPostsFromFirebase("user")
            ComplainBtn.setTextColor(android.graphics.Color.WHITE)
            NewsBtn.setTextColor(android.graphics.Color.parseColor("#7393B3"))
            NewUnderLine.visibility = GONE
            ComplainUnderLine.visibility = VISIBLE
        }
        if(accounttype == "official"){
            ComplainBtn.performClick()
        } else {
            NewsBtn.performClick()
        }

        nametxt.text = name
        positiontxt.text = position
        agetxt.text = age
        phonetxt.text = contact
        emailtext.text = email
        addresstxt.text = "$street, $barangay"

        //region Card visibility Condition
        val cardInfo = findViewById<View>(R.id.cardinfo)
        val line = findViewById<View>(R.id.separator)
        cardInfo.visibility = if (accounttype == "admin") { GONE } else { VISIBLE }
        line.visibility = if (accounttype == "admin") { GONE } else { VISIBLE }
        //endregion

        postContent = findViewById(R.id.post)
        postContent.visibility = if (accounttype == "admin") { GONE } else { VISIBLE }
        postContent.setOnClickListener { showPostDialog() }

        findViewById<TextView>(R.id.nav_home).setOnClickListener {
            Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.nav_officials).setOnClickListener {
            Toast.makeText(this, "Barangay Officials clicked", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.nav_hotlines).setOnClickListener {
            Toast.makeText(this, "Hotlines clicked", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.nav_logout).setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.clear()  // Clears all saved preferences
            editor.apply()  // Apply the changes

            // Optionally, you can also navigate to the login screen after logout
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Optional: finish the current activity so the user cannot go back to it
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
    fun getPostsFromFirebase(accountType: String) {
        progressBar.visibility = VISIBLE
        val postsList = mutableListOf<Map<String, Any>>()
        val database = FirebaseDatabase.getInstance().reference

        // Get all users
        database.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userAccountType = userSnapshot.child("Info").child("accounttype").getValue(String::class.java)

                    if (userAccountType == accountType) {
                        val name = userSnapshot.child("Info").child("name").getValue(String::class.java).orEmpty()
                        val position = userSnapshot.child("Info").child("position").getValue(String::class.java).orEmpty()

                        userSnapshot.child("Post").children.forEach { postSnapshot ->
                            val content = postSnapshot.child("content").getValue(String::class.java).orEmpty()
                            val date = postSnapshot.child("date").getValue(String::class.java).orEmpty()
                            val PostKey = postSnapshot.child("postkey").getValue(String::class.java).orEmpty()
                            val KeySecret = postSnapshot.child("keysecret").getValue(String::class.java).orEmpty()

                            // We assume that you are saving the timestamp in milliseconds
                            val timestamp = postSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                            val postMap = mapOf(
                                "name" to name,
                                "position" to position,
                                "content" to content,
                                "date" to date,
                                "postkey" to PostKey,
                                "keysecret" to KeySecret,
                                "timestamp" to timestamp
                            )

                            postsList.add(postMap)
                        }
                    }

                    progressBar.visibility = GONE
                }

                // Sort posts by timestamp in descending order (latest posts first)
                postsList.sortByDescending { it["timestamp"] as Long }

                // Set the posts to RecyclerView Adapter
                val adapter = PostAdapter(postsList, accountType)
                recycleriew.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
        database.keepSynced(true)
    }

    //region Post Dialog
    private fun showPostDialog() {
        // Create the Dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.postcontent)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Get current date
        val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())

        // Set the date in the dialog
        val dateText: TextView = dialog.findViewById(R.id.dateTextView)
        dateText.text = currentDate

        // Handle the POST button click
        val postBtn: Button = dialog.findViewById(R.id.postButton)
        val contentEdit: EditText = dialog.findViewById(R.id.postEditText)

        postBtn.setOnClickListener {
            val content = contentEdit.text.toString().trim()

            if (content.isNotEmpty()) {
                savePostToFirebase(content, currentDate)
                dialog.dismiss() // Close the dialog after saving
            } else {
                contentEdit.error = "Content cannot be empty"
            }
        }

        dialog.show()
    }
    //endregion
    //region Save the posted content to database
    private fun savePostToFirebase(content: String, currentDate: String) {
        val username = sharedPreferences.getString("username", "") ?: ""

        // Validate if the username is empty
        if (username.isEmpty()) {
            showToast("Username is missing.")
            return
        }

        // Get Firebase database reference for the Users node
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users")
        val timestamp = System.currentTimeMillis()

        // Search for the user by username in the "Info" node
        databaseRef.orderByChild("Info/username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userRandomKey = snapshot.children.first().key

                    if (userRandomKey != null) {
                        // Generate the unique post key using push()
                        val postRef = databaseRef.child(userRandomKey).child("Post").push()
                        val postKey = postRef.key // Save the post key

                        // Create post data to save
                        val postData = mapOf(
                            "content" to content,
                            "date" to currentDate,
                            "timestamp" to ServerValue.TIMESTAMP,
                            "postkey" to postKey.toString(),
                            "keysecret" to userRandomKey.toString()
                        )

                        // Save the post data under the "Post" node for the specific user
                        postRef.setValue(postData)
                            .addOnSuccessListener {
                                showToast("Post saved successfully!")
                            }
                            .addOnFailureListener { error ->
                                showToast("Error saving post: ${error.message}")
                            }

                    } else {
                        showToast("User not found.")
                    }
                } else {
                    showToast("No user found with the username: $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Error retrieving user data: ${error.message}")
            }
        })

        NewsBtn.performClick()
    }
    //endregion
    //region Show a message after completing the post
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    //endregion
    //region Full Screen
    fun makeFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()) // Hides the status bar
                controller.hide(WindowInsets.Type.navigationBars()) // Hides the navigation bar
            }
        } else {
            // For older versions of Android (below API 30)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
    //endregion
}