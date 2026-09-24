# Futbol Ligi Yönetim ve Simülasyon Sistemi — Geliştirme Planı

Stack: **Java 21 + Spring Boot 3** (backend) · **PostgreSQL** (veritabanı) · **React + TypeScript** (frontend)

Kaynak: PoC — Senaryo ve İster Dokümanı (IQB Solutions)

---

## Faz 0 — Proje Kurulumu ✅

- [x] Repo yapısını oluştur: `backend/` ve `frontend/` klasörleri
- [x] Spring Initializr ile backend iskeleti (Web, Data JPA, PostgreSQL Driver, Validation, Lombok, H2, springdoc-openapi — Spring Boot 4.1.1, Java 25)
- [x] Docker Compose ile yerel PostgreSQL servisi tanımla
- [x] `application.yml` — dev / test (H2) profilleri
- [x] React + Vite + TypeScript ile frontend iskeleti (`npm create vite@latest`, bağımlılıklar kuruldu)
- [x] `.gitignore`, temel `README.md` (kurulum talimatları)

## Faz 1 — Veritabanı ve Domain Tasarımı

- [x] Entity'leri tasarla: `Team`, `MatchWeek`, `Match`
- [x] İlişkileri belirle (Match → home/away Team, Match → MatchWeek)
- [x] Puan durumu: ayrı tablo yerine Match sonuçlarından **runtime hesaplanan** bir görünüm olarak tasarla (veri tutarsızlığı riskini azaltır) — bu yüzden ayrı bir Standing entity yok
- [ ] Flyway ile migration script'leri (opsiyonel ama "temiz kod" kriteri için artı puan)
- [ ] ER diyagramını taslak olarak çıkar

## Faz 2 — Takım Yönetimi (Backend CRUD) ✅

- [x] `TeamRepository` (Spring Data JPA)
- [x] `TeamService` — iş kuralları (isim tekrarı engelleme, kuruluş yılı gelecekte olamaz)
- [x] `TeamController` — REST endpoint'leri: ekle / güncelle / sil / listele / tekil getir
- [x] Logo dosya yükleme (multipart/form-data, yerel dosya sistemine kaydet, `/uploads/**` üzerinden servis edilir)
- [x] Validation (`@Valid`, `@NotBlank`, `@Min` vb. + merkezi `GlobalExceptionHandler`)
- [x] curl ile uçtan uca manuel test (Swagger UI ile de aynı şekilde test edilebilir)

## Faz 3 — Fikstür Oluşturma Algoritması ✅

