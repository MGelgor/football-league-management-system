# Futbol Ligi Yönetim ve Simülasyon Sistemi

Takım yönetimi, çift devreli fikstür oluşturma, maç simülasyonu, puan durumu ve sezon sonu şampiyon belirleme yapan bir lig yönetim sistemi (PoC).
Geliştirme süreci için [GELISTIRME_PLANI.md](GELISTIRME_PLANI.md), veritabanı şeması için [ER_DIYAGRAMI.md](ER_DIYAGRAMI.md) dosyasına bakınız.

## Özellikler

- **Takım yönetimi:** ekle / düzenle / sil, logo yükleme, isim tekrarı ve gelecekteki kuruluş yılı engeli; **rastgele N takım ekle** (şehir adı + ek, rastgele kuruluş yılı ve renkler; varsayılan sayı fikstür için eksik takım sayısıdır)
- **4 büyükler:** Galatasaray, Fenerbahçe, Beşiktaş ve Trabzonspor uygulama açılışında backend tarafından oluşturulur; silinemez / düzenlenemez, güçleri her zaman 85–100 aralığındadır (normal takımlar en fazla 84) ve maç hesabında +8 güç bonusu alırlar
- **Oyuncular:** takım oluştururken isteğe bağlı olarak oyuncu (ad, mevki, forma no, güç) eklenir, eksik mevkiler rastgele oyuncularla 18'e tamamlanır; sonradan oyuncu eklenip düzenlenebilir. Takımlar listesinde satıra tıklayınca oyuncu adları açılır; takım sayfasında sezon ve kariyer gol / asist / kart
- **Oyuncu gücü:** golün / asistin bir oyuncuya yazılma olasılığı = mevki ağırlığı × oyuncu gücü (aynı mevkide gücü iki katı olan oyuncu iki kat sık gol atar)
- **Oyuncu istatistikleri:** her maçın golleri, asistleri ve kartları `match_events` tablosunda kalıcı saklanır; Puan Durumu'ndaki "Oyuncular" sekmesi sezonun gol / asist / kart tablosunu gösterir (sütuna tıklayınca sıralanır)
- **Fikstür:** en az 18 ve çift sayıda takımdan round-robin (circle method) ile çift devreli fikstür (18 takım → 34 hafta, 306 maç)
- **Maç simülasyonu:** takım gücü + moral + ev sahibi avantajına dayalı, Poisson dağılımıyla skor üretimi; haftalar **sırayla** oynanır ("Haftayı Oynat" yalnızca sıradaki haftada) ve "Tüm Sezonu Oynat"
- **Kazanma olasılıkları:** her oynanmamış maçta ev / beraberlik / deplasman yüzdeleri; maç oynanınca maç öncesi olasılıklar saklanır
- **Maç detayı:** goller (mevkiye göre ağırlıklı) + asistler, sarı / kırmızı kartlar, topla oynama, şut, isabetli şut, kurtarış, korner, ofsayt, faul
- **Güç değişimi:** her maçtan sonra beklentiye göre (sürpriz galibiyet artırır, beklenmedik yenilgi düşürür), sezon sonunda sıralamaya göre; arayüzde haftalık / sezonluk ▲▼ okları, puan tablosunda sıra değişimi okları
- **Maç kadrosu:** her maçta güç ağırlıklı ilk 11 (1 KL, 4 DEF, 4 OS, 2 FV), en fazla 3 değişiklik; gol / asist / kart yalnızca o dakikada sahada olana. Oyuncu başına oynadığı maç, dakika, gol/maç
- **Ceza ve sakatlık:** kırmızı kart 1 maç, sezonda her 4 sarı 1 maç ceza; maç içi sakatlık 1–3 maç; cezalı / sakat oyuncu kadroya alınmaz (sezon başında sıfırlanır)
- **Reyting ve maçın oyuncusu:** gol, asist, sonuç, gol yememe, kurtarış ve kartlardan 3.0–10.0 reyting; en yükseği maçın oyuncusu
- **Oyuncu sayfası:** profil, sezon / turnuva bazında istatistikler, attığı goller ve asistleri (maç bağlantılı)
- **Yaş ve gelişim:** lig bitince herkes bir yaş büyür; gençler gelişir, yaşlılar geriler, iyi / kötü sezon ±1; 35 yaşından sonra emeklilik, yerine aynı mevkide genç oyuncu
- **Puan durumu:** O / G / B / M / A / Y / AV / P + son 5 maç formu, sıralama Puan → Averaj → Atılan gol; bölge şeritleri (1. Şampiyonlar Ligi, 2–3. Avrupa Ligi, düşen 3 takım)
- **Küme düşme / yükselme:** lig bitince son 3 takım düşer (4 büyükler düşmez, bir üstteki düşer), yerlerine rastgele 3 takım çıkar
- **Kupa:** lig bitince ilk 8 takımla tek maçlık eleme (çeyrek final, yarı final, final), beraberlikte penaltı atışları; kupa yarıdayken yeni sezona geçilemez
- **Sezonlar ve rekorlar:** lig / kupa şampiyonlukları, düşen / çıkan takımlar; en gollü maç, en farklı galibiyet, en uzun galibiyet / yenilmezlik serisi, sezon puan / gol rekorları, sezon ve tüm zamanların gol kralı, en çok şampiyonluk / kupa
- **Takım istatistikleri:** iç saha / deplasman, topla oynama ortalaması, şut isabeti, gol yemediği maç, en golcü / en çok asist; haftalık sıralama ve güç geçmişi grafikleri
- **Karşılaştırma:** iki takım arasındaki tüm maçlar (lig + kupa, tüm sezonlar) ve özet
- **Sezonlar:** son hafta oynanınca sezon biter, şampiyon kaydedilir; "Yeni sezon" ile devam edilir, eski sezonların puan tabloları ve şampiyonlar arşivde kalır
- **Sıfırlama:** yalnızca devam eden sezon iptal edilir (güç ve moral sezon başına döner); sezon devam ederken takım ekleme/silme kilitlidir, maç geçmişi olan takım silinince arşivlenir

