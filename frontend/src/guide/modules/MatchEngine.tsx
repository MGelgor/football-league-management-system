import type { ReactNode } from 'react'
import { Box, Edge, Figure, GTable, Note, Prose, Section, Txt } from '../parts'

const LAMBDA_HOME = 1.5702
const LAMBDA_AWAY = 0.9291

function poisson(lambda: number, k: number) {
  let result = Math.exp(-lambda)
  for (let i = 1; i <= k; i++) result *= lambda / i
  return result
}

function pct(value: number, digits = 0) {
  return `%${(value * 100).toFixed(digits).replace('.', ',')}`
}

const SETUP_ROWS = [
  {
    label: 'EV SAHİBİ',
    y: 40,
    tone: 'acc' as const,
    steps: [
      ['Güç 90', '4 büyüklerden biri'],
      ['Maç gücü 98', '+8 bonus'],
      ['Hoca +1,5', 'ustalık 80'],
      ['Diziliş +2', '4-3-3 > 3-5-2'],
      ['Ev +5, moral +1', '(60 − 50) × 0,1'],
      ['Etkin güç 107,5', ''],
    ],
  },
  {
    label: 'DEPLASMAN',
    y: 160,
    tone: 'sig' as const,
    steps: [
      ['Güç 70', 'normal takım'],
      ['Maç gücü 70', 'bonus yok'],
      ['Hoca −0,5', 'ustalık 40'],
      ['Diziliş −2', '3-5-2 yenilir'],
      ['Ev 0, moral −1', '(40 − 50) × 0,1'],
      ['Etkin güç 66,5', ''],
    ],
  },
]

type Mark = 'goal' | 'own' | 'var' | 'yellow' | 'injury' | 'sub'

function marker(kind: Mark, x: number, y: number): ReactNode {
  switch (kind) {
    case 'goal':
      return <circle className="f-acc" cx={x} cy={y} r={8} />
    case 'own':
      return <circle className="f-dan" cx={x} cy={y} r={8} />
    case 'var':
      return <circle className="f-surf s-strong" cx={x} cy={y} r={7} strokeWidth={1.5} strokeDasharray="3 2" />
    case 'yellow':
      return <rect className="f-sig" x={x - 5} y={y - 8} width={10} height={15} rx={1} />
    case 'injury':
      return <path className="g-ed dan" style={{ strokeWidth: 3 }} d={`M${x - 7},${y} H${x + 7} M${x},${y - 7} V${y + 7}`} />
    case 'sub':
      return <rect className="f-surf" x={x - 6} y={y - 6} width={12} height={12} transform={`rotate(45 ${x} ${y})`} stroke="var(--text)" strokeWidth={1.4} />
  }
}

const HOME_EVENTS: [number, Mark, string, string][] = [
  [12, 'yellow', 'sarı', 'DEF'],
  [27, 'goal', 'gol (P)', 'penaltıcı FV'],
  [58, 'sub', 'değişiklik', 'OS çıkar, OS girer'],
  [68, 'var', 'VAR iptal', 'FV, skor değişmez'],
  [81, 'yellow', 'sarı', 'OS'],
]

const AWAY_EVENTS: [number, Mark, string, string][] = [
  [23, 'yellow', 'sarı', 'OS'],
  [38, 'own', 'kendi kalesine', 'DEF · ev golü'],
  [49, 'injury', 'sakatlık', 'DEF → yedek'],
  [64, 'goal', 'gol', 'FV, asist OS'],
  [82, 'sub', 'değişiklik', 'FV çıkar, FV girer'],
]

const WEIGHTS = [
  { title: 'Gol ağırlığı', note: '× o anki güç', values: [['FV', 15], ['OS', 5], ['DEF', 2], ['KL', 0]] },
  { title: 'Asist ağırlığı', note: '× o anki güç · %70 olasılıkla asist', values: [['FV', 4], ['OS', 6], ['DEF', 3], ['KL', 0]] },
  { title: 'Kart ağırlığı', note: 'güç etkisi yok · × hakem çarpanı', values: [['FV', 3], ['OS', 4], ['DEF', 5], ['KL', 1]] },
] as const

const RATING_BARS = [
  { name: 'Taban', from: 5, to: 6, cls: 'f-strong', label: '6,0', sub: '' },
  { name: 'Gol', from: 6, to: 7, cls: 'f-acc', label: '+1,0', sub: '' },
  { name: 'Asist', from: 7, to: 7.6, cls: 'f-acc', label: '+0,6', sub: '' },
  { name: 'Galibiyet', from: 7.6, to: 8, cls: 'f-acc', label: '+0,4', sub: '× 90/90' },
  { name: 'Sarı kart', from: 8, to: 7.6, cls: 'f-dan', label: '−0,4', sub: '' },
  { name: 'Rastgele', from: 7.6, to: 7.7, cls: 'f-strong', label: '+0,1', sub: '−0,4 … +0,4' },
  { name: 'Reyting', from: 5, to: 7.7, cls: 'f-sig', label: '7,7', sub: '3,0–10,0' },
]

