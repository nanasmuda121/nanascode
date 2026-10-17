# NasCode

> Code editor Android berfitur lengkap ala VS Code dengan antarmuka ringkas dan ramah layar sentuh.

[![Android CI & Build APK](https://github.com/nanasmuda121/nanascode/actions/workflows/android-ci.yml/badge.svg)](https://github.com/nanasmuda121/nanascode/actions/workflows/android-ci.yml)
[![Latest Release](https://img.shields.io/github/v/release/nanasmuda121/nanascode?color=FFFFFF&label=Release%20APK)](https://github.com/nanasmuda121/nanascode/releases)

NasCode adalah IDE / Code Editor mobile berkinerja tinggi untuk pengembang web, Node.js, React, Tailwind, dan ekosistem modern yang ingin melakukan coding, git commit, eksekusi terminal, hingga live preview langsung dari smartphone atau tablet Android.

---

## Download APK Terbaru

- **[Download Release APK (v0.2.0)](https://github.com/nanasmuda121/nanascode/releases/download/v0.2.0/app-standalone-release.apk)** *(Disarankan - Teroptimasi R8, ~3.7 MB)*
- **[Download Debug APK (v0.2.0)](https://github.com/nanasmuda121/nanascode/releases/download/v0.2.0/app-standalone-debug.apk)** *(Debug build dengan signature identik)*

---

## Ringkasan Fitur Utama

- **Langsung Koding Seperti Acode (Tanpa Login)**:
  - Aplikasi langsung membuka layar editor koding seketika tanpa hambatan atau kewajiban login GitHub.
  - Sidebar penjelajah berkas dengan opsi buka folder dari penyimpanan perangkat (SAF) atau template proyek siap pakai.
  - Login GitHub sepenuhnya opsional dan hanya diperlukan jika pengguna ingin klon/publikasikan repositori remote.
- **Sistem Plugin (Kompatibel Ekstensi Acode & JavaScript)**:
  - **Toko Plugin Resmi (Marketplace)**: Pasang ekstensi sekali klik langsung di aplikasi.
  - Pilihan plugin bawaan: **Prettier Code Formatter**, **Auto Close Tag**, **Emmet HTML/CSS**, **Color Picker & Inline Preview**, **Markdown Live Previewer**, **Interactive DevTools Console**, **Bracket Matcher**, dan **Code Snippets Pro**.
  - **Pasang Plugin Kustom**: Dukungan memasang skrip plugin JavaScript kustom langsung dari URL atau teks kode.
- **Pratinjau Web Langsung & Konsol Interaktif (Live Web Preview + REPL)**:
  - Tombol Play langsung menjalankan pratinjau HTML, berkas Markdown (.md), atau script JS dengan aman di dalam WebView.
  - Dilengkapi panel konsol pengembang interaktif (mencatat log, info, peringatan, dan error secara real-time) serta input bar REPL untuk mengevaluasi kode JavaScript secara langsung.
- **Bilah Tombol Simbol Koding Cepat (Mobile Accessory Bar)**:
  - Tombol akses cepat tepat di atas keyboard: `Tab`, `Undo`, `Redo`, `{}`, `()`, `[]`, `<>`, `/`, `;`, `=`, `"`, `'`, dll.
- **Pengaturan Editor Fleksibel**:
  - Pengaturan ukuran font (10px - 24px), ukuran indentasi tab (2 atau 4 spasi), bungkus baris (*word wrap*), dan nomor baris (*gutter*).
- **Remote File Explorer GitHub**:
  - Membuka repositori remote menampilkan struktur pohon direktori (`GET /repos/{owner}/{repo}/contents`) dan pemilih branch (`GET /repos/{owner}/{repo}/branches`).
  - Penanganan pesan error asli dari GitHub API (kode HTTP + pesan server) dan status informatif untuk repositori kosong.
  - Tombol kloning cepat ke workspace lokal.
- **Desain UI Monokrom & Blok Kode Berwarna**:
  - Antarmuka elegan bertema *dark monochrome* (hitam, putih, abu-abu).
  - Editor CodeMirror menerapkan pewarnaan sintaks *VS Code Dark+* yang kaya warna.
- **Offline Git Engine (JGit)**:
  - Inisialisasi git lokal (`git init`), commit (`git commit`), status berkas, dan percabangan secara offline.
- **Keamanan Tingkat Tinggi**:
  - Android Keystore AES-256-GCM + Preferences DataStore.
  - Redaksi otomatis untuk mencegah kebocoran token pada Logcat dan URL.

---

## Palet Desain UI (Monochrome Theme)

| Peran | Deskripsi | Hex Code |
| :--- | :--- | :--- |
| **Latar Belakang (Canvas)** | Dark Canvas | `#121212` |
| **Permukaan (Card/Surface)** | Dark Surface | `#1E1E1E` |
| **Surface Variant** | Surface Light | `#262626` |
| **Border / Garis** | Dark Border | `#3F3F46` |
| **Teks Primer / Aksen** | Putih Murni | `#FFFFFF` |
| **Teks Sekunder** | Zinc Gray | `#A1A1AA` |
| **Error / Peringatan** | Red Alert | `#EF4444` |

---

## Struktur Arsitektur Multi-Module

```text
nascode/
├── app/                  # Application Shell, Flavor Standalone & PlayStore, Host Navigasi
├── core/
│   ├── model/            # Model domain murni (ProjectSource, GitHubUser, ProjectTemplate, GitHubToken)
│   ├── common/           # DispatcherProvider, SecureLogger, NanasForegroundService
│   ├── security/         # AndroidKeystoreCipher (AES-256-GCM), DataStoreTokenStorage
│   ├── git/              # JGit Engine (init, clone, commit, push, status)
│   ├── network/          # Retrofit, OkHttp, AuthInterceptor, RateLimitHandler
│   ├── database/         # Room Database & Cache Offline (UserDao, RepoDao)
│   ├── datastore/        # Preferences DataStore (Tema, Font)
│   └── ui/               # Design tokens, komponen UI Compose (NasCodeLogo, RepoItemCard)
└── feature/
    ├── workspace/        # Layar Sambutan, Manajemen Workspace Lokal, Folder Bersama
    ├── auth/             # Layar Login PAT, Validasi Scope, Multi-Akun
    ├── repo/             # Daftar Repo, Filter, Remote File Explorer, Branch Picker
    ├── editor/           # Editor CodeMirror, PluginManager, PluginManagerDialog, LivePreviewDialog, Settings
    └── actions/          # Modul F6 GitHub Actions (YAML parser, status runner)
```
