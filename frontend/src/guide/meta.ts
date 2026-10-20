import type { ModuleMeta } from './parts'

export const architecture: ModuleMeta = {
  id: 'mimari',
  code: 'DM 01',
  title: 'Mimari',
  summary: 'Backend katmanları, bir isteğin yolu, hata yolu, frontend yapısı ve çalışma profilleri.',
  lead: 'Bu modül sistemin katmanlarını anlatır. Bir isteğin bu katmanlardan nasıl geçtiğini gösterir. Hata yolunu, frontend yapısını ve veritabanı profillerini de gösterir.',
  source: 'backend/src/main/java/com/footballleague · frontend/src',
  sections: [
    { id: 'katman', no: '1.1', title: 'Backend katmanları' },
    { id: 'istek', no: '1.2', title: 'Bir isteğin yolu' },
    { id: 'hata', no: '1.3', title: 'Hata yolu' },
    { id: 'frontend', no: '1.4', title: 'Frontend yapısı' },
    { id: 'profil', no: '1.5', title: 'Çalışma profilleri' },
  ],
}

export const cupLive: ModuleMeta = {
  id: 'kupa',
  code: 'DM 09',
  title: 'Kupa, canlı yayın ve animasyon',
  summary: 'Eleme ağacı, penaltı atışları, SSE ile canlı yayın, dakika yorumları ve sahada 2D maç animasyonu.',
  lead: 'Bu modül sezon sonu kupasını, canlı yayını ve maç animasyonunu anlatır. Lig bitince ilk 8 takım tek maçlık eleme oynar. Lig haftaları ve kupa turları aynı canlı yayın sistemiyle izlenir. Her oynanmış maç sahada animasyonla da izlenebilir.',
  source: 'CupService · LiveBroadcastService · MatchCommentary · match2d/ · MatchCanvas.tsx',
  sections: [
    { id: 'agac', no: '9.1', title: 'Eleme ağacı' },
    { id: 'penalti', no: '9.2', title: 'Penaltı atışları' },
    { id: 'yayin', no: '9.3', title: 'Canlı yayın' },
    { id: 'yorum', no: '9.4', title: 'Dakika yorumları' },
    { id: 'animasyon', no: '9.5', title: 'Maç animasyonu' },
    { id: 'fark', no: '9.6', title: 'Lig ve kupa farkı' },
    { id: 'oynat', no: '9.7', title: 'Kupayı oynatın' },
  ],
  pages: [
    { to: '/cup', label: 'Kupa' },
    { to: '/fixture', label: 'Fikstür' },
  ],
}

export const dataModel: ModuleMeta = {
  id: 'veri',
  code: 'DM 02',
  title: 'Veri modeli',
  summary: '17 tablo, dört alan, saklanan ve her istekte hesaplanan veri.',
  lead: 'Bu modül veritabanındaki 17 tabloyu anlatır. Tablolar dört alana ayrılır. Bazı veriler tabloda saklanmaz; sistem bu verileri her istekte hesaplar. Bu modül bu ayrımı da gösterir.',
  source: 'entity/ · ER_DIYAGRAMI.md',
  sections: [
    { id: 'harita', no: '2.1', title: 'Tablo haritası' },
    { id: 'hesap', no: '2.2', title: 'Saklanan ve hesaplanan veri' },
    { id: 'yazim', no: '2.3', title: 'Bir maç neleri yazar' },
    { id: 'kararlar', no: '2.4', title: 'Tasarım kararları' },
  ],
}

export const dataSaves: ModuleMeta = {
  id: 'kayit',
  code: 'DM 10',
  title: 'Veri ve kayıt',
  summary: 'Kayıt noktaları, JSON yedek, geri yükleme, CSV dışa aktarma ve kalıcı veritabanı.',
  lead: 'Bu modül oyunun verisini korumayı anlatır. Kullanıcı oyunu adlandırılmış kayıt noktalarına kaydeder. Tüm ligi tek bir JSON dosyasına yedekler. Tabloları Excel için CSV olarak indirir.',
  source: 'BackupService · SaveSlotService · CsvExportService · DataPage.tsx',
  sections: [
    { id: 'yontem', no: '10.1', title: 'Üç yöntem' },
    { id: 'yedek', no: '10.2', title: 'Yedek nasıl çalışır' },
    { id: 'kayit-al', no: '10.3', title: 'Kayıt noktası alın' },
    { id: 'csv', no: '10.4', title: 'CSV dosyaları' },
    { id: 'sema', no: '10.5', title: 'Şema değişikliğinde veriyi taşıyın' },
  ],
  pages: [{ to: '/data', label: 'Veri' }],
}

