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
- Joining another share group redraws the map from the new group's exploration; the old group's areas used to stay.
  (antique-atlas#343)
- Reloading resources (F3+T) redraws the atlas with the new textures and tile rules; it used to keep the old ones until
  you rejoined. (antique-atlas#243)
- The book frame's soft edge over the map is translucent again. (antique-atlas#358)
- Hovering your own player icon no longer shifts everything drawn after it in that frame.
- Option `fadeEdges` (off by default): tiles next to unexplored areas fade out towards them instead of ending in a hard
  square edge. (antique-atlas#87)
- The map updates when terrain changes, instead of keeping the old drawing until you rejoin.
- A dimension explored along a single row of chunks is no longer hidden from the dimension list.
- The first tile of the drawn area, and the last row and column when zoomed out, were only half drawn (inside the
  margin around the visible map).
- Holes to the void and the edges of End islands are drawn with the empty or End void tile, not a random biome of the
  region.
- A biome outside a region's palette no longer crashes the game every tick; that chunk is left undrawn.

### Map export
- A new bookmark saves the whole explored area of the shown dimension as a PNG in `screenshots/atlas/`, at 16 pixels per
  tile, zoomed out as needed to fit 4096 pixels. It doesn't use AWT, so it also works on macOS. (antique-atlas#216)

### Minimap
- An optional minimap in a corner of the screen, with the atlas's tiles, markers and other players. Off by default
  (`minimap`); corner, size, chunks per tile and markers are configurable. It is hidden while a screen, F1 or F3 is
  open, and respects `requireItem`. (antique-atlas#254)

### Handheld atlas
- Sneak and use the atlas book to zoom the handheld map out: 1, 2 or 4 chunks per tile. (antique-atlas#353)
- A book named with the translated atlas name, and the creative atlas in any language, work as an atlas; only the
  English name worked before. (antique-atlas#367)

### Markers
- Middle-click a marker to copy its coordinates. (antique-atlas#169)
- Structure icons can be picked for your own markers: by default only on servers without Surveyor, where structures
  aren't discovered for you. Option `pickStructureMarkers`. (antique-atlas#171)
- Other players' graves show their owner's name. (antique-atlas#342)
- The list of your markers keeps the same order every session.
- Pinned markers at the edge of the map fade the same way on all four sides.
- The bookmark lists keep their scroll position when markers are added or removed while the atlas is open.
- Grave markers show their text on systems set to Turkish, instead of raw translation keys.
- Markers of landmarks without their own texture (e.g. waypoints from other mods) use the texture of their item;
  resource packs can list more items and item tags per marker texture. (antique-atlas#350)

### Resource packs
- Resource packs choose which tile draws water, swamp water, ice and lava, and can add their own features by block,
  block tag, biome or biome tag, in `atlas/features/*.json`. The built-in rules draw the map as before.
  (antique-atlas#318)
- A malformed `.mcmeta` on a tile, marker or dimension icon texture is logged and ignored. It used to fail the resource
  reload (turning every resource pack off) or crash the game when the atlas opened.
- A tile texture without `.mcmeta` named in another texture's `tilesToThis` no longer fails the resource reload, and a
  loop of `parent` textures no longer freezes the game.
- A biome or structure tile provider with an empty texture list is logged and skipped, instead of crashing the game every
  tick while that area is on the map.
- Structure-type tiles work when the structure's id differs from its type's (e.g. `woodland_mansion` for the mansion)
  instead of crashing on discovery; `piece/jigsaw/feature` tiles are used; tile priorities apply to every tile of a
  structure, not only its start chunk.
- Resource packs set how the map reads each dimension in `atlas/dimension/*.json`: surface or nether-style scan, sea
  level, scan height, ravines and the tile for the void. Modded dimensions (sky islands, deep caves) can now be drawn
  with their own settings; the built-in files draw the vanilla dimensions as before. (antique-atlas#77,
  antique-atlas#249)

### Stability
- Singleplayer is safe from races between the game and its built-in server, which could crash the game while the map
  was being drawn.
- Memory is no longer kept from previous singleplayer worlds.

### Performance
- Holding the atlas no longer checks for shader mods hundreds of times per frame.
- Drawing the map, its markers and players creates next to no garbage per frame; markers are drawn in batches.
- Working out a chunk's tile is cheaper, and the map is only rebuilt when something on it changed.
- Drawing the map no longer creates an object for every empty tile, and looks each tile up once.
- Markers arriving while the atlas is open rebuild its bookmark lists at most once per tick.

### Tests
- Regression tests for tile iterator coverage and shapes, tiling rules, feature rules, elevation bands, allocation
  checks for per-frame code, and source guards for thread and caching rules.
