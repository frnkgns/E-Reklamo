package com.example.e_reklamo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class ActivityRegistration : AppCompatActivity() {

    private lateinit var uploadedPhoto: ImageView
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null

    // Initialize Supabase client and storage bucket
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkYWJxbWFvb2NxaXFqbGJqeW1pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI4NTQyODcsImV4cCI6MjA0ODQzMDI4N30.m0Mi4G4Henu9nt_E4P0TqJVKe_Q1S6ZhC7UkLRWpTsA"
    ) {
        install(Storage)
    }

    private val bucket = supabase.storage.from("images")  // Use the bucket name

    // Registering activity result launcher for image picking
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(uploadedPhoto)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                uploadedPhoto.setImageBitmap(bitmap) // Display the selected image in ImageView
                uploadedPhoto.setPadding(0,0,0,0)
            }
        }
    }

    @SuppressLint("MissingInflatedId", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        uploadedPhoto = findViewById(R.id.uploadedPhoto)
        progressBar = findViewById(R.id.progressBar)

        val username = findViewById<EditText>(R.id.regusename)
        val password = findViewById<EditText>(R.id.regpassword)
        val password2 = findViewById<EditText>(R.id.regpassword2)
        val name = findViewById<EditText>(R.id.regname)
        val position = findViewById<EditText>(R.id.resgpos)
        val age = findViewById<EditText>(R.id.regage)
        val contact = findViewById<EditText>(R.id.regcontact)
        val email = findViewById<EditText>(R.id.regemail)
        val street = findViewById<EditText>(R.id.regstreet)
        val barangay = findViewById<EditText>(R.id.regbarangay)
        val saveButton = findViewById<Button>(R.id.saveaccount)
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val accountType = intent.getStringExtra("accounttype").toString()
        position.visibility = if (accountType == "admin") View.VISIBLE else View.GONE

        // Set up the image picker when the upload button is clicked
        uploadedPhoto.setOnClickListener {
            openImagePicker()
        }

        saveButton.setOnClickListener {
            val accountType = if (intent.getStringExtra("accounttype") == "admin") "official" else "user"
            val usernameText = username.text.toString()
            val emailTxt = email.text.toString()
            val p0sition = if (intent.getStringExtra("accounttype") == "admin") position.text.toString() else "citizen"

            //region Checking if input field is empty and valid
            if(username.text.toString().isNotEmpty() && password.text.toString().isNotEmpty()
                && name.text.toString().isNotEmpty() && age.text.toString().isNotEmpty()
                && contact.text.toString().isNotEmpty() && email.text.toString().isNotEmpty()
                && street.text.toString().isNotEmpty() && barangay.text.toString().isNotEmpty()
                && selectedImageUri != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()){

                if (password.text.toString() == password2.text.toString()) {
                    // Add code to check if username is taken
                    val database = FirebaseDatabase.getInstance().reference
                    database.child("Users").addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var usernameTaken = false

                            for (userSnapshot in snapshot.children) {
                                val existingUsername = userSnapshot.child("Info").child("username").getValue(String::class.java)
                                if (existingUsername == username.text.toString()) {
                                    usernameTaken = true
                                    break
                                }
                            }

                            if (usernameTaken) {
                                Toast.makeText(this@ActivityRegistration, "Username is already taken!", Toast.LENGTH_SHORT).show()
                            } else {
                                if (selectedImageUri == null) {
                                    Toast.makeText(this@ActivityRegistration, "Please select an image first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    lifecycleScope.launch {
                                        try {
                                            progressBar.visibility = View.VISIBLE

                                            // Get the file's input stream and convert it to a byte array
                                            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                                            val byteArray = inputStream?.readBytes() ?: throw Exception("File not found")

                                            // Get the file name (including the correct extension)
                                            val fileName = getFileName(selectedImageUri!!)

                                            // Upload the file to Supabase storage
                                            val uploadResult = bucket.upload(fileName, byteArray)

                                            // Check if the upload was successful
                                            if (uploadResult.key != null) {
                                                // File uploaded successfully
                                                val imageUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co/storage/v1/object/public/images/$fileName"

                                                // Save data in Firebase database
                                                val userInfo = mapOf(
                                                    "username" to usernameText,
                                                    "password" to password.text.toString(),
                                                    "name" to name.text.toString(),
                                                    "age" to age.text.toString(),
                                                    "contact" to contact.text.toString(),
                                                    "email" to emailTxt,
                                                    "street" to street.text.toString(),
                                                    "barangay" to barangay.text.toString(),
                                                    "position" to p0sition,
                                                    "accounttype" to accountType,
                                                    "profileImage" to imageUrl
                                                )

                                                if (accountType == "user") {
                                                    val editor = sharedPreferences.edit()
                                                    for (key in userInfo.keys) {
                                                        editor.putString(key, userInfo[key])
                                                    }
                                                    editor.apply()
                                                }

                                                val randomKey = database.child("Users").push().key
                                                randomKey?.let {
                                                    database.child("Users").child(it).child("Info").setValue(userInfo)
                                                        .addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                Toast.makeText(this@ActivityRegistration, "Account Registered!", Toast.LENGTH_SHORT).show()
                                                                startActivity(Intent(this@ActivityRegistration, MainActivity::class.java))
                                                            } else {
                                                                Toast.makeText(this@ActivityRegistration, "Failed to Register", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    database.child("Users").child(it).child("PostStatistic").child("postcount").setValue(0)
                                                    database.child("Users").child(it).child("PostStatistic").child("postdate").setValue("")
                                                }

                                                // Hide progress bar and show success message
                                                runOnUiThread {
                                                    progressBar.visibility = View.GONE
                                                    Toast.makeText(this@ActivityRegistration, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                                                }

                                            } else {
                                                throw Exception("Upload failed: No key returned")
                                            }
                                        } catch (e: Exception) {
                                            // Handle errors during upload
                                            runOnUiThread {
                                                progressBar.visibility = View.GONE
                                                Toast.makeText(this@ActivityRegistration, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@ActivityRegistration,
                                "Error checking username: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
                }

            }
            else{
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTxt).matches()) {
                    email.error = "Used gmail address"
                }
                else if(emailTxt.isEmpty()){
                    email.error = "Email is required"
                }
                if(usernameText.isEmpty()){
                    username.error = "Username is required"
                }
                if(password.text.toString().isEmpty()) {
                    password.error = "Password is required"
                }
                if(name.text.toString().isEmpty()){
                    name.error = "Name is required"
                }
                if(age.text.toString().isEmpty()){
                    age.error = "Age is required"
                }
                if(contact.text.toString().isEmpty()){
                    contact.error = "Contact is required"
                }
                if(street.text.toString().isEmpty()) {
                    street.error = "Street is required"
                }
                if(barangay.text.toString().isEmpty()){
                    barangay.error
                }
                if(selectedImageUri == null){
                    Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show()
                }
            }
            //endregion
        }
    }

    // Open image picker to select an image
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"  // You can specify other types like "image/png" or "image/jpeg"
        imagePickerLauncher.launch(intent)
    }

    // Helper function to get the real file name, including the extension
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

    // Helper function to get the file's extension based on MIME type
    private fun getFileExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            else -> "jpg"
        }
    }
}
