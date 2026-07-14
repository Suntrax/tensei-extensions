# Tensei Extensions

Official template repository for creating background scraper extensions for the **Tensei** anime client for Android.

## 📖 How it Works

Tensei extensions are headless Android apps (no UI) that run in the background.
1. The Main App scans your phone for apps containing the `EXTENSION_BEACON` receiver.
2. When you search an anime, the Main App queries the extension's `ContentProvider`, passing the `anime` (English name), `animeRomaji`, and `anilistId`.
3. The extension scrapes its target website and returns a JSON string containing either an episode list or a stream URL.

## 🛠️ Creating a New Extension

1. Click **"Use this template"** at the top of this repository to create a new repo for your extension.
2. Clone your new repo and open it in Android Studio.
3. In Android Studio, press `Ctrl+Shift+R` (or `Cmd+Shift+R` on Mac) to open **Replace in Path**.
   - Search for `com.blissless.tensei_extension_template` and replace it with your new package name (e.g., `com.blissless.megaplay`).
   - Search for `TEMPLATE_NAME` and replace it with your extension's display name (e.g., `MegaPlay`). This string is also used in the app label (`Tensei: TEMPLATE_NAME`) declared in `AndroidManifest.xml`.
4. Move your Kotlin files into the new package folder structure (e.g., `com/blissless/megaplay/`).
5. Place your release keystore at `app/release.jks` and update the passwords in `app/build.gradle.kts`.
6. Open `TemplateScraper.kt` (rename it if you like) and implement your scraping logic!

## 📦 Data Contract

The Main App sends parameters to your `ContentProvider` via query parameters on the `scrape` URI:

| Parameter    | Always sent? | Description                                            |
| ------------ | ------------ | ------------------------------------------------------ |
| `anime`      | Yes          | The English name of the anime                          |
| `animeRomaji`| Yes          | The Romaji name of the anime                           |
| `anilistId`  | Yes          | The AniList ID (e.g. `"137822"`)                       |
| `episode`    | Mode B only  | Episode number — only sent for single-stream requests  |
| `lang`       | Mode B only  | `"sub"` or `"dub"` — only sent for single-stream requests |
| `mode`       | Optional     | `"episode_map"` to request the old Format 1 explicitly |

Your scraper must return a JSON string in one of the formats below. All formats return an error object on failure:

```json
{
  "error": "Description of what went wrong."
}
```

---

### Format 1: Episode Map (magnets)

