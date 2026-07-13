# ADR-0007: No navigation library

**Status**: accepted

## Context
The app has ~4 static screens. navigation-compose is itself churning (Nav2 -> Nav3) and is
an upgrade surface we do not need.

## Decision
A sealed Screen class + when in the single MainActivity.

## Revisit trigger
Deep links, multi-pane navigation, or a screen graph that stops fitting in one when.
