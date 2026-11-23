# Arta — Arsitektur teknis

## 1. Stack

| Kebutuhan | Pilihan | Catatan |
|---|---|---|
| Bahasa | Kotlin | |
| UI | Jetpack Compose + Material 3 | Single activity, Navigation Compose |
| Efek frosted | `dev.chrisbanes.haze` (haze + haze-blur) | Header, bottom nav, composer Advisor. Lihat §8 untuk mode ringan |
| Async | Kotlin Coroutines + Flow | StateFlow di ViewModel |
| DI | Koin (atau manual) | |
| Database | Room + `net.zetetic:sqlcipher-android` + `androidx.sqlite` | Integrasi via `SupportOpenHelperFactory` (Room 2) atau `SQLCipherDriver` (Room 3) |
| Setelan non-sensitif | Jetpack DataStore (Preferences) | Tema, interval, batas, toggle. Bukan untuk rahasia |
| Kunci | Android Keystore (AES-256-GCM, StrongBox jika tersedia) | Membungkus passphrase DB |
| Biometrik | `androidx.biometric` (BiometricPrompt) | |
| Jaringan | Ktor client (OkHttp engine) + kotlinx.serialization | Timeout pendek, retry terbatas |
| Kerja latar | WorkManager | Update harga periodik + snapshot harian |
| Grafik | Gambar sendiri dengan Compose Canvas | Garis, area, bar bertumpuk — ringan, tanpa library chart |
| Font | Plus Jakarta Sans (bundled `res/font`, OFL) | 400/500/600/700/800 |
| Uji | JUnit, kotlinx-coroutines-test, Turbine, Room in-memory | |

Semua versi: pakai **versi stabil terbaru** saat membuat version catalog.

## 2. Struktur proyek

Satu modul `:app` dengan pembagian package (cukup untuk app pribadi; bisa dipecah modul nanti):

```
id.shoko.arta
├── App.kt                       // Application, init Koin, WorkManager
├── MainActivity.kt              // FLAG_SECURE opsional, lock gate, NavHost
├── core/
│   ├── money/                   // Money, BigDecimal helpers, format IDR (id-ID)
│   ├── security/                // KeystoreManager, PassphraseStore, BiometricGate
│   ├── time/                    // Clock abstraction (testable)
│   └── ui/                      // Theme, tokens, typography, komponen bersama
├── data/
│   ├── db/                      // ArtaDatabase, entities, DAOs, converters, migrations
│   ├── prices/                  // PriceProvider interface + implementasi, PriceRepository
│   ├── fx/                      // FxProvider + FxRepository
│   ├── settings/                // SettingsRepository (DataStore)
│   └── repo/                    // PortfolioRepository, LocationRepository, AdvisorRepository
├── domain/
│   ├── valuation/               // ValuationEngine
│   ├── analytics/               // Concentration, Allocation, UsdExposure, Simulation
│   └── advisor/                 // AdvisorService, PromptBuilder, TemplateResponder
├── llm/
│   ├── LlmClient.kt             // interface
│   ├── ProviderLlmClient.kt     // PLACEHOLDER — diisi pemilik (lihat §7)
│   └── LlmConfig.kt             // baca BuildConfig
├── work/                        // PriceRefreshWorker, DailySnapshotWorker
└── feature/
    ├── lock/  onboarding/  summary/  location/  position/
    ├── addposition/  simulation/  advisor/  settings/
```

## 3. Konfigurasi build & placeholder API key

`local.properties` (tidak di-commit) — lihat `local.properties.example`:

```properties
LLM_API_KEY=
LLM_BASE_URL=
LLM_MODEL=
```

`app/build.gradle.kts`:

```kotlin
val localProps = java.util.Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun lp(key: String) = (localProps[key] as String?).orEmpty()

android {
    buildFeatures { buildConfig = true; compose = true }
    defaultConfig {
        buildConfigField("String", "LLM_API_KEY", "\"${lp("LLM_API_KEY")}\"")
        buildConfigField("String", "LLM_BASE_URL", "\"${lp("LLM_BASE_URL")}\"")
        buildConfigField("String", "LLM_MODEL", "\"${lp("LLM_MODEL")}\"")
    }
    buildTypes {
        release { isMinifyEnabled = true; isShrinkResources = true }
    }
}
```

