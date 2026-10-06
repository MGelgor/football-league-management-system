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

# İkinci Aşama — Yeni Özellikler

## Faz 13 — Haftaların Sırayla Oynanması + Kazanma Olasılıkları ✅

- [x] Backend: bir hafta, önceki tüm haftalar oynanmadan oynatılamaz (`WeekOrderException` → `409`, mesajda sıradaki hafta). Canlı: sezon başında `POST /api/weeks/3/play` → `409 "... sıradaki hafta 1"`
- [x] `ScoreSimulator.probabilities`: beklenen gollerden (iki Poisson dağılımının ortak tablosu) ev / beraberlik / deplasman olasılıkları; yuvarlamadan sonra toplam her zaman %100 (beraberlik kalan olarak hesaplanır). Test: 20.000 simülasyondaki kazanma oranı gösterilen olasılıkla 2 puan içinde
- [x] Oynanmamış maçlarda olasılıklar canlı hesaplanır (`MatchMapper`), maç oynanınca maç öncesi olasılıklar `matches` tablosuna kaydedilir
- [x] Frontend: yalnızca sıradaki haftada "Haftayı Oynat", diğer oynanmamış haftalarda "Önce Hafta N oynanmalı" (tıklanabilir); hafta şeridinde "Sıradaki" vurgusu; maç satırında 1 / X / 2 olasılık çubuğu. Hafta oynanınca güçler değiştiği için fikstür yeniden yüklenir

## Faz 14 — Sezon Sistemi ✅

- [x] `Season` entity'si (sezon no, bitti mi, şampiyon); `MatchWeek → Season` ilişkisi, `(season_id, week_number)` unique
- [x] Son hafta oynanınca sezon otomatik biter, şampiyon kaydedilir; `POST /api/fixtures/generate` bir sonraki sezonu açar, eski sezonlar silinmez. Canlı: 5 sezon art arda oynatıldı, hepsi arşivde
- [x] "Sıfırla" yalnızca devam eden sezonu iptal eder (olaylar → istatistikler → maçlar → haftalar → sezon silinir, güç ve moral sezon başına döner); tamamlanmış sezonda `409`
- [x] Takım ekleme/silme yalnızca sezon devam etmiyorken açık; maç geçmişi olan takım silinince arşivlenir (`active = false`), eski sezon tablosunda görünmeye devam eder
- [x] `GET /api/seasons`, `GET /api/standings?seasonId=`, `GET /api/fixtures?seasonId=`; `play-all` adresi `/api/seasons/play-all` oldu. Frontend: Sezonlar sayfası (şampiyonluk sayıları + sezon listesi), puan tablosunda sezon seçici (`?season=ID`)

## Faz 15 — 4 Büyükler + Güç Değişimi ve Ok Göstergeleri ✅

- [x] Galatasaray, Fenerbahçe, Beşiktaş, Trabzonspor `StartupDataInitializer` ile açılışta backend'de oluşturulur (aynı isimde normal takım varsa büyüğe çevrilir); düzenleme/silme `409` (`BigFourLockedException`); güç `Team.changeStrength` ile 85–100'de tutulur, normal takımlar 1–84 (başlangıç 20–80)
- [x] Her maçtan sonra güç = `round((alınan puan − beklenen puan) × 0,7)` kadar değişir; sezon sonunda şampiyon +4, ilk 4 +2, ilk yarı +1, alt yarı −1, son 3 −3. Canlı: 5 sezon boyunca büyükler 88–100 arasında kaldı, şampiyonlar GS ×3, BJK ×1, normal bir takım ×1 (sürpriz mümkün)
- [x] Frontend: takımlar tablosunda güç + son değişim ▲/▼ ve sezonluk değişim sütunu, "4 Büyük" rozeti, büyükler için "Sabit takım"; puan tablosunda geçen haftaya göre sıra değişimi ▲/▼ (yalnızca devam eden sezonda)

## Faz 16 — Oyuncular ✅

