# Arta — Spesifikasi produk

## 1. Konsep

Arta menjawab satu pertanyaan: **apa yang aku punya, di mana, dan berapa nilainya sekarang?**
Arta bukan pencatat pengeluaran. Fokusnya kekayaan bersih (net worth) dari semua aset.

Model mentalnya berbasis **lokasi**: aset dikelompokkan berdasarkan *tempat menyimpannya* (Stockbit, Indodax, BCA, brankas rumah, Pegadaian, Gotrade, Bibit, dst.), lalu tiap lokasi berisi **posisi** ("25 gram emas Antam", "0,05 BTC", "150 lot BBCA"). Pengguna cukup mengisi jumlah; harga mengikuti otomatis. Riwayat beli/jual **opsional** — kalau diisi, untung/rugi muncul; kalau tidak, aplikasi tetap jalan.

## 2. Kelas aset

| Kode | Nama di UI | Satuan | Sumber harga default |
|---|---|---|---|
| `STOCK_IDX` | Saham BEI | lot (1 lot = 100 lembar) | Yahoo Finance (`KODE.JK`), tertunda ±15 menit |
| `ETF` | ETF | unit/lembar | Yahoo Finance (USD) |
| `MUTUAL_FUND` | Reksa dana | unit | Manual (NAB diisi pengguna) |
| `CRYPTO` | Kripto | koin | CoinGecko (IDR langsung) |
| `GOLD` | Emas | gram | Manual "harga buyback per gram" (default) atau spot XAU × USD/IDR |
| `CASH` | Tabungan | rupiah / mata uang | Nilai = saldo (×kurs jika valas) |
| `OTHER` | Aset lain | unit | Manual (properti, kendaraan, dll.) |

## 3. Navigasi

Bottom navigation frosted (ikon saja, titik indikator di bawah ikon aktif) dengan 4 tab + FAB:

1. **Ringkasan** (beranda)
2. **Simulasi**
3. **Advisor**
4. **Pengaturan**

FAB "+" (kotak membulat, warna aksen) di kanan bawah, di atas nav → alur **Tambah posisi**.

Tablet (Tab A9, lebar ≥ 840 dp / lanskap): ganti bottom nav dengan **navigation rail** di kiri; FAB pindah ke atas rail; Ringkasan tampil dua panel (konten kiri, panel Advisor kanan).

## 4. Layar

### 4.1 Lock screen
- Muncul saat aplikasi dibuka atau kembali dari background > N menit (default 1 menit, bisa diatur).
- BiometricPrompt (sidik jari) dengan fallback kredensial perangkat. Membuka kunci Keystore → membuka database.

### 4.2 Onboarding (pertama kali)
- Sambutan singkat → buat lokasi pertama → tambah posisi pertama. Bisa dilewati.

### 4.3 Ringkasan (beranda)
Header frosted: avatar akun (kiri), logo + wordmark **arta** (tengah), tombol mata (sembunyikan nominal) di kanan. Di bawahnya tab: **Ringkasan · Kelas aset · Lokasi** (indikator garis aksen).

Konten tab **Ringkasan**, berurutan:
1. **Kartu total kekayaan** (latar `wash`): label "Total kekayaan" + chip mata uang dasar, angka besar, perubahan hari ini (nominal + %), grafik garis, pemilih rentang **1H · 1B · 3B · 1T · Semua**.
2. **Tiga statistik kecil**: Bulan ini (%), Tahun ini (%), jumlah Lokasi.
3. **Kartu insight Advisor**: satu kalimat terpenting hari ini (dari engine analytics lokal, bukan LLM). Tap → tab Advisor.
4. **Alokasi**: bar bertumpuk per kelas (warna kelas) + legenda 3 kolom. Tautan "Per lokasi →" pindah ke tab Lokasi.
5. **Kelas aset**: daftar baris (ilustrasi kelas, nama, bar porsi, %, nilai, perubahan hari ini).

Tab **Kelas aset**: daftar kelas saja. Tap kelas → daftar posisi di kelas itu (lintas lokasi).
Tab **Lokasi**: ringkasan "N lokasi · terbesar X (p%)", lalu daftar lokasi (avatar inisial berwarna kelas dominan, nama, "Kelas · p%", nilai, perubahan). Lokasi yang melewati batas konsentrasi diberi label merah "Di atas batas 25%".

Tombol mata: semua nominal jadi `Rp ••••`; persentase tetap terlihat. Ada setelan "Sembunyikan nominal saat dibuka".

### 4.4 Detail lokasi
- Header: inisial, nama, jenis lokasi, total nilai, perubahan hari ini.
- Bar porsi dari total kekayaan + penanda batas konsentrasi + peringatan bila lewat.
- Ringkasan untung/rugi belum terealisasi ("dihitung dari X dari Y posisi" — posisi tanpa riwayat beli tidak dihitung).
- Daftar posisi: simbol, jumlah + harga terakhir, nilai, perubahan hari ini, P/L (atau label "Tanpa riwayat beli").
- Aksi: Catat beli/jual, Tambah posisi, Edit/arsipkan lokasi.