Catatan: key di `BuildConfig` ikut tertanam di APK. Dapat diterima untuk pemakaian pribadi (APK tidak dibagikan, pasang spending limit di provider). Jika kelak dibagikan, ganti dengan input key di Pengaturan yang disimpan terenkripsi Keystore.

## 4. Skema database (Room + SQLCipher)

Semua uang/kuantitas: `TEXT` berisi desimal (`BigDecimal.toPlainString()`), dikonversi via `TypeConverter`. Waktu: `INTEGER` epoch millis UTC. ID: `TEXT` UUID.

### `location`
| kolom | tipe | catatan |
|---|---|---|
| id | TEXT PK | |
| name | TEXT | "Stockbit" |
| type | TEXT | `BROKER`, `CRYPTO_EXCHANGE`, `BANK`, `PHYSICAL`, `PLATFORM`, `WALLET`, `OTHER` |
| default_currency | TEXT | ISO 4217, default `IDR` |
| note | TEXT? | |
| sort_order | INTEGER | |
| archived | INTEGER | 0/1 |
| created_at, updated_at | INTEGER | |

### `instrument`
| kolom | tipe | catatan |
|---|---|---|
| id | TEXT PK | |
| asset_class | TEXT | `STOCK_IDX`, `ETF`, `MUTUAL_FUND`, `CRYPTO`, `GOLD`, `CASH`, `OTHER` |
| symbol | TEXT | `BBCA`, `bitcoin`, `XAU`, `IDR`… |
| name | TEXT | |
| quote_currency | TEXT | mata uang harga |
| unit | TEXT | `LOT`, `SHARE`, `UNIT`, `COIN`, `GRAM`, `CURRENCY` |
| units_per_lot | TEXT | "100" untuk saham BEI, "1" lainnya |
| price_source | TEXT | `YAHOO`, `COINGECKO`, `GOLD_SPOT`, `MANUAL`, `NONE` (CASH) |
| source_ref | TEXT? | `BBCA.JK`, `bitcoin`, dst. |
| UNIQUE(asset_class, symbol) | | |

### `position`
| kolom | tipe | catatan |
|---|---|---|
| id | TEXT PK | |
| location_id | TEXT FK → location | |
| instrument_id | TEXT FK → instrument | |
| quantity | TEXT | dalam `unit` instrumen (lot untuk saham BEI) |
| note | TEXT? | |
| archived | INTEGER | |
| created_at, updated_at | INTEGER | |
| UNIQUE(location_id, instrument_id) | | |

### `txn` (riwayat opsional)
| kolom | tipe | catatan |
|---|---|---|
| id | TEXT PK | |
| position_id | TEXT FK | |
| type | TEXT | `BUY`, `SELL`, `DEPOSIT`, `WITHDRAW`, `DIVIDEND`, `ADJUST` |
| quantity | TEXT | |
| price | TEXT? | per unit, di `currency` |
| fee | TEXT? | |
| currency | TEXT | |
| executed_at | INTEGER | |
| note | TEXT? | |

Aturan: `position.quantity` adalah **sumber kebenaran**. Transaksi BUY/SELL juga memperbarui quantity (dalam satu transaksi DB). Posisi "punya riwayat" jika ada ≥1 BUY; rata-rata harga = metode *average cost*.

### `price_latest`
`instrument_id` PK, `price` TEXT, `currency` TEXT, `as_of` INTEGER (waktu harga di sumber), `fetched_at` INTEGER, `source` TEXT.

### `price_history`
PK (`instrument_id`, `date` TEXT `yyyy-MM-dd`), `close` TEXT, `currency` TEXT. Satu baris per hari untuk grafik.

### `fx_rate`
PK (`base`, `quote`), `rate` TEXT, `as_of`, `fetched_at`, `source`. Simpan minimal USD→IDR; tambahkan pasangan lain sesuai `quote_currency` yang dipakai.

