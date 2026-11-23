# Arta — Design system

Arah visual: **frosted minimal**. Latar polos, header & navigasi bawah semi-transparan dan buram (konten terlihat samar lewat di baliknya saat di-scroll), satu warna aksen **teal tua (petrol)**, ilustrasi kelas aset sebagai sumber warna kedua. Hijau/merah hanya untuk naik/turun nilai dan peringatan.

Referensi visual pasti: `design/FrostRingkasan.dc.html`, `design/FrostAdvisor.dc.html`, `design/FrostPengaturan.dc.html` (nilai token di bawah diambil dari file-file itu).

## 1. Warna

| Token | Terang | Gelap | Pemakaian |
|---|---|---|---|
| `bg` | `#FCFCFD` | `#0B0C0F` | Latar layar |
| `surf` | `#F1F3F6` | `#181A1F` | Input, avatar, chip netral |
| `on` | `#10131A` | `#E9EBEF` | Teks utama |
| `muted` | `#5A6372` | `#8E96A1` | Teks sekunder, ikon nav tidak aktif |
| `hair` | `#E7EAEE` | `#22252B` | Garis pemisah 1 dp |
| `hairStrong` | `#D3D7DD` | `#343840` | Border chip |
| `accent` | `#0B6E69` | `#4FD1C5` | FAB, tab/nav aktif, grafik, rentang terpilih, tombol kirim, switch on |
| `onAccent` | `#FFFFFF` | `#0B0C0F` | Teks/ikon di atas aksen |
| `wash` | `#E3F3F0` | `#0E2624` | Kartu total kekayaan, kartu akun, latar ikon Advisor, kartu simulasi |
| `up` | `#15803D` | `#4CC38A` | Nilai naik |
| `down` | `#C2362B` | `#FF7A6E` | Nilai turun, aksi berbahaya |
| `downWash` | `#FBE9E7` | `#3A1A17` | Latar label peringatan |
| `track` | `#10131A` @8% | `#FFFFFF` @10% | Track bar porsi |

**Frosted surface** (header, bottom nav, composer): `bg` dengan alpha 0.76 (terang) / 0.74 (gelap) + blur 20 dp + saturasi ±180%, border 1 dp `hair` di sisi yang berbatasan dengan konten. Mode ringan (lihat ARCHITECTURE §8): alpha 0.94 tanpa blur.

### Warna kelas aset (bar alokasi, bar porsi, tile ikon)

| Kelas | Terang | Gelap | Tile terang (alpha) | Tile gelap (alpha) |
|---|---|---|---|---|
| Saham BEI | `#4A67C9` | `#8AA0F0` | 10% | 15% |
| Emas | `#C99A2E` | `#E5BE5C` | 12% | 15% |
| Kripto | `#7E63CF` | `#AE98F0` | 10% | 15% |
| Tabungan | `#3E9A8C` | `#6FCDBE` | 10% | 14% |
| ETF | `#C25E8C` | `#EB9BC0` | 10% | 15% |
| Reksa dana | `#6E7886` | `#A4ADB9` | 10% | 14% |
| Lainnya | `#8C7A6B` | `#C2B3A6` | 10% | 14% |

Ikon di Pengaturan memakai pasangan tile/ikon dari palet ini (warna ikon versi lebih gelap di mode terang, lebih terang di mode gelap).

## 2. Tipografi — Plus Jakarta Sans

| Gaya | Ukuran / tebal | Catatan |
|---|---|---|
| Wordmark "arta" | 26 / 800, tracking −0.05em | Huruf kecil |
| Judul halaman | 26 / 800, tracking −0.03em | "Pengaturan" |
| Judul header tengah | 20 / 800 | "Advisor" |
| Angka total kekayaan | 34 / 800, tracking −0.03em, tabular | Muat "Rp 1.284.560.000" di 412 dp |
| Judul bagian | 19 / 800, tracking −0.02em | "Alokasi", "Kelas aset" |
| Nama baris | 16 / 600 | |
| Isi | 15–16 / 400 | Chat 15.5, line-height 1.6 |
| Nilai di baris | 15 / 600, tabular | |
| Label kecil/caption | 12–13 / 400–600 | Warna `muted` |
| Tab | 15 / 700 aktif, 500 tidak aktif | |

Angka selalu `tabular-nums` (Compose: `fontFeatureSettings = "tnum"`).

## 3. Spasi, radius, ukuran

- Padding layar horizontal 16 dp. Baris daftar 72–78 dp, padding 16.
- Radius: kartu besar 24–26, tile ikon 16, avatar lokasi bulat, chip & tombol rentang penuh (pill), FAB 20, input 26, kartu simulasi 18, gelembung chat 20 (sudut bawah kanan 6 untuk pengguna).
- Header Ringkasan: inset status bar + baris 52 dp + tab 51 dp. Bottom nav: 64 dp + inset gesture (±16). FAB 58×58 dp, 16 dp dari kanan, 16 dp di atas nav.
- Target sentuh minimal 44–48 dp.

