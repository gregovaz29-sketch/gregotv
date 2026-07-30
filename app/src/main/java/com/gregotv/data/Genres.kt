package com.gregotv.data

import com.gregotv.model.MediaItem
import java.text.Normalizer

/**
 * The 15 default lists ship hundreds of different `group-title` values
 * ("Sports", "ES| DEPORTES", "Spain", "Undefined"...). This maps all of them
 * onto one small fixed set of genres so the home screen shows one row per
 * genre instead of one row per raw group.
 */
object Genres {

    const val OTHER = "Otros canales"

    /** Row order on the home screen. */
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
        OTHER
    )

    /**
     * Match order, most specific first. Keywords are compared against the
     * accent-stripped lowercase text, so write them without accents.
     */
    private val RULES: List<Pair<String, List<String>>> = listOf(
        "Infantil" to listOf(
            "infantil", "kids", "ninos", "child", "cartoon", "caricatur",
            "disney", "nickelodeon", "nick jr", "boomerang", "baby", "junior",
            "anime", "animacion", "peppa", "clan"
        ),
        "Deportes" to listOf(
            "deporte", "sport", "futbol", "football", "soccer", "dazn", "espn",
            "eurosport", "motogp", "formula 1", "nba", "nfl", "mlb", "nhl",
            "ufc", "wwe", "tenis", "tennis", "beisbol", "boxeo", "boxing",
            "olimp", "golf", "la liga", "gol tv", "vamos", "movistar plus"
        ),
        "Noticias" to listOf(
            "noticia", "news", "informativ", "actualidad", "24h", "24 horas",
            "cnn", "euronews", "bbc world", "france 24", "al jazeera",
            "telesur", "rt espanol", "canal 24"
        ),
        "Documentales" to listOf(
            "documental", "documentary", "docu", "discovery", "natgeo",
            "national geographic", "animal planet", "history", "historia",
            "odisea", "ciencia", "science", "nature", "naturaleza"
        ),
        "Música" to listOf(
            "musica", "music", "mtv", "vh1", "hits", "rock", "reggaeton",
            "flamenco", "jazz", "clasica", "radio", "karaoke", "40 tv"
        ),
        "Películas" to listOf(
            "pelicula", "movie", "cine", "cinema", "film", "hollywood",
            "dark", "somos", "tcm", "action"
        ),
        "Series" to listOf(
            "serie", "shows", "tv show", "novela", "telenovela", "drama",
            "sitcom", "comedia", "comedy"
        ),
        "Religión" to listOf(
            "religio", "cristian", "christian", "catolic", "catholic",
            "iglesia", "church", "gospel", "ewtn", "islam", "quran", "biblia",
            "trece", "fe "
        ),
        "Autonómicas" to listOf(
            "autonomic", "regional", "local", "andaluc", "catalu", "galic",
            "euskadi", "canarias", "valencia", "aragon", "asturias", "murcia",
            "castilla", "extremadura", "baleares", "navarra", "cantabria",
            "rioja", "telemadrid", "tv3", "etb", "aragon tv", "a punt"
        ),
        "Cultura y educación" to listOf(
            "cultura", "culture", "educa", "education", "arte", "teatro",
            "libro", "ciencias", "universidad", "aprend"
        ),
        "Estilo de vida" to listOf(
            "lifestyle", "estilo de vida", "cocina", "food", "gourmet",
            "viaje", "travel", "moda", "fashion", "salud", "health", "hogar",
            "decorac", "motor", "auto", "caza", "pesca"
        ),
        "Entretenimiento" to listOf(
            "entreteni", "entertainment", "variety", "reality", "humor",
            "general", "generalista", "talk", "concurso", "gameshow"
        )
    )

    /** Genre for one channel: group-title wins, channel name is the fallback. */
    fun of(item: MediaItem): String =
        match(normalize(item.group)) ?: match(normalize(item.title)) ?: OTHER

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
