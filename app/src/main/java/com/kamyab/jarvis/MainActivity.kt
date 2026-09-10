package com.kamyab.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
    private lateinit var orb: TextView
    private lateinit var micButton: Button
    private var waitingForCommand = false
    private var keepListening = true
    private var micEnabled = true

    private val cyan = Color.rgb(65, 230, 255)
    private val cyanSoft = Color.rgb(135, 239, 255)
    private val bg = Color.rgb(3, 9, 17)
    private val panel = Color.rgb(8, 22, 34)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        buildUi()
        setupSpeech()
        requestMicAndStart()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(fill: Int, stroke: Int = cyan, radius: Float = 22f): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius.toInt()).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun label(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 11f
        setTextColor(cyanSoft)
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(7), dp(10), dp(7))
        background = rounded(Color.rgb(6, 27, 39), Color.rgb(25, 105, 125), 14f)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(16))
            setBackgroundColor(bg)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"; textSize = 29f; setTextColor(cyan)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0.18f
        }
        val subtitle = TextView(this).apply {
            text = "PERSONAL AI ASSISTANT  •  v1.1"; textSize = 9f
            setTextColor(Color.rgb(90, 150, 170)); letterSpacing = 0.1f
        }
        brand.addView(title); brand.addView(subtitle)
        top.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(label("●  ONLINE"))
        root.addView(top, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(12)) }
        listOf("AI CORE  READY", "MIC  CONTROL", "FA-IR").forEachIndexed { i, s ->
            val p = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) p.marginStart = dp(7)
            chips.addView(label(s), p)
        }
        root.addView(chips, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        orb = TextView(this).apply {
            text = "◉"; textSize = 100f; gravity = Gravity.CENTER; setTextColor(cyan)
            setShadowLayer(28f, 0f, 0f, cyan)
            background = rounded(Color.rgb(4, 19, 29), Color.rgb(31, 151, 180), 100f)
            contentDescription = "JARVIS voice core"
            setOnClickListener { if (micEnabled) startListening() }
        }
        root.addView(orb, LinearLayout.LayoutParams(dp(190), dp(190)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(4); bottomMargin = dp(12) })

        status = TextView(this).apply {
            text = "LISTENING  •  منتظر فرمان شما"; textSize = 13f; setTextColor(cyanSoft); gravity = Gravity.CENTER
            setTypeface(Typeface.DEFAULT, Typeface.BOLD); setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        root.addView(TextView(this).apply {
            text = "  CONVERSATION LOG   /   گزارش گفتگو"; textSize = 10f
            setTextColor(Color.rgb(78, 164, 188)); setPadding(dp(4), dp(7), 0, dp(7))
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val scroll = ScrollView(this).apply { background = rounded(panel, Color.rgb(21, 71, 88), 18f) }
        log = TextView(this).apply {
            text = "JARVIS  ›  سیستم آماده است. فقط بگو «جارویس».\n"; textSize = 15f
            setTextColor(Color.rgb(220, 246, 250)); setLineSpacing(dp(3).toFloat(), 1.05f)
            setPadding(dp(16), dp(14), dp(16), dp(14)); textDirection = View.TEXT_DIRECTION_RTL
        }
        scroll.addView(log)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val commandBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(7), dp(7), dp(7))
            background = rounded(Color.rgb(7, 18, 28), Color.rgb(25, 95, 115), 20f)
        }
        input = EditText(this).apply {
            hint = "فرمانت را بنویس..."; setHintTextColor(Color.rgb(85, 128, 140)); setTextColor(Color.WHITE)
            textSize = 15f; setSingleLine(true); background = null; textDirection = View.TEXT_DIRECTION_RTL; setPadding(dp(8), 0, dp(8), 0)
        }
        val send = Button(this).apply {
            text = "ارسال  ›"; textSize = 12f; setTextColor(bg); background = rounded(cyan, cyan, 16f)
            setOnClickListener { val command = input.text.toString().trim(); if (command.isNotEmpty()) { input.setText(""); processCommand(command) } }
        }
        commandBox.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f)); commandBox.addView(send, LinearLayout.LayoutParams(dp(92), dp(48)))
        root.addView(commandBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })

        micButton = Button(this).apply {
            text = "🎙  میکروفون روشن — لمس برای قطع"; textSize = 14f; setTextColor(bg)
            background = rounded(cyan, cyan, 18f)
            setOnClickListener { toggleMicrophone() }
        }
        root.addView(micButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(9) })
        setContentView(root)
    }

    private fun toggleMicrophone() {
        micEnabled = !micEnabled
        if (micEnabled) {
            keepListening = true
            micButton.text = "🎙  میکروفون روشن — لمس برای قطع"
            micButton.setTextColor(bg)
            micButton.background = rounded(cyan, cyan, 18f)
            append("JARVIS  ›  میکروفون روشن شد.")
            setState("LISTENING  •  میکروفون روشن")
            startListening()
        } else {
            keepListening = false
            waitingForCommand = false
            try { speechRecognizer.cancel() } catch (_: Exception) {}
            micButton.text = "🔇  میکروفون خاموش — لمس برای وصل"
            micButton.setTextColor(cyanSoft)
            micButton.background = rounded(Color.rgb(35, 18, 24), Color.rgb(180, 70, 85), 18f)
            append("JARVIS  ›  میکروفون خاموش شد.")
            setState("MIC OFF  •  میکروفون خاموش", false)
        }
    }

    private fun setState(text: String, active: Boolean = true) { status.text = text; orb.setTextColor(if (active) cyan else Color.rgb(80, 135, 150)) }

    private fun setupSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (micEnabled) setState("LISTENING  •  در حال شنیدن") }
            override fun onBeginningOfSpeech() { if (micEnabled) setState("VOICE DETECTED  •  صدای شما دریافت شد") }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { if (micEnabled) setState("THINKING  •  در حال پردازش") }
            override fun onError(error: Int) { if (micEnabled) { setState("STANDBY  •  منتظر «جارویس»", false); restartListening() } }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                if (!micEnabled) return
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) handleSpeech(text) else restartListening()
            }
        })
    }

    private fun handleSpeech(text: String) {
        append("شما  ›  $text")
        val lower = text.lowercase(Locale.getDefault())
        if (waitingForCommand) { waitingForCommand = false; processCommand(text); return }
        if (lower.contains("جارویس") || lower.contains("jarvis")) {
            val command = lower.replace("جارویس", "").replace("jarvis", "").trim()
            if (command.isBlank()) { waitingForCommand = true; speak("بله؟") } else processCommand(command)
        } else restartListening()
    }

    private fun processCommand(command: String) {
        setState("THINKING  •  پردازش دستور")
        val c = command.lowercase(Locale.getDefault())
        val answer = when {
            c.contains("سلام") -> "سلام. جارویس در خدمت شماست."
            c.contains("ساعت") -> "الان ساعت ${SimpleDateFormat("HH:mm", Locale("fa", "IR")).format(Date())} است."
            c.contains("اسمت") || c.contains("کی هستی") -> "من جارویس هستم، دستیار صوتی شما."
            c.contains("خوبی") -> "ممنون، همه سیستم‌ها فعال هستند."
            else -> "دستور شما دریافت شد: $command"
        }
        append("JARVIS  ›  $answer"); speak(answer)
    }

    private fun speak(text: String) {
        setState("SPEAKING  •  در حال پاسخ")
        val shouldResume = micEnabled
        keepListening = false
        try { speechRecognizer.cancel() } catch (_: Exception) {}
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
        android.os.Handler(mainLooper).postDelayed({ if (shouldResume && micEnabled) { keepListening = true; startListening() } }, 1800L + text.length * 35L)
    }

    private fun append(text: String) { log.append("\n$text\n") }

    private fun requestMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && micEnabled) startListening()
    }

    private fun startListening() {
        if (!micEnabled || !keepListening || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        try { speechRecognizer.startListening(speechIntent) } catch (_: Exception) {}
    }

    private fun restartListening() { if (micEnabled && keepListening) android.os.Handler(mainLooper).postDelayed({ startListening() }, 650) }

    override fun onInit(statusCode: Int) { if (statusCode == TextToSpeech.SUCCESS) { tts.language = Locale("fa", "IR"); tts.setSpeechRate(0.95f) } }

    override fun onDestroy() {
        micEnabled = false; keepListening = false
        speechRecognizer.destroy(); tts.stop(); tts.shutdown(); super.onDestroy()
    }
}
