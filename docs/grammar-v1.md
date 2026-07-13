# Grammar v1 — frozen scope

The sentence engine turns a telegraphic pictogram sequence into a small set of
grammatical sentence proposals. **This scope is frozen.** Adding any grammatical
phenomenon requires a failing real-user scenario plus a new ADR — a good idea is not
enough. Keep each language's engine under ~2k LOC.

Universal rules (every language):

- The engine is a pipeline of pure functions:
  `SlotTagger → PatternMatcher → Realizer → CandidateBuilder`.
- Unmatched input degrades to `FALLBACK_CONCAT` (plain label concatenation). Never throw.
- Missing morphology data degrades to citation form. Never guess silently.
- Semantic ambiguity (article definiteness, tense) is resolved by *proposal*: default
  candidate + tap-to-cycle alternatives.
- Morphological facts that are not rule-derivable (verb auxiliaries, irregular participles,
  irregular inflections) are **lexicon data**, never code.
- Every candidate carries a `trace` (pattern id + morphology decisions) for golden tests.
- Golden-file test corpora live in `nlg/src/test/resources/golden/{lang}/`; goldens change
  only with linguistic justification in the PR.

Canonical example (English pack): "I want eat pizza" → "I want to eat pizza".

## English (`en`) — v1 scope

Included: present simple (3rd-person -s), modal want/can/must + to-infinitive, simple past
(irregular forms as lexicon data), articles a/an/the (phonological a/an rule),
plural -s/-es with irregulars from the lexicon, negation with do-support in the fixed
patterns listed below, sentence patterns: S-V, S-V-O(±Adj), S-Modal-Inf-O, negated and
past variants, social-phrase passthrough.

Explicitly excluded (v1): progressive/perfect aspects, questions, passives, phrasal verbs.

## Italian (`it`) — v1 scope

Included:

1. **Presente indicativo**, 6 persons; -are/-ere/-ire conjugations incl. -isc- verbs;
   irregular presente forms from the lexicon.
2. **Modal + infinitive**: volere/potere/dovere + infinitive.
3. **Passato prossimo** with per-verb auxiliary (essere/avere) as lexicon data and
   participle agreement under essere. (The auxiliary is famously not rule-derivable:
   "è successo", never "ha successo".)
4. **Articles** (definite/indefinite) with phonological rules: il/lo/l'/la/i/gli/le,
   un/uno/una/un'; lo/gli before s+consonant, z, gn, ps; elision before vowels.
5. **Articulated prepositions** for di/a/da/in/su (per/tra/fra never contract).
6. **Gender/number agreement** noun ↔ adjective.
7. **Negation**: pre-verbal "non".
8. Same sentence-pattern set as English.

Explicitly excluded (v1): clitics, subjunctive, passives, passato remoto, imperative,
questions.

Lexicon entry (Italian): lemma, POS, gender, singular/plural, verb class, auxiliary,
past participle, **transitivity**, irregular present forms. Derived offline from Morph-it!
(LGPL option) intersected with a curated core vocabulary (`tools/core-vocabulary.csv`).

Not-rule-derivable facts that are **curated data**, reviewed by a linguist:
- `tools/lexicon-build/verbs_it.csv` — auxiliary (essere/avere), reflexivity, and
  **transitivity**. An object noun after an *intransitive* verb needs a preposition
  ("vado **a** casa") which v1 does not build, so it degrades to concat — never "vado la casa".
- `tools/lexicon-build/non_attributive_it.csv` — DESCRIPTOR lemmas that are NOT postnominal
  agreeing adjectives (quantifiers "molto", possessives "tuo", demonstratives "questo",
  adverbs "dopo"). The realizer builds only postnominal descriptive adjectives ("la pizza
  rossa"); these are marked unsupported and their sentences degrade to concat rather than
  being misplaced ("*pizza molta") or wrongly inflected ("*dopa").
- `tools/lexicon-build/overrides_it.csv` — patches for Morph-it! data errors (wrong gender,
  variant-form selection, apostrophe-encoded accents), each documented.

The realizer refuses (→ concat) rather than guess when: the object noun is unsupported and
carries an adjective (cannot agree it); an adjective precedes its object noun; "non" precedes
the subject; or two finite verbs appear. It never drops a user-selected word.

## Adding a language

A new language = a new `Realizer` + lexicon + golden corpus behind the same interfaces —
never a change to the engine core (see `language-packs.md`). Start from the pattern list
above; scope morphology to what the patterns need and freeze it in this file.
