package io.github.giuseppesorge.pictospeak.nlg.lang.en

/**
 * Self-contained English fixture (the engine tests do not depend on the bundled asset).
 * Regular forms are spelled out here as the English lexicon-build would compute them.
 */
object EnglishTestLexicon {
    @Suppress("LongParameterList")
    private fun v(
        lemma: String,
        third: String,
        past: String,
        participle: String,
        transitive: Boolean = false,
        modal: Boolean = false,
    ) = EnVerb(lemma, lemma, third, past, participle, transitive, modal)

    val lexicon =
        EnglishLexicon(
            language = "en",
            entries =
                listOf(
                    v("want", "wants", "wanted", "wanted", transitive = true),
                    v("can", "can", "could", "could", modal = true),
                    v("must", "must", "must", "must", modal = true),
                    v("be", "is", "was", "been"),
                    v("have", "has", "had", "had", transitive = true),
                    v("eat", "eats", "ate", "eaten", transitive = true),
                    v("drink", "drinks", "drank", "drunk", transitive = true),
                    v("go", "goes", "went", "gone"),
                    v("sleep", "sleeps", "slept", "slept"),
                    v("play", "plays", "played", "played"),
                    v("see", "sees", "saw", "seen", transitive = true),
                    v("watch", "watches", "watched", "watched", transitive = true),
                    EnNoun("pizza", "pizza", "pizzas"),
                    EnNoun("water", "water", null),
                    EnNoun("apple", "apple", "apples"),
                    EnNoun("egg", "egg", "eggs"),
                    EnNoun("house", "house", "houses"),
                    EnNoun("mum", "mum", "mums"),
                    EnNoun("book", "book", "books"),
                    EnAdjective("big", "big"),
                    EnAdjective("red", "red"),
                    EnAdjective("happy", "happy"),
                    EnAdjective("tired", "tired"),
                    EnAdjective("old", "old"),
                ),
            unsupported = listOf("now", "here"),
        )
}
