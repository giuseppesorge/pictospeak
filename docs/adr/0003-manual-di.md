# ADR-0003: Manual dependency wiring, no DI framework

**Status**: accepted

## Context
A DI framework (Hilt/Koin) buys compile-time graphs at the cost of annotation processing,
build complexity, and a learning curve for occasional contributors.

## Decision
One AppContainer class with lazy singletons, small enough to read in one screen.
Constructor injection everywhere; fakes in tests are plain constructor arguments.

## Revisit trigger
The container stops fitting in one screen, or scoped lifetimes appear (they should not in
this app).
