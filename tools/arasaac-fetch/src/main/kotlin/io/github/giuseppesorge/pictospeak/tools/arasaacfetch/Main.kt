package io.github.giuseppesorge.pictospeak.tools.arasaacfetch

import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess

/**
 * Builds the bundled ARASAAC assets from the curated vocabulary:
 *   tools/core-vocabulary.csv + assets-src snapshot
 *     -> app/src/main/assets/arasaac/{id}.png (downloaded once, rate-limited)
 *     -> app/src/main/assets/arasaac/catalog_{lang}.json   (List<PictogramToken>)
 *     -> app/src/main/assets/boards/default_{lang}/ (home + one JSON board per category)
 *     -> app/src/main/assets/arasaac/attribution-manifest.json
 *
 * Fails loudly if any CSV row does not verify against the snapshot — license compliance
 * and catalog integrity are part of the definition of done.
 */

private const val RATE_LIMIT_MS = 350L
private const val CSV_FIELDS = 8
private const val PROGRESS_EVERY = 25
private const val HTTP_OK = 200

// Per-language folder titles. Keys are the language-agnostic category ids.
internal val CATEGORY_TITLES: Map<String, Map<String, String>> =
    mapOf(
        "persone" to mapOf("it" to "Persone", "en" to "People"),
        "azioni" to mapOf("it" to "Azioni", "en" to "Actions"),
        "cibo" to mapOf("it" to "Cibo", "en" to "Food"),
        "casa-oggetti" to mapOf("it" to "Casa", "en" to "Home"),
        "corpo-salute" to mapOf("it" to "Corpo", "en" to "Body"),
        "emozioni" to mapOf("it" to "Emozioni", "en" to "Feelings"),
        "scuola-gioco" to mapOf("it" to "Scuola e gioco", "en" to "School & play"),
        "luoghi-trasporti" to mapOf("it" to "Luoghi", "en" to "Places"),
        "tempo-natura" to mapOf("it" to "Tempo e natura", "en" to "Time & nature"),
        "sociale" to mapOf("it" to "Parole sociali", "en" to "Social words"),
        "descrittori" to mapOf("it" to "Descrittori", "en" to "Describing words"),
        "numeri-colori" to mapOf("it" to "Numeri e colori", "en" to "Numbers & colours"),
    )

internal fun categoryTitle(
    category: String,
    lang: String,
): String = CATEGORY_TITLES.getValue(category).let { it[lang] ?: it.getValue("it") }

data class Row(
    val arasaacId: String,
    val lemma: String,
    val label: String,
    val pos: Pos,
    val lang: String,
    val category: String,
    val core: Boolean,
    val rationale: String,
)

fun main(args: Array<String>) {
    val repoRoot = Paths.get("").toAbsolutePath().let { findRepoRoot(it) }
    val lang = args.getOrNull(0) ?: "it"
    val csvPath = repoRoot.resolve("tools/core-vocabulary.csv")
    val snapshotPath =
        repoRoot
            .resolve("assets-src/arasaac")
            .listDirectoryEntries("snapshot-*")
            .maxOrNull()
            ?.resolve("all_$lang.json")
            ?: fail("no snapshot found under assets-src/arasaac/")
    val assetsDir = repoRoot.resolve("app/src/main/assets/arasaac")
    val boardsDir = repoRoot.resolve("app/src/main/assets/boards/default_$lang")

    val rows = parseCsv(csvPath, lang)
    println("csv: ${rows.size} rows for '$lang'")

    val snapshot = loadSnapshotKeywords(snapshotPath)
    validate(rows, snapshot)

    downloadMissing(rows, assetsDir)
    // Images are SHARED across language packs, so keep any id referenced by ANY language.
    pruneStaleImages(allLanguageIds(csvPath), assetsDir)
    writeCatalog(rows, assetsDir.resolve("catalog_$lang.json"))
    writeBoards(rows, lang, boardsDir)
    writeManifest(rows, snapshotPath, assetsDir.resolve("attribution-manifest.json"))
    val categoryCount = rows.map { it.category }.distinct().size
    println("done: ${rows.size} pictograms, ${rows.count { it.core }} core, $categoryCount categories")
}

private fun findRepoRoot(start: Path): Path {
    var dir: Path? = start
    while (dir != null) {
        if (dir.resolve("settings.gradle.kts").exists()) return dir
        dir = dir.parent
    }
    return fail("run from inside the repository")
}

