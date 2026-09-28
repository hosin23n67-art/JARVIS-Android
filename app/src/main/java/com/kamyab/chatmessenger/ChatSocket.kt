package com.kamyab.chatmessenger

import okhttp3.*
import org.json.JSONObject

class ChatSocket(
    private val token: String,
    private val baseUrl: String,
    private val onMessage: (JSONObject) -> Unit,
    private val onState: (Boolean) -> Unit = {}
) {
    private var socket: WebSocket? = null

    fun connect() {
        val httpUrl = baseUrl.trimEnd('/')
        val wsUrl = if (httpUrl.startsWith("https://")) {
            "wss://" + httpUrl.removePrefix("https://")
        } else {
            "ws://" + httpUrl.removePrefix("http://")
        } + "/?token=" + token

        val request = Request.Builder().url(wsUrl).build()
        socket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onState(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try { onMessage(JSONObject(text)) } catch (_: Exception) {}
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onState(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onState(false)
            }
        })
    }

    fun sendMessage(toUserId: String, text: String): Boolean {
        val body = JSONObject()
            .put("type", "message")
            .put("to", toUserId)
            .put("text", text)
        return socket?.send(body.toString()) == true
    }

    fun close() {
        socket?.close(1000, "closed")
        socket = null
    }
}
