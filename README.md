# Draw On Screen v4.0

Ekran üzerine çizim uygulaması. Hem **parmak** hem **stylus** ile çalışır.

## Düzeltilen Sorunlar (v4.0)
- ✅ Android 14+ crash fix: `startForeground` hemen çağrılıyor
- ✅ Settings açılmıyor crash düzeltildi
- ✅ Overlay ekranda görünmüyor sorunu düzeltildi
- ✅ Parmakla çizim artık varsayılan olarak aktif (stylus zorunlu değil)
- ✅ Logo artık tam ortada
- ✅ AMOLED siyah tema (#000000 arka plan)
- ✅ Launcher ikonu ince kalem

## Yeni Özellikler
- Araç çubuğunu sağa/sola taşıma ayarı
- Çizgi düzleştirme (Smoothing) ayarı
- Basınç hassasiyeti ayarı
- Titreşim API düzeltmesi (Android 12+)
- try-catch ile sağlam hata yönetimi

## Termux Build
```bash
cd DrawOnScreen_new
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Push
```bash
git add -A
git commit -m "v4.0: crash fixes, AMOLED theme, finger draw fix"
git push origin main
```