Used by torrent-based extensions like [SubsPlease](https://github.com/Suntrax/subsplease-extension). Returns every episode upfront with magnet links per quality.

**Query:** `content://...provider/scrape?anime=<name>&anilistId=<id>`

```json
{
  "1": {
    "1080p": "magnet:?xt=urn:btih:...",
    "720p": "magnet:?xt=urn:btih:..."
  },
  "2": {
    "1080p": "magnet:?xt=urn:btih:..."
  }
}
```

### Format 2: Flat Magnet List

Used by metadata-style extensions like [SeaDex](https://github.com/Suntrax/seadex-extension). Returns a flat list of magnet links with no episode grouping.

**Query:** `content://...provider/scrape?anime=<name>&anilistId=<id>`

```json
[
  "magnet:?xt=urn:btih:...",
  "magnet:?xt=urn:btih:..."
]
```

### Format 3: Two-Mode Streaming (recommended for stream sources)

Used by streaming extensions like [MegaPlay](https://github.com/Suntrax/megaplay-extension), [AnimeToast](https://github.com/Suntrax/animetoast-extension), [Miruro](https://github.com/Suntrax/miruro-extension), and [Anivexa](https://github.com/Suntrax/anivexa-extension). Splits the contract into two queries so the m3u8 URL is only fetched for the episode the user actually selects — much faster than fetching all URLs upfront.

#### Mode A — Episode list

Returns the available episodes with per-episode langs (no m3u8 URLs yet).

**Query:** `content://...provider/scrape?anime=<name>&anilistId=<id>`

```json
{
  "episodes": [
    {"number": "1", "langs": ["sub", "dub"]},
    {"number": "2", "langs": ["sub", "dub"]},
    {"number": "3", "langs": ["sub"]}
  ]
}
```

#### Mode B — Single stream

Returns the m3u8 URL for one specific episode × lang, **plus the required HTTP headers and soft-subtitle tracks** for playback.

**Query:** `content://...provider/scrape?anilistId=<id>&episode=1&lang=sub`

```json
{
  "url": "https://cdn.example.com/.../master.m3u8",
  "headers": {
    "Referer": "https://embed.example.com/",
    "User-Agent": "Mozilla/5.0 ..."
  },
  "url_with_headers": "https://...master.m3u8|Referer=https%3A%2F%2Fembed.example.com%2F&User-Agent=Mozilla%2F5.0...",
  "subtitles": [
    {"label": "English",  "language": "en", "url": "https://.../eng.vtt", "default": true},
    {"label": "Spanish",  "language": "es", "url": "https://.../spa.vtt", "default": false}
  ],
  "streams": [
    {
      "lang": "sub",
      "default": true,
      "url": "https://cdn.example.com/.../master.m3u8",
      "headers": {"Referer": "https://embed.example.com/", "User-Agent": "..."},
      "url_with_headers": "https://...|Referer=..."
    },
    {
      "lang": "dub",
      "default": false,
      "url": "https://cdn.example.com/.../master_dub.m3u8",
      "headers": {"Referer": "https://embed.example.com/", "User-Agent": "..."},
      "url_with_headers": "https://...|Referer=..."
    }
  ]
}
```

| Field                | Description                                                              |
| -------------------- | ------------------------------------------------------------------------ |
| `url`                | The plain m3u8 URL of the requested lang (for ExoPlayer — pair with `headers`) |
| `headers`            | Map of HTTP headers the CDN requires (for ExoPlayer's `setDefaultRequestProperties`) |
| `url_with_headers`   | Pipe-encoded URL `url\|Header=value&...` (for VLC, mpv, Kodi, ffplay)    |
| `subtitles`          | Array of soft-subtitle VTT tracks (for ExoPlayer's `SubtitleConfiguration`) |
| `streams`            | Array of both sub + dub streams (requested lang first, `default: true`)  |

Each `subtitles` entry has:
- `label` — human-readable name (e.g. `"English"`)
- `language` — ISO 639-1 code (e.g. `"en"`); `"und"` if unknown
- `url` — VTT file URL
- `default` — `true` if the player should auto-enable this track on first play

The `streams` array contains both sub and dub (requested lang first, `default: true`). This lets the player offer a sub↔dub toggle without a second round-trip to the extension.

#### Why split into two modes?

Fetching the m3u8 for every episode upfront would be 2×N HTTP calls (e.g. 48 for a 24-episode show with sub+dub). Mode A returns the episode list in 2 calls, and Mode B fetches the m3u8 in 2 more calls — but only for the episode the user actually picks.

#### Backwards compatibility — Format 1 Episode Map for streams

If your app only implements the original Format 1 contract, streaming extensions can still return an episode map of pipe-encoded URLs when queried with `mode=episode_map`:

**Query:** `content://...provider/scrape?anime=<name>&anilistId=<id>&mode=episode_map`

```json
{
  "1": {
    "sub": "https://cdn.example.com/.../master.m3u8|Referer=https%3A%2F%2Fembed.example.com%2F&User-Agent=Mozilla%2F5.0...",
    "dub": "https://cdn.example.com/.../master.m3u8|Referer=..."
  },
  "2": {
    "sub": "https://...master.m3u8|Referer=..."
  }
}
```

Each value is a pipe-encoded URL (URL + `|` + URL-encoded header key-value pairs) that VLC, mpv, Kodi, and ffplay accept directly. ExoPlayer can't parse this format — for ExoPlayer use Mode B which returns a separate `headers` map.

---

## 🏗️ Building

Extensions are built to be as tiny as possible (~40KB). 
- Do not add any external dependencies (no OkHttp, no Jsoup, no Gson). Use Android's built-in `HttpURLConnection`, `WebView`, and `org.json`.
- R8 shrinking rules are stored in `app/src/main/keepRules/rules.keep`.
- Always build the **Release APK** (`./gradlew assembleRelease`) to ensure R8 shrinks the APK size.

## 🔗 Example Extensions

| Extension                                                         | Format | Source type |
| ----------------------------------------------------------------- | ------ | ----------- |
| [SubsPlease](https://github.com/Suntrax/subsplease-extension)     | 1      | Torrents (magnets) |
| [SeaDex](https://github.com/Suntrax/seadex-extension)             | 2      | Torrents (magnets) |
| [MegaPlay](https://github.com/Suntrax/megaplay-extension)         | 3      | Streaming (m3u8)   |
| [AnimeToast](https://github.com/Suntrax/animetoast-extension)     | 3      | Streaming (m3u8, German Ger Sub/Dub) |
| [Miruro](https://github.com/Suntrax/miruro-extension)             | 3      | Streaming (self-hosted API proxy) |
| [Anivexa](https://github.com/Suntrax/anivexa-extension)           | 3      | Streaming (self-hosted API aggregator, 7+ providers) |
