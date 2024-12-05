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
import android.text.InputType
import android.view.View
import android.view.View.*
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
    private lateinit var temprecycler: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var ProfilePic: ImageView
    private lateinit var postImage: ImageView
    private lateinit var OfficialImage: ImageView

    private lateinit var purpose: String
    private lateinit var AddOfficial: Button
    private lateinit var AddHotline: Button
    private lateinit var AddBtnsLayout: LinearLayout

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkYWJxbWFvb2NxaXFqbGJqeW1pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI4NTQyODcsImV4cCI6MjA0ODQzMDI4N30.m0Mi4G4Henu9nt_E4P0TqJVKe_Q1S6ZhC7UkLRWpTsA"
    ) {
        install(Storage)
    }

    private val bucket = supabase.storage.from("images")
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
        val userKey = sharedPreferences.getString("userKey", "").toString()
        val name = sharedPreferences.getString("name", "").toString()
        val age = sharedPreferences.getString("age", "").toString()
        val contact = sharedPreferences.getString("contact", "").toString()
        val email = sharedPreferences.getString("email", "").toString()
        val street = sharedPreferences.getString("street", "").toString()
        val barangay = sharedPreferences.getString("barangay", "").toString()
        val position = sharedPreferences.getString("position", "citizen").toString()
        val accounttype = sharedPreferences.getString("accounttype", "user").toString()
        val profilUri = sharedPreferences.getString("profileImage", "").toString()
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
        progressBar = findViewById(R.id.progressbaR)
        ProfilePic = findViewById(R.id.profilepic)
        recycleriew = findViewById(R.id.recyclerView)
        recycleriew.layoutManager = LinearLayoutManager(this)
        temprecycler = findViewById(R.id.temprecyclver)
        temprecycler.layoutManager = LinearLayoutManager(this)
        NewsBtn = findViewById(R.id.newsbtn)
        NewUnderLine = findViewById(R.id.newsunderline)
        ComplainBtn = findViewById(R.id.complainbtn)
        ComplainUnderLine = findViewById(R.id.complainsunderline)
        //endregion
        AddOfficial = findViewById(R.id.addofficialbtn)
        AddHotline = findViewById(R.id.addhotlinebtn)
        AddBtnsLayout = findViewById(R.id.otherBtnLayout)
        AddOfficial.setOnClickListener { AddDataUsingNavbar(1) }
        AddHotline.setOnClickListener { AddDataUsingNavbar(2) }
        NewsBtn.setOnClickListener {
            getAllPostFromFirebase("official", "", "", "", "")
            NewsBtn.setTextColor(android.graphics.Color.WHITE)
            ComplainBtn.setTextColor(android.graphics.Color.parseColor("#7393B3"))
            NewUnderLine.visibility = VISIBLE
            ComplainUnderLine.visibility = INVISIBLE
        }
        ComplainBtn.setOnClickListener {
            if(accounttype != "user"){
                getAllPostFromFirebase("user", profilUri, name, userKey, "")
            } else {
                getMyComplain(userKey)
            }
            ComplainBtn.setTextColor(android.graphics.Color.WHITE)
            NewsBtn.setTextColor(android.graphics.Color.parseColor("#7393B3"))
            NewUnderLine.visibility = INVISIBLE
            ComplainUnderLine.visibility = VISIBLE
        }
        if (accounttype != "user"){
            NewsBtn.performClick()
            getAllPostFromFirebase("user", profilUri, name, userKey, "check")
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

        purpose = ""
        postContent = findViewById(R.id.post)
        postContent.visibility = if(accounttype == "admin") GONE else VISIBLE
        postContent.setOnClickListener {
            showPostDialog(accounttype, userKey)
        }

        findViewById<TextView>(R.id.nav_home).setOnClickListener {
            AddBtnsLayout.visibility = GONE
            NewsBtn.visibility = VISIBLE
            ComplainBtn.visibility = VISIBLE
            NewsBtn.performClick()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.nav_officials).setOnClickListener {
            AddBtnsLayout.visibility = VISIBLE
            AddOfficial.visibility = VISIBLE
            AddHotline.visibility = GONE
            NewsBtn.visibility = GONE
            ComplainBtn.visibility = GONE
            NewUnderLine.visibility = GONE
            ComplainUnderLine.visibility = GONE
            getOtherData("BarangayOfficials", accounttype)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.nav_hotlines).setOnClickListener {
            AddBtnsLayout.visibility = VISIBLE
            AddOfficial.visibility = GONE
            AddHotline.visibility = VISIBLE
            NewsBtn.visibility = GONE
            NewUnderLine.visibility = GONE
            ComplainUnderLine.visibility = GONE
            ComplainBtn.visibility = GONE
            getOtherData("Hotlines", accounttype)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        //region Logout
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
        //endregion
        //region Image Picker Using File Manager
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri = data?.data
                selectedImageUri?.let { uri ->
                    // Show selected image in the ImageView
                    if(purpose == "postContent"){
                        Glide.with(this).load(uri).circleCrop().into(postImage)
                        postImage.setPadding(0,0,0,0)
                        postImage.tag = uri // Use tag to store the URI

                    } else {
                        Glide.with(this).load(uri).circleCrop().into(OfficialImage)
                        OfficialImage.setPadding(0,0,0,0)
                        OfficialImage.tag = uri // Use tag to store the URI
                    }
                }
            }
        }
        //endregion
    }
    //region Retrieved Post from Database
    fun getAllPostFromFirebase(accountType: String, Image: String, name: String, key: String, Purpose: String) {
        progressBar.visibility = VISIBLE
        val postsList = mutableListOf<Map<String, Any>>()
        val temppostsList = mutableListOf<Map<String, Any>>()
        val database = FirebaseDatabase.getInstance().reference
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
                            if(Purpose == "check"){
                                temppostsList.add(postMap)
                            } else {
                                postsList.add(postMap)
                            }
                        }
                    }
                    progressBar.visibility = GONE
                }


                if(Purpose == "check"){
                    temppostsList.sortByDescending { it["timestamp"] as Long }
                    val adapter = ComplainGetAdapter(temppostsList, Image, name, key, ComplainBtn)
                    temprecycler.adapter = adapter
                } else {
                    postsList.sortByDescending { it["timestamp"] as Long }
                    if(accountType == "user"){
                        val adapter = ComplainGetAdapter(postsList, Image, name, key, ComplainBtn)
                        recycleriew.adapter = adapter
                    } else if(accountType == "official"){
                        val adapter = PostGetAdapter(postsList)
                        recycleriew.adapter = adapter
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
        database.keepSynced(true)
    }
    //endregion
    //region Get My Complain
    fun getMyComplain(userKey: String) {
        progressBar.visibility = VISIBLE
        val postsList = mutableListOf<Map<String, Any>>()
        val database = FirebaseDatabase.getInstance().getReference("Users/$userKey")
        database.keepSynced(true)
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val snapshotPost = snapshot.child("Post")
                if(snapshotPost.exists()){
                    val name = snapshot.child("Info/name").getValue(String::class.java).toString()
                    val position = snapshot.child("Info/position").getValue(String::class.java).toString()
                    val profileUri = snapshot.child("Info/profileImage").getValue(String::class.java).toString()

                    for (postSnapshot in snapshotPost.children) {
                        val content = postSnapshot.child("content").getValue(String::class.java).toString()
                        val date = postSnapshot.child("date").getValue(String::class.java).toString()
                        val PostKey = postSnapshot.child("postkey").getValue(String::class.java).orEmpty()
                        val KeySecret = postSnapshot.child("keysecret").getValue(String::class.java).orEmpty()
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

                        // Sort posts by timestamp in descending order (latest posts first)
                        postsList.sortByDescending { it["timestamp"] as Long }
                        val adapter = PostGetAdapter(postsList)
                        recycleriew.adapter = adapter
                    }
                }

                progressBar.visibility = GONE
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
    //endregion
    //region Retrieved Barangay Officials from Database
    fun getOtherData(type: String, AccountType: String) {
        progressBar.visibility = VISIBLE
        val dataList = mutableListOf<Map<String, Any>>() // Use List<Map<String, Any>>
        val database = FirebaseDatabase.getInstance().reference
        database.keepSynced(true)

        database.child(type).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataList.clear()

                for (snapshot in dataSnapshot.children) {
                    val data = snapshot.value as? Map<String, Any>
                    val key = snapshot.key.toString()
                    if (data != null) {
                        val mutableData = data.toMutableMap()
                        mutableData["snapshotKey"] = key
                        mutableData["type"] = type

                        dataList.add(mutableData)
                    }
                }

                // Update RecyclerView with dataList
                recycleriew.adapter = OfficialAdapter(dataList, supabase, AccountType)
                progressBar.visibility = GONE
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = GONE
                Toast.makeText(this@UserDashboardActivity, "Error retrieving data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    //endregion
    //region Post Dialog
    private fun showPostDialog(accountType: String, key: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.postcontent)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        val dateText: TextView = dialog.findViewById(R.id.dateTextView)
        dateText.text = currentDate

        val postBtn: Button = dialog.findViewById(R.id.postButton)
        val contentEdit: EditText = dialog.findViewById(R.id.postEditText)

        postImage = dialog.findViewById(R.id.postImageView)
        postImage.setOnClickListener{
            purpose = "postContent"
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        postBtn.setOnClickListener {
            val content = contentEdit.text.toString().trim()
            if (content.isNotEmpty()) {
                savePostToFirebase(content, currentDate, postImage, key, accountType)
                dialog.dismiss()
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
        OfficialPosition.inputType = if(Type == 1) { InputType.TYPE_CLASS_TEXT } else { InputType.TYPE_CLASS_NUMBER}

        OfficialImage.setOnClickListener{
            purpose = "OfficialImage"
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        addofficialbtn.setOnClickListener {
            val name = OfficialName.text.toString().trim()
            val position = OfficialPosition.text.toString().trim() // Get official position
            val imageUri = OfficialImage.tag as? Uri
  
            if (name.isNotEmpty() && position.isNotEmpty()) {
                saveOfficialorHotline(name, position, Type, imageUri!!) // Pass name and position
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
    private fun saveOfficialorHotline(name: String, position: String, type: Int, imageUri: Uri) {
        val database = FirebaseDatabase.getInstance().reference
        val officialsRef = if (type == 1) database.child("BarangayOfficials").push() else database.child("Hotlines").push()
        imageUri.let {
            lifecycleScope.launch {
                try {
                    // Ensure progressBar is visible
                    progressBar.visibility = VISIBLE

                    // Upload to Supabase storage
                    val fileName = getFileName(it) // Use your existing method to get the file name
                    val byteArray = contentResolver.openInputStream(it)?.readBytes() ?: throw Exception("File not found")
                    val uploadResult = bucket.upload(fileName, byteArray)

                    if (uploadResult.key != null) {
                        val imageUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co/storage/v1/object/public/images/$fileName"

                        // Create post data
                        val postData = mapOf(
                            "name" to name,
                            "other" to position,
                            "imageUrl" to imageUrl
                        )

                        // Save the post data
                        officialsRef.setValue(postData)
                            .addOnSuccessListener {
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
                    progressBar.visibility = GONE
                    showToast("Upload failed: ${e.message}")
                }
            }
        }
    }
    //endregion
    //region Save the posted content to database
    private fun savePostToFirebase(content: String, currentDate: String, postImage: ImageView, userKey: String, AccountType: String) {
        progressBar.visibility = VISIBLE
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users/$userKey")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (userKey != null) {
                    val postRef = databaseRef.child("Post").push()
                    val postKey = postRef.key

                    // Upload image to Supabase if selected
                    val imageUri = postImage.tag as? Uri
                    if(imageUri == null){
                        // Create post data
                        val postData = mapOf(
                            "content" to content,
                            "date" to currentDate,
                            "timestamp" to ServerValue.TIMESTAMP,
                            "postkey" to postKey.toString(),
                            "keysecret" to userKey
                        )

                        // Save the post data
                        postRef.setValue(postData)
                            .addOnSuccessListener {
                                // Show success message
                                showToast("Post saved successfully!")
                                progressBar.visibility = GONE
                                if(AccountType == "user"){
                                    ComplainBtn.performClick()
                                } else {
                                    NewsBtn.performClick()
                                }
                            }
                            .addOnFailureListener { error ->
                                // Show error message
                                showToast("Error saving post: ${error.message}")
                                progressBar.visibility = GONE
                            }
                    }
                    imageUri?.let {
                        lifecycleScope.launch {
                            try {

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
                                        "keysecret" to userKey.toString(),
                                        "imageUrl" to imageUrl // Include the image URL in the post data
                                    )

                                    // Save the post data
                                    postRef.setValue(postData)
                                        .addOnSuccessListener {
                                            // Show success message
                                            showToast("Post saved successfully!")
                                            progressBar.visibility = GONE
                                            if(AccountType == "user"){
                                                ComplainBtn.performClick()
                                            } else {
                                                NewsBtn.performClick()
                                            }
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
                    //endregion
                }
                progressBar.visibility = GONE
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Error retrieving user data: ${error.message}")
            }
        })
    }    //endregion
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