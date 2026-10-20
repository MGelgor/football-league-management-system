import { Box, Edge, Figure, GTable, Note, Prose, Section, Txt } from '../parts'

interface TableBox {
  name: string
  cols: string
  fk: string
  tone?: 'acc'
}

const CLUSTERS: { x: number; y: number; h: number; title: string; first: number; boxes: TableBox[] }[] = [
  {
    x: 20,
    y: 30,
    h: 300,
    title: 'LİG VE SEZON',
    first: 60,
    boxes: [
      { name: 'season', cols: 'season_number, finished', fk: '→ team (şampiyon, kupa)' },
      { name: 'match_week', cols: 'week_number, competition, cup_round', fk: '→ season' },
      { name: 'season_team_change', cols: 'type: düştü / çıktı', fk: '→ season, team' },
      { name: 'referee', cols: 'name, strictness 1–10', fk: 'havuz: 14 hakem' },
    ],
  },
  {
    x: 360,
    y: 30,
    h: 380,
    title: 'MAÇ',
    first: 60,
    boxes: [
      { name: 'match', cols: 'skor, 3 olasılık, penaltılar', fk: '→ match_week, team ×2, referee', tone: 'acc' },
      { name: 'match_event', cols: 'type, minute, penalty', fk: '→ match, team, player ×2' },
      { name: 'match_appearance', cols: 'starter, dakikalar, rating', fk: '→ match, team, player' },
      { name: 'match_team_stats', cols: 'şut, topla oynama, diziliş', fk: '→ match, team' },
      { name: 'match_lineup', cols: 'diziliş, stil, ilk 11, kaptan', fk: '→ match, team' },
    ],
  },
  {
    x: 720,
    y: 30,
    h: 380,
    title: 'KULÜP VE KADRO',
    first: 60,
    boxes: [
      { name: 'team', cols: 'strength, morale, budget', fk: '→ manager', tone: 'acc' },
      { name: 'player', cols: 'strength, age, form, wage', fk: '→ team, academy_team' },
      { name: 'manager', cols: 'tactical_skill, formation', fk: 'yapay zekâ hocası' },
      { name: 'transfer', cols: 'fee, season_number', fk: '→ player, team ×2' },
      { name: 'finance_entry', cols: 'type, amount, week', fk: '→ team, season' },
    ],
  },
  {
    x: 20,
    y: 350,
    h: 230,
    title: 'MENAJER MODU',
    first: 380,
    boxes: [
      { name: 'manager_profile', cols: 'name, confidence, hedefler', fk: '→ team (tek satır)' },
      { name: 'manager_spell', cols: 'sezon aralığı, ayrılış nedeni', fk: '→ team' },
      { name: 'inbox_message', cols: 'type, title, amount, resolved', fk: '→ player, team' },
    ],
  },
]

const COMPUTED = [
  { table: 'matches', what: 'skor, olasılık', calc: 'StandingsCalculator', out: 'Puan durumu', outSub: 'O G B M A Y AV P, form, bölge' },
  { table: 'match_events', what: 'gol, asist, kart, sakatlık', calc: 'PlayerTotals', out: 'Oyuncu istatistikleri', outSub: 'gol krallığı, asist, kart' },
  { table: 'match_appearances', what: 'dakika, reyting', calc: 'TeamStatsService', out: 'Takım istatistikleri', outSub: 'iç saha, deplasman, grafik' },
  { table: 'match_team_stats', what: 'şut, topla oynama, güç', calc: 'RecordService', out: 'Rekorlar', outSub: 'en gollü maç, seriler' },
  { table: 'finance_entries', what: 'gelir ve gider kayıtları', calc: 'EconomyService', out: 'Finans özeti', outSub: 'sezon geliri, gideri, net' },
]

const WRITES = [
  ['matches', '1 satır güncellenir', 'skor, 3 olasılık, hakem'],
  ['match_events', 'ortalama ~7 yeni satır', 'gol, kart, sakatlık, penaltı, VAR'],
  ['match_appearances', '24–28 yeni satır', '2 × 11 + oyuna girenler'],
  ['match_team_stats', '2 yeni satır', 'istatistik, diziliş, stil'],
  ['teams', '2 satır güncellenir', 'moral ve güç (yalnız lig), bütçe'],
  ['players', 'durum güncellenir', 'ceza, sakatlık, form, yorgunluk'],
  ['finance_entries', '3 yeni satır (lig)', 'ev sahibine bilet, iki takıma maaş'],
  ['manager_profile', 'menajerin maçında', 'güven ±2'],
  ['inbox_messages', 'menajerin takımında', 'sakatlık haberi'],
]

