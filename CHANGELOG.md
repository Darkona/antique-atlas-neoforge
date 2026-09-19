# Changelog

Changes of this NeoForge fork. Issue numbers such as `antique-atlas#358` refer to the
[original project's issues](https://github.com/sisby-folk/antique-atlas/issues); the bugs they describe also existed in
this fork until the listed commit.

## Unreleased

Nothing yet.

## 3.1.2+1.21-neoforge

First release of the NeoForge port of Antique Atlas 4 (3.1.2). Same features, art, resource-pack format and mod id as
the original. Needs [Surveyor for NeoForge](https://github.com/Darkona/surveyor-neoforge).

### Platform
- Runs natively on NeoForge 21.1 (Minecraft 1.21.1): no Sinytra Connector or Fabric API. (`6e372b4d`)
- The configuration file uses NeoForge's format; `structureMarkers` and dimension `scales` are lists of `"name=value"`
  entries.

### Fixed (bugs of the original)
- Singleplayer is safe from races between the game and its built-in server, which could crash the game while the map
  was being drawn. (`6e372b4d`)
- The map updates when terrain changes, instead of keeping the old drawing until you rejoin. (`6e372b4d`)
- The list of your markers keeps the same order every session. (`6e372b4d`)
- Pinned markers at the edge of the map fade the same way on all four sides. (`6e372b4d`)
- A dimension explored along a single row of chunks is no longer hidden from the dimension list. (`6e372b4d`)
- Memory is no longer kept from previous singleplayer worlds. (`6e372b4d`)
- The first tile of the drawn area, and the last row and column when zoomed out, were only half drawn (inside the
  margin around the visible map). (`62217fdd`)

### Performance
- Holding the atlas no longer checks for shader mods hundreds of times per frame. (`6e372b4d`)
- Drawing the map, its markers and players creates next to no garbage per frame; markers are drawn in batches.
  (`6e372b4d`)
- Working out a chunk's tile is cheaper, and the map is only rebuilt when something on it changed. (`6e372b4d`)

### Development
- Regression tests: tile iterator coverage and shapes, tiling rules, elevation bands, allocation checks for per-frame
  code, and source guards for thread and caching rules. (`6e372b4d`, `62217fdd`)
