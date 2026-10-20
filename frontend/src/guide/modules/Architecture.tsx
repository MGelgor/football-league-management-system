import { Box, Edge, Figure, GTable, Note, Prose, Section, Txt } from '../parts'

const ALGORITHMS = [
  'RoundRobinScheduler',
  'ScoreSimulator',
  'MatchDetailGenerator',
  'StandingsCalculator',
  'SquadGenerator',
  'PlayerDevelopment',
  'RandomTeamGenerator',
  'Economy',
  'MatchCommentary',
]

const LIFELINES = [
  { x: 64, title: 'Sayfa', sub: 'FixturePage' },
  { x: 192, title: 'client.ts', sub: 'fetch, EventSource' },
  { x: 320, title: 'Controller', sub: 'MatchWeek…' },
  { x: 448, title: 'Simülasyon', sub: 'MatchSimulation…' },
  { x: 576, title: 'Taktik + motor', sub: 'Tactics, Score…' },
  { x: 704, title: 'Ekonomi, kariyer', sub: 'Economy, Career' },
  { x: 832, title: 'Repository', sub: 'veritabanı' },
  { x: 960, title: 'Canlı yayın', sub: 'LiveBroadcast…' },
]

const ROUTES = [
  ['/my-team', 'MyTeamPage'],
  ['/teams/:id', 'TeamDetailPage'],
  ['/players/:id', 'PlayerPage'],
  ['/fixture', 'FixturePage'],
  ['/matches/:id', 'MatchDetailPage'],
  ['/standings', 'StandingsPage'],
  ['/cup', 'CupPage'],
  ['/seasons', 'SeasonsPage'],
  ['/transfers', 'TransfersPage'],
  ['/data', 'DataPage'],
  ['/guide/:id', 'GuidePage'],
]

