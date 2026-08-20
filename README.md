# GregoTV

Centro multimedia estilo Netflix para Android TV. Unifica archivos locales, red
SMB e IPTV (HLS) en una sola UI con navegación por D-pad, hecha con Jetpack
Compose for TV y Media3.

## Características

- Solo Android TV (leanback). minSdk 23, targetSdk 34.
- IPTV por 12 listas M3U públicas y legales, curadas para contenido en
  español (editables en Ajustes).
- Filtro "solo español" activado por defecto (toggle en Ajustes); las
  fuentes que el usuario añade están exentas.
- Filtro NSFW por `group-title`, desactivado por defecto y con toggle.
- Reproductor Media3 con HLS y "continuar viendo" (posición recordada).
- Fuentes locales (MediaStore) y red SMB (jcifs-ng).
- Favoritos y progreso en Room. Ajustes en DataStore.
- TMDB desactivado: la app arranca sin API key y usa los logos del M3U.

## Compilar en local

Necesitas Android SDK (compileSdk 34) y JDK 17.

```bash
# 1. Genera el wrapper (el jar binario no está en el repo)
gradle wrapper --gradle-version 8.7

# 2. Compila el APK debug
./gradlew assembleDebug
```

APK resultante: `app/build/outputs/apk/debug/app-debug.apk`.

Opcional, activar TMDB: pasa la key como propiedad de Gradle.

```bash
./gradlew assembleDebug -PTMDB_API_KEY=tu_key
```

## APK autodescargable (CI)

Cada push a `main` dispara `.github/workflows/build-apk.yml`, que compila el APK
y publica una Release marcada como "latest". El asset se llama siempre
`GregoTV.apk`, así que esta URL estable apunta SIEMPRE al APK más reciente:

```
https://github.com/gregovaz29-sketch/gregotv/releases/latest/download/GregoTV.apk
```

Esa URL es abrible desde el navegador de la TV para instalar directamente.

## 📺 Instalar en la TV con Downloader (código numérico)

La forma más cómoda en Android TV / Fire TV: un código numérico corto.

- Requisito: el repositorio de GitHub debe ser **PÚBLICO**.
- URL estable de descarga (siempre la última versión):

```
https://github.com/gregovaz29-sketch/gregotv/releases/latest/download/GregoTV.apk
```

- Paso 1: entra en https://go.aftvnews.com/ y pega esa URL. Te devuelve un
  **código numérico**.
- Paso 2: en la TV instala **"Downloader by AFTVnews"** desde Google Play.
- Paso 3: abre Downloader, escribe el código numérico y pulsa **Go**. Descarga
  el APK e instala.
- Nota: el mismo código sirve para futuras versiones, porque la URL `latest`
  siempre entrega el APK más reciente.

## Instalar en la TV (alternativas)

1. Ajustes de Android TV: activa "Fuentes desconocidas" para el navegador o
   para "Downloader".
2. Abre la URL estable (arriba) desde el navegador de la TV, o instala vía adb:

```bash
adb install -r GregoTV.apk
```

## Listas IPTV por defecto

12 fuentes públicas y legales: iptv-org (España y Latinoamérica: mx, ar,
co, cl, pe, ve, ec, uy, pr; y `languages/spa`) y `tdtchannels`.
No se incluyen repos que retransmitan canales de pago o con copyright.
Edítalas en Ajustes.

Los tres FAST de `i.mjh.nz` (Pluto, Samsung TV+, Plex) se retiraron: ese
host dejó de publicar M3U y ahora solo sirve EPG en XMLTV, así que las
tres URLs devolvían 404.

La lista viva está en `DefaultLists.IPTV`
(`app/src/main/java/com/gregotv/data/settings/SettingsRepository.kt`); el
paso de diagnóstico del CI la lee de ahí para no desincronizarse.

## Estructura

```
app/src/main/java/com/gregotv/
  GregoTvApp.kt        Application + Hilt
  MainActivity.kt      Nav graph (home/detail/player/settings)
  MediaRepository.kt   Unifica local + SMB + IPTV
  model/               Modelo unificado MediaItem
  data/                M3uParser, SmbScanner, LocalScanner, Room, settings, tmdb
  ui/                  theme, home, components, player, detail, settings
```