- [x] `Player` entity'si (ad, mevki, forma no); `SquadGenerator` her yeni takıma 18 kişilik kadro üretir (2 KL, 6 DEF, 6 OS, 4 FV; isim ve numara tekrarsız, 1 numara kalecide). Bu özellikten önce oluşmuş kadrosuz takımlara açılışta kadro eklenir
- [x] `GET /api/teams/{id}/players` (güncel sezon gol / asist / sarı / kırmızı), `PUT /api/players/{id}` (forma no takımda tekil → aksi halde `400`, 1–99 validasyonu)
- [x] Frontend: takım adına tıklayınca `/teams/:id` kadro sayfası (mevkiye göre sıralı, renkli mevki etiketi), satır içi düzenleme; tarayıcıda çakışan numara hatası ve başarılı kayıt doğrulandı

## Faz 17 — Maç Detayı ve İstatistikleri ✅

- [x] `MatchEvent` (gol + asist, sarı, kırmızı; dakika) ve `MatchTeamStats` (topla oynama, şut, isabetli şut, korner, faul, ofsayt, kurtarış); kartlar olaylardan sayılır
- [x] `MatchDetailGenerator`: goller forvet 10 / orta saha 5 / defans 2 ağırlıkla (kaleci 0), asist %70; kartlar iki takıma aynı ortalamayla, 2. sarı → kırmızı, atılan oyuncu sonrasında gol / asist / kart alamaz. Canlı örneklem: FV 355, OS 238, DEF 115 gol; sarı kart ev 482 / dep 450
- [x] Tutarlılık kuralları (şut ≥ isabetli şut ≥ gol, kurtarış = rakibin isabetli şutu − rakibin golü, faul ≥ kart, topla oynama toplamı %100) testte 2000'er maçla, canlıda ~300 maçta kontrol edildi: 0 ihlal
- [x] `GET /api/matches/{id}`; frontend `/matches/:id`: skor tabelası + golcüler, maç öncesi olasılık çubuğu, iki taraflı olay akışı, karşılaştırmalı çubuklu istatistik tablosu, gollerin mevkilere dağılımı. Fikstürde oynanmış maç satırı tıklanabilir

## Faz 18 — Test ve Dokümantasyon ✅

