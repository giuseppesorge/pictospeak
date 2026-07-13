package io.github.giuseppesorge.pictospeak.nlg.lang.it

import io.github.giuseppesorge.pictospeak.nlg.api.AdjectiveEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Auxiliary
import io.github.giuseppesorge.pictospeak.nlg.api.Gender
import io.github.giuseppesorge.pictospeak.nlg.api.Lexicon
import io.github.giuseppesorge.pictospeak.nlg.api.NounEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Person
import io.github.giuseppesorge.pictospeak.nlg.api.VerbEntry

/**
 * Self-contained fixture so the engine's tests do not depend on the bundled asset
 * (which is validated separately in :app). Includes "succedere" to encode the
 * SimpleNLG-IT lesson: the auxiliary is lexical data — "è successo", never "ha successo".
 */
object TestLexicon {
    @Suppress("LongParameterList")
    private fun verb(
        lemma: String,
        aux: Auxiliary,
        participle: String,
        forms: List<String>,
        transitive: Boolean = false,
        reflexive: Boolean = false,
    ) = VerbEntry(
        lemma = lemma,
        auxiliary = aux,
        pastParticiple = participle,
        presentIndicative = Person.entries.zip(forms).toMap(),
        transitive = transitive,
        reflexive = reflexive,
    )

    // NOTE: object properties initialize top-to-bottom, so the verb entries are built here
    // inline (no forward references to helper lists).
    val lexicon =
        Lexicon(
            language = "it",
            entries =
                listOf(
                    verb(
                        "volere",
                        Auxiliary.AVERE,
                        "voluto",
                        l("voglio", "vuoi", "vuole", "vogliamo", "volete", "vogliono"),
                        transitive = true,
                    ),
                    verb(
                        "potere",
                        Auxiliary.AVERE,
                        "potuto",
                        l("posso", "puoi", "può", "possiamo", "potete", "possono"),
                    ),
                    verb(
                        "dovere",
                        Auxiliary.AVERE,
                        "dovuto",
                        l("devo", "devi", "deve", "dobbiamo", "dovete", "devono"),
                    ),
                    verb("essere", Auxiliary.ESSERE, "stato", l("sono", "sei", "è", "siamo", "siete", "sono")),
                    verb(
                        "avere",
                        Auxiliary.AVERE,
                        "avuto",
                        l("ho", "hai", "ha", "abbiamo", "avete", "hanno"),
                        transitive = true,
                    ),
                    verb(
                        "fare",
                        Auxiliary.AVERE,
                        "fatto",
                        l("faccio", "fai", "fa", "facciamo", "fate", "fanno"),
                        transitive = true,
                    ),
                    verb(
                        "mangiare",
                        Auxiliary.AVERE,
                        "mangiato",
                        l("mangio", "mangi", "mangia", "mangiamo", "mangiate", "mangiano"),
                        transitive = true,
                    ),
                    verb(
                        "bere",
                        Auxiliary.AVERE,
                        "bevuto",
                        l("bevo", "bevi", "beve", "beviamo", "bevete", "bevono"),
                        transitive = true,
                    ),
                    verb("andare", Auxiliary.ESSERE, "andato", l("vado", "vai", "va", "andiamo", "andate", "vanno")),
                    verb(
                        "dormire",
                        Auxiliary.AVERE,
                        "dormito",
                        l("dormo", "dormi", "dorme", "dormiamo", "dormite", "dormono"),
                    ),
                    verb(
                        "giocare",
                        Auxiliary.AVERE,
                        "giocato",
                        l("gioco", "giochi", "gioca", "giochiamo", "giocate", "giocano"),
                    ),
                    verb(
                        "cadere",
                        Auxiliary.ESSERE,
                        "caduto",
                        l("cado", "cadi", "cade", "cadiamo", "cadete", "cadono"),
                    ),
                    verb(
                        "succedere",
                        Auxiliary.ESSERE,
                        "successo",
                        l("succedo", "succedi", "succede", "succediamo", "succedete", "succedono"),
                    ),
                    verb(
                        "sedersi",
                        Auxiliary.ESSERE,
                        "seduto",
                        l("siedo", "siedi", "siede", "sediamo", "sedete", "siedono"),
                        reflexive = true,
                    ),
                    NounEntry("pizza", Gender.FEMININE, "pizza", "pizze"),
                    NounEntry("acqua", Gender.FEMININE, "acqua", "acque"),
                    NounEntry("mamma", Gender.FEMININE, "mamma", "mamme"),
                    NounEntry("amica", Gender.FEMININE, "amica", "amiche"),
                    NounEntry("zaino", Gender.MASCULINE, "zaino", "zaini"),
                    NounEntry("amico", Gender.MASCULINE, "amico", "amici"),
                    NounEntry("gelato", Gender.MASCULINE, "gelato", "gelati"),
                    NounEntry("gnomo", Gender.MASCULINE, "gnomo", "gnomi"),
                    NounEntry("yogurt", Gender.MASCULINE, "yogurt", null),
                    NounEntry("uovo", Gender.MASCULINE, "uovo", "uova"),
                    NounEntry("casa", Gender.FEMININE, "casa", "case"),
                    AdjectiveEntry("grande", "grande", "grande", "grandi", "grandi"),
                    AdjectiveEntry("rosso", "rosso", "rossa", "rossi", "rosse"),
                    AdjectiveEntry("felice", "felice", "felice", "felici", "felici"),
                    AdjectiveEntry("stanco", "stanco", "stanca", "stanchi", "stanche"),
                    AdjectiveEntry("rotto", "rotto", "rotta", "rotti", "rotte"),
                ),
            unsupported = listOf("patatine fritte", "dopo"),
        )

    private fun l(vararg forms: String): List<String> = forms.toList()
}
