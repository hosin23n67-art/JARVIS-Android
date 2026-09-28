package com.kamyab.chatmessenger

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var messages: TextView

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { addMessage("پیوست: " + (it.lastPathSegment ?: "فایل")) }
    }
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { addMessage("فایل: " + (it.lastPathSegment ?: "فایل")) }
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) addMessage("📷 عکس دوربین آماده شد")
    }
    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        addMessage(if (granted) "🎙️ میکروفون آماده است" else "دسترسی میکروفون رد شد")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        messages = findViewById(R.id.messages)
        val input = findViewById<EditText>(R.id.messageInput)

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage("شما: " + text)
                input.text.clear()
            }
        }
        findViewById<Button>(R.id.attachButton).setOnClickListener { picker.launch("*/*") }
        findViewById<Button>(R.id.imageButton).setOnClickListener { picker.launch("image/*") }
        findViewById<Button>(R.id.videoButton).setOnClickListener { picker.launch("video/*") }
        findViewById<Button>(R.id.audioButton).setOnClickListener { picker.launch("audio/*") }
        findViewById<Button>(R.id.fileButton).setOnClickListener { filePicker.launch(arrayOf("*/*")) }
        findViewById<Button>(R.id.cameraButton).setOnClickListener { camera.launch(null) }
        findViewById<Button>(R.id.voiceButton).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                addMessage("🎙️ ویس آماده ارسال است")
            } else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    private fun addMessage(text: String) { messages.append("\n" + text) }
}
