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
    private var recording = false
    private var recorder: android.media.MediaRecorder? = null
    private var voiceFile: java.io.File? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadMedia(it) }
    }
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { uploadMedia(it) }
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { val f = java.io.File(cacheDir, "camera_" + System.currentTimeMillis() + ".jpg"); java.io.FileOutputStream(f).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }; uploadMedia(Uri.fromFile(f)) }
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
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { audioPermission.launch(Manifest.permission.RECORD_AUDIO); return@setOnClickListener }
            if (!recording) startVoice() else stopVoice()
        }
    }

    private fun startVoice() {
        try {
            voiceFile = java.io.File(cacheDir, "voice_" + System.currentTimeMillis() + ".m4a")
            recorder = android.media.MediaRecorder(this).apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(voiceFile!!.absolutePath)
                prepare(); start()
            }
            recording = true
            findViewById<Button>(R.id.voiceButton).text = "⏹️"
            addMessage("🎙️ در حال ضبط...")
        } catch (_: Exception) { toast("شروع ضبط ناموفق بود") }
    }

    private fun stopVoice() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release(); recorder = null; recording = false
        findViewById<Button>(R.id.voiceButton).text = "🎙️"
        voiceFile?.let { uploadMedia(Uri.fromFile(it)) }
    }

    private fun uploadMedia(uri: Uri) {
        addMessage("⏳ در حال ارسال فایل...")
        ApiClient.upload(uri, this) { result ->
            runOnUiThread {
                if (result != null) {
                    try {
                        val obj = JSONObject(result)
                        val url = obj.optString("url")
                        if (peerId.isNotEmpty() && url.isNotEmpty()) socket?.sendMessage(peerId, "[media] " + url)
                        addMessage("✅ فایل روی سرور قرار گرفت")
                    } catch (_: Exception) { addMessage("❌ پاسخ سرور نامعتبر است") }
                } else addMessage("❌ ارسال فایل ناموفق بود")
            }
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