function ratingY(value: number) {
  return 250 - (value - 5) * 60
}

function fatigueX(minute: number) {
  return 70 + (minute * 840) / 90
}

function fatigueY(value: number) {
  return 230 - value * 20
}

function MatchEngine() {
  const homeWin = 0.5234
  const awayWin = 0.2232
  return (
    <>
      <Section id="skor" no="4.1" title="Takım kurulumu ve skor">
        <Prose>
          <p>Skor rastgeledir. Rastgelelik güce bağlıdır. Sistem önce her takımın kurulumunu hazırlar: maç gücü, moral, diziliş, oyun stili ve hoca bonusu (<code>TeamSetup</code>). Sonra her takım için bir "beklenen gol" (λ) değeri hesaplar. Gol sayısı Poisson dağılımından seçilir.</p>
          <p>Şekil 4.1 bir örnek maçı adım adım hesaplar. Ev sahibi 4 büyüklerden biridir. Deplasman takımı normal bir takımdır.</p>
        </Prose>
        <Figure
          no="4.1"
          width={1020}
          height={380}
          minWidth={900}
          label="Takım kurulumu örneği: ev etkin gücü 107,5, deplasman 66,5. Oran 0,618. Stil çarpanlarıyla beklenen gol ev 1,57, deplasman 0,93. Olasılıklar yüzde 52, 26, 22."
          caption="Takım kurulumu ve skor, sayısal örnek. λ = 2,6 × güç oranı × kendi stil çarpanı × rakibin stil çarpanı. Aynı λ değerleri iki işe yarar: skoru seçer ve maç öncesi olasılıkları hesaplar. 4 büyükler bonusu yalnız bu hesapta kullanılır. Ekrandaki güç 90 kalır."
        >
          {SETUP_ROWS.map((row) => (
            <g key={row.label}>
              <Txt x={20} y={row.y - 10} cls="g-ms">{row.label}</Txt>
              {row.steps.map(([title, sub], i) => {
                const x = 20 + i * 132
                const last = i === row.steps.length - 1
                return (
                  <g key={title}>
                    <Box x={x} y={row.y} w={120} h={64} tone={i === 0 || last ? row.tone : 'plain'} strong={last} title={title} lines={sub ? [sub] : []} />
                    {!last && <Edge from={[x + 120, row.y + 32]} to={[x + 129, row.y + 32]} />}
                  </g>
                )
              })}
            </g>
          ))}
          <Edge d="M800,72 H812 V92 H827" />
          <Edge d="M800,192 H812 V116 H827" />
          <Box x={830} y={70} w={176} h={64} title="Güç oranı" lines={[{ text: '107,5 / (107,5 + 66,5)', mono: true }, { text: '= 0,618', mono: true, tone: 'acc', bold: true }]} />
          <Edge from={[918, 134]} to={[918, 157]} />
          <Box x={830} y={160} w={176} h={80} tone="sig" title="Stil çarpanı" lines={['ev hücum, dep savunma', { text: 'ev: ×1,15 × 0,85', mono: true }, { text: 'dep: ×0,85 × 1,10', mono: true }]} />
          <Edge from={[918, 240]} to={[918, 263]} />
          <Box x={830} y={266} w={176} h={64} tone="acc" title="Beklenen gol (λ)" lines={[{ text: 'ev λ = 1,57', mono: true }, { text: 'dep λ = 0,93', mono: true }]} />
          <Edge from={[830, 298]} to={[793, 298]} />
          <Box x={620} y={266} w={170} h={64} tone="sig" title="Olasılıklar" lines={['tablo toplamı (Şekil 4.3)', { text: '%52 · %26 · %22', mono: true }]} />
          <Edge d="M918,330 V356 H515 V333" />
          <Box x={430} y={266} w={170} h={64} title="Skor" lines={['iki Poisson örneği', { text: 'ör. 2–1', mono: true }]} />
          <Txt x={20} y={270} cls="g-s">Temel toplam gol 2,6'dır (2 × 1,3).</Txt>
          <Txt x={20} y={287} cls="g-s">Güç oranı bu toplamı iki takıma paylaştırır.</Txt>
          <Txt x={20} y={304} cls="g-s">Stil çarpanları iki λ değerini değiştirir.</Txt>
          <Txt x={20} y={321} cls="g-s">Etkin güç en az 1 olur.</Txt>
        </Figure>
        <GTable
          head={['GİRDİ', 'ETKİ', 'KAYNAK']}
          rows={[
            ['4 büyükler bonusu', '+8 maç gücü', <code key="k161_56">Team.matchStrength()</code>],
            ['Ev avantajı', '+5 etkin güç', <code key="k162_49">ScoreSimulator</code>],
            ['Moral', '(moral − 50) × 0,1 → −5 … +5', <code key="k163_65">ScoreSimulator</code>],
            ['Hoca ustalığı', '(ustalık − 50) / 20 → −2,5 … +2,5', <code key="k164_81">Manager.strengthBonus()</code>],
            ['Diziliş', 'yenen +2, yenilen −2, aynı diziliş 0', <code key="k165_71">Formation.advantageOver()</code>],
            ['Oyun stili', 'hücum ×1,15 / ×1,10 · savunma ×0,85 / ×0,85', <code key="k166_82">PlayStyle</code>],
            ['Kadro kalitesi', 'λ × 0,8 … 1,0 (yalnız kullanıcının seçtiği 11)', <code key="k167_93">TacticsService.lineupQuality()</code>],
          ]}
        />
        <Note>
          <p>Stil satırında ilk çarpan takımın kendi λ değerini, ikinci çarpan rakibin λ değerini değiştirir. Diziliş, hoca ve yapay zekânın stil seçimi Modül 06'da ayrıntılıdır.</p>
        </Note>
        <Figure
          no="4.2"
          height={270}
          minWidth={720}
          label="Poisson dağılımı: ev sahibi λ 1,57, deplasman λ 0,93 için 0-5 gol olasılıkları."
          caption="Gol sayısı olasılıkları. Sistem her takım için bu dağılımdan bağımsız bir sayı çeker (poissonRandom). Güçlü takımın çubukları sağa kayar."
        >
          {[0, 0.1, 0.2, 0.3, 0.4].map((g) => {
            const y = 230 - g * 500
            return (
              <g key={g}>
                <line className={g === 0 ? 'g-axis' : 'g-grid'} x1={80} y1={y} x2={940} y2={y} />
                <Txt x={72} y={y + 4} cls="g-ms" anchor="end">{pct(g)}</Txt>
              </g>
            )
          })}
          {[0, 1, 2, 3, 4, 5].map((k) => {
            const x0 = 100 + k * 140
            return (
              <g key={k}>
                {[
                  [LAMBDA_HOME, 'f-acc'],
                  [LAMBDA_AWAY, 'f-sig'],
                ].map(([lambda, cls], j) => {
                  const value = poisson(Number(lambda), k)
                  const h = value * 500
                  const x = x0 + 20 + j * 50
                  return (
                    <g key={j}>
                      <rect className={String(cls)} x={x} y={230 - h} width={44} height={h} />
                      <Txt x={x + 22} y={225 - h} cls="g-ms" anchor="middle">{pct(value)}</Txt>
                    </g>
                  )
                })}
                <Txt x={x0 + 67} y={250} cls="g-m" anchor="middle">{k} gol</Txt>
              </g>
            )
          })}
          <rect className="f-acc" x="600" y="6" width="12" height="12" />
          <Txt x={618} y={16}>Ev sahibi, λ = 1,57</Txt>
          <rect className="f-sig" x="770" y="6" width="12" height="12" />
          <Txt x={788} y={16}>Deplasman, λ = 0,93</Txt>
        </Figure>
      </Section>

      <Section id="olasilik" no="4.2" title="Kazanma olasılıkları">
        <Prose>
          <p>Fikstür her oynanmamış maç için üç yüzde gösterir. Sistem iki dağılımı çarparak bir skor tablosu kurar. Sonra tabloyu üç bölgeye ayırır ve her bölgeyi toplar. Maç oynanınca bu yüzdeler maç kaydında kalır.</p>
        </Prose>
        <Figure
          no="4.3"
          height={340}
          minWidth={720}
          label="Skor olasılık tablosu: çapraz altı ev galibiyeti yüzde 52, çapraz beraberlik yüzde 26, çapraz üstü deplasman galibiyeti yüzde 22."
          caption="Skor olasılık tablosu. Her hücre bir skorun olasılığıdır. Beraberlik yüzdesi kalan değerdir. Bu kural toplamı her zaman 100 yapar."
        >
          <Txt x={242} y={30} cls="g-s" anchor="middle">deplasman golü →</Txt>
          <Txt x={96} y={44} cls="g-s" anchor="end">ev golü ↓</Txt>
          {[0, 1, 2, 3, 4, 5].map((j) => (
            <Txt key={j} x={110 + j * 44 + 22} y={52} cls="g-m" anchor="middle">{j}</Txt>
          ))}
          {[0, 1, 2, 3, 4, 5].map((i) => (
            <g key={i}>
              <Txt x={100} y={60 + i * 44 + 27} cls="g-m" anchor="end">{i}</Txt>
              {[0, 1, 2, 3, 4, 5].map((j) => {
                const value = poisson(LAMBDA_HOME, i) * poisson(LAMBDA_AWAY, j)
                return (
                  <g key={j}>
                    <rect className={i > j ? 'f-accs' : i === j ? 'f-line' : 'f-sigs'} x={110 + j * 44} y={60 + i * 44} width={42} height={42} rx={2} />
                    <Txt x={110 + j * 44 + 21} y={60 + i * 44 + 26} cls="g-ms" anchor="middle">{(value * 100).toFixed(1).replace('.', ',')}</Txt>
                  </g>
                )
              })}
            </g>
          ))}
          {[
            ['f-accs', 'Ev sahibi kazanır', 'ev golü > deplasman golü', pct(homeWin)],
            ['f-line', 'Beraberlik', 'ev golü = deplasman golü', '%26'],
            ['f-sigs', 'Deplasman kazanır', 'ev golü < deplasman golü', pct(awayWin)],
          ].map(([cls, title, sub, value], k) => {
            const y = 74 + k * 62
            return (
              <g key={title}>
                <rect className={`${cls} s-strong`} x="440" y={y} width="28" height="28" rx="2" />
                <Txt x={482} y={y + 12} cls="g-t">{title}</Txt>
                <Txt x={482} y={y + 28} cls="g-s">{sub}</Txt>
                <Txt x={760} y={y + 22} cls="g-xl" anchor="end">{value}</Txt>
              </g>
            )
          })}
          <Txt x={440} y={278} cls="g-m">P(ev = i, dep = j) = P(i; 1,57) × P(j; 0,93)</Txt>
          <Txt x={440} y={298} cls="g-s">Kod 0–12 gol tablosunu toplar. Şekil 0–5 bölümünü gösterir.</Txt>
          <Txt x={440} y={316} cls="g-s">Hücre değerleri yüzde olarak verilmiştir.</Txt>
        </Figure>
      </Section>

      <Section id="kadro" no="4.3" title="İlk 11">
        <Prose>
          <p>Skor belli olunca sistem iki takımın kadrosunu kurar. Diziliş takımın dizilişidir. Seçim oyuncunun efektif gücünü kullanır: güç × form − yorgunluk. Seçimde biraz rastgelelik var. Bu nedenle güçleri yakın oyuncular sırayla oynar.</p>
          <p>Menajer modunda kullanıcı kendi takımının ilk 11'ini seçer. Rakibin kadrosunu yapay zekâ seçer.</p>
        </Prose>
        <Figure
          no="4.4"
          height={380}
          minWidth={820}
          label="İlk 11 seçimi: kadrodan cezalı ve sakat oyuncular çıkar, kalanlara efektif güç artı rastgele skor verilir, dizilişteki her mevkide en yüksek skor seçilir."
          caption="İlk 11 seçimi, diziliş 4-3-3. Kutulardaki sayılar efektif güçtür. Kırmızı çizgili oyuncular cezalı veya sakattır ve kadroya girmez."
        >
          <Txt x={20} y={24} cls="g-ms">KADRO · 18 OYUNCU</Txt>
          {(
            [
              ['KL', [[78, 's'], [64, 'b']]],
              ['DEF', [[75, 's'], [72, 's'], [70, 'x'], [68, 's'], [66, 's'], [60, 'b']]],
              ['OS', [[80, 's'], [77, 'x'], [74, 's'], [71, 's'], [65, 'b'], [62, 'b']]],
              ['FV', [[83, 's'], [79, 's'], [72, 's'], [61, 'b']]],
            ] as [string, [number, 's' | 'b' | 'x'][]][]
          ).map(([position, players], r) => {
            const y = 40 + r * 50
            return (
              <g key={position}>
                <Txt x={20} y={y + 18} cls="g-ms">{position}</Txt>
                {players.map(([value, state], i) => {
                  const x = 56 + i * 38
                  return (
                    <g key={i}>
                      <rect className={state === 's' ? 'g-box acc' : state === 'x' ? 'g-box dan' : 'g-box'} x={x} y={y} width={34} height={26} rx={3} />
                      <Txt x={x + 17} y={y + 17} cls="g-m" anchor="middle">{value}</Txt>
                      {state === 'x' && <line className="g-ed dan" x1={x + 3} y1={y + 23} x2={x + 31} y2={y + 3} />}
                    </g>
                  )
                })}
              </g>
            )
          })}
          {[
            ['g-box acc', 'ilk 11'],
            ['g-box', 'yedek'],
            ['g-box dan', 'cezalı / sakat'],
          ].map(([cls, text], i) => (
            <g key={text}>
              <rect className={cls} x={20 + i * 92} y={250} width={14} height={14} rx={2} />
              <Txt x={40 + i * 92} y={262} cls="g-s">{text}</Txt>
            </g>
          ))}
          <Txt x={20} y={292} cls="g-s">Yedek: 5 oyuncu. En fazla 3 değişiklik.</Txt>
          <Txt x={20} y={309} cls="g-s">Yedek tercihen aynı mevkiden girer.</Txt>
          {[
            ['Cezalı ve sakatları çıkar', 'isAvailable() = false olanlar'],
            ['Her oyuncuya skor ver', 'efektif güç + rastgele 0–15'],
            ['Dizilişe göre seç', '4-3-3: 1 KL · 4 DEF · 3 OS · 3 FV'],
            ['Eksik mevkiyi doldur', 'kalan en iyiler, kaleci en son'],
          ].map(([title, sub], i) => {
            const y = 40 + i * 66
            return (
              <g key={title}>
                <Box x={320} y={y} w={270} h={48} tag={String(i + 1)} title={title} lines={[sub]} />
                {i < 3 && <Edge from={[455, y + 48]} to={[455, y + 63]} />}
              </g>
            )
          })}
          <Edge from={[290, 64]} to={[317, 64]} />
          <Edge d="M590,262 H604 V190 H617" />
          <Txt x={620} y={14} cls="g-ms">DİZİLİŞ 4-3-3 · HÜCUM YÖNÜ ↑</Txt>
          <rect className="g-pitch" x="620" y="20" width="320" height="340" rx="3" />
          <line className="g-pline" x1="620" y1="190" x2="940" y2="190" />
          <circle className="g-pline" cx="780" cy="190" r="36" />
          <rect className="g-pline" x="700" y="20" width="160" height="52" />
          <rect className="g-pline" x="700" y="308" width="160" height="52" />
          {(
            [
              [690, 92, 79],
              [780, 82, 83],
              [870, 92, 72],
              [700, 160, 74],
              [780, 150, 80],
              [860, 160, 71],
              [665, 250, 72],
              [745, 250, 75],
              [815, 250, 68],
              [895, 250, 66],
              [780, 330, 78],
            ] as [number, number, number][]
          ).map(([x, y, value]) => (
            <g key={`${x}-${y}`}>
              <circle className="f-surf s-acc" cx={x} cy={y} r={16} strokeWidth={1.5} />
              <Txt x={x} y={y + 4} cls="g-m g-b" anchor="middle">{value}</Txt>
            </g>
          ))}
        </Figure>
      </Section>

      <Section id="olay" no="4.4" title="Dakika dakika olaylar">
        <Prose>
          <p>Sistem önce olayların dakikasını seçer. Sonra olayları dakika sırasıyla işler. Bir olay yalnız o dakikada sahada olan oyuncuya yazılır. Bu nedenle oyundan çıkan oyuncu gol atamaz.</p>
        </Prose>
        <GTable
          head={['OLAY', 'KURAL']}
          rows={[
            ['Gol', 'Sayı skordan gelir. Her gol için 1–90 arası bir dakika seçilir.'],
            ['Penaltı golü', 'Gollerin ~%9\'u × hakem çarpanı. Penaltıcı atar, asist yok. Kaleci penaltı atmaz.'],
            ['Kaçan penaltı', 'Başarı oranı ~%78. Skoru değiştirmez. Rakip kaleci +0,6 reyting alır.'],
            ['Kendi kalesine gol', 'Penaltı olmayan gollerin %4\'ü. Sahadaki bir rakip atar, gol yine aynı takıma yazılır.'],
            ['VAR iptali', 'Maç başına %10. Skoru değiştirmez, yalnız olay olarak yazılır.'],
            ['Sarı kart', 'Takım başına ortalama 1,8 × hakem çarpanı. İkinci sarı kırmızıya döner.'],
            ['Direkt kırmızı', '%4 × hakem çarpanı. Atılan oyuncunun yerine kimse girmez.'],
            ['Sakatlık', 'Sahadaki her oyuncu için %0,8. Hak varsa yerine yedek girer.'],
            ['Taktik değişiklik', '46.–88. dakikada 1–3 değişiklik. Kaleci çıkmaz. O anki gücü düşük oyuncu daha sık çıkar.'],
          ]}
        />
        <Figure
          no="4.5"
          width={1000}
          height={310}
          minWidth={860}
          label="Örnek maçın zaman çizelgesi: ev sahibi 27. dakikada penaltıdan, deplasman 38. dakikada kendi kalesine gol atar; deplasman 64. dakikada gol atar. Sonuç 2-1."
          caption="Örnek maçın olay çizelgesi, sonuç 2–1. Kendi kalesine gol oyuncunun takımının satırında görünür ama rakibe sayılır. VAR iptali skoru değiştirmez. Aynı dakikada sıra: kart → sakatlık → değişiklik → VAR → kaçan penaltı → gol."
        >
          <rect className="g-box ghost" x={60 + (46 * 880) / 90} y={52} width={(42 * 880) / 90} height={170} rx={3} />
          <Txt x={60 + (46 * 880) / 90 + 8} y={46} cls="g-s">taktik değişiklik penceresi: 46.–88. dakika</Txt>
          <line className="g-grid" x1={60 + (45 * 880) / 90} y1={52} x2={60 + (45 * 880) / 90} y2={232} strokeDasharray="3 3" />
          <Txt x={60 + (45 * 880) / 90 - 4} y={46} cls="g-ms" anchor="end">devre arası</Txt>
          {[
            [112, 'Ev'],
            [182, 'Dep'],
          ].map(([y, label]) => (
            <g key={String(label)}>
              <line className="g-grid" x1={60} y1={Number(y)} x2={940} y2={Number(y)} />
              <Txt x={50} y={Number(y) + 4} cls="g-t" anchor="end">{label}</Txt>
            </g>
          ))}
          <line className="g-axis" x1={60} y1={236} x2={940} y2={236} />
          {[0, 15, 30, 45, 60, 75, 90].map((m) => {
            const x = 60 + (m * 880) / 90
            return (
              <g key={m}>
                <line className="g-axis" x1={x} y1={236} x2={x} y2={242} />
                <Txt x={x} y={255} cls="g-ms" anchor="middle">{m}'</Txt>
              </g>
            )
          })}
          {HOME_EVENTS.map(([minute, kind, title, detail]) => {
            const x = 60 + (minute * 880) / 90
            return (
              <g key={`h${minute}`}>
                {marker(kind, x, 112)}
                <Txt x={x} y={94} cls="g-b" anchor="middle">{`${minute}' ${title}`}</Txt>
                <Txt x={x} y={80} cls="g-s" anchor="middle">{detail}</Txt>
              </g>
            )
          })}
          {AWAY_EVENTS.map(([minute, kind, title, detail]) => {
            const x = 60 + (minute * 880) / 90
            return (
              <g key={`a${minute}`}>
                {marker(kind, x, 182)}
                <Txt x={x} y={205} cls="g-b" anchor="middle">{`${minute}' ${title}`}</Txt>
                <Txt x={x} y={220} cls="g-s" anchor="middle">{detail}</Txt>
              </g>
            )
          })}
          <Txt x={972} y={117} cls="g-tl" anchor="middle">2</Txt>
          <Txt x={972} y={187} cls="g-tl" anchor="middle">1</Txt>
          {(
            [
              ['goal', 'gol'],
              ['own', 'kendi kalesine'],
              ['var', 'VAR iptal'],
              ['yellow', 'sarı'],
              ['injury', 'sakatlık'],
              ['sub', 'değişiklik'],
            ] as [Mark, string][]
          ).map(([kind, text], i) => (
            <g key={kind}>
              {marker(kind, 68 + i * 120, 286)}
              <Txt x={82 + i * 120} y={290} cls="g-s">{text}</Txt>
            </g>
          ))}
        </Figure>
        <Prose>
          <p>Sistem gol atan, asist yapan ve kart gören oyuncuyu ağırlıklı seçimle bulur. Ağırlık mevkiye bağlıdır. Gol ve asistte ağırlık oyuncunun o anki gücüyle çarpılır. Gücü iki kat olan oyuncu iki kat sık gol atar.</p>
        </Prose>
        <Figure
          no="4.6"
          height={210}
          minWidth={760}
          label="Mevki ağırlıkları. Gol: forvet 15, orta saha 5, defans 2, kaleci 0. Asist: forvet 4, orta saha 6, defans 3, kaleci 0. Kart: forvet 3, orta saha 4, defans 5, kaleci 1."
          caption="Mevki ağırlıkları. 4-4-2'de bu ağırlıklar golleri yaklaşık %50 forvet, %35 orta saha, %15 defans olarak dağıtır. Kaleci gol atmaz ve asist yapmaz."
        >
          {WEIGHTS.map((panel, i) => {
            const x0 = 20 + i * 320
            const max = Math.max(...panel.values.map(([, v]) => v))
            return (
              <g key={panel.title}>
                <Txt x={x0} y={22} cls="g-t">{panel.title}</Txt>
                {panel.values.map(([position, value], r) => {
                  const y = 40 + r * 34
                  return (
                    <g key={position}>
                      <Txt x={x0} y={y + 15} cls="g-ms">{position}</Txt>
                      <rect className="f-line" x={x0 + 36} y={y} width={195} height={22} rx={1} />
                      {value > 0 && <rect className={value === max ? 'f-acc' : 'f-strong'} x={x0 + 36} y={y} width={value * 13} height={22} rx={1} />}
                      <Txt x={x0 + 42 + value * 13} y={y + 15} cls="g-m g-b">{value}</Txt>
                    </g>
                  )
                })}
                <Txt x={x0} y={194} cls="g-s">{panel.note}</Txt>
              </g>
            )
          })}
        </Figure>
      </Section>

      <Section id="yorgunluk" no="4.5" title="Maç içi yorgunluk">
        <Prose>
          <p>Sahada geçen her dakika oyuncunun o anki gücünü düşürür. 90 dakikada düşüş 8'dir. 31 yaş ve üstünde düşüş 1,25 kat, 21 yaş ve altında 0,85 kat olur. Gol ve asist ağırlığı o anki gücü kullanır. Bu nedenle oyuna sonradan giren dinç oyuncu öne çıkar.</p>
        </Prose>
        <Figure
          no="4.7"
          height={270}
          minWidth={720}
          label="Maç içi yorgunluk: 90 dakikada 31 yaş üstü 10, normal 8, 21 yaş altı 6,8 güç kaybeder. 60. dakikada giren yedek maç sonunda yaklaşık 2,7 kaybeder."
          caption="Maç içi yorgunluk. O anki güç = efektif güç − maç içi yorgunluk. Kesik çizgi 60. dakikada giren yedektir. Menajerin canlı maçında ikinci yarıda sahadakilerin ortalama yorgunluğu × 2 takım gücünden düşer."
        >
          {[0, 2, 4, 6, 8, 10].map((v) => (
            <g key={v}>
              <line className={v === 0 ? 'g-axis' : 'g-grid'} x1={70} y1={fatigueY(v)} x2={910} y2={fatigueY(v)} />
              <Txt x={62} y={fatigueY(v) + 4} cls="g-ms" anchor="end">{v === 0 ? '0' : `−${v}`}</Txt>
            </g>
          ))}
          {[0, 15, 30, 45, 60, 75, 90].map((m) => (
            <Txt key={m} x={fatigueX(m)} y={250} cls="g-ms" anchor="middle">{m}'</Txt>
          ))}
          <Txt x={20} y={18} cls="g-s">güç kaybı</Txt>
          {[
            [10, 'f-dan', 'dan', '31+ yaş: −10'],
            [8, 'f-strong', '', 'normal: −8'],
            [6.8, 'f-acc', 'acc', '≤ 21 yaş: −6,8'],
          ].map(([end, , tone, label]) => (
            <g key={String(label)}>
              <line className={`g-ed ${tone}`} x1={fatigueX(0)} y1={fatigueY(0)} x2={fatigueX(90)} y2={fatigueY(Number(end))} style={{ strokeWidth: 2 }} />
              <Txt x={fatigueX(90) - 6} y={fatigueY(Number(end)) - 8} cls={tone ? `g-s t-${tone}` : 'g-s'} anchor="end">{String(label)}</Txt>
            </g>
          ))}
          <line className="g-ed dash" x1={fatigueX(60)} y1={fatigueY(0)} x2={fatigueX(90)} y2={fatigueY(8 / 3)} style={{ strokeWidth: 2 }} />
          <Txt x={fatigueX(60)} y={fatigueY(0) - 8} cls="g-s">60' yedek girer</Txt>
          <Txt x={fatigueX(90) + 4} y={fatigueY(8 / 3) + 4} cls="g-s">−2,7</Txt>
        </Figure>
        <Note>
          <p>Bu yorgunluk yalnız maçın içinde geçerlidir. Maçlar arası yorgunluk (üst üste ilk 11'de başlama) ayrı bir kuraldır. Ayrıntı: Modül 05.</p>
        </Note>
      </Section>

      <Section id="reyting" no="4.6" title="İstatistik ve reyting">
        <Prose>
          <p>Takım istatistikleri skorla tutarlı olur. Sistem şu kuralları uygular:</p>
          <ul>
            <li>Şut ≥ isabetli şut ≥ gol.</li>
            <li>Kurtarış = rakibin isabetli şutu − rakibin golü.</li>
            <li>Faul ≥ kart sayısı.</li>
            <li>Topla oynama λ payına göre hesaplanır. Değer aralığı %30–70. İki takımın toplamı 100'dür.</li>
          </ul>
          <p>Her oyuncu 6,0 puanla başlar. Olaylar puanı artırır veya azaltır. Sonuç 3,0–10,0 aralığında kalır. En yüksek reytingli oyuncu maçın oyuncusu olur.</p>
        </Prose>
        <Figure
          no="4.8"
          height={300}
          minWidth={720}
          label="Reyting şelale grafiği: taban 6,0, gol +1,0, asist +0,6, galibiyet +0,4, sarı kart −0,4, rastgele +0,1, sonuç 7,7."
          caption="Bir forvetin reytingi: 90 dakika, 1 gol, 1 asist, 1 sarı kart, takım 2–1 kazandı. Eksen 5,0'dan başlar."
        >
          {[5, 6, 7, 8].map((v) => (
            <g key={v}>
              <line className={v === 5 ? 'g-axis' : 'g-grid'} x1={70} y1={ratingY(v)} x2={930} y2={ratingY(v)} />
              <Txt x={62} y={ratingY(v) + 4} cls="g-ms" anchor="end">{v},0</Txt>
            </g>
          ))}
          {RATING_BARS.map((bar, i) => {
            const x = 90 + i * 120
            const top = ratingY(Math.max(bar.from, bar.to))
            const bottom = ratingY(Math.min(bar.from, bar.to))
            return (
              <g key={bar.name}>
                <rect className={bar.cls} x={x} y={top} width={80} height={bottom - top} rx={1} />
                <Txt x={x + 40} y={top - 6} cls="g-m g-b" anchor="middle">{bar.label}</Txt>
                <Txt x={x + 40} y={270} anchor="middle">{bar.name}</Txt>
                {bar.sub && <Txt x={x + 40} y={287} cls="g-s" anchor="middle">{bar.sub}</Txt>}
                {i > 0 && i < 6 && <line className="g-grid" x1={x - 40} y1={ratingY(bar.from)} x2={x} y2={ratingY(bar.from)} strokeDasharray="3 3" />}
              </g>
            )
          })}
        </Figure>
        <GTable
          head={['KURAL', 'DEĞER']}
          rows={[
            ['Gol / asist', '+1,0 / +0,6'],
            ['Galibiyet / mağlubiyet (oynanan süre payı ile)', '+0,4 / −0,4'],
            ['Gol yememe, en az 60 dakika (kaleci / defans)', '+0,8 / +0,5'],
            ['Kaleci: her kurtarış / her yenilen gol / kurtarılan penaltı', '+0,15 / −0,25 / +0,6'],
            ['Defans: her yenilen gol', '−0,1'],
            ['Sarı / kırmızı kart', '−0,4 / −1,5'],
            ['Kendi kalesine gol / kaçan penaltı', '−1,0 / −0,8'],
            ['Rastgele etki', '−0,4 … +0,4'],
            ['20 dakikadan az oynayan', 'tüm etki × 0,5'],
          ]}
        />
      </Section>

      <Section id="sonra" no="4.7" title="Maçtan sonra">
        <Prose>
          <p>Maç bitince sistem takımları ve oyuncuları günceller. Kupa maçı moral ve gücü değiştirmez. Diğer güncellemeler kupada da olur.</p>
        </Prose>
        <Figure
          no="4.9"
          height={420}
          minWidth={820}
          label="Maçtan sonra moral, takım gücü, oyuncu durumu, form ve yorgunluk, finans güncellenir. Ligde hepsi değişir. Kupada moral ve güç değişmez."
          caption="Maç sonrası güncellemeler. Sağdaki tablo lig ve kupa farkını gösterir."
        >
          <Box x={20} y={170} w={170} h={80} tone="acc" center title="Maç bitti" lines={['skor ve olaylar hazır']} />
          {[
            ['Moral', 'galibiyet +10 · mağlubiyet −10', 'güçlü rakibe karşı ek bonus (tablo)', true, false],
            ['Takım gücü', 'round((puan − beklenen puan) × 0,7)', 'beklenen = 3 × P(G) + P(B)', true, false],
            ['Oyuncu durumu', 'kırmızı +1 maç · her 4. sarı +1 maç', 'sakatlık: hafif, orta veya uzun', true, true],
            ['Form ve yorgunluk', 'reyting son 5 maça eklenir', 'ilk 11\'de başlayan: üst üste sayı +1', true, true],
            ['Finans', 'ev sahibine bilet geliri', 'lig haftasında iki takıma maaş', true, true],
          ].map(([title, line1, line2, league, cup], i) => {
            const y = 14 + i * 80
            return (
              <g key={String(title)}>
                <Edge from={[190, 210]} to={[257, y + 33]} />
                <Box x={260} y={y} w={370} h={66} title={String(title)} lines={[String(line1), String(line2)]} />
                <line className="g-grid" x1={630} y1={y + 33} x2={714} y2={y + 33} strokeDasharray="2 3" />
                {[
                  [760, league],
                  [870, cup],
                ].map(([x, yes]) => (
                  <g key={String(x)}>
                    <rect className={yes ? 'g-box acc' : 'g-box dan'} x={Number(x) - 44} y={y + 20} width={88} height={26} rx={13} />
                    <Txt x={Number(x)} y={y + 37} cls="g-s" anchor="middle">{yes ? 'değişir' : 'değişmez'}</Txt>
                  </g>
                ))}
              </g>
            )
          })}
          <Txt x={760} y={10} cls="g-ms" anchor="middle">LİG</Txt>
          <Txt x={870} y={10} cls="g-ms" anchor="middle">KUPA</Txt>
        </Figure>
        <Prose>
          <p>Moral değişimi rakibin gücüne bağlıdır. Rakip en az 5 güç üstünse sürpriz sonuç ek moral getirir. Favori takım zayıf rakibe kaybederse ek moral kaybeder.</p>
        </Prose>
        <GTable
          head={['DURUM', 'HESAP', 'MORAL']}
          rows={[
            ['Eşit güç, 2–1 galibiyet', '+10', '+10'],
            ['16 güç zayıf takım, 2–1 galibiyet', '+10 + 16/2 + 2 gol', '+20'],
            ['16 güç zayıf takım, 1–1 beraberlik', '8/2 + 1 gol', '+5'],
            ['16 güç zayıf takım, 1–3 mağlubiyet', '−10 + 1 gol', '−9'],
            ['16 güç favori takım, 1–2 mağlubiyet', '−10 − 16/2', '−18'],
            ['16 güç favori takım, beraberlik', '−8/2', '−4'],
          ]}
        />
        <Prose>
          <p>Güç değişimi beklentiye bağlıdır. Beklenen sonucu alan takım az değişir. Sürpriz sonuç gücü daha çok değiştirir. Tablo Şekil 4.1'deki maçın sonuçlarını gösterir.</p>
        </Prose>
        <GTable
          head={['TAKIM', 'BEKLENEN PUAN', 'GALİBİYET', 'BERABERLİK', 'MAĞLUBİYET']}
          rows={[
            ['Ev sahibi (güç 90)', '1,82', '+1', '−1', '−1'],
            ['Deplasman (güç 70)', '0,92', '+1', '0', '−1'],
          ]}
        />
        <Note>
          <p>Cezalı veya sakat oyuncu maçı kaçırır. Maç bitince sistem önce bu oyuncuların sayacını 1 azaltır. Sonra bu maçın yeni kartlarını ve sakatlıklarını yazar.</p>
        </Note>
      </Section>
    </>
  )
}

export default MatchEngine
