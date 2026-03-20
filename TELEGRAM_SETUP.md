# 📱 تفعيل تسجيل الدخول برقم الهاتف في تليجرام

## المطلوب: TDLib Native Library

تسجيل الدخول برقم الهاتف يتطلب **TDLib** المكتبة الرسمية من Telegram.

---

## الخطوات

### 1. حمّل TDLib Prebuilt لـ Android

اذهب إلى: https://github.com/tdlight-team/tdlight-java/releases

حمّل أحدث إصدار (`tdlight-java-X.X.X.zip`)

**أو** من الـ GitHub Actions artifacts على:
https://github.com/tdlight-team/tdlight-java-natives/releases

---

### 2. انسخ الملفات للمشروع

من داخل الـ ZIP:

**ملف الـ JAR:**
```
tdlib.jar  →  app/libs/tdlib.jar
```

**ملفات الـ .so حسب المعالج:**
```
libtdjni.so (arm64-v8a)  →  app/src/main/jniLibs/arm64-v8a/libtdjni.so
libtdjni.so (armeabi-v7a) →  app/src/main/jniLibs/armeabi-v7a/libtdjni.so
libtdjni.so (x86_64)      →  app/src/main/jniLibs/x86_64/libtdjni.so
```

---

### 3. فعّل TDLib في الكود

افتح `TelegramClient.kt` وغيّر السطر:
```kotlin
private const val TDLIB_AVAILABLE = false   // ← غيّره لـ true
```

---

### 4. احصل على API ID و API Hash

1. روح: https://my.telegram.org
2. سجل الدخول برقمك
3. اضغط "API development tools"
4. أنشئ تطبيق جديد
5. احفظ الـ `App api_id` و `App api_hash`

---

### 5. أضف المفاتيح في التطبيق

الإعدادات → مفاتيح API:
- **API ID** ← ضع الرقم
- **API Hash** ← ضع الـ hash

---

### 6. ابدأ تسجيل الدخول

ربط الحسابات → تليجرام → "ابدأ الربط"

الآن سيظهر حقل رقم الهاتف الحقيقي! 📱

---

## بدون TDLib (للقنوات العامة فقط)

لو محتاج فقط تتابع قنوات عامة بدون حساب شخصي:
1. أنشئ بوت من @BotFather
2. احفظ Bot Token في مفاتيح API
3. أضف البوت للقنوات كـ Admin
4. أضف أسماء القنوات من ربط الحسابات

---

## ملاحظة مهمة للـ CI/CD

ملفات `.so` و `.jar` الخاصة بـ TDLib لا تُضمَّن في هذا المستودع لأسباب:
1. حجمها كبير (كل `.so` ≈ 15MB)
2. يجب تحميلها من المصدر الرسمي

للبناء على GitHub Actions مع TDLib، أضف الملفات كـ GitHub Secrets أو repository releases.
