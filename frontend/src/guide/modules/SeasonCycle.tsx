import { Box, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

const CIRCLE_WEEKS = [
  { title: 'HAFTA 1', arr: 'ABCDEF', matches: 'A–F · E–B · C–D' },
  { title: 'HAFTA 2', arr: 'AFBCDE', matches: 'E–A · F–D · C–B' },
  { title: 'HAFTA 3', arr: 'AEFBCD', matches: 'A–D · C–E · F–B' },
]

const END_STEPS = [
  { title: 'Puan durumu', lines: ['son tablo hesaplanır', 'güç: sıraya göre −3…+4'] },
  { title: 'Şampiyon', lines: ['1. sıra kaydedilir', 'finished = true'] },
  { title: 'Lig ödülleri', lines: ['1. €20M … son €2M', 'Modül 07'] },
  { title: 'Yönetim kurulu', lines: ['menajerin güveni', 'Modül 08'] },
  { title: 'Oyuncu gelişimi', lines: ['yaş +1, güç, emeklilik', 'Modül 05'] },
  { title: 'Düşme / yükselme', lines: ['3 takım düşer', '3 takım çıkar'] },
  { title: 'Sözleşmeler', lines: ['uzatılır ya da', 'oyuncu serbest kalır'] },
  { title: 'Transfer penceresi', lines: ['açılır: altyapı, serbest', 'oyuncular, yapay zekâ'] },
]

function rankChange(rank: number) {
  if (rank === 1) return 4
  if (rank <= 4) return 2
  if (rank > 15) return -3
  return rank <= 9 ? 1 : -1
}

function signed(value: number) {
  return value > 0 ? `+${value}` : value < 0 ? `−${-value}` : '0'
}

function RelegationTable({ x, rows }: { x: number; rows: [string, string, 'down' | 'big' | 'stay'][] }) {
  return (
    <>
      {rows.map(([rank, name, state], i) => {
        const y = 50 + i * 34
        return (
          <g key={rank}>
            <rect className={state === 'down' ? 'g-box dan' : state === 'big' ? 'g-box acc' : 'g-box'} x={x} y={y} width="280" height="30" rx="3" />
            <Txt x={x + 14} y={y + 20} cls="g-m">{rank}</Txt>
            <Txt x={x + 48} y={y + 20}>{name}</Txt>
            <Txt x={x + 266} y={y + 20} cls={state === 'down' ? 'g-s t-dan' : state === 'big' ? 'g-s t-acc' : 'g-s'} anchor="end">
              {state === 'down' ? 'düşer' : state === 'big' ? '4 büyük · kalır' : 'kalır'}
            </Txt>
          </g>
        )
      })}
    </>
  )
}

function SeasonCycle() {
  return (
    <>
      <Section id="durum" no="3.1" title="Sezon durumları">
        <Prose>
          <p>Sistemin 5 durumu var. Durumu ayrı bir alan tutmaz. Sistem durumu son sezonun kayıtlarından çıkarır: <code>finished</code> alanı, kupa maçları ve <code>cup_winner</code> alanı.</p>
        </Prose>
        <Figure
          no="3.1"
          width={1010}
          height={340}
          minWidth={820}
          label="Durum diyagramı: sezon yok, lig devam ediyor, lig bitti, kupa devam ediyor, kupa bitti. Yeni sezon lig bitti veya kupa bitti durumundan başlar."
          caption="Sezon durumları. Yeşil kesik ok yeni sezondur ve transfer penceresini kapatır. Kupa devam ederken yeni sezon başlamaz. Kırmızı ok devam eden sezonu siler."
        >
          <Box x={20} y={130} w={130} h={60} rx={28} tone="ghost" center title="Sezon yok" lines={['ilk açılış']} />
          <Box x={250} y={130} w={170} h={60} rx={28} tone="acc" center title="Lig devam ediyor" lines={['takım ekle/sil kilitli']} />
          <Box x={500} y={130} w={140} h={60} rx={28} center title="Lig bitti" lines={['pencere açık']} />
          <Box x={720} y={30} w={180} h={60} rx={28} tone="sig" center title="Kupa devam ediyor" lines={['yeni sezon kilitli']} />
          <Box x={720} y={230} w={180} h={60} rx={28} center title="Kupa bitti" lines={['pencere hâlâ açık']} />

          <Edge from={[150, 160]} to={[247, 160]} tone="acc" />
          <Txt x={200} y={152} cls="g-s" anchor="middle">fikstür oluştur</Txt>
          <Edge d="M305,130 C305,82 365,82 365,127" />
          <Txt x={335} y={80} anchor="middle">haftayı oynat (34 kez)</Txt>
          <Edge from={[420, 150]} to={[497, 150]} />
          <Txt x={459} y={142} cls="g-s" anchor="middle">son hafta</Txt>
          <Edge from={[500, 174]} to={[423, 174]} tone="acc" dash />
          <Txt x={460} y={190} cls="t-acc" anchor="middle">yeni sezon</Txt>
          <Edge d="M570,130 V60 H717" />
          <Txt x={645} y={52} cls="g-s" anchor="middle">kupayı başlat</Txt>
          <Edge d="M862,90 C862,128 896,128 896,93" />
          <Txt x={904} y={122} cls="g-s">turu oynat (3 kez)</Txt>
          <Edge from={[790, 90]} to={[790, 227]} />
          <Txt x={782} y={165} cls="g-s" anchor="end">final oynandı</Txt>
          <Edge d="M810,290 V320 H370 V193" tone="acc" dash />
          <Txt x={590} y={313} cls="t-acc" anchor="middle">yeni sezon</Txt>
          <Edge d="M300,190 V250 H85 V193" tone="dan" dash />
          <Txt x={192} y={243} cls="t-dan" anchor="middle">sıfırla</Txt>
        </Figure>
        <GTable
          head={['GEÇİŞ', 'API', 'ENGEL']}
          rows={[
            ['Fikstür oluştur / yeni sezon', <code key="k94_49">POST /api/fixtures/generate</code>, 'Sezon devam ediyor veya kupa yarıda (409). Takım sayısı 18\'den az veya tek (400).'],
            ['Haftayı oynat', <code key="k95_33">POST /api/weeks/{'{n}'}/play</code>, 'Hafta oynandı, sırası değil, menajerin kadrosu yok (409).'],
            ['Tüm sezonu oynat', <code key="k96_36">POST /api/seasons/play-all</code>, 'Fikstür yok (400). Menajer modunda ?auto=true gerekir.'],
            ['Kupa', <code key="k97_23">POST /api/cup/start · /play-round · /play-all</code>, 'Lig bitmedi. Kupa zaten başladı veya bitti (409).'],
            ['Sıfırla', <code key="k98_28">DELETE /api/fixtures</code>, 'Sezon zaten bitti (409).'],
          ]}
        />
      </Section>

      <Section id="fikstur" no="3.2" title="Fikstür algoritması">
        <Prose>
          <p>Sistem "circle method" algoritmasını kullanır. Algoritma bir takımı sabit tutar. Diğer takımlar her hafta bir adım döner. Karşı karşıya gelen takımlar o haftanın maçlarını oluşturur.</p>
          <p>Şekil 3.2 algoritmayı 6 takımla gösterir. Gerçek ligde 18 takım var. Kural aynıdır.</p>
        </Prose>
        <Figure
          no="3.2"
          height={250}
          minWidth={760}
          label="Circle method: A takımı sabit kalır, diğer beş takım her hafta saat yönünde bir adım döner. Dikey çizgiler eşleşmeleri gösterir."
          caption={
            <>
              Circle method, 6 takım, ilk 3 hafta. Yeşil A sabit kalır. Diğer takımlar her hafta saat yönünde bir adım döner. Maç listesinde ilk takım ev sahibidir. Kural: <code>(hafta + sıra) % 2 == 0</code> ise üstteki takım ev sahibidir.
            </>
          }
        >
          {CIRCLE_WEEKS.map((week, k) => {
            const x0 = 20 + k * 320
            const top = [week.arr[0], week.arr[1], week.arr[2]]
            const bottom = [week.arr[5], week.arr[4], week.arr[3]]
            return (
              <g key={week.title}>
                <rect className="g-box ghost" x={x0} y={10} width={280} height={228} rx={5} />
                <Txt x={x0 + 16} y={32} cls="g-ms">{week.title}</Txt>
                {[0, 1, 2].map((i) => {
                  const cx = x0 + 60 + i * 80
                  return (
                    <g key={i}>
                      <line className="g-ed" x1={cx} y1={82} x2={cx} y2={138} />
                      {[
                        [62, top[i]],
                        [158, bottom[i]],
                      ].map(([cy, team]) => {
                        const fixed = team === 'A'
                        return (
                          <g key={String(cy)}>
                            <circle className={fixed ? 'f-acc s-acc' : 'f-surf s-strong'} cx={cx} cy={Number(cy)} r={20} strokeWidth={1.2} />
                            <Txt x={cx} y={Number(cy) + 5} cls={fixed ? 'g-t t-surf' : 'g-t'} anchor="middle">
                              {team}
                            </Txt>
                          </g>
                        )
                      })}
                    </g>
                  )
                })}
                <Txt x={x0 + 140} y={206} cls="g-m" anchor="middle">{week.matches}</Txt>
                <Txt x={x0 + 140} y={224} cls="g-s" anchor="middle">ilk takım ev sahibi</Txt>
                {k < 2 && <Edge from={[x0 + 282, 110]} to={[x0 + 318, 110]} />}
              </g>
            )
          })}
        </Figure>
        <Prose>
          <p>Algoritma yalnız ilk devreyi üretir. İkinci devre aynı eşleşmeleri ters ev sahibiyle tekrarlar.</p>
        </Prose>
        <Figure
          no="3.3"
          height={170}
          minWidth={760}
          label="34 haftalık şerit: 1-17 ilk devre, 18-34 ikinci devre. Hafta 1'deki A-F maçı hafta 18'de F-A olarak tekrarlanır."
          caption="İki devre. N takım için ilk devre N−1 haftadır. 18 takım 34 hafta ve 306 maç oynar. Her hafta 9 maç vardır."
        >
          {Array.from({ length: 34 }, (_, i) => {
            const x = 30 + i * 26
            const first = i < 17
            return (
              <g key={i}>
                <rect className={first ? 'f-accs s-acc' : 'f-sigs'} stroke={first ? undefined : 'var(--warning)'} x={x} y={50} width={24} height={34} rx={2} />
                <Txt x={x + 12} y={71} cls="g-ms" anchor="middle">{i + 1}</Txt>
              </g>
            )
          })}
          <Txt x={251} y={38} cls="g-t" anchor="middle">1. devre · hafta 1–17</Txt>
          <Txt x={693} y={38} cls="g-t" anchor="middle">2. devre · hafta 18–34</Txt>
          <Edge d="M42,86 Q263,150 481,89" tone="acc" />
          <Txt x={263} y={140} cls="t-acc" anchor="middle">Hafta 1: A–F → Hafta 18: F–A</Txt>
          <Txt x={263} y={157} cls="g-s" anchor="middle">aynı eşleşme, ev sahibi ters</Txt>
          <Txt x={914} y={140} cls="g-m" anchor="end">18 takım · 34 hafta · 306 maç</Txt>
        </Figure>
        <Note>
          <p>Fikstür oluşunca her haftanın maçlarına bir hakem atanır. Aynı haftada bir hakem yalnız bir maç yönetir. Ayrıntı: Modül 06.</p>
        </Note>
      </Section>

      <Section id="sira" no="3.3" title="Hafta sırası">
        <Prose>
          <p>Haftalar sırayla oynanır. Backend bu kuralı kontrol eder. Frontend "Haftayı Oynat" düğmesini yalnız sıradaki haftada gösterir. Başka bir hafta için gelen istek <code>409</code> yanıtı alır.</p>
        </Prose>
        <Figure
          no="3.4"
          height={190}
          minWidth={760}
          label="Haftalar 1-4 oynandı, hafta 5 sıradaki, 6-10 kilitli. Hafta 5 isteği 200, hafta 3 ve 7 istekleri 409 alır."
          caption={
            <>
              Hafta sırası kuralı. Sistem ilk oynanmamış haftayı bulur. İstek bu haftaya değilse sistem <code>WeekOrderException</code> fırlatır. Menajer modunda bir kontrol daha var: kullanıcının kadrosu.
            </>
          }
        >
          {Array.from({ length: 10 }, (_, i) => {
            const x = 30 + i * 90
            const week = i + 1
            const state = week <= 4 ? 'played' : week === 5 ? 'next' : 'locked'
            return (
              <Box
                key={week}
                x={x}
                y={60}
                w={80}
                h={56}
                tone={state === 'played' ? 'acc' : state === 'next' ? 'sig' : 'ghost'}
                strong={state === 'next'}
                center
                title={`Hafta ${week}`}
                lines={[state === 'played' ? 'oynandı' : state === 'next' ? 'sıradaki' : 'kilitli']}
              />
            )
          })}
          <Edge from={[430, 18]} to={[430, 57]} tone="acc" />
          <Txt x={440} y={32} cls="t-acc">POST /weeks/5/play → 200 OK</Txt>
          <Edge from={[250, 160]} to={[250, 119]} tone="dan" />
          <Txt x={260} y={174} cls="t-dan">POST /weeks/3/play → 409 (oynandı)</Txt>
          <Edge from={[610, 160]} to={[610, 119]} tone="dan" />
          <Txt x={620} y={174} cls="t-dan">POST /weeks/7/play → 409 (sırası değil)</Txt>
        </Figure>
        <Note>
          <p>"Tüm Sezonu Oynat" aynı kuralı kullanır. Sistem kalan haftaları sırayla tek tek oynatır.</p>
        </Note>
      </Section>

      <Section id="son" no="3.4" title="Sezon sonu">
        <Prose>
          <p>Son hafta oynanınca <code>SeasonEndService</code> otomatik çalışır. Servis 8 adımı aynı transaction içinde ve şekildeki sırayla yapar. Kullanıcı bir düğmeye basmaz.</p>
        </Prose>
        <Figure
          no="3.5"
          height={300}
          minWidth={820}
          label="Sezon sonu adımları: puan durumu ve güç, şampiyon, lig ödülleri, yönetim kurulu, oyuncu gelişimi, düşme ve yükselme, sözleşmeler, transfer penceresi."
          caption="Sezon sonu adımları. Adım 4 yalnız menajer modunda bir şey yapar. Adım 8'den sonra kupa ve transferler aynı dönemde yapılabilir."
        >
          <Box x={10} y={100} w={120} h={90} tone="sig" center title="Son hafta" lines={['oynandı', 'playWeek içinde']} />
          <rect className="g-grp" x="148" y="16" width="802" height="270" rx="6" />
          <Txt x={160} y={34} cls="g-ms">SeasonEndService.finishSeason() · tek transaction</Txt>
          <Edge from={[130, 145]} to={[157, 145]} />
          {END_STEPS.map((step, i) => {
            const row = Math.floor(i / 4)
            const col = i % 4
            const x = 166 + col * 194
            const y = 50 + row * 116
            return (
              <g key={step.title}>
                <Box x={x} y={y} w={176} h={90} tone={i === 7 ? 'acc' : i === 3 ? 'ghost' : 'plain'} tag={`ADIM ${i + 1}`} title={step.title} lines={step.lines} />
                {col < 3 && <Edge from={[x + 176, y + 45]} to={[x + 191, y + 45]} />}
              </g>
            )
          })}
          <Edge d="M836,140 V154 H254 V163" />
        </Figure>
        <Prose>
          <p>Adım 1 her takımın gücünü son sıraya göre değiştirir. Üst sıradaki takım güç kazanır. Alt sıradaki takım güç kaybeder.</p>
        </Prose>
        <Figure
          no="3.6"
          height={280}
          minWidth={720}
          label="Sıraya göre güç değişimi: 1. sıra +4, 2-4 +2, 5-9 +1, 10-15 -1, 16-18 -3."
          caption="18 takımlı ligde sıraya göre sezon sonu güç değişimi. 4 büyükler 85'in altına düşmez. Normal takım 84'ün üstüne çıkmaz."
        >
          <Txt x={20} y={18} cls="g-s">güç değişimi</Txt>
          {[-3, -2, -1, 0, 1, 2, 3, 4].map((v) => {
            const y = 130 - v * 22
            return (
              <g key={v}>
                <line className={v === 0 ? 'g-zero' : 'g-grid'} x1={64} y1={y} x2={924} y2={y} />
                <Txt x={56} y={y + 4} cls="g-ms" anchor="end">{signed(v)}</Txt>
              </g>
            )
          })}
          {Array.from({ length: 18 }, (_, i) => {
            const rank = i + 1
            const v = rankChange(rank)
            const x = 70 + i * 48
            const y = v > 0 ? 130 - v * 22 : 130
            const h = Math.abs(v) * 22
            return (
              <g key={rank}>
                <rect className={v > 0 ? 'f-acc' : 'f-dan'} x={x} y={y} width={34} height={h} />
                <Txt x={x + 17} y={v > 0 ? y - 5 : 130 + h + 13} cls="g-m" anchor="middle">{signed(v)}</Txt>
                <Txt x={x + 17} y={226} cls="g-ms" anchor="middle">{rank}</Txt>
              </g>
            )
          })}
          <Txt x={20} y={226} cls="g-s">sıra</Txt>
          <rect className="f-away" x="68" y="236" width="38" height="18" rx="2" />
          <Txt x={87} y={249} cls="g-ms t-surf" anchor="middle">ŞL</Txt>
          <rect className="f-sigs" x="116" y="236" width="86" height="18" rx="2" />
          <Txt x={159} y={249} cls="g-ms" anchor="middle">Avrupa Ligi</Txt>
          <rect className="f-dans" x="788" y="236" width="134" height="18" rx="2" />
          <Txt x={855} y={249} cls="g-ms t-dan" anchor="middle">düşme hattı</Txt>
        </Figure>
      </Section>

      <Section id="dusme" no="3.5" title="Düşme ve yükselme">
        <Prose>
          <p>Son 3 takım düşer. 4 büyükler düşmez. Sistem tabloyu sondan yukarı tarar. Sistem 4 büyükleri atlar ve 3 takım bulunca durur. Bu nedenle 4 büyüklerden biri son 3'teyse bir üstteki takım düşer.</p>
        </Prose>
        <Figure
          no="3.7"
          height={260}
          minWidth={820}
          label="Normal durumda 16, 17, 18. sıradaki takımlar düşer. 17. sırada 4 büyüklerden biri varsa 15, 16 ve 18 düşer. Yerlerine 3 takım çıkar."
          caption="Düşme hattı. Sol tablo normal durumu, orta tablo 4 büyükler istisnasını gösterir. Takım adları örnektir. Sağ taraf iki sonucu ve kaydı gösterir."
        >
          <Txt x={20} y={34} cls="g-t">Normal durum</Txt>
          <Txt x={340} y={34} cls="g-t">4 büyüklerden biri son 3'te</Txt>
          <RelegationTable
            x={20}
            rows={[
              ['14', 'Bursa SK', 'stay'],
              ['15', 'Konya FK', 'stay'],
              ['16', 'Samsun Spor', 'down'],
              ['17', 'Adana Gücü', 'down'],
              ['18', 'İzmir Birlik', 'down'],
            ]}
          />
          <RelegationTable
            x={340}
            rows={[
              ['14', 'Bursa SK', 'stay'],
              ['15', 'Konya FK', 'down'],
              ['16', 'Samsun Spor', 'down'],
              ['17', 'Beşiktaş', 'big'],
              ['18', 'İzmir Birlik', 'down'],
            ]}
          />
          <Txt x={340} y={240} cls="g-s">tarama: 18 → 17 (atla) → 16 → 15, dur</Txt>
          <Box x={680} y={50} w={260} h={56} tone="dan" title="3 takım düşer" lines={['1. Lig\'den ayrılır, kayıtları kalır']} />
          <Box x={680} y={120} w={260} h={56} tone="acc" title="3 takım çıkar" lines={['1. Lig\'e katılır']} />
          <Box x={680} y={196} w={260} h={56} lines={[{ text: 'season_team_change', mono: true, bold: true }, 'düşen ve çıkan takımların kaydı']} />
          <Edge d="M680,78 H662 V224 H677" />
          <line className="g-ed" x1="662" y1="148" x2="677" y2="148" />
        </Figure>
        <Note label="SONRAKİ FAZ">
          <p>Alt lig (2. Lig) Faz 36'da tamamlanacak. Bu faz bitince düşen takımlar 2. Lig'de oynayacak ve 2. Lig'in ilk 3'ü yükselecek. Bu rehber o fazdan sonra güncellenmelidir.</p>
        </Note>
      </Section>

      <Section id="sifirla" no="3.6" title="Sezonu sıfırlayın">
        <Prose>
          <p>Bu prosedür devam eden sezonu siler. Bitmiş sezonlar arşivde kalır.</p>
        </Prose>
        <Note kind="caution">
          <p>Sıfırlama bu sezonun tüm maçlarını, olaylarını, istatistiklerini ve finans kayıtlarını siler. Bu işlem geri alınamaz. Gerekirse önce bir kayıt noktası alın (Modül 10).</p>
        </Note>
        <Steps>
            <p>Fikstür sayfasını açın.</p>
            <p>"Sezonu sıfırla" düğmesine basın.</p>
            <p>
              "Evet, sıfırla" düğmesine basın. Frontend <code>DELETE /api/fixtures</code> isteğini gönderir.
            </p>
            <p>Sonucu kontrol edin. Takımların gücü ve bütçesi sezon başındaki değere döner. Moral 50 olur. Ceza, sakatlık ve sarı kart sayıları 0 olur.</p>
            <p>Takım listesini değiştirin (isteğe bağlı). Sonra fikstürü yeniden oluşturun.</p>
        </Steps>
      </Section>
    </>
  )
}

export default SeasonCycle
