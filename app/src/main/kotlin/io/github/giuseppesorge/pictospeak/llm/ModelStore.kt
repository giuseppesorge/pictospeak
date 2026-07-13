package io.github.giuseppesorge.pictospeak.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * Metadata for an imported model file, recorded at import time (llm/NOTICE-models.md).
 * The SHA-256 and size are shown to the user so they can verify the weights they imported.
 */
@Serializable
data class ModelInfo(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
)

/**
 * Manages the single, user-imported `.litertlm` model file living privately in
 * `filesDir/models/` (weights are NEVER bundled — llm/NOTICE-models.md, CLAUDE.md). Import
 * is via SAF in the play flavor; this class only deals with the resolved [InputStream] and
 * the private directory, so its core is JVM-unit-testable.
 *
 * Deliberately holds at most one model: importing a new one replaces the old.
 */
class ModelStore(
    private val modelsDir: File,
) {
    private val json = Json { prettyPrint = true }
    private val metaFile = File(modelsDir, META_FILE_NAME)

    /**
     * Copy [input] into the private models dir under a sanitized [displayName], streaming a
     * SHA-256 as it writes, and record the metadata. Any previous model is removed first.
     * The write is atomic (temp file + rename). Never partially replaces on failure.
     */
    fun importFrom(
        input: InputStream,
        displayName: String,
    ): ModelInfo {
        modelsDir.mkdirs()
        val safeName = sanitizeFileName(displayName)
        val target = File(modelsDir, safeName)
        val tmp = File(modelsDir, "$safeName.tmp")

        val digest = MessageDigest.getInstance("SHA-256")
        // copyTo reads through the DigestInputStream, so the SHA-256 is computed as we write.
        val size =
            DigestInputStream(input, digest).use { digestStream ->
                tmp.outputStream().use { out -> digestStream.copyTo(out) }
            }

        // Remove the previous model (and any stale temp) but keep the freshly written temp
        // so the rename below can succeed.
        clearModelFiles(keep = tmp)
        check(tmp.renameTo(target)) { "failed to store imported model" }

        val info = ModelInfo(fileName = safeName, sizeBytes = size, sha256 = digest.digest().toHex())
        metaFile.writeText(json.encodeToString(info))
        return info
    }

    /** The currently imported model, or null if none has been imported (or files went missing). */
    fun current(): ModelInfo? {
        val info = runCatching { json.decodeFromString<ModelInfo>(metaFile.readText()) }.getOrNull() ?: return null
        return info.takeIf { File(modelsDir, it.fileName).exists() }
    }

    /** Absolute path of the imported model file, for the refiner; null if none. */
    fun currentModelPath(): String? = current()?.let { File(modelsDir, it.fileName).absolutePath }

    /** Forget and delete the imported model. */
    fun clear() {
        clearModelFiles(keep = null)
        metaFile.delete()
    }

    private fun clearModelFiles(keep: File?) {
        modelsDir.listFiles()?.forEach { if (it.name != META_FILE_NAME && it != keep) it.delete() }
    }

    private fun sanitizeFileName(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "model.litertlm" }
        return base.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val META_FILE_NAME = "model.meta.json"
    }
}
