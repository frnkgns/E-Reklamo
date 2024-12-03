package com.example.e_reklamo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
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

    @SuppressLint("SetTextI18n", "MissingInflatedId")
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
        val resetButton = findViewById<TextView>(R.id.resetbutton)
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

        resetButton.setOnClickListener {
            var isAccountFound = false
            val CheckDatabase = FirebaseDatabase.getInstance().getReference("Users")
            CheckDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (randomKey in snapshot.children) {
                        val email = randomKey.child("Info/email").value.toString()
                        val username = randomKey.child("Info/username").value.toString()
                        val emailInput = passwordInput.text.toString()
                        val usernameInput = usernameInput.text.toString()

                        if (emailInput.isNotEmpty() && usernameInput.isNotEmpty()) {
                            if (email == emailInput && username == usernameInput) {
                                val userKey = randomKey.key.toString()
                                ShowDialogueInput(userKey, passwordInput)
                                resetButton.visibility = GONE
                                loginButton.visibility = VISIBLE
                                isAccountFound = true
                                break
                            }
                        } else {
                            if (emailInput.isEmpty()) {
                                passwordInput.error = "Please enter your email"
                            }
                            if (usernameInput.isEmpty()) {
                                passwordInput.error = "Please enter your username"
                            }
                        }
                    }

                    if(!isAccountFound){
                        Toast.makeText(this@MainActivity, "Incorrect username or email", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    //Handle error
                }
            })
        }
        forgotpassword.setOnClickListener {
            passwordInput.setText("")
            passwordInput.setHint("Enter your email here")
            passwordInput.inputType= InputType.TYPE_CLASS_TEXT
            loginButton.visibility = GONE
            resetButton.visibility = VISIBLE
        }
    }

    //region Log in Handliong
    private fun handleLogin(username: String, password: String) {
        progressbar.visibility = VISIBLE

        // Fetch all users
        database.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressbar.visibility = GONE

                if (snapshot.exists()) {
                    var userFound = false

                    for (userSnapshot in snapshot.children) {
                        val usernameInDb = userSnapshot.child("Info/username").value?.toString()
                        val passwordInDb = userSnapshot.child("Info/password").value?.toString()
                        val accountType = userSnapshot.child("Info/accounttype").value?.toString()

                        if (username == usernameInDb) {
                            userFound = true
                            if (password == passwordInDb) {
                                val userInfo = if (accountType == "admin") {
                                    mapOf(
                                        "username" to usernameInDb,
                                        "password" to passwordInDb,
                                        "accounttype" to accountType
                                    )
                                } else {
                                    mapOf(
                                        "username" to usernameInDb,
                                        "password" to passwordInDb,
                                        "userKey" to userSnapshot.key.toString(),
                                        "name" to userSnapshot.child("Info/name").value?.toString(),
                                        "age" to userSnapshot.child("Info/age").value?.toString(),
                                        "contact" to userSnapshot.child("Info/contact").value?.toString(),
                                        "email" to userSnapshot.child("Info/email").value?.toString(),
                                        "street" to userSnapshot.child("Info/street").value?.toString(),
                                        "barangay" to userSnapshot.child("Info/barangay").value?.toString(),
                                        "position" to userSnapshot.child("Info/position").value?.toString(),
                                        "profileImage" to userSnapshot.child("Info/profileImage").value?.toString(),
                                        "accounttype" to accountType
                                    )
                                }

                                val editor = sharedPreferences.edit()
                                for ((key, value) in userInfo) {
                                    editor.putString(key, value)
                                }
                                editor.apply()

                                if (accountType == "admin") {
                                    showAdminButtons()
                                } else {
                                    showUserDashboard()
                                }
                            } else {
                                Toast.makeText(this@MainActivity, "Invalid password", Toast.LENGTH_SHORT).show()
                            }
                            break
                        }
                    }

                    if (!userFound) {
                        Toast.makeText(this@MainActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "No users found in the database", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressbar.visibility = GONE
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    //endregion
    //region Got To User Dashboard
    private fun showUserDashboard() {
        val intent = Intent(this@MainActivity, UserDashboardActivity::class.java)
        startActivity(intent)
    }
    //endregion
    //region Show Admin Options
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
    //endregion
    fun ShowDialogueInput(userkey: String, passwordInput: EditText){
        val dialogView = LayoutInflater.from(this).inflate(R.layout.resetpassword, null) // Inflate your layout
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPassword)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.confirmPassword)
        val savePasswordButton = dialogView.findViewById<TextView>(R.id.savePasswordButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        savePasswordButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if(newPassword == confirmPassword){
                val SavedPass = FirebaseDatabase.getInstance().getReference("Users")
                SavedPass.child("$userkey/Info/password").setValue(confirmPassword)
                passwordInput.inputType= InputType.TYPE_CLASS_TEXT


                Toast.makeText(this, "Password Reset Complete", Toast.LENGTH_SHORT).show()

                passwordInput.setText("")
                passwordInput.setHint("Password")
                dialog.dismiss()

            } else {
                Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
    //region Full Screen
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
    //endregion
}
