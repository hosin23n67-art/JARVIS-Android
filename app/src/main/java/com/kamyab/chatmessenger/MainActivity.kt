package com.kamyab.chatmessenger

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.media.MediaRecorder
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var recorder: MediaRecorder? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { addAttachment(it, "رسانه") }
    }
    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { addAttachment(it, "فایل") }
    }
    private val requestAudio = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else toast("اجازه میکروفون داده نشد")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val input = findViewById<EditText>(R.id.messageInput)
        val messages = findViewById<TextView>(R.id.messages)
        findViewById<Button>(R.id.sendButton).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) { messages.append("\nشما: $text"); input.text.clear() }
        }
        findViewById<Button>(R.id.attachButton).setOnClickListener { showAttachmentMenu() }
        findViewById<Button>(R.id.voiceButton).setOnClickListener {
            if (recorder == null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    startRecording()
                else requestAudio.launch(Manifest.permission.RECORD_AUDIO)
            } else stopRecording(messages)
        }
    }

    private fun showAttachmentMenu() {
        val items = arrayOf("🖼 عکس", "🎞 GIF", "🎬 ویدئو", "📄 فایل")
        AlertDialog.Builder(this).setTitle("ارسال").setItems(items) { _, which ->
            when (which) {
                0 -> pickMedia.launch("image/*")
                1 -> pickMedia.launch("image/gif")
                2 -> pickMedia.launch("video/*")
                3 -> pickFile.launch(arrayOf("*/*"))
            }
        }.show()
    }

    private fun addAttachment(uri: Uri, type: String) {
        findViewById<TextView>(R.id.messages).append("\nشما: [$type انتخاب شد]")
    }

    private fun startRecording() {
        val file = java.io.File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        recorder = MediaRecorder(this).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            try { prepare(); start(); toast("🎙 ضبط شروع شد؛ دوباره بزنید برای توقف") }
            catch (_: IOException) { release(); recorder = null; toast("خطا در ضبط") }
        }
    }

    private fun stopRecording(messages: TextView) {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release(); recorder = null
        messages.append("\nشما: 🎙 پیام صوتی آماده ارسال")
        toast("ویس آماده شد")
    }

    override fun onDestroy() { recorder?.release(); recorder = null; super.onDestroy() }
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
