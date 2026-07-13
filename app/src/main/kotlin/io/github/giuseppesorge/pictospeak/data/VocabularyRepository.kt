package io.github.giuseppesorge.pictospeak.data

import android.content.Context
import io.github.giuseppesorge.pictospeak.nlg.api.Board
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Provides the bundled board content. Implementations must never touch the network. */
interface VocabularyRepository {
    /** Pictogram catalog, keyed by pictogram id. */
    suspend fun loadCatalog(): Map<String, PictogramToken>

    /** All boards of the active language pack, keyed by board id (always includes "home"). */
    suspend fun loadBoards(): Map<String, Board>
}

/**
 * Reads the pipeline-generated assets (`assets/arasaac/catalog_{lang}.json` and
 * `assets/boards/default_{lang}/ board JSONs`, produced by tools/arasaac-fetch).
 */
class AssetVocabularyRepository(
    private val appContext: Context,
    private val language: String,
) : VocabularyRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadCatalog(): Map<String, PictogramToken> =
        withContext(Dispatchers.IO) {
            val text =
                appContext.assets
                    .open("arasaac/catalog_$language.json")
                    .use { it.readBytes().decodeToString() }
            json.decodeFromString<List<PictogramToken>>(text).associateBy { it.id }
        }

    override suspend fun loadBoards(): Map<String, Board> =
        withContext(Dispatchers.IO) {
            val dir = "boards/default_$language"
            val files =
                appContext.assets
                    .list(dir)
                    .orEmpty()
                    .filter { it.endsWith(".json") }
            files.associate { file ->
                val text = appContext.assets.open("$dir/$file").use { it.readBytes().decodeToString() }
                val board = json.decodeFromString<Board>(text)
                board.id to board
            }
        }
}
