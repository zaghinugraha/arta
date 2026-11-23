# Mockup referensi

File `.dc.html` di folder ini adalah mockup final dari kanvas desain (versi Arta, frosted, Plus Jakarta Sans, aksen petrol). Mereka **bukan kode produksi** dan tidak bisa dibuka langsung di browser karena butuh runtime kanvas.

Cara membacanya:
- Markup di dalam `<x-dc>` adalah tata letak. Nilai `{{ ... }}` diisi dari fungsi `renderVals()` di blok `<script type="text/x-dc">` di bagian bawah file.
- Objek `TH.light` / `TH.dark` di script = token warna (sama dengan `docs/DESIGN.md`).
- `<sc-for>` = perulangan, `<sc-if>` = kondisi.
- SVG ilustrasi kelas aset ada di `FrostRingkasan.dc.html` (blok `isSaham`, `isEmas`, `isKripto`, `isTabungan`, `isEtf`, `isRd`) — sumber untuk VectorDrawable.
- Logo (koin "A") ada di header `FrostRingkasan.dc.html` dan kartu akun `FrostPengaturan.dc.html`.

| File | Layar |
|---|---|
| `FrostRingkasan.dc.html` | Ringkasan (tab Ringkasan / Kelas aset / Lokasi) |
| `FrostAdvisor.dc.html` | AI Advisor |
| `FrostPengaturan.dc.html` | Pengaturan |
| `Simulasi.dc.html` | Simulasi what-if (logika & tata letak; gaya visual lama — terapkan token Arta) |
| `Lokasi-Stockbit.dc.html` | Detail lokasi (logika & tata letak; gaya visual lama — terapkan token Arta) |

Ukuran artboard ponsel: 412×891 dp.
