# Dungeon Spawn Task

## Goal

Create a small Paper/Purpur plugin that teleports players to randomized safe
locations around the outer area of a WorldGuard dungeon region.

The purpose is to reduce spawn camping by preventing players from always
entering the dungeon at the exact same location.

## Requirements

- Use an existing WorldGuard region as the allowed spawn area
- World name and region name must be configurable
- Find randomized spawn positions close to the outer area of the region
- Never teleport a player outside the configured WorldGuard region
- Avoid spawning directly next to another player if possible.
- Handle edgecases.. set yourself some more requirements..

## Delivery

- Keep the implementation self-contained in this repository.
- Open a pull request when finished.
- Briefly explain the chosen approach in the PR.
- Mention assumptions, edge cases and known limitations.
- Do not commit server files, secrets, IDE folders or generated build output.

## Notes

How the region edge is determined, how candidate positions are selected and how
spawn safety is evaluated are not specified.
