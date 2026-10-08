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

## Faz 27 — Kalıcılık, Kayıt Noktaları ve Dışa Aktarma ✅

- [x] H2 dosya modu profili (`h2file`): `jdbc:h2:file:./data/league`, `ddl-auto: update`; `.gitignore`'a `backend/data/` ve `backend/saves/`. Canlı: 3 hafta oynatıp backend yeniden başlatıldı, 27 maç ve kayıt noktası yerinde
- [x] JSON dışa/içe aktarma (`GET /api/data/export`, `POST /api/data/import`): `BackupService` tabloları JDBC metadata'sından bulur, yabancı anahtar sırasına dizer (yeni tablolar kod değişmeden yedeğe girer); her tablo sütunlar + satırlar, logolar base64. İçe aktarmada tüm tablolar boşaltılır, satırlar kendi id'leriyle eklenir, id sayaçları en büyük id'nin üstüne alınır. Bozuk / farklı sürüm dosya `400`, veri bozulmaz. Multipart sınırı 200MB'a çıktı, logo için 5MB kontrolü `FileStorageService`'e taşındı
- [x] Kayıt noktaları (`SaveSlotService`, `/api/data/saves`): adlandırılmış anlık görüntü `saves/ad.json` + kısa bilgi `ad.meta.json` (takım sayısı, sezon, oynanan maç); dosyada tutulduğu için bellek içi H2'de de yeniden başlatmada kalır. Ad doğrulaması (`../` gibi yollar `400`)
- [x] CSV (`/api/data/csv/standings|fixture|players?seasonId=`): Türkçe Excel için `;` ayraç + UTF-8 BOM, mevkiler Türkçe
- [x] Frontend: "Veri" sayfası — kayıt noktaları tablosu (kaydet / yükle / sil, yükleme onaylı), yedek indir / yükle, sezon seçerek CSV indirme
- [x] Testler 99 → **103** (`BackupIntegrationTest`: dışa aktar → hafta oynat → içe aktar → puan durumu birebir aynı, sonra sezon sorunsuz devam eder; kayıt noktası kaydet / yükle / sil; geçersiz dosya ve ad; CSV biçimi). Tarayıcıda kayıt noktası yüklendi (36 → 27 maç), 375px'te taşma yok

## Faz 28 — Canlı Lig Maçları ve Dakika Yorumları ✅

