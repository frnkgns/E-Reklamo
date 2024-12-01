package com.example.e_reklamo

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class SupabasUpload : AppCompatActivity() {

    private lateinit var selectFileButton: Button
    private lateinit var uploadButton: Button
    private lateinit var imageView: ImageView
    private lateinit var progressBar: ProgressBar
    private var selectedFileUri: Uri? = null

    // Initialize Supabase client with your credentials
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://zdabqmaoocqiqjlbjymi.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkYWJxbWFvb2NxaXFqbGJqeW1pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI4NTQyODcsImV4cCI6MjA0ODQzMDI4N30.m0Mi4G4Henu9nt_E4P0TqJVKe_Q1S6ZhC7UkLRWpTsA"
    ) {
        install(Storage) {
            // Configure storage settings if needed
        }
    }

    // Registering activity result launcher for file picking
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            selectedFileUri = data?.data
            selectedFileUri?.let {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap) // Display selected image in ImageView
                imageView.visibility = View.VISIBLE // Make ImageView visible
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabas_upload)

        selectFileButton = findViewById(R.id.selectFileButton)
        uploadButton = findViewById(R.id.uploadButton)
        imageView = findViewById(R.id.imageView)
        progressBar = findViewById(R.id.progressBar)

        // Open file picker when select button is clicked
        selectFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "*/*" // You can specify MIME type (e.g., "image/*" for images)
            filePickerLauncher.launch(intent)
        }

        // Upload selected file to Supabase when upload button is clicked
        uploadButton.setOnClickListener {
            selectedFileUri?.let { uri ->
                uploadFileToSupabase(uri)
            } ?: Toast.makeText(this, "Please select a file first.", Toast.LENGTH_SHORT).show()
        }
    }

    // Upload the selected file to Supabase storage
    private fun uploadFileToSupabase(uri: Uri) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                // Get the file's input stream and convert it to a byte array
                val inputStream = contentResolver.openInputStream(uri)
                val byteArray = inputStream?.readBytes() ?: throw Exception("File not found")

                // Get the file name (including the correct extension)
                val fileName = getFileName(uri)

                // Upload the file to Supabase storage
                val bucket = supabase.storage.from("images") // Specify your bucket name here
                val uploadResult = bucket.upload(fileName, byteArray)

                // Check if the upload was successful
                if (uploadResult.key != null) {
                    // Show success message and hide progress bar
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@SupabasUpload, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw Exception("Upload failed: No key returned")
                }
            } catch (e: Exception) {
                // Handle errors during upload
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@SupabasUpload, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Helper function to get the real file name, including the extension
    private fun getFileName(uri: Uri): String {
        var fileName = ""

        // Try to get the file name from the URI
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            it.moveToFirst()
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            fileName = it.getString(nameIndex)
            cursor.close()
        }

        // If fileName is empty, use a fallback with a default extension
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