- [x] Testler: 43 → **71**, hepsi geçiyor. Yeni: `MatchDetailGeneratorTest` (5), `SquadGeneratorTest`, `TeamTest` (güç sınırları), `ScoreSimulatorTest`'e olasılık testleri, `MatchSimulationServiceTest` (sıralı hafta, olasılık kaydı, güç değişimi, sezon bitişi), `TeamServiceTest` (4 büyükler, arşivleme), `SeasonServiceTest` (sezon listesi), `MatchRepositoryTest` (sezon filtresi, ilk oynanmamış hafta, sezon silme), entegrasyon testleri (tüm yeni akış + sıra değişimi + oyuncu validasyonu)
- [x] README (özellikler, kullanım akışı, algoritmalar, API tablosu, frontend yapısı), ER diyagramı güncellendi
- [x] Uçtan uca canlı doğrulama: backend `h2` profiliyle ayrı portta (8090) curl ile 5 sezon + sıfırlama; tarayıcıda takımlar → kadro düzenleme → sezonlar → geçmiş sezon tablosu → maç detayı → yeni sezon → sıralı hafta → puan tablosu okları; 375px mobilde yatay taşma yok. `npm run build` + `npm run lint` temiz. Bulunup düzeltilen görsel hatalar: işlem sütunu hizası (`td`'de `display:flex`), sıra sütunu eklenince takım sütununun kayması, mobilde menü ve skorun satır kırması

## Faz 19 — Rastgele Takım Ekleme + "4 Büyük" Yazısının Kaldırılması ✅

- [x] Frontend'den "4 Büyük" rozeti kaldırıldı (takımlar listesi ve takım sayfası); liste artık düz alfabetik. Bu takımlar backend'de yine sabit: düzenle/sil yerine "Sabit takım" yazıyor
- [x] `RandomTeamGenerator`: şehir + ek ("Adanaspor", "Bolu FK", "Rize Gücü"…) adayları arasından var olan adlarla (büyük/küçük harf duyarsız) çakışmayanları seçer, önce her şehirden en fazla bir takım; kuruluş yılı 1900–2020, iki farklı renk
- [x] `POST /api/teams/random {count}` (1–50, validasyon `400`, sezon devam ederken `409`); her takım normal güç bandında ve 18 kişilik kadroyla oluşur
- [x] Frontend: Takımlar başlığında sayı kutusu + "Rastgele takım ekle"; varsayılan sayı 18'e tamamlayan sayı (18+ ise çift sayıya tamamlayan); sezon devam ederken pasif
- [x] Testler 71 → **77** (`RandomTeamGeneratorTest` 4, `TeamServiceTest` +2, entegrasyon testinde 14 rastgele takım → 18 takımla sezon + kilit + validasyon). Tarayıcıda: 4 takımda öneri 14 → tek tıkla 18 takım, öneri 2'ye döndü; fikstür sonrası buton pasif, API `409`

## Faz 20 — Oyuncu Gücü, Oyuncu İstatistikleri ve 4 Büyükler Ayarı ✅

- [x] Takımlar listesinde satıra tıklayınca kadro açılır (mevkilere göre oyuncu adları, forma no, bu sezonki golleri) + "Kadro sayfası ve istatistikler →"; kadro ilk açılışta yüklenip saklanır
- [x] Gol / asist / kart verisi: zaten her maçta `match_events`'e kalıcı yazılıyordu; buna ek olarak takım sayfasında **kariyer** (tüm sezonlar) gol / asist ve `GET /api/players/stats?seasonId=` ile sezon oyuncu istatistikleri. Entegrasyon testi: sezon sonunda takımların attığı gol toplamı = oyuncuların gol toplamı; yeni sezonda sezon golleri 0, kariyer golleri saklı
- [x] Takım oluştururken oyuncu ekleme isteğe bağlı: "+ Oyuncu ekle" ile ad / mevki / forma no / güç; `SquadGenerator.complete` eklenenleri korur, şablondaki (2 KL, 6 DEF, 6 OS, 4 FV) eksikleri rastgele oyuncularla tamamlar (rastgele oyuncu gücü takım gücü ±15). Forma no çakışmasında takım kaydedilmeden `400`. Takım sayfasında da "+ Oyuncu ekle" (`POST /api/teams/{id}/players`) ve güç düzenleme
- [x] `Player.strength` (1–100): gol / asist ağırlığı = mevki ağırlığı × güç. Test: aynı takımda güç 100 ve 20 olan iki forvetin gol oranı ~5. Canlı: güç 100 forvet 10 gol, takım arkadaşı güç 30 forvet 5 gol
- [x] Puan Durumu'nda "Takımlar / Oyuncular" sekmeleri (`?view=players`, sezon seçimiyle birlikte); oyuncu tablosu G / A / 🟨 / 🟥 başlığına tıklayınca sıralanır
- [x] 4 büyükler: maç hesabında +8 güç bonusu (`Team.matchStrength`, gösterilen güç değişmez; olasılıklar da bonusu içerir). Backend formülleri Python'a aktarılıp 200 sezonla ölçüldü — bonus 0 / 5 / 8 / 12 için şampiyonun büyüklerden çıkma oranı %76 / %78 / %84 / %90; "çok az artış" için 8 seçildi
- [x] Testler 77 → **83**, build + lint temiz; tarayıcıda: oyunculu takım oluşturma, satır açılınca kadro, oyuncu sekmesi + asist sıralaması, takım sayfasında oyuncu ekleme

## Faz 21 — Oyuncu Derinliği ✅

- [x] İlk 11 + yedekler: `MatchDetailGenerator` maçı dakika sırasıyla işler (kart → sakatlık → değişiklik → gol); ilk 11 dizilişe göre (1 KL, 4 DEF, 4 OS, 2 FV) güç + rastgelelikle seçilir, en fazla 3 değişiklik (taktik değişiklikte zayıf oyuncu daha sık çıkar). Gol / asist / kart yalnızca o dakikada sahada olana. `MatchAppearance` (giriş-çıkış dakikası, yerine girdiği oyuncu, reyting, maçın oyuncusu). İlk 11'de 2 forvet olduğu için forvetin gol ağırlığı 10 → 15 (dağılım ~%50 FV / %35 OS / %15 DEF korunsun diye)
- [x] Oynadığı maç, dakika, gol/maç (kadro, oyuncu sayfası, oyuncular sekmesi)
- [x] Ceza ve sakatlık: kırmızı → 1 maç, sezonda her 4 sarı → 1 maç, maç içi sakatlık (`INJURY` olayı; hak varsa yerine yedek girer) → 1–3 maç; kaçırılan her maçta kalan sayı azalır; cezalı / sakat oyuncu kadroya alınmaz; yeni sezonda / sıfırlamada sıfırlanır. Kırmızı gören oyuncunun yerine kimse giremez
- [x] Reyting (3.0–10.0) ve maçın oyuncusu; maç detayında kadrolar + reyting rozetleri, skor tabelasında maçın oyuncusu
- [x] Oyuncu sayfası `/players/:id`: profil (yaş, güç ▲▼, durum, emekli), sezon + turnuva satırları, gol / asist listesi (maça bağlantılı). Arşivlenmiş takımın / emekli oyuncunun sayfası da açılır
- [x] Yaş ve gelişim (`PlayerDevelopment`): lig bitince +1 yaş; ≤21 +2..+5 … 33+ −5..−2, sezon reytingi ≥7.2 +1 / ≤5.8 −1; 35: %25, 36: %50, 37: %80, 38+: kesin emeklilik, yerine aynı mevkide 17–19 yaşında genç. Gelişim lig bitişinde uygulanır (tamamlanmış sezon sıfırlanamadığı için iki kez uygulanma riski yok). Takım oluştururken / oyuncu eklerken yaş girilir

## Faz 22 — Lig ve Sezon Yapısı ✅

- [x] Küme düşme / yükselme (`SeasonEndService`): lig bitince son 3 takım arşivlenir (4 büyükler düşmez, bir üstteki düşer), `RandomTeamGenerator` ile 3 takım çıkar (güç 35–60, kadrolu); `SeasonTeamChange` kaydı, Sezonlar sayfasında ↓ / ↑ listesi. Takım sayısı 18'de kalır
- [x] Kupa (`CupService`): lig bitince ilk 8 ile tek maç eleme (1–8, 4–5, 2–7, 3–6 → yarı final → final), üst sıradaki ev sahibi, beraberlikte penaltı atışları; kupa maçları da `matches`'ta (hafta `competition = CUP`, 101+), moral / gücü etkilemez ama kadro / kart / sakatlık işler. Kupa yarıdayken yeni sezon `409`; başlatılmadıysa atlanabilir. Kupa sayfası: eşleşme ağacı, tur oynat / tümünü oynat, kupa şampiyonu. Puan durumu, fikstür, gol krallığı yalnızca lig haftalarını okur
- [x] Puan tablosunda bölge şeritleri (1. Şampiyonlar Ligi mavi, 2–3. Avrupa Ligi turuncu, düşen 3 takım kırmızı) + açıklama
- [x] Tarihsel rekorlar (`RecordService`, `GET /api/records`): en gollü maç, en farklı galibiyet, en uzun galibiyet / yenilmezlik serisi, sezonda en çok puan / gol, sezon ve tüm zamanların gol kralı, en çok şampiyonluk / kupa; Sezonlar sayfasında kartlar (maç / oyuncu / takım bağlantılı)

## Faz 23 — İstatistik ve Görselleştirme ✅

- [x] Haftalık sıralama grafiği (her haftadan sonraki puan durumu yeniden hesaplanır; bağımlılıksız SVG `LineChart`)
- [x] Puan tablosunda form (son 5 maç G / B / M rozetleri)
- [x] Takım sezon istatistikleri (`GET /api/teams/{id}/stats?seasonId=`): toplam / iç saha / deplasman, topla oynama ortalaması, şut isabeti, gol yemediği maç, kartlar, en golcü / en çok asist, form; takım sayfasında sezon seçici
- [x] Karşılaştırma sayfası `/compare?a=&b=` (`GET /api/teams/head-to-head`): lig + kupa tüm maçlar, galibiyet / beraberlik / gol özeti; takım sayfasından "Başka bir takımla karşılaştır"
- [x] Güç geçmişi grafiği (`MatchTeamStats.strengthAfter`: her maçtan sonraki güç kaydedilir)

## Faz 24 — Test ve Dokümantasyon ✅

- [x] Testler 83 → **99**, üst üste çalıştırmalarda kararlı. Yeni: kadro kuralları (ilk 11, ≤3 değişiklik, olaylar yalnızca sahadakine, kırmızıda yerine giriş yok), reyting / maçın oyuncusu, gol atanın reytingi yüksek, dakika başına gol ∝ güç, `PlayerDevelopmentTest`, `CupServiceTest` (penaltılar, kazanan), `SeasonEndServiceTest` (sezon sonu gücü, düşme bölgesi, 4 büyükler muafiyeti), ceza / sakatlık / kadroya alınmama, kupa maçında moral / güç değişmemesi; entegrasyon testi küme düşme, rekorlar, oyuncu profili, takım istatistikleri, karşılaştırma ve kupa akışıyla genişledi. Var olan kırılgan bir test düzeltildi (rastgele kadroda dolu olabilen 88 numara)
- [x] README, ER diyagramı güncellendi
- [x] Canlı doğrulama: backend 8090'da 3 sezon + kupa + 4. sezonda 6 hafta (sezon ~3,4 sn); tarayıcıda puan durumu bölge / form, oyuncular sekmesi, oyuncu sayfası, takım istatistikleri + grafikler, kadro rozetleri (cezalı), kupa ağacı + penaltılı maç detayı (kadrolar, değişiklikler, maçın oyuncusu), sezonlar + rekorlar, karşılaştırma; 375px'te 7 sayfada yatay taşma yok. Bulunup düzeltilenler: kupa turu başlıklarının kayması, karşılaştırma özetinin mobilde taşması

## Faz 25 — Saha Üzerinde Diziliş ✅

- [x] `PitchFormation`: kuşbakışı dikey saha (SVG çizgiler, 68 × 105 oranı), oyuncular mevkilerine göre satırlara (KL / DEF / OS / FV), satır içinde eşit aralıkla yerleşir; forma numarası, ad (oyuncu sayfasına bağlantı), rozet, gol / kart simgeleri, not. Diziliş etiketi (ör. 4-4-2) kadrodaki mevkilerden hesaplanır
- [x] Maç detayı: iki takımın gerçek ilk 11'i aynı sahada (ev sahibi alt yarı yeşil, deplasman üst yarı mavi); rozet = maç reytingi, ⚽ / 🟨 / 🟥 türe göre gruplu (3'ten fazla golde "⚽×4"), maçın oyuncusu altın çerçeve + ⭐, oyundan çıkanlarda "↓63'"
- [x] Takım sayfası: "Muhtemel ilk 11" — backend'in ilk 11 kuralının rastgelelik olmayan hâli (cezalı / sakatlar hariç, her mevkide en güçlüler); rozet = oyuncu gücü, ⚽ = bu sezonki goller, kadro dışı oyuncular altta
- [x] Mobilde (≤480px) yalnızca soyadı gösterilir, simge satırı kırılmaz; 375px'te yatay taşma yok
- [x] Bulunup düzeltilen: maç akışında aynı dakikadaki olayların ters görünmesi (tutarsız sıralama karşılaştırıcısı: ikinci sarı kırmızıdan sonra görünüyordu)

