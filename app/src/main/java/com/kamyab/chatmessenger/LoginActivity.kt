package com.kamyab.chatmessenger

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,32,32,32) }
        val title = TextView(this).apply { text = "ChatMessenger"; textSize = 28f }
        val username = EditText(this).apply { hint = "نام کاربری" }
        val password = EditText(this).apply { hint = "رمز عبور"; inputType = 129 }
        val login = Button(this).apply { text = "ورود" }
        val register = Button(this).apply { text = "ثبت‌نام" }
        box.addView(title); box.addView(username); box.addView(password); box.addView(login); box.addView(register)
        setContentView(box)

        login.setOnClickListener {
            ApiClient.post("/auth/login", JSONObject().put("username", username.text.toString()).put("password", password.text.toString())) { result ->
                runOnUiThread { if (result != null) open(result) else toast("اتصال به سرور برقرار نشد") }
            }
        }
        register.setOnClickListener {
            ApiClient.post("/auth/register", JSONObject().put("username", username.text.toString()).put("password", password.text.toString()).put("name", username.text.toString())) { result ->
                runOnUiThread { if (result != null) open(result) else toast("ثبت‌نام ناموفق بود") }
            }
        }
    }
    private fun open(result: String) {
        try {
            ApiClient.token = JSONObject(result).getString("token")
            getSharedPreferences("chat", MODE_PRIVATE).edit().putString("token", ApiClient.token).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (_: Exception) { toast("پاسخ سرور نامعتبر است") }
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}