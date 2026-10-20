import { Box, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

function Tie({ x, y, top, bottom, topName, bottomName, tone }: { x: number; y: number; top: string; bottom: string; topName: string; bottomName: string; tone?: 'acc' }) {
  return (
    <g>
      <rect className={tone ? 'g-box acc' : 'g-box'} x={x} y={y} width={220} height={64} rx={4} />
      <line className="g-grid" x1={x} y1={y + 32} x2={x + 220} y2={y + 32} />
      <Txt x={x + 12} y={y + 21} cls="g-m g-b t-acc">{top}</Txt>
      <Txt x={x + 44} y={y + 21}>{topName}</Txt>
      <Txt x={x + 208} y={y + 21} cls="g-s" anchor="end">ev</Txt>
      <Txt x={x + 12} y={y + 53} cls="g-m g-b t-acc">{bottom}</Txt>
      <Txt x={x + 44} y={y + 53}>{bottomName}</Txt>
    </g>
  )
}

function Kick({ x, y, result, no }: { x: number; y: number; result: 'goal' | 'miss' | 'none'; no: number }) {
  return (
    <g>
      {result === 'goal' && <circle className="f-acc" cx={x} cy={y} r={14} />}
      {result === 'miss' && (
        <>
          <circle className="f-surf s-dan" cx={x} cy={y} r={14} strokeWidth={1.6} />
          <path className="g-ed dan" d={`M${x - 6},${y - 6} L${x + 6},${y + 6} M${x + 6},${y - 6} L${x - 6},${y + 6}`} />
        </>
      )}
      {result === 'none' && <circle className="f-muted s-strong" cx={x} cy={y} r={14} strokeDasharray="3 3" />}
      <Txt x={x + 18} y={y - 12} cls="g-ms">{no}</Txt>
    </g>
  )
}

const HOME_KICKS = ['goal', 'goal', 'miss', 'goal', 'goal'] as const
const AWAY_KICKS = ['goal', 'miss', 'goal', 'miss', 'none'] as const

function CupLive() {
  return (
    <>
      <Section id="agac" no="9.1" title="Eleme ağacı">
        <Prose>
          <p>Sistem eşleşmeleri lig sırasına göre kurar. Ligde üst sıradaki takım ev sahibi olur. Eşleşme sırası, 1. ve 2. takımın en erken finalde karşılaşmasını sağlar. Kupa turları 101, 102 ve 103 numaralı haftalardır.</p>
        </Prose>
        <Figure
          no="9.1"
          height={370}
          minWidth={820}
          label="Eleme ağacı: çeyrek finalde 1-8, 4-5, 2-7, 3-6. Yarı finalde 1/8 kazananı 4/5 kazananıyla, 2/7 kazananı 3/6 kazananıyla oynar. Final hafta 103."
          caption="Kupa eleme ağacı. Sayılar lig sırasıdır. 'ev' etiketi ev sahibini gösterir. Ağaç canlı yayın bitene kadar yeni sonucu göstermez."
        >
          <Txt x={40} y={18} cls="g-ms">ÇEYREK FİNAL · HAFTA 101</Txt>
          <Txt x={370} y={18} cls="g-ms">YARI FİNAL · HAFTA 102</Txt>
          <Txt x={700} y={18} cls="g-ms">FİNAL · HAFTA 103</Txt>
          {(
            [
              [30, '1', '8'],
              [115, '4', '5'],
              [200, '2', '7'],
              [285, '3', '6'],
            ] as [number, string, string][]
          ).map(([y, a, b]) => (
            <Tie key={a} x={40} y={y} top={a} bottom={b} topName={`Lig ${a}. takımı`} bottomName={`Lig ${b}. takımı`} />
          ))}
          <Tie x={370} y={72} top="1/8" bottom="4/5" topName="   kazanan" bottomName="   kazanan" />
          <Tie x={370} y={242} top="2/7" bottom="3/6" topName="   kazanan" bottomName="   kazanan" />
          <Tie x={700} y={157} top="YF1" bottom="YF2" topName="    kazanan" bottomName="    kazanan" tone="acc" />
          <path className="g-ed" d="M260,62 H315 V147 H260" />
          <Edge from={[315, 104]} to={[367, 104]} />
          <path className="g-ed" d="M260,232 H315 V317 H260" />
          <Edge from={[315, 274]} to={[367, 274]} />
          <path className="g-ed" d="M590,104 H645 V274 H590" />
          <Edge from={[645, 189]} to={[697, 189]} />
          <Edge from={[810, 221]} to={[810, 257]} tone="acc" />
          <Box x={700} y={260} w={220} h={56} tone="gold" title="Kupa şampiyonu" lines={[{ text: 'season.cup_winner · €6M', mono: true }]} />
          <Txt x={370} y={160} cls="g-s">Ligde üst sıradaki takım</Txt>
          <Txt x={370} y={176} cls="g-s">ev sahibi olur.</Txt>
        </Figure>
        <Note>
          <p>Kupa ödülleri: çeyrek finalde elenen €0,75M, yarı finalde elenen €1,5M, finali kaybeden €3M, şampiyon €6M alır (Modül 07). Menajer modunda kupa hedefi yönetim kurulunun güvenini etkiler (Modül 08).</p>
        </Note>
      </Section>

      <Section id="penalti" no="9.2" title="Penaltı atışları">
        <Prose>
          <p>Kupa maçı berabere biterse penaltı atışları olur. Uzatma yoktur. Her atışın gol olma olasılığı %75'tir. Şekil 9.2 bir örnek atış serisini gösterir.</p>
        </Prose>
        <Figure
          no="9.2"
          height={270}
          minWidth={820}
          label="Penaltı örneği: ev sahibi 5 atışta 4 gol, deplasman 4 atışta 2 gol. Deplasman artık yetişemediği için 10. atış yapılmaz. Sonuç 4-2."
          caption="Penaltı atışları. Küçük sayılar atış sırasıdır. 9. atıştan sonra deplasman en fazla 3 gole ulaşabilir. Ev sahibinin 4 golü var. Bu nedenle seri biter."
        >
          <Txt x={20} y={56} cls="g-t">Ev</Txt>
          <Txt x={20} y={106} cls="g-t">Dep</Txt>
          {HOME_KICKS.map((result, k) => (
            <Kick key={`h${k}`} x={110 + k * 70} y={52} result={result} no={2 * k + 1} />
          ))}
          {AWAY_KICKS.map((result, k) => (
            <Kick key={`a${k}`} x={110 + k * 70} y={102} result={result} no={2 * k + 2} />
          ))}
          <Txt x={500} y={58} cls="g-tl" anchor="middle">4</Txt>
          <Txt x={500} y={108} cls="g-tl" anchor="middle">2</Txt>
          <Txt x={560} y={56} cls="g-s">9. atıştan sonra: deplasman 2 gol, 1 atış hakkı.</Txt>
          <Txt x={560} y={74} cls="g-s">En iyi durumda 3 gol olur. 3 &lt; 4.</Txt>
          <Txt x={560} y={106} cls="t-dan">10. atış yapılmaz. Sonuç 4–2.</Txt>
          {[
            ['1 · Sırayla atış', 'ev, dep, ev, dep …', 'her atış %75 gol'],
            ['2 · Erken bitiş', 'rakip artık yetişemezse', 'seri hemen biter'],
            ['3 · Ani ölüm', '5\'er atıştan sonra eşitse', 'birer atış, fark olana kadar'],
            ['4 · Sonuç', 'penaltı skoru maça yazılır', 'home_penalties, away_penalties'],
          ].map(([title, a, b], i) => {
            const x = 20 + i * 236
            return (
              <g key={title}>
                <Box x={x} y={160} w={210} h={84} title={title} lines={[a, b]} />
                {i < 3 && <Edge from={[x + 210, 202]} to={[x + 233, 202]} />}
              </g>
            )
          })}
        </Figure>
      </Section>

      <Section id="yayin" no="9.3" title="Canlı yayın">
        <Prose>
          <p>Canlı yayın bir tekrar gösterimidir. Backend haftayı veya kupa turunu önce bir anda oynatır ve kaydeder. Sonra kayıtlı olayları dakika dakika gönderir. Yayın Server-Sent Events (SSE) kullanır. Tarayıcı bağlantıyı <code>EventSource</code> ile açar.</p>
          <p>Uç nokta: <code>GET /api/weeks/{'{n}'}/live?speed=1|3|10|0&amp;from=dakika</code>. Kupa turları da aynı uçtan yayınlanır (hafta 101–103). Oynanmamış hafta <code>409</code>, geçersiz hız <code>400</code> alır.</p>
        </Prose>
        <Figure
          no="9.3"
          width={1000}
          height={330}
          minWidth={860}
          label="Canlı yayın akışı: önce start olayı maçları gönderir, sonra 0'dan 90'a her dakika için minute olayı, en sonda end olayı. Hız 1x'te maç 30 saniye, 3x'te 10 saniye, 10x'te 3 saniye sürer."
          caption="SSE olay akışı. Her 'minute' olayının id değeri dakikadır. Bağlantı koparsa tarayıcı Last-Event-ID ile kaldığı dakikadan devam eder. 'Atla' bağlantıyı yeni hızla, aynı dakikadan yeniden açar."
        >
          <Box x={20} y={30} w={170} h={64} tone="acc" title="LiveBroadcast.tsx" lines={['new EventSource(…)']} />
          <Box x={790} y={30} w={190} h={64} tone="away" title="LiveBroadcastService" lines={['sanal thread (virtual)']} />
          <Edge from={[190, 52]} to={[787, 52]} tone="away" />
          <Txt x={488} y={44} anchor="middle">GET /weeks/5/live?speed=3&amp;from=0</Txt>
          {[
            [130, 'start', 'maçlar ve takımlar'],
            [170, 'minute · id=0 … 90', 'o dakikanın olayları, yorumlar, biriken skorlar'],
            [210, 'end', 'sonuçlar ve penaltılar'],
          ].map(([y, name, data]) => (
            <g key={String(name)}>
              <Edge from={[790, Number(y)]} to={[193, Number(y)]} tone="away" dash />
              <Txt x={488} y={Number(y) - 7} cls="g-m g-b" anchor="middle">{String(name)}</Txt>
              <Txt x={210} y={Number(y) + 14} cls="g-s">{String(data)}</Txt>
            </g>
          ))}
          <Txt x={20} y={256} cls="g-t">Hızlar</Txt>
          {[
            ['1x', '30 sn', '333 ms/dk'],
            ['3x', '10 sn', '111 ms/dk'],
            ['10x', '3 sn', '33 ms/dk'],
            ['0', 'bekleme yok', 'test, atla'],
          ].map(([speed, total, step], i) => (
            <Box key={speed} x={20 + i * 240} y={266} w={220} h={50} tone={i === 1 ? 'acc' : 'plain'} lines={[{ text: speed, mono: true, bold: true }, `maç ${total} · ${step}`]} />
          ))}
        </Figure>
        <Prose>
          <p>Ekran tüm maçları aynı anda gösterir. Skor golün dakikası gelince artar. Gol anında "GOL!" yazısı çıkar. Sağda yorum akışı var; en yeni yorum üsttedir. Fikstürde "Haftayı Oynat" önce haftayı oynatır, sonra yayını açar. Hafta sonuçları yayın bitince yüklenir.</p>
        </Prose>
      </Section>

      <Section id="yorum" no="9.4" title="Dakika yorumları">
        <Prose>
          <p><code>MatchCommentary</code> her olay türü için Türkçe bir cümle üretir. İsim ekleri ünlü uyumuna uyar: "Kaya'nın", "Ahmet'e", "Öztürk'ün". Aynı olay her zaman aynı cümleyi alır. Bu nedenle yayın ve maç detayı aynı yorumu gösterir.</p>
        </Prose>
        <GTable
          head={['OLAY', 'ÖRNEK YORUM']}
          rows={[
            ['Gol (asistli)', 'GOL! Kaya\'nın pasında Demir topu ağlara gönderdi.'],
            ['Penaltı golü', 'Ekranda "(P)" işareti. Penaltı golünde asist yoktur.'],
            ['Kendi kalesine gol', 'Ekranda "(KK)" işareti. Gol rakip takımın hanesine yazılır.'],
            ['Kaçan penaltı, VAR iptali', 'Olay akışında not: "penaltı kaçırdı", "gol VAR ile iptal".'],
            ['Değişiklik', 'Oyuna giren ve çıkan oyuncu adlarıyla bir cümle.'],
          ]}
        />
      </Section>

      <Section id="animasyon" no="9.5" title="Maç animasyonu">
        <Prose>
          <p>Oynanmış her maç sahada 2D animasyonla izlenebilir. Animasyon yeni bir simülasyon yapmaz. Skor ve olaylar backend'de bellidir. Animasyon bu olayları sahnelere çevirir. Bu yönteme "olay güdümlü koreografi" denir.</p>
          <ul>
            <li>Olay dakikasında "yönetmen" bir sahne kurar. Golde paslaşma, asist yapan oyuncunun pası ve golcünün şutu oynanır. Penaltı, kendi kalesine gol, VAR, kart, sakatlık ve değişikliğin de kendi sahnesi var.</li>
            <li>Olaysız zamanda paslaşma, top kaybı ve gol olmayan şutlar oynanır. Bu oyun tohumlu rastgele sayıyla üretilir. Aynı maç her açılışta aynı görünür.</li>
            <li>Oyuncular dizilişteki yerlerinden topa göre kayan bir blok hâlinde koşar. Topa en yakın rakip baskı yapar.</li>
            <li>Sahne sürerken dakika sayacı bekler. Skor ve anlatım, sahne bitince güncellenir. Bu nedenle gol ekranda animasyondan önce görünmez.</li>
          </ul>
        </Prose>
        <Figure
          no="9.4"
          height={280}
          minWidth={820}
          label="Animasyon akışı: backend olayları yönetmene gelir, yönetmen sahne adımları üretir, motor oyuncuları ve topu hareket ettirir, tuval çizer. Sahne bitince ekran sayacı bir sonraki dakikaya geçer."
          caption="Maç animasyonunun akışı. Kesik ok sayaç ile sahne arasındaki beklemedir. Olaysız dakikada sayaç beklemez."
        >
          <Box x={20} y={40} w={180} h={80} tone="sig" title="Olaylar" lines={['dakika, tür, oyuncu,', { text: 'assistPlayerId', mono: true }, 'kadro: giriş / çıkış']} />
          <Edge from={[200, 80]} to={[247, 80]} />
          <Box x={250} y={40} w={200} h={80} tone="acc" title="Yönetmen" lines={['olaydan sahne adımları:', 'pas, top sürme, şut,', 'faul, kutlama, santra']} />
          <Edge from={[450, 80]} to={[497, 80]} />
          <Box x={500} y={40} w={200} h={80} title="Motor" lines={['oyuncu: blok + "arrive"', 'top: uçuş, yükseklik', 'saniyede 60 güncelleme']} />
          <Edge from={[700, 80]} to={[747, 80]} />
          <Box x={750} y={40} w={190} h={80} tone="away" title="Tuval (canvas)" lines={['saha, oyuncular, top', 'GOL! ve kart yazıları']} />
          <Box x={250} y={180} w={450} h={60} tone="ghost" title="Ekran sayacı (dakika)" lines={['olaylı dakikada sahne bitene kadar bekler → onMinuteShown']} />
          <Edge d="M475,180 V122" dash />
          <Edge d="M600,122 V178" dash />
          <Txt x={485} y={156} cls="g-s">yeni dakika</Txt>
          <Txt x={610} y={156} cls="g-s">sahne bitti</Txt>
          <Txt x={20} y={210} cls="g-s">Nerede: menajer maçı</Txt>
          <Txt x={20} y={226} cls="g-s">(Animasyon / Diziliş), maç</Txt>
          <Txt x={20} y={242} cls="g-s">detayında "Maçı izle",</Txt>
          <Txt x={20} y={258} cls="g-s">canlı yayında "Sahada izle".</Txt>
        </Figure>
        <Note>
          <p>Testler animasyonu 27 maçta doğruladı. 50 golün hepsinde top doğru kaleye girdi ve golcü şutu topun en fazla 2,5 m yanından çekti. Kart sahnelerinde kartı gören oyuncu topu sürene müdahale etti. Hiçbir sahne takılmadı.</p>
        </Note>
      </Section>

      <Section id="fark" no="9.6" title="Lig ve kupa farkı">
        <GTable
          head={['KONU', 'LİG', 'KUPA']}
          rows={[
            ['Katılan takım', 'Tüm 1. Lig takımları (18)', 'Ligin ilk 8 takımı'],
            ['Biçim', 'Çift devre, 34 hafta', 'Tek maç eleme, 3 tur'],
            ['Hafta numarası', '1–34', '101–103'],
            ['Beraberlik', '1 puan', 'Penaltı atışları'],
            ['Moral ve takım gücü', <span key="k188_41" className="g-pill ok">değişir</span>, <span key="k188_87" className="g-pill no">değişmez</span>],
            ['Kadro, kart cezası, sakatlık, form', <span key="k189_55" className="g-pill ok">işler</span>, <span key="k189_98" className="g-pill ok">işler</span>],
            ['Bilet geliri / maaş', <span key="k190_39" className="g-pill ok">ikisi de</span>, <span key="k190_84" className="g-pill warn">yalnız bilet</span>],
            ['Puan durumu, sezon istatistiği', <span key="k191_50" className="g-pill ok">sayılır</span>, <span key="k191_96" className="g-pill no">sayılmaz</span>],
            ['Kariyer istatistiği', <span key="k192_39" className="g-pill ok">sayılır</span>, <span key="k192_85" className="g-pill ok">sayılır</span>],
            ['Menajerin maçı', 'kadro bekler', 'kadro bekler'],
          ]}
        />
      </Section>

      <Section id="oynat" no="9.7" title="Kupayı oynatın">
        <Note kind="caution">
          <p>Kupa yarıdayken yeni sezon başlamaz. Kupayı başlattıysanız tüm turları bitirin. Aksi halde fikstür isteği <code>409</code> yanıtı alır.</p>
        </Note>
        <Steps>
            <p>Ligin 34 haftasını oynatın. Kupa yalnız lig bitince açılır.</p>
            <p>Kupa sayfasını açın.</p>
            <p>"Kupayı başlat" düğmesine basın. Sistem çeyrek final eşleşmelerini kurar.</p>
            <p>"Çeyrek final · canlı oynat" düğmesine basın. Yayını izleyin veya "Atla" düğmesine basın.</p>
            <p>Yarı final ve final için 4. adımı tekrarlayın.</p>
        </Steps>
        <Note>
          <p>Canlı yayını atlamak için "Tüm kupayı oynat" düğmesine basın. Sistem kalan turları tek istekte oynatır.</p>
        </Note>
      </Section>
    </>
  )
}

export default CupLive
