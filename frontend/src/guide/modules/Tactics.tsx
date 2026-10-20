import { Box, Edge, Figure, GTable, Note, Prose, Section, Txt } from '../parts'

const FORMATIONS: [string, number, number, number][] = [
  ['4-4-2', 4, 4, 2],
  ['4-3-3', 4, 3, 3],
  ['3-5-2', 3, 5, 2],
  ['5-3-2', 5, 3, 2],
  ['4-5-1', 4, 5, 1],
]

function node(i: number) {
  const angle = -Math.PI / 2 + (i * 2 * Math.PI) / 5
  return [250 + Math.cos(angle) * 140, 175 + Math.sin(angle) * 140] as const
}

/** İki düğüm arasında, çemberlerin kenarında biten ok. */
function between(a: number, b: number, inset = 34) {
  const [x1, y1] = node(a)
  const [x2, y2] = node(b)
  const len = Math.hypot(x2 - x1, y2 - y1)
  const ux = (x2 - x1) / len
  const uy = (y2 - y1) / len
  return `M${x1 + ux * inset},${y1 + uy * inset} L${x2 - ux * (inset + 2)},${y2 - uy * (inset + 2)}`
}

function MiniPitch({ x, label, def, mid, fwd }: { x: number; label: string; def: number; mid: number; fwd: number }) {
  const rows: [number, number][] = [
    [fwd, 34],
    [mid, 74],
    [def, 114],
    [1, 148],
  ]
  return (
    <g>
      <rect className="g-pitch" x={x} y={20} width={150} height={150} rx={3} />
      <line className="g-pline" x1={x} y1={95} x2={x + 150} y2={95} />
      <rect className="g-pline" x={x + 40} y={146} width={70} height={24} />
      {rows.map(([count, y]) =>
        Array.from({ length: count }, (_, i) => (
          <circle key={`${y}-${i}`} className="f-surf" cx={x + (150 * (i + 1)) / (count + 1)} cy={y} r={7} />
        )),
      )}
      <Txt x={x + 75} y={192} cls="g-tl" anchor="middle">{label}</Txt>
    </g>
  )
}

