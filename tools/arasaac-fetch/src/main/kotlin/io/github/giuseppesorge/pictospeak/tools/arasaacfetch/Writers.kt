package io.github.giuseppesorge.pictospeak.tools.arasaacfetch

import io.github.giuseppesorge.pictospeak.nlg.api.Board
import io.github.giuseppesorge.pictospeak.nlg.api.BoardCell
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

internal val json = Json { prettyPrint = true }

internal fun writeCatalog(
    rows: List<Row>,
    path: Path,
) {
    val tokens =
        rows
            .sortedWith(compareBy({ it.category }, { it.lemma }))
            .map { PictogramToken(id = it.arasaacId, lemma = it.lemma, pos = it.pos, label = it.label) }
    Files.writeString(path, json.encodeToString(tokens))
    println("catalog: ${path.fileName} (${tokens.size} entries)")
}

internal fun writeBoards(
    rows: List<Row>,
    lang: String,
    boardsDir: Path,
) {
    Files.createDirectories(boardsDir)
    boardsDir.listDirectoryEntries("*.json").forEach(Files::delete)
    val byCategory = rows.groupBy { it.category }.toSortedMap()

    for ((category, categoryRows) in byCategory) {
        val board =
            Board(
                id = category,
                locale = lang,
                name = CATEGORY_TITLES.getValue(category),
                cells = categoryRows.sortedBy { it.lemma }.map { BoardCell.Pictogram(it.arasaacId) },
            )
        Files.writeString(boardsDir.resolve("$category.json"), json.encodeToString(board))
    }

    // Home: pinned core words (stable positions matter for motor planning) + category folders.
    val home =
        Board(
            id = "home",
            locale = lang,
            name = "Home",
            cells =
                rows.filter { it.core }.map { BoardCell.Pictogram(it.arasaacId) } +
                    byCategory.map { (category, categoryRows) ->
                        BoardCell.Link(
                            boardId = category,
                            name = CATEGORY_TITLES.getValue(category),
                            iconPictogramId = (categoryRows.firstOrNull { it.core } ?: categoryRows.first()).arasaacId,
                        )
                    },
        )
    Files.writeString(boardsDir.resolve("home.json"), json.encodeToString(home))
    println("boards: home + ${byCategory.size} categories -> ${boardsDir.fileName}")
}

internal fun writeManifest(
    rows: List<Row>,
    snapshotPath: Path,
    path: Path,
) {
    val manifest =
        mapOf(
            "source" to "https://api.arasaac.org/v1/pictograms/all",
            "snapshot" to snapshotPath.parent.fileName.toString(),
            "pictograms" to rows.size.toString(),
            "imageFormat" to "300px PNG (as served by static.arasaac.org; WebP conversion deliberately deferred)",
            "generatedBy" to "tools/arasaac-fetch (curated vocabulary: tools/core-vocabulary.csv)",
        )
    Files.writeString(
        path,
        json.encodeToString(manifest),
    )
}
