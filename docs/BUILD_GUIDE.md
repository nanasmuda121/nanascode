# 🛠️ Panduan Build NanasCode

Dokumen ini menjelaskan alur kompilasi aplikasi **NanasCode** menggunakan **GitHub Actions CI** (rekomendasi utama) maupun kompilasi manual.

---

## ☁️ 1. Build Otomatis via GitHub Actions (Sangat Disarankan)

Karena lingkungan smartphone Android tidak didesain untuk menjalankan Android SDK / Gradle daemon yang membutuhkan RAM besar (>4 GB), proses build NanasCode didelegasikan sepenuhnya ke **GitHub Actions runner (Ubuntu-latest)**.

### Langkah Build di GitHub:
1. Buat repositori baru di akun GitHub Anda (misalnya: `nanascode-android`).
2. Di terminal ponsel Anda, hubungkan remote git dan lakukan push:
   ```bash
   cd /root/nanascode
   git remote add origin https://github.com/<username-anda>/<nama-repo>.git
   git push -u origin main
   ```
3. Buka tab **Actions** pada halaman repositori GitHub Anda di browser.
4. Anda akan melihat pipeline **`Android CI & Build APK`** berjalan secara otomatis.
5. Setelah build selesai (biasanya 2-3 menit):
   - Klik pada run yang berhasil (tanda centang hijau).
   - Di bagian bawah halaman (bagian **Artifacts**), klik **`NanasCode-debug-apk`**.
   - Berkas zip akan terunduh. Ekstrak berkas tersebut untuk mendapatkan `app-debug.apk` dan instal langsung di smartphone Android Anda!

---

## 💻 2. Build Lokal di Komputer / Laptop (Opsional)

Jika Anda ingin mengompilasi langsung di laptop atau PC pengembang:

### Persyaratan:
- JDK 17 (OpenJDK 17 / Temurin)
- Android SDK Platform 34
- Android Build Tools 34.0.0
- NDK (r25c atau lebih baru untuk ABI `arm64-v8a` & `armeabi-v7a`)

### Perintah Build:
```bash
# Menjalankan seluruh Unit Tests
./gradlew testDebugUnitTest

# Melakukan kompilasi APK Debug
./gradlew assembleDebug

# Hasil APK akan berada di:
# app/build/outputs/apk/debug/app-debug.apk
```
