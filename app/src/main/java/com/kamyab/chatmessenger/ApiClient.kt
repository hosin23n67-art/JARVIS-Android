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

    fun upload(uri: android.net.Uri, context: android.content.Context, done: (String?) -> Unit) {
        try {
            val resolver = context.contentResolver
            val type = resolver.getType(uri) ?: "application/octet-stream"
            val name = "upload_" + System.currentTimeMillis()
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return done(null)
            val part = okhttp3.RequestBody.create(type.toMediaType(), bytes)
            val body = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", name, part).build()
            val request = Request.Builder().url(baseUrl + "/media")
                .header("Authorization", "Bearer " + token).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = done(null)
                override fun onResponse(call: Call, response: Response) = done(response.body?.string())
            })
        } catch (_: Exception) { done(null) }
    }

    fun upload(uri: android.net.Uri, context: android.content.Context, done: (String?) -> Unit) {
        try {
            val resolver = context.contentResolver
            val type = resolver.getType(uri) ?: "application/octet-stream"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return done(null)
            val part = okhttp3.RequestBody.create(type.toMediaType(), bytes)
            val body = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", "media_${System.currentTimeMillis()}", part).build()
            val request = Request.Builder().url(baseUrl + "/media")
                .header("Authorization", "Bearer " + token).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = done(null)
                override fun onResponse(call: Call, response: Response) = done(response.body?.string())
            })
        } catch (_: Exception) { done(null) }
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