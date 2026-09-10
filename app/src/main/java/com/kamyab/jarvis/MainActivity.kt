package com.kamyab.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var input: EditText
    private var waitingForCommand = false
    private var keepListening = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        setupSpeech()
        requestMicAndStart()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.rgb(5, 11, 18))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 34f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "CORE ONLINE • MIC READY"
            textSize = 14f
            setTextColor(Color.rgb(120, 220, 255))
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 22)
        }
        val scroll = ScrollView(this)
        log = TextView(this).apply {
            text = "جارویس آماده است. نام من را صدا بزن.\n"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(18, 18, 18, 18)
        }
        scroll.addView(log)
        input = EditText(this).apply {
            hint = "دستور خود را بنویسید..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textDirection = android.view.View.TEXT_DIRECTION_RTL
        }
        val send = Button(this).apply {
            text = "ارسال"
            setOnClickListener {
                val command = input.text.toString().trim()
                if (command.isNotEmpty()) {
                    input.setText("")
                    processCommand(command)
                }
            }
        }
        val mic = Button(this).apply {
            text = "🎙 میکروفون / JARVIS"
            setOnClickListener { startListening() }
        }
        root.addView(title, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(send, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(mic, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setContentView(root)
    }

    private fun setupSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "LISTENING • در حال شنیدن" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "THINKING • پردازش" }
            override fun onError(error: Int) { restartListening() }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) handleSpeech(text)
                else restartListening()
            }
        })
    }

    private fun handleSpeech(text: String) {
        append("شما: $text")
        val lower = text.lowercase(Locale.getDefault())
        val wakeFa = lower.contains("جارویس")
        val wakeEn = lower.contains("jarvis")
        if (waitingForCommand) {
            waitingForCommand = false
            processCommand(text)
            return
        }
        if (wakeFa || wakeEn) {
            val command = lower.replace("جارویس", "").replace("jarvis", "").trim()
            if (command.isBlank()) {
                waitingForCommand = true
                speak("بله؟")
            } else {
                processCommand(command)
            }
        } else restartListening()
    }

    private fun processCommand(command: String) {
        status.text = "THINKING • پردازش دستور"
        val c = command.lowercase(Locale.getDefault())
        val answer = when {
            c.contains("سلام") -> "سلام. جارویس در خدمت شماست."
            c.contains("ساعت") -> "الان ساعت ${SimpleDateFormat("HH:mm", Locale("fa", "IR")).format(Date())} است."
            c.contains("اسمت") || c.contains("کی هستی") -> "من جارویس هستم، دستیار صوتی شما."
            c.contains("خوبی") -> "ممنون، همه سیستم‌ها فعال هستند."
            else -> "دستور شما دریافت شد: $command"
        }
        append("JARVIS: $answer")
        speak(answer)
    }

    private fun speak(text: String) {
        status.text = "SPEAKING • در حال پاسخ"
        keepListening = false
        try { speechRecognizer.cancel() } catch (_: Exception) {}
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
        android.os.Handler(mainLooper).postDelayed({
            keepListening = true
            startListening()
        }, 1800L + text.length * 35L)
    }

    private fun append(text: String) { log.append("\n$text\n") }

    private fun requestMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
    }

    private fun startListening() {
        if (!keepListening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        try { speechRecognizer.startListening(speechIntent) } catch (_: Exception) {}
    }

    private fun restartListening() {
        if (keepListening) android.os.Handler(mainLooper).postDelayed({ startListening() }, 650)
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts.language = Locale("fa", "IR")
            tts.setSpeechRate(0.95f)
        }
    }

    override fun onDestroy() {
        keepListening = false
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
