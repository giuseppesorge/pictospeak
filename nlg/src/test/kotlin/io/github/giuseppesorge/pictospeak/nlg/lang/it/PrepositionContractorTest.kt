package io.github.giuseppesorge.pictospeak.nlg.lang.it

import org.junit.Assert.assertEquals
import org.junit.Test

class PrepositionContractorTest {
    @Test
    fun `the full contraction table`() {
        val articles = listOf("il", "lo", "la", "l'", "i", "gli", "le")
        val expected =
            mapOf(
                "di" to listOf("del", "dello", "della", "dell'", "dei", "degli", "delle"),
                "a" to listOf("al", "allo", "alla", "all'", "ai", "agli", "alle"),
                "da" to listOf("dal", "dallo", "dalla", "dall'", "dai", "dagli", "dalle"),
                "in" to listOf("nel", "nello", "nella", "nell'", "nei", "negli", "nelle"),
                "su" to listOf("sul", "sullo", "sulla", "sull'", "sui", "sugli", "sulle"),
            )
        expected.forEach { (prep, forms) ->
            articles.zip(forms).forEach { (article, contraction) ->
                assertEquals(contraction, PrepositionContractor.contract(prep, article))
            }
        }
    }

    @Test
    fun `per, tra and fra never contract`() {
        assertEquals("per il", PrepositionContractor.contract("per", "il"))
        assertEquals("tra le", PrepositionContractor.contract("tra", "le"))
        assertEquals("fra gli", PrepositionContractor.contract("fra", "gli"))
    }
}
