package com.kamyab.chatmessenger

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    var baseUrl = "http://10.0.2.2:8080"
    var token = ""
    private val client = OkHttpClient()

    fun post(path: String, body: JSONObject, done: (String?) -> Unit) {
        val request = Request.Builder().url(baseUrl + path)
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = done(null)
            override fun onResponse(call: Call, response: Response) = done(response.body?.string())
        })
    }

    fun get(path: String, done: (String?) -> Unit) {
        val request = Request.Builder().url(baseUrl + path)
            .header("Authorization", "Bearer " + token).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = done(null)
            override fun onResponse(call: Call, response: Response) = done(response.body?.string())
        })
    }
}