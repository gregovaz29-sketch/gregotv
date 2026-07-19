# GregoTV - Estado de canales IPTV

Diagnóstico de red ejecutado el 2026-07-19 contra las 15 listas por defecto
(`DefaultLists.IPTV`). Cada lista: descarga con timeout, conteo de `#EXTINF`
(canales), y código HTTP.

## Resumen

- Listas con canales > 0: **12 / 15**
- Total de canales detectados: **20.451** (conteo bruto, antes del filtro NSFW)
- Reproductor: Media3 + HLS reproduce el stream al seleccionar un canal.

## Detalle por lista

| HTTP | Canales | Lista |
|-----:|--------:|-------|
| 200 | 358 | https://iptv-org.github.io/iptv/countries/es.m3u |
| 200 | 2417 | https://iptv-org.github.io/iptv/languages/spa.m3u |
| 200 | 13378 | https://iptv-org.github.io/iptv/index.m3u  (filtro NSFW obligatorio) |
| 200 | 547 | https://www.tdtchannels.com/lists/tv.m3u8 |
| 200 | 493 | https://iptv-org.github.io/iptv/categories/sports.m3u |
| 200 | 1000 | https://iptv-org.github.io/iptv/categories/news.m3u |
| 200 | 747 | https://iptv-org.github.io/iptv/categories/movies.m3u |
| 200 | 769 | https://iptv-org.github.io/iptv/categories/music.m3u |
| n/a | - | https://i.mjh.nz/PlutoTV/es.m3u8  (ver nota) |
| n/a | - | https://i.mjh.nz/SamsungTVPlus/es.m3u8  (ver nota) |
| n/a | - | https://i.mjh.nz/Plex/es.m3u8  (ver nota) |
| 200 | 174 | https://iptv-org.github.io/iptv/countries/mx.m3u |
| 200 | 185 | https://iptv-org.github.io/iptv/countries/ar.m3u |
| 200 | 130 | https://iptv-org.github.io/iptv/countries/co.m3u |
| 200 | 253 | https://iptv-org.github.io/iptv/countries/cl.m3u |

## Nota sobre i.mjh.nz (listas FAST: Pluto, Samsung TV+, Plex)

No es un fallo de la app. En la máquina de build el dominio `i.mjh.nz` resuelve
por DNS a `127.0.0.1` (bloqueo local tipo hosts/DNS filtering), así que no hay
conexión. Con una red normal (la de la Android TV) resuelven y sirven las listas
FAST. El parser trata cualquier lista inalcanzable de forma segura: usa la caché
en disco si existe, o la salta sin romper el arranque.

## Comportamiento del parser confirmado

- Descarga con timeout de 10s por lista.
- Deduplica por URL de stream.
- Descarta entradas sin URL.
- Filtro NSFW por `group-title` (xxx/adult/porn...), desactivado por defecto,
  con toggle en Ajustes.
- Caché en disco por lista; arranque offline con la última copia válida.
- Agrupa por `group-title` en carruseles con badge "EN VIVO".

## Reproducir el diagnóstico dentro de la app

`data/M3uDiagnostics.kt` expone `run()` / `report()` para recorrer las listas y
devolver el mismo informe (HTTP + canales por lista) desde el dispositivo.
