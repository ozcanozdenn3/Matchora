function layout(title, body) {
  return `<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${title} · Matchora</title>
  <style>
    :root {
      --bg: #ffffff;
      --card-bg: #f8fafc;
      --text: #0f172a;
      --text-muted: #64748b;
      --brand: #2563eb;
      --brand-dark: #1d4ed8;
      --border: #e2e8f0;
      --radius: 12px;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #0b0f19;
        --card-bg: #151d2f;
        --text: #f8fafc;
        --text-muted: #94a3b8;
        --brand: #3b82f6;
        --brand-dark: #60a5fa;
        --border: #1e293b;
      }
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      background: var(--bg);
      color: var(--text);
      line-height: 1.65;
      padding: 24px 16px 64px;
    }
    .container {
      max-width: 760px;
      margin: 0 auto;
    }
    .header {
      margin-bottom: 32px;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--border);
    }
    .brand {
      font-weight: 800;
      font-size: 20px;
      color: var(--brand);
      letter-spacing: -0.5px;
      margin-bottom: 8px;
    }
    h1 {
      font-size: 28px;
      font-weight: 800;
      letter-spacing: -0.5px;
      margin-bottom: 8px;
    }
    .meta {
      color: var(--text-muted);
      font-size: 13px;
    }
    .card {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 20px;
      margin: 20px 0;
    }
    h2 {
      font-size: 19px;
      font-weight: 700;
      margin: 28px 0 12px;
      color: var(--text);
    }
    p, ul, ol {
      margin-bottom: 14px;
      color: var(--text);
      font-size: 15px;
    }
    ul, ol {
      padding-left: 24px;
    }
    li {
      margin-bottom: 6px;
    }
    strong {
      color: var(--text);
    }
    a {
      color: var(--brand);
      text-decoration: none;
    }
    a:hover {
      text-decoration: underline;
    }
    .footer {
      margin-top: 48px;
      padding-top: 24px;
      border-top: 1px solid var(--border);
      font-size: 13px;
      color: var(--text-muted);
      text-align: center;
    }
    .tag {
      display: inline-block;
      background: rgba(37, 99, 235, 0.1);
      color: var(--brand);
      padding: 3px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      margin-bottom: 8px;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="brand">MATCHO<span style="color:#ef4444;">RA</span> ⚽</div>
      <h1>${title}</h1>
      <div class="meta">Son Güncelleme: 12 Kasım 2026 · Sürüm 1.0.0 · Apple App Store & Google Play Uyumlu</div>
    </div>
    ${body}
    <div class="footer">
      <p>&copy; 2026 Matchora Spor Teknolojileri. Tüm hakları saklıdır.</p>
      <p>Sorularınız için: <a href="mailto:ozden9865@gmail.com">ozden9865@gmail.com</a></p>
    </div>
  </div>
</body>
</html>`;
}

