# 🍍 NanasCode

> Code editor Android berfitur lengkap ala VS Code dengan antarmuka ringkas dan ramah layar sentuh seperti Acode.

[![Android CI & Build APK](https://github.com/nanasmuda121/nanascode/actions/workflows/android-ci.yml/badge.svg)](https://github.com/nanasmuda121/nanascode/actions/workflows/android-ci.yml)
[![Latest Release](https://img.shields.io/github/v/release/nanasmuda121/nanascode?color=F5C518&label=Release%20APK)](https://github.com/nanasmuda121/nanascode/releases)

NanasCode adalah IDE / Code Editor mobile berkinerja tinggi untuk pengembang web, Node.js, React, Tailwind, dan ekosistem modern yang ingin melakukan coding, git commit, terminal command, hingga live preview langsung dari smartphone atau tablet Android.

---

## 📥 Download APK Terbaru

- 🚀 **[Download Release APK (v0.1.5-alpha)](https://github.com/nanasmuda121/nanascode/releases/download/v0.1.5-alpha/app-standalone-release.apk)** *(Disarankan - Siap Pasang)*
- 🛠️ **[Download Debug APK](https://github.com/nanasmuda121/nanascode/releases/download/v0.1.5-alpha/app-standalone-debug.apk)**

---

## 📱 Ringkasan Fitur Utama

- **Mode Lokal Offline-First (Login GitHub Sekarang Opsional)**:
  - Layar awal menyediakan pilihan **"Lanjut Tanpa Login"** dan **"Masuk dengan GitHub"**.
  - Koding, manajemen file, terminal, template proyek, dan git commit offline dapat digunakan sepenuhnya tanpa wajib akun GitHub.
  - Login dapat dilakukan kapan saja nanti dari menu Pengaturan atau saat mempublikasikan proyek ke GitHub.
- **Manajemen Workspace & Project Templates**:
  - Template bawaan siap pakai:
    - 🎨 **HTML5 + Tailwind CSS v4** (kompilasi CSS kilat ~268ms di ARM64).
    - ⚡ **Node.js + Express API** (REST backend).
    - ⚛️ **React + Vite SPA** (Single Page App).
    - 🐍 **Python CLI Script**.
    - 📁 **Proyek Kosong**.
  - Buka & impor folder eksternal dari penyimpanan HP via **SAF (*Storage Access Framework*)**.
  - Fitur **"Publikasikan ke GitHub"** untuk mengekspor proyek lokal ke repositori GitHub baru dengan satu ketukan.
- **Offline Git Engine (JGit)**:
  - Inisialisasi git lokal (`git init`), commit (`git commit`), status per berkas, dan percabangan secara offline.
  - Kloning repositori publik tanpa autentikasi (*anonymous clone* via JGit).
- **Keamanan Tingkat Tinggi (Android Keystore AES-256-GCM + DataStore)**:
  - Menggantikan *EncryptedSharedPreferences* dengan *Hardware-backed Keystore AES-GCM*.
  - Ciphertext tersimpan aman di Preferences DataStore secara asinkron tanpa I/O blocking.
  - Redaksi otomatis untuk mencegah kebocoran token pada Logcat dan URL.
- **Manajemen Repositori GitHub**:
  - Penjelajahan repositori publik & private, pencarian instan, filter visibilitas, dan urutan pembaruan.
  - Deteksi kuota **GitHub API Rate Limit** (`X-RateLimit-Remaining`).
- **Integrasi GitHub Actions (F6 - Khusus Repo Publik)**:
  - Tab khusus Actions untuk memantau status workflow run (Queued, In Progress, Success, Failure).
  - Parser interaktif form `workflow_dispatch` dari YAML.

---

## 📺 Video Tutorial Rekomendasi untuk Pengguna

Untuk memahami cara kerja koding mobile, pembuatan Personal Access Token GitHub, dan setup development di Android, simak video tutorial pilihan berikut:

1. **Membuat Personal Access Token GitHub (Classic & Fine-grained)**:
   - 🎥 [How to Create a GitHub Personal Access Token (PAT) Tutorial](https://www.youtube.com/results?search_query=how+to+create+github+personal+access+token)
   - *Panduan langkah demi langkah memilih scope `repo`, `read:user`, dan `workflow` untuk NanasCode.*

2. **Koding & Menjalankan Node.js / Linux di Android**:
   - 🎥 [How to Code on Android with Termux, Node.js & Git](https://www.youtube.com/results?search_query=termux+nodejs+git+coding+on+android)
   - *Penjelasan arsitektur Linux rootfs dan eksekusi command line di ponsel Android.*

3. **Code Editor Mobile & Web Development di HP**:
   - 🎥 [Best Mobile Code Editors for Android (Acode & VS Code Setup)](https://www.youtube.com/results?search_query=acode+editor+android+web+development)
   - *Tips tata letak ramah sentuh dan alur kerja pengembangan web di layar sentuh.*

---

## 🎨 Palet Desain (Acode Style)

| Peran | Warna | Hex Code |
| :--- | :--- | :--- |
| **Primer** | Kuning Nanas | `#F5C518` |
| **Aksen** | Hijau Daun | `#2E7D32` |
| **Latar Belakang** | Abu Gelap | `#1E1E1E` |
| **Permukaan / Kartu**| Abu Permukaan | `#252526` |
| **Border / Garis** | Abu Garis | `#3C3C3C` |
| **Teks Primer** | Abu Terang | `#D4D4D4` |
| **Teks Sekunder** | Abu Muted | `#9E9E9E` |

---

## 🏗️ Struktur Arsitektur Multi-Module

Mengadopsi **Clean Architecture + MVVM** dengan Dependency Injection (Hilt), Room DB, Preferences DataStore, dan Jetpack Compose Material 3:

```text
nanascode/
├── app/                  # Application Shell, Flavor Standalone & PlayStore, Host Navigasi
├── core/
│   ├── model/            # Model domain murni (ProjectSource, GitHubUser, ProjectTemplate, GitHubToken)
│   ├── common/           # DispatcherProvider, SecureLogger (anti token leak)
│   ├── security/         # AndroidKeystoreCipher (AES-256-GCM), DataStoreTokenStorage
│   ├── git/              # JGit Engine (init, clone, commit, push, status)
│   ├── network/          # Retrofit, OkHttp, AuthInterceptor, RateLimitHandler
│   ├── database/         # Room Database & Cache Offline (UserDao, RepoDao)
│   ├── datastore/        # Preferences DataStore (Tema Dark/Light/AMOLED, Font)
│   └── ui/               # Design tokens, komponen UI Compose, tema Nanas
└── feature/
    ├── workspace/        # Layar Sambutan, Manajemen Workspace Lokal, Impor SAF, Publikasi ke GitHub
    ├── auth/             # Layar Login PAT, Validasi Scope, Multi-Akun
    ├── repo/             # Daftar Repo, Filter, Search, Pembuatan Repo Baru
    └── actions/          # Modul F6 GitHub Actions (YAML parser, status runner)
```

---

## 📑 Dokumentasi Lanjutan
- [🧪 Laporan Spike Teknis Terminal & Tailwind v4 (ARM64)](docs/TERMINAL_SPIKE_REPORT.md)
- [📘 Panduan Build & CI](docs/BUILD_GUIDE.md)
- [🔑 Panduan Membuat Token GitHub](docs/TOKEN_GUIDE.md)