- [x] Round-robin (circle method) algoritmasını `RoundRobinScheduler` içinde implemente et (saf, Spring'den bağımsız, test edilebilir)
- [x] Minimum 18 takım kuralını doğrula (+ çift sayı kuralı), aksi halde anlamlı `400` hatası dön
- [x] Çift devre (rövanş: ev sahibi/deplasman ters çevrilir) mantığını `FixtureService` içinde ekle
- [x] `POST /api/fixtures/generate` + `GET /api/fixtures` endpoint'leri
- [x] Unit test (`RoundRobinSchedulerTest`, 4 test, hepsi geçti): her ikili tam 1 kez (tek devre) / 2 kez (çift devre) eşleşiyor mu, bir haftada bir takım iki kez oynuyor mu, farklı takım sayılarında hafta/maç sayısı doğru mu, tek sayı takımda hata fırlatıyor mu
- [x] Canlı doğrulama: 18 takımla gerçek uygulama üzerinden `curl` ile test edildi — 34 hafta, 306 maç, tekrar yok, `409` (zaten oluşturulmuş) doğru çalışıyor

## Faz 4 — Maç Simülasyon Motoru ✅

- [x] Takım "gücü" modeli: `Team.strength` (1-100 arası, takım oluşturulurken rastgele atanır)
- [x] Skor hesaplama algoritması: `ScoreSimulator` — güç farkından beklenen gol sayısı (lambda) hesaplanır, gerçek skor Poisson dağılımından örneklenir (ev sahibi avantajı dahil)
- [x] Moral sistemi: `Team.morale` (0-100, başlangıç 50) — galibiyet +10, mağlubiyet -10, beraberlikte değişmez; moral, etkin gücü hafifçe ayarlar
- [x] `MatchSimulationService` — bir haftanın tüm maçlarını simüle eder, skorları ve moralleri kaydeder
- [x] `POST /api/weeks/{weekNumber}/play` endpoint'i ("Haftayı Oynat")
- [x] Unit/istatistiksel test (`ScoreSimulatorTest`, 4 test, hepsi geçti — 1000'er simülasyonluk örneklemle): güçlü takım belirgin şekilde daha sık kazanıyor, eşit güçte aşırı tek taraflı sonuç yok, skorlar hiç negatif değil, yüksek moral kazanma şansını artırıyor
- [x] Canlı doğrulama: 18 takımla gerçek uygulama üzerinden test edildi — skorlar üretildi, moral değişimleri (galip +10, mağlup -10, beraberlik ±0) doğru işledi, tekrar oynatma `409`, olmayan hafta `404`

## Faz 5 — Puan Tablosu ve Sıralama ✅

- [x] `StandingsCalculator` (saf, test edilebilir) + `StandingsService` (veritabanı) — O, G, B, M, A, Y, P hesaplama
- [x] Sıralama kuralı: Puan → Averaj → Atılan gol
- [x] `GET /api/standings` endpoint'i
- [x] Unit test (`StandingsCalculatorTest`, 4 test, hepsi geçti): puana göre sıralama, eşit puanda averaja göre, eşit puan+averajda atılan gole göre, hiç maç oynamamış takımın sıfırlarla tabloya girmesi
- [x] Canlı doğrulama: 2 hafta oynatıldıktan sonra tablo, sıralama kuralına uyduğu programatik olarak doğrulandı

## Faz 6 — Sezonu Tamamlama ✅

- [x] `POST /api/season/play-all` endpoint'i — kalan tüm haftaları sırayla simüle eder
- [x] Şampiyon belirleme mantığı (sezon sonu sıralamasının 1.si, `SeasonResultResponse` ile döner)
- [x] Zaten oynanmış haftaları tekrar oynatmama kontrolü (`Match::isPlayed` kontrolü ile atlanır)
- [x] Canlı doğrulama: fikstürsüz çağrıda `400`, sezon sonunda şampiyon (Takım 3, 76 puan) belirlendi, elle oynanmış Hafta 1 tekrar oynatılmadı (skor aynı kaldı, 3-0), 306/306 maç tamamlandı, sezon bittikten sonra tekrar çağrıda hata vermeden aynı şampiyonu döndürdü

## Faz 7 — Frontend Temel Yapı

- [ ] Routing (React Router)
- [ ] API client (Axios) + backend base URL config
- [ ] Genel layout (navigasyon: Takımlar / Fikstür / Puan Durumu)

## Faz 8 — Frontend: Takım Yönetimi Ekranı

- [ ] Takım listesi görünümü
- [ ] Takım ekleme / düzenleme formu (logo upload dahil)
- [ ] Silme onayı
- [ ] Minimum 18 takım şartı UI'da net gösterilsin

## Faz 9 — Frontend: Fikstür ve Simülasyon Ekranı

- [ ] Fikstürü haftalara göre listele
- [ ] "Haftayı Oynat" butonu ve maç sonuçlarının anlık gösterimi
- [ ] Oynanan / oynanmamış hafta ayrımı (görsel durum)

## Faz 10 — Frontend: Puan Tablosu ve Şampiyon

- [ ] Puan tablosu görünümü (O/G/B/M/A/Y/P + sıralama)
- [ ] "Tüm Sezonu Oynat" butonu
- [ ] Sezon bitince şampiyon vurgusu (banner/rozet)

## Faz 11 — Test ve Kalite

- [ ] Backend: servis katmanı için unit testler (JUnit + Mockito)
- [ ] Backend: repository/entegrasyon testleri (Testcontainers veya H2)
- [ ] Uçtan uca manuel senaryo: 18 takım ekle → fikstür oluştur → haftaları oynat → puan tablosunu doğrula → şampiyonu doğrula
- [ ] Kod gözden geçirme — katman sınırlarının ihlal edilmediğinden emin ol

## Faz 12 — Son Rötuşlar ve Teslim

- [ ] README'yi tamamla (kurulum, çalıştırma, mimari özeti, API dokümantasyonu linki)
- [ ] Docker Compose ile tüm sistemi sıfırdan ayağa kaldırma testi
- [ ] Kod temizliği son geçişi (isimlendirme, ölü kod, tutarlılık)
- [ ] Teslim kriterleri kontrol listesi: ✅ çalışan backend + frontend, ✅ temiz kod, ✅ katmanlı mimari

---

## Önerilen Sıra Mantığı

Fazlar bağımlılık sırasına göre dizilmiştir: önce domain/veritabanı, sonra backend iş mantığı (CRUD → fikstür → simülasyon → puan tablosu → sezon), en son frontend. Fikstür ve simülasyon algoritmaları (Faz 3-4) projenin en kritik/özgün kısımları olduğu için önce backend'de sağlam ve test edilmiş halde bitirilmeli; frontend bu API'ler üzerine inşa edilecek.
