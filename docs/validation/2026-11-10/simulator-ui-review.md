# Son simülatör kontrolü — 10 Kasım 2026

Ana cihaz: iPhone 17 Pro, iOS 26.5, `FFD364D8-C8F8-4886-A74A-DCB4792DD958`. Mod: `preview`. Canlı Supabase veya mağaza hesabı kullanılmadı.

## Bu çalışma sırasında gözlenenler

- Girişten önce/sonra Explore pusulası ve premium ana yerleşim korundu. Ana akışta 65 örnek Saha oyunu görünür.
- Radarın yeni ID alanına `e1` girildiğinde Moda Akşam Halı Saha detayı açıldı. Gerçek UUID/şehirden bağımsız okuma ve yetki koşulları backend/istemci testleriyle ayrıca doğrulandı.
- Bölge seçicisinde Ankara ve Çankaya uygulanınca mini haritada ÇANKAYA sokakları görüldü; aynı anda 65 mock oyun korundu. Kampüs, Şirket ve Seyahat sekmelerindeki özgün örnek ilanlar da görüntülendi.
- İlan formunun ilk adımında dört ağ seçeneği görüldü; kampüs/şirket alanları seçimle değişti ve seyahat hedefi Ankara–Çankaya’yı taşıdı.
- Apple düğmesi yalnız preview pasaportunu açtı. Üç adım geçildi. Son güncel turda yeni preview isimle %94 güven, 59 maç, 4.8 fair play, 8 hafta ve 10 örnek branş görüntülendi. Canlı yeni hesap kuralı değişmedi.
- Mesaj örnekleri, Anı Vitrini ve En Çok Beğenilen modu görüldü.
- Yarış → Turnuvalar → Kupanı oluştur formunda gerçek format/kapasite, takım başına kadro ve turnuva bölgesi alanları görüldü. İki takımlı tek eleme seçildi; isim ve tarih metni girildi.
- Fikstür ve kayıt penceresinde dört sekme görüldü. Eski mock kupada kayıt sekmesinin boş mesaj verdiği yakalandı; son kaynakta mevcut fikstür/puan tablosu isimlerini gösteren preview görünümüyle düzeltildi. Kontenjan birimi kişi yerine takım yapıldı.

## Görsel kontrolün sınırı

Son ek düzeltmeden sonra Ağ ekranını açma kontrolü sırasında CUA bağlantısı `Sky Computer Use native pipe closed before response` hatasıyla koptu. Yeniden bağlanma ve `js_reset` de aynı hatayı verdi. Bu nedenle son mock kayıt görünümü ve Ağ’daki tüm alt formlar yeniden görsel olarak doğrulanmış sayılmadı. Bu hata uygulamanın crash yaptığına dair kanıt değildir.

Son kaynakla Android/iOS derlemeleri ve 65 ortak test geçti. Swift uygulamasının yeniden kurulum/çalıştırma komut sonucu `ios-preview-build.txt` içinde ayrıca tutulur. Derleme/kurulum başarısı bütün GUI akışlarının geçtiği anlamına gelmez. Fiziksel kamera/GPS/QR/izin ve gerçek mağaza/sağlayıcı testleri tamamlanmadı.
