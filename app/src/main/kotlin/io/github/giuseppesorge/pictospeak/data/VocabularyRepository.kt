package io.github.giuseppesorge.pictospeak.data

import android.content.Context
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Provides the board vocabulary. Implementations must never touch the network. */
interface VocabularyRepository {
    suspend fun load(): List<PictogramToken>
}

/**
 * Reads the bundled pictogram catalog (`assets/arasaac/catalog_{lang}.json`, produced by
 * the offline pipeline in tools/). The catalog is a plain JSON array of [PictogramToken].
 */
class AssetVocabularyRepository(
    private val appContext: Context,
    private val language: String,
) : VocabularyRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): List<PictogramToken> =
        withContext(Dispatchers.IO) {
            appContext.assets.open("arasaac/catalog_$language.json").use { stream ->
                json.decodeFromString<List<PictogramToken>>(stream.readBytes().decodeToString())
            }
        }
}