### `snapshot_daily` + `snapshot_item`
- `snapshot_daily`: `date` PK, `total_base` TEXT, `base_currency` TEXT, `created_at`.
- `snapshot_item`: PK (`date`, `asset_class`), `value_base` TEXT.
Diisi DailySnapshotWorker (dan saat app dibuka bila hari ini belum ada). Dasar grafik 1B/3B/1T/Semua dan statistik Bulan ini/Tahun ini.

### `target_allocation`
`asset_class` PK, `percent` TEXT. Total harus 100 (validasi di UI).

### `advisor_message`
`id` PK, `conversation_id` TEXT, `role` (`USER`/`ASSISTANT`), `content` TEXT, `card_json` TEXT?, `created_at`.

### DataStore (non-sensitif)
`base_currency`, `theme`, `language`, `price_interval_min`, `gold_reference` (`BUYBACK_MANUAL`/`SPOT`), `mark_stale`, `concentration_limit_pct` (25), `notify_move_pct` (3), `advisor_read_portfolio`, `advisor_daily_insight`, `biometric_lock`, `hide_amounts_on_open`, `auto_lock_min`, `glass_mode` (`AUTO`/`FULL`/`LIGHT`/`OFF`).

## 5. Harga & kurs

### 5.1 Abstraksi
```kotlin
interface PriceProvider {
    val source: PriceSource
    suspend fun fetch(instruments: List<Instrument>): List<PriceQuote> // batch
}
data class PriceQuote(val instrumentId: String, val price: BigDecimal,
                      val currency: String, val asOf: Instant)
interface FxProvider { suspend fun fetch(base: String, quotes: Set<String>): List<FxQuote> }
```

### 5.2 Implementasi awal (semua dipanggil langsung dari perangkat)
- **CoinGecko** (kripto): endpoint harga sederhana, `vs_currencies=idr,usd`, batch semua id koin dalam satu panggilan.
- **Yahoo Finance** (saham BEI `.JK`, ETF): endpoint chart/quote tidak resmi. Bungkus rapat di balik interface karena bisa berubah; tangani 4xx/5xx dengan menandai harga basi, bukan crash.
- **Kurs**: Frankfurter (kurs referensi ECB, mencakup IDR) atau penyedia kurs gratis lain. Kurs ECB hanya update hari kerja — tampilkan tanggalnya.
- **Emas**:
  - `BUYBACK_MANUAL` (default): pengguna mengisi harga buyback per gram (Antam/Pegadaian) di detail instrumen; tampilkan "diisi manual · tanggal".
  - `GOLD_SPOT`: harga spot XAU/USD per troy ounce dari penyedia publik × USD/IDR ÷ 31,1034768 = harga per gram. Biarkan provider sebagai implementasi yang mudah diganti.
- **Reksa dana & aset lain**: `MANUAL` (NAB/nilai diisi pengguna).

**Wajib**: sebelum implementasi, verifikasi endpoint, batas rate, dan ketentuan pemakaian tiap sumber. Semua provider harus bisa dimatikan/diganti tanpa menyentuh UI.

### 5.3 Penjadwalan
- Saat app dibuka/di-resume: refresh jika data > 5 menit.
- WorkManager periodik sesuai setelan (minimal 15 menit — batas Android), constraint `NetworkType.CONNECTED`, backoff eksponensial.
- DailySnapshotWorker sekali sehari (setelah ±16.30 WIB agar penutupan BEI tercakup).
- **Harga basi**: lewat 2× interval (atau > 1 hari kerja untuk sumber harian) → tampilkan harga terakhir + label waktu dengan warna peringatan. Jangan pernah mengosongkan nilai.

## 6. Domain

### 6.1 Valuasi
```
nilai_posisi_base = quantity × units_per_lot × price × fx(quote_currency → base)
CASH: nilai = quantity × fx(currency → base)
```
- Rounding hanya saat tampil (IDR 0 desimal untuk nominal besar; ringkas "Rp 411,1 jt", "Rp 1,28 M").
- Perubahan hari ini = nilai sekarang − nilai dengan harga penutupan kemarin (dari `price_history`) untuk instrumen berharga; 0 untuk CASH/MANUAL.

