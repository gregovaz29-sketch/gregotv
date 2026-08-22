# GregoTV — Traspaso de sesión

Fecha: 2026-08-22. Estado para retomar en un chat nuevo sin perder contexto.
Fuente de verdad del repo: `ESTADO_GREGOTV.md`. Este fichero es el resumen
vivo de la rama `mejoras/premium`.

## Qué es

App Android TV nativa (Leanback). Reproduce canales IPTV (M3U/HLS legales),
vídeos locales (MediaStore) y red SMB. Kotlin + Compose + tv-material +
Media3/ExoPlayer + Hilt + Room + DataStore + Coil. Paquete `com.gregotv`.

- Repo: https://github.com/gregovaz29-sketch/gregotv (público)
- Rama de trabajo: `mejoras/premium` (18 commits sobre `main`, HEAD `5aab0e9`)
- PR abierto sin mergear: https://github.com/gregovaz29-sketch/gregotv/pull/1
- APK de prueba (prerelease, no toca el `latest` de la TV):
  https://github.com/gregovaz29-sketch/gregotv/releases/download/premium-preview/GregoTV.apk
- No hay Android SDK ni gradlew en la máquina de dev. TODO se compila por CI.

## Reglas del entorno (no romper)

- No compilar en local. CI en GitHub Actions.
- No tocar `main`. Trabajar en `mejoras/premium`.
- Español prioritario. Nada de Kodi, addons ni fuentes pirata.
- El `Publish release` del CI está blindado a `main` (`if: github.ref ==
  'refs/heads/main'`). Las builds de rama NO publican `latest`.
- Fuentes del usuario van a DataStore, nunca a DefaultLists, nunca se commitean.

## Commits de la rama (main -> HEAD)

```
5aab0e9 Add a local upload page so lists can be pasted from a phone
2648248 Reject non-URL lines so an HTML response can't become fake channels
87e26b8 Guard every background write so a failed save cannot kill the app
0b7a6ad Allow cleartext streams and record crashes to a file
43af741 Commit the exported Room schema for v2
d08a0f3 Test the Room migration on an emulator instead of guessing
f4c6e2d Sign every build with the same key so updates install over each other
1fffdf0 Read the tvg-id variant, so Spanish feeds stop being labelled German
2f7e08f Keep the best variant of each channel and split them by country
5689da5 Drop the hard Spanish filter; language stays an ordering signal
ebcf67e docs: cifras finales de 12 listas y decisión sobre spanishOnly
6e2b801 Fix unclosed KDoc comment that broke the whole build
8e36ebb Gate the release to main, drop the dead i.mjh.nz feeds, single status doc
045f925 Add an open source system: user M3U lists and Xtream Codes accounts
82961e0 Rebuild the home screen as a Netflix-style dashboard
76d0018 Curate genre rules and shrink the origin buckets
f1d2279 Quarantine channels that fail to play
6dfddd2 Curate default lists to Spanish and add spanishOnly toggle
```

## Qué se ha hecho, con cifras (todo MEDIDO contra las 12 listas reales)

- **Catálogo recortado a español**: 12 listas (es, spa, tdtchannels, + 9 países
  LatAm). Fuera index.m3u global, categories/, e i.mjh.nz (muerto, sirve solo
  EPG XMLTV ahora). 20.600 brutos -> 4.242 -> **2.653 únicos**.
- **spanishOnly RETIRADO**: era filtro duro que contradecía el recorte en
  origen y escondía en silencio las listas del usuario. `MediaItem.spanish`
  queda solo como criterio de ORDEN (sortedWith), no de censura.
- **Dedupe determinista** (`ChannelDedupe.kt`): elige la MEJOR variante
  (resolución declarada + tags [Not 24/7]/[Geo-blocked] + desempate total por
  título/url). Arregla dos bugs: descartaba calidad (28 casos, La 1 1080p sobre
  La 1 UHD) e inestabilidad por orden de Set (124 canales cambiaban entre
  arranques). Medido: 0 inestables ante 30 permutaciones, sin migrar DataStore.
- **País en la clave de dedupe + sufijo visible**: TVN (Chile)/(Panamá)/(R.
  Dominicana) ya no colapsan. País del tvg-id con whitelist (el .TV de
  tdtchannels es marca, no Tuvalu) y con variante @ES/@LatAm que manda sobre el
  código (feeds Pluto en español bajo .de/.us: "Pluto TV Anime (España)", no
  "(Alemania)"). 132 canales con sufijo.
- **Clasificador**: cajones de origen 43,8% -> 35,1%. Fila "Autonómicas y
  locales" para españolas sin género.
- **Dashboard tipo Netflix**: TopNav (Inicio/Canales/Películas/Series/Buscar/
  Ajustes), Billboard rotatorio que pausa con foco, carpetas por categoría,
  buscador, tarjetas con fallback (inicial+tinte) sin imágenes rotas.
- **Fiabilidad**: tabla `channel_health`, cuarentena 3 días de canales que
  fallan al reproducir. Room v2 con `MIGRATION_1_2` real (solo CREATE TABLE,
  no toca favorites/progress), sin fallbackToDestructiveMigration.
