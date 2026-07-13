package io.github.giuseppesorge.pictospeak.nlg.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A communication board: an ordered list of cells rendered as a grid.
 * Deliberately isomorphic to Open Board Format (one locale per board, cells reference
 * pictograms or link to other boards) so .obf/.obz interop stays mechanical (ADR-0008).
 */
@Serializable
data class Board(
    val schemaVersion: Int = 1,
    val id: String,
    val locale: String,
    val name: String,
    val cells: List<BoardCell>,
)

@Serializable
sealed interface BoardCell {
    /** A tappable pictogram; [pictogramId] must resolve in the bundled catalog. */
    @Serializable
    @SerialName("pictogram")
    data class Pictogram(
        val pictogramId: String,
    ) : BoardCell

    /** A folder tile navigating to another board; icon is a catalog pictogram. */
    @Serializable
    @SerialName("link")
    data class Link(
        val boardId: String,
        val name: String,
        val iconPictogramId: String,
    ) : BoardCell
}
