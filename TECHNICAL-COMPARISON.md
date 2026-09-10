# Perbandingan Teknis — Laravel vs Spring Boot

Dua versi dari **MamiKos Kost API** yang dibuat dari spesifikasi yang sama (endpoint, kode status,
kode error, dan aturan bisnis sama), lalu dibandingkan.

| | Project A | Project B |
|---|---|---|
| Folder | [`mamikosLaravel/`](mamikosLaravel) | [`mamikosJava/`](mamikosJava) |
| Bahasa | PHP 8.3 | Java 21 |
| Framework | Laravel 13 | Spring Boot 3.5 |
| Database | MySQL 8 | PostgreSQL 16 |
| Cache / rate limit | Redis 7 | Redis 7 |
| Login/Auth | Token Sanctum (disimpan di DB) | JWT + refresh token |
| Migrasi database | Laravel migration (bisa di-rollback) | Flyway (satu arah, tidak bisa rollback) |

Cara install & jalankan kedua project ada di [README.md](README.md).

---

## Daftar isi

1. [Ringkasan & rekomendasi](#1-ringkasan--rekomendasi)
2. [Cara pengujian](#2-cara-pengujian)
3. [Hasil pengukuran](#3-hasil-pengukuran)
4. [Struktur kode](#4-struktur-kode)
5. [Keamanan saat request bersamaan (race condition)](#5-keamanan-saat-request-bersamaan-race-condition)
6. [Keamanan aplikasi](#6-keamanan-aplikasi)
7. [Testing](#7-testing)
8. [Performa & pemakaian resource](#8-performa--pemakaian-resource)
9. [Enaknya jadi developer di masing-masing stack](#9-enaknya-jadi-developer-di-masing-masing-stack)
10. [Siap produksi?](#10-siap-produksi)
11. [Maintenance & cari SDM](#11-maintenance--cari-sdm)
12. [Bug yang muncul di masing-masing stack](#12-bug-yang-muncul-di-masing-masing-stack)
13. [Kesimpulan](#13-kesimpulan)
14. [Lampiran A — perbedaan response API](#lampiran-a--perbedaan-response-api)
15. [Lampiran B — cara ngulang pengukuran ini](#lampiran-b--cara-ngulang-pengukuran-ini)

---

## 1. Ringkasan & rekomendasi

Dua-duanya lolos semua requirement, termasuk yang paling susah: 10 request inquiry yang nembak
bersamaan ke saldo 20 kredit, hasilnya harus tepat 4 yang sukses dan saldo tidak boleh minus.
Dua-duanya juga jumlah kodenya mirip — Laravel 4.674 baris, Spring Boot 5.033 baris. Jadi anggapan
"kode Java itu 2x lebih banyak" nggak kebukti di project ini.

Bedanya di mana?

| Soal | Menang | Kenapa |
|---|---|---|
| Kecepatan response | **Spring Boot** | tapi sebagian karena setup testing-nya kurang adil, lihat §2 |
| Ketauan salah sebelum jalan (compile-time) | **Spring Boot** | ada type checking + aturan arsitektur otomatis |
| Gampang deploy & rollback | **Spring Boot** | 1 file jar vs source code + PHP runtime |
| Setup awal buat developer baru | **Laravel** | tidak perlu compile |
| Jumlah kode per fitur | **Laravel** | 8% lebih sedikit |
| Kecepatan run test | **Laravel** | 42 detik vs 55 detik |
| Bug-bug "jebakan" dari framework | **Laravel** | Spring Boot punya 4 bug gara-gara transaction proxy, lihat §12 |
| Ketersediaan developer di Indonesia | **Laravel** | PHP/Laravel lebih umum dipakai |

**Rekomendasi.** Untuk service ini — dompet kredit yang kalau ada race condition bisa bikin saldo
salah atau user dapat inquiry gratis — kami akan pilih **Spring Boot**. Alasannya: hal-hal yang
paling penting di sini (hitungan uang, transaksi, tipe data) sudah dicek otomatis sebelum kode
dijalankan, dan responnya jauh lebih cepat & stabil.

Tapi rekomendasi ini bisa berubah tergantung situasi:

- **Kalau tim isinya kebanyakan PHP, atau servicenya cuma CRUD biasa** → pakai Laravel. Hasilnya
  sama bagus, kodenya lebih sedikit, dan lebih cepat buat development sehari-hari.
- **Kalau servicenya soal duit, atau butuh nyala lama dan trafik tinggi** → pakai Spring Boot.
- **Kalau butuh dua-duanya seperti project ini** → pastikan API contract-nya sama, itu yang bikin
  dua tim bisa kerja paralel tanpa bentrok.

---

## 2. Cara pengujian

Semua angka di §3 diambil tanggal **10 September 2026**, di laptop yang sama, dua stack jalan
bersamaan.

| | |
|---|---|
| Laptop | Windows 11, Docker Desktop, RAM 15.5 GB buat VM Linux |
| Laravel | jalan di Docker Sail, source code-nya di-mount dari Windows, mode debug nyala |
| Spring Boot | jalan pakai image production, jar sudah dibundel di image |
| Alat test | `curl` nembak 200 request beruntun, setelah 25 request pemanasan |

**3 catatan penting biar nggak salah baca angkanya:**

1. **Laravel di sini kena "penalti" dari cara file di-mount.** Karena source code PHP-nya dibaca
   dari folder Windows lewat Docker, tiap request jadi nunggu I/O, bukan nunggu proses hitung.
   Buktinya: jalanin `php artisan --version` makan waktu 4.2 detik, tapi CPU-nya cuma kepake 0.2
   detik — sisanya nunggu baca file doang. Kalau dijalankan di Linux asli, latency Laravel bakal
   jauh lebih bagus dari yang diukur di sini.
2. **Image Docker yang dibandingkan juga beda kelas.** Image Laravel (3 GB) itu image development
   yang isinya lengkap (Composer, Node, dll). Image Spring Boot (433 MB) itu image production yang
   isinya cuma Java + 1 file jar. Kalau Laravel juga pakai image production, ukurannya bakal jadi
   sekitar 150–250 MB — jadi sebenarnya Laravel yang lebih kecil kalau dibandingkan secara adil.
3. **Container database-nya beda kondisi.** MySQL sudah nyala 1 jam (cache-nya sudah "panas"),
   PostgreSQL baru nyala. Jadi perbandingan pemakaian memory database di sini kurang adil, cuma
   dicatat buat lengkap-lengkap saja.

Code coverage cuma diukur buat Spring Boot (otomatis kepake pas build). Laravel butuh setup
tambahan (Xdebug/PCOV) yang belum dijalankan, jadi ditulis "belum diukur", bukan ditebak.

---

## 3. Hasil pengukuran

### 3.1 Kode & dependency

| | Laravel | Spring Boot |
|---|---|---|
| File kode production | 79 file PHP | 120 file Java |
| Baris kode production | **4.674** | **5.033** |
| File test | 26 | 16 |
| Baris kode test | **1.748** | **1.612** |
| Jumlah test | **106 test**, 233 assertion | **61 test** (17 unit + 44 integration) |
| Migration database | 10 (bisa rollback) | 7 (satu arah) |
| Dependency langsung | 13 | 26 |
| Total package yang ke-download | 119 | 112 |

### 3.2 Build & test

| | Laravel | Spring Boot |
|---|---|---|
| Perlu compile? | Tidak | Ya (javac + Lombok + MapStruct) |
| Full test + quality check | **42.09 detik** | **54.9 detik** (compile + test + coverage check) |
| Code coverage | belum diukur | **81.87%** (target minimal 80% — lolos) |
| Hasil akhir build | tidak ada file khusus, source code = artifact-nya, folder `vendor/` 123 MB | 1 file `jar` — **74 MB** |

### 3.3 Runtime

| | Laravel | Spring Boot |
|---|---|---|
| Waktu startup aplikasi | tidak ada tahap startup, langsung jalan per request | **8.85 detik** |
| Restart container sampai bisa dipakai lagi | 10.4 detik | 10.8 detik |
| Kecepatan endpoint `GET /kosts` (mode dev) | rata-rata 265 ms, p50 **125 ms** | rata-rata 4.5 ms, p50 **1.4 ms** |
| Sama, tapi setelah `php artisan optimize` | p50 **29.3 ms** | *(sudah production dari awal)* |
| Pemakaian memory aplikasi (nganggur) | **265 MB** total (3 container: web, queue, scheduler) | **519 MB** (1 proses Java) |
| Pemakaian memory database | MySQL 474 MB *(sudah lama nyala)* | PostgreSQL 36 MB *(baru nyala)* |
| Ukuran image Docker | 3.03 GB *(image dev, lihat catatan di §2)* | 433 MB *(image production)* |

### 3.4 Yang perlu digarisbawahi dari tabel di atas

- **Jalankan `php artisan optimize` itu wajib, bukan opsional.** Ini bikin p50 turun dari 125 ms
  jadi 29 ms (turun 77%). Kalau ada benchmark Laravel yang nggak jalanin ini dulu, hasilnya nggak
  representatif.
- **Dua-duanya sempat kena "spike" beberapa detik sekali-sekali** — kemungkinan besar gara-gara
  disk laptop lag, bukan gara-gara aplikasinya.
- **Spring Boot memang secara desain lebih cepat**, karena prosesnya nyala terus (JVM), jadi nggak
  perlu bangun ulang routing/koneksi tiap request seperti PHP. Walaupun setup Laravel-nya lebih
  adil, Spring Boot tetap bakal lebih cepat — cuma selisihnya nggak akan sebesar ini.

---

## 4. Struktur kode

Susunan layer-nya mirip di dua project:

```
Laravel:     route → middleware → controller → form request → service → repository → database
Spring Boot: filter → controller → validasi   → service     → repository (Spring Data) → database
```

**Bedanya cara ngelompokin file.** Laravel ngelompokin berdasarkan jenis file (folder
`Services`, `Repositories`, `Controllers`). Spring Boot ngelompokin berdasarkan fitur (folder
`auth/`, `kost/`, `credit/`, dst, dan di dalamnya ada controller-service-repository-nya sendiri).
Buat project sekecil ini dua-duanya sama enaknya dipakai, tapi cara Spring Boot bakal lebih enak
kalau nanti fiturnya makin banyak — karena kalau mau ubah fitur "credit", semua filenya ngumpul
di satu folder.

**Yang paling beda: Spring Boot bisa "memaksa" aturan arsitektur lewat tool otomatis
(ArchUnit)** — misalnya build bakal gagal kalau controller langsung akses database tanpa lewat
service, atau kalau enum disimpan dengan cara yang berbahaya. Di Laravel, aturan seperti ini cuma
bisa ditulis di dokumentasi, developer harus disiplin sendiri untuk mengikutinya. Buat tim kecil
nggak masalah, tapi buat tim besar yang gonta-ganti orang, ini penting.

---

## 5. Keamanan saat request bersamaan (race condition)

Ini bagian paling kritis, dan untungnya dua project sampai ke solusi yang sama:

| | Laravel | Spring Boot |
|---|---|---|
| Kunci transaksi | `DB::transaction()` | `@Transactional` |
| Kunci baris saldo | `SELECT ... FOR UPDATE` | `SELECT ... FOR UPDATE` |
| Pengaman terakhir | `CHECK (balance >= 0)` di database | `CHECK (balance >= 0)` di database |
| Cara membuktikan aman | test dengan 10 proses nembak bersamaan → tepat 4 sukses, saldo akhir 0 | test dengan 10 thread nembak bersamaan → tepat 4 sukses, saldo akhir 0 |

Jadi walaupun bahasanya beda, solusinya sama: kunci baris saldo, bungkus dalam satu transaksi, dan
kasih pengaman terakhir di level database. Test race condition di Laravel sedikit lebih repot
ditulis (harus fork proses asli karena test Laravel biasanya dibungkus transaction juga), sementara
di Spring Boot bisa langsung pakai thread biasa lawan database PostgreSQL asli.

---

## 6. Keamanan aplikasi

| | Laravel | Spring Boot |
|---|---|---|
| Jenis token login | Token biasa (disimpan di database) | JWT + refresh token |
| Cek token tiap request | 1x query ke database | cek tanda tangan saja, tanpa query |
| Masa aktif token | 24 jam | akses 24 jam, refresh 7 hari |
| Logout | hapus token dari database → langsung mati | refresh token dicabut + access token di-blacklist di Redis sampai expired |
| Rate limiting | 60/menit biasa, 5/menit buat login, 20/menit buat inquiry | sama: 60/5/20 per menit |
| Proteksi SQL injection | otomatis dari Eloquent | otomatis dari JPA |

**Poin penting:** token biasa di Laravel itu paling gampang dipahami — hapus dari database, langsung
mati, selesai. JWT di Spring Boot lebih hemat query, tapi karena JWT itu "sudah ditandatangani",
dia tetap valid sampai expired walau sudah logout — makanya Spring Boot perlu tambahan blacklist di
Redis. Kalau ada yang bikin JWT tapi lupa bikin blacklist ini, tombol logout-nya bakal nggak
berfungsi walau kelihatannya jalan.

---

## 7. Testing

| | Laravel | Spring Boot |
|---|---|---|
| Tools test | PHPUnit | JUnit (unit test) + Testcontainers (integration test) |
| Database buat test | MySQL asli, di-rollback tiap test | PostgreSQL & Redis asli lewat Testcontainers |
| Jumlah test | 106 test, 42 detik | 61 test, 55 detik |
| Test arsitektur | tidak ada | 4 aturan (ArchUnit) |
| Coverage | belum diukur | 81.9% |

**Laravel lebih cepat buat coba-coba sehari-hari** karena nggak perlu compile dan nggak perlu
nyalain container database tiap test.

**Test Spring Boot lebih "real"** karena benar-benar jalan di atas PostgreSQL asli, bukan cuma
tiruan. Ini kebukti berguna — 3 dari 4 bug di §12 cuma ketemu karena test-nya jalan di database
asli.

Jumlah test yang lebih banyak di Laravel (106 vs 61) bukan berarti Laravel lebih teruji — cuma cara
motong test-nya beda. Laravel bikin banyak test kecil, Spring Boot bikin test yang lebih sedikit
tapi masing-masing ngecek lebih banyak hal sekaligus.

---

## 8. Performa & pemakaian resource

**Kecepatan.** Spring Boot p50-nya 1.4 ms, Laravel 125 ms (29 ms setelah dioptimasi). Sebagian
selisih ini karena setup testing yang kurang adil buat Laravel (lihat §2), tapi walau dihitung adil
sekalipun, Spring Boot tetap lebih cepat karena prosesnya nyala terus — cuma selisihnya nggak akan
sebesar 20x seperti yang terukur di sini.

**Konsistensi.** Angka Spring Boot lebih rapat (jarang ada yang tiba-tiba lambat). Angka Laravel
lebih naik-turun, bahkan setelah dioptimasi.

**Memory.** Laravel total 265 MB tersebar di 3 container, Spring Boot 519 MB di 1 proses. Tapi ini
kurang lebih adil: memory Spring Boot itu kebanyakan "dicadangkan", bukan benar-benar kepake, dan
jumlahnya tetap segitu mau aplikasinya sibuk atau nggak. Memory Laravel itu naik kalau makin banyak
request bersamaan, karena tiap request butuh proses sendiri.

**Hasil build.** 1 file jar (74 MB) vs folder `vendor/` (123 MB) + source code. File jar lebih
gampang dipindah-pindahkan dan di-rollback karena cuma 1 file.

---

## 9. Enaknya jadi developer di masing-masing stack

**Enaknya pakai Laravel:**

- Nggak perlu compile — save file, langsung refresh, langsung kelihatan hasilnya.
- Bikin 1 endpoint butuh lebih sedikit file dibanding Spring Boot.
- Pesan error-nya lebih enak dibaca dan lebih jelas maksudnya.
- Ada `artisan` CLI yang banyak membantu (`make:*`, `route:list`, `tinker`, dll).

**Enaknya pakai Spring Boot:**

- Compiler-nya otomatis jadi "test" duluan — salah ketik nama variabel langsung ketahuan sebelum
  dijalankan.
- Ada ArchUnit yang bisa memaksa aturan arsitektur lewat build, bukan cuma dokumentasi (lihat §4).
- Kalau struktur database nggak cocok sama kode, aplikasi langsung gagal start — bukan baru
  ketahuan pas ada user yang kena errornya.
- Refactor kode besar-besaran lebih aman karena IDE Java bisa "lihat" semua pemakaian sebuah
  fungsi/variabel dengan pasti.

**Satu hal yang sama-sama harus diperhatikan:** dua-duanya secara default punya masalah performa
yang tersembunyi (N+1 query) kalau nggak dimatikan manual — Laravel harus set
`Model::preventLazyLoading()`, Spring Boot harus set `open-in-view: false`. Dua-duanya wajib
diaktifkan manual, jangan andalkan default framework.

---

## 10. Siap produksi?

| | Laravel | Spring Boot |
|---|---|---|
| Health check | `GET /api/v1/health` | `GET /actuator/health` |
| Metrics (buat monitoring) | tidak ada | ada, otomatis siap dipakai Prometheus |
| Log | teks biasa | format JSON, lebih mudah diproses tool log |
| Validasi config saat startup | tidak ada — salah config baru ketahuan pas dipakai | ada — salah config, aplikasi langsung gagal start |
| Proses yang harus dijalankan | 3 (web, queue worker, scheduler) | 1 (semua jadi satu proses) |

**Spring Boot lebih siap pakai di production dari awal** — metrics, health check yang lebih detail,
log JSON, dan validasi config semuanya sudah ada tanpa nambah apa-apa. Laravel bisa dapat semua ini
juga, cuma harus ditambah manual dulu (dan itu nggak susah, cuma butuh waktu tambahan).

---

## 11. Maintenance & cari SDM

| | Laravel | Spring Boot |
|---|---|---|
| Ketersediaan developer di Indonesia | banyak, terutama level menengah | lebih sedikit di level menengah, tapi banyak yang senior |
| Onboarding developer baru | beberapa hari | 1-2 minggu kalau belum pernah pakai Spring |
| Biaya per perubahan kode | lebih murah — file lebih sedikit, nggak perlu compile | agak lebih mahal per perubahan, tapi lebih kecil kemungkinan salahnya |

**Intinya:** Laravel bikin perubahan kode jadi murah dan cepat, tapi ada risiko kesalahan tipe data
baru ketahuan pas sudah di staging/production. Spring Boot bikin perubahan sedikit lebih mahal, tapi
kesalahan seperti itu ketahuan dari awal (compile time). Buat dompet kredit seperti project ini,
lebih worth-it pilih yang aman duluan (Spring Boot). Buat service CRUD biasa, lebih worth-it pilih
yang cepat duluan (Laravel).

---

## 12. Bug yang muncul di masing-masing stack

Ini bagian paling jujur — bug yang benar-benar ditemukan pas ngembangin masing-masing project.

**Bug khas Spring Boot (4 bug, semuanya karena cara Spring menangani transaction & proxy):**

1. Manggil method `@Transactional` dari dalam class yang sama (`this.method()`) bikin
   transaction-nya nggak jalan sama sekali, karena "proxy" Spring-nya dilewati.
2. Rollback transaksi yang salah tempat bisa membatalkan hal yang seharusnya tetap tersimpan
   (misal: token yang sudah dicabut malah balik aktif lagi).
3. Data yang dibaca di satu transaksi, terus diubah di transaksi lain, ternyata perubahannya nggak
   ke-save tanpa ada error sama sekali.
4. Error "akses ditolak" (403) malah balik jadi error server (500) karena urutan pengecekan
   keamanan yang salah.

Semua bug ini sudah diperbaiki dan dikasih test khusus biar nggak kejadian lagi. Ini adalah "harga"
yang harus dibayar karena Spring Boot pakai proxy & transaction management yang canggih.

**Bug khas Laravel:** nggak ada bug se-tricky di atas, tapi risikonya beda — karena nggak ada
compiler yang ketat dan nggak ada pemaksa aturan arsitektur, kesalahan tipe data atau struktur kode
yang salah bisa lolos sampai runtime. Tantangan teknisnya lebih ke arah: test race condition harus
pakai `pcntl_fork` (bisa auto-skip di Windows tanpa WSL2), dan N+1 query harus dimatikan manual
lewat `Model::preventLazyLoading()`.

---

## 13. Kesimpulan

**Pilih Laravel kalau:**

- Tim sudah biasa pakai PHP, atau susah cari developer Spring Boot di daerahmu.
- Service-nya kebanyakan CRUD, cuma sedikit bagian yang butuh transaksi rumit.
- Lebih butuh cepat rilis daripada jaminan ketat dari compiler.
- Budget server terbatas.

**Pilih Spring Boot kalau:**

- Yang dikerjain soal duit — dompet, ledger, pembayaran, kuota — di mana race condition = rugi
  uang.
- Aplikasinya butuh nyala lama, trafik tinggi, atau banyak job terjadwal.
- Banyak tim/orang yang gonta-ganti pegang kode ini selama bertahun-tahun.
- Butuh monitoring & operasional yang lengkap dari hari pertama.

**Buat kasus MamiKos ini secara spesifik:** bagian dompet kredit itu yang paling nggak boleh salah,
dan itu juga bagian yang paling rawan race condition — jadi Spring Boot lebih cocok buat bagian
ini. Tapi yang lebih penting dari pilihan framework: **dua-duanya bisa sama-sama benar kalau
desainnya benar** — kunci baris database, satu transaksi buat proses "kurangi saldo + catat
riwayat", dan test yang benar-benar menembakkan request bersamaan. Framework apapun yang dipakai,
kalau desain ini dilewatkan, saldo user bisa jadi minus.

---

## Lampiran A — perbedaan response API

Dicek langsung dengan manggil API dua-duanya dan dibandingkan hasilnya. Yang nggak disebut di sini
berarti sama persis (selain id dan timestamp).

| Bagian | Laravel | Spring Boot | Disengaja? |
|---|---|---|---|
| Format nama field JSON | `snake_case` | `camelCase` | Ya, ikut kebiasaan masing-masing bahasa |
| Nilai `role` | huruf kecil (`"regular"`) | huruf besar (`"REGULAR"`) | Ya |
| Field uang | dikirim sebagai **teks** (`"609089.00"`) | dikirim sebagai **angka** (`600000.00`) | **Tidak** — ini beda yang nggak sengaja, dan bisa bikin error di client yang cuma nerima salah satu tipe |
| Field yang isinya `null` | tetap muncul di response | dihilangkan dari response | Sebagian, sebaiknya disamakan |
| Format timestamp | presisi detik | presisi nanodetik | **Tidak** — dua-duanya valid, tapi bisa beda kalau ada parser yang strict |
| Token login | token biasa, ~50 karakter | JWT, ~260 karakter + refresh token | Ya, desain auth beda |

Kode status, kode error, dan struktur pesan error semuanya identik di dua-duanya.

**Saran kalau nanti API ini dipakai bareng oleh 1 client yang sama:** samakan format uang jadi
angka, dan potong presisi timestamp jadi per detik saja di dua-duanya. Beda casing (`snake_case` vs
`camelCase`) nggak masalah karena itu bisa di-handle sekali di sisi client.

---

## Lampiran B — cara ngulang pengukuran ini

```bash
# ---- hitung baris kode & dependency -------------------------------------
find mamikosLaravel/app -name "*.php" | wc -l
find mamikosJava/src/main/java -name "*.java" | wc -l
grep -c "<dependency>" mamikosJava/pom.xml

# ---- jalankan test & build ----------------------------------------------
cd mamikosLaravel && docker compose exec laravel.test php artisan test
cd mamikosJava   && ./mvnw -B verify

# ---- ukuran hasil build --------------------------------------------------
ls -lh mamikosJava/target/mamikos-kost-api.jar
du -sh mamikosLaravel/vendor
docker images --format "{{.Repository}}:{{.Tag}}\t{{.Size}}"

# ---- ukur kecepatan response (200 request beruntun) ----------------------
for i in $(seq 1 200); do printf -- '-o\n/dev/null\n%s\n' "$URL"; done \
  | xargs -d '\n' curl -s -w "%{time_total}\n" | sort -n

# ---- ukur pemakaian memory ------------------------------------------------
docker stats --no-stream --format "{{.Name}}\t{{.MemUsage}}"
```

Buat ukur Laravel dengan cache production nyala (baris "setelah `artisan optimize`"):

```bash
docker compose exec laravel.test php artisan optimize
# ... lakukan pengukuran ...
docker compose exec laravel.test php artisan optimize:clear   # balikin ke mode dev
```

---

*Laporan ini dibuat tanggal 10 September 2026 berdasarkan kondisi kode di kedua project saat itu.
Angka yang belum diukur ditulis dengan jelas sebagai "belum diukur", bukan ditebak.*
