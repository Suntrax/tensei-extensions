package com.blissless.tensei_extension_template

import android.content.Context

/**
 * Template scraper — implement your site's scraping logic here.
 *
 * Called by [ScraperProvider] with the query parameters received from the
 * Tensei main app:
 *   - `anime`       — English name of the anime
 *   - `animeRomaji` — Romaji name of the anime
 *   - `anilistId`   — AniList ID
 *   - `episode`     — Episode number (only for stream URL requests)
 *   - `lang`        — "sub" or "dub" (only for stream URL requests)
 *
 * Return a `Map<String, *>` (preferred) or a `List<*>` (legacy flat list).
 * [ScraperProvider.serializeResult] handles the JSON conversion either way.
 *
 * Response formats:
 *
 *   Episode list (Mode A):
 *     {"episodes": [{"number":"1","langs":["sub","dub"]}, ...]}
 *
 *   Single stream (Mode B):
 *     {"url":"https://...m3u8","headers":{"Referer":"...","User-Agent":"..."},
 *      "url_with_headers":"...|Referer=...","streams":[...]}
 *
 *   Episode map (Mode C, with mode=episode_map):
 *     {"1":{"sub":"https://...|Referer=...","dub":"..."}, ...}
 *
 *   Error:
 *     {"error": "Description of what went wrong."}
 *
 * Reference implementations:
 *   - MegaPlay: https://github.com/Suntrax/megaplay-extension
 *   - AnimeToast: https://github.com/Suntrax/animetoast-extension
 *   - Miruro: https://github.com/Suntrax/miruro-extension
 */
object TemplateScraper {

    fun scrape(
        context: Context,
        animeName: String?,
        animeRomaji: String?,
        anilistId: String?,
        episode: String?,
        lang: String?,
        mode: String?
    ): Any {
        // TODO: Implement your scraping logic here.

        // Example returning an episode list:
        // return mapOf("episodes" to listOf(
        //     mapOf("number" to "1", "langs" to listOf("sub", "dub")),
        //     mapOf("number" to "2", "langs" to listOf("sub", "dub"))
        // ))

        // Example returning a single stream:
        // return mapOf(
        //     "url" to "https://example.com/stream.m3u8",
        //     "headers" to mapOf("Referer" to "https://example.com/", "User-Agent" to "Mozilla/5.0 ..."),
        //     "url_with_headers" to "https://example.com/stream.m3u8|Referer=https%3A%2F%2Fexample.com%2F&User-Agent=...",
        //     "streams" to listOf(mapOf(
        //         "lang" to "sub", "default" to true, "url" to "https://...m3u8",
        //         "headers" to mapOf("Referer" to "https://example.com/"),
        //         "url_with_headers" to "https://...|Referer=..."
        //     ))
        // )

        // Example returning an error:
        return mapOf("error" to "TemplateScraper.scrape() not implemented.")
    }
}
