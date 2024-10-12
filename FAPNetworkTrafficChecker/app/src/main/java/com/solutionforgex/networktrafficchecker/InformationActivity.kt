package com.solutionforgex.networktrafficchecker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InformationActivity : AppCompatActivity() {
    private var isEnglish = false  // Store language preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)

        // Retrieve the language preference from the Intent
        isEnglish = intent.getBooleanExtra("isEnglish", false)

        // Update the UI based on the language preference
        updateLanguage()

        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setOnClickListener {
            val url = "https://github.com/VDoring"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        val imageView2 = findViewById<ImageView>(R.id.imageView2)
        imageView2.setOnClickListener {
            val url = "https://buymeacoffee.com/vdoring"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    // Function to update the UI based on the selected language
    private fun updateLanguage() {
        // Update the action bar title
        supportActionBar?.title = if (isEnglish) {
            getString(R.string.information_title_english)
        } else {
            getString(R.string.information_title_korean)
        }

        // Update the TextView based on the language preference
        val textView = findViewById<TextView>(R.id.textView)
        textView.text = if (isEnglish) {
            getString(R.string.information_text_english)
        } else {
            getString(R.string.information_text_korean)
        }

        // Update other UI elements here if needed (e.g., Buttons)
    }
}
