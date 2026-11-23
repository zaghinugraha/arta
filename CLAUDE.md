# Arta — instruksi untuk Claude Code

Arta adalah aplikasi Android pribadi untuk mencatat **semua aset** (saham BEI, ETF, reksa dana, kripto, emas, tabungan, aset lain) dengan nilai yang mengikuti **harga dan kurs terbaru**. Aplikasi ini **local-first**: tidak ada backend, semua data tersimpan terenkripsi di perangkat. Internet hanya dipakai untuk (1) mengambil harga/kurs dari API publik dan (2) memanggil API provider LLM untuk AI Advisor.

Pemakaian awal: pribadi, di 4 perangkat Samsung — Galaxy S25 Ultra, Galaxy S24 Ultra, Galaxy S25 FE (ponsel ±412×891 dp) dan Galaxy Tab A9 (tablet 8.7", lanskap, perangkat paling lemah).

## Baca dulu, berurutan

1. `docs/SPEC.md` — fitur, layar, perilaku.
2. `docs/ARCHITECTURE.md` — stack, struktur modul, skema database, harga, keamanan, placeholder LLM.
3. `docs/DESIGN.md` — design token, tipografi, komponen, ikon.
4. `design/*.dc.html` — mockup final (referensi visual & path SVG ikon). Lihat `design/README.md` cara membacanya.

## Aturan yang tidak boleh dilanggar

- **Tanpa server/backend milik sendiri.** Semua logika di perangkat.
- **Tanpa SDK analytics, crash reporting, iklan, atau tracking** apa pun.
- **Database wajib terenkripsi** (Room + SQLCipher `net.zetetic:sqlcipher-android`, bukan `android-database-sqlcipher` yang sudah deprecated). Passphrase dibuat acak dan dibungkus kunci Android Keystore.
- **Jangan pakai `androidx.security:security-crypto`** (deprecated). Pakai Android Keystore langsung atau Google Tink.
- **Uang & kuantitas = `BigDecimal`**, disimpan sebagai TEXT desimal. Jangan pernah `Double`/`Float` untuk nilai uang.
- **API key LLM hanya dari `BuildConfig`** yang diisi dari `local.properties` (lihat `local.properties.example`). Jangan hardcode key, jangan commit `local.properties`.
- **Jangan log** API key, passphrase, nominal, nama lokasi, atau isi percakapan Advisor (termasuk di Logcat build debug).
- `android:allowBackup="false"` + `dataExtractionRules` yang mengecualikan semua data.
- **Tangkapan layar & rekam layar diizinkan secara default** (keputusan pemilik, pemakaian pribadi). `FLAG_SECURE` hanya dipasang bila pengguna menyalakan "Blokir tangkapan layar" di Pengaturan.
- Data yang dikirim ke LLM hanya **ringkasan agregat** (persentase, perubahan, batas) — tanpa nama lokasi, simbol spesifik bila tidak perlu, dan tanpa nominal persis. Lihat `ARCHITECTURE.md §7`.
- Teks UI berbahasa **Indonesia** (siapkan `strings.xml` agar bisa ditambah bahasa Inggris nanti).

## Konvensi

- Package: `id.shoko.arta` (boleh diganti pemilik).
- Kotlin + Jetpack Compose + Material 3, minSdk 31, targetSdk & compileSdk terbaru yang stabil.
- Gunakan **versi stabil terbaru** setiap library saat membuat `gradle/libs.versions.toml`; cek dulu, jangan menebak nomor versi.
- Arsitektur: single-activity, MVVM + UDF (StateFlow), Coroutines/Flow, DI pakai Koin (atau manual). Hindari Hilt untuk menjaga build ringan.
- Unit test wajib untuk: valuasi, konversi kurs, analytics Advisor (konsentrasi, alokasi vs target, eksposur USD, simulasi what-if), dan parser respons harga.

## Urutan pengerjaan (milestone)

1. **M1 — Fondasi:** proyek Gradle (version catalog), tema & token dari `DESIGN.md`, font Plus Jakarta Sans (bundled di `res/font`), navigasi 4 tab + FAB, kerangka layar kosong.
2. **M2 — Keamanan & data:** Keystore + passphrase, Room + SQLCipher, entity/DAO sesuai `ARCHITECTURE.md §4`, lock screen biometrik, FLAG_SECURE opsional, backup off.
3. **M3 — Pencatatan:** CRUD lokasi, alur "Tambah posisi" (FAB), catat beli/jual opsional, detail lokasi.
4. **M4 — Harga:** provider harga (interface + implementasi), cache, WorkManager periodik, penanda harga basi, konversi kurs.
5. **M5 — Ringkasan:** total kekayaan, grafik, alokasi, kelas aset, tab Lokasi, snapshot harian.
6. **M6 — Simulasi:** layar what-if (slider + preset).
7. **M7 — Advisor:** engine analytics lokal, `LlmClient` placeholder + mode template, layar chat.
8. **M8 — Pengaturan & polish:** semua setelan, ekspor CSV, tablet (navigation rail + dua panel), mode kaca ringan untuk perangkat lemah, uji di 412 dp dan tablet lanskap.

Setiap milestone harus bisa di-build dan dijalankan sebelum lanjut.