## Stack

| Katman | Teknoloji |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1 (Web MVC, Data JPA / Hibernate 7, Validation), Lombok, springdoc-openapi (Swagger) |
| Veritabanı | PostgreSQL 16 (Docker Compose) — Docker olmadan çalıştırmak için H2 (bellek içi) |
| Frontend | React 19, TypeScript 6, Vite 8, React Router 8 |
| Test | JUnit 6, Mockito, Spring Boot Test (MockMvc, `@DataJpaTest`), H2 |

## Gereksinimler

- JDK 25 (Maven kurmaya gerek yok, proje `./mvnw` wrapper'ı ile gelir)
- Node.js 22.22+ ve npm
- Docker (isteğe bağlı — yalnızca PostgreSQL ile çalıştırmak için)

## Kurulum ve Çalıştırma

### 1. Backend

**Seçenek A — PostgreSQL ile (Docker gerekir):**

```bash
docker compose up -d
```

Bu komut `localhost:5432` üzerinde `football_league` adlı bir PostgreSQL veritabanı açar (kullanıcı ve şifre: `football_league`). Ardından:

```bash
cd backend
./mvnw spring-boot:run
```

**Seçenek B — Docker olmadan (H2, bellek içi):**

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Bu modda veriler bellekte tutulur ve uygulama kapanınca silinir.

> PostgreSQL ile eski bir sürümden güncelliyorsanız şema değiştiği için (sezonlar, oyuncular, maç olayları) veritabanını sıfırdan oluşturun: `docker compose down -v && docker compose up -d`.

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

- Uygulama: http://localhost:5173

Vite geliştirme sunucusu `/api` ve `/uploads` isteklerini `http://localhost:8080` adresine yönlendirir (proxy, bkz. `frontend/vite.config.ts`; farklı port için `BACKEND_URL=http://localhost:8090 npm run dev`). Bu nedenle backend'de CORS ayarı gerekmez; frontend'i açmadan önce backend'in çalışıyor olması yeterlidir.

### Kullanım akışı

1. 4 büyükler hazır gelir. **Takımlar** sayfasından en az 14 takım daha ekleyerek 18'e (çift sayıya) tamamlayın; "Rastgele takım ekle" ile tek tıkla tamamlanabilir. Bir takıma tıklayınca kadrosu açılır ve oyuncular düzenlenebilir.
2. **Fikstür** sayfasında "Fikstürü oluştur"a basın. Her maçta kazanma olasılıkları görünür.
3. Haftaları sırayla "Haftayı Oynat" ile oynatın ya da **Puan Durumu** sayfasında "Tüm Sezonu Oynat"ı kullanın. Oynanmış maça tıklayınca maç detayı açılır.
4. Sezon bitince şampiyon gösterilir, son 3 takım düşer ve yerlerine 3 yeni takım çıkar; sezon **Sezonlar** sayfasına arşivlenir.
5. İsterseniz **Kupa** sayfasından ilk 8 takımla kupayı oynayın.
6. İsterseniz takım listesini değiştirin, sonra Fikstür sayfasından "Yeni sezonu başlat"a basın.

## Testler

```bash
cd backend
./mvnw test
```

99 test: saf algoritmalar (fikstür, rastgele takım üretimi, skor simülasyonu + olasılıklar, puan tablosu, kadro üretimi, maç kadrosu / olay / istatistik / reyting kuralları, oyuncu gelişimi, penaltılar, küme düşme bölgesi), Mockito ile servis testleri (ceza / sakatlık, sezon sonu), H2 üzerinde repository testleri ve 4 büyükler → kadro → sıralı haftalar → maç detayı → küme düşme → rekorlar → oyuncu / takım istatistikleri → kupa → sezon arşivi akışını HTTP üzerinden koşan entegrasyon testleri (`LeagueFlowIntegrationTest`).

Frontend için tip kontrolü + derleme ve lint:

```bash
cd frontend
npm run build
npm run lint
```

## Mimari

### Backend — katmanlı mimari

```
controller  →  service  →  repository  →  entity
   (HTTP)     (iş kuralları)  (Spring Data JPA)  (JPA tabloları)
      ↑            ↓
     dto  ←────────┘   (controller'lar yalnızca DTO alır/döndürür, entity dışarı sızmaz)
```

| Paket | Sorumluluk |
|---|---|
| `controller` | REST endpoint'leri; iş mantığı içermez, servise yönlendirir |
| `service` | İş kuralları ve transaction sınırları (`@Transactional`) |
| `service` (saf algoritmalar) | `RoundRobinScheduler`, `ScoreSimulator`, `StandingsCalculator`, `SquadGenerator`, `RandomTeamGenerator`, `MatchDetailGenerator`, `PlayerDevelopment` — Spring/JPA'ya bağımlı değil, doğrudan unit test edilir |
| `repository` | Spring Data JPA arayüzleri |
| `entity` | `Team`, `Player`, `Season`, `SeasonTeamChange`, `MatchWeek`, `Match`, `MatchEvent`, `MatchAppearance`, `MatchTeamStats` |
| `config` | `WebConfig` (logo dosyaları), `StartupDataInitializer` (açılışta 4 büyükler) |
| `dto` | İstek/yanıt modelleri (Java `record`) |
| `exception` | Özel exception'lar + `GlobalExceptionHandler` (hataları `{ "message": ... }` gövdesiyle uygun HTTP koduna çevirir) |

Önemli tasarım kararları:

- **Puan durumu tabloda tutulmaz**, her istekte oynanan maçlardan hesaplanır. Böylece maç sonuçları ile puan tablosu hiçbir zaman birbirinden kopamaz.
- **Algoritmalar veritabanından bağımsızdır.** Servisler veriyi repository'den okur, saf algoritmaya verir, sonucu kaydeder. Algoritmalar Spring context'i açmadan test edilir.
- **Oyuncu istatistikleri ayrı tabloda tutulmaz.** Gol / kart / sakatlık `match_events`'te, kim kaç dakika oynadı ve reytingi `match_appearances`'ta maç oynandığı anda kalıcı yazılır; sezon ve kariyer toplamları bunlardan sayılır. "Sezon" istatistikleri lig maçlarıdır, kariyer lig + kupa.
- **Sezon devam ederken takım ekleme/silme backend'de engellenir** (`409`), çünkü fikstürü bozar. Maç geçmişi olan takım silinmez, arşivlenir (`active = false`), böylece eski sezonların kayıtları bozulmaz.
- **Haftalar sırayla oynanır.** Kural backend'de (`409`); arayüz yalnızca sıradaki haftaya "Haftayı Oynat" butonu koyar.

### Algoritmalar

- **Fikstür (circle method):** Bir takım sabit tutulur, diğerleri her turda bir adım döndürülür. N takım için N−1 haftalık ilk devre üretilir, ikinci devrede aynı eşleşmeler ev sahibi/deplasman ters çevrilerek tekrarlanır.
- **Skor simülasyonu:** Her takımın etkin gücü = güç (1–100) + moral etkisi (+ ev sahibine avantaj). Etkin güçlerin oranına göre iki takımın beklenen gol sayısı hesaplanır, gerçek skor Poisson dağılımından örneklenir. Güçlü takım daha sık kazanır ama sürpriz her zaman mümkündür.
- **Kazanma olasılıkları:** Aynı beklenen gol değerleriyle iki Poisson dağılımının ortak olasılık tablosu (0–12 gol) toplanır: P(ev > dep), P(ev = dep), P(ev < dep).
- **Moral:** Galibiyet +10, mağlubiyet −10, beraberlik 0 (0–100 arasında sınırlı). Kupa maçları moral ve gücü değiştirmez.
- **Reyting:** 6.0 + gol 1.0 + asist 0.6 + sonuç ±0.4 + gol yememe (KL 0.8, DEF 0.5) + kaleci kurtarışı 0.15 − yenilen gol − sarı 0.4 / kırmızı 1.5 (± küçük rastgelelik; 20 dakikadan az oynayanda etki yarıya iner), 3.0–10.0.
- **Oyuncu gelişimi:** ≤21 yaş +2..+5, 22–25 0..+3, 26–29 −1..+1, 30–32 −3..0, 33+ −5..−2; sezon reytingi ≥7.2 ise +1, ≤5.8 ise −1. Emeklilik olasılığı 35: %25, 36: %50, 37: %80, 38+: kesin.
- **Penaltılar:** 5'er atış (%75 isabet; bir taraf yetişemeyecek duruma düşünce biter), eşitlikte tek tek.
- **Güç değişimi:** `(alınan puan − beklenen puan) × 0,7` yuvarlanır (beklenen puan = 3·P(galibiyet) + P(beraberlik)). Sezon sonunda: şampiyon +4, ilk 4 +2, ilk yarı +1, alt yarı −1, son 3 −3.
- **4 büyükler bonusu:** Skor ve olasılık hesabında 4 büyüklerin gücüne +8 eklenir (gösterilen güç değişmez). Simülasyonla ölçüldü: şampiyonun 4 büyüklerden çıkma oranı %76 → %84, ilk 4'teki ortalama büyük sayısı 2,58 → 2,75.
- **Maç olayları:** Maç dakika sırasıyla işlenir (aynı dakikada kart → sakatlık → değişiklik → gol). İlk 11, her mevkide (güç + 0–15 rastgele) en yüksek oyunculardır. Goller sahadakiler arasında mevki ağırlığı (forvet 15, orta saha 5, defans 2, kaleci 0) × oyuncu gücü ile dağıtılır (ilk 11'de ~%50 / %35 / %15); asist %70 olasılıkla. Taktik değişiklik 46–88. dakikalarda, zayıf oyuncunun çıkma olasılığı daha yüksek. Kartlar iki takıma aynı ortalamayla verilir, ikinci sarı kırmızıya döner; kırmızı gören oyuncunun yerine kimse giremez, sakatlananın yerine (hak varsa) yedek girer. İstatistik kuralları: şut ≥ isabetli şut ≥ gol, kurtarış = rakibin isabetli şutu − rakibin golü, faul ≥ kart, topla oynama toplamı 100.

### Frontend

```
src/
├── api/
│   ├── client.ts      # fetch sarmalayıcı + tüm endpoint fonksiyonları (sayfalar fetch'i doğrudan çağırmaz)
│   └── types.ts       # backend DTO'larının TypeScript karşılıkları
├── components/
│   ├── TeamForm.tsx        # takım ekleme/düzenleme formu (logo + isteğe bağlı oyuncular)
│   ├── PlayerStatsTable.tsx # puan durumu "Oyuncular" sekmesi
│   ├── LineChart.tsx       # bağımlılıksız SVG çizgi grafik (sıra / güç geçmişi)
│   ├── FormBadges.tsx      # son 5 maç G / B / M
│   ├── PlayerStatus.tsx    # cezalı / sakat rozeti
│   ├── TeamLogo.tsx        # logo ya da baş harf
│   ├── Trend.tsx           # ▲/▼ değişim göstergesi
│   └── ProbabilityBar.tsx  # 1 / X / 2 olasılık çubuğu
├── pages/
│   ├── TeamsPage.tsx
│   ├── TeamDetailPage.tsx  # /teams/:id — sezon istatistikleri, grafikler, kadro + oyuncu düzenleme
│   ├── PlayerPage.tsx      # /players/:id — profil, sezon sezon, goller
│   ├── FixturePage.tsx
│   ├── MatchDetailPage.tsx # /matches/:id — olay akışı, istatistikler, kadrolar ve reytingler
│   ├── StandingsPage.tsx   # ?season=ID geçmiş sezon, ?view=players oyuncu sekmesi
│   ├── CupPage.tsx         # /cup — eşleşme ağacı
│   ├── SeasonsPage.tsx     # şampiyonluklar, rekorlar, düşen / çıkanlar
│   └── ComparePage.tsx     # /compare?a=ID&b=ID — iki takım karşılaştırma
├── labels.ts          # mevki, kupa turu, bölge etiketleri
├── App.tsx            # layout + navigasyon + rotalar
├── main.tsx           # giriş noktası (BrowserRouter)
└── index.css          # global stiller (açık/koyu tema)
```

## API

Tüm endpoint'ler Swagger UI üzerinden de denenebilir: http://localhost:8080/swagger-ui.html

| Metod | Yol | Açıklama |
|---|---|---|
| GET | `/api/teams` | Aktif takımları listele |
| GET | `/api/teams/{id}` | Tek takım |
| POST | `/api/teams` | Takım ekle `{name, foundedYear, colors, players?}` → `201`; `players` isteğe bağlı, eksik mevkiler 18'e tamamlanır (sezon devam ediyorsa `409`) |
| POST | `/api/teams/random` | `{count}` (1–50) kadar rastgele takım ekle → `201` (sezon devam ediyorsa `409`) |
| PUT | `/api/teams/{id}` | Takım güncelle (4 büyüklerde `409`) |
| DELETE | `/api/teams/{id}` | Takım sil / maç geçmişi varsa arşivle → `204` (sezon devam ediyorsa veya 4 büyüklerde `409`) |
| POST | `/api/teams/{id}/logo` | Logo yükle (`multipart/form-data`, alan adı `file`, yalnızca görsel, en fazla 5MB) |
| GET | `/api/teams/{id}/players` | Kadro (emekliler hariç): yaş, ceza / sakatlık, güncel sezon maç / dakika / gol / asist / kart / reyting / maçın oyuncusu + kariyer |
| POST | `/api/teams/{id}/players` | Kadroya oyuncu ekle `{name, position, shirtNumber, strength, age}` → `201` |
| GET | `/api/teams/{id}/stats?seasonId=` | Takımın sezon istatistikleri + haftalık sıra / puan / güç |
| GET | `/api/teams/head-to-head?teamA=&teamB=` | İki takım arasındaki tüm maçlar ve özet |
| GET | `/api/players/{id}` | Oyuncu profili: sezon / turnuva satırları, goller ve asistler |
| PUT | `/api/players/{id}` | Oyuncu düzenle `{name, position, shirtNumber, strength, age}` (forma no takımda tekil 1–99, güç 1–100, yaş 16–45) |
| GET | `/api/players/stats?seasonId=` | Sezonun lig oyuncu istatistikleri (gol → asist sıralı; varsayılan: güncel sezon) |
| POST | `/api/fixtures/generate` | Yeni sezon aç ve fikstürünü oluştur → `201` (en az 18 ve çift sayıda takım; devam eden sezon ya da yarım kalmış kupa varsa `409`) |
| GET | `/api/fixtures?seasonId=` | Fikstürü haftalara göre getir (varsayılan: güncel sezon), oynanmamış maçlarda olasılıklarla |
| DELETE | `/api/fixtures` | Devam eden sezonu sıfırla → `204` (tamamlanmış sezonda `409`) |
| POST | `/api/weeks/{weekNumber}/play` | Güncel sezonda haftayı oynat (oynanmışsa veya sırası gelmemişse `409`, hafta yoksa `404`) |
| GET | `/api/matches/{id}` | Maç detayı: olaylar, istatistikler, kadrolar + reytingler, maç öncesi olasılıklar, kupa turu / penaltılar |
| GET | `/api/standings?seasonId=` | Puan durumu + sıra değişimi + form + bölge (varsayılan: güncel sezon) |
| GET | `/api/seasons` | Sezonlar (en yeni başta): şampiyon, kupa şampiyonu, düşen / çıkan takımlar, oynanan maç sayısı |
| GET | `/api/cup?seasonId=` | Kupa durumu ve turları |
| POST | `/api/cup/start` · `/api/cup/play-round` · `/api/cup/play-all` | Kupayı başlat / sıradaki turu oynat / kalan tüm turları oynat (lig bitmeden `409`) |
| GET | `/api/records` | Tarihsel rekorlar |
| POST | `/api/seasons/play-all` | Güncel sezonun kalan haftalarını oynat, şampiyonu ve son tabloyu döndür (fikstür yoksa `400`) |

Hata yanıtları `{ "message": "..." }` biçimindedir; validasyon hatalarında `400` ile `{ "alanAdı": "mesaj" }` döner.

## Proje Yapısı

```
.
├── backend/               # Spring Boot REST API
│   └── src/main/resources/
│       ├── application.yml      # varsayılan: PostgreSQL
│       └── application-h2.yml   # "h2" profili: bellek içi veritabanı
├── frontend/              # React + TypeScript SPA
├── docker-compose.yml     # Yerel PostgreSQL servisi
├── ER_DIYAGRAMI.md        # Veritabanı şeması
└── GELISTIRME_PLANI.md    # Faz faz geliştirme planı
```
