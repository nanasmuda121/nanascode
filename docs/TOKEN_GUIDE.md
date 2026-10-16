# 🔑 Panduan Membuat GitHub Personal Access Token (PAT)

Dokumen ini menjelaskan cara membuat Personal Access Token di GitHub untuk digunakan pada aplikasi **NanasCode**.

---

## 1. Opsi A: Token Classic (Sangat Disarankan)

Token Classic adalah jenis token yang paling mudah diatur dan mendukung seluruh fitur NanasCode (termasuk pembuatan repo, commit file, dan tab GitHub Actions).

### Langkah-langkah:
1. Buka peramban (browser) dan masuk ke akun GitHub Anda:
   👉 [https://github.com/settings/tokens](https://github.com/settings/tokens)
2. Klik tombol **Generate new token** lalu pilih **Generate new token (classic)**.
3. Berikan nama pada kolom **Note**, misalnya: `NanasCode Mobile Editor`.
4. Pilih **Expiration** (Masa Berlaku), disarankan `90 days` atau `No expiration` jika untuk penggunaan jangka panjang pribadi.
5. **Centang Scope Wajib Berikut**:
   - ✅ **`repo`** *(Wajib)*: Memberikan akses penuh ke repositori publik & private, commit file, dan pembuatan branch.
   - ✅ **`read:user`** *(Wajib)*: Mengizinkan aplikasi membaca profil (nama, avatar, email, jumlah repo).
   - ✅ **`workflow`** *(Wajib untuk Fitur F6)*: Mengizinkan melihat status, re-run, dan menjalankan `workflow_dispatch` pada GitHub Actions.
6. Gulir ke bagian paling bawah dan klik **Generate token**.
7. **Salin token** yang diawali dengan `ghp_...` ke clipboard Anda.
8. Buka **NanasCode**, tekan tombol **"Tempel dari Clipboard"**, lalu tekan **Masuk ke GitHub**.

---

## 2. Opsi B: Token Fine-Grained

Jika Anda ingin membatasi akses hanya ke repositori tertentu dengan izin yang lebih spesifik:

### Langkah-langkah:
1. Buka [https://github.com/settings/tokens?type=beta](https://github.com/settings/tokens?type=beta)
2. Klik **Generate new token**.
3. Isi nama token, deskripsi, dan pilih masa berlaku (maksimal 1 tahun).
4. Di bagian **Repository access**, pilih **All repositories** atau **Only select repositories**.
5. Pada bagian **Permissions** -> **Repository permissions**:
   - **Contents**: Pilih **Read and write** (untuk mengedit dan meng-commit berkas).
   - **Metadata**: Pilih **Read-only** (wajib default).
   - **Workflows**: Pilih **Read and write** (untuk mengelola GitHub Actions).
   - **Administration**: Pilih **Read and write** (jika ingin bisa membuat repositori baru dari aplikasi).
6. Klik **Generate token**, salin token yang diawali dengan `github_pat_...`, lalu tempelkan di NanasCode.

---

## 🛡️ Praktik Keamanan NanasCode

- **Penyimpanan Lokal**: NanasCode menyimpan token Anda di `EncryptedSharedPreferences` yang dienkripsi secara hardware melalui **Android Keystore** (AES-256 GCM).
- **Anti Kebocoran**: Token otomatis disensor pada seluruh logcat internal aplikasi (`Bearer [REDACTED]`), crash reports, maupun network trace.
- **Logout Total**: Menekan tombol Logout akan langsung menghapus token terenkripsi dan cache repositori dari penyimpanan internal perangkat Anda.
