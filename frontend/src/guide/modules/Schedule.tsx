import { Box, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

// Şekil 12.3: dört gün, her gün 210 px; 21:00 günün 21/24'ü
const DAY_W = 210
const DAY_X = 80
const dayX = (day: number, hour: number) => DAY_X + day * DAY_W + (DAY_W * hour) / 24

// Şekil 12.5: 20:50 → 21:15, dakika başına 34,4 px
const minX = (minute: number) => 60 + (minute - 50) * 34.4

// Şekil 12.6: 18 hücre
const cellX = (i: number) => 120 + i * 46

function Schedule() {
  return (
    <>
      <Section id="durum" no="12.1" title="Oda durumları">
        <Prose>
          <p>Tek kişilik ligde durum son sezonun kayıtlarından çıkar (Modül 03). Odanın ise kendi durum alanı var: <code>rooms.status</code>. Yanında bir de <code>next_event_at</code> durur: odanın sıradaki olayının zamanı. Takvim bu iki alanla çalışır.</p>
        </Prose>
        <Figure
          no="12.1"
          height={300}
          minWidth={820}
          label="Oda durumları: LOBBY, SEASON, TRANSFER_WINDOW. Lobiden sezona herkes hazır olunca, sezon içinde her gün bir maç günü, kupa finalinden sonra pencere, pencere bitince yeni sezon."
          caption="Odanın üç durumu. Yeşil kesik ok yeni sezondur. Her ok bir olaydır ve olay next_event_at zamanında işlenir. Sezon içindeki döngü her gün bir kez döner."
        >
          <Box x={20} y={120} w={160} h={64} rx={30} tone="ghost" center title="LOBBY" lines={['oda kuruldu', 'takım seçimi']} />
          <Box x={340} y={120} w={200} h={64} rx={30} tone="acc" center title="SEASON" lines={['her gün maç saatinde', 'bir maç günü']} />
          <Box x={720} y={120} w={220} h={64} rx={30} tone="sig" center title="TRANSFER_WINDOW" lines={['transferHours saat (24)', 'kapalı teklifler']} />

          <Edge from={[180, 152]} to={[337, 152]} />
          <Txt x={258} y={128} cls="g-s" anchor="middle">herkes "Hazırım"</Txt>
          <Txt x={258} y={144} cls="g-s" anchor="middle">ya da sahip: Ligi başlat</Txt>
          <Txt x={258} y={172} cls="g-s" anchor="middle">fikstür · ilk maç</Txt>
          <Txt x={258} y={188} cls="g-s" anchor="middle">≥ 10 dk sonra</Txt>

          <Edge d="M400,120 C400,58 480,58 480,117" />
          <Txt x={440} y={44} anchor="middle">maç günü (34 lig haftası + 3 kupa turu)</Txt>

          <Edge from={[540, 145]} to={[717, 145]} />
          <Txt x={628} y={137} cls="g-s" anchor="middle">kupa finali oynandı</Txt>
          <Txt x={628} y={166} cls="g-s" anchor="middle">pencere açılır</Txt>

          <Edge d="M830,184 C830,262 440,262 440,187" tone="acc" dash />
          <Txt x={635} y={258} cls="t-acc" anchor="middle">süre doldu ya da herkes "Transferlerim bitti"</Txt>
          <Txt x={635} y={276} cls="g-s" anchor="middle">teklifler çözülür → yeni sezonun fikstürü</Txt>
        </Figure>
        <GTable
          head={['OLAY', 'NE OLUR', 'SIRADAKİ next_event_at']}
          rows={[
            ['Lig başlar', 'Fikstür oluşur, durum SEASON.', 'En erken 10 dk sonraki maç saati.'],
            ['Maç günü', <><code>RoomLeague.playMatchDay</code>: sıradaki lig haftası; lig bittiyse sıradaki kupa turu.</>, 'Planlanan günün ertesi, maç saatinde.'],
            ['Kupa finali', 'Durum TRANSFER_WINDOW, pencere açılır.', 'Planlanan saat + transferHours.'],
            ['Pencere biter', <><code>startSeason</code>: teklifler çözülür, yeni fikstür.</>, 'En erken 10 dk sonraki maç saati.'],
          ]}
        />
        <Note>
          <p>Ne oynanacağını oda değil, ligin kendisi söyler. <code>RoomLeague</code> sıradaki oynanmamış haftayı veritabanından okur. Bu yüzden aynı gün iki kez oynanmaz; bir olay iki kez tetiklense bile ikinci seferde oynanacak hafta farklıdır.</p>
        </Note>
      </Section>

      <Section id="tetik" no="12.2" title="Takvimi kim ilerletir">
        <Prose>
          <p>Takvimi üç ayrı tetikleyici ilerletir. Üçü de aynı metodu çağırır: <code>RoomScheduler.tick</code>. Metot "zamanı geçmiş olay var mı?" diye bakar. Varsa işler, yoksa hiçbir şey yapmaz. Bu yüzden tetikleyicilerin çok sık ya da üst üste gelmesi sorun değildir.</p>
        </Prose>
        <Figure
          no="12.2"
          height={300}
          minWidth={860}
          label="Üç tetikleyici: dakikalık iş, GitHub Actions cron, odaya gelen her istek. Hepsi RoomScheduler.tick'i çağırır. tick oda kilidini alır ve zamanı geçmiş olayları sırayla işler."
          caption="Takvimin tetikleyicileri. Hangisi önce gelirse olayı o işler. Oda başına kilit aynı odada iki işin aynı anda çalışmasını engeller. Döngü kaçırılan olayları da sırayla işler (12.3)."
        >
          <Box x={20} y={20} w={230} h={70} tone="acc" title="RoomTickJob" lines={['@Scheduled · her dakika', 'sunucu uyanıkken']} />
          <Box x={20} y={110} w={230} h={70} tone="away" title="GitHub Actions · tick.yml" lines={['10 dakikada bir (yedek)', 'GET /internal/tick · 20 sn sınırı']} />
          <Box x={20} y={200} w={230} h={70} tone="sig" title="RoomFilter" lines={['odaya gelen her istek', 'yalnızca o oda']} />
          <Edge d="M250,55 C280,55 280,110 307,110" />
          <Edge from={[250, 145]} to={[307, 145]} />
          <Edge d="M250,235 C280,235 280,180 307,180" />

          <Box x={310} y={70} w={210} h={150} tone="sig" tag="TAKVİM" title="RoomScheduler" lines={['tickAll: zamanı gelen', 'odalar · tick(oda)', 'oda başına tryLock', 'meşgulse beklemeden döner']} />
          <Edge from={[520, 145]} to={[577, 145]} />

          <rect className="g-grp" x="580" y="16" width="360" height="270" rx="6" />
          <Txt x={596} y={38} cls="g-ms">WHILE next_event_at ≤ ŞİMDİ (EN FAZLA 60)</Txt>
          <Box x={600} y={52} w={320} h={50} center title="runEvent(oda)" lines={[{ text: 'RoomContext.call(odanın şeması, …)', mono: true }]} />
          <Box x={600} y={130} w={100} h={56} tone="acc" center title="SEASON" lines={['playMatchDay']} />
          <Box x={710} y={130} w={100} h={56} tone="sig" center title="WINDOW" lines={['startSeason']} />
          <Box x={820} y={130} w={100} h={56} tone="ghost" center title="LOBBY" lines={['olay yok']} />
          <Edge from={[650, 102]} to={[650, 127]} />
          <Edge from={[760, 102]} to={[760, 127]} />
          <Edge from={[870, 102]} to={[870, 127]} />
          <Box x={600} y={214} w={320} h={50} center title="next_event_at ilerler, oda kaydedilir" lines={['markDue: bellekteki en erken zaman']} />
          <Edge from={[650, 186]} to={[650, 211]} />
          <Edge from={[760, 186]} to={[760, 211]} />
          <Edge d="M920,239 H932 V77 H923" dash />
        </Figure>
        <Note label="NEDEN ÜÇ TETİKLEYİCİ">
          <p>Dakikalık iş yalnızca sunucu uyanıkken çalışır. Ücretsiz sunucu uyursa (Modül 13) bu iş de durur. Sunucuyu dışarıdan bir istek uyandırmalıdır: ya GitHub Actions'ın zamanlanmış işi ya da odaya giren bir oyuncu. Sunucu kendini uyanık tuttuğu için bugün asıl işi dakikalık iş yapar; diğer ikisi yedektir.</p>
        </Note>
      </Section>

      <Section id="yetisme" no="12.3" title="Kaçırılan günler">
        <Prose>
          <p>Sunucu iki gün uyumuş olsun. Uyandığı an ilk tetikleyici <code>tick</code>'i çağırır. Döngü kaçırılan maç günlerini sırayla oynar. Sıradaki gün <b>planlanan günün ertesidir</b>, "şimdi + 1 gün" değildir. Böylece takvim kaymaz: maçlar yine 21:00'de oynanır.</p>
        </Prose>
        <Figure
          no="12.3"
          height={260}
          minWidth={860}
          label="Dört günlük zaman çizgisi. Pazartesi 21:00 5. hafta zamanında oynanır. Sunucu Pazartesi 22:00'den Perşembe 10:00'a kadar uyur. Uyanınca 6. ve 7. hafta sırayla oynanır. 8. hafta Perşembe 21:00'de oynanır."
          caption="Kaçırılan günlerin yetiştirilmesi. Gri alan sunucunun uyuduğu süredir. Kırmızı kesik daireler kaçırılan maç günleridir. Uyanınca ikisi sırayla oynanır; takvim yine 21:00'e döner."
        >
          <rect className="f-muted" x={dayX(0, 22)} y="56" width={dayX(3, 10) - dayX(0, 22)} height="104" rx="4" />
          <Txt x={(dayX(0, 22) + dayX(3, 10)) / 2} y={84} cls="g-t" anchor="middle">sunucu uyuyor</Txt>
          <Txt x={(dayX(0, 22) + dayX(3, 10)) / 2} y={102} cls="g-s" anchor="middle">dakikalık iş çalışmaz</Txt>

          <line className="g-axis" x1={DAY_X} y1="170" x2={DAY_X + 4 * DAY_W} y2="170" />
          {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe'].map((day, d) => (
            <g key={day}>
              <line className="g-grid" x1={DAY_X + d * DAY_W} y1="40" x2={DAY_X + d * DAY_W} y2="190" />
              <Txt x={DAY_X + d * DAY_W + 8} y={208} cls="g-s">
                {day}
              </Txt>
            </g>
          ))}
          <Txt x={20} y={174} cls="g-ms">21:00</Txt>

          <circle className="f-acc" cx={dayX(0, 21)} cy="170" r="8" />
          <Txt x={dayX(0, 21)} y={148} cls="t-acc" anchor="middle">5. hafta</Txt>
          {[1, 2].map((d) => (
            <g key={d}>
              <circle className="g-box dan" cx={dayX(d, 21)} cy="170" r="8" strokeDasharray="3 2" />
              <Txt x={dayX(d, 21)} y={196} cls="g-s t-dan" anchor="middle">{d + 5}. kaçtı</Txt>
            </g>
          ))}
          <line className="s-acc" x1={dayX(3, 10)} y1="40" x2={dayX(3, 10)} y2="170" strokeWidth="2" />
          <Txt x={dayX(3, 10) - 6} y={34} cls="g-s" anchor="end">10:00 ilk istek → tick</Txt>
          <Box x={dayX(3, 10) + 8} y={60} w={90} h={30} tone="acc" center title="6. hafta" />
          <Box x={dayX(3, 10) + 8} y={98} w={90} h={30} tone="acc" center title="7. hafta" />
          <Edge from={[dayX(3, 10) + 53, 90]} to={[dayX(3, 10) + 53, 96]} head={false} />
          <circle className="f-acc" cx={dayX(3, 21)} cy="170" r="8" />
          <Txt x={dayX(3, 21)} y={148} cls="t-acc" anchor="middle">8. hafta</Txt>
          <Txt x={DAY_X} y={240} cls="g-s">Döngü: next_event_at = Sal 21:00 ≤ şimdi → 6. hafta · Çar 21:00 ≤ şimdi → 7. hafta · Prş 21:00 &gt; şimdi → dur.</Txt>
        </Figure>
        <Note>
          <p>Yetişen haftaların canlı yayını izlenmez; sonuçlar hemen açılır. Canlı yayın yalnızca son oynanan maç gününde ve planlanan saatten itibaren 10 dakika sürer (12.5).</p>
        </Note>
      </Section>

      <Section id="bosta" no="12.4" title="Boştayken veritabanına gitmemek">
        <Prose>
          <p>Dakikalık iş her dakika veritabanına "zamanı gelen oda var mı?" diye sorsaydı, Neon hiç uyuyamazdı. Neon'un ücretsiz planında işlem süresi sınırlıdır (Modül 13). Bu yüzden <code>RoomScheduler</code> en erken olayın zamanını bellekte tutar: <code>nextDue</code>.</p>
          <p>Saat <code>nextDue</code>'dan önceyse dakikalık iş hemen döner ve sorgu atmaz. Bir oda sıradaki olayını öne çekerse (ör. sahip maç saatini değiştirir) <code>markDue</code> bellekteki zamanı günceller.</p>
        </Prose>
        <Figure
          no="12.4"
          height={220}
          minWidth={860}
          label="20:00 ile 21:05 arasında dakikalık işin çağrıları. 21:00'den önceki çağrılar sorgu atmaz. 21:00'de sorgu ve maç günü. Neon bu arada uyur ve 21:00'de uyanır."
          caption="Bir akşamın dakikalık işi. Gri çizgiler sorgusuz dönen çağrılardır. Yeşil çizgi nextDue anıdır. Alttaki şerit Neon'un durumudur: sorgu gelmeyince 5 dakika sonra uyur."
        >
          <Txt x={20} y={58} cls="g-ms">DAKİKALIK İŞ</Txt>
          {Array.from({ length: 66 }, (_, m) => {
            const x = 150 + m * 11.8
            const due = m === 60
            return <line key={m} className={due ? 's-acc' : 'g-grid'} x1={x} y1={due ? 30 : 46} x2={x} y2="80" strokeWidth={due ? 3 : 1} />
          })}
          <Txt x={150} y={100} cls="g-s">clock &lt; nextDue → return 0 (sorgu yok)</Txt>
          <Txt x={150 + 60 * 11.8 - 6} y={24} cls="t-acc" anchor="end">21:00 nextDue → sorgu → maç günü</Txt>

          <Txt x={20} y={150} cls="g-ms">NEON</Txt>
          <rect className="f-accs" x="150" y="132" width={5 * 11.8} height="28" rx="3" />
          <rect className="f-muted" x={150 + 5 * 11.8} y="132" width={55 * 11.8} height="28" rx="3" />
          <rect className="f-accs" x={150 + 60 * 11.8} y="132" width={6 * 11.8} height="28" rx="3" />
          <Txt x={150 + 32 * 11.8} y={151} cls="g-s" anchor="middle">uyuyor (işlem süresi harcanmaz)</Txt>
          <Txt x={150} y={180} cls="g-s">20:00 son sorgu</Txt>
          <Txt x={150 + 5 * 11.8} y={196} cls="g-s">+5 dk: uyur</Txt>
          <Txt x={150 + 60 * 11.8} y={180} cls="g-s t-acc" anchor="middle">uyanır</Txt>
          {['20:00', '20:30', '21:00'].map((label, i) => (
            <Txt key={label} x={150 + i * 30 * 11.8} y={212} cls="g-m t-mut" anchor="middle">
              {label}
            </Txt>
          ))}
        </Figure>
        <Note>
          <p>Bağlantı havuzu da buna uyar. Yayın profilinde havuz boşta bağlantı tutmaz ve canlı tutma sorgusu atmaz (<code>minimum-idle: 0</code>, <code>keepalive-time: 0</code>). Bir oyuncu sayfayı açarsa sorgu gider ve Neon uyanır; bu normaldir.</p>
        </Note>
      </Section>

      <Section id="canli" no="12.5" title="Maç günü: kilit, oyun, yayın">
        <Prose>
          <p>Maç günü dört adımlı bir sahnedir. Maçtan 5 dakika önce kadrolar kilitlenir. Maç saatinde maçlar bir anda oynanır ve sonuç kaydedilir. Sonra sonuç 10 dakika boyunca canlı yayınlanır. Herkes aynı anda aynı dakikayı görür.</p>
          <p>Yayın sürerken sonuç gösterebilecek sayfalar kapalıdır. Filtre "izinliler dışında her şey kapalı" kuralıyla çalışır. Böylece sonradan eklenen bir sayfa da kendiliğinden korunur.</p>
        </Prose>
        <Figure
          no="12.5"
          height={290}
          minWidth={860}
          label="20:50 ile 21:15 arası zaman çizgisi. 20:55'te kadro kilitlenir. 21:00'de maçlar oynanır. 21:00 ile 21:10 arasında canlı yayın ve sonuç sayfaları kapalı. 21:10'da sonuçlar açılır."
          caption="Bir maç günü (maç saati 21:00). Kırmızı şeritler 409 dönen dönemlerdir. Mavi şerit canlı yayındır: 90 maç dakikası 10 gerçek dakikaya yayılır. 21:04'te bağlanan izleyici 36. dakikadan başlar."
        >
          {[
            { y: 40, label: 'KADRO / TAKTİK' },
            { y: 100, label: 'MAÇLAR' },
            { y: 160, label: 'CANLI YAYIN' },
            { y: 220, label: 'SONUÇ SAYFALARI' },
          ].map((lane) => (
            <Txt key={lane.label} x={minX(50)} y={lane.y - 6} cls="g-ms">
              {lane.label}
            </Txt>
          ))}
          <rect className="f-accs" x={minX(50)} y="40" width={minX(55) - minX(50)} height="26" rx="3" />
          <Txt x={minX(52.5)} y={58} cls="g-s" anchor="middle">değiştirilebilir</Txt>
          <rect className="f-dans" x={minX(55)} y="40" width={minX(60) - minX(55)} height="26" rx="3" />
          <Txt x={minX(57.5)} y={58} cls="g-s t-dan" anchor="middle">kilitli · 409</Txt>
          <rect className="f-accs" x={minX(60)} y="40" width={minX(75) - minX(60)} height="26" rx="3" />
          <Txt x={minX(67.5)} y={58} cls="g-s" anchor="middle">sıradaki gün için açık</Txt>

          <rect className="f-sig" x={minX(60) - 3} y="100" width="6" height="26" />
          <Txt x={minX(60) + 10} y={118} cls="g-s">tick → playWeek(n, auto=true): 1. Lig + 2. Lig maçları bir anda oynanır, liveStartedAt = 21:00</Txt>

          <rect className="f-away" x={minX(60)} y="160" width={minX(70) - minX(60)} height="26" rx="3" opacity="0.35" />
          <Txt x={minX(65)} y={178} cls="g-s" anchor="middle">90 dk → 10 dk (1 maç dakikası ≈ 6,7 sn)</Txt>
          <line className="s-strong" x1={minX(64)} y1="150" x2={minX(64)} y2="196" strokeWidth="2" />
          <Txt x={minX(64) + 4} y={208} cls="g-s">21:04'te giren: (4/10) × 90 = 36. dakika</Txt>

          <rect className="f-dans" x={minX(60)} y="220" width={minX(70) - minX(60)} height="26" rx="3" />
          <Txt x={minX(65)} y={238} cls="g-s t-dan" anchor="middle">puan durumu, fikstür, maç: 409</Txt>
          <rect className="f-accs" x={minX(70)} y="220" width={minX(75) - minX(70)} height="26" rx="3" />
          <Txt x={minX(72.5)} y={238} cls="g-s" anchor="middle">açık</Txt>

          {[50, 55, 60, 65, 70, 75].map((m) => (
            <g key={m}>
              <line className="g-grid" x1={minX(m)} y1="28" x2={minX(m)} y2="252" />
              <Txt x={minX(m)} y={272} cls="g-m t-mut" anchor="middle">
                {m < 60 ? `20:${m}` : `21:${String(m - 60).padStart(2, '0')}`}
              </Txt>
            </g>
          ))}
        </Figure>
        <GTable
          head={['PARÇA', 'NE YAPAR']}
          rows={[
            [<code key="a">Room.liveWeek, liveStartedAt</code>, 'Son oynanan maç günü ve yayının başladığı an. Erken oynatılan gün o an başlar.'],
            [<code key="b">LiveBroadcastService.streamLive</code>, 'Geçen süreyi 90 dakikaya oranlar ve o dakikadan başlar. Geç gelen ilk dakikayı beklemez.'],
            [<code key="c">RoomFilter.revealsLiveResults</code>, 'Yayın sürerken izinli olmayan GET isteklerine 409 döner.'],
            [<code key="d">LivePage, RoomStatusBar</code>, 'Canlı sayfası (hız ve atla düğmesi yok), şeritte "🔴 Canlı … İzle →".'],
          ]}
        />
      </Section>

      <Section id="ikinci" no="12.6" title="2. Lig birlikte oynanır">
        <Prose>
          <p>Fikstür iki ligi birlikte üretir. 2. Lig haftalarının numarası 200'den başlar (201, 202 …). 1. Lig'in N. haftası oynanınca 2. Lig'in <b>200 + N'e kadar</b> oynanmamış bütün haftaları da sırayla oynanır. Böylece geride kalmış bir 2. Lig ilk maç gününde yetişir.</p>
          <p>2. Lig maçları lig maçı sayılır: moral, güç, maaş ve yönetim güveni bu maçlarda da işler. 2. Lig maçı canlı oynanmaz; kadroyu yapay zekâ seçer. 1. Lig bitince 2. Lig'in kalan haftaları da oynanır, sonra sezon kapanır.</p>
        </Prose>
        <Figure
          no="12.6"
          height={240}
          minWidth={860}
          label="Üst sıra 1. Lig haftaları 1-18; 1-14 oynanmış, 15 şimdi oynanıyor. Alt sıra 2. Lig haftaları 201-218; 201-215 aynı tick içinde sırayla oynanır."
          caption="Geride kalan 2. Lig'in yetişmesi. Örnekte 2. Lig hiç oynanmamış. 1. Lig'in 15. haftası oynanınca 2. Lig'in 201–215 haftaları aynı işlemde sırayla oynanır."
        >
          <Txt x={20} y={58} cls="g-t">1. Lig</Txt>
          <Txt x={20} y={148} cls="g-t">2. Lig</Txt>
          {Array.from({ length: 18 }, (_, i) => {
            const top = i < 14 ? 'g-box ghost' : i === 14 ? 'g-box sig strong' : 'g-box'
            const bottom = i < 15 ? 'g-box acc' : 'g-box'
            return (
              <g key={i}>
                <rect className={top} x={cellX(i)} y="36" width="42" height="34" rx="3" />
                <Txt x={cellX(i) + 21} y={58} cls="g-m" anchor="middle">{i + 1}</Txt>
                <rect className={bottom} x={cellX(i)} y="126" width="42" height="34" rx="3" />
                <Txt x={cellX(i) + 21} y={148} cls="g-m" anchor="middle">{201 + i}</Txt>
              </g>
            )
          })}
          <Txt x={cellX(0)} y={26} cls="g-s">oynandı (1–14)</Txt>
          <Txt x={cellX(14) + 21} y={26} cls="g-s t-sig" anchor="middle">şimdi</Txt>
          <Edge from={[cellX(14) + 21, 70]} to={[cellX(14) + 21, 110]} tone="sig" />
          <path className="g-ed" d={`M${cellX(0)},118 V112 H${cellX(14) + 42} V118`} />
          <Txt x={(cellX(0) + cellX(14) + 42) / 2} y={104} cls="g-s" anchor="middle">200 + 15 = 215'e kadar oynanmamış haftalar, sırayla</Txt>
          <Txt x={cellX(0)} y={190} cls="g-s">MatchSimulationService.playSecondLeagueUpTo(season, 15)</Txt>
          <Txt x={cellX(0)} y={210} cls="g-s">1. Lig bitince: playSecondLeagueUpTo(…, son) → kalan 2. Lig haftaları → finishSeason</Txt>
        </Figure>
        <Note>
          <p>Puan Durumu ve Fikstür sayfalarında lig seçici var (<code>LeagueSwitch</code>, <code>?lig=2</code>). 2. Lig fikstürü salt görüntülemedir; ilk sıralar "Süper Lig'e çıkış" bölgesi olarak işaretlenir.</p>
        </Note>
      </Section>

      <Section id="ayar" no="12.7" title="Takvimi ayarlayın">
        <Steps>
          <p><b>Odalar</b> sayfasında aktif odanın <b>Takvim</b> kartına bakın; ayarlar <b>Takvim ayarları</b> bölümündedir. Kart odanın durumunu ve sıradaki olayı gösterir ("5. hafta, bugün 21:00").</p>
          <p>Oda sahibiyseniz <b>maç saatini</b>, <b>saat dilimini</b> ve <b>transfer penceresi süresini</b> değiştirin. Sezondaki odanın sıradaki maç günü yeni saate göre yeniden hesaplanır.</p>
          <p>Beklemek istemiyorsanız <b>5. hafta: şimdi oynat</b> gibi görünen düğmeye basın. Sıradaki olay hemen işlenir; takvim kaymaz, ertesi gün yine maç saatinde oynanır.</p>
          <p>Transfer penceresinde işiniz bitince <b>Transferlerim bitti</b>'ye basın. Herkes basınca pencere erken kapanır.</p>
        </Steps>
      </Section>
    </>
  )
}

export default Schedule
