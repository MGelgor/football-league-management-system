import { Box, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

const ROOM_SCHEMAS = [
  { x: 360, name: 'room_allzh7', sub: 'Ayşe’nin odası', tone: 'acc' as const },
  { x: 555, name: 'room_vk7ugc', sub: 'Bora’nın odası', tone: 'acc' as const },
  { x: 750, name: 'room_…', sub: 'en fazla 30 oda', tone: 'ghost' as const },
]

const FILTER_STEPS: { q: string; sub: string; out?: string; outSub?: string; code?: string; tone?: 'dan' | 'sig' | 'acc' }[] = [
  { q: 'İstekte oda kodu var mı?', sub: 'X-Room-Code başlığı ya da ?room=', out: 'Yok → yayında 401', outSub: '"Önce bir odaya katılın" · lokalde tek kişilik lig', code: '401' },
  { q: 'Token bu odanın üyesine mi ait?', sub: 'SHA-256 özeti room_members’ta aranır', out: 'Hayır → 403', outSub: '"Bu odaya erişiminiz yok"', code: '403' },
  { q: 'Maç oynatan bir uç mu?', sub: 'hafta oynat, fikstür, kupa, sezon, içe aktar', out: 'Evet → 403', outSub: '"Odada maçları zamanlayıcı oynatır"', code: '403' },
  { q: 'Yazma isteği ve sahip değil mi?', sub: 'üye yolları hariç: my-team, teklif, kendi taktiği', out: 'Evet → 403', outSub: '"Yalnızca oda sahibi yapabilir"', code: '403' },
  { q: 'Zamanı gelmiş olay var mı?', sub: 'RoomScheduler.tick(oda)', out: 'Varsa önce oynanır', outSub: 'üyelik yeniden okunur (kilit saati değişir)', tone: 'sig' },
  { q: 'Kadro / taktik değişikliği, maça < 5 dk?', sub: 'lineupLockAt = maç saati − 5 dk', out: 'Evet → 409', outSub: '"Kadro ve taktik maça 5 dakika kala kilitlenir"', code: '409' },
  { q: 'Canlı yayın sürerken sonuç gösteren okuma?', sub: 'izinliler: yayın, takımlar, kadro, pencere, hakemler', out: 'Evet → 409', outSub: '"Sonuçlar yayın bitince açılır"', code: '409' },
]

const TEAMS = Array.from({ length: 18 }, (_, i) => i)

function Rooms() {
  return (
    <>
      <Section id="sema" no="11.1" title="Oda başına bir şema">
        <Prose>
          <p>PostgreSQL bir veritabanının içinde birden çok <b>şema</b> tutar. Şema bir klasöre benzer: içinde tablolar vardır. İki şemada aynı adlı tablo olabilir. <code>room_allzh7.teams</code> ve <code>room_vk7ugc.teams</code> iki ayrı tablodur.</p>
          <p>Sistem her oda için bir şema açar. Şemanın içinde tek kişilik ligin bütün tabloları vardır. Oda kayıtları (<code>rooms</code>, <code>room_members</code>) ise varsayılan şemada (<code>public</code>) durur. Böylece sistem bir odanın hangi şemada olduğunu her zaman bulur.</p>
          <p>Bu yöntemin büyük avantajı şudur: 40'tan fazla servis hiç değişmedi. Servisler "hangi odadayım?" sorusunu bilmez. Bağlantı doğru şemaya ayarlanır, sorgu odanın tablosuna gider.</p>
        </Prose>
        <Figure
          no="11.1"
          height={330}
          minWidth={820}
          label="Bir PostgreSQL veritabanı. Varsayılan şemada rooms ve room_members tabloları. Her oda için room_ önekli ayrı bir şema ve içinde ligin tüm tabloları."
          caption="Bir veritabanı, çok şema. Sarı kutu varsayılan şemadır. Yeşil kutular odaların şemalarıdır. rooms tablosundaki schema_name sütunu odanın şemasını gösterir. Tablo adları her şemada aynıdır."
        >
          <rect className="g-box ghost" x="20" y="16" width="920" height="300" rx="6" />
          <Txt x={36} y={40} cls="g-tl">PostgreSQL veritabanı · neondb</Txt>
          <Txt x={924} y={40} cls="g-s" anchor="end">lokalde: H2 veya Docker Postgres · yayında: Neon</Txt>

          <Box
            x={40}
            y={60}
            w={270}
            h={170}
            tone="sig"
            tag="VARSAYILAN ŞEMA"
            title="public"
            lines={[
              { text: 'rooms', mono: true, bold: true },
              { text: '  code, schema_name, status,', mono: true },
              { text: '  match_time, next_event_at', mono: true },
              { text: 'room_members', mono: true, bold: true },
              { text: '  nickname, token_hash, owner', mono: true },
              'tek kişilik ligin tabloları',
            ]}
          />
          {ROOM_SCHEMAS.map((s) => (
            <Box
              key={s.name}
              x={s.x}
              y={60}
              w={175}
              h={170}
              tone={s.tone}
              tag={s.sub.toLocaleUpperCase('tr')}
              title={s.name}
              lines={[
                { text: 'teams, players', mono: true },
                { text: 'matches, match_weeks', mono: true },
                { text: 'seasons, transfers', mono: true },
                { text: 'transfer_bids', mono: true },
                { text: 'manager_profiles …', mono: true },
                '~30 tablo',
              ]}
            />
          ))}
          <path className="g-ed dash" d="M310,262 H838" />
          <Edge d="M175,230 V262 H310" dash head={false} />
          {ROOM_SCHEMAS.map((s) => (
            <Edge key={s.name} from={[s.x + 88, 262]} to={[s.x + 88, 233]} dash />
          ))}
          <Txt x={330} y={284} cls="g-s">rooms.schema_name → şema adı. Kod ALLZH7 ise şema room_allzh7.</Txt>
          <Txt x={330} y={302} cls="g-s">Oda silinince: drop schema room_allzh7 cascade (tüm tabloları gider).</Txt>
        </Figure>
        <Note>
          <p>Neden her oda için ayrı veritabanı değil? Her veritabanı ayrı bir bağlantı havuzu ister; 30 havuz 512 MB'lık sunucuya sığmaz. Neden tablolara <code>room_id</code> sütunu eklenmedi? O zaman 30 tablonun her sorgusuna <code>where room_id = …</code> eklemek gerekirdi. Bir sorguda unutulan koşul başka odanın verisini gösterirdi. Şema yönteminde bu hata yapılamaz.</p>
        </Note>
      </Section>

      <Section id="istek" no="11.2" title="Odanın içindeki bir istek">
        <Prose>
          <p>Tarayıcı her isteğe aktif odanın kodunu ve token'ını ekler. <code>RoomFilter</code> bu başlıkları okur ve üyeliği kontrol eder. Sonra odanın bilgisini <code>RoomContext</code>'e yazar. <code>RoomContext</code> bir <code>ThreadLocal</code>'dır: her istek kendi thread'inde çalışır, bu yüzden aynı anda gelen iki odanın isteği birbirine karışmaz.</p>
          <p>Asıl iş en altta olur. Hibernate bir bağlantı istediğinde <code>RoomAwareDataSource</code> bağlantıyı verir ve önce <code>connection.setSchema(...)</code> çağırır. Sorgu şemasız yazılır (<code>select * from teams</code>), ama veritabanı onu odanın şemasında çalıştırır.</p>
        </Prose>
        <Figure
          no="11.2"
          height={310}
          minWidth={860}
          label="Tarayıcı başlıklarla istek gönderir, RoomFilter üyeliği kontrol eder, RoomContext odayı tutar, servis değişmeden çalışır, RoomAwareDataSource bağlantının şemasını ayarlar, sorgu odanın tablosuna gider."
          caption="Odanın içindeki bir istek. Üst sıra soldan sağa, alt sıra sağdan sola okunur. Yeşil kesik ok önemli bağlantıdır: veri katmanı odayı RoomContext'ten okur. Servisler odayı bilmez."
        >
          <Box x={20} y={30} w={205} h={100} tone="acc" tag="TARAYICI" title="GET /api/standings" lines={[{ text: 'X-Room-Code: ALLZH7', mono: true }, { text: 'X-Room-Token: 9f…c2', mono: true }, 'api/rooms.ts ekler']} />
          <Box x={260} y={30} w={205} h={100} tone="sig" tag="FİLTRE" title="RoomFilter" lines={['token → SHA-256 özeti', 'room_members’ta ara', 'kurallar (11.3)']} />
          <Box x={500} y={30} w={205} h={100} tone="acc" tag="THREADLOCAL" title="RoomContext" lines={[{ text: 'schema: room_allzh7', mono: true }, { text: 'memberId: 12', mono: true }, { text: 'owner: false', mono: true }]} />
          <Box x={740} y={30} w={200} h={100} tag="DEĞİŞMEDİ" title="Controller → Service" lines={['StandingsService', 'oda bilmez', 'repository çağırır']} />
          <Edge from={[225, 80]} to={[257, 80]} />
          <Edge from={[465, 80]} to={[497, 80]} />
          <Edge from={[705, 80]} to={[737, 80]} />

          <Box x={740} y={190} w={200} h={90} title="Hibernate" lines={['bağlantı ister', { text: 'select … from teams', mono: true }]} />
          <Box x={500} y={190} w={205} h={90} tone="acc" tag="BAĞLANTI SARMALAYICI" title="RoomAwareDataSource" lines={[{ text: 'setSchema("room_allzh7")', mono: true }]} />
          <Box x={260} y={190} w={205} h={90} tone="sig" tag="VERİTABANI" title="room_allzh7.teams" lines={['odanın tablosu okunur']} />
          <Box x={20} y={190} w={205} h={90} tone="ghost" tag="İSTEK BİTER" title="RoomContext.clear()" lines={['finally bloğunda', 'thread temiz kalır']} />
          <Edge d="M840,130 V187" />
          <Edge from={[740, 235]} to={[708, 235]} />
          <Edge from={[500, 235]} to={[468, 235]} />
          <Edge from={[260, 235]} to={[228, 235]} dash />
          <Edge from={[602, 130]} to={[602, 187]} tone="acc" dash />
          <Txt x={612} y={164} cls="t-acc">okur</Txt>
        </Figure>
        <Note>
          <p>EventSource (canlı yayın) ve indirme linkleri başlık gönderemez. Bu istekler kodu ve token'ı adreste taşır: <code>?room=ALLZH7&amp;token=…</code>. Frontend'de <code>withRoom(url)</code> bunu ekler.</p>
        </Note>
      </Section>

      <Section id="filtre" no="11.3" title="Filtrenin kararları">
        <Prose>
          <p><code>RoomFilter</code> her <code>/api</code> isteğinde aynı soruları sırayla sorar. Bir soru "dur" derse istek controller'a hiç ulaşmaz ve filtre hata yanıtını kendisi yazar. <code>/api/rooms</code> uçları filtreden geçmez; oda bilgisi varsayılan şemadadır ve bu uçlar token'ı kendisi kontrol eder.</p>
        </Prose>
        <Figure
          no="11.3"
          height={500}
          minWidth={860}
          label="RoomFilter karar zinciri: oda kodu, token, zamanlayıcıya özel uç, sahiplik, zamanı gelmiş olay, kadro kilidi, canlı yayın koruması. Her basamakta hata yanıtı ya da bir sonraki basamak."
          caption="Filtrenin karar zinciri. Soldaki sorular yukarıdan aşağı sorulur. Kırmızı kutular isteği durdurur. Sarı kutu isteği durdurmaz: zamanı gelmiş maç günü önce oynanır, sonra istek devam eder."
        >
          {FILTER_STEPS.map((step, i) => {
            const y = 16 + i * 62
            return (
              <g key={step.q}>
                <Box x={20} y={y} w={440} h={48} title={`${i + 1} · ${step.q}`} lines={[step.sub]} />
                {i < FILTER_STEPS.length - 1 && <Edge from={[240, y + 48]} to={[240, y + 59]} />}
                <Edge from={[460, y + 24]} to={[517, y + 24]} tone={step.tone === 'sig' ? undefined : 'dan'} />
                <Box x={520} y={y} w={420} h={48} tone={step.tone ?? 'dan'} title={step.out} lines={[step.outSub ?? '']} />
              </g>
            )
          })}
          <Box x={20} y={452} w={440} h={40} tone="acc" center title="RoomContext.set(oda) → controller → clear()" />
          <Edge from={[240, 438]} to={[240, 449]} />
        </Figure>
        <GTable
          head={['İŞLEM', 'SAHİP', 'ÜYE', 'NOT']}
          rows={[
            ['Okuma: puan durumu, fikstür, takımlar, maçlar', <span key="a1" className="g-pill ok">evet</span>, <span key="a2" className="g-pill ok">evet</span>, 'Canlı yayın sürerken sonuçlar kapalı.'],
            ['Kendi takımı: kadro, taktik, gelen kutusu, sözleşme', <span key="b1" className="g-pill ok">evet</span>, <span key="b2" className="g-pill ok">evet</span>, 'Servis takımın o üyeye ait olduğunu denetler.'],
            ['Transfer teklifi (kendi takımı adına)', <span key="c1" className="g-pill ok">evet</span>, <span key="c2" className="g-pill ok">evet</span>, 'Odada teklif kapalı ve gizlidir (11.6).'],
            ['Takvim ayarı, "şimdi oynat", üye çıkarma, oda silme', <span key="d1" className="g-pill ok">evet</span>, <span key="d2" className="g-pill no">hayır</span>, <code key="d3">/api/rooms</code>],
            ['Hafta oynatma, fikstür, kupa, sezon, yedek yükleme', <span key="e1" className="g-pill no">hayır</span>, <span key="e2" className="g-pill no">hayır</span>, 'Yalnızca zamanlayıcı (Modül 12).'],
          ]}
        />
      </Section>

      <Section id="kurma" no="11.4" title="Oda kurma ve giriş">
        <Prose>
          <p>Hesap yok, şifre yok. Oda kuran ya da katılan kişi rastgele bir <b>token</b> alır. Token yalnızca bu yanıtta bir kez döner. Tarayıcı onu <code>localStorage</code>'a yazar (<code>fl.rooms</code>). Veritabanı token'ın kendisini değil, SHA-256 özetini saklar. Veritabanı sızsa bile kimse özetten token'ı üretemez.</p>
          <p>Oda kurmak birkaç saniye sürer, çünkü sistem bir şema açar, ~30 tablo oluşturur ve ligi doldurur. Bir adım hata verirse şema ve kayıt silinir; yarım oda kalmaz.</p>
        </Prose>
        <Figure
          no="11.4"
          height={250}
          minWidth={860}
          label="Oda kurma: istek, varsayılan şemaya oda ve sahip kaydı, yeni şema ve tablolar, odanın içinde lig hazırlığı, kod ve token yanıtı. Hata olursa şema ve kayıt silinir."
          caption="Oda kurma adımları. Sarı kutu varsayılan şemaya, yeşil kutular odanın şemasına yazar. Kırmızı kesik çizgi hata durumunda geri almadır."
        >
          <Box x={20} y={40} w={165} h={110} tone="acc" tag="1 · İSTEK" title="POST /api/rooms" lines={['ad, takma ad']} />
          <Box x={207} y={40} w={165} h={110} tone="sig" tag="2 · PUBLIC" title="rooms + sahip" lines={['kod: 6 harf', '(0/O, 1/I yok)', 'token özeti']} />
          <Box x={394} y={40} w={165} h={110} tone="acc" tag="3 · ŞEMA" title="create schema" lines={[{ text: 'room_xxxxxx', mono: true }, 'SchemaManager', '~30 tablo']} />
          <Box x={581} y={40} w={165} h={110} tone="acc" tag="4 · LİG" title="initializeLeague(18)" lines={['RoomContext.call(oda)', '4 büyük + 14 rastgele', 'hakem, hoca, bütçe']} />
          <Box x={768} y={40} w={172} h={110} tag="5 · YANIT" title="oda + token" lines={['token bir kez döner', 'tarayıcı saklar', 'aktif oda olur']} />
          {[185, 372, 559, 746].map((x) => (
            <Edge key={x} from={[x, 95]} to={[x + 19, 95]} />
          ))}
          <Edge d="M663,150 V200 H290 V153" tone="dan" dash />
          <Txt x={476} y={222} cls="t-dan" anchor="middle">hata → drop schema + oda kaydı silinir</Txt>
        </Figure>
        <Note label="NEDEN ELLE TRANSACTION">
          <p>Şema oluşturma ve lig hazırlığı odanın şemasındaki <b>yeni</b> bağlantılarla yapılmalıdır. Açık bir <code>@Transactional</code> varsayılan şemadaki bağlantıyı tutar ve sorgular yanlış şemaya gider. Bu yüzden <code>RoomService</code> transaction'ları <code>TransactionTemplate</code> ile adım adım açar.</p>
        </Note>
      </Section>

      <Section id="menajer" no="11.5" title="Menajerler ve botlar">
        <Prose>
          <p>Tek kişilik ligde tek menajer vardı. Odada birden çok insan menajer olur. "Şu anki menajer kim?" sorusunu artık tek bir sınıf cevaplar: <code>Managers</code>. Cevap isteği kimin yaptığına göre değişir. Zamanlayıcının işinde menajer yoktur; o işte bütün insan takımları ayrı ayrı ele alınır.</p>
          <p>Bir takımı tek bir insan yönetir. İnsanı olmayan takımlar <b>bot</b>tur: yapay zekâ hocası kadroyu seçer ve transfer yapar. Bir üye ayrılırsa ya da sahip onu çıkarırsa takımı bot devralır. Takım boşa çıkar ve yeni üye onu seçebilir.</p>
        </Prose>
        <Figure
          no="11.5"
          height={300}
          minWidth={820}
          label="Ayşe'nin, Bora'nın ve zamanlayıcının istekleri Managers.current üzerinden ayrı profillere gider. 18 takımın 2'si insan, 16'sı bot."
          caption="Kim menajer? Soldaki üç istek aynı servisi çağırır ama farklı menajer profiline ulaşır. Sağda odanın 18 takımı: yeşil olanları insan yönetir, gri olanlar bottur."
        >
          <Box x={20} y={30} w={170} h={56} tone="acc" title="Ayşe’nin isteği" lines={[{ text: 'memberId = 12', mono: true }]} />
          <Box x={20} y={110} w={170} h={56} tone="acc" title="Bora’nın isteği" lines={[{ text: 'memberId = 15', mono: true }]} />
          <Box x={20} y={190} w={170} h={56} tone="ghost" title="Zamanlayıcı" lines={[{ text: 'memberId = yok', mono: true }]} />
          <Box x={240} y={100} w={170} h={76} tone="sig" title="Managers.current()" lines={['RoomContext.memberId', '→ ManagerProfile']} />
          <Edge d="M190,58 C215,58 215,120 237,120" />
          <Edge from={[190, 138]} to={[237, 138]} />
          <Edge d="M190,218 C215,218 215,158 237,158" dash />
          <Box x={460} y={30} w={150} h={56} tone="acc" title="Profil · Ayşe" lines={['Galatasaray']} />
          <Box x={460} y={110} w={150} h={56} tone="acc" title="Profil · Bora" lines={['Fenerbahçe']} />
          <Box x={460} y={190} w={150} h={56} tone="ghost" title="boş" lines={['her insan için döngü']} />
          <Edge d="M410,120 C435,120 435,58 457,58" />
          <Edge from={[410, 138]} to={[457, 138]} />
          <Edge d="M410,158 C435,158 435,218 457,218" dash />

          <Txt x={650} y={34} cls="g-ms">ODANIN 18 TAKIMI</Txt>
          {TEAMS.map((i) => {
            const col = i % 6
            const row = Math.floor(i / 6)
            const human = i === 0 || i === 1
            return (
              <g key={i}>
                <rect className={human ? 'g-box acc' : 'g-box ghost'} x={650 + col * 48} y={46 + row * 48} width="40" height="40" rx="4" />
                <Txt x={670 + col * 48} y={71 + row * 48} cls={human ? 'g-m t-acc' : 'g-s t-mut'} anchor="middle">
                  {human ? (i === 0 ? 'GS' : 'FB') : 'bot'}
                </Txt>
              </g>
            )
          })}
          <Txt x={650} y={212} cls="g-s">GS: Ayşe · FB: Bora · diğerleri yapay zekâ</Txt>
          <Txt x={650} y={232} cls="g-s">Başkasının takımı seçicide kilitli:</Txt>
          <Txt x={650} y={250} cls="g-s">"Bora yönetiyor" (409)</Txt>
          <Txt x={20} y={280} cls="g-s">Gelen kutusu ve kariyer de profile bağlı: InboxMessage.profile, ManagerSpell.profile. Başkasının mesajı açılamaz.</Txt>
        </Figure>
        <GTable
          head={['KURAL', 'NASIL']}
          rows={[
            ['Kadro ve taktik kilidi', <>Maç saatinden 5 dakika önce kapanır (<code>lineupLockAt</code>). Sonra değişiklik <span className="g-pill no">409</span>. Maç oynanınca kilit ertesi güne geçer.</>],
            ['Kadro verilmezse', <>Maç günü <code>playWeek(n, auto=true)</code> ile oynanır. Kadrosu olmayan insan takımının kadrosunu yapay zekâ seçer.</>],
            ['Güven ve kovulma', 'Her insan menajerin güveni kendi sonuçlarına göre ayrı hesaplanır (Modül 08).'],
            ['Ayrılma', <>Takım serbest kalır, bot devralır (<code>MyTeamService.release</code>). Sahip odadan ayrılamaz; odayı silebilir.</>],
          ]}
        />
      </Section>

      <Section id="teklif" no="11.6" title="Kapalı teklif">
        <Prose>
          <p>Tek kişilik ligde teklif hemen sonuçlanır. Odada bu adil olmaz: önce davranan kazanır. Bu yüzden odada teklifler <b>kapalı</b> zarfla verilir. Kimse başkasının teklifini görmez. Pencere kapanınca bütün teklifler aynı anda çözülür.</p>
          <p>Çözüm en yüksek bedelden başlar. Bedeller eşitse önce veren kazanır. Her teklif o anda yeniden denetlenir, çünkü pencere boyunca bütçe ve kadro değişmiş olabilir.</p>
        </Prose>
        <Figure
          no="11.6"
          height={320}
          minWidth={860}
          label="Pencere açıkken teklifler gizli kaydedilir. Pencere kapanınca resolveBids teklifleri bedele göre sıralar, her birini denetler ve kazandı, kaybetti ya da geçersiz sonucunu yazar."
          caption="Kapalı teklifin çözümü. Örnekte iki menajer aynı oyuncuya teklif verdi. Yüksek bedel kazanır; aynı oyuncunun diğer teklifi kaybeder. Her sonuç teklif verenin gelen kutusuna yazılır."
        >
          <Txt x={20} y={30} cls="g-ms">PENCERE AÇIK · transfer_bids</Txt>
          {[
            ['Bora → forvet Kaya', '€15M', 'PENDING'],
            ['Ayşe → forvet Kaya', '€12M', 'PENDING'],
            ['Ayşe → serbest kaleci', '€0', 'PENDING'],
          ].map(([who, fee, st], i) => (
            <g key={who}>
              <rect className="g-box" x="20" y={42 + i * 44} width="260" height="36" rx="3" />
              <Txt x={32} y={65 + i * 44}>{who}</Txt>
              <Txt x={200} y={65 + i * 44} cls="g-m" anchor="end">{fee}</Txt>
              <Txt x={268} y={65 + i * 44} cls="g-s t-mut" anchor="end">{st}</Txt>
            </g>
          ))}
          <Txt x={20} y={192} cls="g-s">Aynı oyuncuya yeni teklif öncekinin yerine geçer.</Txt>
          <Txt x={20} y={208} cls="g-s">İstenen bedelin altı → hemen ret / karşı teklif.</Txt>

          <Box x={320} y={42} w={190} h={124} tone="sig" tag="PENCERE KAPANIR" title="resolveBids()" lines={['süre doldu ya da', 'herkes "Transferlerim', 'bitti" dedi', 'bedel ↓, eşitse önce veren']} />
          <Edge from={[280, 104]} to={[317, 104]} />

          <Box
            x={550}
            y={42}
            w={190}
            h={124}
            tag="HER TEKLİF İÇİN"
            title="tryWin denetimi"
            lines={['oyuncu hâlâ ligde mi?', 'alıcı hâlâ insan mı?', 'satıcı satabilir mi?', 'bütçe ve kadro yeter mi?']}
          />
          <Edge from={[510, 104]} to={[547, 104]} />

          <Box x={780} y={42} w={160} h={36} tone="acc" center title="WON → transfer" />
          <Box x={780} y={86} w={160} h={36} tone="ghost" center title="LOST" />
          <Box x={780} y={130} w={160} h={36} tone="dan" center title="FAILED + neden" />
          <Edge d="M740,80 C760,80 760,60 777,60" tone="acc" />
          <Edge from={[740, 104]} to={[777, 104]} />
          <Edge d="M740,128 C760,128 760,148 777,148" tone="dan" />

          <Box x={320} y={200} w={420} h={60} tone="acc" center title="Bora €15M → kazandı · Ayşe €12M → kaybetti" lines={['Ayşe serbest kaleci €0 → kazandı (başka teklif yok)']} />
          <Edge d="M860,166 V230 H743" dash />
          <Txt x={870} y={200} cls="g-s">finishBid:</Txt>
          <Txt x={870} y={216} cls="g-s">gelen kutusu</Txt>
          <Txt x={20} y={300} cls="g-s">Sonra botların 2. transfer turu, kadro tamamlama ve yeni sezonun fikstürü gelir (Modül 12).</Txt>
        </Figure>
        <Note>
          <p>Başka bir <b>insanın</b> oyuncusuna teklif kapalı zarfa girmez. Teklif o menajerin gelen kutusuna gider (<code>SENT</code>). Menajer kabul ederse transfer hemen olur.</p>
        </Note>
      </Section>

      <Section id="katil" no="11.7" title="Oda kurun ve katılın">
        <Steps>
          <p><b>Odalar</b> sayfasını açın. Oda adını ve takma adınızı yazıp <b>Oda kur</b>'a basın. Siz odanın sahibisiniz.</p>
          <p>Aktif oda kartındaki <b>davet linkini</b> kopyalayın ve arkadaşlarınıza gönderin. Link <code>/join/KOD</code> biçimindedir.</p>
          <p>Arkadaşınız linki açar, takma adını yazar ve katılır. Kod ve token onun tarayıcısında saklanır.</p>
          <p>Herkes <b>Takımım</b> sayfasından bir takım seçer. Başkasının seçtiği takım kilitli görünür.</p>
          <p>Herkes <b>Hazırım</b>'a basınca lig başlar. Sahip beklemeden <b>Ligi başlat</b>'a da basabilir. İlk maç en erken 10 dakika sonraki maç saatindedir.</p>
        </Steps>
        <Note kind="caution">
          <p>Token yalnızca o tarayıcıdadır. Tarayıcı verisini silerseniz odaya aynı kişi olarak dönemezsiniz. Davet koduyla yeni bir takma adla katılmanız gerekir.</p>
        </Note>
      </Section>
    </>
  )
}

export default Rooms
