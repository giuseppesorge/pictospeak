# ADR-0002: Versioned JSON files, no Room

**Status**: accepted

## Context
The app's data is small (hundreds of catalog entries, a handful of profiles) and must be
diffable, debuggable, and maintainable by occasional contributors.

## Decision
kotlinx.serialization JSON files: read-only content in assets/ (pipeline-generated),
mutable state in filesDir/ with atomic temp-file+rename writes and a schemaVersion field
on every root object. Import/export via SAF.

## Revisit trigger
Relational queries over thousands of entries (e.g. full 13k-pictogram catalog search).
Until then, Room's migrations and DAO layer are pure handover burden.
