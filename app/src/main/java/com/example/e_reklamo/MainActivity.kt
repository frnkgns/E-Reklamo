package com.example.e_reklamo

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var progressbar: ProgressBar

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText

    private lateinit var sharedPreferences: SharedPreferences


    @SuppressLint("WrongViewCast", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        makeFullscreen()

        auth = FirebaseAuth.getInstance()

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        val loginButton = findViewById<TextView>(R.id.loginbutton)
        val signup = findViewById<TextView>(R.id.signupLink)
        val forgotpassword = findViewById<TextView>(R.id.forgotPassword)
        progressbar = findViewById(R.id.progressBar)

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val savedUsername = sharedPreferences.getString("username", "").toString()
        val savedPassword = sharedPreferences.getString("password", "").toString()
        if(savedUsername.isNotEmpty() && savedPassword.isNotEmpty()){
            handleLogin(savedUsername, savedPassword)
        } else {
            loginButton.performClick()
        }
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            handleLogin(username, password)
        }

        signup.setOnClickListener {
            val intent = Intent(this@MainActivity, ActivityRegistration::class.java)
                .apply {
                    putExtra("accounttype", "user")
                }
            startActivity(intent)
        }

        forgotpassword.setOnClickListener {
            val eMail = usernameInput.text.toString()
            usernameInput.setHint("Enter your email to reset password")
            if (eMail.isEmpty()) {
                Toast.makeText(this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(eMail).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(eMail)
            }
        }
    }

    private fun handleLogin(username: String, password: String) {
        progressbar.visibility = View.VISIBLE
        database = FirebaseDatabase.getInstance().getReference("Users")
        database.orderByChild("Info/username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userSnapshot = snapshot.children.first()
                        val userPassword = userSnapshot.child("Info/password").value.toString()
                        val accountType = userSnapshot.child("Info/accounttype").value.toString()

                        // Retrieve other user details
                        val name = userSnapshot.child("Info/name").value.toString()
                        val age = userSnapshot.child("Info/age").value.toString()
                        val contact = userSnapshot.child("Info/contact").value.toString()
                        val email = userSnapshot.child("Info/email").value.toString()
                        val street = userSnapshot.child("Info/street").value.toString()
                        val barangay = userSnapshot.child("Info/barangay").value.toString()
                        val position = userSnapshot.child("Info/position").value.toString()

                        // Check if password matches
                        if (password == userPassword) {
                            // Save user details to SharedPreferences
                            val editor = sharedPreferences.edit()
                            editor.putString("username", username)
                            editor.putString("name", name)
                            editor.putString("age", age)
                            editor.putString("contact", contact)
                            editor.putString("email", email)
                            editor.putString("street", street)
                            editor.putString("barangay", barangay)
                            editor.putString("position", position)
                            editor.putString("accounttype", accountType)
                            editor.apply()

                            // Navigate to different dashboard based on account type
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
                    progressbar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
}

    private fun showUserDashboard() {
        // Handle what happens when user logs in (can show a specific UI for user)
        Toast.makeText(this@MainActivity, "User logged in", Toast.LENGTH_SHORT).show()
        // Navigate to user dashboard or activity
        val intent = Intent(this@MainActivity, UserDashboardActivity::class.java)
        startActivity(intent)
    }
    private fun showAdminButtons() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val adminUsername = usernameInput.text.toString()
        val adminPassword = passwordInput.text.toString()
        editor.putString("username", adminUsername)
        editor.putString("password", adminPassword)
        editor.apply()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Admin Actions")

        builder.setPositiveButton("Add Barangay Official Account") { dialog, _ ->
            Toast.makeText(this@MainActivity, "Add Official clicked", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MainActivity, ActivityRegistration::class.java)
                .apply { putExtra("accounttype", "admin") }
            startActivity(intent)
            dialog.dismiss()
        }

        builder.setNegativeButton("Proceed to Dashboard") { dialog, _ ->
            Toast.makeText(this@MainActivity, "Proceeding to Dashboard", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@MainActivity, UserDashboardActivity::class.java))
            dialog.dismiss()
        }

        builder.create().show()
    }

    //region Password Reset
    private fun resetPassword(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
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