export function privacyHtml(config = {}) {
  const email = config.supportEmail || 'ozden9865@gmail.com';
  return layout('Gizlilik Politikası (Privacy Policy)', `
    <div class="tag">KVKK & GDPR & Google Play & Apple Uyumlu</div>
    <div class="card">
      <p>Matchora olarak kişisel verilerinizin güvenliğine ve gizliliğinize en üst düzeyde önem veriyoruz. Bu Gizlilik Politikası, Matchora mobil uygulamasını (iOS ve Android) ve bağlantılı servislerimizi kullanırken toplanan, işlenen ve korunan verilere ilişkin haklarınızı açıklamaktadır.</p>
    </div>

    <h2>1. Veri Sorumlusu</h2>
    <p>6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") ve Avrupa Birliği Genel Veri Koruma Tüzüğü ("GDPR") kapsamında veri sorumlusu Matchora Spor Teknolojileri'dir. İletişim: <a href="mailto:${email}">${email}</a>.</p>

    <h2>2. Toplanan Kişisel Veriler</h2>
    <ul>
      <li><strong>Hesap ve Kimlik Bilgileri:</strong> Ad, soyad, e-posta adresi, telefon numarası (isteğe bağlı SMS doğrulaması için) ve profil fotoğrafı.</li>
      <li><strong>Sporcu Profili ve İstatistikleri:</strong> Tercih edilen spor branşları (Futbol, Basketbol, Voleybol, Tenis vb.), oyuncu seviyesi, Matchora MMR (beceri derecesi), fair play değerlendirmeleri ve maç geçmişi.</li>
      <li><strong>Konum Bilgisi (Location Data):</strong>
        <ul>
          <li><em>Yaklaşık Konum (İl/İlçe):</em> Yakınınızdaki maç ilanlarını, turnuvaları ve spor tesislerini listeleyebilmek için kullanılır.</li>
          <li><em>Hassas GPS Konumu:</em> Yalnızca kullanıcı izin verdiğinde ve sahadaki check-in işlemini doğrulamak veya harita radarında anlık yakın etkinlikleri görmek için kullanılır. Konum verileriniz hiçbir zaman reklam amaçlı paylaşılmaz veya arka planda gizlice izlenmez.</li>
        </ul>
      </li>
      <li><strong>Cihaz ve Bildirim Bilgileri (Push Tokens):</strong> Apple Push Notification service (APNs) veya Google Firebase Cloud Messaging (FCM) belirteçleri. Bu belirteçler yalnızca kullanıcıya maç hatırlatmaları, acil kadro çağrıları ve sohbet mesajları iletmek için kullanılır. Kullanıcı bu bildirimleri dilediği zaman uygulama içi ayarlarından veya cihaz ayarlarından kapatabilir.</li>
      <li><strong>Kamera ve Galeri Erişimi:</strong> Profil fotoğrafı seçimi ve saha anıları ("Memories") paylaşımı sırasında kullanıcının açık rızası ve onayı ile kullanılır.</li>
    </ul>

    <h2>3. Verilerin İşlenme Amaçları</h2>
    <ol>
      <li>Sporcular arasında maç eşleştirmeleri sağlamak ve spor organizasyonlarını yürütmek.</li>
      <li>Takım içi ve maç katılımcıları arasındaki anlık mesajlaşmayı sağlamak.</li>
      <li>Saha rezervasyonlarını yönetmek ve sahadaki oyuncu check-in'lerini doğrulamak.</li>
      <li>Fair play sistemini korumak, sahte hesapları ve sportmenlik dışı davranışları engellemek.</li>
      <li>Kullanıcı tercihlerine uygun maç ve acil oyuncu arayışı bildirimleri göndermek.</li>
    </ol>

    <h2>4. Veri Güvenliği ve Saklama</h2>
    <p>Kişisel verileriniz Supabase bulut altyapısında, endüstri standardı SSL/TLS şifreleme ve gelişmiş erişim kontrolleri (Row-Level Security) ile saklanmaktadır. Verileriniz hiçbir koşulda ticari üçüncü taraf reklam ağlarına satılmaz veya kiralanmaz.</p>

    <h2>5. İlgili Kişi Hakları (KVKK & GDPR)</h2>
    <p>Kullanıcılarımız diledikleri zaman aşağıdaki haklarını kullanabilirler:</p>
    <ul>
      <li><strong>Verilere Erişim ve Dışa Aktarma:</strong> Uygulama içi Ayarlar menüsünden "Hesabı Dışa Aktar" seçeneğiyle tüm verilerinizi JSON formatında indirebilirsiniz.</li>
      <li><strong>Düzeltme:</strong> Profil bilgilerinizi dilediğiniz an güncelleyebilirsiniz.</li>
      <li><strong>Hesap ve Veri Silme (Unutulma Hakkı):</strong> Uygulama içindeki "Ayarlar -> Hesabı Sil" seçeneğiyle veya <a href="/deletion">Hesap Silme sayfamızdan</a> hesabınızı ve tüm kişisel verilerinizi kalıcı olarak silebilirsiniz.</li>
      <li><strong>Bildirim Tercihleri:</strong> Ayarlar ekranından dilediğiniz bildirim kategorisini (Maçlar, Acil Çağrılar, Sosyal vb.) açıp kapatabilirsiniz.</li>
    </ul>

    <h2>6. İletişim</h2>
    <p>Gizlilik ve veri haklarınızla ilgili her türlü soru ve talebiniz için: <a href="mailto:${email}">${email}</a></p>
  `);
}