### 6.2 Untung/rugi
- Average cost dari BUY (+fee); SELL mengurangi quantity, realized P/L dicatat terpisah.
- Unrealized P/L hanya untuk posisi yang punya riwayat; UI menyebut "dihitung dari X dari Y posisi".

### 6.3 Analytics (dipakai Ringkasan, Advisor, notifikasi)
- **Konsentrasi**: porsi tiap lokasi = nilai_lokasi / total. Lewat `concentration_limit_pct` → flag. Kelebihan nominal = nilai_lokasi − batas × total.
- **Alokasi vs target**: selisih % dan nominal per kelas.
- **Eksposur USD**: porsi nilai yang harganya berbasis USD (kripto, ETF luar negeri, emas spot/berbasis USD, kas valas USD).
- **Insight harian**: pilih satu kalimat prioritas tertinggi: (1) konsentrasi lewat batas, (2) gerak harian > `notify_move_pct`, (3) alokasi menyimpang > 5 poin dari target, (4) harga basi.

### 6.4 Simulasi what-if
Input: k (kripto, %), i (IHSG, %), e (emas USD, %), u (USD/IDR, %).
```
saham'   = saham × (1 + i)
rd'      = rd × (1 + 0,5 × i)          // asumsi reksa dana campuran; buat konstanta yang bisa diatur
kripto'  = kripto × (1 + k) × (1 + u)
emas'    = emas × (1 + e) × (1 + u)
etf'     = etf × (1 + u)
tabungan'= tabungan                     // valas: × (1 + u) untuk bagian USD
```
Output: total baru, selisih, selisih per kelas. Engine yang sama dipakai kartu simulasi di Advisor.

## 7. AI Advisor & placeholder LLM

### 7.1 Prinsip
- **Semua angka dihitung lokal** (§6). LLM hanya menyusun kalimat dari ringkasan yang diberikan. LLM tidak boleh menjadi sumber angka.
- Jika `LLM_API_KEY` kosong, toggle "Boleh membaca portofolio" mati, offline, atau error → **TemplateResponder** (kalimat template + angka lokal).

### 7.2 Interface
```kotlin
interface LlmClient {
    val isConfigured: Boolean
    suspend fun complete(system: String, messages: List<ChatTurn>): Result<String>
}
data class ChatTurn(val role: Role, val content: String) // Role.USER / Role.ASSISTANT

object LlmConfig {
    val apiKey get() = BuildConfig.LLM_API_KEY
    val baseUrl get() = BuildConfig.LLM_BASE_URL
    val model get() = BuildConfig.LLM_MODEL
    val isConfigured get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}
```

`ProviderLlmClient` dibuat sebagai **placeholder** yang jelas:
```kotlin
class ProviderLlmClient(private val http: HttpClient) : LlmClient {
    override val isConfigured get() = LlmConfig.isConfigured
    override suspend fun complete(system: String, messages: List<ChatTurn>): Result<String> {
        if (!isConfigured) return Result.failure(IllegalStateException("LLM belum dikonfigurasi"))
        // TODO(owner): sesuaikan endpoint, header autentikasi, dan format body/response
        //              dengan dokumentasi provider LLM yang dipilih.
        //              Gunakan LlmConfig.baseUrl, LlmConfig.apiKey, LlmConfig.model.
        TODO("Implementasi request ke provider LLM")
    }
}
```
Sertakan komentar TODO yang mudah dicari (`TODO(owner)`), dan jangan pernah mencetak key/response ke log.

