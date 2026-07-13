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

## Italian (`it`) — v1 scope

Included:

1. **Presente indicativo**, 6 persons; -are/-ere/-ire conjugations incl. -isc- verbs;
   irregular presente forms from the lexicon.
2. **Modal + infinitive**: volere/potere/dovere + infinito
   ("io volere mangiare pizza" → "io voglio mangiare la pizza").
3. **Passato prossimo** with per-verb auxiliary (essere/avere) as lexicon data and
   participle agreement under essere. (The auxiliary is famously not rule-derivable:
   "è successo", never "ha successo".)
4. **Articles** (definite/indefinite) with phonological rules: il/lo/l'/la/i/gli/le,
   un/uno/una/un'; lo/gli before s+consonant, z, gn, ps; elision before vowels.
5. **Preposizioni articolate** for di/a/da/in/su (per/tra/fra never contract).
6. **Gender/number agreement** noun ↔ adjective.
7. **Negation**: pre-verbal "non".
8. Sentence patterns: S-V, S-V-O(±Adj), S-Modal-Inf-O, negated variants, passato-prossimo
   variants, social-phrase passthrough (ciao/grazie/aiuto…).

Explicitly excluded (v1): clitics, congiuntivo, passives, passato remoto, imperativo,
questions.

Lexicon entry (Italian): lemma, POS, gender, singular/plural, verb class, auxiliary,
past participle, irregular presente forms. Derived offline from Morph-it! (LGPL option)
intersected with a curated core vocabulary (`tools/core-vocabulary.csv`).

## English (`en`) — v1 scope

Included: present simple (3rd-person -s), modal want/can/must + to-infinitive, simple past
(irregular forms as lexicon data), articles a/an/the (phonological a/an rule),
plural -s/-es with irregulars from the lexicon, negation with do-support in the fixed
patterns above, the same sentence patterns as Italian.

Explicitly excluded (v1): progressive/perfect aspects, questions, passives, phrasal verbs.

## Adding a language

A new language = a new `Realizer` + lexicon + golden corpus behind the same interfaces —
never a change to the engine core (see `language-packs.md`). Start from the pattern list
above; scope morphology to what the patterns need and freeze it in this file.
