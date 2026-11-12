# Cara membuat APK My Ride tanpa Android Studio

APK dibuat di cloud menggunakan GitHub Actions. Anda hanya perlu browser; Android Studio tidak diperlukan.

## Langkah-langkah

1. Login atau buat akun di https://github.com.
2. Buat repository baru, misalnya `my-ride-android`. Untuk proses sederhana, pilih **Public**. Jangan centang opsi untuk menambahkan README, `.gitignore`, atau license.
3. Ekstrak ZIP project My Ride di komputer.
4. Di halaman repository GitHub, pilih **Add file > Upload files**.
5. Upload seluruh isi folder `MyRideNative`, termasuk folder tersembunyi `.github`. Pastikan `.github/workflows/build-apk.yml`, `settings.gradle.kts`, dan `build.gradle.kts` berada pada lokasi yang benar.
6. Tekan **Commit changes**.
7. Buka tab **Actions**. Workflow **Build My Ride APK** berjalan otomatis setelah commit ke branch `main`.
8. Tunggu sampai status workflow hijau, lalu buka hasil run tersebut.
9. Di bagian **Artifacts**, unduh `MyRide-debug-apk`.
10. Ekstrak ZIP artifact. File `app-debug.apk` ada di dalamnya.
11. Kirim APK ke POCO F4 GT, buka file tersebut, lalu izinkan instalasi dari sumber yang digunakan untuk membukanya.

## Jika workflow tidak berjalan

- Pastikan repository memiliki file `.github/workflows/build-apk.yml`.
- Pastikan Actions diizinkan pada **Settings > Actions > General**.
- Buka tab Actions dan periksa log jika build gagal.

## Catatan

- Ini menghasilkan debug APK yang dapat dipasang untuk pengujian; bukan rilis Play Store.
- Build pertama perlu mengunduh Gradle dan dependency Android sehingga bisa memerlukan beberapa menit.
- Peta OpenStreetMap memerlukan internet. Database nota tetap tersimpan lokal.
- Jangan unggah data nota pribadi ke repository. Yang diunggah hanya source code aplikasi.
