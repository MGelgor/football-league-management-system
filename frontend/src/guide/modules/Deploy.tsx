import { Box, Cmd, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

// Şekil 13.2: 512 MB'lık bellek şeridi
const MB = 920 / 512
const MEMORY = [
  { mb: 256, label: 'heap · -Xmx256m', cls: 'f-accs' },
  { mb: 160, label: 'metaspace · 160m', cls: 'f-sigs' },
  { mb: 48, label: 'code cache', cls: 'f-muted' },
  { mb: 48, label: 'thread, native', cls: 'f-line' },
]

function memoryX(index: number) {
  return 20 + MEMORY.slice(0, index).reduce((sum, part) => sum + part.mb * MB, 0)
}

function Deploy() {
  return (
    <>
      <Section id="lokal" no="13.1" title="Lokalden yayına">
        <Prose>
          <p>Lokalde üç parça aynı bilgisayardadır. Tarayıcı sayfayı Vite'tan alır. Vite <code>/api</code> ile başlayan istekleri Spring Boot'a iletir (proxy). Tarayıcı için sayfa ve API aynı adrestedir, bu yüzden ek bir izin gerekmez.</p>
          <p>Yayında parçalar ayrılır. Frontend derlenmiş statik dosyalardır ve GitHub Pages'te durur. Backend Render'da bir Docker konteynerinde çalışır. Veritabanı Neon'dadır. Tarayıcı sayfayı GitHub'dan indirir, sonra API isteklerini <b>doğrudan</b> Render'a gönderir. Sayfa ve API farklı adreslerde olduğu için iki şey gerekir: frontend backend'in adresini bilmeli (<code>VITE_API_URL</code>) ve backend bu sayfaya izin vermeli (CORS).</p>
        </Prose>
        <Figure
          no="13.1"
          height={420}
          minWidth={880}
          label="Solda lokal: tarayıcı, Vite geliştirme sunucusu, Spring Boot 8080, H2 veya Docker Postgres. Sağda yayın: tarayıcı, GitHub Pages, Render, Neon. Yayında tarayıcı API'ye doğrudan bağlanır."
          caption="Lokal ve yayın mimarisi. Kod iki tarafta aynıdır. Lokalde istekler Vite'tan geçer (aynı köken). Yayında tarayıcı sayfayı GitHub Pages'ten, veriyi Render'dan alır; bu iki köken arasında CORS izni gerekir."
        >
          <rect className="g-grp" x="20" y="16" width="450" height="392" rx="6" />
          <Txt x={36} y={40} cls="g-tl">LOKAL · tek bilgisayar</Txt>
          <Box x={60} y={56} w={370} h={52} tone="acc" center title="Tarayıcı" lines={[{ text: 'http://localhost:5173', mono: true }]} />
          <Box x={60} y={140} w={370} h={66} center title="Vite geliştirme sunucusu :5173" lines={['React kaynak kodu, anında yenileme', { text: "proxy: '/api' → localhost:8080", mono: true }]} />
          <Box x={60} y={238} w={370} h={66} center title="Spring Boot :8080" lines={[{ text: './mvnw spring-boot:run', mono: true }, 'profil: h2, h2file ya da varsayılan']} />
          <Box x={60} y={336} w={370} h={52} tone="sig" center title="H2 (bellek / dosya) ya da Docker Postgres" lines={['localhost:5432 · SSL yok']} />
          <Edge from={[245, 108]} to={[245, 137]} />
          <Txt x={255} y={128} cls="g-s">sayfa + /api (aynı köken)</Txt>
          <Edge from={[245, 206]} to={[245, 235]} />
          <Txt x={255} y={226} cls="g-s">proxy iletir</Txt>
          <Edge from={[245, 304]} to={[245, 333]} />
          <Txt x={255} y={324} cls="g-s">JDBC</Txt>

          <rect className="g-grp" x="490" y="16" width="450" height="392" rx="6" />
          <Txt x={506} y={40} cls="g-tl">YAYIN · üç ücretsiz servis</Txt>
          <Box x={530} y={56} w={370} h={52} tone="acc" center title="Tarayıcı" lines={[{ text: 'mgelgor.github.io/football-league-…', mono: true }]} />
          <Box x={530} y={140} w={290} h={66} center title="GitHub Pages" lines={['statik: index.html, assets/*.js', 'sunucu kodu yok']} />
          <Box x={530} y={238} w={370} h={66} tone="away" center title="Render · Docker" lines={[{ text: 'futbol-ligi-api.onrender.com', mono: true }, 'profil: prod · Frankfurt']} />
          <Box x={530} y={336} w={370} h={52} tone="sig" center title="Neon Postgres" lines={['Frankfurt · sslmode=require']} />
          <Edge from={[675, 108]} to={[675, 137]} />
          <Txt x={685} y={128} cls="g-s">1 · sayfa ve JS</Txt>
          <Edge from={[860, 108]} to={[860, 235]} tone="away" />
          <Txt x={868} y={160} cls="g-s t-away">2 · API</Txt>
          <Txt x={868} y={176} cls="g-s t-away">HTTPS</Txt>
          <Txt x={868} y={192} cls="g-s t-away">+ CORS</Txt>
          <Edge from={[715, 304]} to={[715, 333]} />
          <Txt x={725} y={324} cls="g-s">JDBC + SSL</Txt>
        </Figure>
        <GTable
          head={['PARÇA', 'LOKAL', 'YAYIN']}
          rows={[
            ['Frontend', <><code>npm run dev</code> (Vite, :5173)</>, <>GitHub Pages, <code>npm run build</code> çıktısı</>],
            ['API adresi', <>boş: istekler <code>/api/…</code>, Vite iletir</>, <><code>VITE_API_URL</code> + <code>/api/…</code> (<code>apiUrl()</code>)</>],
            ['Alt yol', <><code>/</code></>, <><code>/football-league-management-system/</code> (<code>VITE_BASE</code>, <code>appPath()</code>)</>],
            ['Backend', <><code>./mvnw spring-boot:run</code></>, <>Render, <code>backend/Dockerfile</code></>],
            ['Profil', 'h2, h2file ya da varsayılan', <><code>prod</code> (<code>application-prod.yml</code>)</>],
            ['Veritabanı', 'H2 ya da Docker Postgres', 'Neon Postgres (SSL)'],
            ['Oda', 'isteğe bağlı: odasız istek tek kişilik lig', <>zorunlu: <code>app.rooms.required=true</code>, odasız istek 401</>],
            ['Uyku', 'yok', 'Render 15 dk, Neon 5 dk boşta kalınca uyur (13.4)'],
          ]}
        />
      </Section>

      <Section id="render" no="13.2" title="Render: backend sunucusu">
        <Prose>
          <p><b>Render</b> bir barındırma servisidir. Ona bir Docker tarifi (<code>Dockerfile</code>) verirsiniz; Render imajı derler, konteyneri çalıştırır ve ona bir HTTPS adresi verir. Sunucu kurmak, işletim sistemi güncellemek, sertifika almak gerekmez. Ayarlar depodaki <code>render.yaml</code> dosyasındadır (Blueprint).</p>
          <p>Ücretsiz planın üç sınırı tasarımı belirledi: <b>512 MB bellek</b>, düşük işlemci gücü ve <b>15 dakika istek gelmezse uyku</b>. Uyuyan sunucu ilk istekte yeniden açılır; bu bir dakikayı bulabilir.</p>
        </Prose>
        <Figure
          no="13.2"
          height={300}
          minWidth={860}
          label="İki aşamalı Docker derlemesi: JDK ile derleme, yalnız JRE ile çalıştırma. Render konteyneri çalıştırır. Altta 512 MB belleğin paylaşımı: heap 256, metaspace 160, code cache 48, kalan 48."
          caption="Dockerfile ve bellek. Derleme aşaması büyük JDK imajını kullanır, ama son imaja yalnızca app.jar geçer. Alttaki şerit 512 MB'ın JVM ayarlarıyla nasıl paylaşıldığını gösterir."
        >
          <Box x={20} y={24} w={260} h={140} tag="AŞAMA 1 · BUILD" title="eclipse-temurin:25-jdk" lines={[{ text: 'COPY pom.xml .mvn mvnw', mono: true }, { text: 'mvnw dependency:go-offline', mono: true }, { text: 'COPY src', mono: true }, { text: 'mvnw package (testsiz)', mono: true }]} />
          <Box x={320} y={24} w={260} h={140} tone="acc" tag="AŞAMA 2 · ÇALIŞTIR" title="eclipse-temurin:25-jre" lines={[{ text: 'COPY app.jar', mono: true }, { text: 'JAVA_TOOL_OPTIONS=…', mono: true }, { text: 'SPRING_PROFILES_ACTIVE=prod', mono: true }, { text: 'java -jar app.jar', mono: true }]} />
          <Box x={620} y={24} w={320} h={140} tone="away" tag="RENDER · futbol-ligi-api" title="Web servisi (ücretsiz)" lines={['HTTPS vekil → konteynerin PORT’u', 'healthCheckPath: /health', 'ortam değişkenleri (13.6)', '15 dk istek yoksa uyur']} />
          <Edge from={[280, 94]} to={[317, 94]} />
          <Txt x={298} y={86} cls="g-s" anchor="middle">jar</Txt>
          <Edge from={[580, 94]} to={[617, 94]} />
          <Txt x={598} y={86} cls="g-s" anchor="middle">imaj</Txt>

          <Txt x={20} y={204} cls="g-ms">512 MB BELLEK</Txt>
          {MEMORY.map((part, i) => (
            <g key={part.label}>
              <rect className={part.cls} x={memoryX(i)} y="214" width={part.mb * MB - 2} height="36" rx="3" />
              <Txt x={memoryX(i) + (part.mb * MB) / 2} y={237} cls="g-s" anchor="middle">
                {part.label}
              </Txt>
            </g>
          ))}
          <Txt x={20} y={276} cls="g-s">-XX:+UseSerialGC: tek thread'li çöp toplayıcı · -XX:TieredStopAtLevel=1: hızlı açılış (C1 derleyicisi) · -Xss512k: küçük thread yığını</Txt>
        </Figure>
        <Note>
          <p><code>/health</code> (<code>HealthController</code>) veritabanına gitmez, yalnızca "ayaktayım" der. Render yeni sürümü açarken bu adrese bakar: 200 gelince trafiği yeni konteynere verir. Veritabanına gitmediği için Neon'u da uyandırmaz.</p>
        </Note>
      </Section>

      <Section id="neon" no="13.3" title="Neon: veritabanı">
        <Prose>
          <p><b>Neon</b> sunucusuz (serverless) bir PostgreSQL servisidir. Klasik bir Postgres sunucusu sürekli çalışır. Neon ise iki parçaya ayrılır: veriyi tutan <b>depolama</b> (storage) ve sorguları çalıştıran <b>işlem birimi</b> (compute). Depolama her zaman durur. İşlem birimi yalnızca sorgu gelince çalışır; 5 dakika sorgu gelmezse uyur. İlk sorgu onu kısa sürede uyandırır.</p>
          <p>Ücretsiz planda işlem biriminin çalışma süresi sınırlıdır. Bu yüzden sistem Neon'u boş yere uyanık tutmaz (Modül 12.4). Veritabanı 27 Ekim 2026'da Render Postgres'ten Neon'a taşındı, çünkü Render'ın ücretsiz veritabanının süresi doluyordu.</p>
        </Prose>
        <Figure
          no="13.3"
          height={270}
          minWidth={860}
          label="Spring'in bağlantı havuzu Neon'a doğrudan uç noktadan bağlanır. Havuzlayıcı uç noktası (pooler) kullanılmaz, çünkü işlem modunda setSchema kaybolur. Neon compute ve storage ayrıdır."
          caption="Neon'a bağlantı. Yeşil yol kullanılan doğrudan uç noktadır. Kırmızı yol kullanılmaz: havuzlayıcı her işlemde başka bir sunucu bağlantısı verebilir ve oda şeması (Modül 11.2) kaybolur."
        >
          <Box x={20} y={60} w={200} h={110} tone="acc" tag="SPRING" title="Hikari havuzu" lines={[{ text: 'maximum-pool-size: 5', mono: true }, { text: 'minimum-idle: 0', mono: true }, { text: 'keepalive-time: 0', mono: true }]} />
          <Box x={290} y={24} w={270} h={84} tone="acc" tag="KULLANILIR" title="doğrudan uç nokta" lines={[{ text: 'ep-purple-water-….neon.tech', mono: true }, 'bağlantı = bir Postgres oturumu']} />
          <Box x={290} y={140} w={270} h={96} tone="dan" tag="KULLANILMAZ" title="…-pooler (PgBouncer)" lines={['işlem modu: her işlem başka', 'bir sunucu bağlantısına gider', 'setSchema kaybolur → yanlış oda']} />
          <Edge d="M220,100 C255,100 255,66 287,66" tone="acc" />
          <Edge d="M220,130 C255,130 255,188 287,188" tone="dan" dash />
          <Box x={620} y={24} w={150} h={110} tone="sig" tag="COMPUTE" title="Postgres işlemi" lines={['5 dk boşta → uyur', 'sorgu gelince uyanır']} />
          <Box x={800} y={24} w={140} h={110} tag="STORAGE" title="veri" lines={['her zaman durur', 'compute uyurken de']} />
          <Edge from={[560, 66]} to={[617, 66]} tone="acc" />
          <Edge from={[770, 79]} to={[797, 79]} both />
          <Txt x={620} y={170} cls="g-s">Adres, kullanıcı: render.yaml (DB_HOST, DB_USER …)</Txt>
          <Txt x={620} y={188} cls="g-s">Parola: Render panelinde (sync: false)</Txt>
          <Txt x={620} y={206} cls="g-s">SSL: DB_SSLMODE=require</Txt>
          <Txt x={620} y={224} cls="g-s">Bölge: aws-eu-central-1 (Frankfurt),</Txt>
          <Txt x={620} y={240} cls="g-s">Render ile aynı şehir: düşük gecikme</Txt>
        </Figure>
        <Note label="SETSCHEMA NEDEN KAYBOLUR">
          <p><code>setSchema</code> bir oturum ayarıdır: bağlantı açık kaldıkça geçerlidir. PgBouncer'ın işlem modunda bir istemci bağlantısı her işlemde başka bir sunucu bağlantısına verilebilir. Bir önceki işlemde ayarlanan şema yeni bağlantıda yoktur; sorgu varsayılan şemaya ya da başka bir odanın şemasına gidebilir. Doğrudan uç noktada her bağlantı tek bir oturumdur.</p>
        </Note>
      </Section>

      <Section id="uyku" no="13.4" title="Uyanık tutma ve uyku">
        <Prose>
          <p>Uyuyan bir sunucu dakikalık işi çalıştıramaz; maç günü kaçar. Önce GitHub Actions'ın zamanlanmış işi sunucuyu uyandırıyordu. Ama GitHub bu işleri saatlerce geciktirebiliyor. Bu yüzden sunucu artık <b>kendini</b> uyanık tutar: <code>KeepAwakeJob</code> 10 dakikada bir kendi genel adresine istek atar.</p>
          <p>Hile şudur: istek konteynerin içinden çıkar ama Render'ın vekil sunucusundan geçerek geri gelir. Render bunu dışarıdan gelmiş bir istek sayar ve uyku sayacını sıfırlar. İstek <code>/health</code>'e gider; veritabanına dokunmaz. Böylece Render uyanık kalır, Neon uyuyabilir.</p>
        </Prose>
        <Figure
          no="13.4"
          height={270}
          minWidth={860}
          label="KeepAwakeJob konteynerden RENDER_EXTERNAL_URL/health adresine istek atar, istek Render vekil sunucusundan geçip HealthController'a gelir. RoomTickJob yalnız nextDue zamanında Neon'a gider. tick.yml yedektir."
          caption="Uyanık tutma döngüsü. Mavi ok sunucunun kendine attığı istektir ve Render'ın vekilinden geçer. Kesik ok veritabanına yalnızca iş olunca gidildiğini gösterir. tick.yml sunucu bir sebeple uyursa onu uyandırır."
        >
          <rect className="g-grp" x="20" y="16" width="600" height="230" rx="6" />
          <Txt x={36} y={38} cls="g-ms">RENDER</Txt>
          <Box x={40} y={60} w={210} h={80} tone="away" title="Vekil sunucu" lines={[{ text: '*.onrender.com', mono: true }, 'uyku sayacı burada']} />
          <rect className="g-box" x="340" y="50" width="260" height="180" rx="4" />
          <Txt x={356} y={70} cls="g-t">Konteyner · Spring Boot</Txt>
          <Box x={356} y={82} w={228} h={40} tone="away" title="KeepAwakeJob · 10 dk" />
          <Box x={356} y={132} w={228} h={40} tone="acc" title="/health · veritabanı yok" />
          <Box x={356} y={182} w={228} h={40} tone="sig" title="RoomTickJob · 1 dk" />
          <Edge d="M356,102 H300 V88 H253" tone="away" />
          <Txt x={298} y={80} cls="g-s t-away" anchor="middle">GET …/health</Txt>
          <Edge d="M253,118 H300 V152 H353" tone="away" />
          <Txt x={150} y={160} cls="g-s" anchor="middle">"dışarıdan geldi":</Txt>
          <Txt x={150} y={176} cls="g-s" anchor="middle">15 dk sayacı sıfırlanır</Txt>

          <Box x={680} y={40} w={260} h={90} tone="sig" title="Neon" lines={['yalnızca gerçek iş olunca:', 'maç günü, oyuncu isteği', '5 dk sonra yine uyur']} />
          <Edge d="M584,202 H640 V100 H677" dash />
          <Txt x={646} y={160} cls="g-s">nextDue</Txt>
          <Box x={680} y={160} w={260} h={60} tone="ghost" title="tick.yml · 10 dk (yedek)" lines={['GitHub Actions → /internal/tick']} />
          <Edge d="M680,236 H70 V143" dash />
        </Figure>
        <GTable
          head={['SERVİS', 'NE ZAMAN UYUR', 'NASIL UYANIR', 'SINIR']}
          rows={[
            ['Render', '15 dk dışarıdan istek gelmezse', 'Herhangi bir HTTP isteği (açılış ~1 dk)', '750 saat/ay; 31 × 24 = 744 saat yeter.'],
            ['Neon', '5 dk sorgu gelmezse', 'İlk sorgu (çok kısa)', 'İşlem birimi süresi sınırlı: boşta sorgu atılmaz (12.4).'],
            ['GitHub Pages', 'uyumaz', '—', 'Statik dosya; her zaman açık.'],
          ]}
        />
      </Section>

      <Section id="hat" no="13.5" title="Push'tan yayına">
        <Prose>
          <p><code>main</code> dalına yapılan her push GitHub Actions'ı çalıştırır. Hangi işin çalışacağını değişen dosyalar belirler (<code>paths</code>). Frontend değiştiyse sayfa yeniden derlenip Pages'e yüklenir. Backend değiştiyse Render'a "yeni sürümü derle" denir. Üçüncü iş push'la değil saatle çalışır.</p>
        </Prose>
        <Figure
          no="13.5"
          height={330}
          minWidth={880}
          label="git push main sonrası iki iş: pages.yml frontend'i derleyip GitHub Pages'e yükler; deploy.yml Render deploy hook'unu çağırır, Render Docker imajını derler ve yeni konteyneri açar. tick.yml her 10 dakikada bir çalışır."
          caption="Yayın hattı. Üst sıra frontend, orta sıra backend yayınıdır. Alt sıra push'tan bağımsızdır: saat tetikler. Render yeni konteyneri /health 200 dönene kadar eski sürümü çalıştırmaya devam eder."
        >
          <Box x={20} y={120} w={150} h={64} tone="acc" center title="git push main" lines={['GitHub']} />
          <Txt x={190} y={22} cls="g-ms">PAGES.YML · paths: frontend/**</Txt>
          <Box x={190} y={30} w={250} h={80} lines={[{ text: 'npm ci · npm run build', mono: true }, { text: 'VITE_API_URL = vars.API_URL', mono: true }, { text: 'VITE_BASE = /<repo>/', mono: true }]} />
          <Box x={470} y={30} w={210} h={80} lines={[{ text: 'cp index.html 404.html', mono: true }, '/join/KOD gibi yollar', 'uygulamayı açsın diye']} />
          <Box x={710} y={30} w={230} h={80} tone="acc" center title="GitHub Pages" lines={['yeni dosyalar yayında']} />
          <Edge from={[440, 70]} to={[467, 70]} />
          <Edge from={[680, 70]} to={[707, 70]} />

          <Txt x={190} y={142} cls="g-ms">DEPLOY.YML · paths: backend/**</Txt>
          <Box x={190} y={150} w={250} h={80} lines={[{ text: 'curl -X POST $HOOK', mono: true }, { text: 'secrets.RENDER_DEPLOY_HOOK', mono: true }, 'gizli Render adresi']} />
          <Box x={470} y={150} w={210} h={80} tone="away" title="Render: docker build" lines={['Dockerfile (13.2)', 'birkaç dakika']} />
          <Box x={710} y={150} w={230} h={80} tone="away" title="Yeni konteyner açılır" lines={['şemalar güncellenir (13.7)', '/health 200 → trafik yeni sürüme']} />
          <Edge from={[440, 190]} to={[467, 190]} />
          <Edge from={[680, 190]} to={[707, 190]} />

          <Edge d="M170,140 C180,140 180,70 187,70" />
          <Edge d="M170,165 C180,165 180,190 187,190" />

          <Txt x={190} y={264} cls="g-ms">TICK.YML · schedule: */10 * * * *</Txt>
          <Box x={190} y={272} w={750} h={44} tone="ghost" center lines={[{ text: 'curl --retry 4 "$API_URL/internal/tick"  →  RoomScheduler.tickAllThrottled()', mono: true }]} />
          <Txt x={95} y={300} cls="g-s" anchor="middle">saat tetikler</Txt>
        </Figure>
        <Note>
          <p>Render servisi public depo adresiyle kurulduğu için push'u kendisi görmez. Bu yüzden <code>deploy.yml</code> Render'ın <b>Deploy Hook</b> adresini çağırır. Adres gizlidir: GitHub'da secret olarak durur. Secret yoksa iş uyarı verir ve yayın Render panelinden elle yapılır (Manual Deploy).</p>
        </Note>
      </Section>

      <Section id="ortam" no="13.6" title="Ortam değişkenleri">
        <Prose>
          <p>Kod adres ve parola bilmez. Bu bilgiler ortam değişkenlerinden gelir. <code>application-prod.yml</code> onları <code>{'${DB_HOST}'}</code> gibi yer tutucularla okur. Gizli olanlar depoya yazılmaz; ilgili servisin panelinden girilir.</p>
        </Prose>
        <GTable
          head={['DEĞİŞKEN', 'NEREDE TANIMLI', 'NE İŞE YARAR']}
          rows={[
            [<code key="a">DB_HOST, DB_PORT, DB_NAME, DB_USER</code>, <code key="a2">render.yaml</code>, 'Neon bağlantı bilgileri (gizli değil).'],
            [<code key="b">DB_PASSWORD</code>, <>Render paneli (<code>sync: false</code>)</>, <><span className="g-pill no">gizli</span> Neon parolası.</>],
            [<code key="c">DB_SSLMODE</code>, <code key="c2">render.yaml</code>, <><code>require</code>: Neon şifresiz bağlantı kabul etmez.</>],
            [<code key="d">CORS_ORIGINS</code>, <code key="d2">render.yaml</code>, 'İzinli frontend kökeni (GitHub Pages).'],
            [<code key="e">PORT, RENDER_EXTERNAL_URL</code>, 'Render kendisi verir', <>Dinlenecek port; <code>KeepAwakeJob</code>'un adresi. Lokalde boştur, iş kurulmaz.</>],
            [<code key="f">API_URL</code>, 'GitHub → Settings → Variables', <>Frontend derlenirken <code>VITE_API_URL</code> olur; <code>tick.yml</code> da kullanır.</>],
            [<code key="g">RENDER_DEPLOY_HOOK</code>, 'GitHub → Settings → Secrets', <><span className="g-pill no">gizli</span> Render'a yeni sürümü derletir.</>],
          ]}
        />
        <Note>
          <p><code>VITE_</code> ile başlayan değişkenler derleme anında JavaScript dosyasının içine yazılır. Yani tarayıcıdaki herkes görebilir. Bu yüzden frontend'e asla parola konmaz; yalnızca herkese açık backend adresi konur.</p>
        </Note>
      </Section>

      <Section id="sema" no="13.7" title="Şema güncellemesi">
        <Prose>
          <p>Yeni bir sürüm yeni bir sütun ya da tablo getirebilir. Açılışta Hibernate varsayılan şemayı günceller (<code>ddl-auto: update</code>). Ama Hibernate oda şemalarını bilmez. Bu yüzden <code>RoomSchemas.update</code> aynı güncellemeyi her oda şeması için tekrar çalıştırır. Eski odalar da yeni sütunları alır.</p>
        </Prose>
        <Figure
          no="13.7"
          height={210}
          minWidth={860}
          label="Açılış: Hibernate public şemayı günceller, rooms tablosu okunur, her oda şeması için aynı güncelleme ve elle düzeltmeler çalışır."
          caption="Açılışta şema güncellemesi. Her oda şeması varsayılan şemayla aynı aracı kullanır (hibernate.default_schema = odanın şeması). Sarı kutu aracın yapamadığı, elle yazılan düzeltmelerdir."
        >
          <Box x={20} y={60} w={150} h={70} tone="acc" center title="Açılış" lines={['yeni konteyner']} />
          <Box x={200} y={60} w={180} h={70} center title="public" lines={[{ text: 'ddl-auto: update', mono: true }]} />
          <Box x={410} y={60} w={170} h={70} center title="rooms okunur" lines={['30 odaya kadar']} />
          <Box x={610} y={20} w={150} h={44} tone="acc" center title="room_allzh7" />
          <Box x={610} y={74} w={150} h={44} tone="acc" center title="room_vk7ugc" />
          <Box x={610} y={128} w={150} h={44} tone="ghost" center title="room_…" />
          <Box x={790} y={54} w={150} h={84} tone="sig" title="elle düzeltme" lines={[{ text: 'competition', mono: true }, { text: 'varchar(10→20)', mono: true }]} />
          <Edge from={[170, 95]} to={[197, 95]} />
          <Edge from={[380, 95]} to={[407, 95]} />
          <Edge d="M580,95 C595,95 595,42 607,42" />
          <Edge from={[580, 96]} to={[607, 96]} />
          <Edge d="M580,97 C595,97 595,150 607,150" />
          <Edge from={[760, 96]} to={[787, 96]} />
          <Txt x={20} y={196} cls="g-s">RoomSchemas.update(şema): Hibernate'in şema aracı (SchemaManagementToolCoordinator), hedef şema = oda</Txt>
        </Figure>
        <GTable
          head={['DEĞİŞİKLİK', 'OTOMATİK Mİ', 'NOT']}
          rows={[
            ['Yeni tablo', <span key="a" className="g-pill ok">evet</span>, 'Tüm şemalarda oluşur.'],
            ['Yeni sütun', <span key="b" className="g-pill ok">evet</span>, <>Boş bırakılabilir olmalı ya da <code>@ColumnDefault</code> almalı; yoksa dolu tabloya eklenemez.</>],
            ['Enum’a yeni değer', <span key="c" className="g-pill no">hayır</span>, <>Postgres'te sütunun <code>check</code> kısıtı güncellenmez; yeni değer yazılamaz.</>],
            ['Sütun uzunluğu, tip, silme', <span key="d" className="g-pill no">hayır</span>, <>Elle SQL. Örnek: <code>SECOND_LEAGUE</code> (13 harf) <code>varchar(10)</code>'a sığmıyordu → "Ligi başlat" 500 veriyordu.</>],
          ]}
        />
      </Section>

      <Section id="tasi" no="13.8" title="Veritabanını taşıyın">
        <Prose>
          <p><code>scripts/veritabani-tasi.sh</code> bir Postgres'i başka bir Postgres'e birebir kopyalar: <code>public</code> ve bütün <code>room_…</code> şemaları, tablolar ve veriler. Render'dan Neon'a geçiş bu betikle yapıldı. Aynı betik ileride başka bir servise geçmek için de kullanılır.</p>
        </Prose>
        <Steps>
          <p>Render panelinde <code>futbol-ligi-api</code> servisini <b>askıya alın</b> (Suspend). Taşıma sırasında maç oynanmasın.</p>
          <p>Kaynak ve hedef bağlantı adreslerini alın. Hedef SSL istiyorsa adrese <code>?sslmode=require</code> ekleyin.</p>
          <p>Betiği çalıştırın. Betik <code>pg_dump</code> ile döker, <code>pg_restore</code> ile yükler ve iki taraftaki oda sayısını karşılaştırır.</p>
          <p><code>render.yaml</code> içindeki <code>DB_*</code> değerlerini yeni veritabanına göre değiştirin, parolayı Render panelinden girin ve servisi yeniden başlatın.</p>
          <p><code>/health</code> adresini ve bir odanın açıldığını kontrol edin.</p>
        </Steps>
        <Cmd>{`KAYNAK_URL='postgresql://kullanici:sifre@eski-host/veritabani' \\
HEDEF_URL='postgresql://kullanici:sifre@yeni-host/neondb?sslmode=require' \\
scripts/veritabani-tasi.sh`}</Cmd>
        <Note kind="caution">
          <p><code>pg_dump</code> sürümü kaynak sunucunun sürümünden eski olmamalıdır. Neon Postgres 18 kullanıyorsa: <code>brew install postgresql@18</code>.</p>
        </Note>
      </Section>
    </>
  )
}

export default Deploy
