# ADR-0008: Open Board Format import/export deferred; data model kept isomorphic

**Status**: accepted

## Context
OBF (.obf/.obz) interop is the right anti-lock-in story and enables board exchange with
other AAC tools, but the codec + image-resolution mapping is real work that competes with
the sentence engine for the same weeks.

## Decision
Defer the codec. Keep the board JSON deliberately isomorphic to OBF (grid rows/cols, cells
with pictogram id | board link | hidden; one locale per board — OBF's own convention) so
the future codec is mechanical.