## Faz 26 — Kupa Maçlarını Canlı Oynatma ✅

- [x] Kupa turu "canlı oynat": tur backend'de bir anda simüle edilir, maçların olayları çekilir ve `LiveRound` 90 dakikayı 10 saniyede yeniden oynatır (turdaki maçlar aynı anda); geçen süre başlangıç anından ölçülür (kayma yok)
- [x] Canlıyken yanıp sönen kırmızı nokta + "CANLI 47'", skor gollerin dakikası geldikçe artar, gol anında kart parlar ve "GOL!" çıkar, golcüler dakikasıyla listelenir; bitince "MS" (+ penaltılar) ve maç detayı bağlantısı
- [x] Eşleşme ağacı yayın bitene kadar yeni sonucu göstermez (sürpriz bozulmasın); "Atla" ile hemen biter; "Tüm kupayı oynat" anında oynatır. `prefers-reduced-motion` açıksa animasyon yok
- [x] Tarayıcıda çeyrek final (10 sn canlı) ve yarı final (atla, penaltılı maç) doğrulandı

---

## Önerilen Sıra Mantığı

Fazlar bağımlılık sırasına göre dizilmiştir: önce domain/veritabanı, sonra backend iş mantığı (CRUD → fikstür → simülasyon → puan tablosu → sezon), en son frontend. Fikstür ve simülasyon algoritmaları (Faz 3-4) projenin en kritik/özgün kısımları olduğu için önce backend'de sağlam ve test edilmiş halde bitirilmeli; frontend bu API'ler üzerine inşa edilecek.