function DataModel() {
  return (
    <>
      <Section id="harita" no="2.1" title="Tablo haritası">
        <Prose>
          <p>Şemada 17 tablo var. Şekil 2.1 tabloları dört alana ayırır. Her kutu önemli sütunları ve yabancı anahtarları (→) gösterir. Çizgiler yalnız alanlar arasındaki ana bağlantılardır.</p>
        </Prose>
        <Figure
          no="2.1"
          width={1040}
          height={620}
          minWidth={900}
          label="Tablo haritası: lig ve sezon, maç, kulüp ve kadro, menajer modu alanları ve aralarındaki bağlantılar."
          caption="Dört alanda 17 tablo. Yeşil kutular merkez tablolardır: match ve team. Dikey çizgiler match_id ve team_id bağlantısıdır. Kesik çizgi sezon şampiyonudur."
        >
          {CLUSTERS.map((cluster) => (
            <g key={cluster.title}>
              <rect className="g-grp" x={cluster.x} y={cluster.y} width={cluster.x === 20 ? 300 : 320} height={cluster.h} rx={6} />
              <Txt x={cluster.x + 12} y={cluster.y + 18} cls="g-ms">
                {cluster.title}
              </Txt>
              {cluster.boxes.map((box, i) => {
                const width = cluster.x === 20 ? 260 : 280
                return (
                  <Box
                    key={box.name}
                    x={cluster.x + 20}
                    y={cluster.first + i * 66}
                    w={width}
                    h={58}
                    tone={box.tone}
                    lines={[
                      { text: box.name, mono: true, bold: true },
                      box.cols,
                      { text: box.fk, mono: true, tone: 'acc' },
                    ]}
                  />
                )
              })}
            </g>
          ))}
          <path className="g-ed thin" d="M372,89 V353 M372,155 H380 M372,221 H380 M372,287 H380 M372,353 H380" />
          <Txt x={366} y={250} cls="g-ms" anchor="end">
            match_id
          </Txt>
          <path className="g-ed thin" d="M732,89 V353 M732,155 H740 M732,221 H740 M732,287 H740 M732,353 H740" />
          <Txt x={726} y={250} cls="g-ms" anchor="end">
            team_id
          </Txt>
          <Edge from={[300, 155]} to={[377, 100]} />
          <Txt x={316} y={118} cls="g-s">1–N</Txt>
          <Edge from={[300, 287]} to={[377, 112]} />
          <Txt x={312} y={232} cls="g-s">hakem</Txt>
          <Edge from={[737, 82]} to={[663, 82]} />
          <Txt x={700} y={74} cls="g-s" anchor="middle">ev + dep</Txt>
          <Edge d="M840,30 V16 H170 V57" dash />
          <Txt x={505} y={12} cls="g-s" anchor="middle">şampiyon ve kupa şampiyonu</Txt>
          <Edge d="M320,465 H340 V600 H860 V413" />
          <Txt x={600} y={594} cls="g-s" anchor="middle">menajer tabloları → team</Txt>
        </Figure>
        <GTable
          head={['İLİŞKİ', 'TİP', 'AÇIKLAMA']}
          rows={[
            [<code key="k149_15">season → match_week</code>, '1–N', 'Lig haftaları 1–34, kupa turları 101–103.'],
            [<code key="k150_15">match_week → match</code>, '1–N', 'Lig haftasında 9 maç, kupa turunda 4, 2 veya 1 maç.'],
            [<code key="k151_15">referee → match</code>, '1–N', 'Bir hakem bir haftada yalnız bir maç yönetir.'],
            [<code key="k152_15">team → player</code>, '1–N', 'Kadro 18–30 oyuncu. Serbest oyuncunun takımı boştur.'],
            [<code key="k153_15">team → manager</code>, 'N–1', 'Her takımın bir yapay zekâ hocası var. Menajer devralınca hoca ayrılır.'],
            [<code key="k154_15">match → match_lineup</code>, '1–2', 'Yalnız menajerin seçtiği kadro kaydedilir.'],
            [<code key="k155_15">team → finance_entry</code>, '1–N', 'Her gelir ve gider bir satırdır. Bütçe bu satırların toplamıdır.'],
            [<code key="k156_15">manager_profile</code>, 'tek satır', 'Sistem tek kullanıcılıdır. Hesap ve şifre yoktur.'],
          ]}
        />
      </Section>

      <Section id="hesap" no="2.2" title="Saklanan ve hesaplanan veri">
        <Prose>
          <p>Puan durumu bir tabloda saklanmaz. Oyuncu istatistikleri de saklanmaz. Sistem bu verileri her istekte ham kayıtlardan hesaplar. Bu yöntem iki kaynağın birbirinden farklı olmasını önler.</p>
        </Prose>
        <Figure
          no="2.2"
          height={360}
          minWidth={820}
          label="Beş tablo beş hesaplayıcıya veri verir. Hesaplayıcılar puan durumu, oyuncu ve takım istatistikleri, rekorlar ve finans özeti üretir."
          caption="Saklanan ve hesaplanan veri. Sol sütun tek veri kaynağıdır. Sağ sütundaki kesik kutular hiçbir tabloda yoktur. Her hesaplayıcı birden çok tabloyu okuyabilir; şekil ana kaynağı gösterir."
        >
          <Txt x={30} y={24} cls="g-ms">TABLODA SAKLANIR</Txt>
          <Txt x={370} y={24} cls="g-ms">HESAPLAR</Txt>
          <Txt x={710} y={24} cls="g-ms">HER İSTEKTE ÜRETİLİR</Txt>
          {COMPUTED.map((row, i) => {
            const y = 40 + i * 62
            return (
              <g key={row.table}>
                <Box x={30} y={y} w={220} h={48} tone="sig" lines={[{ text: row.table, mono: true, bold: true }, row.what]} />
                <Box x={370} y={y} w={220} h={48} lines={[{ text: row.calc, mono: true, bold: true }]} />
                <Box x={710} y={y} w={230} h={48} tone="ghost" title={row.out} lines={[row.outSub]} />
                <Edge from={[250, y + 24]} to={[367, y + 24]} thin />
                <Edge from={[590, y + 24]} to={[707, y + 24]} dash />
              </g>
            )
          })}
        </Figure>
        <Note>
          <p>Takım bütçesi bir istisnadır. Bütçe <code>teams.budget</code> sütununda saklanır. Her finans kaydı bütçeyi aynı transaction içinde değiştirir. Testler bütçenin "başlangıç + gelir − gider" eşitliğini kontrol eder.</p>
        </Note>
      </Section>

      <Section id="yazim" no="2.3" title="Bir maç neleri yazar">
        <Prose>
          <p>Bir lig maçı oynanınca sistem en fazla 9 tabloya yazar. Şekil 2.3 her tabloya yazılan satırları gösterir. Sistem tüm satırları hafta sonunda toplu olarak kaydeder (<code>saveAll</code>).</p>
        </Prose>
        <Figure
          no="2.3"
          height={420}
          minWidth={820}
          label="Bir lig maçı dokuz tabloya yazar: matches, match_events, match_appearances, match_team_stats, teams, players, finance_entries, manager_profile, inbox_messages."
          caption="Bir lig maçının yazdığı kayıtlar. Son iki satır yalnız menajerin takımında yazılır. Kupa maçı moral ve gücü değiştirmez, maaş da ödenmez."
        >
          <Box x={20} y={170} w={190} h={80} tone="acc" center title="1 lig maçı oynanır" lines={['MatchSimulationService']} />
          {WRITES.map(([table, count, note], i) => {
            const y = 14 + i * 44
            return (
              <g key={table}>
                <Edge from={[210, 210]} to={[327, y + 17]} tone="acc" thin />
                <rect className={i >= 7 ? 'g-box ghost' : 'g-box'} x="330" y={y} width="610" height="34" rx="3" />
                <Txt x={344} y={y + 22} cls="g-m g-b">{table}</Txt>
                <Txt x={540} y={y + 22} cls="g-b">{count}</Txt>
                <Txt x={720} y={y + 22} cls="g-s">{note}</Txt>
              </g>
            )
          })}
        </Figure>
      </Section>

      <Section id="kararlar" no="2.4" title="Tasarım kararları">
        <GTable
          head={['KARAR', 'NEDEN']}
          rows={[
            ['Puan durumu tablosu yok.', 'Puan durumu maç skorlarından hesaplanır. Skor ve puan birbirinden farklı olamaz.'],
            ['Oyuncu istatistik tablosu yok.', 'Gol ve kart match_events tablosundan, dakika ve reyting match_appearances tablosundan sayılır.'],
            ['Kupa maçları için ayrı tablo yok.', 'Kupa maçları matches tablosundadır. Haftanın competition = CUP değeri kupayı ayırır.'],
            ['Silinen takım ve emekli oyuncu arşivlenir.', 'Kayıt active = false olur. Eski sezonların kayıtları bozulmaz.'],
            ['İstatistikler takımı oyuncudan okumaz.', 'Oyuncu takım değiştirebilir veya serbest kalabilir. Sorgular takımı maç kaydından okur.'],
            ['Form son 5 reytingi metin olarak tutar.', 'Ayrı bir tablo gerekmez. Player.recentRatings "6.8,7.1,…" biçimindedir.'],
            ['Yedekler tabloları kodda listelemez.', 'BackupService tabloları veritabanından bulur. Yeni bir tablo kod değişmeden yedeğe girer.'],
          ]}
        />
      </Section>
    </>
  )
}

export default DataModel
