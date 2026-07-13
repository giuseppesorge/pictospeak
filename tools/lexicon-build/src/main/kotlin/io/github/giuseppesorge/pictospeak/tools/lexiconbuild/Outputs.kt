package io.github.giuseppesorge.pictospeak.tools.lexiconbuild

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

internal fun writeReferenceTable(
    repoRoot: Path,
    lang: String,
    rows: List<MorphRow>,
    usedLemmas: Set<String>,
) {
    val dir = repoRoot.resolve("app/src/test/resources/lexicon")
    Files.createDirectories(dir)
    val used = usedLemmas.map { baseLemma(it.lowercase()) }.toSet() + usedLemmas.map { it.lowercase() }
    val content =
        rows
            .filter { it.lemma in used }
            .sortedWith(compareBy({ it.lemma }, { it.features }, { it.form }))
            .joinToString("\n") { "${it.form}\t${it.lemma}\t${it.features}" }
    Files.writeString(dir.resolve("morphit_reference_$lang.tsv"), content + "\n")
    println("reference table: morphit_reference_$lang.tsv (${content.lines().size} rows)")
}

internal fun writeLicense(assetsDir: Path) {
    Files.writeString(
        assetsDir.resolve("LICENSE"),
        """
        The lexicon data in this directory is DERIVED from Morph-it! (Eros Zanchetta and
        Marco Baroni, "Morph-it! A free corpus-based morphological resource for the Italian
        language") and is NOT covered by the repository's Apache-2.0 code license.

        Morph-it! is dual-licensed under the Creative Commons Attribution-ShareAlike 2.0
        license and the GNU Lesser General Public License (LGPL). This project uses it
        under the LGPL option (see docs/adr/0006 for the licensing layering).

        The derived lexicon files here are therefore distributed under the LGPL.
        Derivation is fully reproducible: see PROVENANCE.md and tools/lexicon-build.
        """.trimIndent() + "\n",
    )
}

internal fun writeProvenance(
    assetsDir: Path,
    repoRoot: Path,
) {
    val tgz = repoRoot.resolve("assets-src/morph-it/morph-it.tgz")
    val sha = if (tgz.exists()) sha256(tgz) else "(archive not present — re-download to verify)"
    Files.writeString(
        assetsDir.resolve("PROVENANCE.md"),
        """
        # Lexicon provenance

        - Source: Morph-it! v0.48 (505,074 forms), Zanchetta & Baroni, University of Bologna.
        - Download: https://docs.sslmit.unibo.it/doku.php?id=resources:morph-it
          (archive `morph-it.tgz`, kept in `assets-src/morph-it/`, sha256 `$sha`).
        - Derivation: `./gradlew :tools:lexicon-build:run` — selects the curated vocabulary's
          lemmas (`tools/core-vocabulary.csv`), extracts present indicative, past participle,
          noun gender/number and adjective agreement forms; merges the hand-curated
          auxiliary table `tools/lexicon-build/aux_it.csv` (essere/avere is lexical data,
          not rule-derivable) and the data patches in `tools/lexicon-build/overrides_it.csv`.
        - Encoding note: Morph-it! is ISO-8859-1; outputs are UTF-8.
        """.trimIndent() + "\n",
    )
}

private fun sha256(path: Path): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }
}