export const economy: ModuleMeta = {
  id: 'ekonomi',
  code: 'DM 07',
  title: 'Ekonomi ve transfer',
  summary: 'Piyasa değeri ve maaş, gelir ve gider, sözleşmeler, transfer penceresi, teklifler, yapay zekâ alımları.',
  lead: 'Bu modül takımların parasını ve oyuncu hareketlerini anlatır. Her takımın bir bütçesi var. Bilet geliri ve ödüller bütçeyi artırır, maaşlar azaltır. Lig bitince transfer penceresi açılır ve oyuncular takım değiştirir.',
  source: 'Economy · EconomyService · TransferService',
  sections: [
    { id: 'deger', no: '7.1', title: 'Piyasa değeri ve maaş' },
    { id: 'butce', no: '7.2', title: 'Gelir ve gider' },
    { id: 'sozlesme', no: '7.3', title: 'Sözleşmeler' },
    { id: 'pencere', no: '7.4', title: 'Transfer penceresi' },
    { id: 'teklif', no: '7.5', title: 'Teklif yapın' },
    { id: 'yapay', no: '7.6', title: 'Yapay zekâ transferleri' },
  ],
  pages: [
    { to: '/transfers', label: 'Transferler' },
    { to: '/teams', label: 'Takım sayfası → Finans' },
  ],
}

export const managerMode: ModuleMeta = {
  id: 'menajer',
  code: 'DM 08',
  title: 'Menajer modu',
  summary: 'Takımı devralma, kadro seçimi, canlı maç ve devre arası, rakip analizi, yönetim kurulu, gelen kutusu.',
  lead: 'Bu modül "Takımımı Yönet" modunu anlatır. Kullanıcı bir takımın teknik direktörü olur. Kadroyu seçer, maçı canlı oynar, transfer ve sözleşme kararları verir. Yönetim kurulu sonuçları değerlendirir ve gerekirse menajeri görevden alır.',
  source: 'MyTeamService · InteractiveMatchService · CareerService · InboxService',
  sections: [
    { id: 'dongu', no: '8.1', title: 'Modun yaşam döngüsü' },
    { id: 'kadro', no: '8.2', title: 'Kadro seçimi' },
    { id: 'canli', no: '8.3', title: 'Canlı maç ve devre arası' },
    { id: 'rakip', no: '8.4', title: 'Rakip analizi' },
    { id: 'guven', no: '8.5', title: 'Yönetim kurulu ve güven' },
    { id: 'kutu', no: '8.6', title: 'Gelen kutusu, sözleşme ve satış' },
    { id: 'baslat', no: '8.7', title: 'Takımı devralın' },
  ],
  pages: [{ to: '/my-team', label: 'Takımım' }],
}

export const matchEngine: ModuleMeta = {
  id: 'mac',
  code: 'DM 04',
  title: 'Maç motoru',
  summary: 'Takım kurulumu, skor ve olasılıklar, ilk 11, dakika dakika olaylar, yorgunluk, reyting.',
  lead: 'Bu modül bir maçın nasıl oynandığını anlatır. Sistem önce iki takımın kurulumunu hazırlar ve skoru hesaplar. Sonra skoru dakika dakika olaylara dağıtır. En son takımları ve oyuncuları günceller.',
  source: 'TacticsService · ScoreSimulator · MatchDetailGenerator · MatchSimulationService',
  sections: [
    { id: 'skor', no: '4.1', title: 'Takım kurulumu ve skor' },
    { id: 'olasilik', no: '4.2', title: 'Kazanma olasılıkları' },
    { id: 'kadro', no: '4.3', title: 'İlk 11' },
    { id: 'olay', no: '4.4', title: 'Dakika dakika olaylar' },
    { id: 'yorgunluk', no: '4.5', title: 'Maç içi yorgunluk' },
    { id: 'reyting', no: '4.6', title: 'İstatistik ve reyting' },
    { id: 'sonra', no: '4.7', title: 'Maçtan sonra' },
  ],
  pages: [
    { to: '/fixture', label: 'Fikstür' },
    { to: '/referees', label: 'Hakemler' },
  ],
}

