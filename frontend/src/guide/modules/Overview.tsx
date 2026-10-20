import { Box, Cmd, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

function Overview() {
  return (
    <>
      <Section id="ne" no="0.1" title="Sistem ne yapar">
        <Prose>
          <p>Sistem bir futbol ligini yönetir ve maçları simüle eder. Simülasyon rastgeledir. Güçlü takım daha sık kazanır.</p>
          <ul>
            <li>Sistem takımları, oyuncuları, teknik direktörleri ve hakemleri kaydeder.</li>
            <li>Sistem 18 takım için çift devreli fikstür hazırlar. Bir sezon 34 hafta ve 306 maçtır.</li>
            <li>Sistem her maçı dakika dakika oynatır. Her maç için skor, olaylar, kadrolar, istatistikler ve reytingler üretir.</li>
            <li>Kullanıcı haftayı canlı izleyebilir. Ekran her dakikanın olaylarını ve yorumlarını gösterir.</li>
            <li>Oynanmış her maç sahada 2D animasyonla izlenebilir: paslar, şutlar, goller, kartlar.</li>
            <li>Takımların bütçesi, oyuncuların maaşı ve sözleşmesi var. Sezon arasında transfer penceresi açılır.</li>
            <li>Kullanıcı isterse bir takımı yönetir. Kadroyu seçer, maçı canlı oynar, devre arasında karar verir.</li>
            <li>Lig bitince şampiyon kaydedilir, 3 takım düşer, ilk 8 takım kupayı oynayabilir.</li>
            <li>Kullanıcı oyunu kayıt noktasına kaydeder, yedek alır ve tabloları CSV olarak indirir.</li>
          </ul>
        </Prose>
      </Section>

      <Section id="parcalar" no="0.2" title="Ana parçalar">
        <Prose>
          <p>Sistemin iki ana parçası var: frontend ve backend. Frontend tarayıcıda çalışır. Backend verileri işler ve saklar. İki parça HTTP üzerinden JSON ile konuşur. Canlı yayın aynı yoldan Server-Sent Events (SSE) ile gelir.</p>
        </Prose>
        <Figure
          no="0.1"
          height={330}
          minWidth={820}
          label="Tarayıcı Vite sunucusu üzerinden Spring Boot backend'e istek gönderir. Backend veritabanına, uploads ve saves klasörlerine yazar."
          caption="Sistemin ana parçaları. Yeşil ok frontend ile backend arasındaki sınırdır. Sağdaki üç kutu backend'in dosya ve veri depolarıdır. Ayrıntı: Modül 01."
        >
          <rect className="g-grp" x="10" y="20" width="440" height="150" rx="6" />
          <Txt x={22} y={40} cls="g-ms">FRONTEND · npm run dev</Txt>
          <Box x={20} y={55} w={170} h={95} tone="acc" center title="Tarayıcı" lines={['React 19 + React Router', { text: 'localhost:5173', mono: true }]} />
          <Box x={265} y={55} w={170} h={95} center title="Vite sunucusu" lines={['proxy: /api, /uploads', { text: 'vite.config.ts', mono: true }]} />
          <Edge from={[190, 88]} to={[262, 88]} />
          <Txt x={227} y={80} cls="g-s" anchor="middle">istek</Txt>
          <Edge from={[263, 120]} to={[193, 120]} />
          <Txt x={227} y={137} cls="g-s" anchor="middle">JSON, SSE</Txt>

          <rect className="g-grp" x="490" y="20" width="250" height="290" rx="6" />
          <Txt x={502} y={40} cls="g-ms">BACKEND · Spring Boot</Txt>
          <Txt x={728} y={40} cls="g-m" anchor="end">:8080</Txt>
          <Box x={520} y={85} w={190} h={42} center title="controller" lines={['13 sınıf · REST uç noktaları']} />
          <Box x={520} y={150} w={190} h={42} center title="service" lines={['iş kuralları + simülasyon']} />
          <Box x={520} y={225} w={190} h={42} center title="repository" lines={['JPA ile veri erişimi']} />
          <Edge from={[615, 127]} to={[615, 147]} />
          <Edge from={[615, 192]} to={[615, 222]} />
          <Txt x={615} y={294} cls="g-s" anchor="middle">Swagger UI: /swagger-ui.html</Txt>
          <Edge from={[435, 106]} to={[517, 106]} tone="acc" />
          <Txt x={470} y={98} cls="g-s" anchor="middle">/api/*</Txt>

          <Box x={790} y={55} w={155} h={50} center title="uploads/" lines={['takım logoları']} />
          <Box x={790} y={125} w={155} h={50} center title="saves/" lines={['kayıt noktaları (JSON)']} />
          <Box x={790} y={205} w={155} h={78} tone="sig" center title="Veritabanı" lines={['H2 bellek · H2 dosya', 'veya PostgreSQL']} />
          <Edge d="M710,163 H748 V80 H787" />
          <Txt x={768} y={74} cls="g-s" anchor="middle">logo</Txt>
          <Edge d="M710,178 H758 V150 H787" />
          <Txt x={773} y={144} cls="g-s" anchor="middle">kayıt</Txt>
          <Edge from={[710, 246]} to={[787, 246]} />
          <Txt x={750} y={238} cls="g-s" anchor="middle">SQL</Txt>

          <Txt x={20} y={205} cls="g-s">Geliştirme modunda iki sunucu çalışır.</Txt>
          <Txt x={20} y={223} cls="g-s">Tarayıcı yalnız 5173 portuna bağlanır.</Txt>
          <Txt x={20} y={241} cls="g-s">Vite, /api isteklerini 8080 portuna iletir.</Txt>
          <Txt x={20} y={259} cls="g-s">Bu nedenle CORS ayarı gerekmez.</Txt>
        </Figure>
      </Section>

      <Section id="modlar" no="0.3" title="İki çalışma modu">
        <Prose>
          <p>Sistem iki modda çalışır. Otomatik modda yapay zekâ tüm takımları yönetir. Menajer modunda kullanıcı bir takımı yönetir. İki mod aynı maç motorunu kullanır. Fark, kararları kimin verdiğidir.</p>
        </Prose>
        <Figure
          no="0.2"
          height={290}
          minWidth={820}
          label="Otomatik modda hafta doğrudan oynanır. Menajer modunda sistem kullanıcının kadrosunu bekler, sonra maçı canlı oynatır."
          caption="İki mod. Üst sıra otomatik mod, alt sıra menajer modu. Yeşil kutular kullanıcının karar verdiği adımlardır. Kadro seçilmezse hafta oynanmaz. Ayrıntı: Modül 08."
        >
          <Txt x={20} y={30} cls="g-ms">OTOMATİK MOD</Txt>
          <Box x={20} y={40} w={170} h={64} title="Haftayı oynat" lines={['Fikstür sayfası']} />
          <Box x={210} y={40} w={170} h={64} title="Yapay zekâ seçer" lines={['kadro, diziliş, stil']} />
          <Box x={400} y={40} w={170} h={64} title="9 maç oynanır" lines={['aynı maç motoru']} />
          <Box x={590} y={40} w={170} h={64} title="Sonuçlar" lines={['canlı yayın, puan durumu']} />
          <Edge from={[190, 72]} to={[207, 72]} />
          <Edge from={[380, 72]} to={[397, 72]} />
          <Edge from={[570, 72]} to={[587, 72]} />

          <Txt x={20} y={140} cls="g-ms">MENAJER MODU</Txt>
          <Box x={20} y={150} w={170} h={64} title="Haftayı oynat" lines={['Fikstür veya Takımım']} />
          <Box x={210} y={150} w={170} h={64} tone="sig" title="Kadro kontrolü" lines={['senin maçın var mı?', 'kadro seçildi mi?']} />
          <Box x={400} y={150} w={170} h={64} tone="acc" title="Senin kadron" lines={['ilk 11, diziliş, stil', 'kaptan, penaltıcı']} />
          <Box x={590} y={150} w={170} h={64} tone="acc" title="Canlı maç" lines={['devre arası: değişiklik,', 'stil, konuşma']} />
          <Box x={780} y={150} w={170} h={64} title="Sonuçlar" lines={['diğer 8 maç da oynanır']} />
          <Edge from={[190, 182]} to={[207, 182]} />
          <Edge from={[380, 182]} to={[397, 182]} />
          <Edge from={[570, 182]} to={[587, 182]} />
          <Edge from={[760, 182]} to={[777, 182]} />
          <Edge from={[295, 214]} to={[295, 241]} tone="dan" />
          <Box x={210} y={244} w={170} h={36} tone="dan" center title="kadro yok → 409" />
          <Txt x={392} y={266} cls="g-s">Hata kutusu: "Takımım →" ya da "Kadroyu yapay zekâ seçsin"</Txt>
        </Figure>
      </Section>

      <Section id="akis" no="0.4" title="Bir sezonun akışı">
        <Prose>
          <p>Bir sezon 6 adımda ilerler. Adım 4 otomatik olur. Adım 5 isteğe bağlıdır. Adım 5 ve 6 aynı dönemde olur: lig bitince hem kupa hem transfer penceresi açılır. Yeni sezon adım 2 ile başlar.</p>
        </Prose>
        <Figure
          no="0.3"
          width={980}
          height={240}
          minWidth={820}
          label="Sezon akışı: takımları hazırla, fikstürü oluştur, haftaları oynat, sezon biter, kupa, transfer penceresi; sonra yeni sezon."
          caption="Bir sezonun adımları. Her kutu bir sayfayı ve bu sayfanın çağırdığı API yolunu gösterir. Tüm yollar /api ile başlar. Ayrıntı: Modül 03."
        >
          {[
            { x: 10, tag: 'ADIM 1', title: 'Takımları hazırla', page: 'Takımlar sayfası', api: ['POST', '/teams/random'] },
            { x: 172, tag: 'ADIM 2', title: 'Fikstürü oluştur', page: 'Fikstür sayfası', api: ['POST', '/fixtures/generate'], tone: 'acc' as const },
            { x: 334, tag: 'ADIM 3 · 34 KEZ', title: 'Haftaları oynat', page: 'canlı yayınla', api: ['POST', '/weeks/{n}/play'] },
            { x: 496, tag: 'ADIM 4 · OTOMATİK', title: 'Sezon biter', page: 'ödül, gelişim, düşme', api: ['SeasonEnd', 'Service'], tone: 'sig' as const },
            { x: 658, tag: 'ADIM 5 · İSTEĞE BAĞLI', title: 'Kupayı oynat', page: 'Kupa sayfası', api: ['POST', '/cup/play-round'], tone: 'ghost' as const },
            { x: 820, tag: 'ADIM 6', title: 'Transfer penceresi', page: 'Transferler sayfası', api: ['POST', '/transfers/offers'] },
          ].map((step) => (
            <Box
              key={step.tag}
              x={step.x}
              y={30}
              w={150}
              h={112}
              tone={step.tone}
              tag={step.tag}
              title={step.title}
              lines={[step.page, { text: step.api[0], mono: true }, { text: step.api[1], mono: true }]}
            />
          ))}
          {[160, 322, 484, 646, 808].map((x) => (
            <Edge key={x} from={[x, 86]} to={[x + 10, 86]} />
          ))}
          <Edge d="M895,142 V185 H247 V145" tone="acc" />
          <rect className="f-muted" x="400" y="176" width="340" height="18" />
          <Txt x={570} y={189} cls="t-acc" anchor="middle">yeni sezon: fikstür transfer penceresini kapatır</Txt>
          <Txt x={10} y={222} cls="g-s">Kupa yarıdayken yeni sezon başlamaz. Menajer modunda hafta kullanıcının kadrosunu bekler.</Txt>
        </Figure>
      </Section>

      <Section id="calistir" no="0.5" title="Sistemi çalıştırın">
        <Prose>
          <p>Bu prosedür sistemi Docker olmadan çalıştırır. Veriyi kalıcı tutmak için <code>h2file</code> profilini kullanın. Geçici bir deneme için <code>h2</code> profilini kullanın.</p>
        </Prose>
        <Note>
          <p>Gereksinimler: JDK 25 ve Node.js 22.22 veya üstü. Maven kurmayın. Proje kendi <code>./mvnw</code> dosyasını kullanır.</p>
        </Note>
        <Steps>
            <>
              <p>Bir terminal açın. Backend klasörüne gidin.</p>
              <Cmd>cd backend</Cmd>
            </>
            <>
              <p>Backend'i başlatın. Veri kalıcı olsun istiyorsanız <code>h2file</code>, geçici olsun istiyorsanız <code>h2</code> yazın.</p>
              <Cmd>./mvnw spring-boot:run -Dspring-boot.run.profiles=h2file</Cmd>
              <Note kind="caution">
                <p>Profil adından sonra başka karakter yazmayın. <code>h2~</code> gibi bir değer uygulamayı durdurur.</p>
              </Note>
            </>
            <p>
              Logda <code>Started FootballLeagueApplication</code> satırını bekleyin. Backend şimdi 8080 portunda çalışır.
            </p>
            <>
              <p>İkinci bir terminal açın. Frontend'i başlatın.</p>
              <Cmd>cd frontend && npm install && npm run dev</Cmd>
            </>
            <p>
              Tarayıcıda <code>http://localhost:5173</code> adresini açın.
            </p>
        </Steps>
        <Note>
          <p>Backend başka bir portta çalışırsa frontend'i şu komutla başlatın: <code>BACKEND_URL=http://localhost:8090 npm run dev</code>.</p>
        </Note>
      </Section>

      <Section id="terimler" no="0.6" title="Terimler">
        <Prose>
          <p>Bu rehber her kavram için tek bir terim kullanır. Tablo bu terimleri tanımlar.</p>
        </Prose>
        <GTable
          head={['TERİM', 'ANLAM']}
          rows={[
            ['Güç', 'Takımın veya oyuncunun seviyesi. Değer aralığı 1–100. Normal takım en fazla 84 olur.'],
            ['4 büyükler', 'Galatasaray, Fenerbahçe, Beşiktaş, Trabzonspor. Güçleri 85–100 aralığında kalır. Maç hesabında +8 alırlar.'],
            ['Moral', 'Takımın son sonuçlara göre durumu. Değer aralığı 0–100. Her sezon 50 ile başlar.'],
            ['Efektif güç', 'Oyuncunun maçtaki gücü: güç × form − yorgunluk. İlk 11 seçimi ve gol dağılımı bunu kullanır.'],
            ['Form', 'Son 5 maçın reyting ortalamasından gelen çarpan. Değer aralığı 0,9–1,1.'],
            ['Diziliş', '4-4-2, 4-3-3, 3-5-2, 5-3-2 veya 4-5-1. Her diziliş ikisini yener, ikisine yenilir.'],
            ['Oyun stili', 'Hücum, dengeli veya savunma. Stil iki takımın gol beklentisini çarpar.'],
            ['Hakem sertliği', 'Hakemin 1–10 arası değeri. Kart ve penaltı sayısını çarpar.'],
            ['Piyasa değeri', 'Oyuncunun avro değeri. Güç, yaş ve mevkiden hesaplanır. Maaş bu değerden gelir.'],
            ['Transfer penceresi', 'Lig bitince açılan, yeni fikstürle kapanan dönem. Teklifler yalnız bu dönemde yapılır.'],
            ['Güven', 'Yönetim kurulunun menajere güveni. Değer aralığı 0–100. 0 olursa menajer kovulur.'],
            ['SSE', 'Server-Sent Events. Backend\'in tarayıcıya tek yönlü olay akışı. Canlı yayın bunu kullanır.'],
            ['Kayıt noktası', 'Oyunun adlandırılmış anlık görüntüsü. saves/ klasöründe JSON dosyasıdır.'],
            ['Profil', 'Spring Boot ayar seti. Veritabanını seçer: varsayılan, h2 veya h2file.'],
          ]}
        />
        <Note label="YAZIM KURALI">
          <p>
            Bu rehber ASD-STE100 (Simplified Technical English) kurallarını Türkçe metne uygular. Prosedür cümleleri en fazla 20, açıklama cümleleri en fazla 25 kelimedir. Bir cümle bir konuyu anlatır. Prosedürler emir kipi kullanır. Uyarılar ilgili adımdan önce veya adımın içinde gelir.
          </p>
        </Note>
      </Section>
    </>
  )
}

export default Overview
