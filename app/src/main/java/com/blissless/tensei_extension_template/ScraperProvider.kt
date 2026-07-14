package com.blissless.tensei_extension_template

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * ContentProvider queried by the Tensei main app.
 *
 * Query URI:
 *   content://com.blissless.tensei_extension_template.provider/scrape
 *     ?anime=<name>&animeRomaji=<romaji>&anilistId=<id>
 *     [&episode=N&lang=sub|dub]  (for stream URL requests)
 *     [&mode=episode_map]        (for Format 1 episode map)
 *
 * Returns a single-row MatrixCursor whose "data" column holds a JSON string.
 */
class ScraperProvider : ContentProvider() {

    companion object {
        private const val TAG = "Template/Provider"
        const val AUTHORITY = "com.blissless.tensei_extension_template.provider"
        const val PATH_SCRAPE = "scrape"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_SCRAPE")
        private const val CODE_SCRAPES = 1
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, PATH_SCRAPE, CODE_SCRAPES)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        when (uriMatcher.match(uri)) {
            CODE_SCRAPES -> {
                val animeName   = uri.getQueryParameter("anime")
                val animeRomaji = uri.getQueryParameter("animeRomaji")
                val anilistId   = uri.getQueryParameter("anilistId")
                val episode     = uri.getQueryParameter("episode")
                val lang        = uri.getQueryParameter("lang")
                val mode        = uri.getQueryParameter("mode")
                val cursor = MatrixCursor(arrayOf("data"))

                Log.d(TAG, "scrape: anime=$animeName romaji=$animeRomaji anilistId=$anilistId ep=$episode lang=$lang mode=$mode")

                try {
                    val result = TemplateScraper.scrape(
                        context!!, animeName, animeRomaji, anilistId, episode, lang, mode
                    )
                    val json = serializeResult(result)
                    cursor.addRow(arrayOf(json))
                } catch (e: Exception) {
                    Log.e(TAG, "scrape failed", e)
                    cursor.addRow(arrayOf(
                        "{\"error\":\"Scraping failed: " +
                                "${e.message?.replace("\"", "\\\"")}\"}"
                    ))
                }
                return cursor
            }
        }
        return null
    }

    private fun serializeResult(result: Any): String {
        return when (result) {
            is Map<*, *> -> {
                val obj = JSONObject()
                for ((key, value) in result) {
                    when (value) {
                        is Map<*, *> -> obj.put(key.toString(), JSONObject(value as Map<*, *>))
                        is List<*> -> {
                            val arr = JSONArray()
                            for (item in value) arr.put(item)
                            obj.put(key.toString(), arr)
                        }
                        is JSONArray -> obj.put(key.toString(), value)
                        is JSONObject -> obj.put(key.toString(), value)
                        null -> obj.put(key.toString(), JSONObject.NULL)
                        else -> obj.put(key.toString(), value)
                    }
                }
                obj.toString()
            }
            is List<*> -> {
                val arr = JSONArray()
                for (item in result) arr.put(item)
                arr.toString()
            }
            else -> result.toString()
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
