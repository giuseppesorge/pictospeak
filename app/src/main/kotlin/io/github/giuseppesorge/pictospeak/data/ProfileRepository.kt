package io.github.giuseppesorge.pictospeak.data

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads and persists the [Profile] as JSON in filesDir (no Room — ADR-0002). Writes are
 * atomic (temp file + rename) so a crash mid-write can never corrupt the settings. Never
 * touches the network.
 */
class ProfileRepository(
    filesDir: File,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    private val file = File(filesDir, FILE_NAME)

    fun load(): Profile =
        runCatching {
            if (file.exists()) json.decodeFromString<Profile>(file.readText()) else Profile()
        }.getOrDefault(Profile())

    fun save(profile: Profile) {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(json.encodeToString(profile))
        check(tmp.renameTo(file)) { "failed to persist profile" }
    }

    private companion object {
        const val FILE_NAME = "settings.json"
    }
}
