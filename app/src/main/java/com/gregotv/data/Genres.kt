package com.gregotv.data

import com.gregotv.model.MediaItem
import java.text.Normalizer

/**
 * The 15 default lists ship 210 different `group-title` values ("Sports",
 * "ES| DEPORTES", "Undefined", "Legislative"...). This maps all of them onto
 * one small fixed set of rows so the home screen shows one row per genre
 * instead of one row per raw group.
 *
 * Resolution order per channel: group-title, then channel name, then the list
 * it came from. Roughly 43% of the merged catalogue matches no genre keyword
 * (mostly small local stations carrying no metadata at all), so the origin
 * fallback is what keeps them reachable instead of piling them into one row.
 */
object Genres {

    const val OTHER = "Otros canales"

    /** Row order on the home screen. Origins come after the genres. */
    val ORDER = listOf(
        "Deportes",
        "Noticias",
        "Películas",
        "Series",
        "Infantil",
        "Documentales",
        "Música",
        "Entretenimiento",
        "Cultura y educación",
        "Estilo de vida",
        "Autonómicas",
        "Religión",
        "España",
        "En español",
        "Latinoamérica",
        "Internacional",
        OTHER
    )

    /**
     * Values that carry no information. `Undefined` is iptv-org's own filler and
     * the rest show up across the other lists; treating them as genres would
     * bucket thousands of channels under a meaningless heading.
     */
    private val PLACEHOLDER_GROUPS = setOf(
        "general", "undefined", "other", "others", "otros", "varios",
        "uncategorized", "sin categoria", "n/a"
    )

    /**
     * Match order, most specific first. Keywords are compared against the
     * accent-stripped lowercase text, so write them without accents.
     */
    private val RULES: List<Pair<String, List<String>>> = listOf(
        "Infantil" to listOf(
            "infantil", "kids", "ninos", "child", "cartoon", "caricatur",
            "disney", "nickelodeon", "nick jr", "boomerang", "baby", "junior",
            "anime", "animacion", "animation", "peppa", "clan"
        ),
        "Deportes" to listOf(
            "deporte", "sport", "futbol", "football", "soccer", "dazn", "espn",
            "eurosport", "motogp", "formula 1", "nba", "nfl", "mlb", "nhl",
            "ufc", "wwe", "tenis", "tennis", "beisbol", "boxeo", "boxing",
            "olimp", "golf", "la liga", "gol tv", "teledeporte", "tdp"
        ),
        "Noticias" to listOf(
            "noticia", "news", "informativ", "actualidad", "24h", "24 horas",
            "cnn", "euronews", "bbc world", "france 24", "al jazeera",
            "telesur", "rt espanol", "canal 24", "legislat", "parlament",
            "congreso", "senado", "politic", "business", "negocio", "economia",
            "bloomberg", "weather", "meteo"
        ),
        "Documentales" to listOf(
            "documental", "documentary", "docu", "docs", "discovery", "natgeo",
            "national geographic", "animal planet", "history", "historia",
            "odisea", "ciencia", "science", "nature", "naturaleza"
        ),
        "Música" to listOf(
            "musica", "music", "mtv", "vh1", "hits", "rock", "reggaeton",
            "flamenco", "jazz", "clasica", "radio", "karaoke", "40 tv"
        ),
        "Películas" to listOf(
            "pelicula", "movie", "cine", "cinema", "film", "hollywood",
            "classic", "clasico", "somos", "tcm"
        ),
        "Series" to listOf(
            "serie", "shows", "tv show", "novela", "telenovela", "drama",
            "sitcom", "comedia", "comedy"
        ),
        "Religión" to listOf(
            "religio", "cristian", "christian", "catolic", "catholic",
            "iglesia", "church", "gospel", "ewtn", "islam", "quran", "biblia",
            "diocesan"
        ),
        "Autonómicas" to listOf(
            "autonomic", "regional", "local", "andaluc", "catalu", "galic",
            "euskadi", "canarias", "valencia", "aragon", "asturias", "murcia",
            "castilla", "extremadura", "baleares", "navarra", "cantabria",
            "rioja", "telemadrid", "tv3", "etb", "a punt"
        ),
        "Cultura y educación" to listOf(
            "cultura", "culture", "educa", "education", "arte", "teatro",
            "libro", "ciencias", "universidad", "aprend"
        ),
        "Estilo de vida" to listOf(
            "lifestyle", "estilo de vida", "cocina", "cooking", "food",
            "gourmet", "viaje", "travel", "moda", "fashion", "salud", "health",
            "hogar", "decorac", "motor", "caza", "pesca", "outdoor", "shop",
            "teletienda", "compras"
        ),
        "Entretenimiento" to listOf(
            "entreteni", "entertainment", "variety", "reality", "humor",
            "talk", "concurso", "gameshow", "family", "familia", "generalista"
        )
    )

    /** Row for one channel: group-title, then channel name, then its list. */
    fun of(item: MediaItem): String =
        match(groupText(item.group))
            ?: match(normalize(item.title))
            ?: item.origin
            ?: OTHER

    /**
     * Coarse origin for a list URL, used when nothing else identifies a
     * channel. Any string returned here that also appears in `isSpanish`'s
     * whitelist marks the channel as Spanish, so the FAST feeds under
     * `i.mjh.nz/*/es.m3u8` must map to a Spanish origin — otherwise the
     * Spanish-only filter would drop them.
     */
    fun originOf(listUrl: String): String {
        val u = listUrl.lowercase()
        return when {
            "tdtchannels" in u || "/countries/es." in u -> "España"
            "/languages/spa" in u -> "En español"
            "i.mjh.nz" in u && Regex("/(es|spa)(?:[./_-]|$)").containsMatchIn(u) ->
                "En español"
            Regex("""/countries/(mx|ar|co|cl|pe|ve|ec|uy|pr|bo|py|gt|cu|do|hn|sv|ni|cr|pa|gq)\.""")
                .containsMatchIn(u) -> "Latinoamérica"
            else -> "Internacional"
        }
    }

    private fun groupText(group: String?): String {
        val g = normalize(group).trim()
        return if (g in PLACEHOLDER_GROUPS) "" else g
    }

    private fun match(text: String): String? {
        if (text.isBlank()) return null
        for ((genre, keywords) in RULES) {
            if (keywords.any { it in text }) return genre
        }
        return null
    }

    private val parenthesised = Regex("""\([^)]*\)|\[[^]]*]""")
    private val qualityTags =
        Regex("""\b(hd|fhd|uhd|sd|4k|1080p?|720p?|576p?|480p?|360p?|h265|hevc|raw)\b""")
    private val nonAlphanumeric = Regex("""[^a-z0-9]+""")

    /**
     * Key used to collapse the same channel appearing in several lists
     * ("Antena 3 (720p)", "Antena 3 HD", "ANTENA 3" -> "antena3").
     */
    fun channelKey(title: String): String =
        normalize(title)
            .replace(parenthesised, " ")
            .replace(qualityTags, " ")
            .replace(nonAlphanumeric, "")

    private fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("""\p{Mn}+"""), "")
            .lowercase()
    }
}