function Tactics() {
  return (
    <>
      <Section id="dizilis" no="6.1" title="Dizilişler">
        <Prose>
          <p>Sistemde 5 diziliş var. Diziliş ilk 11'deki mevki sayılarını belirler. Her diziliş iki dizilişi yener ve iki dizilişe yenilir. Kural taş-kâğıt-makas gibi döngüseldir. Hiçbir diziliş her rakibe karşı en iyi değildir.</p>
        </Prose>
        <Figure
          no="6.1"
          height={210}
          minWidth={760}
          label="Beş diziliş: 4-4-2, 4-3-3, 3-5-2, 5-3-2, 4-5-1. Her birinde kaleci, defans, orta saha ve forvet sayıları."
          caption="Beş diziliş. Saha alttan yukarı hücum eder. Her dizilişte 1 kaleci var. Sayılar defans, orta saha ve forvet sırasıdır."
        >
          {FORMATIONS.map(([label, def, mid, fwd], i) => (
            <MiniPitch key={label} x={20 + i * 188} label={label} def={def} mid={mid} fwd={fwd} />
          ))}
        </Figure>
        <Figure
          no="6.2"
          height={350}
          minWidth={720}
          label="Diziliş döngüsü: her diziliş sıradaki bir sonraki ve üç sonraki dizilişi yener. Yenen taraf etkin güce +2, yenilen taraf −2 alır."
          caption="Diziliş döngüsü. Ok yenen dizilişten yenilen dizilişe gider. Dış oklar 'bir sonraki', iç oklar 'üç sonraki' kuralıdır. Yenen taraf etkin güce +2, yenilen −2 alır. Aynı diziliş 0'dır."
        >
          {FORMATIONS.map((_, i) => (
            <g key={`e${i}`}>
              <Edge d={between(i, (i + 1) % 5)} tone="acc" />
              <Edge d={between(i, (i + 3) % 5)} tone="sig" />
            </g>
          ))}
          {FORMATIONS.map(([label], i) => {
            const [x, y] = node(i)
            return (
              <g key={label}>
                <circle className="f-surf s-strong" cx={x} cy={y} r={32} strokeWidth={1.5} />
                <Txt x={x} y={y + 5} cls="g-t" anchor="middle">{label}</Txt>
              </g>
            )
          })}
          <rect className="f-acc" x="480" y="40" width="22" height="4" />
          <Txt x={510} y={46} cls="g-s">yener (bir sonraki)</Txt>
          <rect className="f-sig" x="480" y="62" width="22" height="4" />
          <Txt x={510} y={68} cls="g-s">yener (üç sonraki)</Txt>
          <Txt x={480} y={110} cls="g-t">Örnek: 4-3-3 ile 3-5-2</Txt>
          <Txt x={480} y={130} cls="g-s">4-3-3, 3-5-2'yi yener.</Txt>
          <Txt x={480} y={148} cls="g-s">4-3-3 tarafı etkin güce +2 alır.</Txt>
          <Txt x={480} y={166} cls="g-s">3-5-2 tarafı −2 alır.</Txt>
          <Txt x={480} y={184} cls="g-s">Toplam fark 4 güçtür.</Txt>
          <Txt x={480} y={226} cls="g-m">advantageOver(other):</Txt>
          <Txt x={480} y={244} cls="g-m">mesafe = (diğer − bu) mod 5</Txt>
          <Txt x={480} y={262} cls="g-m">1 veya 3 → +2 · 2 veya 4 → −2</Txt>
        </Figure>
        <GTable
          head={['DİZİLİŞ', 'YENER', 'YENİLİR']}
          rows={[
            ['4-4-2', '4-3-3, 5-3-2', '4-5-1, 3-5-2'],
            ['4-3-3', '3-5-2, 4-5-1', '4-4-2, 5-3-2'],
            ['3-5-2', '5-3-2, 4-4-2', '4-3-3, 4-5-1'],
            ['5-3-2', '4-5-1, 4-3-3', '3-5-2, 4-4-2'],
            ['4-5-1', '4-4-2, 3-5-2', '5-3-2, 4-3-3'],
          ]}
        />
      </Section>

      <Section id="stil" no="6.2" title="Oyun stili">
        <Prose>
          <p>Oyun stili iki takımın gol beklentisini (λ) çarpar. Hücum daha çok gol attırır ama daha çok gol yedirir. Savunma iki tarafın da golünü azaltır. Stil kazanma olasılığını değiştirir, maçtaki toplam gol sayısını da değiştirir.</p>
          <p>Yapay zekâ stili maçtan önce seçer. Kural güç farkına bağlıdır. Fark ev avantajını içerir: ev sahibine +5, deplasmana −5.</p>
        </Prose>
        <Figure
          no="6.3"
          height={300}
          minWidth={760}
          label="Oyun stili çarpanları: hücum kendi λ değerini 1,15, rakibinkini 1,10 ile çarpar; savunma ikisini de 0,85 ile çarpar. Yapay zekâ fark 12 ve üstünde hücuma, −12 ve altında savunmaya geçer."
          caption="Üst: stil çarpanları. Alt: yapay zekânın stil seçimi. Kullanıcının seçtiği stil yapay zekâ tarafından değiştirilmez."
        >
          {[
            ['Hücum', 1.15, 1.1, 'f-dan'],
            ['Dengeli', 1, 1, 'f-strong'],
            ['Savunma', 0.85, 0.85, 'f-away'],
          ].map(([label, own, opp, cls], i) => {
            const y = 30 + i * 46
            return (
              <g key={String(label)}>
                <Txt x={20} y={y + 22} cls="g-t">{String(label)}</Txt>
                <rect className={String(cls)} x={110} y={y + 4} width={Number(own) * 260} height={14} />
                <Txt x={116 + Number(own) * 260} y={y + 16} cls="g-m">{`kendi λ ×${String(own).replace('.', ',')}`}</Txt>
                <rect className={`${cls}`} opacity={0.45} x={110} y={y + 22} width={Number(opp) * 260} height={14} />
                <Txt x={116 + Number(opp) * 260} y={y + 34} cls="g-m">{`rakip λ ×${String(opp).replace('.', ',')}`}</Txt>
              </g>
            )
          })}
          <line className="g-ed dash" x1={370} y1={26} x2={370} y2={170} />
          <Txt x={374} y={22} cls="g-ms">×1</Txt>
          <Txt x={580} y={52} cls="g-s">Örnek: hücum ile savunma karşılaşır.</Txt>
          <Txt x={580} y={70} cls="g-s">Hücum yapan: λ × 1,15 × 0,85 = × 0,98</Txt>
          <Txt x={580} y={88} cls="g-s">Savunma yapan: λ × 0,85 × 1,10 = × 0,94</Txt>

          <line className="g-axis" x1={80} y1={240} x2={900} y2={240} />
          <rect className="f-away" opacity={0.25} x={80} y={222} width={(18 / 60) * 820} height={18} />
          <rect className="f-muted" x={80 + (18 / 60) * 820} y={222} width={(24 / 60) * 820 - 1} height={18} />
          <rect className="f-dan" opacity={0.25} x={80 + (42 / 60) * 820} y={222} width={(18 / 60) * 820} height={18} />
          {[-30, -20, -12, 0, 12, 20, 30].map((v) => {
            const x = 80 + ((v + 30) / 60) * 820
            return (
              <g key={v}>
                <line className="g-axis" x1={x} y1={240} x2={x} y2={246} />
                <Txt x={x} y={260} cls="g-ms" anchor="middle">{v > 0 ? `+${v}` : v < 0 ? `−${-v}` : '0'}</Txt>
              </g>
            )
          })}
          <Txt x={80 + (9 / 60) * 820} y={214} cls="g-s t-away" anchor="middle">savunma</Txt>
          <Txt x={490} y={214} cls="g-s" anchor="middle">takımın varsayılan stili</Txt>
          <Txt x={80 + (51 / 60) * 820} y={214} cls="g-s t-dan" anchor="middle">hücum</Txt>
          <Txt x={490} y={284} cls="g-s" anchor="middle">güç farkı = kendi maç gücü − rakibin maç gücü ± 5 (ev / deplasman)</Txt>
        </Figure>
      </Section>

      <Section id="hoca" no="6.3" title="Teknik direktör">
        <Prose>
          <p>Her takımın bir yapay zekâ teknik direktörü var. Hocanın taktik ustalığı 1–100 arasıdır. Ustalık etkin güce küçük bir bonus verir. Hocanın tercih ettiği diziliş takımın başlangıç dizilişi olur.</p>
          <ul>
            <li>Sistem hocaları açılışta atar. Yeni ve yükselen takımlar da oluşturulurken hoca alır.</li>
            <li>Normal takımın hocası 30–90, 4 büyüklerin hocası 60–95 ustalıktadır.</li>
            <li>Bonus formülü: (ustalık − 50) / 20. Örnek: 80 ustalık +1,5 güç verir.</li>
            <li>Menajer modunda kullanıcı takımı devralır ve hoca ayrılır. Kullanıcı takımı bırakınca takıma yeni bir yapay zekâ hocası gelir.</li>
          </ul>
        </Prose>
        <Figure
          no="6.4"
          height={210}
          minWidth={720}
          label="Hoca bonusu ustalıkla doğrusal artar: 30 ustalık −1, 50 ustalık 0, 90 ustalık +2, 95 ustalık +2,25."
          caption="Hoca ustalığından gelen güç bonusu. Gri bant normal takımların, yeşil bant 4 büyüklerin ustalık aralığıdır."
        >
          {[-1, 0, 1, 2].map((v) => {
            const y = 150 - v * 40
            return (
              <g key={v}>
                <line className={v === 0 ? 'g-zero' : 'g-grid'} x1={80} y1={y} x2={900} y2={y} />
                <Txt x={72} y={y + 4} cls="g-ms" anchor="end">{v > 0 ? `+${v}` : v < 0 ? `−${-v}` : '0'}</Txt>
              </g>
            )
          })}
          <rect className="f-muted" x={80 + 30 * 8.2} y={182} width={60 * 8.2} height={8} />
          <rect className="f-accs" x={80 + 60 * 8.2} y={194} width={35 * 8.2} height={8} />
          {[0, 30, 50, 60, 90, 100].map((v) => (
            <Txt key={v} x={80 + v * 8.2} y={176} cls="g-ms" anchor="middle">{v}</Txt>
          ))}
          <line className="g-ed acc" x1={80 + 30 * 8.2} y1={150 + 40} x2={80 + 95 * 8.2} y2={150 - 2.25 * 40} style={{ strokeWidth: 2.2 }} />
          <circle className="f-acc" cx={80 + 80 * 8.2} cy={150 - 1.5 * 40} r={5} />
          <Txt x={80 + 80 * 8.2 - 8} y={150 - 1.5 * 40 - 10} cls="g-s" anchor="end">ustalık 80 → +1,5</Txt>
          <Txt x={20} y={18} cls="g-s">güç bonusu</Txt>
        </Figure>
        <Note>
          <p>Takım sayfasındaki "Taktik" kartı hocayı, ustalığını, dizilişi ve varsayılan stili gösterir. Kullanıcı diziliş ve stili sezon ortasında da değiştirebilir: <code>PUT /api/teams/{'{id}'}/tactics</code>.</p>
        </Note>
      </Section>

      <Section id="hakem" no="6.4" title="Hakemler">
        <Prose>
          <p>Sistem açılışta 14 hakemlik bir havuz oluşturur. Her hakemin 1–10 ölçeğinde bir sertlik değeri var. Havuzdaki hakemler 2–9 arasında rastgele değer alır. Sertlik kart ve penaltı sayısını çarpar. Sistem her haftanın maçlarına farklı hakem atar. Kupa turlarına da hakem atanır.</p>
        </Prose>
        <Figure
          no="6.5"
          height={250}
          minWidth={720}
          label="Hakem sertliği 1'den 10'a çıktıkça kart çarpanı 0,68'den 1,40'a, penaltı çarpanı 0,76'dan 1,30'a çıkar."
          caption="Hakem sertliği ve çarpanlar. Kart çarpanı = 0,6 + 0,08 × sertlik. Penaltı çarpanı = 0,7 + 0,06 × sertlik. Ortalama hakem (5) kartta 1,0, penaltıda 1,0 verir."
        >
          {[0.6, 0.8, 1, 1.2, 1.4].map((v) => {
            const y = 200 - (v - 0.6) * 200
            return (
              <g key={v}>
                <line className={v === 1 ? 'g-zero' : 'g-grid'} x1={80} y1={y} x2={620} y2={y} />
                <Txt x={72} y={y + 4} cls="g-ms" anchor="end">{`×${v.toFixed(1).replace('.', ',')}`}</Txt>
              </g>
            )
          })}
          {Array.from({ length: 10 }, (_, i) => (
            <Txt key={i} x={80 + i * 60} y={222} cls="g-ms" anchor="middle">{i + 1}</Txt>
          ))}
          <Txt x={350} y={242} cls="g-s" anchor="middle">hakem sertliği</Txt>
          <polyline className="g-ed dan" style={{ strokeWidth: 2.2 }} points={`80,${200 - (0.68 - 0.6) * 200} 620,${200 - (1.4 - 0.6) * 200}`} />
          <polyline className="g-ed sig" style={{ strokeWidth: 2.2 }} points={`80,${200 - (0.76 - 0.6) * 200} 620,${200 - (1.3 - 0.6) * 200}`} />
          <Txt x={626} y={44} cls="g-s t-dan">kart ×1,40</Txt>
          <Txt x={626} y={64} cls="g-s t-sig">penaltı ×1,30</Txt>
          <Box x={720} y={30} w={220} h={170} title="Hakem atama" lines={['fikstür oluşunca her hafta', 'için havuz karıştırılır', 'bir hakem, bir haftada', 'bir maç yönetir', 'kupa turlarına da atanır', 'maç detayında hakem adı']} />
        </Figure>
        <Note>
          <p>Hakemler sayfası her hakemin sertliğini, maç sayısını, sarı ve kırmızı kartlarını, verdiği penaltıları ve maç başına kart sayısını gösterir. Sayfaya Sezonlar sayfasından ulaşılır.</p>
        </Note>
      </Section>
    </>
  )
}

export default Tactics
