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

    /**
     * Spanish regional and local stations. Most small local channels ship no
     * usable `group-title` at all, so anything from a Spain-origin list that
     * matches no genre lands here rather than in a nameless origin bucket.
     */
    const val LOCAL_ES = "Autonómicas y locales"

    /** Origin used by channels coming from a source the user configured. */
    const val USER_SOURCES = "Mis fuentes"

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
        LOCAL_ES,
        "Religión",
        "España",
        "En español",
        "Latinoamérica",
        "Internacional",
        USER_SOURCES,
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
            "disney", "nickelodeon", "nick jr", "boomerang", "baby",
            "junior tv", "anime", "animacion", "animation", "peppa", "clan",
            "peque", "boing", "canal panda", "discovery kids"
        ),
        "Deportes" to listOf(
            "deporte", "sport", "futbol", "football", "soccer", "dazn", "espn",
            "eurosport", "motogp", "formula 1", "formula1", "nba", "nfl", "mlb",
            "nhl", "ufc", "wwe", "tenis", "tennis", "beisbol", "boxeo",
            "boxing", "olimp", "golf", "la liga", "gol tv", "teledeporte",
            "tdp", "premier league", "champions", "copa america", "copa del rey",
            "ligue 1", "bundesliga", "serie a", "rugby", "cricket"
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
            // "classic"/"clasico" stay broad on purpose: iptv-org's "Classic"
            // category is classic cinema, and narrowing them cost 10 channels.
            "pelicula", "peliculas", "pelis", "movie", "movies", "cine",
            "cinema", "film", "films", "hollywood", "classic", "clasico",
            "somos", "tcm", "accion", "terror", "romance", "aventura",
            "thriller", "pluto tv peliculas", "pluto tv cine"
        ),
        "Series" to listOf(
            "serie", "series", "shows", "tv show", "novela", "telenovela",
            "sitcom", "comedia", "comedy", "dramatic serie", "drama serie",
            "pluto tv series", "pluto tv novelas"
        ),
        "Religión" to listOf(
            "religio", "cristian", "christian", "catolic", "catholic",
            "iglesia", "church", "gospel", "ewtn", "islam", "quran", "biblia",
            "diocesan"
        ),
        LOCAL_ES to listOf(
            // Removed generic "local" — matched hundreds of unrelated titles.
            "autonomic", "regional", "andaluc", "catalu", "galic",
            "euskadi", "canarias", "valencia", "aragon", "asturias", "murcia",
            "castilla", "extremadura", "baleares", "navarra", "cantabria",
            "la rioja", "telemadrid", "tv3", "etb", "a punt", "canal sur",
            "aragon tv", "tvg", "ib3", "7 region", "cmm"
        ),
        "Cultura y educación" to listOf(
            "cultura", "culture", "educa", "education", "arte", "teatro",
            "libro", "ciencias", "universidad", "aprend", "museo", "opera",
            "ballet"
        ),
        "Estilo de vida" to listOf(
            "lifestyle", "estilo de vida", "cocina", "cooking", "food",
            "gourmet", "viaje", "travel", "moda", "fashion", "salud", "health",
            "hogar", "decorac", "motor", "caza", "pesca", "outdoor", "shop",
            "teletienda", "compras", "bricolaje", "jardin"
        ),
        "Entretenimiento" to listOf(
            "entreteni", "entertainment", "variety", "reality", "humor",
            "talk", "concurso", "gameshow", "family", "familia", "generalista"
        )
    )

    /**
     * The few keywords where plain `contains` demonstrably drags in unrelated
     * channels, measured against the default lists: "clan" hit "8tv Chiclana",
     * "arte" hit "3Cat Joc de Cartes". These must match a whole word.
     *
     * Deliberately small. Whole-word matching was also tried on "cine",
     * "film", "rock" and the regional call signs (etb/tv3/tvg/ib3/cmm) and it
     * made things worse — it dropped Cinecanal, Filmex, MTV Rocks and seven
     * real regional channels — so those stay as substring matches.
     */
    private val WHOLE_WORD = setOf("clan", "arte")

    /**
     * Pre-compiled matchers, built once. Compiling a regex per channel per
     * keyword would be ~12k x 200 regex builds on every load.
     */
    private class Keyword(value: String) {
        private val literal = value
        private val wholeWord: Regex? =
            if (value in WHOLE_WORD) {
                Regex("(?<![a-z0-9])" + Regex.escape(value) + "(?![a-z0-9])")
            } else null

        fun matches(text: String): Boolean =
            wholeWord?.containsMatchIn(text) ?: (literal in text)
    }

    private val COMPILED: List<Pair<String, List<Keyword>>> =
        RULES.map { (genre, keywords) -> genre to keywords.map { Keyword(it) } }

    /** Row for one channel: group-title, then channel name, then its origin. */
    fun of(item: MediaItem): String {
        val genre = match(groupText(item.group)) ?: match(normalize(item.title))
        if (genre != null) return genre
        // No genre metadata. Spain-origin leftovers are local/regional
        // stations, which is a far more useful row than a bare origin bucket.
        if (item.origin == "España") return LOCAL_ES
        return item.origin ?: OTHER
    }

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
            latamList.containsMatchIn(u) -> "Latinoamérica"
            else -> "Internacional"
        }
    }

    /**
     * Origin for one entry. The country code in `tvg-id` is more precise than
     * the list URL: `languages/spa.m3u` is one big multi-country feed, and
     * without this its ~900 channels all pile into a vague "En español" row
     * instead of splitting into España and Latinoamérica.
     */
    fun originFor(listUrl: String, tvgId: String?): String {
        val code = tvgId?.let { idCountry.find(it)?.groupValues?.get(1)?.lowercase() }
        return when {
            code == "es" -> "España"
            code != null && code in latamCountries -> "Latinoamérica"
            else -> originOf(listUrl)
        }
    }

    private val latamList =
        Regex("""/countries/(mx|ar|co|cl|pe|ve|ec|uy|pr|bo|py|gt|cu|do|hn|sv|ni|cr|pa|gq)\.""")

    private val latamCountries = setOf(
        "mx", "ar", "co", "cl", "ve", "pe", "ec", "uy", "pr", "bo", "py",
        "gt", "cu", "do", "hn", "sv", "ni", "cr", "pa", "gq"
    )

    // tvg-id looks like "Name.cc@Quality" or "Name.cc".
    private val idCountry = Regex("""\.([a-z]{2})(?:@|$)""", RegexOption.IGNORE_CASE)

    private fun groupText(group: String?): String {
        val g = normalize(group).trim()
        return if (g in PLACEHOLDER_GROUPS) "" else g
    }

    private fun match(text: String): String? {
        if (text.isBlank()) return null
        for ((genre, keywords) in COMPILED) {
            if (keywords.any { it.matches(text) }) return genre
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