export function termsHtml(config = {}) {
  const email = config.supportEmail || 'ozden9865@gmail.com';
  return layout('Kullanım Koşulları & Apple EULA (Terms of Use)', `
    <div class="tag">Apple Standart Lisans Sözleşmesi (EULA) & Matchora Kuralları</div>
    <div class="card">
      <p>Bu Kullanım Koşulları ve Son Kullanıcı Lisans Sözleşmesi ("Sözleşme"), Matchora mobil uygulamasını kullanımınızı düzenleyen hukuki bir anlaşmadır. Uygulamayı indirerek veya kullanarak bu koşulları kabul etmiş sayılırsınız.</p>
    </div>

    <h2>1. Apple Standart Lisans Sözleşmesi (Apple Standard EULA Hükümleri)</h2>
    <p>Matchora iOS uygulaması için Apple Inc.'in Standart EULA şartları geçerlidir (<a href="https://www.apple.com/legal/internet-services/itunes/dev/stdeula/" target="_blank">Apple Standard EULA</a>):</p>
    <ul>
      <li><strong>Taraflar:</strong> Bu Sözleşme yalnızca kullanıcı ile Matchora arasında akdedilmiştir. Apple Inc. bu sözleşmenin tarafı değildir ve uygulamanın içeriğinden sorumlu değildir.</li>
      <li><strong>Lisansın Kapsamı:</strong> Kullanıcıya, Apple App Store Kullanım Kuralları'na uygun olarak sahip olduğu Apple markalı cihazlarda Matchora'yı kişisel ve ticari olmayan amaçlarla kullanması için devredilemez, sınırlı bir lisans verilir.</li>
      <li><strong>Bakım ve Destek:</strong> Uygulamanın bakım ve desteğinden yalnızca Matchora sorumludur. Apple'ın bakım veya destek sağlama yükümlülüğü yoktur.</li>
      <li><strong>Garanti ve İade:</strong> Yasal düzenlemelerin izin verdiği azami ölçüde, Apple'ın uygulama ile ilgili başka hiçbir garanti yükümlülüğü bulunmamaktadır.</li>
      <li><strong>Ürün Talepleri:</strong> Tüketici hakları, fikri mülkiyet iddiaları ve yasal uyumluluk dahil tüm talepler doğrudan Matchora'ya iletilmelidir (<a href="mailto:${email}">${email}</a>).</li>
      <li><strong>Üçüncü Taraf Lehtar:</strong> Apple ve Apple'ın iştirakleri, bu Sözleşme'nin üçüncü taraf lehtarlarıdır ve bu Sözleşme'yi kullanıcıya karşı doğrudan uygulama hakkına sahiptir.</li>
    </ul>

    <h2>2. Fair Play, Spor Etiği ve Topluluk Kuralları</h2>
    <ul>
      <li>Matchora sportmenliği, saygıyı ve güvenilirliği esas alır.</li>
      <li>Küfür, hakaret, tehdit, ayrımcılık, cinsel taciz ve nefret söylemi içeren paylaşımlar kesinlikle yasaktır ve sıfır tolerans politikası uygulanır.</li>
      <li>Maçlara mazeretsiz katılmamak ("no-show"), sahadan sebepsiz ayrılmak veya skor manipülasyonu yapmak sporcu güvenilirlik puanının (Reliability) düşmesine ve hesabın askıya alınmasına neden olur.</li>
    </ul>

    <h2>3. Kullanıcı Tarafından Oluşturulan İçerikler (UGC)</h2>
    <p>Kullanıcılar paylaştıkları profil fotoğrafları, sohbet mesajları ve saha anılarından bizzat sorumludur. Herhangi bir uygunsuz içerikle karşılaştığınızda, içerik yanındaki "Bildir / Şikayet Et" butonunu kullanarak durumu anında bildirebilirsiniz. Moderasyon ekibimiz bildirilen içerikleri 24 saat içerisinde inceleyip gerekli tedbirleri uygular.</p>

    <h2>4. Sağlık ve Güvenlik Sorumluluk Reddi</h2>
    <p>Spor aktiviteleri fiziksel efor gerektirir ve sakatlanma riski taşır. Matchora sporcuları bir araya getiren bir platform olup, etkinlikler sırasında meydana gelebilecek fiziksel sakatlıklardan, sağlık sorunlarından veya kaza durumlarından sorumlu tutulamaz. Spor yapmaya uygun olduğunuzdan emin olunuz.</p>

    <h2>5. İptal ve İade Politikası</h2>
    <p>Saha rezervasyonları ve turnuva katılımlarında belirlenen iptal sürelerine uyulmalıdır. Organizatör tarafından iptal edilen maçlarda katılımcıların hakları iade edilir.</p>

    <h2>6. İletişim</h2>
    <p>Kullanım koşulları hakkında sorularınız için: <a href="mailto:${email}">${email}</a></p>
  `);
}

