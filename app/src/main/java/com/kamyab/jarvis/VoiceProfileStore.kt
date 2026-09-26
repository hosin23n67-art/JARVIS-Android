package com.kamyab.jarvis

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("voice_profile_v5", Context.MODE_PRIVATE)

    fun save(embedding: FloatArray) {
        require(embedding.size == 192)
        val bytes = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        embedding.forEach(bytes::putFloat)
        prefs.edit().putString("embedding", android.util.Base64.encodeToString(bytes.array(), android.util.Base64.NO_WRAP)).apply()
    }

    fun load(): FloatArray? {
        val b64 = prefs.getString("embedding", null) ?: return null
        val bytes = try { android.util.Base64.decode(b64, android.util.Base64.NO_WRAP) } catch (_: Exception) { return null }
        if (bytes.size != 192 * 4) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(192) { buf.float }
    }

    fun clear() = prefs.edit().remove("embedding").apply()
    fun exists() = prefs.contains("embedding")
}