@Suppress("MagicNumber") // CSV column indices
private fun parseCsv(
    path: Path,
    lang: String,
): List<Row> =
    Files
        .readAllLines(path)
        .drop(1)
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        .map { line ->
            val f = line.split(",", limit = CSV_FIELDS).map { it.trim() }
            require(f.size == CSV_FIELDS) { "malformed CSV line (expected $CSV_FIELDS fields): $line" }
            Row(
                arasaacId = f[0],
                lemma = f[1],
                label = f[2],
                pos = Pos.valueOf(f[3]),
                lang = f[4],
                category = f[5],
                core = f[6].toBooleanStrict(),
                rationale = f[7],
            )
        }.filter { it.lang == lang }

/** id -> set of keywords, from the versioned API snapshot. */
private fun loadSnapshotKeywords(path: Path): Map<String, Set<String>> {
    val root = Json.parseToJsonElement(Files.readString(path)).jsonArray
    return root.associate { el ->
        val obj = el.jsonObject
        val id = obj.getValue("_id").jsonPrimitive.content
        val keywords =
            (obj["keywords"] as? JsonArray)
                .orEmpty()
                .mapNotNull {
                    it.jsonObject["keyword"]
                        ?.jsonPrimitive
                        ?.content
                        ?.lowercase()
                }.toSet()
        id to keywords
    }
}

private fun validate(
    rows: List<Row>,
    snapshot: Map<String, Set<String>>,
) {
    val problems =
        buildList {
            rows
                .groupBy { it.arasaacId }
                .filterValues { it.size > 1 }
                .forEach { (id, dupes) -> add("duplicate id $id: ${dupes.joinToString { it.lemma }}") }
            rows
                .groupBy { it.lemma.lowercase() }
                .filterValues { it.size > 1 }
                .forEach { (lemma, dupes) -> add("duplicate lemma '$lemma' in ${dupes.joinToString { it.category }}") }
            for (r in rows) {
                val keywords = snapshot[r.arasaacId]
                when {
                    keywords == null -> add("${r.lemma}: id ${r.arasaacId} not in snapshot")
                    // Label must be a real ARASAAC keyword, or an intentionally curated label
                    // equal to the lemma (e.g. the modal "can"/"must", whose only keywords are
                    // periphrastic "be able to"/"obligation").
                    r.label.lowercase() !in keywords && r.label.lowercase() != r.lemma.lowercase() ->
                        add("${r.lemma}: label '${r.label}' not a keyword of ${r.arasaacId}")
                }
                if (r.category !in CATEGORY_TITLES) add("${r.lemma}: unknown category '${r.category}'")
            }
        }
    if (problems.isNotEmpty()) {
        problems.forEach { System.err.println("VALIDATION: $it") }
        fail("${problems.size} validation problem(s) — fix tools/core-vocabulary.csv")
    }
}

private fun downloadMissing(
    rows: List<Row>,
    assetsDir: Path,
) {
    Files.createDirectories(assetsDir)
    val missing = rows.filter { !assetsDir.resolve("${it.arasaacId}.png").exists() }
    if (missing.isEmpty()) {
        println("images: all ${rows.size} already present")
        return
    }
    println("images: downloading ${missing.size} of ${rows.size} (rate-limited)")
    val client = HttpClient.newHttpClient()
    var done = 0
    // Some snapshot ids have no image on the static server — collect ALL failures so the
    // CSV can be fixed in one pass instead of one failure per run.
    val failures = mutableListOf<String>()
    for (r in missing) {
        val url = "https://static.arasaac.org/pictograms/${r.arasaacId}/${r.arasaacId}_300.png"
        val response =
            client.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray(),
            )
        if (response.statusCode() == HTTP_OK) {
            Files.write(assetsDir.resolve("${r.arasaacId}.png"), response.body())
        } else {
            failures += "HTTP ${response.statusCode()} for ${r.arasaacId} (${r.lemma})"
        }
        done++
        if (done % PROGRESS_EVERY == 0) println("  $done/${missing.size}")
        Thread.sleep(RATE_LIMIT_MS)
    }
    if (failures.isNotEmpty()) {
        failures.forEach { System.err.println("DOWNLOAD: $it") }
        fail("${failures.size} image(s) unavailable — replace those ids in tools/core-vocabulary.csv")
    }
}

/** Every pictogram id referenced by any language row in the vocabulary (images are shared). */
@Suppress("MagicNumber")
private fun allLanguageIds(csvPath: Path): Set<String> =
    Files
        .readAllLines(csvPath)
        .drop(1)
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        .map { it.split(",", limit = 8)[0].trim() }
        .toSet()

private fun pruneStaleImages(
    wanted: Set<String>,
    assetsDir: Path,
) {
    val stale = assetsDir.listDirectoryEntries("*.png").filter { it.nameWithoutExtension !in wanted }
    stale.forEach(Files::delete)
    if (stale.isNotEmpty()) println("pruned ${stale.size} stale image(s)")
}

private fun fail(message: String): Nothing {
    System.err.println("ERROR: $message")
    exitProcess(1)
}
