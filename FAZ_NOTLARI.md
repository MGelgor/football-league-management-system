# Faz Notları (Faz 27–35)

Her fazda eklenen / değişen dosyalar ve aralarındaki akış.

## Faz 27 — Kalıcılık, Kayıt Noktaları ve Dışa Aktarma

| Dosya | Ne işe yarar |
|---|---|
| `backend/src/main/resources/application-h2file.yml` | Yeni profil. H2'yi bellek yerine `backend/data/league.mv.db` dosyasında çalıştırır; `ddl-auto: update` tabloları silmeden günceller. |
| `application.yml` | Yedek dosyaları büyük olabileceği için yükleme sınırı 200MB; kayıt noktası klasörü `app.save-dir`. |
| `service/BackupService.java` | Yedeklemenin kalbi. `JdbcTemplate` ile ham SQL çalışır (entity'lerden bağımsız). `tablesInInsertOrder()` JDBC `DatabaseMetaData`'dan tabloları ve yabancı anahtarları okur, topolojik sıralar (önce `teams`, sonra ona bağlı `players`...). `export()` her tabloyu `select * order by id` ile okur; `importSnapshot()` tabloları ters sırada boşaltır, düz sırada doldurur, `alter table ... restart with` ile id sayacını ileri alır. Logolar base64 olarak yedeğe girer. |
| `service/SaveSlotService.java` | Kayıt noktalarını `saves/` klasöründe dosya olarak tutar: `ad.json` (BackupService'in çıktısı) + `ad.meta.json` (listede gösterilen kısa bilgi). Yükleme = dosyayı okuyup `importSnapshot`. Yol kaçışına (`../`) karşı kontrol var. |
| `service/CsvExportService.java` | Var olan `StandingsService`, `FixtureService`, `PlayerService` çıktılarını CSV'ye çevirir. `;` ayraç + BOM, Türkçe Excel doğru açsın diye. |
| `controller/DataController.java` | `/api/data/...` uçları; `Content-Disposition: attachment` başlığıyla tarayıcı dosyayı indirir. |
| `dto/LeagueSnapshot.java`, `SaveSlotRequest.java`, `SaveSlotResponse.java` | Yedek dosyasının biçimi, kayıt adı doğrulaması (`@Pattern`), kayıt listesi satırı. |
| `exception/SaveSlotNotFoundException.java` + `GlobalExceptionHandler` | Olmayan kayıt → 404. |
| `service/FileStorageService.java` | Genel yükleme sınırı büyüdüğü için logo 5MB kontrolü burada yapılıyor. |
| `frontend/src/pages/DataPage.tsx` | Veri sayfası: kayıt noktaları tablosu, yedek indir/yükle (onaylı), sezon seçip CSV indirme. İndirmeler `<a href download>` ile doğrudan backend'e gider. |
| `frontend/src/api/client.ts`, `types.ts` | Yeni API çağrıları ve `downloadUrls`. |
| `backend/src/test/.../BackupIntegrationTest.java` | Dışa aktar → değiştir → içe aktar → aynı puan durumu; kayıt noktası akışı; hatalı girdiler; CSV biçimi. |

**Akış:** DataPage → `api.createSave` → `DataController.save` → `SaveSlotService.save` → `BackupService.export` → JSON dosyası. Yükleme ters yönde: dosya → `LeagueSnapshot` → `BackupService.importSnapshot` → tablolar.

## Faz 28 — Canlı Lig Maçları ve Dakika Yorumları

| Dosya | Ne işe yarar |
|---|---|
| `service/MatchCommentary.java` | Olay türüne göre Türkçe cümle üretir. `genitive()` / `dative()` son ünlüye bakarak ek seçer (Kaya → Kaya'nın, Ahmet → Ahmet'e). Kalıp seçimi `hash(dakika, oyuncu)` ile olduğu için aynı olay hep aynı cümleyi alır. Spring bean değil, saf yardımcı sınıf. |
| `service/LiveBroadcastService.java` | `timeline()` (transaction içinde) haftanın olaylarını ve değişikliklerini okuyup `LiveTimeline`'a dizer. `stream()` bir `SseEmitter` döner ve sanal thread'de her dakika için `minute` olayı gönderir; `minutes()` skorları 0. dakikadan biriktirir, böylece yayın ortadan başlasa da skor doğru olur. |
| `dto/LiveTimeline.java` | Yayının veri biçimi: maçlar, olaylar, dakika paketi, skorlar. |
| `controller/MatchWeekController.java` | `GET /api/weeks/{n}/live`. `Last-Event-ID` başlığı varsa (tarayıcı kopan bağlantıyı yenilerken gönderir) bir sonraki dakikadan başlar. |
| `exception/WeekNotPlayedException.java` | Oynanmamış haftanın yayını istenirse 409. |
| `dto/CupResponse.java`, `CupService` | Kupa turuna `weekNumber` eklendi; kupa sayfası yayını bu numarayla açar. |
| `dto/MatchDetailResponse.java`, `MatchDetailService` | Olaylara `commentary` alanı. |
| `frontend/src/components/LiveBroadcast.tsx` | `EventSource` ile yayına bağlanır; `start` → maç kartları, `minute` → skor + yorum akışı, `end` → bağlantıyı kapatıp `onFinish`. Hız değişince `{speed, from}` state'i değişir, effect bağlantıyı kapatıp yenisini açar. `lastMinute` ref'i aynı dakikanın iki kez işlenmesini engeller. Eski `LiveRound.tsx` silindi. |
| `pages/FixturePage.tsx`, `pages/CupPage.tsx` | Oynat → backend'de simüle et → `LiveBroadcast` aç → bitince sonuçları yükle. |
| `pages/MatchDetailPage.tsx` | Olay akışında yorum satırı. |

**Akış:** "Haftayı Oynat" → `POST /api/weeks/4/play` (anında simülasyon, DB'ye yazılır) → `new EventSource('/api/weeks/4/live?speed=3')` → backend olayları dakika dakika iter → istemci skorları ve akışı günceller → `end` → fikstür yeniden yüklenir.

## Faz 29 — Gelişmiş Maç Olayları ve Hakem

| Dosya | Ne işe yarar |
|---|---|
| `entity/MatchEventType.java` | `OWN_GOAL`, `PENALTY_MISSED`, `VAR_DISALLOWED` eklendi. Java'da `switch` ifadeleri enum'un tüm değerlerini kapsamak zorunda olduğu için derleyici güncellenmesi gereken her yeri (PlayerTotals, MatchSimulationService, MatchCommentary) gösterdi. |
| `entity/MatchEvent.java` | `penalty` alanı. `@ColumnDefault("false")` veritabanına varsayılan değer yazar; Faz 27'deki eski yedekler bu sütun olmadan da yüklenir. |
| `entity/Referee.java` | Hakem. `cardFactor()` / `penaltyFactor()` sertliği olasılık çarpanına çevirir (hakem yoksa ortalama). |
| `entity/Match.java` | `referee` ilişkisi. |
| `service/MatchDetailGenerator.java` | Yeni olay türleri: `goal()` önce "bu gol penaltı mı?" diye bakar; `missedPenalty()`, `disallowedGoal()` yeni olaylar; `convertOwnGoals()` iki taraf da simüle edildikten sonra bazı golleri rakip oyuncunun kendi kalesine golüne çevirir (rakibin o dakikada sahada olanları kadro kayıtlarından bulunur). Kart sayısı hakem çarpanıyla çarpılır. |
| `service/RefereeService.java`, `RefereeRepository.java`, `RefereeController.java`, `dto/RefereeResponse.java` | Hakem havuzu (açılışta), haftalık atama (aynı haftada tekrar yok), JPQL `group by` sorgularıyla maç / kart / penaltı sayıları. |
| `FixtureService`, `CupService` | Hafta / tur maçları oluşturulurken `refereeService.assign()`. |
| `PlayerTotals`, `PlayerStatsResponse` | `ownGoals`. |
| `LiveBroadcastService`, `MatchCommentary` | Kendi kalesine gol skoru rakibe yazar; yeni olaylar için yorum cümleleri; penaltı golüne özel cümle. |
| `frontend/src/labels.ts` | `EVENT_ICONS` ortak oldu; `scoringSide()` (olay hangi takımın skoruna yazılır) ve `goalSuffix()` ("(P)", "(KK)") hem maç detayında hem canlı yayında kullanılır. |
| `pages/RefereesPage.tsx` | Hakem tablosu. |
| `pages/MatchDetailPage.tsx`, `components/LiveBroadcast.tsx` | Yeni olay notları, hakem adı, doğru tarafta kendi kalesine gol. |

**Önemli ders:** Lombok `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` yalnızca `id`'yi karşılaştırır; henüz kaydedilmemiş (id = null) iki olay "eşit" sayılır. Bu yüzden listeden `remove(nesne)` yerine `removeIf(e -> e == nesne)` (kimlik karşılaştırması) kullanıldı.

## Faz 30 — Taktik ve Teknik Direktör

| Dosya | Ne işe yarar |
|---|---|
| `entity/Formation.java` | Diziliş enum'u. Her değer mevki sayılarını taşır (`count(Position)`). `advantageOver()` döngüsel avantajı hesaplar: dizilişler bir çembere dizilir, her biri bir ve üç sonraki komşusunu yener (5 elemanlı taş-kâğıt-makas). |
| `entity/PlayStyle.java` | Stil enum'u; `attack()` kendi gol beklentisinin, `concede()` rakibin gol beklentisinin çarpanı. |
| `entity/Manager.java` + `ManagerRepository` | Teknik direktör. `strengthBonus()` ustalığı küçük bir güç bonusuna çevirir. |
| `entity/Team.java` | `formation`, `playStyle`, `manager` (OneToOne). `@ColumnDefault` sayesinde var olan veritabanına sütun eklenirken eski satırlar varsayılan değeri alır. |
| `entity/MatchTeamStats.java` | Maçta gerçekten oynanan diziliş / stil (yapay zekâ stili maçtan maça değiştirebildiği için saklanır). |
| `service/ScoreSimulator.java` | Yeni `TeamSetup` kaydı ve `expectedGoals(TeamSetup, TeamSetup)`: hoca bonusu + diziliş avantajı efektif güce eklenir, stil çarpanları beklenen gollere uygulanır. Eski `expectedGoals(int...)` aynı sonucu veren sade kuruluma yönlenir. |
| `service/TacticsService.java` | Bir takımın maçtaki kurulumunu üretir; `chooseStyle()` güç farkına göre hücum / savunma / varsayılan. |
| `service/ManagerService.java` | Hoca işe alma (rastgele ad, ustalık, tercih edilen diziliş); açılışta hocasız takımlara atama. |
| `service/MatchSimulationService.java`, `MatchMapper.java` | Skor ve maç öncesi olasılık aynı `TacticsService` kurulumuyla hesaplanır (olasılıklar ile gerçekleşen maç tutarlı). |
| `service/MatchDetailGenerator.java` | `pickStartingEleven(available, formation)`: sabit 4-4-2 haritası yerine takımın dizilişi. |
| `TeamService`, `TeamController`, `dto/TacticsRequest`, `dto/TeamResponse` | `PUT /api/teams/{id}/tactics`; takım yanıtında diziliş, stil, hoca. |
| `frontend/src/labels.ts` | Diziliş / stil etiketleri, `FORMATION_COUNTS` (backend'le aynı sayılar), `formationBeats()`. |
| `pages/TeamDetailPage.tsx` | `TacticsCard`: seçim + kaydet; muhtemel ilk 11 seçilen dizilişe göre. |
| `pages/MatchDetailPage.tsx` | Diziliş başlığında stil. |

**Akış:** maç oynanırken `MatchSimulationService` → `TacticsService.setup(ev, dep)` → `ScoreSimulator.expectedGoals(kurulumlar)` → skor → `MatchDetailGenerator.generate(..., evDiziliş, depDiziliş)` → istatistiğe diziliş/stil yazılır.

## Faz 31 — Form, Moral ve Sakatlık Detayı

| Dosya | Ne işe yarar |
|---|---|
| `entity/Player.java` | `recentRatings` (son 5 reyting, metin olarak virgülle), `consecutiveStarts`, `injurySeverity`. Hesaplanan değerler: `form()`, `fatigue()`, `effectiveStrength()` = güç × form − yorgunluk. Bu yardımcılar entity'de olduğu için hem maç üreticisi hem DTO dönüşümü aynı formülü kullanır. |
| `entity/InjurySeverity.java` | Hafif / orta / uzun; `of(maç)` süreden türü bulur. |
| `entity/MatchEvent.java` | `injuryMatches`: sakatlık olayına süresi yazılır (geçmiş için). |
| `service/MatchDetailGenerator.java` | `pickStartingEleven` ve gol / asist ağırlıkları `effectiveStrength()` kullanır. |
| `service/MatchSimulationService.java` | `injure()`: süre dağılımı (`injuryDuration`), tür, uzun sakatlıkta kalıcı güç kaybı. `updateFormAndFatigue()`: her maçtan sonra oynayanların reytingi forma eklenir, ilk 11'de başlayanın sayacı artar, diğerlerininki sıfırlanır. |
| `service/FixtureService.java` | Yeni sezon (`startPlayerSeason`): sakatlık 4 maç iyileşir, yorgunluk sıfırlanır; sezon iptali (`resetPlayerStatuses`) her şeyi sıfırlar. |
| `dto/PlayerResponse`, `PlayerProfileResponse`, `PlayerService` | Form, yorgunluk, sakatlık türü, sakatlık geçmişi (`InjuryLine`). |
| `frontend/src/components/FormIndicator.tsx` | ↗ / ↘ form oku ve 💤 yorgunluk simgesi. |
| `pages/TeamDetailPage.tsx` | Kadroda form göstergesi, `Infirmary` (revir) kartı. |
| `pages/PlayerPage.tsx` | Form (son 5 maç ortalaması), sakatlık türü rozeti, sakatlık geçmişi. |

## Faz 32 — Ekonomi: Bütçe, Maaş ve Sözleşme

| Dosya | Ne işe yarar |
|---|---|
| `service/Economy.java` | Saf formüller (Spring'siz): piyasa değeri, haftalık maaş, bilet geliri, başlangıç bütçesi, lig ödülü. Test etmesi kolay olsun diye ayrı tutuldu. |
| `entity/FinanceEntry.java`, `FinanceType.java`, `FinanceEntryRepository` | Gelir/gider defteri. Bütçe = başlangıç + tüm kayıtların toplamı; hesap hatası olursa defterden doğrulanabilir (entegrasyon testi bunu yapıyor). |
| `entity/Team.java` | `budget` ve `addToBudget()`. |
| `entity/Player.java` | `wage`, `contractUntil`; `team` artık boş olabilir (serbest oyuncu). |
| `service/EconomyService.java` | `recordMatchFinances` (her maç: ev sahibine bilet geliri, lig haftasında iki takıma maaş gideri), `awardLeaguePrizes`, `awardCupPrizes`, `processContracts` (sezon sonu uzatma / serbest bırakma + şablon tamamlama), `revertSeason` (sezon iptali), `getFinances` (Finans kartı verisi), `ensureEconomy` (eksik bütçe / sözleşme tamamlama). |
| `MatchSimulationService`, `SeasonEndService`, `CupService`, `FixtureService`, `TeamService`, `PlayerService` | İlgili anlarda EconomyService çağrıları. |
| `RecordService`, `PlayerService`, repository sorguları | Oyuncunun takımı `MatchEvent.team` / `MatchAppearance.team`'den okunuyor (oyuncu sonradan takım değiştirebilir ya da serbest kalabilir; geçmiş kayıtlar o maçtaki takımı göstermeli). |
| `frontend/src/components/FinancePanel.tsx` | Finans kartı. |
| `frontend/src/labels.ts` | `formatMoney()` (€12,5M / €850B), finans türü etiketleri. |

## Faz 33 — Transfer Sistemi

| Dosya | Ne işe yarar |
|---|---|
| `entity/Transfer.java`, `TransferRepository` | Transfer kaydı; `findPlayerIdsBySeasonNumber` aynı penceredeki transferleri bulur. |
| `service/TransferService.java` | Pencere durumu (`window`), piyasa listesi (`market`), teklif (`makeOffer`: bütçe → kadro kuralı → istenen bedelle karşılaştırma), serbest oyuncu imzası, `openWindow` / `closeWindow` (sezon sonu ve yeni sezon başında çağrılır), yapay zekâ turu (`aiRound`: ihtiyaç → aday → en iyi aday → `move`), `ensureSquads` (şablon güvencesi). `move()` tek yerden transferi uygular: forma no, sözleşme, finans kayıtları, iki takımın gücü, `Transfer` kaydı. |
| `service/SquadStrength.java` | Kadronun ilk 11 ortalaması ve bir mevkideki en zayıf ilk 11 oyuncusu (takım gücü değişimi ve yapay zekâ ihtiyaç analizi için). |
| `service/SquadGenerator.java` | `academyPlayer`, `freeAgent`, `freeShirtNumber`. |
| `service/EconomyService.java` | `signTransfer` (yeni sözleşme), `recordTransfer` (alıcı / satıcı kayıtları); `processContracts` artık yalnızca uzatır / serbest bırakır. |
| `service/SeasonEndService.java` | Emekli yerine otomatik genç kaldırıldı; sözleşmelerden sonra `transferService.openWindow`. |
| `service/FixtureService.java` | Yeni sezon fikstüründen önce `transferService.closeWindow`. |
| `controller/TransferController.java` + DTO'lar | `/api/transfers/window|market|offers|free-agents/{id}/sign`, son transferler. |
| `frontend/src/pages/TransfersPage.tsx` | Piyasa, filtreler, `MarketRow` (satır içi teklif formu ve sonucu). |
| `pages/PlayerPage.tsx` | Transfer geçmişi, serbest oyuncu. |

## Faz 34 — "Takımımı Yönet" Modu (Çekirdek)

| Dosya | Ne işe yarar |
|---|---|
| `entity/ManagerProfile.java` | Kullanıcının menajer profili (ad, takım). Tabloda en fazla bir satır; `team == null` mod kapalı demek. |
| `entity/MatchLineup.java` | Bir maç için kullanıcının seçtiği kadro. İlk 11 id'leri virgüllü metin olarak saklanır (`starterIdList()` / `setStarterIdList()` dönüştürür). |
| `service/MatchDetailGenerator.java` | **En büyük değişiklik.** Maç artık `LiveMatch` nesnesiyle parça parça oynatılabiliyor: `play(1, 45, ...)` → devre arası `substitute(...)` → `play(46, 90, ...)` → `finish()`. Tek seferde oynatmak da aynı yoldan geçiyor (`generate` = `start` + `play(1, 90)` + `finish`). `SideSetup`: yapay zekâ mı seçecek, yoksa kullanıcının ilk 11'i / penaltıcısı mı; otomatik değişiklik yapılsın mı. Kendi kalesine gol dönüşümü her parçadan sonra yapılır ki kullanıcının ilk yarıda gördüğü olaylar sonradan değişmesin. |
| `service/MatchSimulationService.java` | `simulateMatches` kayıtlı kadroları (`MatchLineup`) kullanır; `requireManagedLineup` (409 kuralı); `recordManagedResult` canlı maçın sonucunu kaydeder: üretici iki istek arasında bellekte durduğu için olaylardaki oyuncu / takım / maç referansları bu transaction'da yüklenen nesnelerle değiştirilir (`rebind`). `playRestOfLeagueWeek`. Ortak kayıt kodu `MatchResults` iç sınıfında. |
| `service/TacticsService.java` | Kadro varsa diziliş / stil kadrodan; `lineupQuality()` seçilen 11'in en iyi 11'e oranı. |
| `service/MyTeamService.java` | Pano (`dashboard`), takım devralma / bırakma, sıradaki maç (`nextMatch`: lig haftası ya da kupa turu), kadro önerisi / doğrulama / kaydetme, hızlı oynatma. |
| `service/InteractiveMatchService.java` | Canlı maç oturumu (`ConcurrentHashMap`'te, maç id'siyle). `start()` ilk yarıyı oynatır; `secondHalf()` değişiklik + stil + konuşma etkilerini uygular, ikinci yarıyı oynatır, kaydeder, haftanın / turun kalanını oynatır. `talk()` konuşmanın skora göre etkisi. |
| `service/CupService.java`, `SeasonService`, controller'lar | `auto` parametresi; `finishRound` / `playRestOfRound` (kupa turu kullanıcı maçından sonra tamamlanır). |
| `service/LiveBroadcastService.java` | `buildItems()` statik: yorumlu satırlar hem veritabanından hem canlı maçın bellekteki olaylarından üretilir. |
| `controller/MyTeamController.java` + DTO'lar | `/api/my-team` uçları. |
| `frontend/src/pages/MyTeamPage.tsx` | Takım seçimi (`TeamPicker`), pano, görünümler arası geçiş (pano / kadro / canlı maç). |
| `components/LineupEditor.tsx` | Saha üzerinde slotlar; HTML5 sürükle-bırak (`draggable`, `onDragStart`, `onDrop`) ve tıklama ile yerleştirme; `buildSlots` / `refill` yardımcıları. |
| `components/LiveMatchView.tsx` | İlk yarıyı dakika dakika gösterir; faz (ilk yarı / devre arası / ikinci yarı / maç sonu) dakikadan türetilir; devre arası paneli (değişiklik seçici, stil, konuşma); maç sonu reytingleri ve diğer sonuçlar. |
| `components/ErrorAlert.tsx` | Kadro bekleyen maç hatasında "Takımım →" ve "Kadroyu yapay zekâ seçsin". |

**Akış (canlı maç):** Takımım → "Maçı canlı oyna" → `POST /live/start` (ilk yarı backend'de oynanır, bellekte durur) → istemci 0-45 olaylarını oynatır → devre arası kararları → `POST /live/second-half` (değişiklik, stil, konuşma → ikinci yarı → kayıt → haftanın kalanı) → istemci 46-90'ı oynatır → maç sonu.

## Faz 35 — Menajer Modu: Transfer, Finans ve Yönetim Kurulu

| Dosya | Ne işe yarar |
|---|---|
| `entity/InboxMessage.java`, `InboxMessageType.java`, `InboxMessageRepository` | Gelen kutusu mesajı; teklif türlerinde oyuncu / karşı takım / bedel ve `resolved` (yanıtlandı) alanı. |
| `entity/ManagerSpell.java`, `ManagerSpellRepository` | Menajerin bir takımdaki dönemi (başlangıç / bitiş sezonu, ayrılış nedeni). Kariyer özeti bunlardan ve maç sonuçlarından hesaplanır. |
| `entity/ManagerProfile.java` | `confidence`, `targetRank`, `cupTarget`, `objectiveSeason`. |
| `entity/Player.java` | `academyTeam`: altyapıdan gelip menajerin kararını bekleyen genç. |
| `service/InboxService.java` | Mesaj yazma / listeleme; `managedTeam()` ve `isManaged()` diğer servislerin "bu takım kullanıcının mı?" sorusunu cevaplar. Yalnızca repository'lere bağımlı, böylece döngüsel bağımlılık oluşmaz. |
| `service/CareerService.java` | Kancalar: `onSeasonStart` (hedefler, kupa hedefi değerlendirmesi, biten sözleşme hatırlatması), `onMatchesPlayed` (güven, sakatlık haberleri), `onLeagueFinished` (sezon değerlendirmesi, kovulma). İşlemler: teklif kabul / ret (`accept` / `reject`), sözleşme pazarlığı, satışa çıkarma, altyapı terfisi, iş teklifi, `career()`. |
| `service/TransferService.java` | Yapay zekâ yönetilen takımdan alım yapmaz, teklif gönderir; yönetilen takım için yapay zekâ alım yapmaz; altyapı gençleri karar bekler; `acceptManagerOffer`, `listForSale`; menajer modunda yalnızca kendi takımı adına alım. |
| `service/EconomyService.java` | `renew()` (sözleşme pazarlığı kuralları); yönetilen takımda sözleşmeler otomatik uzatılmaz. |
| `MatchSimulationService`, `SeasonEndService`, `FixtureService`, `MyTeamService` | Kariyer kancalarının çağrıldığı yerler. |
| `controller/MyTeamController.java` | `/inbox`, `/inbox/{id}/accept|reject|read`, `/contracts/{id}`, `/sell/{id}`, `/academy`, `/career`. |
| `frontend/src/components/InboxPanel.tsx` | Mesaj listesi, okunmamış vurgusu, teklif düğmeleri. |
| `components/ContractsPanel.tsx` | Sözleşme tablosu, satır içi uzatma formu (maaş + süre, "€X ver"), "Satışa çıkar". |
| `components/CareerPanel.tsx` | Kariyer özeti ve dönemler tablosu. |
| `pages/MyTeamPage.tsx` | Sekmeler, güven çubuğu, hedefler, işsiz menajer ekranı. |

## Faz 45 — Oda Altyapısı

| Dosya | Ne işe yarar |
|---|---|
| `entity/Room.java`, `RoomRepository` | Oda: davet kodu (6 karakter, 0/O ve 1/I yok), ad, şema adı (`room_<kod>`). Varsayılan şemada durur. |
| `entity/RoomMember.java`, `RoomMemberRepository` | Odadaki oyuncu: takma ad, sahip mi, token'ın SHA-256 özeti (token'ın kendisi saklanmaz; veritabanı sızsa bile kimse üye yerine geçemez). |
| `config/RoomContext.java` | İsteğin hangi odada çalıştığı. `ThreadLocal`: her istek kendi thread'inde çalıştığı için aynı anda gelen iki farklı odanın isteği birbirine karışmaz. `call(oda, iş)` işi bir odanın içinde çalıştırır (oda kurulurken kullanılır). |
| `config/RoomAwareDataSource.java` | **Mimarinin kalbi.** Spring'in bağlantı havuzunu sarar; her bağlantı verilirken `connection.setSchema(...)` ile şemayı o anki odaya ayarlar. Hibernate, JdbcTemplate ve yedekleme aynı yerden bağlantı aldığı için `select * from teams` odanın `teams` tablosunu okur; mevcut 40+ servis hiç değişmeden odaya özel çalışır. İç sınıf `Wrapper` (BeanPostProcessor) Spring'in oluşturduğu DataSource'u bununla değiştirir. H2 şema adlarını büyük, PostgreSQL küçük harfle sakladığı için ad buna göre çevrilir. |
| `config/RoomFilter.java` | Her `/api` isteğinde: `X-Room-Code` / `X-Room-Token` başlıklarını (ya da `?room=&token=`) okur, `RoomService.authenticate` ile üyeliği kontrol eder, `RoomContext`'i doldurur ve istek bitince temizler. GET dışındaki istekleri yalnızca sahip yapabilir. Oda yoksa varsayılan lig; `app.rooms.required=true` ise 401. `/api/rooms` uçlarını filtrelemez (oda bilgisi varsayılan şemadadır). |
| `service/RoomService.java` | `create`: oda + sahip kaydı → `SchemaManager.forSchema(şema).exportMappedObjects(true)` ile şema ve tüm tablolar → odanın içinde `initializeLeague(18)`; bir adım başarısız olursa şema ve kayıt silinir. `join` (dolu oda / aynı takma ad 409), `get`, `leave` (sahip ayrılamaz), `delete` (`drop schema … cascade`), `authenticate`. Oda kurarken transaction'lar elle (`TransactionTemplate`) açılır, çünkü şema oluşturma ve lig hazırlığı açık bir transaction'ın (varsayılan şemadaki bağlantının) dışında olmalı. |
| `controller/RoomController.java` + `CreateRoomRequest`, `JoinRoomRequest`, `RoomResponse`, `RoomJoinResponse` | `POST /api/rooms`, `POST /api/rooms/{kod}/join`, `GET /api/rooms/{kod}`, `DELETE /api/rooms/{kod}/members/me`, `DELETE /api/rooms/{kod}`. Token yalnızca kurma / katılma yanıtında bir kez döner. |
| `exception/RoomNotFoundException` (404), `RoomConflictException` (409), `RoomAccessException` (403), `GlobalExceptionHandler` | Oda hataları. |
| `config/StartupDataInitializer.java` | `initializeLeague(minTeams)`: açılışta (0 ile) ve her yeni odada (18 ile) aynı hazırlık; 1. Lig eksikse rastgele takımlarla tamamlanır. |
| `service/TeamService.java` | `countFirstDivision()`. |
| `service/InteractiveMatchService.java` | Canlı maç oturumları bellekte maç id'siyle tutuluyordu; her odanın maç id'leri 1'den başladığı için anahtar artık `şema:maçId`. |
| `service/BackupService.java` | `rooms` / `room_members` yedeğe girmez ve geri yüklemede silinmez (yoksa tek kişilik ligde yedek yüklemek tüm odaları silerdi). |
| `service/SaveSlotService.java` | Kayıt noktası dosyaları tüm odaların ortak klasöründe olduğu için odada kapalı (liste boş, kaydet / yükle / sil 400). |
| `application.yml` | `app.rooms.required`, `max-rooms` (30), `max-members` (8). |
| `RoomIntegrationTest`, `RoomRequiredIntegrationTest` | İki oda birbirini görmez, maç 1 odada var / varsayılan ligde yok, üye okur ama yazamaz, sorgu parametreleri, ayrılma / silme, kayıt noktası ve yedek, oda zorunlu modu. |
| `frontend/src/api/rooms.ts` | Katılınan odalar ve aktif oda `localStorage`'da (`fl.rooms`, `fl.activeRoom`); `roomHeaders()`, `withRoom(url)` (EventSource ve indirme linkleri başlık gönderemez), `inviteLink`. |
| `frontend/src/api/client.ts` | `request()` her isteğe aktif odanın başlıklarını ekler; oda uçları; `downloadUrls` oda parametreli (`backup` artık fonksiyon). |
| `frontend/src/pages/RoomsPage.tsx` | Oda kur / koda katıl / aktif oda kartı (kod, davet linki + kopyala, oyuncular, ayrıl / sil) / odalarım. `/join/:kod` de bu sayfayı açar (kod dolu, katılma kartı üstte). Odaya girince sayfa yeniden yüklenir ki tüm sayfalar odanın verisini çeksin. |
| `frontend/src/App.tsx`, `index.css` | "Odalar" menüsü, üst çubukta aktif oda etiketi (sahip / üye), `/rooms` ve `/join/:code` yolları; oda yoksa açılış sayfası Odalar. |
| `components/LiveBroadcast.tsx`, `pages/DataPage.tsx` | EventSource ve yedek linki `withRoom` ile. |

**Akış (oda içinde bir istek):** tarayıcı `GET /api/standings` + `X-Room-Code: ALLZH7`, `X-Room-Token: …` → `RoomFilter` token özetini `room_members`'ta bulur → `RoomContext = {şema: room_allzh7, sahip: …}` → controller → servis → Hibernate bağlantı ister → `RoomAwareDataSource` `setSchema("room_allzh7")` → sorgu odanın tablolarında çalışır → istek bitince `RoomContext` temizlenir.

**Akış (oda kurma):** `POST /api/rooms` → `rooms` + `room_members` (varsayılan şema) → `create schema room_xxx` + ~30 tablo → `RoomContext.call(oda, initializeLeague(18))`: 4 büyükler, 14 rastgele takım, hakemler, hocalar, bütçeler odanın şemasına yazılır → yanıt: oda + token → tarayıcı token'ı saklar, aktif oda yapar.

## Faz 45 eki — Yayın Hazırlığı

| Dosya | Ne işe yarar |
|---|---|
| `config/RoomSchemas.java` | Oda şemalarını oluşturur (`create`) ve günceller (`update`). Hibernate açılışta yalnızca varsayılan şemayı günceller; aynı işlemi her oda şeması için Hibernate'in şema aracıyla (`SchemaManagementToolCoordinator`, `hibernate.default_schema` = oda şeması) yapar. Gereken eşleme bilgisini (Metadata) açılışta `Integrator` olarak yakalar. `RoomService.run` açılışta tüm odaları günceller: yeni sürümdeki tablo / sütunlar eski odalara da eklenir. |
| `application-prod.yml` | Yayın profili: veritabanı bilgileri ortam değişkenlerinden, `app.rooms.required=true`, CORS kökeni. |
| `config/WebConfig.java` | CORS: frontend başka adreste (GitHub Pages) olduğu için tarayıcı backend'e izin sorar; izinli başlıklar oda başlıkları. |
| `controller/HealthController.java` | `GET /health`: Render "uygulama ayakta mı?" diye buraya bakar. |
| `backend/Dockerfile` | İki aşama: JDK ile derle, yalnızca JRE ile çalıştır; 512 MB'a göre JVM ayarları. |
| `render.yaml` | Render Blueprint: Docker web servisi + ücretsiz Postgres; veritabanı bilgileri `fromDatabase` ile servise kendiliğinden bağlanır (şifre elle girilmez). |
| `.github/workflows/pages.yml` | Her main push'unda frontend'i `VITE_API_URL` (depo değişkeni `API_URL`) ve `/football-league-management-system/` alt yoluyla derler, Pages'e yayınlar; `404.html` kopyası `/join/KOD` gibi yolların açılması için. |
| `frontend/src/api/config.ts` | `apiUrl()` (backend adresi ön eki), `appPath()` (alt yol). `client.ts`, `LiveBroadcast`, `TeamLogo`, `RoomsPage`, `main.tsx` (router `basename`), `vite.config.ts` (`base`) bunları kullanır. |

## Faz 46 — Oda Yaşam Döngüsü ve Zamanlayıcı

| Dosya | Ne işe yarar |
|---|---|
| `entity/RoomStatus.java`, `entity/Room.java` | `LOBBY / SEASON / TRANSFER_WINDOW`; maç saati, saat dilimi, pencere süresi, `nextEventAt` (sıradaki olay). |
| `entity/RoomMember.java` | `ready` (hazırım); aşama değişince sıfırlanır (`RoomMemberRepository.resetReady`). |
| `service/RoomLeague.java` | Odanın içinde ligi bir adım ilerletir: `playMatchDay` (sıradaki hafta, lig bittiyse kupayı başlatıp sıradaki tur), `startSeason` (yeni fikstür, pencereyi kapatır), `nextMatchDay` ("5. hafta", "Kupa finali"). Ne oynanacağını ligin kendisinden okur; bu yüzden aynı gün iki kez oynanmaz. |
| `service/RoomScheduler.java` | **Takvim.** `tick(oda)`: `nextEventAt` geçmişse olayı işler, takvimi ilerletir, hâlâ geçmişse tekrarlar (uyuyan sunucu kaçırdığı günleri sırayla oynar). Sezonda sonraki gün = planlanan günün ertesi, maç saatinde (erken oynatılan gün tekrar oynanmaz). Final bitince pencere `transferHours` saat; pencere sonunda yeni sezon, ilk maç en az 10 dk sonraki maç saati. Oda başına `ReentrantLock`. Saat `Clock` bean'inden (testte elle ilerletilir). |
| `config/RoomTickJob.java` | Dakikada bir `tickAll` (sunucu uyanıkken). |
| `controller/InternalController.java` | `GET /internal/tick`: harici cron için; 20 sn'den sık çağrılırsa atlar. |
| `config/RoomFilter.java` | Odada maç oynatan uçlar 403; odanın her isteğinden önce `tick` (sunucu uyanınca ilk istek takvimi yetiştirir). |
| `service/RoomService.java` | `updateSettings`, `start`, `setReady` (herkes hazırsa başlat / pencereyi kapat), `runNow`, `get` (önce tick). Oda yanıtı odanın şemasından da okuduğu için bu metodlar açık transaction içinde çalışmaz (yoksa sorgu varsayılan şemanın bağlantısına giderdi); üye `findWithRoom` ile odasıyla birlikte tek sorguda gelir. |
| `config/TimeConfig.java` | `Clock` bean'i. |
| `.github/workflows/tick.yml` | 10 dakikada bir `/internal/tick`. |
| `RoomSchedulerIntegrationTest`, `MutableClock` | Takvim testleri (saat elle ilerletilir). |
| `frontend/src/roomTime.ts` | "bugün 21:00", "2 sa 13 dk sonra", tek satır oda durumu. |
| `components/RoomStatusBar.tsx` | Üst şerit; 30 sn'de bir ve oda değişince (`notifyRoomChanged`) tazelenir. |
| `components/RoomSchedule.tsx` | Odalar sayfasında takvim: durum, Hazırım / Transferlerim bitti, Ligi başlat, şimdi oynat, takvim ayarları. |
| `api/rooms.ts` | `inRoom()`, `notifyRoomChanged` / `onRoomChanged`. |
| `FixturePage`, `CupPage`, `StandingsPage`, `MyTeamPage`, `DataPage` | Odada maç oynatan / sezonu değiştiren düğmeler gizli. |

**Akış (maç günü):** 21:00 → (sunucu uyanıksa dakikalık iş, uyuyorsa cron'un isteği ya da bir oyuncunun isteği) → `RoomScheduler.tick` → kilit → `RoomContext` = oda → `RoomLeague.playMatchDay` → `MatchSimulationService.playWeek(n, auto=true)` (yönetilen takımın kadrosu yoksa yapay zekâ seçer) → `nextEventAt` = ertesi gün 21:00 → kilit bırakılır.

## Faz 47 — Çok Oyunculu Menajer ve Botlar

| Dosya | Ne işe yarar |
|---|---|
| `service/Managers.java` | **Yeni merkez.** "Şu anki menajer" (`current`): odada isteği yapan üyenin profili (`RoomContext.memberId`), tek kişilik ligde tek profil, zamanlayıcının işinde boş. "İnsan takımları" (`withTeam`, `ofTeam`, `isHuman`, `humanTeamIds`). Eskiden yedi serviste `findFirstByOrderByIdAsc()` ile "tek menajer" varsayılıyordu; hepsi buraya taşındı. |
| `entity/ManagerProfile.java` | `memberId`: profil hangi oda üyesinin. |
| `entity/InboxMessage.java`, `InboxMessageRepository` | `profile`: mesajın alıcısı. Liste / okunmamış / iş teklifleri o menajere göre. `profile` null olan eski kayıtlar tek kişilik ligin menajerine sayılır (eski veriler kaybolmasın). |
| `entity/ManagerSpell.java`, `ManagerSpellRepository` | `profile`: kariyer dönemi kimin; kariyer özeti menajere göre. |
| `service/InboxService.java` | `post(alıcı, …)`, `postToTeam(takım, …)` (takımı yöneten menajere; bot ise yazılmaz), `isMine` (başkasının mesajı açılamaz). |
| `service/CareerService.java` | Sezon başı, maç sonuçları, lig sonu kancaları her insan menajer için ayrı çalışır (`seasonStart`, `matchesPlayed`, `leagueFinished`). Dönem açma / kapama profile göre; iş teklifini kabul ederken takımı başka insan yönetiyorsa hata. |
| `service/TransferService.java` | Odada takımı olmayan transfer yapamaz, herkes yalnızca kendi takımı adına; yapay zekâ insan takımlarından doğrudan almaz, menajerine teklif gönderir; altyapı gençleri insan takımlarında menajerin kararını bekler. |
| `service/MyTeamService.java` | Takım devralırken "bu takımı X yönetiyor" (409); `release(memberId)`: ayrılan üyenin takımına yapay zekâ hocası atanır. Odada durum metni "oda takviminde oynanacak". |
| `service/TeamService.java` | Odada yalnızca kendi takımının taktiği (403). |
| `service/SeasonEndService.java` | Ligden ayrılacak 2. Lig takımları da iş teklifi yapamaz. |
| `config/RoomContext.java` | `lineupLockAt` (maç saati − 5 dk), `Current.system(...)` (zamanlayıcı işleri). |
| `config/RoomFilter.java` | Üye yazma izni (Takımım, transfer teklifi, kendi taktiği); kilitten sonra kadro / taktik 409; maç günü oynandıysa üyelik yeniden okunur (kilit ertesi güne geçer). |
| `service/RoomService.java`, `RoomController` | `kick` (`DELETE /api/rooms/{kod}/members/{id}`), ayrılma / atılmada takım bırakılır; oda yanıtında üyelerin takımları. |
| `RoomManagersIntegrationTest` | İki menajerli oda senaryoları. |
| `frontend/src/api/useActiveRoom.ts` | Aktif oda bilgisi (30 sn / oda değişince), `useNow`. Şerit ve Takımım kullanır. |
| `pages/MyTeamPage.tsx` | Takım seçici odada kilitli takımlar, menajer adı takma addan; panoda kilit notu (`LineupLockNote`). |
| `pages/RoomsPage.tsx` | Üyelerin takımları, sahip için "Çıkar". |

**Akış (iki menajerli maç günü):** Ayşe ve Bora 20:55'e kadar kadrolarını `PUT /api/my-team/lineup` ile kaydeder (her biri kendi takımının sıradaki maçı için `MatchLineup`) → 20:55'ten sonra filtre 409 → 21:00 tick → `playWeek(n, auto=true)`: kadrosu olan takımlar kaydedilen 11 ile, diğerleri (botlar ve kadro vermeyen insan) yapay zekânın seçtiğiyle oynar → `CareerService.onMatchesPlayed` her insan menajerin güvenini ayrı ayrı günceller, sakatlık mesajları ilgili menajere gider.
