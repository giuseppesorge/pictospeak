package io.github.giuseppesorge.pictospeak.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.security.MessageDigest

class ModelStoreTest {
    private fun store() = ModelStore(Files.createTempDirectory("models-test").toFile())

    private fun sha256Hex(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `import records size and sha256 and current returns it`() {
        val store = store()
        val bytes = "pretend-model-weights".toByteArray()
        val info = store.importFrom(ByteArrayInputStream(bytes), "gemma3-270m.litertlm")
        assertEquals(bytes.size.toLong(), info.sizeBytes)
        assertEquals(sha256Hex(bytes), info.sha256)
        assertEquals(info, store.current())
        assertNotNull(store.currentModelPath())
    }

    @Test
    fun `importing a second model replaces the first`() {
        val store = store()
        store.importFrom(ByteArrayInputStream("one".toByteArray()), "a.litertlm")
        val second = store.importFrom(ByteArrayInputStream("two".toByteArray()), "b.litertlm")
        assertEquals("b.litertlm", store.current()?.fileName)
        assertEquals(second, store.current())
        assertTrue(store.currentModelPath()!!.endsWith("b.litertlm"))
    }

    @Test
    fun `no model imported yields null`() {
        val store = store()
        assertNull(store.current())
        assertNull(store.currentModelPath())
    }

    @Test
    fun `clear forgets the model`() {
        val store = store()
        store.importFrom(ByteArrayInputStream("x".toByteArray()), "m.litertlm")
        store.clear()
        assertNull(store.current())
    }

    @Test
    fun `a path-traversing display name is sanitized to a bare file`() {
        val store = store()
        val info = store.importFrom(ByteArrayInputStream("x".toByteArray()), "../../etc/passwd")
        assertTrue(!info.fileName.contains("/"))
        assertEquals(info, store.current())
    }
}