export function guidelinesHtml(config = {}) {
  const email = config.supportEmail || 'ozden9865@gmail.com';
  return layout('Topluluk Kuralları (Community Guidelines)', `
    <div class="card">
      <p>Matchora, spor tutkunlarının güvenli, saygılı ve keyifli bir ortamda spor yapabilmesi için tasarlanmıştır. Tüm sporcularımızdan bu kurallara uymasını bekliyoruz.</p>
    </div>

    <h2>1. Saygı ve Fair Play</h2>
    <p>Sahada ve sohbetlerde rakibe, takım arkadaşına, hakemlere ve tesis görevlilerine her zaman saygıyla yaklaşın.</p>

    <h2>2. Zamanında Katılım (Punctuality)</h2>
    <p>Maç saatinizden en az 10-15 dakika önce sahada olun. Gelemeyeceğiniz durumlarda en az 3 saat önceden haber verin veya yerinizi bekleme listesine devredin.</p>

    <h2>3. Sıfır Tolerans</h2>
    <p>Şiddet, tehdit, nefret söylemi ve ayrımcılık içeren davranışlar derhal hesap kapatma ile sonuçlanır.</p>

    <h2>4. İletişim ve Destek</h2>
    <p>Destek ekibimizle iletişime geçmek için: <a href="mailto:${email}">${email}</a></p>
  `);
}

export function deletionHtml(config = {}) {
  const email = config.supportEmail || 'ozden9865@gmail.com';
  return layout('Hesap ve Veri Silme Rehberi (Account Deletion)', `
    <div class="tag">Google Play & KVKK/GDPR Veri Silme Politikası</div>
    <div class="card">
      <p>Google Play Store ve KVKK/GDPR politikaları gereğince, Matchora hesabınızı ve tüm kişisel verilerinizi dilediğiniz zaman kalıcı olarak silebilirsiniz.</p>
    </div>

    <h2>Uygulama İçinden Hesabınızı Silme (Önerilen)</h2>
    <ol>
      <li>Matchora uygulamasını açın ve <strong>Profil</strong> sekmesine gidin.</li>
      <li>Sağ üstteki <strong>Ayarlar (Çark ikonu)</strong> düğmesine dokunun.</li>
      <li>Sayfanın altındaki <strong>Hesap Güvenliği</strong> bölümünden <strong>"Hesabımı Sil"</strong> seçeneğini seçin.</li>
      <li>Onayınızı verin. Hesabınız anında kapatılır ve 7 günlük yasal bekleme süresinin ardından tüm kişisel verileriniz (profil, mesajlar, fotoğraflar, maç kayıtları) veritabanlarımızdan kalıcı olarak silinir.</li>
    </ol>

    <h2>Web Üzerinden Veri Silme Talebi Gönderme</h2>
    <p>Uygulamayı sildiyseniz veya hesabınıza erişemiyorsanız, hesabınızda kayıtlı e-posta adresinizden <a href="mailto:${email}?subject=Hesap%20ve%20Veri%20Silme%20Talebi">${email}</a> adresine <strong>"Hesap ve Veri Silme Talebi"</strong> konulu bir e-posta göndererek silme talebinde bulunabilirsiniz.</p>
    <p>Talebiniz doğrulandıktan sonra 7 iş günü içinde verileriniz kalıcı olarak imha edilecek ve tarafınıza bilgi verilecektir.</p>
  `);
}

