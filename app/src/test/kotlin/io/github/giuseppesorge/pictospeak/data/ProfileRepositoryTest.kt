package io.github.giuseppesorge.pictospeak.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProfileRepositoryTest {
    private fun tempDir(): File = Files.createTempDirectory("profile-test").toFile()

    @Test
    fun `missing file yields defaults`() {
        val profile = ProfileRepository(tempDir()).load()
        assertEquals("it", profile.language)
        assertTrue(!profile.setupComplete)
    }

    @Test
    fun `save then load round-trips`() {
        val dir = tempDir()
        val repo = ProfileRepository(dir)
        val edited = Profile(language = "en", ttsRate = 1.2f, speakLabelOnTap = true, setupComplete = true)
        repo.save(edited)
        assertEquals(edited, ProfileRepository(dir).load())
    }

    @Test
    fun `unknown or corrupt content falls back to defaults, never throws`() {
        val dir = tempDir()
        File(dir, "settings.json").writeText("{ not valid json")
        assertEquals(Profile(), ProfileRepository(dir).load())
    }

    @Test
    fun `forward-compatible - unknown fields are ignored`() {
        val dir = tempDir()
        File(dir, "settings.json").writeText("""{"language":"en","futureField":42}""")
        assertEquals("en", ProfileRepository(dir).load().language)
    }

    // SAF export/import round-trip (the Uri plumbing lives in the UI; the payload is here).
    @Test
    fun `serialize then deserialize round-trips`() {
        val repo = ProfileRepository(tempDir())
        val profile = Profile(language = "en", ttsRate = 1.1f, speakLabelOnTap = true)
        assertEquals(profile, repo.deserialize(repo.serialize(profile)))
    }

    @Test
    fun `deserialize of junk returns null, never throws`() {
        assertEquals(null, ProfileRepository(tempDir()).deserialize("not json at all"))
    }

    // An imported/hand-edited unsupported language must be clamped, not persisted verbatim:
    // otherwise the board later reads a non-existent lexicon asset and crash-loops on launch.
    @Test
    fun `deserialize clamps an unsupported language to a shipped one`() {
        val imported = ProfileRepository(tempDir()).deserialize("""{"language":"fr","setupComplete":true}""")
        assertEquals("it", imported?.language)
        // Other fields survive the clamp.
        assertTrue(imported?.setupComplete == true)
    }

    @Test
    fun `load clamps an unsupported language written straight to disk`() {
        val dir = tempDir()
        File(dir, "settings.json").writeText("""{"language":"fr"}""")
        assertEquals("it", ProfileRepository(dir).load().language)
    }

    @Test
    fun `supported languages are preserved`() {
        val repo = ProfileRepository(tempDir())
        assertEquals("en", repo.deserialize("""{"language":"en"}""")?.language)
    }

    // gridColumns <= 0 would make GridCells.Fixed throw and crash-loop the board — clamp it.
    @Test
    fun `deserialize clamps a non-positive gridColumns`() {
        val repo = ProfileRepository(tempDir())
        assertTrue((repo.deserialize("""{"gridColumns":0}""")?.gridColumns ?: 0) >= Profile.GRID_COLUMNS_MIN)
        assertTrue((repo.deserialize("""{"gridColumns":-3}""")?.gridColumns ?: 0) >= Profile.GRID_COLUMNS_MIN)
    }

    @Test
    fun `deserialize caps an absurd gridColumns`() {
        assertEquals(
            Profile.GRID_COLUMNS_MAX,
            ProfileRepository(tempDir()).deserialize("""{"gridColumns":999}""")?.gridColumns,
        )
    }

    @Test
    fun `a valid gridColumns is preserved`() {
        assertEquals(5, ProfileRepository(tempDir()).deserialize("""{"gridColumns":5}""")?.gridColumns)
    }
}
