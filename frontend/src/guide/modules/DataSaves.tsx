import { Box, Edge, Figure, GTable, Note, Prose, Section, Steps, Txt } from '../parts'

function DataSaves() {
  return (
    <>
      <Section id="yontem" no="10.1" title="Üç yöntem">
        <Prose>
          <p>Veri sayfası üç yöntem sunar. Kayıt noktası sunucuda kalır. Yedek dosyası kullanıcının bilgisayarına iner. CSV dosyası yalnız okumak içindir ve geri yüklenemez.</p>
        </Prose>
        <Figure
          no="10.1"
          height={250}
          minWidth={820}
          label="Veritabanından üç çıkış: kayıt noktası saves klasörüne, yedek dosyası tarayıcıya JSON olarak, CSV dosyası Excel için iner. İlk ikisi geri yüklenebilir."
          caption="Üç yöntem. Çift yönlü oklar geri yüklenebilen yöntemlerdir. Kayıt noktası bellek içi H2'de de yeniden başlatmadan sonra kalır, çünkü dosyadadır."
        >
          <Box x={20} y={90} w={180} h={80} tone="sig" strong center title="Veritabanı" lines={['tüm tablolar', '+ uploads/ logoları']} />
          <Box x={330} y={20} w={260} h={64} tone="acc" title="Kayıt noktası" lines={[{ text: 'saves/ad.json + ad.meta.json', mono: true }, 'sunucuda, adlandırılmış']} />
          <Box x={330} y={98} w={260} h={64} tone="acc" title="Yedek dosyası" lines={[{ text: 'GET /api/data/export', mono: true }, 'tarayıcıya iner (JSON)']} />
          <Box x={330} y={176} w={260} h={64} title="CSV" lines={[{ text: 'GET /api/data/csv/…', mono: true }, 'Excel için, yalnız okuma']} />
          <Edge from={[200, 115]} to={[327, 52]} both tone="acc" />
          <Edge from={[200, 130]} to={[327, 130]} both tone="acc" />
          <Edge from={[200, 145]} to={[327, 208]} />
          <Box x={660} y={20} w={280} h={64} lines={['Kaydet · Yükle · Sil', 'meta: takım sayısı, sezon, oynanan maç']} />
          <Box x={660} y={98} w={280} h={64} lines={['Yedeği indir · Yedek yükle…', 'başka bir bilgisayara taşınabilir']} />
          <Box x={660} y={176} w={280} h={64} lines={['sezon seçilerek indirilir', 'noktalı virgül ayraç, UTF-8 BOM']} />
          <Edge from={[590, 52]} to={[657, 52]} thin />
          <Edge from={[590, 130]} to={[657, 130]} thin />
          <Edge from={[590, 208]} to={[657, 208]} thin />
        </Figure>
      </Section>

      <Section id="yedek" no="10.2" title="Yedek nasıl çalışır">
        <Prose>
          <p>Yedek kodda tablo listesi tutmaz. <code>BackupService</code> tabloları veritabanının kendisinden (JDBC metadata) bulur. Bu nedenle yeni bir tablo kod değişmeden yedeğe girer. Kayıt noktası da aynı yedek biçimini kullanır.</p>
        </Prose>
        <Figure
          no="10.2"
          height={320}
          minWidth={820}
          label="Dışa aktarma: tablolar bulunur, yabancı anahtar sırasına dizilir, her tablo sütun ve satırlarıyla yazılır, logolar base64 eklenir. İçe aktarma: dosya doğrulanır, tüm tablolar boşaltılır, satırlar kendi id'leriyle eklenir, id sayaçları ayarlanır."
          caption="Dışa ve içe aktarma adımları. İçe aktarma tek transaction'dır. Dosya bozuksa veya sürümü farklıysa yanıt 400 olur ve veri bozulmaz."
        >
          <Txt x={20} y={24} cls="g-ms">DIŞA AKTARMA · export</Txt>
          {[
            ['Tabloları bul', 'JDBC metadata'],
            ['Sırala', 'bağlı tablo sonra gelir'],
            ['Satırları yaz', 'sütunlar + satırlar'],
            ['Logoları ekle', 'base64 metin'],
          ].map(([title, sub], i) => (
            <g key={title}>
              <Box x={20 + i * 235} y={34} w={210} h={60} tone="acc" tag={`${i + 1}`} title={title} lines={[sub]} />
              {i < 3 && <Edge from={[230 + i * 235, 64]} to={[252 + i * 235, 64]} />}
            </g>
          ))}
          <Box x={300} y={124} w={360} h={56} tone="sig" center title="JSON dosyası" lines={[{ text: '{ formatVersion: 1, summary, tables, logos }', mono: true }]} />
          <Edge d="M835,94 V152 H663" />
          <Edge d="M480,180 V210" />
          <Txt x={20} y={226} cls="g-ms">İÇE AKTARMA · import</Txt>
          {[
            ['Doğrula', 'biçim ve sürüm', 'dan'],
            ['Boşalt', 'tüm tablolar', ''],
            ['Ekle', 'satırlar kendi id\'leriyle', ''],
            ['Sayaçları ayarla', 'en büyük id\'nin üstü', ''],
          ].map(([title, sub, tone], i) => (
            <g key={title}>
              <Box x={20 + i * 235} y={236} w={210} h={60} tone={tone ? 'dan' : 'plain'} tag={`${i + 1}`} title={title} lines={[sub]} />
              {i < 3 && <Edge from={[230 + i * 235, 266]} to={[252 + i * 235, 266]} />}
            </g>
          ))}
          <Edge d="M480,210 H125 V233" />
        </Figure>
        <GTable
          head={['UÇ NOKTA', 'GÖREV']}
          rows={[
            [<code key="k76_15">GET /api/data/export</code>, 'Tüm ligi JSON olarak indirir.'],
            [<code key="k77_15">POST /api/data/import</code>, 'JSON dosyasını yükler (multipart, en fazla 200 MB).'],
            [<code key="k78_15">GET /api/data/saves</code>, 'Kayıt noktalarını listeler, en yeni başta.'],
            [<code key="k79_15">POST /api/data/saves</code>, 'Kayıt noktası oluşturur. Aynı ad varsa üzerine yazar.'],
            [<code key="k80_15">POST /api/data/saves/{'{ad}'}/load</code>, 'Kayıt noktasını yükler.'],
            [<code key="k81_15">DELETE /api/data/saves/{'{ad}'}</code>, 'Kayıt noktasını siler.'],
          ]}
        />
      </Section>

      <Section id="kayit-al" no="10.3" title="Kayıt noktası alın">
        <Steps>
            <p>Veri sayfasını açın.</p>
            <p>"Kayıt noktaları" bölümünde bir ad yazın. Örnek: <code>sezon-3-baslangic</code>.</p>
            <p>"Kaydet" düğmesine basın. Tabloda yeni satırı kontrol edin: takım sayısı, sezon, oynanan maç.</p>
        </Steps>
        <Note kind="caution">
          <p>Kayıt noktası yüklemek mevcut tüm veriyi siler ve kayıttaki veriyi geri koyar. Gerekirse önce mevcut durumu ayrı bir kayıt noktasına kaydedin. Yükleme bir onay ister: "Evet, yükle".</p>
        </Note>
        <Note>
          <p>Kayıt adı yol ayırıcısı içeremez. <code>../</code> gibi bir ad <code>400</code> yanıtı alır. Bu kural <code>saves/</code> klasörünün dışına yazmayı önler.</p>
        </Note>
      </Section>

      <Section id="csv" no="10.4" title="CSV dosyaları">
        <Prose>
          <p>CSV dosyaları Türkçe Excel ile doğru açılır. Ayraç noktalı virgüldür. Dosya UTF-8 BOM ile başlar. Mevki adları Türkçedir. Kullanıcı sezonu seçerek indirir.</p>
        </Prose>
        <GTable
          head={['DOSYA', 'İÇERİK']}
          rows={[
            [<code key="k107_15">standings</code>, 'Puan durumu: sıra, takım, O, G, B, M, A, Y, AV, P.'],
            [<code key="k108_15">fixture</code>, 'Tüm haftalar ve maçlar, skorlar.'],
            [<code key="k109_15">players</code>, 'Oyuncu istatistikleri: gol, asist, kart, dakika.'],
          ]}
        />
      </Section>

      <Section id="sema" no="10.5" title="Şema değişikliğinde veriyi taşıyın">
        <Prose>
          <p>Kalıcı veritabanı (<code>h2file</code> veya PostgreSQL) <code>ddl-auto: update</code> kullanır. Bu ayar yeni sütun ekler ama bazı kısıtları kaldıramaz. Yeni bir sürüm şemayı değiştirirse bu prosedürü uygulayın.</p>
        </Prose>
        <Steps>
            <p>Eski sürüm çalışırken Veri sayfasından "Yedeği indir" düğmesine basın.</p>
            <p>Backend'i durdurun.</p>
            <>
              <p>Veritabanı klasörünü silin.</p>
              <Note kind="caution">
                <p>Bu adım tüm veriyi siler. 1. adımdaki yedek dosyasının indiğini kontrol edin.</p>
              </Note>
            </>
            <p>Yeni sürümle backend'i başlatın.</p>
            <p>Veri sayfasından "Yedek yükle…" düğmesine basın ve 1. adımdaki dosyayı seçin.</p>
        </Steps>
      </Section>
    </>
  )
}

export default DataSaves