- [x] `MatchCommentary`: her olay türü için Türkçe yorum kalıpları ("GOL! Kaya'nın pasında Demir topu ağlara gönderdi."); isim ekleri ünlü uyumuyla (Kaya'nın / Ahmet'e / Öztürk'ün), aynı olay hep aynı cümleyi alır
- [x] Server-Sent Events (`GET /api/weeks/{n}/live?speed=0|1|3|10&from=`): `LiveBroadcastService` oynanmış haftanın olaylarını + oyuncu değişikliklerini dakika sırasına dizer, sanal thread'de dakika dakika iter (1x = 30 sn, 3x = 10 sn, 10x = 3 sn, 0 = bekleme yok). Olaylar: `start` (maçlar), `minute` (id = dakika, o dakikanın olayları + biriken skorlar, başlama / devre / bitiş), `end` (penaltılar dahil). Oynanmamış hafta `409`, geçersiz hız `400`. Kupa turları (101+) aynı uçtan yayınlanır; Faz 26'daki istemci zamanlayıcısı (`LiveRound`) kaldırıldı
- [x] Frontend `LiveBroadcast`: tüm maçlar aynı anda, skorlar dakikasıyla artar, gol anında "GOL!", sağda yorum akışı (en yeni üstte); hız seçici 1x / 3x / 10x ve "Atla" bağlantıyı kaldığı dakikadan yeni hızla yeniden açar. Fikstürde "Haftayı Oynat" artık yayını açar, hafta sonuçları yayın bitince yüklenir
- [x] Maç detayında olay akışında yorum cümlesi
- [x] Testler 103 → **108** (`LiveBroadcastIntegrationTest`: 0–90 dakika sırası, 90. dakika skorları = maç sonuçları, `Last-Event-ID: 40` → 41'den devam, `from=75`, kupa turu yayını; `MatchCommentaryTest`: ekler ve her olay türü). Tarayıcıda: 4. hafta 3x canlı, 5. haftada 1x'e geçiş (2 sn'de 5 dakika) + "Atla" sonrası goller tekrar etmedi, çeyrek final canlı; 375px'te taşma yok

## Faz 29 — Gelişmiş Maç Olayları ve Hakem ✅

- [x] Yeni olay türleri: `OWN_GOAL` (olayın takımı oyuncunun takımı, gol rakibin hanesine), `PENALTY_MISSED`, `VAR_DISALLOWED` (skoru değiştirmez); `match_events.penalty` (penaltı golü / kaçan penaltı, `@ColumnDefault` ile eski yedekler de yüklenir). Üretici: ortalama hakemde golün %9'u penaltıdan (takımın en güçlü forvet / orta sahası atar, asist yok), kaçan penaltı sayısı başarı oranı ~%78 olacak şekilde, penaltı olmayan gollerin %4'ü o dakikada sahadaki bir rakibin kendi kalesine golüne çevrilir (defans ağırlıklı), maç başına %10 VAR iptali
- [x] `Referee` (ad, sertlik 1–10) ve kurgusal adlarla 14 kişilik havuz (açılışta); her haftanın maçlarına farklı hakem, kupa turlarına da. Sertlik kart çarpanı 0.68–1.4, penaltı çarpanı 0.76–1.3. Maç detayında hakem adı
- [x] Hakemler sayfası (`GET /api/referees`, `/referees`, Sezonlar sayfasından bağlantı): sertlik çubuğu, maç, sarı / kırmızı, verdiği penaltı, maç başına kart
- [x] Reyting: kendi kalesine gol −1.0, kaçan penaltı −0.8, rakip penaltısını kurtaran kaleci +0.6. Oyuncu istatistiklerine `ownGoals`; gol krallığı yalnızca oyuncu golleri
- [x] Frontend: skor özetinde ve canlı yayında kendi kalesine gol doğru tarafta "(KK)", penaltı "(P)"; olay akışında "penaltı / kendi kalesine / penaltı kaçırdı / gol VAR ile iptal" notları ve yorum cümleleri
- [x] Testler 108 → **112** (sertlik 10 / 1 kart oranı > 1.6, penaltı başarısı %73–83 aralığında (6000 maç), penaltı golünde asist yok ve kaleci atmaz, kendi kalesine golü o dakikada sahadaki rakip atar, aynı haftada hakem tekrarı yok, 306 maçın hepsi hakemli; skor = takım golleri + rakibin kendi kalesine golleri). Bulunup düzeltilen: kaydedilmemiş olayların id'si null olduğu için `List.remove` yanlış olayı siliyordu (kimlikle silmeye geçildi). Canlıda bir sezon: maç başına ~4 sarı, ~0,27 penaltı, 30 kendi kalesine gol

## Faz 30 — Taktik ve Teknik Direktör ✅

- [x] `Formation` (4-4-2, 4-3-3, 3-5-2, 5-3-2, 4-5-1): mevki sayıları; her diziliş döngüde sıradaki bir sonraki ve üç sonraki dizilişi yener (±2 efektif güç; her diziliş 2'sini yener, 2'sine yenilir, simetrik)
- [x] `PlayStyle`: hücum kendi gol beklentisini ×1.15, rakibinkini ×1.10; savunma ikisini de ×0.85; dengeli ×1
- [x] `Manager` (ad, taktik ustalığı 1–100, tercih ettiği diziliş): her takıma (açılışta eski takımlara da, yeni / yükselen takımlara oluşturulurken) atanır, takımın dizilişi başlangıçta hocanın tercihi; ustalık −2.5..+2.5 efektif güç. `TacticsService`: rakiple güç farkı (ev avantajı dahil) ≥ 12 ise hücum, ≤ −12 ise savunma, arada takımın varsayılan stili
- [x] `ScoreSimulator.TeamSetup` (güç, moral, diziliş, stil, hoca bonusu); skor ve maç öncesi olasılıklar aynı kurulumla hesaplanır. `MatchDetailGenerator` ilk 11'i takımın dizilişine göre kurar (sabit 1-4-4-2 kalktı); maçta oynanan diziliş ve stil `match_team_stats`'a yazılır
- [x] Frontend: takım sayfasında "Taktik" kartı (hoca, ustalık, diziliş + varsayılan stil seçimi, hangi dizilişleri yendiği), "Muhtemel ilk 11" seçilen dizilişe göre; maç detayında iki takımın dizilişi + stili. `PUT /api/teams/{id}/tactics` (sezon ortasında da)
- [x] Testler 112 → **120** (`FormationTest`, `TacticsServiceTest`: yapay zekâ stil seçimi, hücumda iki tarafın gol beklentisi ↑ ve 5000 maçta ortalama gol ↑, diziliş avantajı / usta hoca gol payını artırır; her dizilişte ilk 11 mevki sayıları; `TacticsIntegrationTest`: 3-5-2 seçilen takım maçta 3 defans 5 orta saha). Canlı: `ddl-auto: update` ile var olan veritabanına yeni sütunlar varsayılanlarıyla eklendi, eski takımlara hoca atandı; Galatasaray 4-3-3 hücumla 3 forvetle başladı, rakip savunmaya geçti

## Faz 31 — Form, Moral ve Sakatlık Detayı ✅

- [x] Oyuncu formu: `Player.recentRatings` (son 5 maç reytingi), `form()` = 1 + (ortalama − 6.5) × 0.08, 0.9–1.1 arası; efektif güç (güç × form − yorgunluk) ilk 11 seçimine ve gol / asist ağırlığına girer
- [x] Sakatlık türleri: %70 hafif (1–2 maç), %22 orta (3–6), %8 uzun (8–20); uzun sakatlıkta %30 kalıcı 1–3 güç kaybı. Süre `match_events.injury_matches`'e, tür `Player.injurySeverity`'ye yazılır; oyuncu sayfasında "Sakatlık geçmişi" (sezon, hafta, dakika, tür, süre). Yaz arasında sakatlıklar 4 maç iyileşir, uzun sakatlıklar yeni sezona taşar (sezon iptalinde tamamen sıfırlanır)
- [x] Revir (takım sayfası): sakat / cezalı oyuncular, türü ve kaç maç sonra döneceği
- [x] Yorgunluk: `Player.consecutiveStarts` (ilk 11'de başlayınca +1, yedek kalınca / girince 0); 3 maçtan sonra her maç −2 efektif güç (en fazla −8) → aynı güçteki dinç oyuncu daha sık başlar (rotasyon). Kadroda ↗ / ↘ form ve 💤 yorgunluk göstergeleri
- [x] Testler 120 → **126** (`PlayerTest`: form hesabı, son 5 maç, sınırlar, yorgunluk; formdaki oyuncu nötrden, nötr formsuzdan daha sık başlar ve formdaki formsuzun 2 katından fazla gol atar, yorgun daha az başlar; sakatlık dağılımı (10 000 örnekte %70 / %8); form ve üst üste maç güncellemesi). Canlıda 8 hafta: revirde "Orta sakatlık · 2 maç sonra döner", oyuncu sayfasında sakatlık geçmişi; 375px'te taşma yok

## Faz 32 — Ekonomi: Bütçe, Maaş ve Sözleşme ✅

- [x] `Economy` formülleri (avro): piyasa değeri = 1M × e^((güç−60)/12) × yaş çarpanı (≤21 1.3 … 33+ 0.4) × mevki (FV 1.2, OS 1.1, DEF 0.9, KL 0.7); haftalık maaş = değerin binde 4'ü; iç saha bilet geliri = 400B × e^((takım gücü−60)/15) (4 büyükler ×1.3); başlangıç bütçesi = 20 iç saha maçı geliri; lig ödülü 1. €20M → son €2M doğrusal; kupa: çeyrek / yarı / final elenen €0,75M / €1,5M / €3M, şampiyon €6M
- [x] `Team.budget`, `Player.wage`, `Player.contractUntil` (o sezonun sonunda biter, 1–4 sezon); açılışta eski takım / oyunculara, takım oluştururken, oyuncu eklerken ve gençlere atanır. `FinanceEntry` defteri (takım, sezon, hafta, tür, tutar, açıklama); her kayıt bütçeye eklenir: iç saha bilet geliri, lig haftası başına maaşlar, sezon sonu lig ödülü, kupa ödülleri. Sezon iptalinde o sezonun kayıtları geri alınır, bütçeler sezon başına döner
- [x] Sözleşme bitişi (sezon sonu): ≤31 yaş ve takım ortalamasının en fazla 10 altındaki oyuncunun sözleşmesi %85 güncel değerine göre uzatılır, diğerleri serbest kalır (`Player.team = null`, oyuncu kaydı ve geçmişi korunur); mevki şablonu eksilirse yerine genç gelir. Serbest oyuncu desteği için istatistik / rekor / profil sorguları takımı oyuncudan değil maç kaydından okuyor
- [x] Frontend: takım sayfasında "Finans" kartı (bütçe, sezon geliri / gideri / net, haftalık maaş, kadro değeri, türe göre toplamlar, sözleşme tablosu — bu sezon biten sarı —, son kayıtlar); başlıkta bütçe. `GET /api/teams/{id}/finances`
- [x] Testler 126 → **132** (`EconomyTest`; `EconomyIntegrationTest`: her takımda bütçe = başlangıç + gelir − gider, şampiyon €20M, kupa şampiyonu €6M; sezon 1 sonunda biten tüm sözleşmeler uzatıldı ya da oyuncu ayrıldı, kadrolar şablonda ve herkesin maaşı var; sezon sıfırlanınca bütçeler başa döner). Bulunup düzeltilen: sezon iptalinde toplu JPQL silme, aynı transaction'da yüklenmiş kayıtları bağlamdan düşürmüyordu (entity olarak silmeye geçildi). Canlıda bir sezon + kupa: 4 büyükler €116–189M, küçük takımlar €2–25M; 375px'te taşma yok
- [x] Not: `ddl-auto: update` var olan NOT NULL kısıtını kaldıramaz (`players.team_id`); kalıcı H2 kullanılıyorsa önce JSON yedek alıp veritabanı silinmeli (Faz 43'te Flyway ile çözülecek)

## Faz 33 — Transfer Sistemi ✅

- [x] Transfer penceresi: lig sezonu bitince açılır, yeni sezonun fikstürü oluşturulunca kapanır (`GET /api/transfers/window`); pencere dışında teklif `409`. Açılırken: serbest oyuncular yaşlanır / emekli olur, havuza 12 yeni serbest oyuncu (en fazla 60), her takıma altyapıdan 1–2 genç (16–18), yapay zekâ 1. tur; kapanırken 2. tur. Her ikisinden sonra mevki şablonu (2 KL, 6 DEF, 6 OS, 4 FV) eksik kalan takımlara altyapıdan genç
- [x] `Transfer` (oyuncu, kimden — null = serbest —, kime, bedel, sezon); yapay zekâ: takımlar rastgele sırayla %70 olasılıkla en fazla 2 alım; önce şablon eksiği, yoksa takım gücünün 8+ altındaki en zayıf ilk 11 mevkisi için en az 3 güç daha iyi, satılabilir ve bütçesinin %40'ını aşmayan en güçlü oyuncu (serbestler bedelsiz); kadro 24'ü aşarsa en zayıflar serbest. Aynı pencerede transfer olan oyuncu yapay zekâ tarafından tekrar alınmaz
- [x] Emekli olanın yerine otomatik genç gelme kaldırıldı; yerine altyapı + serbest oyuncu + transfer (güvence olarak şablon tamamlama)
- [x] Teklif akışı (`POST /api/transfers/offers`): istenen bedel = değer × 1.1 (takımın en güçlü 3 oyuncusunda × 1.4); bedel ≥ istenen → transfer, ≥ %85 → karşı teklif, altı → ret; satıcı kadrosu 18'in / şablonun altına düşecekse ret; bütçeyi aşan teklif `400`. Serbest oyuncu imzası (`POST /api/transfers/free-agents/{id}/sign`) bedelsiz, maaş +%15. Transferde yeni forma no ve 2–4 sezonluk sözleşme, alıcıya gider / satıcıya gelir kaydı; iki takımın gücü ilk 11 ortalamasındaki değişim kadar değişir (4 büyükler 85–100 bandında kalır)
- [x] Frontend: "Transferler" sayfası — pencere durumu, alıcı takım + bütçe, arama / mevki / en fazla bedel / yalnızca serbest filtreleri, piyasa tablosu (değer, istenen, maaş, form), satır içi teklif (karşı teklifte "€X öde"), serbest oyuncuya "İmzala", son transferler; oyuncu sayfasında transfer geçmişi, serbest oyuncu gösterimi
- [x] Testler 132 → **136** (`TransferRulesTest`: istenen bedel, satılabilirlik; `TransferIntegrationTest`: pencere kapalıyken `409`, açıkken %50 ret / %90 karşı teklif / tam bedel kabul → bütçeler ve oyuncunun takımı, transfer geçmişi; bütçe aşımı `400`; serbest oyuncu imzası; yeni sezonda tüm kadrolar 18–30 ve şablonda, forma numaraları tekil, 4 büyükler ≥ 85, bütçeler ≥ 0, yapay zekâ transfer yaptı). Tarayıcıda: kadrosu 18 olan takımdan satın alma reddi, serbest oyuncu imzası, %90 teklif → karşı teklif → "öde" ile transfer; 2. sezon sonunda 20 transfer, takımlarda 2–3 altyapı genci; 375px'te taşma yok

## Faz 34 — "Takımımı Yönet" Modu (Menajer Modu) — Çekirdek ✅

- [x] `ManagerProfile` (tek kullanıcı, hesap yok: ad, takım, başladığı sezon); "Takımım" sayfasında menajer adı + takım kartlarıyla seçim (`POST /api/my-team`). Takımı devralınca yapay zekâ hocası ayrılır; "Takımı bırak" (`DELETE`) ile takıma yeniden yapay zekâ hocası gelir ve sistem tamamen otomatik çalışır (geriye uyumlu)
- [x] Hafta kullanıcı maçında bekler: yönetilen takımın o hafta / kupa turunda maçı varsa ve kadro seçilmediyse `POST /api/weeks/{n}/play`, `/api/seasons/play-all`, `/api/cup/play-round|play-all` `409` döner; `?auto=true` ile kadroyu yapay zekâ seçer. Fikstür / puan durumu / kupa sayfalarında hata kutusunda "Takımım →" ve "Kadroyu yapay zekâ seçsin" düğmeleri
- [x] Kadro yönetimi (`GET|PUT /api/my-team/lineup`, `MatchLineup`): ilk 11, diziliş, stil, kaptan, penaltıcı; doğrulama (11 farklı ve uygun oyuncu, mevki sayıları dizilişe uygun, kaptan / penaltıcı sahada). Saha üzerinde slotlar; oyuncu sürükle-bırak ya da "önce oyuncu, sonra slot" tıklamasıyla yerleşir, yanlış mevki uyarısı, "En iyi 11", diziliş değişince aynı mevkidekiler korunup boşluklar dolar. Seçilen 11 en iyi 11'den zayıfsa gol beklentisi düşer (0.8–1.0 kalite çarpanı); seçilen penaltıcı penaltıları atar
- [x] Canlı maç ve devre arası müdahale (`POST /api/my-team/live/start`, `/live/second-half`): `MatchDetailGenerator` maçı parça parça oynatabilecek şekilde yeniden düzenlendi (`LiveMatch`: `play(from, to)`, `substitute`, `finish`). İlk yarı oynanıp oturum bellekte tutulur; devre arasında en fazla 3 değişiklik (46'), ikinci yarı stili ve soyunma odası konuşması (motive et / sakinleştir / eleştir — etkisi skora bağlı, eleştiri moral düşürür); kırmızı kartla eksik kalan takımın beklentisi oyuncu sayısıyla düşer. Maç bitince kayıtlar transaction'da yüklenen nesnelere yeniden bağlanıp kaydedilir, haftanın / kupa turunun kalan maçları oynanır
- [x] Yönetim panosu: takım, sıra, güç, moral, bütçe; sıradaki maç (rakip, iç saha / deplasman, olasılıklar, kadro durumu) ve "Kadroyu belirle / Maçı canlı oyna / Hızlı oynat"; puan durumunda takımın çevresi, son 5 maç, kadro dışı oyuncular; sezon bitince transfer / kupa bağlantıları
- [x] Testler 136 → **138** (`MyTeamIntegrationTest`: kadro seçilmeden hafta `409`, önerilen kadro 11 kişi, eksik / dizilişe uymayan kadro `400`, seçilen 11 maçta oynadı; canlı maçta ilk yarı olayları ≤ 45', aynı oturum tekrar dönmez başlatılmaz, değişiklik 46'da, ikinci yarı stili kaydedildi, skor maç detayıyla aynı, haftanın diğer 8 maçı oynandı; hızlı oynat; mod kapanınca hoca atandı ve hafta beklemeden oynadı; tüm sezonu oynat `auto` ister, kupa maçı canlı oynanınca tur ilerler). Tarayıcıda: takım seçimi, öneri 5-3-2 → sürükle-bırak / tıklama / yanlış mevki uyarısı → 4-4-2 kaydet, canlı maç (ilk yarı 2-1 → kaleci yerine forvet değişikliği + hücum + motive et → 2-2), fikstürden kadro uyarısı + yapay zekâ seçeneği; 375px'te taşma yok
- [x] Not: devre arası dışında (ör. 60. dakikada) müdahale yok; tek kullanıcılık oturum bellekte (sunucu yeniden başlarsa ilk yarı yeniden oynanır, hiçbir şey kaydedilmemiş olur)

## Faz 35 — Menajer Modu: Transfer, Finans ve Yönetim Kurulu ✅

- [x] Kullanıcı için transfer: menajer modunda alıcı her zaman yönetilen takım (başka takım adına transfer `400`); yapay zekâ kullanıcının oyuncusunu doğrudan alamaz, gelen kutusuna teklif gönderir (istenen bedelin 0.9–1.15 katı); "Satışa çıkar" ile bedeli karşılayabilen en fazla iki takım teklif yapar; teklif kabul edilince oyuncu satılır, bütçe artar. Sözleşme pazarlığı (`POST /api/my-team/contracts/{id}`): oyuncu değerine göre maaş ister (23 yaş altı +%10, takımın en iyi 3 oyuncusu +%15); isteğin %95'i kabul, %80'i karşı istek, altı ret (istek mesajda); yönetilen takımda sözleşmeler yapay zekâ tarafından uzatılmaz, uzatılmayan oyuncu sezon sonunda ayrılır
- [x] Gelen kutusu (`InboxMessage`, `/api/my-team/inbox`): hoş geldin, sezon hedefleri, yönetim değerlendirmesi, transfer teklifleri, bitecek / biten sözleşmeler, sakatlık haberleri (süresiyle), altyapı gençleri, görevden alınma, iş teklifleri; okunmamış sayısı sekmede; teklifler "Sat / Reddet", "Görevi kabul et", "A takıma al / Bırak"
- [x] Yönetim kurulu (`CareerService`): sezon başında hedef — takımın güç sırası + 1 (küme düşme hattının üstünde), güç sırası ilk 2 / 4 / 8 ise kupada final / yarı final / çeyrek final; güven 0–100 (başlangıç 60): lig maçında galibiyet +2 / mağlubiyet −2, sezon sonunda hedef sıraya göre sıra başına ±5 (en fazla +20 / −30), şampiyonluk +15, eksi bütçe −15, kupa hedefi tutarsa +5 tutmazsa −10 (sonraki sezon başında)
- [x] Görevden alınma: güven 0'a düşerse ya da takım küme düşerse; takıma yapay zekâ hocası gelir, menajere (aynı anda düşen takımlar hariç) kovulduğu takımdan en fazla 5 güçlü ya da ligin en zayıf takımlarından 3 iş teklifi; kabul edince yeni takımda kariyer sürer. İşsiz menajer ekranı: iş teklifleri, kariyer, istenirse başka bir takımla yeniden başlama
- [x] Altyapı: her sezon sonunda yönetilen takıma 1–3 genç (16–18) kadroya girmeden karar bekler (`Player.academyTeam`); "A takıma al" forma no + sözleşme verir, "Bırak" futbolu bıraktırır. Serbest oyuncu listeleri altyapı gençlerini içermez
- [x] Menajer kariyeri (`ManagerSpell`, `GET /api/my-team/career`): takım takım dönem (sezon aralığı, ayrılış nedeni), maç / G / B / M, şampiyonluk, kupa ve toplamlar; "Kariyer" sekmesi
- [x] Frontend: "Takımım" sekmeleri (Pano / Gelen kutusu / Sözleşmeler / Kariyer), başlıkta güven çubuğu, sıradaki maç kartında hedefler; Transferler sayfasında menajer modunda alıcı kilitli. Türkçe ekler ünlü uyumuyla (Fenerbahçe'ye, Manisaspor'a)
- [x] Testler 138 → **140** (`CareerIntegrationTest`: hoş geldin + hedef mesajları, güveni bitmek üzere olan menajer sezon sonunda kovulur, takıma hoca gelir, iş teklifi kabulüyle yeni takım, ikinci teklif geçersiz, kariyerde 2 dönem ve 34 maç; sözleşme ret / karşı / kabul ve yeni maaş + bitiş, sezon sonunda altyapı gençleri, terfi / bırakma, satışa çıkarılan oyuncuya gelen teklif kabulüyle satış ve bütçe, başka takım adına imza `400`). Bulunup düzeltilenler: iş teklifi aynı anda küme düşen takımdan gelebiliyordu; en zayıf takımdan kovulana teklif çıkmıyordu; yeni dönem biten sezonu sayıyordu; `ScoreSimulatorTest`'teki moral testi 1000 rastgele maçla arada bir başarısız oluyordu (kesin olasılık karşılaştırmasına çevrildi). Tarayıcıda: güven ve hedefler, sözleşme reddi → "€142B ver" → imza, gelen kutusunda genç terfisi, satış teklifi; 375px'te taşma yok (bulunan: puan tablosu sarmalayıcısızdı)

## Faz 36 — Alt Ligler (2. Lig) ve Gerçek Piramit (devam ediyor)

> Yarıda: backend'de `Competition.SECOND_LEAGUE` (haftalar 201+), `Team.division`, iki ligin birlikte fikstürü, 2. Lig puan durumu / fikstür uçları (`?division=2`) ve sezon sonu düşme / yükselme yazıldı, derleniyor; testler bu değişikliklerle henüz çalıştırılmadı. Kalan: `playWeek`'in 2. Lig haftasını da oynatması, kariyer / Takımım / takım istatistiklerinin 2. Lig'e uyarlanması, lig ödülleri, testler ve frontend lig seçici.

- [ ] `League` (id, ad, seviye): 1. Lig ve 2. Lig; fikstür, puan durumu, sezonlar lig bazında
- [ ] Düşen takımlar gerçekten 2. Lig'e geçer, 2. Lig'in ilk 3'ü yükselir (şu an rastgele üretilen yükselenlerin yerine); 2. Lig takımları da oyuncu / güç / transfer sistemine dahil
- [ ] 2. Lig oynatma: 1. Lig haftasıyla birlikte otomatik oynar
- [ ] Frontend: lig seçici, 2. Lig puan durumu / fikstürü; menajer modunda alt ligdeki takım seçilebilir
- [ ] Test: düşme / yükselme takas tutarlılığı, iki lig de 18 takımda kalır

## Faz 37 — Avrupa Kupaları ve Süper Kupa

- [ ] Süper Kupa: lig şampiyonu vs kupa şampiyonu tek maç, sezon başı
- [ ] Şampiyonlar Ligi ve Avrupa Ligi: puan tablosundaki bölge şeritlerine bağlanır (ilk 1 / 2–3), kurgusal yabancı takımlar üretilir (`ForeignTeam`), grup aşaması + eleme
- [ ] Kupa altyapısı (Faz 22 `CupService`) genelleştirilir; Avrupa maçları haftalara serpiştirilir, yorgunluk / sakatlık yükü eklenir
- [ ] Frontend: Avrupa sayfası, kupa ağaçları / gruplar; takım sayfasında Avrupa istatistikleri
- [ ] Rekorlar ve ödüller Avrupa'yı da kapsar

## Faz 38 — Sezon Formatları ve Lig Ayarları

- [ ] Ayarlar sayfası: takım sayısı (en az 18 kuralı esnetilir: 8–24 çift sayı), devre sayısı (tek / çift), düşen / çıkan sayısı, güç bonusu kapalı / açık
- [ ] Play-off formatı (ilk 4 tek maç yarı final + final) ve grup + eleme formatı
- [ ] Format değişikliği sadece sezon başında yapılabilir
- [ ] Test: tek devre fikstür (n takım → n−1 hafta), play-off eşleşmeleri

## Faz 39 — xG, Şut ve Isı Haritaları

- [ ] Her şuta konum (x, y) ve kalite (xG) atanır; `MatchShot` tablosu
- [ ] Maç detayında şut haritası (SVG saha: gol / isabetli / dışarı renkleri) ve maç xG özeti
- [ ] Oyuncu sayfasında sezon şut haritası ve toplam xG – gol karşılaştırması
- [ ] Takım sayfasında "şans göstergesi" (gol − xG), sezon xG puan durumu
- [ ] Test: toplam gol ≈ toplam xG (büyük örneklemde), konumlar saha sınırları içinde

## Faz 40 — Sezon Özeti, Ödüller ve Tarihçe

- [ ] Sezon özet sayfası: şampiyon, kupa, sezonun 11'i (en yüksek reytingli, mevki başına), sürpriz takım, hayal kırıklığı, en iyi genç
- [ ] Ödüller: Altın Top (en yüksek ortalama reyting), Altın Ayakkabı, Altın Eldiven (en az gol yiyen kaleci), yılın teknik direktörü; kayıtlı ve oyuncu sayfasında rozet
- [ ] Oyuncu karşılaştırma sayfası: radar grafiği (gol, asist, reyting, dakika…) ve yan yana istatistik
- [ ] Kulüp müzesi: tüm zamanların en çok gol atanı, en çok forma giyeni, efsane oyuncular; "Şeref Salonu" (emekliler)
- [ ] Test: sezonun 11'i her mevki sayısına uyar, ödüller sezon bitince bir kez yazılır

## Faz 41 — Tahmin Oyunu

- [ ] `Prediction` (maç, skor / sonuç tahmini): kullanıcı hafta maçlarına tahmin yapar (mevcut kazanma olasılıkları gösterilir)
- [ ] Puanlama: doğru sonuç 1, doğru skor 3; sezon boyu toplam, isabet yüzdesi
- [ ] Tahmin sayfası ve sezon sonu tahmin tablosu; "Yapay zekâ ile yarış" (en yüksek olasılığı seçen bot)
- [ ] Test: puanlama kuralları, oynanmış maça tahmin girilemez

## Faz 42 — Arayüz: Koyu Tema, Çoklu Dil ve Mobil

- [ ] Koyu / açık tema (CSS değişkenleri, sistem tercihine uyum, kullanıcı seçimi saklanır)
- [ ] Türkçe / İngilizce (i18n): metinler `labels.ts` ve sayfalardan sözlük dosyasına taşınır
- [ ] Mobil gözden geçirme: tüm yeni sayfalar 375px'te taşmasız; alt gezinme çubuğu
- [ ] PDF dışa aktarma: puan durumu ve sezon özeti yazdırılabilir görünüm

## Faz 43 — Performans ve Altyapı

- [ ] Sayfalama: oyuncu istatistikleri, maç listeleri, transfer geçmişi
- [ ] Önbellek: puan durumu, rekorlar, istatistik sorguları (`@Cacheable`, hafta oynanınca temizlenir); sık sorgulanan alanlara indeks
- [ ] Flyway migration'ları (Faz 1'den ertelenen madde)
- [ ] Hata günlüğü ve basit sağlık ucu (`/actuator/health`)

## Faz 44 — Test, Denge ve Teslim

- [ ] Simülasyon denge testleri: 500+ sezon simülasyonu (4 büyük şampiyonluk oranı, ortalama gol, ev avantajı, transfer / ekonomi enflasyonu kontrolü); eşikler dışına çıkarsa test kırılır
- [ ] Frontend testleri: Vitest (bileşen) + Playwright (menajer modu uçtan uca: takım seç → maç oyna → transfer → sezon sonu)
- [ ] CI hattı (GitHub Actions): backend `./mvnw test`, frontend `npm run build` + `npm run lint` + testler
- [ ] README, ER diyagramı ve plan güncellemesi (yeni tablolar: Transfer, Referee, MatchShot, League, ManagerProfile, InboxMessage…)
- [ ] Canlı doğrulama: 8090 / 5180 portlarında çok sezonlu tur, tüm yeni sayfalar 375px'te

---

## Önerilen Sıra Mantığı

Fazlar bağımlılık sırasına göre dizilmiştir: önce domain/veritabanı, sonra backend iş mantığı (CRUD → fikstür → simülasyon → puan tablosu → sezon), en son frontend. Fikstür ve simülasyon algoritmaları (Faz 3-4) projenin en kritik/özgün kısımları olduğu için önce backend'de sağlam ve test edilmiş halde bitirilmeli; frontend bu API'ler üzerine inşa edilecek.