## 4. Komponen kunci

- **FrostedTopBar / FrostedBottomBar / FrostedComposer**: Haze `hazeSource` di konten scroll, `hazeEffect` di bar.
- **TotalCard**: latar `wash`, chip mata uang (latar `bg`, teks `accent`), angka besar, perubahan (`up`/`down` + "hari ini"), grafik garis aksen + area aksen 12%, RangeSelector.
- **RangeSelector**: 5 tombol pill; terpilih = latar `accent`, teks `onAccent`, tebal 700; lainnya transparan `muted`.
- **StatRow**: 3 kolom dipisah garis vertikal `hair`.
- **InsightRow**: tile 40 dp `wash` + sparkle `accent`, "Advisor · hari ini", kalimat, chevron.
- **AllocationBar**: tinggi 16 dp, gap 3 dp, segmen ujung membulat 8 dp, warna kelas; legenda grid 3 kolom (titik 8 dp, nama `muted`, angka tebal `on`).
- **AssetClassRow**: tile 52 dp (warna tile kelas) + ilustrasi 40–42 dp, nama, bar porsi 84×5 dp (warna kelas), %, nilai, perubahan.
- **LocationRow**: avatar bulat 46 dp (tile + huruf warna kelas dominan, 19/700), nama, "Kelas · p%", label peringatan opsional (`down` di atas `downWash`), nilai, perubahan.
- **Tabs (header)**: 3 tab rata, indikator garis 56×4 dp radius 2 warna `accent` di bawah tab aktif.
- **BottomNav**: ikon outline 25 dp, aktif = `accent` stroke 2.4 + titik 5 dp di bawah; tidak aktif = `muted` stroke 1.8. Tanpa label (sertakan contentDescription).
- **Chat**: pesan AI tanpa gelembung (avatar 32 dp radius 11 latar `wash`, sparkle `accent`); pesan pengguna gelembung `accent` teks `onAccent`; kartu simulasi latar `wash` dengan bar `accent` dan bagian rugi `down`.
- **SuggestionChip**: pill 36 dp, border `hairStrong`, sparkle kecil `accent` + label `on`.
- **Switch**: 52×32; on = track `accent`, thumb `onAccent` 24 dp dengan centang `accent`; off = border 2 dp abu, thumb 16 dp abu.
- **SettingsRow**: tile ikon 38 dp radius 12, label 16, nilai di bawah label 14 `muted`; judul bagian 13/700 warna `accent`.

## 5. Ilustrasi kelas aset

Ilustrasi sudah dibuat sebagai SVG di `design/FrostRingkasan.dc.html` (cari blok `isSaham`, `isEmas`, `isKripto`, `isTabungan`, `isEtf`, `isRd`). Konversi masing-masing ke **VectorDrawable** (`res/drawable/ic_asset_stock.xml`, `ic_asset_gold.xml`, `ic_asset_crypto.xml`, `ic_asset_savings.xml`, `ic_asset_etf.xml`, `ic_asset_mutual_fund.xml`), viewport 48×48:

- **Saham**: lembar dokumen dengan sudut terlipat, dua garis teks, grafik naik hijau.
- **Emas**: tiga batang emas bertumpuk (2 bawah, 1 atas), sisi atas lebih terang.
- **Kripto**: dua koin bertumpuk ungu dengan simbol ₿ putih (ubah teks ₿ menjadi path saat konversi).
- **Tabungan**: celengan babi teal dengan koin emas di atas slot.
- **ETF**: diagram pie dengan 4 irisan nuansa merah muda.
- **Reksa dana**: kantong uang abu dengan tulisan "Rp" (ubah jadi path).
- **Lainnya** (belum ada di mockup): buat dengan gaya sama, mis. rumah sederhana warna `#8C7A6B`.

Ilustrasi memakai warna tetap (sama di terang & gelap); hanya tile di belakangnya yang mengikuti tema.

## 6. Logo

Koin bulat warna `accent` dengan cincin tipis `onAccent` 35% dan huruf "A" garis (stroke 2.2, ujung membulat) di tengah; di sebelahnya wordmark "arta". Lihat SVG di header `FrostRingkasan.dc.html`. Buat juga adaptive icon (foreground = koin A, background = `accent`) dan monochrome icon untuk themed icons Android.

## 7. Tablet (Galaxy Tab A9, lanskap)

- Navigation rail 92 dp di kiri (FAB di atas, 4 item ikon + label 12 dp, indikator pill 56×32 latar `wash`).
- Ringkasan: dua kolom — kiri (TotalCard + Alokasi berdampingan, lalu grid kelas aset 2 kolom), kanan panel Advisor 392 dp.
- Efek kaca mode ringan (lihat ARCHITECTURE §8).
