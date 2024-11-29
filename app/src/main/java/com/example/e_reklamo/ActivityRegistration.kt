package com.example.e_reklamo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase
import java.io.File

class ActivityRegistration : AppCompatActivity() {

    private lateinit var uploadButton: Button
    private lateinit var uploadedPhoto: ImageView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("MissingInflatedId", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        makeFullscreen()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val account = intent.getStringExtra("accounttype").toString()
        val database = FirebaseDatabase.getInstance().reference
        //region Id Connection
        uploadedPhoto = findViewById(R.id.uploadedPhoto)
        uploadButton = findViewById(R.id.uploadButton)
        progressBar = findViewById(R.id.progressBar)
        val username = findViewById<EditText>(R.id.regusename)
        val password = findViewById<EditText>(R.id.regpassword)
        val name = findViewById<EditText>(R.id.regname)
        val position = findViewById<EditText>(R.id.resgpos)
        val age = findViewById<EditText>(R.id.regage)
        val contact = findViewById<EditText>(R.id.regcontact)
        val email = findViewById<EditText>(R.id.regemail)
        val street = findViewById<EditText>(R.id.regstreet)
        val barangay = findViewById<EditText>(R.id.regbarangay)
        //endregion

        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val saveButton = findViewById<Button>(R.id.saveaccount)
        position.visibility = if(account == "admin") VISIBLE else GONE
        saveButton.setOnClickListener {
            val accountType: String
            val pOsition: String

            if (account.isNotEmpty() && account == "admin") {
                pOsition = position.text.toString().takeIf { it.isNotEmpty() } ?: "default_position" // Use "default_position" if empty
                accountType = "official"
            } else {
                pOsition = "citizen"
                accountType = "user"
            }

            val randomKey = database.child("Users").push().key // Generate a random key
            if (randomKey != null) {
                val userInfo = mapOf(
                    "username" to username.text.toString(),
                    "password" to password.text.toString(),
                    "name" to name.text.toString(),
                    "age" to age.text.toString(),
                    "contact" to contact.text.toString(),
                    "email" to email.text.toString(),
                    "street" to street.text.toString(),
                    "barangay" to barangay.text.toString(),
                    "position" to pOsition,
                    "accounttype" to accountType
                )

                if(account != "admin"){
                    for ((key, value) in userInfo) {
                        editor.putString(key, value)
                    }
                    editor.apply()
                }
                // Save user information in Firebase
                database.child("Users").child(randomKey).child("Info").setValue(userInfo)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Account Registered!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                        } else {
                            Toast.makeText(this, "Failed to Register", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
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
