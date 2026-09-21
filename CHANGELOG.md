# Changelog

Changes of this NeoForge fork, by date and feature. Issue numbers such as `antique-atlas#358` refer to the
[original project's issues](https://github.com/sisby-folk/antique-atlas/issues); the bugs they describe also existed
here until the date listed.

## 2026-09-28

### NeoForge port
- Antique Atlas 4 (3.1.2) runs natively on NeoForge 21.1 (Minecraft 1.21.1): no Sinytra Connector or Fabric API. Same
  features, art, resource-pack format and mod id as the original. Needs
  [Surveyor for NeoForge](https://github.com/Darkona/surveyor-neoforge).
- The configuration file uses NeoForge's format; `structureMarkers` and dimension `scales` are lists of `"name=value"`
  entries.

### Configuration
- The mod list's Config button opens a config screen with every option; it used to be disabled. (antique-atlas#390,
  antique-atlas#372)

### Map drawing
- Reloading resources (F3+T) redraws the atlas with the new textures and tile rules; it used to keep the old ones until
  you rejoined. (antique-atlas#243)
- The book frame's soft edge over the map is translucent again. (antique-atlas#358)
- Hovering your own player icon no longer shifts everything drawn after it in that frame.
- The map updates when terrain changes, instead of keeping the old drawing until you rejoin.
- A dimension explored along a single row of chunks is no longer hidden from the dimension list.
- The first tile of the drawn area, and the last row and column when zoomed out, were only half drawn (inside the
  margin around the visible map).

### Handheld atlas
- Sneak and use the atlas book to zoom the handheld map out: 1, 2 or 4 chunks per tile. (antique-atlas#353)
- A book named with the translated atlas name, and the creative atlas in any language, work as an atlas; only the
  English name worked before. (antique-atlas#367)

### Markers
- Middle-click a marker to copy its coordinates. (antique-atlas#169)
- Other players' graves show their owner's name. (antique-atlas#342)
- The list of your markers keeps the same order every session.
- Pinned markers at the edge of the map fade the same way on all four sides.

### Stability
- Singleplayer is safe from races between the game and its built-in server, which could crash the game while the map
  was being drawn.
- Memory is no longer kept from previous singleplayer worlds.

### Performance
- Holding the atlas no longer checks for shader mods hundreds of times per frame.
- Drawing the map, its markers and players creates next to no garbage per frame; markers are drawn in batches.
- Working out a chunk's tile is cheaper, and the map is only rebuilt when something on it changed.

### Tests
- Regression tests for tile iterator coverage and shapes, tiling rules, elevation bands, allocation checks for
  per-frame code, and source guards for thread and caching rules.
