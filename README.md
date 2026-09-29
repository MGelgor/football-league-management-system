# Futbol Ligi Yönetim ve Simülasyon Sistemi

Takım yönetimi, çift devreli fikstür oluşturma, maç simülasyonu, puan durumu ve sezon sonu şampiyon belirleme yapan bir lig yönetim sistemi (PoC).
Geliştirme süreci için [GELISTIRME_PLANI.md](GELISTIRME_PLANI.md), veritabanı şeması için [ER_DIYAGRAMI.md](ER_DIYAGRAMI.md) dosyasına bakınız.

## Özellikler

- **Takım yönetimi:** ekle / düzenle / sil, logo yükleme, isim tekrarı ve gelecekteki kuruluş yılı engeli
- **Fikstür:** en az 18 ve çift sayıda takımdan round-robin (circle method) ile çift devreli fikstür (18 takım → 34 hafta, 306 maç)
- **Maç simülasyonu:** takım gücü + moral + ev sahibi avantajına dayalı, Poisson dağılımıyla skor üretimi; "Haftayı Oynat" ve "Tüm Sezonu Oynat"
- **Puan durumu:** O / G / B / M / A / Y / AV / P, sıralama Puan → Averaj → Atılan gol
- **Şampiyon:** sezon bitince puan tablosunun lideri vurgulanır
- **Sıfırlama:** fikstür silinip takım listesi değiştirilerek yeni sezona başlanabilir (fikstür varken takım ekleme/silme kilitlidir)

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

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

- Uygulama: http://localhost:5173

Vite geliştirme sunucusu `/api` ve `/uploads` isteklerini `http://localhost:8080` adresine yönlendirir (proxy, bkz. `frontend/vite.config.ts`). Bu nedenle backend'de CORS ayarı gerekmez; frontend'i açmadan önce backend'in çalışıyor olması yeterlidir.

### Kullanım akışı

1. **Takımlar** sayfasından en az 18 (çift sayıda) takım ekleyin. Sayfadaki sayaç kaç takım kaldığını gösterir.
2. **Fikstür** sayfasında "Fikstürü oluştur"a basın.
3. Haftaları "Haftayı Oynat" ile tek tek oynatın ya da **Puan Durumu** sayfasında "Tüm Sezonu Oynat"ı kullanın.
4. Sezon bitince şampiyon Puan Durumu sayfasında gösterilir.
5. Yeni bir sezon için Fikstür sayfasından "Fikstürü sıfırla"yı kullanın.

## Testler

```bash
cd backend
./mvnw test
```

43 test: saf algoritmalar (fikstür, skor simülasyonu, puan tablosu), Mockito ile servis testleri, H2 üzerinde repository testleri ve tüm sezon akışını HTTP üzerinden koşan bir entegrasyon testi (`LeagueFlowIntegrationTest`).

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
| `service` (saf algoritmalar) | `RoundRobinScheduler`, `ScoreSimulator`, `StandingsCalculator` — Spring/JPA'ya bağımlı değil, doğrudan unit test edilir |
| `repository` | Spring Data JPA arayüzleri |
| `entity` | `Team`, `MatchWeek`, `Match` |
| `dto` | İstek/yanıt modelleri (Java `record`) |
| `exception` | Özel exception'lar + `GlobalExceptionHandler` (hataları `{ "message": ... }` gövdesiyle uygun HTTP koduna çevirir) |

Önemli tasarım kararları:

- **Puan durumu tabloda tutulmaz**, her istekte oynanan maçlardan hesaplanır. Böylece maç sonuçları ile puan tablosu hiçbir zaman birbirinden kopamaz.
- **Algoritmalar veritabanından bağımsızdır.** Servisler veriyi repository'den okur, saf algoritmaya verir, sonucu kaydeder. Algoritmalar Spring context'i açmadan test edilir.
- **Fikstür oluşturulduktan sonra takım ekleme/silme backend'de engellenir** (`409`), çünkü fikstürü bozar ya da maçı olan takımın silinmesi foreign key hatasına yol açar.

### Algoritmalar

- **Fikstür (circle method):** Bir takım sabit tutulur, diğerleri her turda bir adım döndürülür. N takım için N−1 haftalık ilk devre üretilir, ikinci devrede aynı eşleşmeler ev sahibi/deplasman ters çevrilerek tekrarlanır.
- **Skor simülasyonu:** Her takımın etkin gücü = güç (1–100) + moral etkisi (+ ev sahibine avantaj). Etkin güçlerin oranına göre iki takımın beklenen gol sayısı hesaplanır, gerçek skor Poisson dağılımından örneklenir. Güçlü takım daha sık kazanır ama sürpriz her zaman mümkündür.
- **Moral:** Galibiyet +10, mağlubiyet −10, beraberlik 0 (0–100 arasında sınırlı).

### Frontend

```
src/
├── api/
│   ├── client.ts      # fetch sarmalayıcı + tüm endpoint fonksiyonları (sayfalar fetch'i doğrudan çağırmaz)
│   └── types.ts       # backend DTO'larının TypeScript karşılıkları
├── components/
│   └── TeamForm.tsx   # takım ekleme/düzenleme formu (logo yükleme dahil)
├── pages/
│   ├── TeamsPage.tsx
│   ├── FixturePage.tsx
│   └── StandingsPage.tsx
├── App.tsx            # layout + navigasyon + rotalar
├── main.tsx           # giriş noktası (BrowserRouter)
└── index.css          # global stiller (açık/koyu tema)
```

## API

Tüm endpoint'ler Swagger UI üzerinden de denenebilir: http://localhost:8080/swagger-ui.html

| Metod | Yol | Açıklama |
|---|---|---|
| GET | `/api/teams` | Takımları listele |
| GET | `/api/teams/{id}` | Tek takım |
| POST | `/api/teams` | Takım ekle `{name, foundedYear, colors}` → `201` (fikstür varsa `409`) |
| PUT | `/api/teams/{id}` | Takım güncelle |
| DELETE | `/api/teams/{id}` | Takım sil → `204` (fikstür varsa `409`) |
| POST | `/api/teams/{id}/logo` | Logo yükle (`multipart/form-data`, alan adı `file`, yalnızca görsel, en fazla 5MB) |
| POST | `/api/fixtures/generate` | Fikstür oluştur → `201` (en az 18 ve çift sayıda takım gerekir; fikstür varsa `409`) |
| GET | `/api/fixtures` | Fikstürü haftalara göre getir |
| DELETE | `/api/fixtures` | Fikstürü sıfırla → `204` (maçlar silinir, moraller 50'ye döner) |
| POST | `/api/weeks/{weekNumber}/play` | Haftayı oynat (oynanmışsa `409`, hafta yoksa `404`) |
| GET | `/api/standings` | Puan durumu |
| POST | `/api/season/play-all` | Kalan tüm haftaları oynat, şampiyonu ve son tabloyu döndür (fikstür yoksa `400`) |

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
