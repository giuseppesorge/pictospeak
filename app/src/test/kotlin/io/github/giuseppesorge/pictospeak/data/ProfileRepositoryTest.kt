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
}
