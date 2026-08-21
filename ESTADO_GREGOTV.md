# GregoTV — Estado y hoja de ruta

**Última actualización:** 20 agosto 2026
**Ubicación:** este fichero va **en la raíz del repo y commiteado**. Si vive solo
fuera del repo, Claude Code no puede leerlo y el contexto se pierde.
**Regla del documento:** cada afirmación lleva su nivel de verificación. Nada de adornos.

- ✅ **MEDIDO** — comprobado con datos o ejecución real, con la cifra delante.
- 🔍 **LEÍDO** — verificado leyendo el código fuente del repo.
- 🟡 **SIN VERIFICAR** — razonado pero no probado. No dar por bueno.
- ❌ **FALSO / MUERTO** — comprobado que no funciona.

---

## 1. Qué es el proyecto

App Android TV nativa (Leanback launcher). Reproduce:
- Canales en vivo desde listas M3U/HLS públicas y legales
- Vídeos locales del dispositivo (MediaStore)
- Vídeos por red SMB

**No es Kodi.** No corre addons de Kodi. No incluye ni incluirá fuentes pirata.

- Repo: `https://github.com/gregovaz29-sketch/gregotv` (público, rama `main`)
- APK release: `https://github.com/gregovaz29-sketch/gregotv/releases/latest/download/GregoTV.apk`
- Stack: Kotlin, Compose + `androidx.tv:tv-material`, Media3/ExoPlayer, Hilt, Room, DataStore, Coil
- **No hay `gradlew` ni Android SDK en la máquina de dev.** Todo se compila por CI.

---

## 2. Decisión tomada sobre el catálogo

**"Solo español" = España + Latinoamérica.** Objetivo de calidad: que los canales funcionen y se vean bien.

**El recorte se hace en ORIGEN (qué listas se descargan), no filtrando en runtime.**
Motivo: el flag `MediaItem.spanish` es una heurística con falsos negativos (canales
españoles sin metadata). Si se usa como filtro duro, esconde contenido bueno.
Se queda como criterio de **orden**, no de censura.

### `spanishOnly`: se retira el filtro duro 🔍 LEÍDO (20 ago)

La Fase 1 (`6dfddd2`) metió en `MediaRepository.loadRows()`:
```kotlin
.let { if (settings.spanishOnly) it.filter { c -> c.spanish } else it }
```
con `spanishOnly = true` por defecto. **Contradice esta sección. Se quita.**

Dos motivos:
1. **Hoy no filtra nada.** `isSpanish()` devuelve `true` en su primera línea si
   el origen es España / En español / Latinoamérica, y `originOf()` mapea las 12
   listas finales a esos tres cubos. Es un no-op sobre el 100% del catálogo.
2. **El día que filtre, romperá cosas en silencio.** Una lista M3U que añada el
   usuario en Ajustes cae en `"Internacional"` → `spanish = false` → la app le
   esconde su propia lista sin dar ningún error.

`spanish` se queda solo en los `sortedWith` de `MediaRepository:95` y
`SearchScreen:46`, que es su sitio.

Fuera: `index.m3u` global y las 8 listas de `categories/`. Son mundiales y metían
la mayor parte del ruido. **Ahí está todo el recorte.**

### Países: NO se amplía la lista ✅ MEDIDO (20 ago)

Se descartó añadir `bo, py, gt, cu, do, hn, sv, ni, cr, pa`. Medido:

| | Canales brutos |
|---|---|
| Países ya presentes | 1.422 |
| Los 10 propuestos | **708 (+50%)** |

Son canales locales de Bolivia, Paraguay, Nicaragua, Panamá... (Cuba aporta 2).
`languages/spa` ya recoge los notables y la dedupe los colapsaría igual.
**Ampliaría el catálogo justo cuando el objetivo es recortarlo. Descartado.**
Se mantienen los países que ya estén en `DefaultLists.IPTV`.

---

## 3. Datos medidos (20 ago 2026)

### Impacto del recorte ✅ MEDIDO

| | Antes (24 listas) | Final (12 listas ES+LatAm) |
|---|---|---|
| Canales brutos | ~20.600 | **4.242** |
| Únicos por URL | ~12.200 | **2.577** |

Las 12: `countries/` es, mx, ar, co, cl, pe, ve, ec, uy, pr + `languages/spa`
+ tdtchannels. Sin `index`, sin `categories/`, sin i.mjh.nz, sin países nuevos.

> Cifras anteriores del doc (4.950 / 2.767 sobre 22 listas) medían un set que
> incluía los 10 países luego descartados. Quedan anuladas.

### Salud real de los streams ✅ MEDIDO
Muestra aleatoria de **400 canales**, timeout 8s:

- **Vivos: 197 (49,2%)**
- **Muertos: 203 (50,8%)** → 127 timeout, 44 HTTP 403, 12 404, 10 503, 4 DNS muerto

> ⚠️ **Sesgo conocido:** medido desde un datacenter fuera de España. Parte de los
> 403 son geo-bloqueo y sí funcionarían desde casa. Estimación real desde red
> española: **55-65% vivos**. Aun así, ~1 de cada 3 canales da error.

