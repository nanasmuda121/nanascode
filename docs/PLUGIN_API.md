# NasCode Plugin System & API Specification

Sistem plugin NasCode memungkinkan pengembang untuk memperluas fungsionalitas editor, mengotomatiskan alur kerja, menambahkan perintah kustom, serta berinteraksi dengan berkas proyek secara terisolasi dan aman.

Arsitektur plugin NasCode terinspirasi oleh sistem modular Acode (di bawah lisensi MIT), namun dioptimalkan khusus untuk arsitektur modern Kotlin dan Android WebView tanpa dependensi Cordova.

---

## 1. Struktur Berkas Plugin (`plugin.zip`)

Setiap plugin didistribusikan dalam format arsip `.zip` dengan struktur minimal sebagai berikut:

```text
my-plugin.zip
├── plugin.json       # Manifest metadata plugin (wajib)
├── main.js           # Berkas logika JavaScript utama (wajib)
├── style.css         # Gaya CSS kustom untuk editor/komponen (opsional)
├── icon.png          # Ikon plugin (opsional)
└── README.md         # Dokumentasi penggunaan (opsional)
```

---

## 2. Format Manifest (`plugin.json`)

Setiap plugin wajib menyertakan berkas `plugin.json` di direktori akar ZIP:

```json
{
  "id": "com.contoh.formatter",
  "name": "JSON & SQL Formatter",
  "version": "1.0.0",
  "versionCode": 1,
  "main": "main.js",
  "description": "Format berkas JSON dan kueri SQL otomatis.",
  "author": "NasCode Developer",
  "icon": "format",
  "minAppVersion": "0.2.0",
  "permissions": [
    "editor:read",
    "editor:write",
    "command:register",
    "notification:show"
  ]
}
```

### Penjelasan Properti:
* `id` *(string, wajib)*: Pengenal unik plugin dalam format reverse domain name (contoh: `com.developer.nama-plugin`).
* `name` *(string, wajib)*: Nama tampilan plugin di antarmuka NasCode.
* `version` *(string, wajib)*: Versi semantik plugin (misal: `1.0.0`).
* `versionCode` *(integer)*: Nomor urut integer versi untuk pengecekan pembaruan.
* `main` *(string, wajib)*: Nama berkas entrypoint JavaScript (default: `main.js`).
* `description` *(string)*: Deskripsi ringkas mengenai fungsi plugin.
* `author` *(string)*: Nama pengembang atau tim pembuat.
* `permissions` *(array string)*: Daftar izin eksplisit yang diminta oleh plugin.

---

## 3. Sistem Perizinan (Explicit Permissions)

NasCode memberlakukan *least privilege principle*. Plugin hanya dapat memanggil API yang telah dideklarasikan di `permissions` pada `plugin.json`. Jika plugin memanggil API tanpa izin yang sesuai, panggilan akan diblokir oleh jembatan keamanan Android:

| Izin | Deskripsi | Risiko |
|---|---|---|
| `editor:read` | Membaca isi teks atau seleksi aktif dari editor. | Rendah |
| `editor:write` | Menyisipkan, mengganti teks, atau mengubah posisi kursor di editor. | Rendah |
| `file:read` | Membaca isi berkas di dalam folder workspace proyek yang aktif. | Sedang |
| `file:write` | Menulis, membuat, atau mengubah berkas di folder workspace. | Sedang (dibatasi sandbox) |
| `command:register` | Menambahkan aksi baru ke Command Palette NasCode. | Rendah |
| `notification:show` | Menampilkan pemberitahuan toast atau pesan status ke pengguna. | Rendah |

> **Catatan Keamanan Sandbox**: Operasi `file:read` dan `file:write` dibatasi secara ketat hanya pada direktori kerja proyek yang sedang dibuka. Plugin tidak memiliki akses ke penyimpanan privat aplikasi atau direktori sistem.

---

## 4. Siklus Hidup Plugin (Lifecycle) & Runtime API

Ketika plugin diaktifkan, runtime mengeksekusi kode JavaScript dalam isolasi closure dan meneruskan objek konteks `ctx`:

```javascript
/**
 * Fungsi inisialisasi plugin
 * @param {NasCodePluginContext} ctx
 */
function init(ctx) {
  // 1. Mendaftarkan Perintah ke Command Palette
  ctx.command.register("format-json", "Format JSON Rapi", function() {
    const rawText = ctx.editor.getText();
    try {
      const parsed = JSON.parse(rawText);
      const formatted = JSON.stringify(parsed, null, 2);
      ctx.editor.insertText(formatted);
      ctx.notification.showToast("JSON berhasil dirapikan!");
    } catch (e) {
      ctx.notification.showToast("Gagal memformat: sintaks JSON tidak valid");
    }
  });

  // 2. Berinteraksi dengan berkas lokal proyek
  ctx.command.register("create-config", "Buat File Konfigurasi", function() {
    const defaultSettings = "{\n  \"theme\": \"dark\"\n}";
    const success = ctx.file.writeFile(".nascode.json", defaultSettings);
    if (success) {
      ctx.notification.showToast("Berkas .nascode.json dibuat!");
    }
  });
}
```

### Spesifikasi Objek `ctx`:

#### A. `ctx.editor` (Izin: `editor:read`, `editor:write`)
* `ctx.editor.getText()`: Mengembalikan seluruh teks dalam tab editor yang aktif.
* `ctx.editor.insertText(text)`: Menyisipkan teks pada posisi kursor saat ini atau menggantikan teks yang sedang disorot.
* `ctx.editor.getSelectedText()`: Mengembalikan teks yang sedang diblok/dipilih oleh pengguna.
* `ctx.editor.setCursor(line, column)`: Memindahkan kursor ke baris dan kolom yang ditentukan.

#### B. `ctx.file` (Izin: `file:read`, `file:write`)
* `ctx.file.readFile(relativePath)`: Membaca isi teks berkas yang berada di dalam folder proyek.
* `ctx.file.writeFile(relativePath, content)`: Menyimpan teks ke dalam berkas di folder proyek (membuat folder induk jika belum ada). Mengembalikan boolean (`true` jika berhasil).

#### C. `ctx.command` (Izin: `command:register`)
* `ctx.command.register(commandId, title, callback)`: Mendaftarkan aksi yang dapat dijalankan melalui menu cepat atau Command Palette.

#### D. `ctx.notification` (Izin: `notification:show`)
* `ctx.notification.showToast(message)`: Menampilkan pesan singkat di layar bawah.
* `ctx.notification.showAlert(title, message)`: Menampilkan dialog peringatan.

---

## 5. Cara Memasang Plugin di NasCode

1. Buka aplikasi NasCode.
2. Masuk ke **Menu Editor** (ikon titik tiga atau ikon puzzle Plugin).
3. Pilih menu **Plugin NasCode**.
4. Di bagian atas layar, klik ikon **Unduh / Pasang dari ZIP**.
5. Pilih berkas `.zip` plugin dari penyimpanan perangkat Anda.
6. Periksa rincian izin yang diminta oleh plugin pada dialog konfirmasi.
7. Klik **Izinkan & Pasang**. Plugin akan langsung aktif di editor!