### 4.5 Detail posisi
- Jumlah, harga terakhir + waktu & sumber harga (tanda "basi" bila lewat ambang), nilai, harga rata-rata & P/L (jika ada lot), riwayat transaksi, ubah jumlah cepat.

### 4.6 Tambah posisi (bottom sheet bertahap)
1. Pilih kelas aset (grid ilustrasi).
2. Cari instrumen (BBCA, bitcoin, emas Antam, dst.) atau buat manual.
3. Pilih lokasi (atau buat baru).
4. Isi jumlah (lot/gram/koin/saldo).
5. Opsional: "Catat harga beli" (harga, tanggal, biaya).

### 4.7 Simulasi (what-if)
- Kartu hasil: "Kekayaan setelah skenario", selisih nominal & %, kalimat eksposur USD.
- Preset: **Pasar turun** (kripto −30, IHSG −10, emas +5, USD/IDR +5), **Rupiah melemah** (IHSG −5, USD/IDR +10), **Reset**.
- Slider (step 5%): Kripto (harga USD) −50…+50, IHSG −30…+30, Emas (harga USD) −20…+20, Kurs USD/IDR −10…+20.
- Dampak per kelas aset: bar divergen (merah kiri / hijau kanan) + nominal.
- Model: lihat `ARCHITECTURE.md §6.4`.

### 4.8 Advisor
- Header frosted: riwayat (kiri), judul "Advisor" + status "Membaca N lokasi · harga HH.MM", percakapan baru (kanan).
- Pesan AI tanpa gelembung (avatar sparkle kecil), pesan pengguna gelembung warna aksen.
- Kartu tertanam di jawaban (mis. kartu simulasi dengan bar sebelum/sesudah + tombol "Buka simulasi →").
- Composer frosted di atas nav: chip saran (Cek konsentrasi, Bandingkan dengan target, Eksposur USD), input, tombol kirim.
- Teks kecil tetap: "Informasi dari datamu, bukan nasihat keuangan berlisensi."
- Jika API key kosong / offline / error: **mode template** — jawab chip saran dengan kalimat template berisi angka dari engine lokal; input bebas menampilkan pesan bahwa AI belum dikonfigurasi.

### 4.9 Pengaturan
Kartu akun ("Akun Arta · Data tersimpan di perangkat · terenkripsi"), lalu bagian:
- **Tampilan:** Mata uang dasar (IDR), Tema (Ikuti sistem/Terang/Gelap), Bahasa.
- **Harga & data:** Update harga (5/15/30/60 menit; WorkManager minimal 15 menit — 5 menit hanya saat aplikasi terbuka), Acuan harga emas (Buyback per gram manual / Spot XAU), Tandai harga basi (toggle).
- **Batas & peringatan:** Batas konsentrasi per lokasi (default 25%), Target alokasi per kelas, Notifikasi pergerakan harian > X% (toggle).
- **AI Advisor:** Boleh membaca portofolio (toggle; jika off, Advisor hanya mode template), Insight harian di Ringkasan (toggle), Hapus riwayat percakapan.
- **Keamanan & data:** Kunci dengan sidik jari (toggle), Sembunyikan nominal saat dibuka (toggle), Kunci otomatis setelah (menit), Ekspor data (CSV), Hapus semua data (konfirmasi ganda).
- Footer: "arta v0.1 · harga dari sumber publik, bisa tertunda".

## 5. Data contoh (untuk preview & mode demo)

Total Rp 1.284.560.000, naik 0,49% (+Rp 6.294.300) hari ini.

| Kelas | % | Nilai | Hari ini |
|---|---|---|---|
| Saham BEI | 32 | Rp 411,1 jt | +1,5% |
| Emas | 22 | Rp 282,6 jt | +0,8% |
| Kripto | 18 | Rp 231,2 jt | −1,4% |
| Tabungan | 16 | Rp 205,5 jt | 0,0% |
| ETF | 7 | Rp 89,9 jt | +1,1% |
| Reksa dana | 5 | Rp 64,2 jt | +0,1% |

Lokasi: Stockbit 32% (saham), Indodax 18% (kripto), Brankas rumah 14% (emas fisik), BCA 10% (tabungan), Pegadaian 8% (emas digital), Gotrade 7% (ETF AS), Bank Jago 6% (tabungan), Bibit 5% (reksa dana).

Posisi Stockbit: BBCA 150 lot @9.250 (avg 8.400), BMRI 120 lot @6.100 (avg 5.450), TLKM 200 lot @2.980 (avg 3.400), ASII 100 lot @5.200 (avg 4.850), ANTM 250 lot @1.620 (tanpa riwayat beli), kas RDN Rp 47.009.200.

Sediakan flag build/debug "Isi data demo" agar layar bisa diuji tanpa input manual. Data demo tidak boleh ada di build release kecuali diaktifkan manual.

## 6. Di luar cakupan v0.1
- Sync antar perangkat, akun cloud, login.
- Pencatatan pengeluaran harian.
- Integrasi langsung ke broker/bank.
- Widget layar utama (rencana v0.2, Glance).
