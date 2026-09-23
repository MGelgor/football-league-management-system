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

## Faz 3 — Fikstür Oluşturma Algoritması

- [ ] Round-robin (circle method) algoritmasını `FixtureGenerationService` içinde implemente et
- [ ] Minimum 18 takım kuralını doğrula, aksi halde anlamlı hata dön
- [ ] Çift devre (rövanş: ev sahibi/deplasman ters çevrilir) mantığını ekle
- [ ] `POST /fixtures/generate` endpoint'i
- [ ] Unit test: her takım her rakiple tam 2 kez (1 iç saha + 1 deplasman) eşleşiyor mu, bir haftada bir takım birden fazla maça giriyor mu

## Faz 4 — Maç Simülasyon Motoru

- [ ] Takım "gücü" modelini tasarla (örn. 1-100 arası rastgele/atanabilir bir `strength` alanı)
- [ ] Skor hesaplama algoritmasını tasarla (örn. güç farkına dayalı ağırlıklı olasılık dağılımı veya Poisson tabanlı gol üretimi)
- [ ] Moral sistemini tasarla ve entegre et (son maç sonuçlarına göre güncellenen, olasılığı hafifçe etkileyen bir çarpan)
- [ ] `MatchSimulationService` — bir haftanın tüm maçlarını simüle eder
- [ ] `POST /weeks/{id}/play` endpoint'i ("Haftayı Oynat")
- [ ] Unit/istatistiksel test: yüksek güçlü takımın kazanma oranının gözle görülür şekilde yüksek çıktığını doğrula (örn. 1000 simülasyonluk örneklem)

## Faz 5 — Puan Tablosu ve Sıralama

- [ ] `StandingsService` — O, G, B, M, A, Y, P hesaplama
- [ ] Sıralama kuralı: Puan → Averaj → Atılan gol
- [ ] `GET /standings` endpoint'i
- [ ] Unit test: eşit puanlı takımların averaj/atılan gole göre doğru sıralandığını doğrula

## Faz 6 — Sezonu Tamamlama

- [ ] `POST /season/play-all` endpoint'i — kalan tüm haftaları sırayla simüle eder
- [ ] Şampiyon belirleme mantığı (sezon sonu sıralamasının 1.si)
- [ ] Zaten oynanmış haftaları tekrar oynatmama kontrolü

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