- **Fuentes ampliables**: Ajustes acepta M3U y Xtream Codes (player_api.php,
  solo live). Cliente genérico y vacío. Cero Kodi/credenciales (verificado).

## Bugs de la TV arreglados (última tanda)

- **BUG cleartext** (`0b7a6ad`): network_security_config bloqueaba http. 542
  canales (12,8%, 118 hosts) daban ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED. Ahora
  base-config cleartextTrafficPermitted="true" para todos. IMPORTANTE Fase 6:
  el CI prueba con curl sin esta restricción -> habría marcado VIVOS canales que
  en la TV fallan.
- **CrashReporter** (`0b7a6ad`): Thread.setDefaultUncaughtExceptionHandler
  escribe stacktrace a fichero. Ajustes -> "Ver último error" lo muestra.
- **safeLaunch** (`87e26b8`): 9 viewModelScope.launch blindados con
  CoroutineExceptionHandler (saves de Room/DataStore ya no tumban el proceso).
  Sin tocar HomeViewModel.load() (ya tiene try/catch) ni el stateIn.
- **Parser anti-basura** (`2648248`): línea sin "://" ya no se vuelve canal.
  HTML de 5000 líneas -> 0 canales.

## Firma / keystore (CERRADO)

- Keystore fijo commiteado: `app/gregotv-debug.keystore`, alias gregotv,
  pass android. `signingConfig` con override por env GREGOTV_KEYSTORE.
- Verificado: builds #10, #11, #14 misma clave pública `8f3d13c2…`. Instalan
  encima sin desinstalar.
- Es SOLO debug y es PÚBLICA (repo público). La release firmada irá en SECRET
  de GitHub, JAMÁS con esta clave. (Anotar en ESTADO_GREGOTV.md.)
- El build-4 de la TV usaba clave efímera vieja: la PRIMERA instalación del
  keystore fijo (#11) sí exigió desinstalar. De ahí en adelante, no.

## Tests / CI

- `build-apk.yml`: assembleDebug, diagnóstico IPTV (URLs parseadas de
  DefaultLists.IPTV, no hardcodeadas), sube artifact GregoTV-APK.
- `migration-tests.yml`: emulador API 30, connectedDebugAndroidTest. Test
  `Migration1To2Test` crea BD v1 con favorito+progress, migra, comprueba que
  sobreviven y que channel_health funciona. VERDE (run #4). exportSchema
  activado, `app/schemas/...AppDatabase/2.json` commiteado.

## Cola de trabajo (NO empezado)

1. **Fase 6 — Verificador de streams** (arquitectura híbrida, especificada):
   - CI: workflow verify-streams.yml cada 6h, prueba cada stream (GET Range,
     timeout 6s, conc 40), genera verified.json en rama huérfana `data`,
     servido por raw.githubusercontent.
   - App: StreamStatusRepository lee verified.json (TTL 6h, caché, nunca
     bloquea). MediaItem.verified (OK/DEAD/UNKNOWN). En el desempate de la
     dedupe, verified==OK como PRIMER criterio, por encima de resolución. NO
     esconder por el CI (geo-bloqueo español da falsos negativos).
   - Aprendizaje local: tabla stream_health (v2->v3, CREATE TABLE IF NOT
     EXISTS). PlayerViewModel: error -> failCount++, >10s ok -> reset. Ajuste
     por defecto OFF "Ocultar canales que me han fallado" (failCount>=3 local).
   - AVISO: la v2->v3 apila sobre la v1->v2. Usar el MigrationTestHelper con
     el 2.json ya exportado.
2. **Fase 7 B — URL maestra de sincronización**: campo en Ajustes, fichero de
   texto (una URL por línea, ignora # y vacías), descarga al arrancar (10s,
   nunca rompe), listas marcadas "sincronizadas" mostradas aparte. Necesita
   clave nueva en DataStore (no Room).
3. **Canales religiosos**: pendiente, sin detalle aún.
4. **docs/REPORTE_MEJORAS.md**: escrito, SIN COMMITEAR. El usuario lo quiere al
   final, cuando lo diga. Está en docs/ sin trackear.

## Avisos honestos

- Salvo la migración Room (probada en emulador), NADA se ha ejecutado. No hay
  tests de UI. Foco/D-pad/reproducción del dashboard sin validar en la TV.
- El orden por calidad usa la resolución DECLARADA en el título. Puede mentir.
  La Fase 6 (verified==OK primero) lo corrige.
- ~50% de streams muertos es normal en IPTV; medición desde datacenter US tiene
  sesgo por geo-bloqueo (estimación real desde España 55-65% vivos).

## Cómo probar en la TV

Downloader, URL del prerelease (arriba). Con la firma fija ya instala encima
sin perder favoritos. Probar: canal http se ve, no se cierra la app (si sí,
Ajustes -> Ver último error), servidor del móvil (Ajustes -> Añadir desde el
móvil, IP:puerto + PIN), TVN por países, La 1 una vez en UHD.