### 7.3 Data yang dikirim
`PromptBuilder` membuat JSON ringkas, contoh:
```json
{
  "base_currency": "IDR",
  "as_of": "2026-11-23T06:48:00+07:00",
  "total_change_today_pct": 0.49,
  "allocation_pct": {"STOCK_IDX": 32, "GOLD": 22, "CRYPTO": 18, "CASH": 16, "ETF": 7, "MUTUAL_FUND": 5},
  "target_pct": {"STOCK_IDX": 30, "GOLD": 20, "CRYPTO": 15, "CASH": 20, "ETF": 10, "MUTUAL_FUND": 5},
  "largest_location_pct": 32,
  "concentration_limit_pct": 25,
  "usd_exposure_pct": 47,
  "simulation": {"crypto": -30, "total_change_pct": -5.4}
}
```
Tanpa nama lokasi, tanpa nominal rupiah persis (cukup persen; nominal dihitung lokal lalu disisipkan ke jawaban oleh aplikasi bila perlu, mis. placeholder `{excess_amount}` yang diganti `TemplateFormatter`).

### 7.4 System prompt awal (bisa disunting di `PromptBuilder`)
> Kamu adalah asisten informasi portofolio di aplikasi Arta. Jawab dalam bahasa Indonesia yang singkat dan jelas. Gunakan hanya angka yang ada di data JSON; jangan mengarang angka. Jelaskan dampak dan pilihan secara netral, bukan rekomendasi beli/jual spesifik. Jika data tidak cukup, katakan apa yang kurang. Akhiri tanpa disclaimer panjang; aplikasi sudah menampilkannya.

### 7.5 Chip saran (juga dasar TemplateResponder)
- **Cek konsentrasi** → lokasi terbesar, batas, kelebihan nominal.
- **Bandingkan dengan target** → kelas yang lebih/kurang dari target (persen + nominal).
- **Eksposur USD** → porsi USD + dampak rupiah ±5%.
- Pertanyaan "kalau X turun Y%" → parsing sederhana → jalankan Simulasi → kartu simulasi.

## 8. Kinerja & efek kaca

- `glass_mode = AUTO`: blur penuh (Haze) di perangkat kelas atas; **mode ringan** (latar semi-opak tanpa blur, atau radius kecil) bila `ActivityManager.isLowRamDevice()`, RAM total < 6 GB, atau refresh rate ≤ 60 Hz. Tab A9 harus jatuh ke mode ringan.
- Grafik digambar Canvas; hindari rekomposisi per frame. Daftar memakai `LazyColumn` dengan `key`.
- Target cold start < 1 dtk di S25 FE; tidak ada pekerjaan jaringan di main thread; buka DB setelah unlock.

## 9. Keamanan

- **Passphrase DB**: 32 byte acak (`SecureRandom`), dienkripsi AES-GCM dengan kunci Keystore (`setUserAuthenticationRequired` sesuai setelan biometrik, `setIsStrongBoxBacked(true)` dengan fallback TEE), ciphertext + IV disimpan di file privat aplikasi.
- Jika kunci Keystore hilang/invalid (mis. sidik jari baru didaftarkan) → tampilkan pemulihan: data tidak bisa dibuka; tawarkan hapus & mulai ulang. **Ekspor CSV berkala** adalah satu-satunya cadangan (sengaja, karena backup cloud dimatikan).
- `FLAG_SECURE` opsional (setelan "Blokir tangkapan layar", default mati — keputusan pemilik), `allowBackup=false`, `dataExtractionRules` exclude semua, `usesCleartextTraffic=false`, network security config hanya HTTPS.
- Ekspor CSV: simpan via Storage Access Framework (pengguna memilih lokasi); peringatkan bahwa file tidak terenkripsi.
- Tidak ada izin selain `INTERNET`, `USE_BIOMETRIC`, `POST_NOTIFICATIONS` (opsional).
- R8/minify aktif di release; hapus log debug di release.

## 10. Pengujian minimum

- Unit: ValuationEngine (lot, gram, kurs, CASH valas), average cost & P/L, Concentration, Allocation vs target, UsdExposure, Simulation (cocokkan contoh: kripto −30% → total −5,4% pada data demo), parser respons tiap provider (pakai fixture JSON).
- Instrumented: buka DB terenkripsi dengan passphrase benar/salah; migrasi Room.
- UI: preview Compose semua layar dalam terang/gelap dan lebar 412 dp & 1196 dp.
