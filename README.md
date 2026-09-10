# JARVIS Android

دستیار صوتی فارسی JARVIS برای Android.

## امکانات
- تشخیص گفتار فارسی (`fa-IR`)
- Wake word: «جارویس» / `Jarvis`
- پاسخ صوتی با Android Text-to-Speech
- رابط کاربری HUD تیره
- ورودی متنی و میکروفون
- ساخت خودکار APK با GitHub Actions

## سازگاری
Android 8.0 (API 26) و بالاتر. مناسب برای Samsung Galaxy A13.

## APK
پس از اجرای GitHub Actions، فایل `app-debug.apk` در Artifact با نام `JARVIS-Android-debug` قرار می‌گیرد.

> کلید API را مستقیماً داخل APK قرار ندهید. برای اتصال امن به مدل هوش مصنوعی از backend/proxy استفاده شود.
