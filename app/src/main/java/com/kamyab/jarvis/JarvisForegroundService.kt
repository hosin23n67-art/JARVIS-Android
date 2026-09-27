package com.kamyab.jarvis

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import androidx.core.app.NotificationCompat

class JarvisForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "jarvis_always_on"
        const val NOTIFICATION_ID = 1301
    }
    private var overlay: View? = null
    private var wm: WindowManager? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        showFloatingOrb()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Settings.canDrawOverlays(this)) showFloatingOrb()
        return START_STICKY
    }

    private fun showFloatingOrb() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val orb = ThinkingOrbView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setThinking(false)
            setOnClickListener {
                val i = Intent(this@JarvisForegroundService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("start_voice", true)
                }
                startActivity(i)
            }
        }
        val size = (76 * resources.displayMetrics.density).toInt()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val p = WindowManager.LayoutParams(
            size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (10 * resources.displayMetrics.density).toInt()
            y = (110 * resources.displayMetrics.density).toInt()
        }
        try {
            wm?.addView(orb, p)
            overlay = orb
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        overlay = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS Always On", NotificationManager.IMPORTANCE_LOW).apply {
                description = "JARVIS در پس‌زمینه فعال است"
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
            .setContentTitle("JARVIS فعال است")
            .setContentText("گوی سه‌بعدی روی همه برنامه‌ها نمایش داده می‌شود")
            .setOngoing(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
