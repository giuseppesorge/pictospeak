#!/usr/bin/env bash
# License/attribution compliance gate — part of the definition of done, run in CI.
# Fails the build if any required compliance surface is missing.
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0
err() {
  echo "LICENSE-CHECK FAIL: $1" >&2
  fail=1
}

# 1. Core legal files exist.
[[ -f LICENSE ]] || err "LICENSE missing"
grep -q "Apache License" LICENSE || err "LICENSE is not Apache-2.0"
[[ -f THIRD_PARTY_NOTICES.md ]] || err "THIRD_PARTY_NOTICES.md missing"

# 2. ARASAAC attribution present in README and notices (Italian + English wording).
# Whitespace-normalized so hard-wrapped Markdown lines still match.
has_phrase() { tr '\n' ' ' < "$1" | tr -s ' ' | grep -qi "$2"; }
has_phrase README.md "Sergio Palao" || err "ARASAAC attribution (author) missing from README.md"
has_phrase README.md "Government of Aragon" || err "ARASAAC attribution (owner, EN) missing from README.md"
has_phrase README.md "Governo di Aragona" || err "ARASAAC attribution (owner, IT) missing from README.md"
has_phrase README.md "BY-NC-SA" || err "ARASAAC license name missing from README.md"
has_phrase THIRD_PARTY_NOTICES.md "Sergio Palao" || err "ARASAAC attribution missing from THIRD_PARTY_NOTICES.md"

# 3. License-fenced asset directories carry their own terms once they exist.
if [[ -d app/src/main/assets/arasaac ]]; then
  [[ -f app/src/main/assets/arasaac/LICENSE ]] || err "assets/arasaac present without LICENSE"
  [[ -f app/src/main/assets/arasaac/ATTRIBUTION.md ]] || err "assets/arasaac present without ATTRIBUTION.md"
fi
if [[ -d app/src/main/assets/lexicon ]]; then
  [[ -f app/src/main/assets/lexicon/LICENSE ]] || err "assets/lexicon present without LICENSE"
  [[ -f app/src/main/assets/lexicon/PROVENANCE.md ]] || err "assets/lexicon present without PROVENANCE.md"
fi

# 4. Model weights must never be committed.
if git ls-files | grep -E '\.(litertlm|task)$' | grep -q .; then
  err "model weight files are committed — forbidden (llm/NOTICE-models.md)"
fi

if [[ $fail -ne 0 ]]; then
  exit 1
fi
echo "license-check: OK"
