package com.kamyab.jarvis

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.speech.*
import android.view.*
import androidx.core.app.NotificationCompat
import java.util.*

class JarvisForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "jarvis_always_on"
        const val NOTIFICATION_ID = 1301
        const val ACTION_SHOW_ORB = "com.kamyab.jarvis.SHOW_ORB"
        const val ACTION_HIDE_ORB = "com.kamyab.jarvis.HIDE_ORB"
    }
    private var overlay: View? = null
    private var wm: WindowManager? = null
    private var recognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var restarting = false
    private val handler = Handler()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        // Hidden until the wake word is heard.
        startWakeWord()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_ORB -> showFloatingOrb()
            ACTION_HIDE_ORB -> hideFloatingOrb()
        }
        if (recognizer == null) startWakeWord()
        return START_STICKY
    }
    private fun startWakeWord() {
        if (!SpeechRecognizer.isRecognitionAvailable(this) || recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { restartWakeWord() }
            override fun onError(error: Int) { restartWakeWord() }
            override fun onResults(results: Bundle?) {
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.joinToString(" ").orEmpty()
                if (isWakeWord(heard)) activateJarvis() else restartWakeWord()
            }
            override fun onPartialResults(results: Bundle?) {
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.joinToString(" ").orEmpty()
                if (isWakeWord(heard)) activateJarvis()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        restartWakeWord()
    }
    private fun isWakeWord(text: String): Boolean {
        val s = text.lowercase(Locale.getDefault()).replace('ي','ی').replace('ك','ک')
        return s.contains("جارویس") || s.contains("jarvis")
    }
    private fun activateJarvis() {
        try { recognizer?.cancel() } catch (_: Exception) {}
        showFloatingOrb()
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("start_voice", true)
        })
    }
    private fun restartWakeWord() {
        if (restarting || recognizer == null) return
        restarting = true
        handler.postDelayed({
            restarting = false
            try { recognizer?.startListening(recognizerIntent) } catch (_: Exception) {}
        }, 350)
    }
    private fun showFloatingOrb() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val orb = ThinkingOrbView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setThinking(false)
            setOnClickListener {
                startActivity(Intent(this@JarvisForegroundService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("start_voice", true)
                })
            }
        }
        val size = (92 * resources.displayMetrics.density).toInt()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val p = WindowManager.LayoutParams(size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (8 * resources.displayMetrics.density).toInt()
            y = (90 * resources.displayMetrics.density).toInt()
        }
        try { wm?.addView(orb, p); overlay = orb } catch (_: Exception) {}
    }
    private fun hideFloatingOrb() {
        overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        overlay = null
    }
    override fun onDestroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
        hideFloatingOrb()
        super.onDestroy()
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS Always On", NotificationManager.IMPORTANCE_LOW).apply {
                description = "JARVIS منتظر کلمه بیدارباش «جارویس» است"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun notification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.kamyab.jarvis.R.drawable.jarvis_icon)
            .setContentTitle("JARVIS آماده است")
            .setContentText("با گفتن «جارویس» گوی ظاهر می‌شود")
            .setOngoing(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}