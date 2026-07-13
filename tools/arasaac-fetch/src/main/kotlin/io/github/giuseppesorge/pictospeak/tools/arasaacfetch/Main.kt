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
internal val CATEGORY_TITLES =
    mapOf(
        "persone" to "Persone",
        "azioni" to "Azioni",
        "cibo" to "Cibo",
        "casa-oggetti" to "Casa",
        "corpo-salute" to "Corpo",
        "emozioni" to "Emozioni",
        "scuola-gioco" to "Scuola e gioco",
        "luoghi-trasporti" to "Luoghi",
        "tempo-natura" to "Tempo e natura",
        "sociale" to "Parole sociali",
        "descrittori" to "Descrittori",
        "numeri-colori" to "Numeri e colori",
    )

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
    pruneStaleImages(rows, assetsDir)
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
                    r.label.lowercase() !in keywords ->
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

/** Remove bundled images that are no longer referenced by any catalog row (e.g. the M1 spike set). */
private fun pruneStaleImages(
    rows: List<Row>,
    assetsDir: Path,
) {
    val wanted = rows.map { it.arasaacId }.toSet()
    val stale = assetsDir.listDirectoryEntries("*.png").filter { it.nameWithoutExtension !in wanted }
    stale.forEach(Files::delete)
    if (stale.isNotEmpty()) println("pruned ${stale.size} stale image(s)")
}

private fun fail(message: String): Nothing {
    System.err.println("ERROR: $message")
    exitProcess(1)
}
