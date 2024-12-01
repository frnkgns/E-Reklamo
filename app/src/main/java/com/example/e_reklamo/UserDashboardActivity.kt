package com.example.e_reklamo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
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

    private lateinit var ProfilePic: ImageView
    private lateinit var postImage: ImageView
    private lateinit var OfficialImage: ImageView

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkYWJxbWFvb2NxaXFqbGJqeW1pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI4NTQyODcsImV4cCI6MjA0ODQzMDI4N30.m0Mi4G4Henu9nt_E4P0TqJVKe_Q1S6ZhC7UkLRWpTsA"
    ) {
        install(Storage)
    }

    private val bucket = supabase.storage.from("images")  // Use the bucket name

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>


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
        val profilUri = sharedPreferences.getString("profileImage", "")
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
        ProfilePic = findViewById(R.id.profilepic)
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
        Glide.with(this)
            .load(profilUri)
            .circleCrop()
            .into(ProfilePic)

        //region Card visibility Condition
        val cardInfo = findViewById<View>(R.id.cardinfo)
        val line = findViewById<View>(R.id.separator)
        cardInfo.visibility = if (accounttype == "admin") { GONE } else { VISIBLE }
        line.visibility = if (accounttype == "admin") { GONE } else { VISIBLE }
        //endregion

        var navBtnClicked = 0
        postContent = findViewById(R.id.post)
        postContent.visibility = if (accounttype == "admin") { GONE } else { VISIBLE }
        postContent.setOnClickListener {
            if(navBtnClicked == 0){
                showPostDialog(accounttype.toString())
            } else if(navBtnClicked == 1){
                AddDataUsingNavbar(1)
            } else if(navBtnClicked == 2){
                AddDataUsingNavbar(2)
            }
        }

        findViewById<TextView>(R.id.nav_home).setOnClickListener {
            navBtnClicked = 0
            Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.nav_officials).setOnClickListener {
            navBtnClicked = 1
            Toast.makeText(this, "Barangay Officials clicked", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.nav_hotlines).setOnClickListener {
            navBtnClicked = 2
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

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if(navBtnClicked == 0){

                }
                val data: Intent? = result.data
                val selectedImageUri = data?.data
                selectedImageUri?.let { uri ->
                    // Show selected image in the ImageView
                    if(navBtnClicked == 0){
                        Glide.with(this).load(uri).into(postImage)
                        postImage.tag = uri // Use tag to store the URI

                    } else if(navBtnClicked == 1 || navBtnClicked == 2){
                        Glide.with(this).load(uri).into(OfficialImage)
                        OfficialImage.tag = uri // Use tag to store the URI
                    }

                    // Optionally, you can store this URI for later upload to Supabase
                }
            }
        }

    }
    fun getPostsFromFirebase(accountType: String) {
        progressBar.visibility = VISIBLE
        val postsList = mutableListOf<Map<String, Any>>()
        val database = FirebaseDatabase.getInstance().reference
        database.keepSynced(true)
        database.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userAccountType = userSnapshot.child("Info").child("accounttype").getValue(String::class.java)

                    if (userAccountType == accountType) {
                        val name = userSnapshot.child("Info").child("name").getValue(String::class.java).orEmpty()
                        val position = userSnapshot.child("Info").child("position").getValue(String::class.java).orEmpty()
                        val profileUri = userSnapshot.child("Info").child("profileImage").getValue(String::class.java).orEmpty()

                        userSnapshot.child("Post").children.forEach { postSnapshot ->
                            val content = postSnapshot.child("content").getValue(String::class.java).orEmpty()
                            val date = postSnapshot.child("date").getValue(String::class.java).orEmpty()
                            val PostKey = postSnapshot.child("postkey").getValue(String::class.java).orEmpty()
                            val KeySecret = postSnapshot.child("keysecret").getValue(String::class.java).orEmpty()

                            // We assume that you are saving the timestamp in milliseconds
                            val timestamp = postSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val ImageUrl = postSnapshot.child("imageUrl").getValue(String::class.java).orEmpty()

                            val postMap = mapOf(
                                "name" to name,
                                "position" to position,
                                "content" to content,
                                "date" to date,
                                "postkey" to PostKey,
                                "keysecret" to KeySecret,
                                "timestamp" to timestamp,
                                "imageUrl" to ImageUrl,
                                "profileImage" to profileUri,
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
    private fun showPostDialog(accountType: String) {
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

        postImage = dialog.findViewById(R.id.postImageView)
        postImage.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
            //open gallery to choose iamge, then preview it to itselft, then upload the image in
            //supabase and save the link to firebase
        }

        postBtn.setOnClickListener {
            val content = contentEdit.text.toString().trim()

            if (content.isNotEmpty()) {
                savePostToFirebase(content, currentDate, postImage, accountType)
                dialog.dismiss() // Close the dialog after saving
            } else {
                contentEdit.error = "Content cannot be empty"
            }
        }

        dialog.show()
    }
    //endregion
    //region Add Hotline
    private fun AddDataUsingNavbar(Type: Int) {
        // Create the Dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.addhotline_or_barangayofficial)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val addofficialbtn: Button = dialog.findViewById(R.id.savedcreatdentails)
        val OfficialName: EditText = dialog.findViewById(R.id.officialname)
        val OfficialPosition: EditText = dialog.findViewById(R.id.officialposition)
        OfficialImage = dialog.findViewById(R.id.officialimage)
        OfficialPosition.hint = if(Type == 1) "Barangay Position" else "Hotline Number"

        OfficialImage.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        addofficialbtn.setOnClickListener {
            val name = OfficialName.text.toString().trim()
            val position = OfficialPosition.text.toString().trim() // Get official position
            val imageUri = OfficialImage.tag.toString()
  
            if (name.isNotEmpty() && position.isNotEmpty()) {
                saveOfficialorHotline(name, position, Type, imageUri) // Pass name and position
                dialog.dismiss()
            } else {
                if (name.isEmpty()) {
                    OfficialName.error = "Name cannot be empty"
                }
                if (position.isEmpty()) {
                    OfficialPosition.error = "Position cannot be empty"
                }
            }
        }

        dialog.show()
    }
    //endregion


    //region Save Official to Database
    private fun saveOfficialorHotline(name: String, position: String, type: Int, imageUri: String) {
        val database = FirebaseDatabase.getInstance().reference
        val officialsRef = if (type == 1) database.child("BarangayOfficials").push() else database.child("Hotlines").push()
        val officialData = mapOf(
            "officialname" to name,
            "officialposition" to position,
            "officialImage" to imageUri
        )

        officialsRef.setValue(officialData)
            .addOnSuccessListener {
                // Data added successfully
                Toast.makeText(this, "Data added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                // Handle errors
                Toast.makeText(this, "Error adding data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    //endregion
    //region Save the posted content to database
    private fun savePostToFirebase(content: String, currentDate: String, postImage: ImageView, accountType: String) {
        progressBar.visibility = VISIBLE
        val username = sharedPreferences.getString("username", "") ?: ""
        if (username.isEmpty()) {
            showToast("Username is missing.")
            return
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference("Users")
        databaseRef.orderByChild("Info/username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userRandomKey = snapshot.children.first().key

                    if (userRandomKey != null) {
                        val postRef = databaseRef.child(userRandomKey).child("Post").push()
                        val postKey = postRef.key

                        // Upload image to Supabase if selected
                        val imageUri = postImage.tag as? Uri
                        imageUri?.let {
                            lifecycleScope.launch {
                                try {
                                    // Ensure progressBar is visible
                                    progressBar.visibility = View.VISIBLE

                                    // Upload to Supabase storage
                                    val fileName = getFileName(it) // Use your existing method to get the file name
                                    val byteArray = contentResolver.openInputStream(it)?.readBytes() ?: throw Exception("File not found")
                                    val uploadResult = bucket.upload(fileName, byteArray)

                                    if (uploadResult.key != null) {
                                        val imageUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co/storage/v1/object/public/images/$fileName"

                                        // Create post data
                                        val postData = mapOf(
                                            "content" to content,
                                            "date" to currentDate,
                                            "timestamp" to ServerValue.TIMESTAMP,
                                            "postkey" to postKey.toString(),
                                            "keysecret" to userRandomKey.toString(),
                                            "imageUrl" to imageUrl // Include the image URL in the post data
                                        )

                                        // Save the post data
                                        postRef.setValue(postData)
                                            .addOnSuccessListener {
                                                // Show success message
                                                showToast("Post saved successfully!")
                                                progressBar.visibility = GONE
                                            }
                                            .addOnFailureListener { error ->
                                                // Show error message
                                                showToast("Error saving post: ${error.message}")
                                                progressBar.visibility = GONE
                                            }
                                    }
                                } catch (e: Exception) {
                                    // Handle error
                                    progressBar.visibility = GONE
                                    showToast("Upload failed: ${e.message}")
                                }
                            }
                        }
                    }
                }

                progressBar.visibility = GONE
                if(accountType == "official"){
                    NewsBtn.performClick()
                } else {
                    ComplainBtn.performClick()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Show error message when data retrieval fails
                showToast("Error retrieving user data: ${error.message}")
            }
        })
    }
    //endregion
    //region Get Image File Name
    private fun getFileName(uri: Uri): String {
        var fileName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            it.moveToFirst()
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            fileName = it.getString(nameIndex)
            cursor.close()
        }
        if (fileName.isEmpty()) {
            fileName = "unknownfile.${getFileExtension(uri)}"
        }
        return fileName
    }
    //endregion
    //region Get Image File Extension
    private fun getFileExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            else -> "jpg"
        }
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