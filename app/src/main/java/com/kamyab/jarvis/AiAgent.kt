package com.kamyab.jarvis

import android.content.Context
import android.content.Intent
import android.provider.Settings

class AiAgent(private val context: Context) {
    data class Action(val type: Type, val value: String = "")
    enum class Type { BACK, HOME, RECENTS, SWIPE_UP, CLICK, TYPE, SETTINGS }

    fun plan(command: String): List<Action> {
        val c = command.lowercase()
        val out = mutableListOf<Action>()
        if (c.contains("برگرد") || c.contains("بازگشت")) out += Action(Type.BACK)
        if (c.contains("خانه") || c.contains("هوم")) out += Action(Type.HOME)
        if (c.contains("برنامه های اخیر") || c.contains("برنامه‌های اخیر")) out += Action(Type.RECENTS)
        if (c.contains("پایین") || c.contains("اسکرول")) out += Action(Type.SWIPE_UP)
        Regex("روی (.+?) بزن").find(c)?.groupValues?.getOrNull(1)?.let { out += Action(Type.CLICK, it.trim()) }
        Regex("(?:بنویس|تایپ کن) (.+)").find(c)?.groupValues?.getOrNull(1)?.let { out += Action(Type.TYPE, it.trim()) }
        if (c.contains("تنظیمات") && (c.contains("باز") || c.contains("برو"))) out += Action(Type.SETTINGS)
        return out
    }

    fun execute(actions: List<Action>): List<Boolean> {
        val s = JarvisAccessibilityService.instance
        return actions.map {
            when (it.type) {
                Type.BACK -> s?.pressBack() == true
                Type.HOME -> s?.pressHome() == true
                Type.RECENTS -> s?.openRecents() == true
                Type.SWIPE_UP -> s?.swipeUp() == true
                Type.CLICK -> s?.findAndClick(it.value) == true
                Type.TYPE -> s?.typeText(it.value) == true
                Type.SETTINGS -> runCatching {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    true
                }.getOrDefault(false)
            }
        }
    }
}