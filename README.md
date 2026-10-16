# 🍍 NanasCode

> Code editor Android berfitur lengkap ala VS Code dengan antarmuka ringkas dan ramah layar sentuh seperti Acode.

[![Android CI & Build APK](https://github.com/OWNER/REPO/actions/workflows/android-ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/android-ci.yml)

NanasCode adalah IDE / Code Editor mobile berkinerja tinggi untuk pengembang web, Node.js, React, Tailwind, dan ekosistem modern yang ingin melakukan coding, git commit, terminal command, hingga live preview langsung dari smartphone atau tablet Android.

---

## 📱 Ringkasan Fitur

- **Kredensial Aman & Multi-Akun**: Login via GitHub Personal Access Token (Classic maupun Fine-grained). Disimpan dengan enkripsi hardware Keystore AES-256 GCM (`EncryptedSharedPreferences`). Redaksi otomatis untuk mencegah kebocoran token pada Logcat dan URL.
- **Manajemen Repositori GitHub**:
  - Penjelajahan repositori publik & private, pencarian instan, filter visibilitas (Publik, Private, Fork), dan pengurutan (Terakhir Diperbarui, Terbaru, Nama).
  - Pembuatan repositori baru (Publik/Private, template .gitignore, README, lisensi).
  - Deteksi & peringatan cerdas kuota **GitHub API Rate Limit** (`X-RateLimit-Remaining`).
- **Integrasi GitHub Actions (F6 - Repo Publik)**:
  - Tab khusus Actions untuk memantau status workflow run (Queued, In Progress, Success, Failure).
  - Parser interaktif form `workflow_dispatch` dari YAML.
- **Editor Ramah Sentuh (Milestone 2)**: CodeMirror 6 modular di dalam WebView terisolasi, virtualized viewport untuk berkas besar (>5 MB), pinch-to-zoom, baris tombol cepat (*Quick Accessory Bar*).
- **Terminal Emulator (Milestone 4)**: xterm.js + PTY native JNI + rootfs Alpine Linux (Node.js LTS, npm, git, python3) dengan Foreground Service.

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

Mengadopsi **Clean Architecture + MVVM** dengan Dependency Injection (Hilt), Room DB, dan Jetpack Compose Material 3:

```text
nanascode/
├── app/                  # Application Shell & Host Navigasi
├── core/
│   ├── model/            # Model domain & value objects murni (GitHubUser, GitHubToken, RateLimitInfo)
│   ├── common/           # DispatcherProvider, SecureLogger (anti token leak)
│   ├── security/         # EncryptedTokenStorage (Android Keystore AES-256)
│   ├── network/          # Retrofit, OkHttp, AuthInterceptor, RateLimitHandler
│   ├── database/         # Room Database & Cache Offline (UserDao, RepoDao)
│   ├── datastore/        # Preferences DataStore (Tema Dark/Light/AMOLED, Font)
│   └── ui/               # Design tokens, komponen UI Compose, tema Nanas
└── feature/
    ├── auth/             # Layar Login PAT, Validasi Scope, Multi-Akun
    ├── repo/             # Daftar Repo, Filter, Search, Pembuatan Repo Baru
    └── actions/          # Modul F6 GitHub Actions (YAML parser, status runner)
```

---

## 🚀 Panduan Build (GitHub Actions & Lokal)

Proyek ini dikonfigurasi untuk **dibuilt otomatis di GitHub Actions** sehingga Anda tidak perlu menginstal Android SDK berat di perangkat ponsel:

1. Push commit ke repositori GitHub Anda.
2. Buka tab **Actions** di repositori GitHub Anda.
3. Workflow **`Android CI & Build APK`** akan otomatis:
   - Menjalankan seluruh Unit Tests (`testDebugUnitTest`).
   - Melakukan kompilasi debug APK (`assembleDebug`).
   - Menghasilkan artifact `NanasCode-debug-apk` yang dapat langsung diunduh dan dipasang di ponsel.

Lihat panduan lengkap di:
- [📘 Panduan Build & CI](docs/BUILD_GUIDE.md)
- [🔑 Panduan Membuat Token GitHub](docs/TOKEN_GUIDE.md)

---

## 📋 Status Milestone

- [x] **M1: Auth & Repo List** (Selesai dengan Unit Tests & CI setup)
- [ ] **M2: Editor Inti & Operasi Berkas GitHub** (CodeMirror 6, Tabs, Git Data API)
- [ ] **M3: Git Lokal & Source Control** (JGit Clone, Commit, Diff Viewer)
- [ ] **M4: Terminal & Node.js Runtime** (xterm.js, Native PTY, Alpine rootfs)
- [ ] **M5: Template Framework, Tailwind, & Live Preview**
- [ ] **M5b: Fitur F6 GitHub Actions UI**
- [ ] **M6: Polish, UAT, & Rilis Beta**
