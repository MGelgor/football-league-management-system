import { Box, Edge, Figure, GTable, Note, Prose, Section, Txt } from '../parts'

const SQUAD = [
  { pos: 'KL', total: 2, start: 1, cls: 'f-sigs' },
  { pos: 'DEF', total: 6, start: 4, cls: 'f-accs' },
  { pos: 'OS', total: 6, start: 4, cls: 'f-line' },
  { pos: 'FV', total: 4, start: 2, cls: 'f-sigs' },
]

const AGE_GROUPS: [string, number, number][] = [
  ['≤ 21', 2, 5],
  ['22–25', 0, 3],
  ['26–29', -1, 1],
  ['30–32', -3, 0],
  ['33 +', -5, -2],
]

function signed(value: number) {
  return value > 0 ? `+${value}` : value < 0 ? `−${-value}` : '0'
}

function formOf(rating: number) {
  return Math.min(1.1, Math.max(0.9, 1 + (rating - 6.5) * 0.08))
}

function fatigueOf(starts: number) {
  return Math.min(8, Math.max(0, starts - 3) * 2)
}

function Players() {
  return (
    <>
      <Section id="kadro" no="5.1" title="Kadro yapısı">
        <Prose>
          <p>Her takımın kadrosu en az 18 oyuncudur. Mevki şablonu 2 kaleci, 6 defans, 6 orta saha ve 4 forvettir. Kadro en fazla 30 oyuncu olur. Yapay zekâ kadrosunu 24 oyuncuda tutar.</p>
          <ul>
            <li>Kullanıcı takım eklerken oyuncu girebilir. Sistem eksik mevkileri rastgele oyuncularla doldurur.</li>
            <li>Rastgele oyuncunun yaşı 18–34 arasıdır. Çoğu oyuncu 26 yaş civarındadır. Gücü takım gücünün ±15 çevresindedir.</li>
            <li>Forma numarası takımda tekildir (1–99). Güç 1–100, yaş 16–45 aralığındadır.</li>
          </ul>
        </Prose>
        <Figure
          no="5.1"
          height={150}
          minWidth={720}
          label="Kadro şablonu 18 oyuncu: 2 kaleci, 6 defans, 6 orta saha, 4 forvet. 4-4-2 ilk 11: 1 kaleci, 4 defans, 4 orta saha, 2 forvet."
          caption="Kadro şablonu ve 4-4-2 dizilişinde ilk 11. Kesik alan yedeklerdir. Her mevkinin en az bir yedeği var. Başka dizilişte ilk 11 sayıları değişir (Modül 06)."
        >
          <Txt x={20} y={52} cls="g-ms">KADRO</Txt>
          <Txt x={20} y={112} cls="g-ms">İLK 11</Txt>
          <Txt x={925} y={20} cls="g-ms" anchor="end">18 OYUNCU</Txt>
          {SQUAD.map((group, i) => {
            const x = 100 + SQUAD.slice(0, i).reduce((sum, g) => sum + g.total, 0) * 46
            return (
              <g key={group.pos}>
                <rect className={`${group.cls} s-strong`} x={x} y={30} width={group.total * 46 - 3} height={36} rx={2} />
                <Txt x={x + 10} y={53} cls="g-t">{`${group.total} ${group.pos}`}</Txt>
                <rect className={`${group.cls} s-strong`} x={x} y={90} width={group.start * 46 - 3} height={36} rx={2} />
                <Txt x={x + 10} y={113} cls="g-t">{group.start}</Txt>
                <rect className="f-muted s-strong" x={x + group.start * 46} y={90} width={(group.total - group.start) * 46 - 3} height={36} rx={2} strokeDasharray="4 3" />
                <Txt x={x + group.start * 46 + 8} y={113} cls="g-s">{`${group.total - group.start} yedek`}</Txt>
              </g>
            )
          })}
        </Figure>
      </Section>

      <Section id="efektif" no="5.2" title="Form ve efektif güç">
        <Prose>
          <p>Oyuncunun maçtaki gücü efektif güçtür: <code>güç × form − yorgunluk</code>. İlk 11 seçimi ve gol dağılımı bu değeri kullanır. Bu nedenle formdaki oyuncu daha sık oynar ve daha çok gol atar. Yorgun oyuncu daha az oynar.</p>
          <ul>
            <li>Form, son 5 maçın reyting ortalamasından gelir. Formül: 1 + (ortalama − 6,5) × 0,08. Değer aralığı 0,9–1,1.</li>
            <li>Yorgunluk, ilk 11'de üst üste başlanan maç sayısından gelir. İlk 3 maç yorgunluk vermez. Sonraki her maç 2 ekler. Üst sınır 8'dir.</li>
            <li>Oyuncu yedek kalırsa veya oyuna sonradan girerse sayaç 0 olur.</li>
          </ul>
        </Prose>
        <Figure
          no="5.2"
          height={280}
          minWidth={760}
          label="Sol grafik: reyting ortalaması 5,25 ve altında form 0,9, 7,75 ve üstünde 1,1. Sağ grafik: üst üste 4. başlangıçta yorgunluk 2, 7. başlangıçta 8."
          caption="Form ve maçlar arası yorgunluk. Örnek: güç 80, son 5 maç ortalaması 7,1 (form 1,05) ve üst üste 5. maç (yorgunluk 4). Efektif güç: 80 × 1,05 − 4 = 80."
        >
          <Txt x={30} y={20} cls="g-t">Form çarpanı</Txt>
          {[0.9, 0.95, 1, 1.05, 1.1].map((f) => {
            const y = 220 - (f - 0.9) * 900
            return (
              <g key={f}>
                <line className={f === 1 ? 'g-zero' : 'g-grid'} x1={70} y1={y} x2={420} y2={y} />
                <Txt x={62} y={y + 4} cls="g-ms" anchor="end">{f.toFixed(2).replace('.', ',')}</Txt>
              </g>
            )
          })}
          {[4, 5, 6, 7, 8, 9].map((r) => (
            <Txt key={r} x={70 + (r - 4) * 70} y={240} cls="g-ms" anchor="middle">{r}</Txt>
          ))}
          <Txt x={245} y={262} cls="g-s" anchor="middle">son 5 maçın reyting ortalaması</Txt>
          <polyline
            className="g-ed acc"
            style={{ strokeWidth: 2.2 }}
            points={[4, 5.25, 7.75, 9].map((r) => `${70 + (r - 4) * 70},${220 - (formOf(r) - 0.9) * 900}`).join(' ')}
          />
          <circle className="f-acc" cx={70 + 3.1 * 70} cy={220 - (formOf(7.1) - 0.9) * 900} r={5} />
          <Txt x={70 + 3.1 * 70 + 8} y={220 - (formOf(7.1) - 0.9) * 900 + 16} cls="g-s">7,1 → 1,05</Txt>

          <Txt x={530} y={20} cls="g-t">Maçlar arası yorgunluk</Txt>
          {[0, 2, 4, 6, 8].map((v) => {
            const y = 220 - v * 22
            return (
              <g key={v}>
                <line className={v === 0 ? 'g-axis' : 'g-grid'} x1={560} y1={y} x2={930} y2={y} />
                <Txt x={552} y={y + 4} cls="g-ms" anchor="end">{v === 0 ? '0' : `−${v}`}</Txt>
              </g>
            )
          })}
          {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => {
            const v = fatigueOf(s)
            const x = 572 + (s - 1) * 44
            return (
              <g key={s}>
                {v > 0 ? <rect className={s === 5 ? 'f-sig' : 'f-dan'} x={x} y={220 - v * 22} width={30} height={v * 22} /> : <rect className="f-accs" x={x} y={216} width={30} height={4} />}
                <Txt x={x + 15} y={240} cls="g-ms" anchor="middle">{s}.</Txt>
              </g>
            )
          })}
          <Txt x={745} y={262} cls="g-s" anchor="middle">ilk 11'de üst üste başladığı maç</Txt>
        </Figure>
        <Note>
          <p>Kadroda ↗ ve ↘ işaretleri formu, 💤 işareti yorgunluğu gösterir. Maç içi yorgunluk ayrı bir kuraldır (Modül 04).</p>
        </Note>
      </Section>

      <Section id="durum" no="5.3" title="Oyuncu durumları">
        <Prose>
          <p>Bir oyuncu birkaç durumdan birindedir. Yalnız "Uygun" oyuncu maç kadrosuna girer. Ceza ve sakatlık sayaçları kaçırılan her maçta bir azalır.</p>
        </Prose>
        <Figure
          no="5.3"
          height={360}
          minWidth={820}
          label="Oyuncu durumları: uygun, cezalı, sakat, emekli, serbest ve altyapı. Kırmızı kart ve her dördüncü sarı ceza verir. Sözleşmesi biten oyuncu serbest kalır."
          caption="Oyuncu durumları. Kırmızı ok ceza, turuncu ok sakatlık yoludur. Yeşil oklar oyuncuyu kadroya geri getirir. Emeklilik kalıcıdır. Serbest oyuncu transfer penceresinde bir takıma imza atabilir."
        >
          <Txt x={480} y={20} cls="g-s" anchor="middle">Cezalı veya sakat oyuncu ilk 11'e ve yedeğe girmez.</Txt>
          <Box x={380} y={30} w={200} h={50} rx={24} tone="ghost" center title="Altyapı genci" lines={['16–18 yaş, karar bekler']} />
          <Edge from={[480, 80]} to={[480, 127]} tone="acc" />
          <Txt x={490} y={108} cls="g-s">A takıma al (menajer)</Txt>
          <Box x={20} y={130} w={200} h={70} rx={30} tone="dan" center title="Cezalı" lines={[{ text: 'suspended_matches > 0', mono: true }]} />
          <Box x={380} y={130} w={200} h={70} rx={30} tone="acc" strong center title="Uygun" lines={['kadroya girebilir']} />
          <Box x={740} y={130} w={200} h={70} rx={30} tone="sig" center title="Sakat" lines={[{ text: 'injured_matches > 0', mono: true }]} />
          <Edge from={[380, 152]} to={[223, 152]} tone="dan" />
          <Txt x={300} y={128} cls="g-s" anchor="middle">kırmızı kart → +1</Txt>
          <Txt x={300} y={143} cls="g-s" anchor="middle">her 4. sarı → +1</Txt>
          <Edge from={[220, 180]} to={[377, 180]} tone="acc" />
          <Txt x={300} y={198} cls="g-s" anchor="middle">kaçırdığı her maç: −1</Txt>
          <Edge from={[580, 152]} to={[737, 152]} tone="sig" />
          <Txt x={660} y={128} cls="g-s" anchor="middle">maçta sakatlanır</Txt>
          <Txt x={660} y={143} cls="g-s" anchor="middle">1–20 maç (Şekil 5.4)</Txt>
          <Edge from={[740, 180]} to={[583, 180]} tone="acc" />
          <Txt x={660} y={198} cls="g-s" anchor="middle">kaçırdığı her maç: −1</Txt>
          <Box x={250} y={280} w={200} h={56} rx={24} tone="ghost" center title="Emekli" lines={[{ text: 'active = false', mono: true }]} />
          <Box x={510} y={280} w={200} h={56} rx={24} tone="away" center title="Serbest" lines={['takımsız, kayıtları kalır']} />
          <Edge from={[440, 200]} to={[370, 277]} dash />
          <Txt x={392} y={238} cls="g-s" anchor="end">sezon sonu, 35+ yaş</Txt>
          <Edge from={[510, 200]} to={[590, 277]} dash />
          <Txt x={562} y={238} cls="g-s">sözleşme biter</Txt>
          <Edge d="M710,308 H800 V203" tone="away" />
          <Txt x={808} y={262} cls="g-s">imza veya transfer</Txt>
          <Txt x={20} y={300} cls="g-s">Sezon başında sıfırlanır:</Txt>
          <Txt x={20} y={317} cls="g-ms">ceza = 0 · sarı = 0</Txt>
        </Figure>
        <Note>
          <p>Sarı kart sayısı sezon boyunca birikir. 4, 8 ve 12. sarı kartta oyuncu 1 maç ceza alır. Aynı maçta 2 sarı alan oyuncu kırmızı kart görür.</p>
        </Note>
      </Section>

      <Section id="sakatlik" no="5.4" title="Sakatlık türleri">
        <Prose>
          <p>Sakatlığın süresi rastgeledir. Üç tür var. Uzun sakatlık bazen kalıcı güç kaybı bırakır. Oyuncu sayfası sakatlık geçmişini gösterir. Takım sayfasındaki "Revir ve cezalılar" kartı oyuncunun ne zaman döneceğini gösterir.</p>
        </Prose>
        <Figure
          no="5.4"
          height={150}
          minWidth={720}
          label="Sakatlık türleri: yüzde 70 hafif 1-2 maç, yüzde 22 orta 3-6 maç, yüzde 8 uzun 8-20 maç."
          caption="Sakatlık türlerinin oranı. Uzun sakatlıkta %30 olasılıkla oyuncu kalıcı olarak 1–3 güç kaybeder. Yaz arasında sakatlıklar 4 maç iyileşir. Uzun sakatlık yeni sezona taşınabilir."
        >
          {(
            [
              ['Hafif', '1–2 maç', 70, 0, 'f-accs'],
              ['Orta', '3–6 maç', 22, 70, 'f-sigs'],
              ['Uzun', '8–20 maç', 8, 92, 'f-dans'],
            ] as [string, string, number, number, string][]
          ).map(([title, range, share, offset, cls]) => {
            const x = 40 + offset * 8.8
            return (
              <g key={title}>
                <rect className={`${cls} s-strong`} x={x} y={40} width={share * 8.8 - 2} height={56} />
                <Txt x={x + 10} y={62} cls="g-t">{`${title} %${share}`}</Txt>
                <Txt x={x + 10} y={82} cls="g-s">{range}</Txt>
              </g>
            )
          })}
          <Txt x={40} y={124} cls="g-s">Olasılık: maç başına sahadaki her oyuncu için %0,8.</Txt>
        </Figure>
      </Section>

      <Section id="gelisim" no="5.5" title="Yaş, gelişim ve emeklilik">
        <Prose>
          <p>Lig bitince sistem her oyuncu için şu adımları sırayla uygular:</p>
          <ol>
            <li>Sistem güç değişimini oyuncunun sezondaki yaşına göre seçer.</li>
            <li>Sistem sezon reytingine göre ±1 ekler.</li>
            <li>Oyuncu bir yaş büyür.</li>
            <li>Sistem yeni yaşa göre emeklilik kontrolü yapar.</li>
          </ol>
        </Prose>
        <Figure
          no="5.5"
          height={300}
          minWidth={720}
          label="Yaşa göre sezon sonu güç değişimi: 21 ve altı +2 ile +5, 22-25 0 ile +3, 26-29 -1 ile +1, 30-32 -3 ile 0, 33 ve üstü -5 ile -2."
          caption="Yaşa göre güç değişimi. Dolu çubuk yaş aralığıdır. Kesik kutu reyting etkisini de içerir: sezon ortalaması 7,2 ve üstü +1, 5,8 ve altı −1. Hiç oynamayan oyuncu reyting etkisi almaz."
        >
          <Txt x={20} y={14} cls="g-s">sezon sonu güç değişimi</Txt>
          {[-6, -4, -2, 0, 2, 4, 6].map((v) => (
            <g key={v}>
              <line className={v === 0 ? 'g-zero' : 'g-grid'} x1={80} y1={150 - v * 20} x2={920} y2={150 - v * 20} />
              <Txt x={72} y={154 - v * 20} cls="g-ms" anchor="end">{signed(v)}</Txt>
            </g>
          ))}
          {AGE_GROUPS.map(([label, low, high], i) => {
            const cx = 180 + i * 160
            const cls = low >= 0 && high > 0 ? 'f-acc' : high <= 0 && low < 0 ? 'f-dan' : 'f-strong'
            return (
              <g key={label}>
                <rect className="f-muted s-strong" x={cx - 34} y={150 - (high + 1) * 20} width={68} height={(high - low + 2) * 20} strokeDasharray="4 3" />
                <rect className={cls} x={cx - 24} y={150 - high * 20} width={48} height={(high - low) * 20} />
                <Txt x={cx} y={150 - (high + 1) * 20 - 7} cls="g-m g-b" anchor="middle">{`${signed(low)} … ${signed(high)}`}</Txt>
                <Txt x={cx} y={292} anchor="middle">{`${label} yaş`}</Txt>
              </g>
            )
          })}
        </Figure>
        <Prose>
          <p>35 yaşından sonra oyuncu emekli olabilir. Olasılık her yaşta artar. Emekli oyuncunun kayıtları kalır. Sistem yerine otomatik oyuncu eklemez. Takım boşluğu transfer penceresinde doldurur: altyapı gençleri, serbest oyuncular ve transferler. Şablon eksik kalırsa sistem altyapıdan genç ekler.</p>
        </Prose>
        <Figure
          no="5.6"
          height={230}
          minWidth={720}
          label="Emeklilik olasılığı: 34 yaş yüzde 0, 35 yüzde 25, 36 yüzde 50, 37 yüzde 80, 38 ve üstü yüzde 100. Boşluk transfer penceresinde dolar."
          caption="Emeklilik olasılığı ve boşluğun dolması. Olasılık oyuncunun yeni yaşına göre hesaplanır. Ayrıntı: Modül 07."
        >
          {[0, 25, 50, 75, 100].map((v) => (
            <g key={v}>
              <line className={v === 0 ? 'g-axis' : 'g-grid'} x1={70} y1={190 - v * 1.5} x2={580} y2={190 - v * 1.5} />
              <Txt x={62} y={194 - v * 1.5} cls="g-ms" anchor="end">{`%${v}`}</Txt>
            </g>
          ))}
          {[
            ['34', 0],
            ['35', 25],
            ['36', 50],
            ['37', 80],
            ['38 +', 100],
          ].map(([age, value], i) => {
            const x = 90 + i * 100
            const h = Number(value) * 1.5
            return (
              <g key={String(age)}>
                {h > 0 && <rect className={value === 100 ? 'f-dan' : 'f-sig'} x={x} y={190 - h} width={60} height={h} />}
                <Txt x={x + 30} y={184 - h} cls="g-m g-b" anchor="middle">{`%${value}`}</Txt>
                <Txt x={x + 30} y={210} anchor="middle">{`${age} yaş`}</Txt>
              </g>
            )
          })}
          <Box x={640} y={20} w={300} h={50} tone="dan" title="Oyuncu emekli olur" lines={['active = false · istatistikleri kalır']} />
          <Edge from={[790, 70]} to={[790, 97]} />
          <Box x={640} y={100} w={300} h={106} tone="acc" title="Transfer penceresi doldurur" lines={['altyapıdan 1–2 genç (16–18 yaş)', 'serbest oyuncu imzası', 'transfer', 'şablon eksikse: altyapıdan genç']} />
        </Figure>
      </Section>

      <Section id="istatistik" no="5.6" title="İstatistiklerin kaynağı">
        <Prose>
          <p>Oyuncu istatistikleri ayrı bir tabloda saklanmaz. Sistem iki tabloyu sayar. Sezon istatistiği yalnız lig maçlarını sayar. Kariyer istatistiği lig ve kupa maçlarını sayar. Gol krallığı kendi kalesine golleri saymaz.</p>
        </Prose>
        <Figure
          no="5.7"
          height={230}
          minWidth={820}
          label="match_events ve match_appearances tabloları turnuva filtresinden geçer. Yalnız lig maçları sezon istatistiğini, lig ve kupa maçları kariyer istatistiğini verir."
          caption="İstatistiklerin kaynağı. Aynı iki tablo iki farklı toplam verir. Fark yalnız turnuva filtresidir."
        >
          <Box x={20} y={30} w={250} h={70} tone="sig" lines={[{ text: 'match_events', mono: true, bold: true }, 'gol, asist, kart, sakatlık,', 'penaltı, kendi kalesine']} />
          <Box x={20} y={130} w={250} h={70} tone="sig" lines={[{ text: 'match_appearances', mono: true, bold: true }, 'maç, dakika, reyting,', 'maçın oyuncusu']} />
          <Box x={350} y={70} w={220} h={90} title="Turnuva filtresi" lines={['haftanın competition alanı', { text: 'LEAGUE / CUP', mono: true }]} />
          <Edge from={[270, 65]} to={[347, 100]} />
          <Edge from={[270, 165]} to={[347, 130]} />
          <Box x={650} y={30} w={290} h={70} tone="acc" title="Sezon istatistiği" lines={['Puan Durumu → Oyuncular sekmesi']} />
          <Box x={650} y={130} w={290} h={70} tone="acc" title="Kariyer istatistiği" lines={['oyuncu ve takım sayfası']} />
          <Edge from={[570, 100]} to={[647, 65]} tone="acc" />
          <Txt x={608} y={72} cls="g-m" anchor="middle">LEAGUE</Txt>
          <Edge from={[570, 130]} to={[647, 165]} tone="acc" />
          <Txt x={608} y={168} cls="g-m" anchor="middle">+ CUP</Txt>
        </Figure>
        <GTable
          head={['SAYFA', 'GÖSTERİLEN']}
          rows={[
            ['Oyuncu sayfası', 'Profil, sezon ve turnuva satırları, goller ve asistler, sakatlık geçmişi, transfer geçmişi.'],
            ['Takım sayfası', 'Kadro (form, yorgunluk, durum), muhtemel ilk 11, revir, sözleşmeler.'],
            ['Puan Durumu → Oyuncular', 'Sezonun gol, asist, kart tablosu. Sütuna tıklayınca sıralanır.'],
          ]}
        />
      </Section>
    </>
  )
}

export default Players
