# Futbol Ligi Yönetim ve Simülasyon Sistemi — Geliştirme Planı

Stack: **Java 25 + Spring Boot 4** (backend) · **PostgreSQL** (veritabanı, Docker'sız çalıştırma için H2) · **React 19 + TypeScript + Vite** (frontend)

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
- [x] ER diyagramını taslak olarak çıkar ([ER_DIYAGRAMI.md](ER_DIYAGRAMI.md); Faz 12'de `strength`/`morale` alanlarıyla güncellendi)

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

## Faz 7 — Frontend Temel Yapı ✅

- [x] Routing (React Router 8): `/teams`, `/fixture`, `/standings`; `/` → `/teams` yönlendirmesi, bilinmeyen rotada "Sayfa bulunamadı"
- [x] API client: Axios yerine yerleşik `fetch` üzerine ince bir katman (`src/api/client.ts`, ek bağımlılık yok) + backend DTO'larının TypeScript tipleri (`src/api/types.ts`). Backend adresi Vite proxy ile ayarlandı (`/api` ve `/uploads` → `http://localhost:8080`), bu sayede backend'e CORS ayarı gerekmedi
- [x] Genel layout (navigasyon: Takımlar / Fikstür / Puan Durumu, aktif sekme vurgulu, açık/koyu tema)
- [x] Vite şablonundan kalan kullanılmayan dosyalar (`App.css`, `assets/`, `icons.svg`) silindi
- [x] Doğrulama: `npm run build` + `npm run lint` temiz; tarayıcıda 3 sayfa arası geçiş, yönlendirme ve 404 rotası çalıştı, proxy üzerinden API'ye ulaşıldı, konsolda hata yok

## Faz 8 — Frontend: Takım Yönetimi Ekranı ✅

- [x] Takım listesi görünümü (logo, ad, kuruluş yılı, renkler, güç, moral; Türkçe alfabetik sıralı)
- [x] Takım ekleme / düzenleme formu (`TeamForm`, logo upload dahil — önce takım kaydedilir, sonra logo `multipart` ile yüklenir; 5MB ve `image/*` kontrolü istemcide de yapılır)
- [x] Silme onayı (satır içinde "Silinsin mi? Evet, sil / Vazgeç")
- [x] Minimum 18 takım şartı UI'da net gösterilsin (sayaç + ilerleme çubuğu, tek sayıda takım uyarısı, 18+ çift sayıda "hazır" bandı)
- [x] Backend koruması: fikstür oluşturulduktan sonra takım ekleme/silme `409` döner (`TeamsLockedException`), UI'da da ilgili butonlar pasif. Düzenleme serbest
- [x] Yeni endpoint `DELETE /api/fixtures` — fikstürü sıfırlar (maçlar + haftalar silinir, moraller başlangıç değerine döner, güç korunur)
- [x] Hata düzeltmeleri: 100 karakteri aşan takım adı/renk artık `500` yerine `400` (`@Size`), 5MB üstü logo boş gövdeli `413` yerine mesajlı `413` döner
- [x] Doğrulama: curl ile kilit/sıfırlama/validasyon senaryoları; tarayıcıda logolu takım ekleme (logo proxy üzerinden görüntülendi), aynı isimde takım → form içinde `409` mesajı, düzenleme, silme onayı + silme, 19 takımda "çift olmalı" uyarısı, fikstür varken butonların pasifleşmesi

## Faz 9 — Frontend: Fikstür ve Simülasyon Ekranı ✅

- [x] Fikstürü haftalara göre listele (hafta şeridi + seçili haftanın maçları, ‹ › ile haftalar arası gezinme; açılışta sıradaki oynanmamış hafta seçili gelir)
- [x] Fikstür yoksa "Fikstürü oluştur" butonu (yetersiz takımda backend'in `400` mesajı gösterilir), fikstür varsa onaylı "Fikstürü sıfırla" butonu
- [x] "Haftayı Oynat" butonu ve maç sonuçlarının anlık gösterimi (endpoint'in döndürdüğü hafta state'e yazılır, sayfa yenilenmeden skorlar ve kazanan takım kalın görünür)
- [x] Oynanan / oynanmamış hafta ayrımı (yeşil/gri hafta çipleri, "Oynandı/Oynanmadı" rozeti, "X oynandı · Y kaldı" sayacı, sezon bitince Puan Durumu'na yönlendiren bant)
- [x] Doğrulama: tarayıcıda 17 takımla oluşturma hatası, 18 takımla 34 haftalık fikstür, Hafta 1 ve 2'nin oynatılması, yenilemeden sonra verinin korunup Hafta 3'ün seçili gelmesi, sıfırlama onayı → boş fikstür + moraller 50 + takım silme yeniden açık

## Faz 10 — Frontend: Puan Tablosu ve Şampiyon ✅

- [x] Puan tablosu görünümü (O/G/B/M/A/Y/AV/P + sıralama; sıralama backend'den geldiği gibi gösterilir, frontend yeniden sıralamaz)
- [x] "Tüm Sezonu Oynat" butonu (yalnızca fikstür varken ve sezon bitmemişken görünür; "X / 306 maç oynandı" ilerleme bilgisi)
- [x] Sezon bitince şampiyon vurgusu (altın banner: ad, puan, G/B/M, averaj + tabloda 🏆 ile vurgulu satır). "Sezon bitti" bilgisi fikstürdeki tüm maçların oynanmış olmasından hesaplanır, bu yüzden sayfa yenilendiğinde de korunur
- [x] Doğrulama: Hafta 1 elle oynatıldıktan sonra tablo (eşit puan/averajda atılan gol kuralı gözle doğrulandı), "Tüm Sezonu Oynat" → şampiyon banner'ı; programatik kontrol: sıralama kuralı, herkes 34 maç, P = 3G + B, AV = A − Y, 306/306 maç, Hafta 1 skorları değişmedi, tekrar `play-all` aynı şampiyonu döndürdü; yenilemeden sonra banner kaldı ve buton gizlendi, Fikstür sayfasında "Sezon tamamlandı" bandı çıktı

## Faz 11 — Test ve Kalite ✅

- [x] Backend: servis katmanı için unit testler (JUnit + Mockito) — `TeamServiceTest` (10), `FixtureServiceTest` (5, gerçek `RoundRobinScheduler` + sahte repository'ler), `MatchSimulationServiceTest` (5, sahte `ScoreSimulator` ile deterministik skor → moral kuralları), `SeasonServiceTest` (2), `FileStorageServiceTest` (3, `@TempDir`)
- [x] Backend: repository/entegrasyon testleri (H2) — `MatchRepositoryTest` (sıralama + `join fetch` gerçekten yükleniyor mu), `TeamRepositoryTest` (büyük/küçük harf duyarsız arama, DB seviyesinde unique kısıtı), `LeagueFlowIntegrationTest` (tam Spring context + MockMvc ile HTTP üzerinden tüm sezon akışı ve validasyon hatası)
- [x] Toplam **43 test, hepsi geçiyor** (`./mvnw test`). Testlerin gerçekten hata yakaladığı kasıtlı hata eklenerek doğrulandı: "fikstür varken takım ekleme" kontrolü silinince 2 test, deplasman galibiyetinde moral ters yazılınca 1 test kırıldı
- [x] Uçtan uca manuel senaryo: 18 takım ekle → fikstür oluştur → haftaları oynat → puan tablosunu doğrula → şampiyonu doğrula (Faz 8–10 boyunca tarayıcıda yapıldı; aynı akış `LeagueFlowIntegrationTest` ile otomatik de koşuyor)
- [x] Kod gözden geçirme — katman sınırları: controller'lar repository/entity import etmiyor ve yalnızca DTO döndürüyor, controller'larda iş mantığı (if/for) yok, repository'leri yalnızca servisler kullanıyor, entity/DTO katmanı üst katmanlara bağımlı değil, saf algoritmalar (`RoundRobinScheduler`, `ScoreSimulator`, `StandingsCalculator`) Spring/JPA'ya bağımlı değil; frontend'de `fetch` yalnızca `api/client.ts` içinde

## Faz 12 — Son Rötuşlar ve Teslim

- [x] README'yi tamamla (özellikler, doğru stack sürümleri, Docker'lı ve Docker'sız kurulum, kullanım akışı, testler, mimari ve algoritma özeti, API tablosu + Swagger linki, proje yapısı)
- [x] Docker'sız çalıştırma için `h2` Spring profili (`application-h2.yml`): `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2`
- [ ] Docker Compose ile tüm sistemi sıfırdan ayağa kaldırma testi — **yapılamadı: geliştirme makinesinde Docker kurulu değil.** Yerine yapılan sıfırdan kurulum testi: proje (`node_modules`/`target` hariç) temiz bir klasöre kopyalandı → `npm ci` + `npm run build` + `npm run lint` temiz, `./mvnw package` ile 43 test geçti ve jar üretildi; jar `h2` profiliyle başlatıldı → 18 takım + fikstür (`201`) + tüm sezon (306 maç, şampiyon belirlendi) + Swagger UI çalıştı. Not: `docker-compose.yml` yalnızca PostgreSQL'i ayağa kaldırır; backend ve frontend yerelde çalıştırılır
- [x] Kod temizliği son geçişi (isimlendirme, ölü kod, tutarlılık): `spring.jpa.open-in-view: false` (tüm DTO dönüşümleri transaction içinde yapıldığı için güvenli; tüm endpoint'ler tekrar doğrulandı, uyarı kalktı), Vite şablon README'si silindi, ER diyagramı güncellendi, kullanılmayan CSS sınıfı yok, backend proxy'ye ulaşılamadığında (`502`) frontend anlaşılır mesaj gösteriyor, mobil genişlikte (375px) üç sayfada da yatay taşma yok
- [x] Teslim kriterleri kontrol listesi: ✅ çalışan backend + frontend (tarayıcıda uçtan uca doğrulandı), ✅ temiz kod (lint/derleme uyarısız, 43 test), ✅ katmanlı mimari (Faz 11'deki katman kontrolü)

---

## Önerilen Sıra Mantığı

Fazlar bağımlılık sırasına göre dizilmiştir: önce domain/veritabanı, sonra backend iş mantığı (CRUD → fikstür → simülasyon → puan tablosu → sezon), en son frontend. Fikstür ve simülasyon algoritmaları (Faz 3-4) projenin en kritik/özgün kısımları olduğu için önce backend'de sağlam ve test edilmiş halde bitirilmeli; frontend bu API'ler üzerine inşa edilecek.