function Architecture() {
  return (
    <>
      <Section id="katman" no="1.1" title="Backend katmanları">
        <Prose>
          <p>Backend katmanlı bir yapı kullanır. Her katman yalnız bir alttaki katmanı çağırır. Controller iş kuralı içermez. Controller isteği servise iletir.</p>
          <p>Hesaplamalar ayrı sınıflardadır. Bu sınıflar veritabanını bilmez. Servis veriyi okur, hesaplayıcıya verir ve sonucu kaydeder. Bu nedenle testler hesaplayıcıları Spring olmadan çalıştırır.</p>
        </Prose>
        <Figure
          no="1.1"
          height={500}
          minWidth={820}
          label="Backend katmanları: frontend, controller, service, repository, veritabanı. Yanlarda dto, hata işleyici, entity, config ve saf hesaplayıcılar."
          caption="Backend katmanları. Orta sütun bir isteğin yukarıdan aşağı yolu. Yeşil kutudaki sınıflar veritabanına erişmez. Kırmızı kesik çizgi hata yoludur."
        >
          <Box x={340} y={20} w={280} h={56} tone="acc" center title="Frontend (React)" lines={['api/client.ts → fetch, EventSource']} />
          <Box x={340} y={120} w={280} h={60} center title="controller" lines={['13 sınıf · REST ve SSE uç noktaları']} />
          <Box x={340} y={220} w={280} h={60} center title="service" lines={['37 sınıf · iş kuralları · @Transactional']} />
          <Box x={340} y={320} w={280} h={60} center title="repository" lines={['17 Spring Data JPA arayüzü']} />
          <Box x={340} y={420} w={280} h={56} tone="sig" center title="Veritabanı" lines={['H2 veya PostgreSQL']} />
          <Edge from={[480, 76]} to={[480, 117]} />
          <Txt x={490} y={101} cls="g-s">HTTP + JSON</Txt>
          <Edge from={[480, 180]} to={[480, 217]} />
          <Txt x={490} y={203} cls="g-s">metot çağrısı</Txt>
          <Edge from={[480, 280]} to={[480, 317]} />
          <Txt x={490} y={303} cls="g-s">metot çağrısı</Txt>
          <Edge from={[480, 380]} to={[480, 417]} />
          <Txt x={490} y={403} cls="g-s">SQL (Hibernate)</Txt>

          <Box x={40} y={120} w={230} h={60} center title="dto" lines={['Java record · istek ve yanıt']} />
          <Edge from={[273, 150]} to={[337, 150]} both />
          <Txt x={305} y={142} cls="g-s" anchor="middle">girdi/çıktı</Txt>
          <Box x={40} y={220} w={230} h={60} tone="dan" center title="GlobalExceptionHandler" lines={['exception → HTTP durum kodu']} />
          <Edge from={[340, 250]} to={[273, 250]} tone="dan" dash />
          <Txt x={306} y={242} cls="g-s" anchor="middle">fırlatır</Txt>
          <Edge d="M40,250 H20 V48 H337" tone="dan" dash />
          <Txt x={180} y={40} cls="t-dan" anchor="middle">{'hata yanıtı: { "message": … }'}</Txt>
          <Box x={40} y={320} w={230} h={60} center title="entity" lines={['Team, Player, Match … 27 dosya']} />
          <Edge from={[273, 350]} to={[337, 350]} both dash />
          <Txt x={305} y={342} cls="g-s" anchor="middle">eşler</Txt>

          <Box x={690} y={100} w={250} h={80} center title="config" lines={['StartupDataInitializer: 4 büyükler,', 'hakem havuzu, hocalar, bütçeler', 'WebConfig: /uploads']} />
          <Edge from={[716, 180]} to={[604, 217]} />
          <Txt x={700} y={206} cls="g-s">açılışta çağırır</Txt>

          <rect className="g-box acc" x="690" y="232" width="250" height="196" rx="4" />
          <Txt x={815} y={254} cls="g-t" anchor="middle">Saf hesaplayıcılar</Txt>
          <Txt x={815} y={271} cls="g-s" anchor="middle">Spring ve JPA bağımlılığı yok</Txt>
          {ALGORITHMS.map((name, i) => (
            <Txt key={name} x={708} y={294 + i * 14} cls="g-m">
              {name}
            </Txt>
          ))}
          <Edge from={[620, 246]} to={[687, 246]} tone="acc" />
          <Txt x={654} y={240} cls="g-s" anchor="middle">veri</Txt>
          <Edge from={[689, 272]} to={[623, 272]} tone="acc" />
          <Txt x={654} y={290} cls="g-s" anchor="middle">sonuç</Txt>
        </Figure>
        <GTable
          head={['PAKET', 'GÖREV', 'ÖRNEK']}
          rows={[
            [<code key="k101_15">controller</code>, 'HTTP isteğini alır. Servisi çağırır. DTO döndürür.', <code key="k101_103">MyTeamController</code>],
            [<code key="k102_15">service</code>, 'İş kurallarını uygular. Transaction sınırını belirler.', <code key="k102_103">MatchSimulationService</code>],
            [<code key="k103_15">repository</code>, 'Tabloları okur ve yazar.', <code key="k103_69">MatchRepository</code>],
            [<code key="k104_15">entity</code>, 'Bir tablonun satırını veya bir sabit listeyi (enum) temsil eder.', <code key="k104_107">Player</code>, ],
            [<code key="k105_15">dto</code>, "API'ye giren ve çıkan veri. Entity dışarı çıkmaz.", <code key="k105_93">LiveSessionResponse</code>],
            [<code key="k106_15">exception</code>, 'Özel hatalar ve hata işleyici.', <code key="k106_75">ManagedMatchPendingException</code>],
            [<code key="k107_15">config</code>, 'Açılış verisi ve logo dosyası sunumu.', <code key="k107_82">StartupDataInitializer</code>],
          ]}
        />
      </Section>

      <Section id="istek" no="1.2" title={'Bir isteğin yolu: "Haftayı Oynat"'}>
        <Prose>
          <p>Kullanıcı Fikstür sayfasında "Haftayı Oynat" düğmesine basar. Şekil 1.2 bu tıklamadan sonraki adımları gösterir. Adım 1–12 tek bir transaction içinde olur. Bir adım hata verirse hiçbir değişiklik kaydedilmez.</p>
          <p>Maçlar adım 6'da bir anda oynanır. Canlı yayın (adım 14–16) kayıtlı sonucu dakika dakika tekrar gösterir. Yayın sonucu değiştirmez.</p>
        </Prose>
        <Figure
          no="1.2"
          width={1040}
          height={640}
          minWidth={900}
          label="Sıra diyagramı: sayfa, API istemcisi, controller, simülasyon servisi, taktik ve maç motoru, ekonomi ve kariyer, repository ve canlı yayın servisi arasında hafta oynatma ve canlı yayın."
          caption="Haftayı oynatma ve canlı yayın. Düz ok çağrı, kesik ok yanıttır. Yeşil çubuk transaction süresidir. Adım 6, Modül 04'te ayrıntılıdır. Adım 14–16, Modül 09'da ayrıntılıdır."
        >
          {LIFELINES.map((life, i) => (
            <g key={life.title}>
              <Box x={life.x - 60} y={12} w={120} h={48} tone={i < 2 ? 'acc' : i === 6 ? 'sig' : i === 7 ? 'away' : 'plain'} center title={life.title} lines={[life.sub]} />
              <line className="g-life" x1={life.x} y1={60} x2={life.x} y2={628} />
            </g>
          ))}
          <rect className="f-accs s-acc" x="443" y="160" width="10" height="356" strokeWidth="1" />
          <Txt x={458} y={177} cls="g-s">@Transactional</Txt>

          <Edge from={[64, 90]} to={[189, 90]} />
          <Txt x={128} y={83} anchor="middle">1 · playWeek(5)</Txt>
          <Edge from={[192, 122]} to={[317, 122]} />
          <Txt x={256} y={115} anchor="middle">2 · POST /weeks/5/play</Txt>
          <Edge from={[320, 164]} to={[440, 164]} />
          <Txt x={384} y={157} anchor="middle">3 · playWeek(5)</Txt>
          <Edge from={[453, 196]} to={[829, 196]} />
          <Txt x={640} y={189} anchor="middle">4 · sezon, hafta, maçlar, kadrolar okunur</Txt>
          <rect className="g-box" x="460" y="208" width="232" height="44" rx="3" />
          <Txt x={472} y={226}>5 · kural: hafta sıradaki mi?</Txt>
          <Txt x={472} y={243} cls="t-dan">menajerin kadrosu var mı? yoksa 409</Txt>
          <Edge from={[453, 272]} to={[573, 272]} />
          <Txt x={514} y={265} anchor="middle">6 · her maç</Txt>
          <rect className="g-box acc" x="584" y="282" width="232" height="44" rx="3" />
          <Txt x={596} y={300}>taktik kurulum → λ → skor</Txt>
          <Txt x={596} y={317} cls="g-s">→ dakika dakika olaylar, reyting</Txt>
          <Edge from={[576, 346]} to={[456, 346]} dash />
          <Txt x={516} y={339} anchor="middle">7 · sonuç</Txt>
          <Edge from={[453, 378]} to={[701, 378]} />
          <Txt x={577} y={371} anchor="middle">8 · bilet, maaş, güven</Txt>
          <Edge from={[453, 410]} to={[829, 410]} />
          <Txt x={640} y={403} anchor="middle">9 · saveAll: maç, olay, kadro, istatistik</Txt>
          <rect className="g-box sig" x="460" y="420" width="232" height="44" rx="3" />
          <Txt x={472} y={438}>10 · son hafta oynandı mı?</Txt>
          <Txt x={472} y={455} cls="g-s">evet → SeasonEndService (Modül 03)</Txt>
          <Edge from={[443, 488]} to={[323, 488]} dash />
          <Txt x={383} y={481} anchor="middle">11 · sonuçlar</Txt>
          <Edge from={[320, 512]} to={[195, 512]} dash />
          <Txt x={256} y={505} anchor="middle">12 · 200 OK + JSON</Txt>
          <Edge from={[192, 544]} to={[957, 544]} tone="away" />
          <Txt x={576} y={537} anchor="middle">14 · GET /weeks/5/live?speed=3 (EventSource)</Txt>
          <Txt x={128} y={540} cls="g-s" anchor="middle">13 · yayını aç</Txt>
          <Edge from={[960, 578]} to={[195, 578]} tone="away" dash />
          <Txt x={576} y={571} anchor="middle">15 · start → minute (0–90) → end olayları</Txt>
          <Edge from={[192, 610]} to={[67, 610]} dash />
          <Txt x={128} y={603} anchor="middle">16 · ekran</Txt>
        </Figure>
      </Section>

      <Section id="hata" no="1.3" title="Hata yolu">
        <Prose>
          <p>Servis bir kural bozulduğunda özel bir exception fırlatır. <code>GlobalExceptionHandler</code> bu exception'ı yakalar. İşleyici exception'ı bir HTTP durum koduna ve <code>{'{"message": …}'}</code> gövdesine çevirir. Frontend bu mesajı <code>ErrorAlert</code> bileşeniyle gösterir.</p>
        </Prose>
        <Figure
          no="1.3"
          height={260}
          minWidth={820}
          label="Hata yolu: servis exception fırlatır, hata işleyici 409 yanıtı üretir, client.ts ApiError oluşturur, sayfa mesajı gösterir. Backend kapalıysa ApiError durum kodu 0 olur."
          caption="Hata yolu. Üst sıra bir iş kuralı hatası. Alt sıra bağlantı hatası. İki yol da aynı mesaj kutusuna gider."
        >
          <Box x={20} y={40} w={168} h={96} tag="SERVİS" title="kural bozulur" lines={[{ text: 'throw new', mono: true }, { text: 'WeekOrderException', mono: true }]} />
          <Box x={208} y={40} w={168} h={96} tone="dan" tag="HATA İŞLEYİCİ" title="Global…Handler" lines={[{ text: '@ExceptionHandler', mono: true }, { text: '→ CONFLICT', mono: true }]} />
          <Box x={396} y={40} w={168} h={96} tag="HTTP YANITI" title="409 Conflict" lines={[{ text: '{"message":', mono: true }, { text: ' "…sıradaki hafta 5"}', mono: true }]} />
          <Box x={584} y={40} w={168} h={96} tone="acc" tag="CLIENT.TS" title="response.ok = false" lines={[{ text: 'throw new', mono: true }, { text: 'ApiError(409, msg)', mono: true }]} />
          <Box x={772} y={40} w={168} h={96} tone="acc" tag="SAYFA" title="ErrorAlert" lines={['mesajı gösterir', 'gerekirse eylem düğmesi']} />
          {[188, 376, 564, 752].map((x) => (
            <Edge key={x} from={[x, 88]} to={[x + 17, 88]} tone="dan" />
          ))}
          <Box x={396} y={176} w={168} h={62} tone="ghost" title="Backend kapalı" lines={['fetch() bağlanamaz']} />
          <Box x={584} y={176} w={168} h={62} tone="ghost" lines={[{ text: 'ApiError(0, …)', mono: true }, '"Sunucuya ulaşılamadı"']} />
          <Edge from={[564, 207]} to={[581, 207]} tone="dan" dash />
          <Edge d="M752,207 H856 V139" tone="dan" dash />
          <Txt x={20} y={180} cls="g-s">Alt sıra: backend hiç yanıt</Txt>
          <Txt x={20} y={196} cls="g-s">vermezse sayfa aynı yolla</Txt>
          <Txt x={20} y={212} cls="g-s">bir mesaj gösterir.</Txt>
        </Figure>
        <GTable
          head={['KOD', 'NE ZAMAN', 'ÖRNEK EXCEPTION']}
          rows={[
            [<span key="k203_15" className="g-pill warn">400</span>, 'Girdi yanlış, fikstür yok, bütçe yetmez, kadro dizilişe uymaz.', <><code>IllegalArgumentException</code>, alan doğrulama hatası</>],
            [<span key="k204_15" className="g-pill warn">404</span>, 'Kayıt bulunamadı.', <><code>TeamNotFound…</code>, <code>SaveSlotNotFound…</code></>],
            [
              <span key="k206_16" className="g-pill no">409</span>,
              'İstek mevcut durumla çelişir.',
              <>
                <code>WeekOrder…</code>, <code>ManagedMatchPending…</code>, <code>TransferWindowClosed…</code>, <code>CupInProgress…</code>, <code>TeamsLocked…</code>
              </>,
            ],
            [<span key="k212_15" className="g-pill warn">413</span>, 'Yüklenen dosya sınırı aşar.', <code key="k212_93">MaxUploadSizeExceededException</code>],
          ]}
        />
        <Note>
          <p>Menajer modunda kadro seçilmeden hafta oynatılırsa yanıt <code>409</code> olur. Hata kutusu iki düğme gösterir: "Takımım →" ve "Kadroyu yapay zekâ seçsin". İkinci düğme isteği <code>?auto=true</code> ile tekrarlar.</p>
        </Note>
      </Section>

      <Section id="frontend" no="1.4" title="Frontend yapısı">
        <Prose>
          <p><code>App.tsx</code> her adres için bir sayfa seçer. Sayfalar <code>fetch</code> fonksiyonunu doğrudan çağırmaz. Sayfalar <code>api/client.ts</code> içindeki fonksiyonları çağırır. Bu kural tüm HTTP kodunu tek bir dosyada tutar.</p>
        </Prose>
        <Figure
          no="1.4"
          height={420}
          minWidth={820}
          label="Frontend yapısı: App.tsx rotaları sayfalara bağlar. Sayfalar bileşenleri kullanır ve client.ts fonksiyonlarını çağırır."
          caption="Frontend dosya yapısı (örnek rotalar). Her rota bir sayfa gösterir. Sayfalar backend'e yalnız client.ts üzerinden ulaşır. Rehber de bir sayfadır: GuidePage."
        >
          <rect className="g-box" x="20" y="16" width="210" height="392" rx="4" />
          <Txt x={34} y={38} cls="g-t">App.tsx</Txt>
          <Txt x={34} y={54} cls="g-s">main.tsx: BrowserRouter</Txt>
          <Txt x={300} y={48} cls="g-s">pages/</Txt>
          <Txt x={263} y={48} cls="g-s" anchor="middle">rota</Txt>
          {ROUTES.map(([route, page], i) => {
            const y = 70 + i * 32
            return (
              <g key={route}>
                <Txt x={34} y={y + 4} cls="g-m">
                  {route}
                </Txt>
                <Edge from={[230, y]} to={[297, y]} thin />
                <rect className={page === 'GuidePage' ? 'g-box sig' : 'g-box acc'} x="300" y={y - 13} width="190" height="26" rx="3" />
                <Txt x={312} y={y + 4} cls="g-m">
                  {page}
                </Txt>
              </g>
            )
          })}
          <Box
            x={560}
            y={30}
            w={180}
            h={196}
            title="components/ · 20"
            lines={['LineupEditor', 'LiveMatchView', 'LivePitch', 'LiveBroadcast', 'OpponentScout', 'PitchFormation', 'FinancePanel', 'InboxPanel', 'ErrorAlert …'].map((text) => ({ text, mono: true }))}
          />
          <Edge from={[490, 128]} to={[557, 128]} />
          <Txt x={524} y={120} cls="g-s" anchor="middle">kullanır</Txt>
          <Box x={560} y={250} w={180} h={56} tone="sig" title="api/client.ts" lines={['tüm endpoint fonksiyonları']} />
          <Edge from={[490, 278]} to={[557, 278]} />
          <Txt x={524} y={270} cls="g-s" anchor="middle">çağırır</Txt>
          <Box x={780} y={250} w={160} h={56} title="api/types.ts" lines={["DTO'ların TS tipleri"]} />
          <Edge from={[740, 278]} to={[777, 278]} dash />
          <Box x={780} y={30} w={160} h={56} title="index.css" lines={['açık ve koyu tema']} />
          <Box x={780} y={100} w={160} h={56} title="labels.ts" lines={['mevki, tür etiketleri']} />
          <Box x={780} y={170} w={160} h={56} tone="sig" title="guide/" lines={['rehber modülleri']} />
          <Box x={560} y={330} w={380} h={40} tone="ghost" center lines={[{ text: "fetch('/api/…') ve EventSource → Vite proxy → :8080", mono: true }]} />
          <Edge from={[650, 306]} to={[650, 327]} />
        </Figure>
      </Section>

      <Section id="profil" no="1.5" title="Çalışma profilleri">
        <Prose>
          <p>Backend üç profille çalışır. Profil yalnız veritabanını değiştirir. Kod ve API aynı kalır. Kayıt noktaları her profilde <code>saves/</code> klasöründe kalır.</p>
        </Prose>
        <Figure
          no="1.5"
          height={250}
          minWidth={820}
          label="Varsayılan profil PostgreSQL kullanır. h2 profili bellek içi H2 kullanır ve veriyi siler. h2file profili H2'yi dosyada tutar."
          caption="Üç profilin farkı. Fark ok hedefindeki veritabanıdır. h2 ve h2file Docker olmadan çalışır."
        >
          {[
            { x: 20, title: 'Varsayılan', file: 'application.yml', db: 'PostgreSQL 16', dbSub: 'localhost:5432', tone: 'sig' as const, ddl: 'ddl-auto: update', keep: 'veri: kalıcı', cmd: 'docker compose up -d' },
            { x: 330, title: 'h2', file: 'application-h2.yml', db: 'H2 (bellek)', dbSub: 'MODE=PostgreSQL', tone: 'ghost' as const, ddl: 'ddl-auto: create-drop', keep: 'veri: kapanınca silinir', cmd: '-D…profiles=h2' },
            { x: 640, title: 'h2file', file: 'application-h2file.yml', db: 'H2 (dosya)', dbSub: './data/league', tone: 'acc' as const, ddl: 'ddl-auto: update', keep: 'veri: kalıcı', cmd: '-D…profiles=h2file' },
          ].map((p) => (
            <g key={p.title}>
              <rect className="g-box ghost" x={p.x} y={16} width={300} height={222} rx={5} />
              <Txt x={p.x + 16} y={42} cls="g-tl">{p.title}</Txt>
              <Txt x={p.x + 16} y={60} cls="g-m">{p.file}</Txt>
              <Box x={p.x + 16} y={78} w={100} h={44} center title="Spring Boot" />
              <Box x={p.x + 170} y={78} w={114} h={44} tone={p.tone} center title={p.db} lines={[p.dbSub]} />
              <Edge from={[p.x + 116, 100]} to={[p.x + 167, 100]} />
              <Txt x={p.x + 16} y={156} cls="g-m">{p.ddl}</Txt>
              <Txt x={p.x + 16} y={176} cls="g-s">{p.keep}</Txt>
              <Txt x={p.x + 16} y={214} cls="g-m t-mut">{p.cmd}</Txt>
            </g>
          ))}
        </Figure>
        <Note kind="caution">
          <p>Şema değişen bir sürüme geçerken kalıcı veritabanı hata verebilir. Önce Veri sayfasından JSON yedek alın. Sonra <code>backend/data/</code> klasörünü silin ve yedeği yükleyin.</p>
        </Note>
      </Section>
    </>
  )
}

export default Architecture
