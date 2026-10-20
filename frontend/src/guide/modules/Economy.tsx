import { Box, Edge, Figure, GTable, Note, Prose, Section, Txt } from '../parts'

function value(strength: number, ageFactor: number) {
  return Math.exp((strength - 60) / 12) * ageFactor
}

function valueX(strength: number) {
  return 80 + (strength - 50) * 18
}

function valueY(millions: number) {
  return 240 - millions * 9
}

function prize(rank: number) {
  return 2 + (18 * (18 - rank)) / 17
}

function Economy() {
  return (
    <>
      <Section id="deger" no="7.1" title="Piyasa değeri ve maaş">
        <Prose>
          <p>Her oyuncunun bir piyasa değeri var. Değer avrodur. Değer güçle üstel olarak büyür: her 12 güç değeri yaklaşık 2,7 katına çıkarır. Genç oyuncu daha değerlidir. Forvet en değerli mevkidir.</p>
          <ul>
            <li>Formül: 1M × e^((güç − 60) / 12) × yaş çarpanı × mevki çarpanı. Değer 10 000'e yuvarlanır.</li>
            <li>Haftalık maaş değerin binde 4'üdür. En az €500'dür.</li>
            <li>Serbest oyuncu imza için %15 daha yüksek maaş ister.</li>
          </ul>
        </Prose>
        <Figure
          no="7.1"
          height={290}
          minWidth={760}
          label="Piyasa değeri güçle üstel artar. Güç 60 olan 26-29 yaşındaki oyuncu 1 milyon, güç 84 olan 7,4 milyon, güç 90 olan 12,2 milyon avro eder. 21 yaş altı 1,3 kat, 33 yaş üstü 0,4 kat."
          caption="Piyasa değeri ve güç (mevki çarpanı 1,0 için). Üç çizgi üç yaş grubudur. Değer aynı güçte yaşla çok değişir: 21 yaş altı ×1,3, 33 yaş üstü ×0,4."
        >
          {[0, 5, 10, 15, 20, 25].map((m) => (
            <g key={m}>
              <line className={m === 0 ? 'g-axis' : 'g-grid'} x1={80} y1={valueY(m)} x2={890} y2={valueY(m)} />
              <Txt x={72} y={valueY(m) + 4} cls="g-ms" anchor="end">{`€${m}M`}</Txt>
            </g>
          ))}
          {[50, 60, 70, 80, 90, 95].map((s) => (
            <Txt key={s} x={valueX(s)} y={258} cls="g-ms" anchor="middle">{s}</Txt>
          ))}
          <Txt x={485} y={280} cls="g-s" anchor="middle">oyuncu gücü</Txt>
          {[
            [1.3, 'acc', '≤ 21 yaş ×1,3'],
            [1, '', '26–29 yaş ×1,0'],
            [0.4, 'dan', '33+ yaş ×0,4'],
          ].map(([factor, tone, label]) => {
            const points = Array.from({ length: 46 }, (_, i) => 50 + i)
              .map((s) => `${valueX(s)},${Math.max(14, valueY(value(s, Number(factor))))}`)
              .join(' ')
            const end = Math.min(95, 60 + 12 * Math.log(25 / Number(factor)))
            return (
              <g key={String(label)}>
                <polyline className={`g-ed ${tone}`} style={{ strokeWidth: 2.2 }} points={points} />
                <Txt x={valueX(end) - 6} y={Math.max(24, valueY(value(end, Number(factor)))) + (tone === 'acc' ? -6 : 16)} cls={tone ? `g-s t-${tone}` : 'g-s'} anchor="end">
                  {String(label)}
                </Txt>
              </g>
            )
          })}
          {[
            [60, 1],
            [72, 2.72],
            [84, 7.39],
          ].map(([s, m]) => (
            <g key={s}>
              <circle className="f-surf s-strong" cx={valueX(s)} cy={valueY(m)} r={4} strokeWidth={1.5} />
              <Txt x={valueX(s) + 8} y={valueY(m) - 6} cls="g-ms">{`€${String(m).replace('.', ',')}M`}</Txt>
            </g>
          ))}
        </Figure>
        <GTable
          head={['ÇARPAN', 'DEĞERLER']}
          rows={[
            ['Yaş', '≤ 21: ×1,3 · 22–25: ×1,2 · 26–29: ×1,0 · 30–32: ×0,7 · 33+: ×0,4'],
            ['Mevki', 'forvet ×1,2 · orta saha ×1,1 · defans ×0,9 · kaleci ×0,7'],
          ]}
        />
      </Section>

      <Section id="butce" no="7.2" title="Gelir ve gider">
        <Prose>
          <p>Her para hareketi bir finans kaydıdır (<code>FinanceEntry</code>). Kayıt takımın bütçesini aynı anda değiştirir. Bütçe eksiye düşebilir. Menajer modunda eksi bütçe yönetim kurulunun güvenini düşürür.</p>
        </Prose>
        <Figure
          no="7.2"
          height={330}
          minWidth={820}
          label="Bütçeye giren para: bilet geliri, lig ödülü, kupa ödülü, transfer satışı. Bütçeden çıkan para: maaşlar ve transfer alımı."
          caption="Gelir ve gider akışı. Sol taraf gelir, sağ taraf giderdir. Başlangıç bütçesi 20 iç saha maçının bilet geliridir. Örnek: güç 60 takım €8M, güç 90 olan 4 büyük €76,8M ile başlar."
        >
          {[
            ['Bilet geliri', 'her iç saha maçında (lig ve kupa)', '€400B × e^((güç − 60) / 15) · 4 büyük ×1,3'],
            ['Lig ödülü', 'sezon sonunda', '1. €20M … 18. €2M (doğrusal)'],
            ['Kupa ödülü', 'elenilen turda', 'ÇF €0,75M · YF €1,5M · F €3M · şampiyon €6M'],
            ['Transfer satışı', 'oyuncu satılınca', 'bedel'],
          ].map(([title, when, formula], i) => {
            const y = 14 + i * 76
            return (
              <g key={title}>
                <Box x={20} y={y} w={300} h={64} tone="acc" title={title} lines={[when, { text: formula, mono: true }]} />
                <Edge from={[320, y + 32]} to={[407, 165]} tone="acc" />
              </g>
            )
          })}
          <Box x={410} y={120} w={150} h={90} tone="sig" strong center title="Bütçe" lines={[{ text: 'teams.budget', mono: true }, 'finans kayıtlarının', 'toplamı']} />
          {[
            ['Maaşlar', 'her lig haftasında', 'kadronun haftalık maaş toplamı'],
            ['Transfer alımı', 'oyuncu alınınca', 'bedel (serbest oyuncu bedelsiz)'],
          ].map(([title, when, formula], i) => {
            const y = 70 + i * 120
            return (
              <g key={title}>
                <Edge from={[560, 165]} to={[637, y + 32]} tone="dan" />
                <Box x={640} y={y} w={300} h={64} tone="dan" title={title} lines={[when, formula]} />
              </g>
            )
          })}
        </Figure>
        <Figure
          no="7.3"
          height={220}
          minWidth={720}
          label="Lig ödülü sıraya göre doğrusal azalır: 1. sıra 20 milyon, 18. sıra 2 milyon avro."
          caption="Sezon sonu lig ödülü. Her sıra bir öncekinden yaklaşık €1,06M az alır."
        >
          {[0, 5, 10, 15, 20].map((m) => (
            <g key={m}>
              <line className={m === 0 ? 'g-axis' : 'g-grid'} x1={70} y1={180 - m * 7.5} x2={930} y2={180 - m * 7.5} />
              <Txt x={62} y={184 - m * 7.5} cls="g-ms" anchor="end">{`€${m}M`}</Txt>
            </g>
          ))}
          {Array.from({ length: 18 }, (_, i) => {
            const rank = i + 1
            const m = prize(rank)
            const x = 80 + i * 47
            return (
              <g key={rank}>
                <rect className={rank === 1 ? 'f-acc' : 'f-strong'} x={x} y={180 - m * 7.5} width={32} height={m * 7.5} />
                <Txt x={x + 16} y={198} cls="g-ms" anchor="middle">{rank}</Txt>
                {(rank === 1 || rank === 9 || rank === 18) && (
                  <Txt x={x + 16} y={174 - m * 7.5} cls="g-ms" anchor="middle">{`€${m.toFixed(1).replace('.', ',')}M`}</Txt>
                )}
              </g>
            )
          })}
          <Txt x={20} y={198} cls="g-s">sıra</Txt>
        </Figure>
        <Note>
          <p>Takım sayfasındaki "Finans" kartı bütçeyi, sezon gelir ve giderini, haftalık maaşı, kadro değerini, sözleşmeleri ve son kayıtları gösterir. Sezon sıfırlanırsa o sezonun kayıtları geri alınır.</p>
        </Note>
      </Section>

      <Section id="sozlesme" no="7.3" title="Sözleşmeler">
        <Prose>
          <p>Her oyuncunun bir sözleşme bitiş sezonu var. Sözleşme o sezonun sonunda biter. Sezon sonunda sistem biten sözleşmeleri işler. Oyuncu ya kalır ya serbest kalır.</p>
        </Prose>
        <Figure
          no="7.4"
          height={250}
          minWidth={820}
          label="Sözleşme bitişi: yönetilen takımda menajer karar verir. Yapay zekâ takımında 31 yaş ve altı, takım ortalamasının en fazla 10 altındaki oyuncu yüzde 85 olasılıkla uzatılır. Diğerleri serbest kalır."
          caption="Sözleşme bitişi kararı. Uzatılan oyuncu güncel değerine göre yeni maaş ve 1–4 sezonluk sözleşme alır. Serbest kalan oyuncunun kayıtları ve geçmişi kalır."
        >
          <Box x={20} y={90} w={170} h={70} tone="sig" center title="Sözleşme bitti" lines={['sezon sonu, adım 7']} />
          <Edge from={[190, 125]} to={[237, 125]} />
          <Box x={240} y={90} w={170} h={70} center title="Menajerin takımı mı?" lines={['inboxService.isManaged']} />
          <Edge d="M325,90 V50 H467" tone="acc" />
          <Txt x={335} y={70} cls="g-s">evet</Txt>
          <Box x={470} y={20} w={220} h={60} tone="ghost" title="Menajer uzattı mı?" lines={['Sözleşmeler sekmesi (Modül 08)']} />
          <Edge from={[325, 160]} to={[325, 197]} />
          <Txt x={335} y={182} cls="g-s">hayır</Txt>
          <Box x={240} y={200} w={450} h={44} lines={['≤ 31 yaş ve güç ≥ takım ortalaması − 10 ve %85 olasılık']} />
          <Edge from={[690, 50]} to={[767, 50]} tone="acc" />
          <Edge from={[690, 222]} to={[767, 70]} tone="acc" />
          <Box x={770} y={20} w={170} h={60} tone="acc" center title="Uzatılır" lines={['yeni maaş, 1–4 sezon']} />
          <Edge d="M580,80 V130 H767" tone="dan" dash />
          <Edge d="M690,236 H730 V150 H767" tone="dan" dash />
          <Box x={770} y={110} w={170} h={60} tone="dan" center title="Serbest kalır" lines={['takım = boş, forma 0']} />
          <Txt x={700} y={124} cls="g-s t-dan" anchor="end">hayır</Txt>
        </Figure>
      </Section>

      <Section id="pencere" no="7.4" title="Transfer penceresi">
        <Prose>
          <p>Transfer penceresi lig bitince açılır. Yeni sezonun fikstürü oluşturulunca kapanır. Pencere dışında yapılan teklif <code>409</code> yanıtı alır. Kupa pencere açıkken oynanır.</p>
        </Prose>
        <Figure
          no="7.5"
          height={250}
          minWidth={820}
          label="Transfer penceresi zaman çizelgesi: açılışta serbest oyuncu havuzu yenilenir, altyapı gençleri gelir, yapay zekâ ilk turu yapar. Kapanışta yapay zekâ ikinci turu yapar ve kadrolar tamamlanır."
          caption="Transfer penceresinin zaman çizelgesi. Pencere açıkken kullanıcı teklif yapar ve serbest oyuncu imzalar. Yapay zekâ iki turda alım yapar: açılışta ve kapanışta."
        >
          <line className="g-axis" x1={40} y1={130} x2={930} y2={130} />
          <rect className="f-accs" x={210} y={118} width={540} height={24} />
          <Txt x={480} y={135} cls="g-ms" anchor="middle">PENCERE AÇIK · kullanıcı teklifleri · kupa</Txt>
          <circle className="f-sig" cx={210} cy={130} r={8} />
          <circle className="f-sig" cx={750} cy={130} r={8} />
          <Txt x={210} y={166} cls="g-t" anchor="middle">Lig biter</Txt>
          <Txt x={210} y={182} cls="g-s" anchor="middle">SeasonEndService, adım 8</Txt>
          <Txt x={750} y={166} cls="g-t" anchor="middle">Fikstür oluşturulur</Txt>
          <Txt x={750} y={182} cls="g-s" anchor="middle">yeni sezon başlar</Txt>
          <Box x={20} y={14} w={380} h={90} tone="acc" tag="AÇILIŞ · openWindow" lines={['1. serbest oyuncular yaşlanır, bazıları emekli olur', '2. havuza 12 yeni serbest oyuncu (en fazla 60)', '3. her takıma altyapıdan 1–2 genç (16–18 yaş)', '4. yapay zekâ 1. tur · 5. eksik mevkiler tamamlanır']} />
          <Box x={560} y={14} w={380} h={74} tone="sig" tag="KAPANIŞ · closeWindow" lines={['1. yapay zekâ 2. tur', '2. mevki şablonu eksik kalan takıma altyapıdan genç']} />
          <Edge from={[210, 104]} to={[210, 120]} />
          <Edge from={[750, 88]} to={[750, 120]} />
          <Txt x={40} y={220} cls="g-s">Menajerin takımında altyapı gençleri kadroya girmeden karar bekler: "A takıma al" veya "Bırak" (Modül 08).</Txt>
          <Txt x={40} y={238} cls="g-s">Piyasa yalnız satılabilir oyuncuları listeler: satılırsa kadrosu 18'in veya şablonun altına düşecek oyuncu görünmez.</Txt>
        </Figure>
      </Section>

      <Section id="teklif" no="7.5" title="Teklif yapın">
        <Prose>
          <p>Satıcı takım bir "istenen bedel" belirler. İstenen bedel değerin 1,1 katıdır. Oyuncu takımın en güçlü 3 oyuncusundan biriyse istenen bedel değerin 1,4 katıdır. Teklifin cevabı bedelin istenen bedele oranına bağlıdır.</p>
        </Prose>
        <Figure
          no="7.6"
          height={200}
          minWidth={760}
          label="Teklif oranı yüzde 85'in altındaysa ret, yüzde 85 ile 100 arasında karşı teklif, yüzde 100 ve üstünde kabul."
          caption="Teklifin cevabı. Oran = teklif edilen bedel / istenen bedel. Karşı teklifte ekran istenen bedeli ödeme düğmesi gösterir."
        >
          {[
            [40, 85, 'f-dans', 'Ret', 't-dan'],
            [85, 100, 'f-sigs', 'Karşı teklif', 't-sig'],
            [100, 130, 'f-accs', 'Kabul · transfer hemen olur', 't-acc'],
          ].map(([from, to, cls, label, tone]) => {
            const x1 = 60 + (Number(from) - 40) * 9.3
            const x2 = 60 + (Number(to) - 40) * 9.3
            return (
              <g key={String(label)}>
                <rect className={String(cls)} x={x1} y={60} width={x2 - x1} height={44} />
                <Txt x={(x1 + x2) / 2} y={87} cls={`g-t ${tone}`} anchor="middle">{String(label)}</Txt>
              </g>
            )
          })}
          <line className="g-axis" x1={60} y1={104} x2={897} y2={104} />
          {[40, 60, 85, 100, 115, 130].map((p) => (
            <g key={p}>
              <line className="g-axis" x1={60 + (p - 40) * 9.3} y1={104} x2={60 + (p - 40) * 9.3} y2={110} />
              <Txt x={60 + (p - 40) * 9.3} y={124} cls="g-ms" anchor="middle">{`%${p}`}</Txt>
            </g>
          ))}
          <Txt x={60} y={30} cls="g-s">Örnek: değer €5M, istenen €5,5M. Teklif €4,5M (%82) ret, €5M (%91) karşı teklif, €5,5M kabul.</Txt>
          <Txt x={60} y={160} cls="g-s">Her zaman ret: satıcının kadrosu 18'in veya mevki şablonunun altına düşer.</Txt>
          <Txt x={60} y={178} cls="g-s">400 hatası: teklif alıcının bütçesini aşar.</Txt>
        </Figure>
        <GTable
          head={['İŞLEM', 'SONUÇ']}
          rows={[
            ['Transfer olur', 'Oyuncu yeni forma numarası ve 2–4 sezonluk sözleşme alır. Alıcıya gider, satıcıya gelir kaydı yazılır.'],
            ['Takım gücü', 'İki takımın gücü, ilk 11 ortalamasındaki değişim kadar değişir. 4 büyükler 85–100 bandında kalır.'],
            ['Serbest oyuncu imzası', 'Bedel yok. Maaş değere göre +%15. API: POST /api/transfers/free-agents/{id}/sign.'],
            ['Menajer modu', 'Alıcı her zaman yönetilen takımdır. Başka takım adına teklif 400 alır.'],
          ]}
        />
      </Section>

      <Section id="yapay" no="7.6" title="Yapay zekâ transferleri">
        <Prose>
          <p>Yapay zekâ her turda takımları rastgele sırayla işler. Her takım %70 olasılıkla alım yapar. Bir takım bir turda en fazla 2 oyuncu alır. Aynı pencerede transfer olan oyuncu tekrar alınmaz.</p>
        </Prose>
        <Figure
          no="7.7"
          height={260}
          minWidth={820}
          label="Yapay zekâ transfer akışı: ihtiyacı bul, adayı bul, satın al, kadro 24'ü aşarsa en zayıfları bırak."
          caption="Yapay zekânın bir takım için alım akışı. Serbest oyuncular bedelsizdir. Yapay zekâ menajerin oyuncusunu doğrudan alamaz; menajerin gelen kutusuna teklif gönderir."
        >
          <Box x={20} y={40} w={200} h={120} tag="1 · İHTİYAÇ" title="Hangi mevki?" lines={['önce şablon eksiği', 'yoksa takım gücünün', '8+ altındaki en zayıf', 'ilk 11 mevkisi']} />
          <Edge from={[220, 100]} to={[247, 100]} />
          <Box x={250} y={40} w={220} h={120} tag="2 · ADAY" title="En güçlü uygun oyuncu" lines={['en az 3 güç daha iyi', 'satılabilir', 'bedel ≤ bütçenin %40\'ı', 'serbest oyuncu bedelsiz']} />
          <Edge from={[470, 100]} to={[497, 100]} />
          <Box x={500} y={40} w={200} h={120} tone="acc" tag="3 · ALIM" title="Transfer" lines={['istenen bedel ödenir', 'yeni sözleşme', 'güçler güncellenir']} />
          <Edge from={[700, 100]} to={[727, 100]} />
          <Box x={730} y={40} w={210} h={120} tone="sig" tag="4 · KADRO" title="24'ü aşıyor mu?" lines={['en zayıf (eşitlikte en', 'yaşlı) oyuncular, şablonu', 'bozmadan serbest kalır']} />
          <Edge d="M600,160 V200 H120 V163" dash />
          <Txt x={360} y={218} cls="g-s" anchor="middle">en fazla 2 alım; ihtiyaç kalmazsa durur</Txt>
        </Figure>
        <Note>
          <p>Bir sezon sonunda yaklaşık 20 transfer olur. Testler her yeni sezonda şunu kontrol eder: tüm kadrolar 18–30 oyuncu ve şablonda, forma numaraları tekil, 4 büyükler güç 85 ve üstünde, bütçeler 0 ve üstünde.</p>
        </Note>
      </Section>
    </>
  )
}

export default Economy
