import { Box, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

const CONFIDENCE = [
  ['Lig galibiyeti', 2],
  ['Sürpriz galibiyet (olasılık < %35)', 5],
  ['Lig mağlubiyeti', -2],
  ['Sezon sonu: hedefin 1 sıra üstü', 5],
  ['Sezon sonu: hedefin 1 sıra altı', -5],
  ['Şampiyonluk', 15],
  ['Bütçe eksiye düştü', -15],
  ['Kupa hedefi tuttu', 5],
  ['Kupa hedefi tutmadı', -10],
] as const

function ManagerMode() {
  return (
    <>
      <Section id="dongu" no="8.1" title="Modun yaşam döngüsü">
        <Prose>
          <p>Sistem tek kullanıcılıdır. Hesap ve şifre yoktur. Menajerin adı ve takımı <code>manager_profile</code> tablosunda tek satırdır. Mod kapalıysa sistem tamamen otomatik çalışır.</p>
        </Prose>
        <Figure
          no="8.1"
          height={300}
          minWidth={820}
          label="Menajer modu durumları: mod kapalı, takımı yönetiyor, işsiz. Takımı devralınca mod açılır, bırakınca kapanır. Güven biterse veya takım düşerse menajer kovulur ve işsiz kalır; iş teklifini kabul edince yeni takımı yönetir."
          caption="Menajer modunun durumları. Yeşil oklar kullanıcının kararlarıdır. Kırmızı ok sistemin kararıdır. Her takım dönemi kariyer tablosuna bir satır yazar (manager_spell)."
        >
          <Box x={20} y={110} w={190} h={70} rx={30} tone="ghost" center title="Mod kapalı" lines={['her takım yapay zekâ hocalı']} />
          <Box x={385} y={110} w={210} h={70} rx={30} tone="acc" strong center title="Takımı yönetiyor" lines={['kadro, maç, transfer kararları']} />
          <Box x={760} y={110} w={180} h={70} rx={30} tone="dan" center title="İşsiz" lines={['iş teklifleri bekler']} />
          <Edge from={[210, 132]} to={[382, 132]} tone="acc" />
          <Txt x={296} y={110} cls="g-s" anchor="middle">takımı devral</Txt>
          <Txt x={296} y={124} cls="g-ms" anchor="middle">POST /my-team</Txt>
          <Edge from={[385, 160]} to={[213, 160]} tone="acc" dash />
          <Txt x={296} y={178} cls="g-s" anchor="middle">takımı bırak (DELETE)</Txt>
          <Edge from={[595, 132]} to={[757, 132]} tone="dan" />
          <Txt x={676} y={110} cls="g-s t-dan" anchor="middle">güven 0 veya küme düştü</Txt>
          <Edge from={[760, 160]} to={[598, 160]} tone="acc" />
          <Txt x={676} y={178} cls="g-s" anchor="middle">iş teklifini kabul et</Txt>
          <Edge d="M850,180 V250 H120 V183" tone="acc" dash />
          <Txt x={485} y={244} cls="g-s" anchor="middle">"başka bir takımla yeniden başla"</Txt>
          <Box x={385} y={14} w={210} h={60} tone="ghost" center lines={['devralınca yapay zekâ hocası ayrılır', 'bırakınca takıma yeni hoca gelir']} />
          <Txt x={20} y={282} cls="g-s">İş teklifleri: kovulan takımdan en fazla 5 güç üstteki ya da ligin en zayıf takımlarından 3 teklif. O sezon küme düşen takımlar teklif yapmaz.</Txt>
        </Figure>
      </Section>

      <Section id="kadro" no="8.2" title="Kadro seçimi">
        <Prose>
          <p>Menajerin takımının o hafta maçı varsa hafta kadroyu bekler. Kadro seçilmeden hafta oynatılırsa yanıt <code>409</code> olur. Kullanıcı kadroyu "Takımım" sayfasında seçer veya seçimi yapay zekâya bırakır (<code>?auto=true</code>).</p>
          <ul>
            <li>Kadro: ilk 11, diziliş, oyun stili, kaptan ve penaltıcı.</li>
            <li>Kural: 11 farklı ve uygun oyuncu. Mevki sayıları dizilişe uymalı. Kaptan ve penaltıcı sahada olmalı. Kural bozulursa yanıt <code>400</code> olur.</li>
            <li>Ekran oyuncuyu sürükle-bırakla veya "önce oyuncu, sonra slot" tıklamasıyla yerleştirir. Yanlış mevki bir uyarı gösterir.</li>
            <li>"En iyi 11" düğmesi yapay zekânın önerisini yükler: rastgelelik olmadan her mevkide en yüksek efektif güç.</li>
          </ul>
        </Prose>
        <Figure
          no="8.2"
          height={230}
          minWidth={720}
          label="Kadro kalitesi: seçilen 11'in efektif güç ortalaması en iyi 11'in ortalamasına bölünür. Sonuç 0,8 ile 1,0 arasında sınırlanır ve gol beklentisini çarpar."
          caption="Kadro kalitesi. Kullanıcı en iyi 11'den zayıf bir kadroyla çıkarsa gol beklentisi düşer. Örnek: oran 0,93 ise λ × 0,93. Oran 0,8'in altına inmez."
        >
          {[0.7, 0.8, 0.9, 1].map((v) => {
            const y = 190 - (v - 0.7) * 500
            return (
              <g key={v}>
                <line className={v === 1 ? 'g-zero' : 'g-grid'} x1={80} y1={y} x2={620} y2={y} />
                <Txt x={72} y={y + 4} cls="g-ms" anchor="end">{`×${v.toFixed(1).replace('.', ',')}`}</Txt>
              </g>
            )
          })}
          {[0.7, 0.8, 0.9, 1].map((v) => (
            <Txt key={v} x={80 + (v - 0.7) * 1800} y={210} cls="g-ms" anchor="middle">{v.toFixed(1).replace('.', ',')}</Txt>
          ))}
          <Txt x={350} y={226} cls="g-s" anchor="middle">seçilen 11 / en iyi 11 (efektif güç ortalaması)</Txt>
          <polyline className="g-ed acc" style={{ strokeWidth: 2.2 }} points={`80,${190 - 0.1 * 500} ${80 + 0.1 * 1800},${190 - 0.1 * 500} ${80 + 0.3 * 1800},${190 - 0.3 * 500}`} />
          <circle className="f-acc" cx={80 + 0.23 * 1800} cy={190 - 0.23 * 500} r={5} />
          <Txt x={80 + 0.23 * 1800 - 10} y={190 - 0.23 * 500 - 10} cls="g-s" anchor="end">0,93 → ×0,93</Txt>
          <Txt x={90} y={190 - 0.1 * 500 - 8} cls="g-s">alt sınır ×0,8</Txt>
          <Box x={680} y={30} w={260} h={150} title="Seçilen penaltıcı" lines={['penaltıları o atar', 'sahada değilse sahadaki en', 'güçlü forvet veya orta saha', '', 'Rakibin kadrosu', 'yapay zekâ tarafından seçilir']} />
        </Figure>
      </Section>

      <Section id="canli" no="8.3" title="Canlı maç ve devre arası">
        <Prose>
          <p>Kullanıcı maçı iki yolla oynatır. "Hızlı oynat" maçı bir anda oynatır. "Maçı canlı oyna" maçı iki devreye böler. Sistem ilk yarıyı oynatır ve devre arasında durur. Kullanıcı karar verir. Sonra sistem ikinci yarıyı yeni kararlarla oynatır.</p>
          <p>Her devrenin gol sayısı, o devrenin beklenen golünün yarısından (λ / 2) Poisson ile seçilir. İkinci yarının λ değeri devre arası kararlarıyla yeniden hesaplanır.</p>
        </Prose>
        <Figure
          no="8.3"
          height={360}
          minWidth={860}
          label="Canlı maç: ilk yarı 1-45 oynanır, oturum bellekte bekler. Devre arasında değişiklik, stil ve konuşma seçilir. İkinci yarı yeni λ ile 46-90 oynanır, maç kaydedilir ve haftanın kalan maçları oynanır."
          caption="Canlı maç akışı. Devre arası oturumu sunucu belleğindedir. Kullanıcı sayfadan ayrılıp dönerse ekran devre arasına döner (GET /my-team/live). Sunucu yeniden başlarsa oturum kaybolur ve hiçbir şey kaydedilmemiş olur."
        >
          <Box x={20} y={30} w={200} h={70} tone="acc" title="İlk yarı" lines={[{ text: 'POST /my-team/live/start', mono: true }, '1.–45. dakika, λ / 2']} />
          <Edge from={[220, 65]} to={[257, 65]} />
          <Box x={260} y={20} w={380} h={170} tone="sig" strong title="Devre arası (kullanıcının kararları)" lines={[
            '• en fazla 3 değişiklik, 46. dakikada',
            '  sürükle-bırak: yedeği sahadakinin üstüne bırak',
            '  çıkanın gücü: maç başı → o anki (ör. 68 → 63 −5,0)',
            '• ikinci yarı stili: hücum, dengeli, savunma',
            '• soyunma odası konuşması (tablo)',
            '• rakip kadro: yapay zekâ değiştirmez',
          ]} />
          <Edge from={[640, 65]} to={[677, 65]} />
          <Box x={680} y={30} w={260} h={70} tone="acc" title="İkinci yarı" lines={[{ text: 'POST /my-team/live/second-half', mono: true }, '46.–90. dakika, yeni λ / 2']} />
          <Edge from={[810, 100]} to={[810, 127]} />
          <Box x={680} y={130} w={260} h={60} title="Kayıt" lines={['maç kaydedilir', 'haftanın veya turun kalanı oynanır']} />

          <Txt x={20} y={232} cls="g-t">İkinci yarının beklenen golü</Txt>
          <Box x={20} y={244} w={180} h={60} lines={[{ text: 'güç − yorgunluk × 2', mono: true }, 'sahadakilerin ortalama', 'maç içi yorgunluğu']} />
          <Txt x={210} y={278} cls="g-tl">×</Txt>
          <Box x={226} y={244} w={140} h={60} lines={[{ text: 'stil çarpanı', mono: true }, 'yeni stil']} />
          <Txt x={376} y={278} cls="g-tl">×</Txt>
          <Box x={392} y={244} w={160} h={60} lines={[{ text: 'oyuncu / 11', mono: true }, 'kırmızı kartla', 'eksik kalan takım']} />
          <Txt x={562} y={278} cls="g-tl">×</Txt>
          <Box x={578} y={244} w={160} h={60} lines={[{ text: 'kadro kalitesi', mono: true }, 'sahadaki 11', '(yalnız kullanıcı)']} />
          <Txt x={748} y={278} cls="g-tl">×</Txt>
          <Box x={764} y={244} w={176} h={60} tone="sig" lines={[{ text: 'konuşma çarpanı', mono: true }, 'skora bağlı (tablo)']} />
          <Txt x={20} y={336} cls="g-s">Dinç yedekler ortalama yorgunluğu düşürür. Bu nedenle devre arası değişikliği ikinci yarıda gücü artırır.</Txt>
        </Figure>
        <GTable
          head={['KONUŞMA', 'GERİDE', 'BERABERE', 'ÖNDE']}
          rows={[
            ['Motive et', 'kendi λ ×1,10', 'kendi λ ×1,10', 'etkisiz'],
            ['Sakinleştir', 'etkisiz', 'etkisiz', 'rakip λ ×0,90'],
            ['Eleştir', 'kendi λ ×1,15, moral −5', 'kendi λ ×0,92, moral −5', 'kendi λ ×0,92, moral −5'],
          ]}
        />
        <Note>
          <p>Canlı maç ekranı yatay bir saha gösterir: ev sahibi solda, deplasman sağda. Oyuncunun üstünde o anki gücü ve kondisyon çubuğu dakika dakika düşer. Olay simgeleri oyuncunun üstünde birikir. Yeni olayda bir balon çıkar: "GOL!", "Sarı kart", "Sakatlandı".</p>
        </Note>
      </Section>

      <Section id="rakip" no="8.4" title="Rakip analizi">
        <Prose>
          <p>"Rakibi incele" düğmesi sıradaki rakibin raporunu açar (<code>GET /api/my-team/opponent</code>). Düğme kadro ekranında ve panoda var. Rapor şunları gösterir:</p>
          <ul>
            <li>Güç farkı, rakibin morali ve hocası.</li>
            <li>Rakibin sırası, puanı, formu ve son maçları.</li>
            <li>Rakibin dizilişi ve beklenen stili. Bu dizilişe karşı avantajlı ve dezavantajlı dizilişler (Modül 06).</li>
            <li>Kullanıcının seçtiği dizilişin durumu. Değer diziliş değişince hemen güncellenir.</li>
            <li>Rakibin muhtemel 11'i, öne çıkan oyuncuları ve eksikleri. Bir strateji ipucu.</li>
          </ul>
        </Prose>
      </Section>

      <Section id="guven" no="8.5" title="Yönetim kurulu ve güven">
        <Prose>
          <p>Yönetim kurulunun menajere güveni 0–100 arasıdır. Başlangıç değeri 60'tır. Sezon başında kurul iki hedef koyar. Sonuçlar güveni artırır veya azaltır. Güven 0'a düşerse veya takım küme düşerse menajer kovulur.</p>
          <ul>
            <li>Lig hedefi: takımın güç sırası + 1. Hedef küme düşme hattının üstünde kalır.</li>
            <li>Kupa hedefi: güç sırası ilk 2 ise final, ilk 4 ise yarı final, ilk 8 ise çeyrek final.</li>
            <li>Bütçe hedefi: bütçe eksiye düşmemeli.</li>
          </ul>
        </Prose>
        <Figure
          no="8.4"
          height={330}
          minWidth={720}
          label="Güven değişimleri: lig galibiyeti +2, sürpriz galibiyet +5, mağlubiyet −2, hedefin her sıra üstü +5, altı −5, şampiyonluk +15, eksi bütçe −15, kupa hedefi +5 veya −10."
          caption="Güveni değiştiren olaylar. Sezon sonu sıra etkisi en fazla +20, en az −30 olur. Kupa hedefi bir sonraki sezonun başında değerlendirilir."
        >
          <line className="g-zero" x1={460} y1={14} x2={460} y2={300} />
          {[-15, -10, -5, 5, 10, 15].map((v) => (
            <g key={v}>
              <line className="g-grid" x1={460 + v * 20} y1={14} x2={460 + v * 20} y2={300} />
              <Txt x={460 + v * 20} y={318} cls="g-ms" anchor="middle">{v > 0 ? `+${v}` : `−${-v}`}</Txt>
            </g>
          ))}
          {CONFIDENCE.map(([label, v], i) => {
            const y = 20 + i * 31
            const x = v > 0 ? 460 : 460 + v * 20
            return (
              <g key={label}>
                <rect className={v > 0 ? 'f-acc' : 'f-dan'} x={x} y={y} width={Math.abs(v) * 20} height={20} />
                <Txt x={v > 0 ? 452 : 468} y={y + 15} cls="g-s" anchor={v > 0 ? 'end' : 'start'}>{label}</Txt>
                <Txt x={v > 0 ? 466 + v * 20 : 454 + v * 20} y={y + 15} cls="g-m g-b" anchor={v > 0 ? 'start' : 'end'}>{v > 0 ? `+${v}` : `−${-v}`}</Txt>
              </g>
            )
          })}
        </Figure>
      </Section>

      <Section id="kutu" no="8.6" title="Gelen kutusu, sözleşme ve satış">
        <Prose>
          <p>Gelen kutusu menajere gelen mesajları tutar. Okunmamış mesaj sayısı sekmede görünür. Teklif türlerinde kullanıcı bir karar verir.</p>
        </Prose>
        <GTable
          head={['TÜR', 'NE ZAMAN', 'EYLEM']}
          rows={[
            ['Hoş geldin, hedefler', 'Takımı devralınca, sezon başında', '—'],
            ['Yönetim değerlendirmesi', 'Sezon sonunda, kupa hedefinden sonra', '—'],
            ['Transfer teklifi', 'Yapay zekâ oyuncunu istiyor (istenen bedelin 0,9–1,15 katı)', 'Sat / Reddet'],
            ['Sözleşme', 'Biten veya bitecek sözleşmeler, ayrılan oyuncu', '—'],
            ['Sakatlık', 'Oyuncun maçta sakatlandı (süresiyle)', '—'],
            ['Altyapı genci', 'Sezon sonunda 1–3 genç (16–18 yaş)', 'A takıma al / Bırak'],
            ['Görevden alınma, iş teklifi', 'Güven bitti veya takım düştü', 'Görevi kabul et'],
          ]}
        />
        <Figure
          no="8.5"
          height={190}
          minWidth={720}
          label="Sözleşme pazarlığı: teklif edilen maaş oyuncunun isteğinin yüzde 80'inin altındaysa ret, yüzde 80 ile 95 arası karşı istek, yüzde 95 ve üstü kabul."
          caption="Sözleşme pazarlığı (Sözleşmeler sekmesi). Oyuncu değerine göre maaş ister. 23 yaş altı +%10, takımın en iyi 3 oyuncusu +%15 ister. Menajerin takımında yapay zekâ sözleşme uzatmaz."
        >
          {[
            [50, 80, 'f-dans', 'Ret', 't-dan'],
            [80, 95, 'f-sigs', 'Karşı istek', 't-sig'],
            [95, 120, 'f-accs', 'Kabul · yeni maaş ve bitiş', 't-acc'],
          ].map(([from, to, cls, label, tone]) => {
            const x1 = 60 + (Number(from) - 50) * 12
            const x2 = 60 + (Number(to) - 50) * 12
            return (
              <g key={String(label)}>
                <rect className={String(cls)} x={x1} y={40} width={x2 - x1} height={44} />
                <Txt x={(x1 + x2) / 2} y={67} cls={`g-t ${tone}`} anchor="middle">{String(label)}</Txt>
              </g>
            )
          })}
          <line className="g-axis" x1={60} y1={84} x2={900} y2={84} />
          {[50, 80, 95, 120].map((p) => (
            <g key={p}>
              <line className="g-axis" x1={60 + (p - 50) * 12} y1={84} x2={60 + (p - 50) * 12} y2={90} />
              <Txt x={60 + (p - 50) * 12} y={104} cls="g-ms" anchor="middle">{`%${p}`}</Txt>
            </g>
          ))}
          <Txt x={480} y={124} cls="g-s" anchor="middle">teklif edilen haftalık maaş / oyuncunun isteği</Txt>
          <Txt x={60} y={160} cls="g-s">Satışa çıkar: bedeli karşılayabilen en fazla 2 takım gelen kutusuna teklif gönderir. Teklif kabul edilince oyuncu satılır, bütçe artar.</Txt>
          <Txt x={60} y={178} cls="g-s">Uzatılmayan sözleşme sezon sonunda biter ve oyuncu serbest kalır.</Txt>
        </Figure>
        <Note>
          <p>"Kariyer" sekmesi menajerin takım takım dönemlerini gösterir: sezon aralığı, ayrılış nedeni, maç, galibiyet, beraberlik, mağlubiyet, şampiyonluk ve kupa (<code>GET /api/my-team/career</code>).</p>
        </Note>
      </Section>

      <Section id="baslat" no="8.7" title="Takımı devralın">
        <Steps>
            <p>"Takımım" sayfasını açın.</p>
            <p>Menajer adını yazın. Takım kartlarından bir takım seçin.</p>
            <p>Gelen kutusunda sezon hedeflerini okuyun.</p>
            <p>"Kadroyu belirle" düğmesine basın. İlk 11'i, dizilişi, stili, kaptanı ve penaltıcıyı seçin. Kadroyu kaydedin.</p>
            <p>"Rakibi incele" düğmesine basın (isteğe bağlı). Dizilişi rakibe göre değiştirin.</p>
            <p>"Maçı canlı oyna" veya "Hızlı oynat" düğmesine basın.</p>
            <p>Canlı maçta devre arasında değişiklikleri, stili ve konuşmayı seçin. İkinci yarıyı başlatın.</p>
        </Steps>
        <Note kind="caution">
          <p>Devre arasındayken backend'i yeniden başlatmayın. Oturum bellektedir. Yeniden başlatma oturumu siler ve ilk yarı yeniden oynanır.</p>
        </Note>
      </Section>
    </>
  )
}

export default ManagerMode
