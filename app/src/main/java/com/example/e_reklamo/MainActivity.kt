package com.example.e_reklamo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var progressbar: ProgressBar
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        makeFullscreen()

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        val loginButton = findViewById<TextView>(R.id.loginbutton)
        val signup = findViewById<TextView>(R.id.signupLink)
        val forgotpassword = findViewById<TextView>(R.id.forgotPassword)
        progressbar = findViewById(R.id.progressBar)

        val savedUsername = sharedPreferences.getString("username", "").toString()
        val savedPassword = sharedPreferences.getString("password", "").toString()

        if (savedUsername.isNotEmpty() && savedPassword.isNotEmpty()) {
            handleLogin(savedUsername, savedPassword)
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            handleLogin(username, password)
        }

        signup.setOnClickListener {
            val intent = Intent(this@MainActivity, ActivityRegistration::class.java).apply {
                putExtra("accounttype", "user")
            }
            startActivity(intent)
        }

//        forgotpassword.setOnClickListener {
//            val username = usernameInput.text.toString().trim()
//            if (username.isEmpty()) {
//                Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show()
//            } else {
//                val randomDigits = generateRandomDigits(6)
//                getEmailFromUsername(randomDigits, username)
//            }
//        }
    }

    private fun handleLogin(username: String, password: String) {
        progressbar.visibility = View.VISIBLE
        database.child("Users").orderByChild("Info/username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressbar.visibility = View.GONE
                    if (snapshot.exists()) {
                        val userSnapshot = snapshot.children.first()
                        val userPassword = userSnapshot.child("Info/password").value.toString()
                        val accountType = userSnapshot.child("Info/accounttype").value.toString()
                        var userInfo = mapOf<String, Any>()

                        if (password == userPassword) {
                            if(accountType == "admin"){
                                userInfo = mapOf(
                                    "username" to userSnapshot.child("Info/username").value.toString(),
                                    "password" to userPassword,
                                    "accounttype" to accountType
                                )
                            } else {
                                userInfo = mapOf(
                                    "username" to userSnapshot.child("Info/username").value.toString(),
                                    "password" to userPassword,
                                    "name" to userSnapshot.child("Info/name").value.toString(),
                                    "age" to userSnapshot.child("Info/age").value.toString(),
                                    "contact" to userSnapshot.child("Info/contact").value.toString(),
                                    "email" to userSnapshot.child("Info/email").value.toString(),
                                    "street" to userSnapshot.child("Info/street").value.toString(),
                                    "barangay" to userSnapshot.child("Info/barangay").value.toString(),
                                    "position" to userSnapshot.child("Info/position").value.toString(),
                                    "imageUrl" to userSnapshot.child("Info/profileImage").value.toString(),
                                    "accounttype" to accountType
                                )
                            }
                            val editor = sharedPreferences.edit()
                            for (key in userInfo.keys) {
                                editor.putString(key, userInfo[key])
                            }

                            editor.apply()

                            if (accountType == "admin") {
                                showAdminButtons()
                            } else {
                                showUserDashboard()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressbar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showUserDashboard() {
        val intent = Intent(this@MainActivity, UserDashboardActivity::class.java)
        startActivity(intent)
    }

    private fun showAdminButtons() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Admin Actions")
        builder.setPositiveButton("Add Barangay Official Account") { dialog, _ ->
            val intent = Intent(this@MainActivity, ActivityRegistration::class.java).apply {
                putExtra("accounttype", "admin")
            }
            startActivity(intent)
            dialog.dismiss()
        }
        builder.setNegativeButton("Proceed to Dashboard") { dialog, _ ->
            val intent = Intent(this@MainActivity, UserDashboardActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun makeFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}