### Marcadores de calidad que las listas ya traen ✅ MEDIDO
iptv-org escribe la calidad en el propio título:

| Marcador | Cantidad | Tasa de acierto medida |
|---|---|---|
| `[Not 24/7]` | 501 (18,1%) | **37% vivos** |
| `[Geo-blocked]` | 60 (2,2%) | 33% vivos (muestra pequeña, n=6) |
| Sin marcador | resto | **53% vivos** |

Resolución declarada: 1080p → 588 · 720p → 928 · 480p o menos → ~140 · sin dato → 1041
Sin `tvg-logo`: 13 (0,5%) — no es un problema.
Sin `group-title` útil: **1079 (39,0%)** — sí es un problema, van al cajón "Otros".

**Conclusión: `[Not 24/7]` predice fallo. Hay que usarlo, no borrarlo.**

---

## 4. Bugs encontrados en el código 🔍 LEÍDO

Por gravedad:

1. **La dedupe tira la información de calidad.** `Genres.channelKey()` borra los
   tags `(1080p)` / `[Not 24/7]` y `distinctBy` se queda con **la primera variante
   que aparezca**. Ejemplo real: `'la1'` fusiona *La 1*, *La 1 (1080p)* y
   *La 1 UHD (2160p)* — elige una al azar.

2. **El orden de listas es aleatorio entre arranques.** `iptvUrls` se guarda con
   `stringSetPreferencesKey` → es un **Set, sin orden**. En cuanto el usuario toca
   Ajustes, cambia qué variante del canal sobrevive a la dedupe. Hoy *La 1* puede
   ser la UHD, mañana la SD rota. Sin tocar nada.

3. **Fusiona canales distintos.** 91 claves colapsan títulos diferentes ✅ MEDIDO.
   Con ES+LatAm juntos empeora: *Canal 10*, *Global TV*, *Telemax*, *TVN* existen
   en varios países y se pisan. La clave de dedupe necesita incluir el país.

4. ~~**Room sin migraciones.**~~ **RESUELTO en `f1d2279` (Fase 2).** El dato de
   `v1` + `fallbackToDestructiveMigration()` venía de leer `main`, no la rama.
   En `mejoras/premium` está en **v2 con `.addMigrations(*ALL_MIGRATIONS)`** y
   existe `Migrations.kt` con un `MIGRATION_1_2` real. Los favoritos y el
   progreso **no se borran**. 🟡 Escrito, no compilado.

5. **Cambiar `DefaultLists` no llega a quien ya tocó Ajustes.** No hay versión de
   defaults. Los usuarios existentes se quedan con las listas viejas (incluidas
   las muertas).

6. **`TmdbRepository` es un stub.** `posterFor()` devuelve `null` siempre, aunque
   se pase la API key. Deshabilitado a propósito, documentado en el código.

---

## 5. Fuentes muertas ❌ CONFIRMADO

**`i.mjh.nz` ya no publica listas M3U.** El dominio responde HTTP 200 pero las
tres URLs FAST dan **404**:

```
404  https://i.mjh.nz/PlutoTV/es.m3u8
404  https://i.mjh.nz/SamsungTVPlus/es.m3u8
404  https://i.mjh.nz/Plex/es.m3u8
```

**Evidencia concluyente** (no es solo el 404): el listado del directorio
`https://i.mjh.nz/PlutoTV/` devuelve únicamente `all.xml ar.xml br.xml ca.xml
cl.xml es.xml mx.xml us.xml ...` y sus `.gz`. **No queda ni un solo `.m3u8`.**
Retiraron las listas y dejaron solo el EPG.

> ⚠️ En la máquina de dev ese dominio resuelve a `127.0.0.1` por bloqueo DNS
> local y `curl` devuelve código `000`. Desde ahí **no se puede confirmar ni
> desmentir**. La medida buena es la de fuera.

**Acciones:**
- Quitar las 3 URLs de `DefaultLists.IPTV`.
- Quitar las 3 URLs del bloque de diagnóstico del workflow.
- Limpiar la rama muerta `"i.mjh.nz" in u -> "En español"` de `Genres.originOf`.

**Lo bueno:** el mismo dominio SÍ sirve EPG en XMLTV. Material gratis para la
Fase EPG:
```
https://i.mjh.nz/PlutoTV/es.xml
https://i.mjh.nz/SamsungTVPlus/es.xml
https://i.mjh.nz/Plex/es.xml
```

---

## 6. Trabajo hecho — rama `mejoras/premium`

> ⚠️ **Esta rama solo existe en la máquina local. NO está en `origin`.**
> Fetch del 20 ago: en el remoto solo hay `main`. Ninguno de los commits de abajo
> es visible desde fuera. Mientras no se suba, no se puede revisar ni compilar.

