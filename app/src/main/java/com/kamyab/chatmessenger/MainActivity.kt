package com.kamyab.chatmessenger

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var messages: TextView
    private var socket: ChatSocket? = null
    private var peerId = ""

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { addMessage("پیوست انتخاب شد: " + (it.lastPathSegment ?: "فایل")) }
    }
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { addMessage("فایل انتخاب شد: " + (it.lastPathSegment ?: "فایل")) }
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
        val peerInput = findViewById<EditText>(R.id.peerInput)

        ApiClient.token = getSharedPreferences("chat", MODE_PRIVATE).getString("token", "") ?: ""
        peerInput.setOnEditorActionListener { _, _, _ ->
            peerId = peerInput.text.toString().trim()
            loadHistory()
            true
        }

        socket = ChatSocket(ApiClient.token, ApiClient.baseUrl, { event ->
            runOnUiThread {
                if (event.optString("type") == "message") {
                    val m = event.optJSONObject("message")
                    if (m != null) addMessage("طرف مقابل: " + m.optString("text"))
                }
            }
        }, { online ->
            runOnUiThread { findViewById<TextView>(R.id.connectionState).text = if (online) "متصل" else "قطع" }
        })
        if (ApiClient.token.isNotEmpty()) socket?.connect()

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            peerId = peerInput.text.toString().trim()
            val text = input.text.toString().trim()
            if (peerId.isEmpty()) { toast("شناسه کاربر مقصد را وارد کنید"); return@setOnClickListener }
            if (text.isNotEmpty()) {
                if (socket?.sendMessage(peerId, text) == true) addMessage("شما: $text")
                else toast("اتصال چت برقرار نیست")
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
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                addMessage("🎙️ میکروفون آماده است")
            else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun loadHistory() {
        if (peerId.isEmpty()) return
        ApiClient.get("/messages/$peerId") { result ->
            runOnUiThread {
                try {
                    val arr = JSONObject(result ?: "{}").optJSONArray("messages") ?: return@runOnUiThread
                    messages.text = ""
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        addMessage(m.optString("text"))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun addMessage(text: String) { messages.append("\n$text") }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        socket?.close()
        super.onDestroy()
    }
}
