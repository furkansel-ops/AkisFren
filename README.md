# AkışFren — Android 0.4.0

**Daha az kaydır, daha çok yaşa.** Tamamen çevrimdışı çalışan, Android erişilebilirlik hizmetiyle *tanımlanabilen* Instagram Reels, Instagram ana akış, YouTube Shorts ve TikTok ekranlarında seçilen süre limitlerini uygulamayı deneyen bir açık kaynak Android proje taslağıdır.

> **DURUM (16 Ekim 2026):** Android debug APK, GitHub Actions üzerinde `testDebugUnitTest assembleDebug` ile **başarıyla derlendi ve otomatik testler geçti**. [Başarılı derlemeden APK'yı indir](https://github.com/furkansel-ops/AkisFren/actions/runs/38950580130) (Artifacts → AkisFren-debug-apk). Uygulama **gerçek Android cihazda henüz test edilmedi**; Instagram, YouTube ve TikTok üzerinde doğrulanmadan güvenilir bir engelleyici olarak kullanılmamalıdır. Bu repo kaynak koddur; Actions çıktısı debug APK'dır, Google Play yayını değildir.

## v0.4 yenilikleri

- **Her akış için ayrı günlük limit:** Instagram Reels, Instagram ana akış, Shorts ve TikTok'a ayrı ayrı 5 / 15 / 30 / 60 dakikalık hedef atanır. Her hedefin limiti isteğe bağlı olarak açılır; varsayılan kapalıdır. Günlük limit dolunca yalnız o tanınmış hedef ekran gece yarısına kadar kısıtlanır. Limiti devre dışı bırakmak veya yükseltmek etkisini hemen kaldırır. Günlük limit dolumu günde bir defa mola olarak raporlanır.
- **Nazik %80 uyarısı:** Sürekli izleme oturumu kendi limitinin %80'ine gelince tek seferlik Android bildirim balonu (Toast) gösterilir; varsayılan açık, **Alışkanlıklar** bölümünden kapatılabilir. İşletim sistemi arka plandaki Toast görünürlüğünü kısıtlayabilir.
- **Veriler kullanıcıda:** **Analiz** ekranından 90 günün tarih/hedef/mola sayılarını içeren CSV dosyası Android belge oluşturma penceresiyle kullanıcının seçtiği yere kaydedilebilir. Tüm yerel verileri kalıcı silme ayrı onay ister; erişilebilirlik iznini kaldırmaz.
- **Daha güvenilir sıfırlama:** Veriler sıfırlanınca kayıtlı aktif oturumun durumu temizlenir, süresi dolmuş mola ekranı kapatılır. Eski kullanıcıların mevcut ayarları korunur.
- **Web demosu:** Yeni hedef bazlı bütçe, %80 uyarısı, CSV indirme ve bütün demo verilerini silme eklenmiştir. Web, **gerçek uygulamaları izlemez veya engellemez**. Tarayıcı demosunun önceki sürümlerinde geçmiş günler için yalnızca toplam süre tutulduğundan, eski günlerin hedef bazlı kolonları boş bırakılır.

### v0.4 test durumu

- Saf Java UsagePolicy kontrolleri: **17/17 geçti** (mola üst sınırı, varsayılan kapalı bütçeler, %80 sınırı, uyku/ders modları).
- Java kaynaklarının `javac` parse kontrolü: **8 kaynak, 0 sözdizimi hatası**. XML kaynakları da ayrıştırıldı.
- Etkileşimli demo: Chromium otomasyonunda 4 hedef, günlük limit açma/kapatma, limit dolumu, %80 uyarısı, CSV ve tam silme akışında tarayıcı hatası gözlenmedi.
- **GitHub Actions Gradle derlemesi ve testler geçti.** Gerçek cihaz testi **yapılmadı**. Erişilebilirlik servisinin üretici, Android sürümü ve Instagram/YouTube/TikTok güncellemelerine göre davranışı değişebilir.

## Özellikler ve uygulanma durumları

| # | Özellik | Android taslak kodu | Tarayıcı demosu | Dikkat |
|---|---|---|---|---|
| 1 | Akıllı Fren ekranı | Erişilebilirlik overlay, kalan mola, kapat/istisna | Etkileşimli modal | Android üreticisi davranışı farklı olabilir |
| 2 | Dijital alışkanlık analizi | Günlük, son 7 gün, saatlik veri | Grafikler/simülasyon | Yalnızca tanınmış ekranlar |
| 3 | XP, seviye, başarımlar | Mola bitişi ve odak bitişi ödülleri | Aynı kural | Gerçek davranışı doğrulamaz |
| 4 | Neden açtın? | İsteğe bağlı giriş sorusu, son seçim | İsteğe bağlı modal | Mesajlar okunmaz, seçim izinleri değiştirmez |
| 5 | Günlük kaydırma bütçesi | Dört akışın toplamı; gece yarısına kadar kısıtlama | Simülasyon | Varsayılan kapalı, kapatınca kaldırılır |
| 6 | Artan mola süresi | 1×, 2×, 3×; 30 dk üst sınır | Simülasyon | Varsayılan kapalı |
| 7 | Odak modları | Ders/uyku/iş/serbest ve isteğe bağlı saat planı | Aynı kurallar | Otomatik zamanlama varsayılan kapalı |
| 8 | Zaman bankası | 15/25 dk odak timer; banka kredi kazan/harca | Hızlı test ile denenebilir | Odak süresi akademik çalışma kanıtı değildir |
| 9 | Acil istisna | Günde 1 kez 5 dk, seçilen hedef; bankadan da kullanılabilir | Aynı | Sadece seçilen hedef için |
| 10 | Ana ekran widget'ı | Android widget XML + AppWidgetProvider | Widget önizlemesi | Android widget OS tarafından 30 dakikada bir de güncellenebilir |
| 11 | Haftalık rapor | Son 7 gün ve önceki 7 gün + paylaş | Grafik/karşılaştırma | Otomatik bildirim değil, uygulama içi rapor |
| 12 | Akıllı öneri | Son 7 günün yoğun kullanım saatinden kural tabanlı öneri | Aynı | En az 10 dk veriye ihtiyaç duyar |
| 13 | Akışa özel günlük limit | İsteğe bağlı her hedef için ayrı günlük sayaç ve bütçe | Aç/kapat, 5/15/30/60 dk | Sadece tanınan hedefte geçerli |
| 14 | Nazik %80 uyarısı | Oturum içinde bir defa Toast | Simülasyon bildirimi | Cihaz görünürlüğü değişebilir |
| 15 | Veri kontrolü | SAF ile 90 günlük CSV ve tüm kayıtları onayla sil | Demo CSV ve sıfırla | Erişilebilirlik izni otomatik kaldırılmaz |

**Uygulama ekranları:** Kontrol, Alışkanlıklar, Analiz. Tema: gece laciverti / turkuaz, responsive koyu kartlar. Eski sürümdeki ayarlar yerel SharedPreferences üzerinde korunur.

### Temel çalışma mantığı

- Erişilebilirlik hizmeti **kullanıcı Android Ayarlarından açıkça izin verdiğinde** çalışır. Hizmet yalnızca seçilmiş uygulamaların erişilebilirlik öğelerine bakarak hedef ekranı ayırt etmeye çalışır.
- Instagram'da tanınamayan sayfalar (örneğin bazı mesaj/profil ekranları) korunma adına **engellenmez**. YouTube'da yalnızca tanınan Shorts ekranı sayılır. TikTok'ta paket bazlı izleme yapılır, diğer TikTok bölümleri ayrıştırılmaz.
- Bir oturumda limit dolunca ana ekrana dönüş dener; erişilebilirlik overlay'i süre/istisna seçenekleri gösterir. Molanın dolmasını cihaz saati üzerinden takip eder, **Android sisteminin resmi bir yasaklama özelliği değildir.**
- Günlük bütçe toplam tanınmış hedef ekran süresidir. Süre sınırı geçildiğinde otomatik mola gece yarısına kadar sürer, **bütçe özelliğini kapatınca** bu kural kalkar.
- Mola bitince her hedef için bir defa XP yazılır; acil geçiş/ödül karşılığı istisna verilirse o moladan ödül verilmez. XP bir üretkenlik ölçüsü değildir.
- Odak timer'ı Android monotonik saatiyle takip edilir. Yeniden başlatma sonrası sürenin tutarsız olduğu tespit edilirse oturum geçersiz kılınır; ödül yalnızca kullanıcı süre dolduktan sonra talep edince yazılır.
- Haftalık rapor ve öneriler sadece uygulamanın kendisinin tuttuğu yerel sayaçlardan çıkar; tahmini "kazanılmış zaman" veya tüm telefon kullanımını temsil etmez.

## APK'ya dönüştürme

### Yöntem 1 — GitHub Actions
1. Bu depo Android kaynaklarını ve `.github/workflows/android-apk.yml` derleme otomasyonunu içeriyor.
2. GitHub > Actions > **Build AkışFren APK** > Run workflow. Ya da `main` veya `master` dalına gönder.
3. Başarılı olursa **AkisFren-debug-apk** adlı çıktıdan `app-debug.apk` dosyasını indir.
4. APK'yı güvenilir cihazda test et ve izin açıklamasını oku. Bu debug APK **Google Play yayın paketi değildir**.

### Yöntem 2 — Android Studio
- JDK 17, Android SDK Platform 35, Gradle 8.11.1 gereklidir (AGP 8.9.2).
- Kaynak klasörünü Android Studio ile aç. Bu projede Gradle wrapper JAR bulunmadığından Android Studio'nun kullandığı Gradle kurulumunu seçebilir veya GitHub Actions yolunu kullanabilirsin.
- Önce `testDebugUnitTest`, sonra `assembleDebug` görevlerini çalıştır.

### Telefonda kontrol listesi

1. AkışFren'i yükle, **Kontrol > Koruma iznini aç** butonundan erişilebilirlik iznini yönet. Android 13+ kısıtlı ayar uyarısı gösterirse izin konusunda sistemin güvenlik bilgilerini oku.
2. Reels limitini örneğin 5 sn yap. Instagram Reels ekranını açıp servis tarafından tanınıp tanınmadığını ve mola ekranını kontrol et.
3. Instagram DM/profil ekranında yanlışlıkla mola başlatıp başlatmadığını kontrol et; sorun varsa izni kapat.
4. Gelişmiş ayarlardan günlük bütçeyi **bilerek** aç. Özelliği kapattığında günlük bütçe kısıtlaması kalkmalı.
5. Banka kredisi, acil istisna, gece yarısı, odak oturumu bitişi, yeniden başlatma ve widget güncellemelerini test et.

## Gizlilik ve izinler

- Android manifestinde **INTERNET izni yoktur**; bulut verisi, takip SDK'sı ve hesap sistemi bulunmaz.
- Erişilebilirlik öğelerinin ekran metinleri sadece anlık hedef tespiti için RAM'de incelenir; **metin içerikleri saklanmaz**.
- Yerel kullanım sayıları, ayarlar, mola saatleri, istisnalar, XP, saatlik istatistikler **cihazın uygulama özel ayar alanında** saklanır. Uygulamayı kaldırmak veri kaybına sebep olabilir.
- Google Play dağıtımı öncesi Accessibility API politikası, izin açıklaması, açık rıza ve mağaza metinleri ayrıca incelenmelidir. Hizmet `isAccessibilityTool=false` kullanır.
- Kullanıcı hizmeti Android Ayarlarından istediği an kapatabilir. Uygulama cihaz yönetici yetkisi istemez, ayarları kilitlemez.

## Kaynaklar

- `app/src/main/java/dev/akisfren/guard/AppSettings.java` — konfigürasyon, güvenli yerel sayaçlar, raporlar, XP, odak.
- `FocusMonitorService.java` — hedef ekran kontrolü, geçici mola, kullanıcıya gösterilen erişilebilirlik panelleri.
- `SurfaceDetector.java` — belirsizlikte işlem yapmayan ekran tespitçisi.
- `UsagePolicy.java` — saf Java bütçe, zamanlama, ceza ve odak modu kuralları.
- `AkisFrenWidget.java` — widget güncellemesi.
- `MainActivity.java` — sekmeler ve etkileşimli kontroller.
- `app/src/test/java/...` — ekran tespit ve kullanım politikası birim testleri.
- `web-demo/index.html` — bağımsız, **gerçek uygulama engellemeyen** etkileşimli tarayıcı demo.

**Sürüm:** v0.4.0 — Henüz fiziksel cihazda doğrulanmadı. Otomatik haftalık bildirim ve tam sürüm Instagram/TikTok ekran ayrımı bu kaynakta **tamamlanmış özellikler değildir**.