| Fase | Commit | Estado |
|---|---|---|
| 0 Preparación | — | ✅ rama creada, baseline anotada |
| 1 Cero inglés | `6dfddd2` | 🟡 revisión lógica + datos reales |
| 2 Fiabilidad | `f1d2279` | 🟡 revisión lógica |
| 3 Curado | `76d0018` | ✅ medido: cajones 43,8% → 35,1% |
| 4 Dashboard | `82961e0` | 🟡 revisión lógica |
| 5 Fuentes ampliables | `045f925` | 🟡 revisión lógica |

**Nada está compilado.** Sin `gradlew` ni `ANDROID_HOME` en local. Todo es
revisión estática. No dar por bueno hasta que el CI lo confirme.

**Nota de la Fase 3:** el primer intento de "coincidencia por palabra completa"
**empeoraba** el clasificador (perdía *Cinecanal*, *Filmex*, *MTV Rocks* y 7
autonómicas). Revertido a aplicarlo solo a `clan` y `arte`, los únicos con falso
positivo medido. **No repetir ese intento sin medir antes.**

**Verificado:** cero rastro de Kodi/addons/piratería, cero credenciales hardcodeadas.

---

## 7. Bloqueantes del CI 🔍 LEÍDO — resolver ANTES de subir

`.github/workflows/build-apk.yml`:

1. **El CI no dispara en ramas.** Está en `on: push: branches: [ main ]`. Un push
   a `mejoras/premium` **no lanza build** y no avisa. Sí existe `workflow_dispatch`,
   así que se puede lanzar a mano desde Actions eligiendo la rama.

2. **El paso `Publish release` no tiene condición** y lleva `make_latest: true`.
   Lanzarlo sobre la rama **publicaría código sin probar como `latest`** y la URL
   de descarga de la TV serviría el APK roto. Arreglo:
   ```yaml
   - name: Publish release
     if: github.ref == 'refs/heads/main'
     uses: softprops/action-gh-release@v2
   ```
   El `upload-artifact` se queda: se puede bajar el APK de la rama desde Actions
   para probarlo en la TV sin tocar el `latest`.

3. **El bloque de diagnóstico sigue con las 15 URLs viejas**, incluidas las tres
   de `i.mjh.nz` muertas. Va a escupir tres 404 en cada build. Actualizarlo.

---

## 8. Próximos pasos, en orden

1. [ ] Blindar `Publish release` con `if: github.ref == 'refs/heads/main'`
2. [ ] Actualizar el bloque de diagnóstico del workflow (quitar i.mjh.nz e `index`/`categories`)
3. [ ] Push de `mejoras/premium`
4. [ ] Actions → Build GregoTV APK → Run workflow → rama `mejoras/premium`
5. [ ] Iterar sobre errores reales del log hasta build verde
6. [ ] Con el verde: revisión estática de **lógica** (dedupe, cajones, regresiones)
7. [ ] Fase 6 — documentar EPG (no implementar)
8. [ ] `docs/REPORTE_MEJORAS.md`
9. [ ] Instalar en la TV y probar navegación real

### Backlog técnico (no olvidar)
- [x] ~~Migraciones Room~~ — hecho en `f1d2279` (v2 + `ALL_MIGRATIONS`). Sin compilar.
- [ ] `stringSetPreferencesKey` → lista ordenada (`stringPreferencesKey` con JSON)
- [ ] Dedupe que elige la **mejor** variante: mayor resolución > sin `[Not 24/7]` > sin `[Geo-blocked]`
- [ ] Clave de dedupe con país incluido
- [ ] Guardar `resolution`, `is24_7`, `geoBlocked` en `MediaItem`; limpiar los tags del título mostrado
- [ ] Versionar `DefaultLists` para que los cambios lleguen a usuarios existentes
- [ ] Verificador de streams (decisión de arquitectura pendiente, ver abajo)
- [ ] APK release firmado (hoy el CI solo genera debug)
- [ ] No hay tests

---

## 9. Decisión pendiente: dónde verificar los streams

Sin decidir. Las tres opciones:

- **En la TV** — verdad desde la red del usuario (geo correcto), pero 2.577 probes
  son minutos de arranque y tráfico en un TV box.
- **En el CI** — arranque instantáneo, pero se verifica desde un datacenter de
  GitHub (USA) → falsos negativos por geo-bloqueo español.
- **Híbrido (recomendado)** — el CI genera cada 6h un JSON de canales verificados
  que la app descarga en una petición; además la app verifica en background lo que
  el usuario abre y **aprende localmente** qué le falla a él. Lo mejor de los dos.

---

## 10. Decisiones firmes (no reabrir)

- **No integrar addons de Kodi.** Ni Balandro, Alfa, Crew, Palantir, Magellan,
  chopo, gtking, bugatsinho, michaz, sammax, magnetic, slyguy, ni ninguno.
  Es piratería y además técnicamente incompatible con esta app.
- **No incluir streams de pago pirateados** (Canal+, DAZN, Movistar+, etc.).
- **Solo listas M3U públicas y legales** (iptv-org, tdtchannels, FAST oficiales).
- **Español prioritario** en cada fila y en el hero.
- **Compilación por CI.** No intentar compilar en local.
- Rama `main`, push directo. CI publica release `build-N`.
