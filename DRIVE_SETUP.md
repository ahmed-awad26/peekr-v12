# إصلاح مشكلة "فشل الاتصال" - Google Drive

## السبب
الخطأ بيحصل لأن Google Cloud Console محتاج الـ SHA-1 fingerprint بتاع APK عشان يسمح بـ OAuth login.

---

## الخطوات (مرة واحدة بس)

### 1. استخرج الـ SHA-1

**لو بتشتغل بـ debug:**
```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

**لو release APK:**
```bash
keytool -list -v \
  -keystore /path/to/your-release.keystore \
  -alias your-alias
```

خد الـ SHA-1 اللي هيظهر في الـ output.

---

### 2. أضفه في Google Cloud Console

1. روح [console.cloud.google.com](https://console.cloud.google.com)
2. اختار المشروع بتاع Peekr (أو أنشئ واحد جديد)
3. من القايمة: **APIs & Services → Credentials**
4. اضغط **Create Credentials → OAuth 2.0 Client ID**
5. اختار **Android**
6. الـ Package name: `com.peekr`
7. الـ SHA-1: الكود اللي جبته من فوق
8. اضغط **Create**

---

### 3. فعّل Drive API

1. روح **APIs & Services → Library**
2. ابحث عن **Google Drive API**
3. اضغط **Enable**

---

### 4. مش محتاج google-services.json

الكود بيستخدم `GoogleSignIn` مباشرة من `play-services-auth` — مش محتاج Firebase ولا `google-services.json`.

---

## ملاحظة مهمة على الـ SHA-1

| نوع البناء | المفتاح |
|---|---|
| Debug | `~/.android/debug.keystore` |
| Release | الـ keystore اللي اخترته وقت التوقيع |
| Google Play | بتعمل SHA-1 تاني من Play Console → Release → Setup → App signing |

لو بترفع على Play Store، روح **Play Console → Setup → App integrity** وخد الـ SHA-1 من هناك وأضفه في Cloud Console كمان.
