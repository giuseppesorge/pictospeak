package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.nlg.api.Board
import io.github.giuseppesorge.pictospeak.nlg.api.BoardCell
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

/**
 * Validates the pipeline-generated assets that ship in the APK (part of the definition of
 * done): the catalog is coherent, every reference resolves, and no orphan files ship.
 * Runs on the JVM against the source tree — unit tests execute with the module directory
 * as working dir.
 */
class BundledAssetsTest {
    private val assets: Path = Paths.get("src/main/assets")
    private val json = Json { ignoreUnknownKeys = true }

    private val language = "it"

    private fun catalog(): List<PictogramToken> =
        json.decodeFromString(Files.readString(assets.resolve("arasaac/catalog_$language.json")))

    private fun boards(): List<Board> =
        assets
            .resolve("boards/default_$language")
            .listDirectoryEntries("*.json")
            .map { json.decodeFromString<Board>(Files.readString(it)) }

    @Test
    fun `catalog ids and lemmas are unique and every image exists`() {
        val entries = catalog()
        assertTrue("catalog is suspiciously small (${entries.size})", entries.size >= 200)
        val dupIds = entries.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate catalog ids: $dupIds", dupIds.isEmpty())
        val dupLemmas = entries.groupBy { it.lemma.lowercase() }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate lemmas: $dupLemmas", dupLemmas.isEmpty())
        val missingImages = entries.filter { !assets.resolve("arasaac/${it.id}.png").exists() }
        assertTrue("catalog entries without image: ${missingImages.map { it.lemma }}", missingImages.isEmpty())
    }

    @Test
    fun `no orphan images ship in the APK`() {
        val wanted = catalog().map { it.id }.toSet()
        val orphans =
            assets
                .resolve("arasaac")
                .listDirectoryEntries("*.png")
                .map { it.nameWithoutExtension }
                .filterNot { it in wanted }
        assertTrue("images not referenced by the catalog: $orphans", orphans.isEmpty())
    }

    @Test
    fun `boards exist, home included, and every cell reference resolves`() {
        val boards = boards()
        val ids = boards.map { it.id }
        assertTrue("home board missing", "home" in ids)
        val catalogIds = catalog().map { it.id }.toSet()
        val boardIds = ids.toSet()
        val problems =
            buildList {
                for (board in boards) {
                    for (cell in board.cells) {
                        when (cell) {
                            is BoardCell.Pictogram ->
                                if (cell.pictogramId !in catalogIds) add("${board.id}: pictogram ${cell.pictogramId}")
                            is BoardCell.Link -> {
                                if (cell.boardId !in boardIds) add("${board.id}: link to missing board ${cell.boardId}")
                                if (cell.iconPictogramId !in
                                    catalogIds
                                ) {
                                    add("${board.id}: icon ${cell.iconPictogramId}")
                                }
                            }
                        }
                    }
                }
            }
        assertTrue("unresolved board references: $problems", problems.isEmpty())
    }

    @Test
    fun `license fencing files ship with the symbols`() {
        assertTrue(assets.resolve("arasaac/LICENSE").exists())
        assertTrue(assets.resolve("arasaac/ATTRIBUTION.md").exists())
        assertTrue(assets.resolve("arasaac/attribution-manifest.json").exists())
    }
}