export const overview: ModuleMeta = {
  id: 'genel',
  code: 'DM 00',
  title: 'Genel bakış',
  summary: 'Sistemin parçaları, iki çalışma modu, bir sezonun akışı ve çalıştırma adımları.',
  lead: 'Bu rehber sistemin parçalarını ve çalışma şeklini anlatır. Her modül bir konuyu anlatır. Şekiller ana bilgi kaynağıdır. Metin şekilleri açıklar.',
  source: 'README.md · GELISTIRME_PLANI.md',
  sections: [
    { id: 'ne', no: '0.1', title: 'Sistem ne yapar' },
    { id: 'parcalar', no: '0.2', title: 'Ana parçalar' },
    { id: 'modlar', no: '0.3', title: 'İki çalışma modu' },
    { id: 'akis', no: '0.4', title: 'Bir sezonun akışı' },
    { id: 'calistir', no: '0.5', title: 'Sistemi çalıştırın' },
    { id: 'terimler', no: '0.6', title: 'Terimler' },
  ],
  pages: [
    { to: '/teams', label: 'Takımlar' },
    { to: '/fixture', label: 'Fikstür' },
    { to: '/my-team', label: 'Takımım' },
  ],
}

export const players: ModuleMeta = {
  id: 'oyuncu',
  code: 'DM 05',
  title: 'Oyuncular',
  summary: 'Kadro yapısı, form, maçlar arası yorgunluk, ceza ve sakatlık, gelişim, emeklilik.',
  lead: 'Bu modül oyuncunun yaşam döngüsünü anlatır. Oyuncu bir kadroya girer, maç oynar, formu değişir, ceza alır veya sakatlanır. Her sezon sonunda yaşlanır, gelişir veya geriler. Sonunda emekli olur.',
  source: 'Player · SquadGenerator · PlayerDevelopment · PlayerService',
  sections: [
    { id: 'kadro', no: '5.1', title: 'Kadro yapısı' },
    { id: 'efektif', no: '5.2', title: 'Form ve efektif güç' },
    { id: 'durum', no: '5.3', title: 'Oyuncu durumları' },
    { id: 'sakatlik', no: '5.4', title: 'Sakatlık türleri' },
    { id: 'gelisim', no: '5.5', title: 'Yaş, gelişim ve emeklilik' },
    { id: 'istatistik', no: '5.6', title: 'İstatistiklerin kaynağı' },
  ],
  pages: [
    { to: '/teams', label: 'Takımlar' },
    { to: '/standings?view=players', label: 'Oyuncu istatistikleri' },
  ],
}

export const seasonCycle: ModuleMeta = {
  id: 'sezon',
  code: 'DM 03',
  title: 'Sezon döngüsü',
  summary: 'Sezon durumları, fikstür algoritması, hafta sırası, sezon sonu ve küme düşme.',
  lead: 'Bu modül bir sezonun başından sonuna kadar olan yolu anlatır. Sistem her an bir durumdadır. Her durum bazı işlemlere izin verir ve bazılarını engeller.',
  source: 'FixtureService · RoundRobinScheduler · SeasonEndService',
  sections: [
    { id: 'durum', no: '3.1', title: 'Sezon durumları' },
    { id: 'fikstur', no: '3.2', title: 'Fikstür algoritması' },
    { id: 'sira', no: '3.3', title: 'Hafta sırası' },
    { id: 'son', no: '3.4', title: 'Sezon sonu' },
    { id: 'dusme', no: '3.5', title: 'Düşme ve yükselme' },
    { id: 'sifirla', no: '3.6', title: 'Sezonu sıfırlayın' },
  ],
  pages: [
    { to: '/fixture', label: 'Fikstür' },
    { to: '/standings', label: 'Puan Durumu' },
    { to: '/seasons', label: 'Sezonlar' },
  ],
}

export const tactics: ModuleMeta = {
  id: 'taktik',
  code: 'DM 06',
  title: 'Taktik ve hakem',
  summary: 'Beş diziliş ve aralarındaki döngü, oyun stilleri, teknik direktör, hakem sertliği.',
  lead: 'Bu modül maçın sonucunu değiştiren taktik kararları ve hakemi anlatır. Etkiler küçüktür. Güç farkı hâlâ en büyük etkendir. Ama yakın güçteki iki takım arasında taktik sonucu değiştirebilir.',
  source: 'Formation · PlayStyle · Manager · TacticsService · Referee · RefereeService',
  sections: [
    { id: 'dizilis', no: '6.1', title: 'Dizilişler' },
    { id: 'stil', no: '6.2', title: 'Oyun stili' },
    { id: 'hoca', no: '6.3', title: 'Teknik direktör' },
    { id: 'hakem', no: '6.4', title: 'Hakemler' },
  ],
  pages: [
    { to: '/teams', label: 'Takım sayfası → Taktik' },
    { to: '/referees', label: 'Hakemler' },
  ],
}
