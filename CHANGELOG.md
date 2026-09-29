# Changelog

This file lists the changes of this NeoForge fork. It follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and each entry starts with the date of its change.

Links such as `antique-atlas#358` go to the [original project's issues](https://github.com/sisby-folk/antique-atlas/issues). The bugs they describe also existed in this fork until the date of the entry. "Closes" means that this fork fully handles that bug or feature request. "Partly addresses" means that it handles only the part that the entry describes. "Answers" means that the issue is a question, and the wiki answers it.

## [3.1.2+1.21-neoforge] - 2026-09-29

### Added
- 2026-09-29: New option `debug` (or the JVM argument `-Dantique_atlas.debug=true`): the atlas logs what it does to `latest.log` with the prefix `[Antique Atlas/debug]`, for bug reports and test runs. Rare events get one line each (rebuilds and their cause, resource-pack rules, item marker textures, exports, minimap, handheld zoom). Busy activity (tiles, markers) is added up every 100 ticks. When the option is off, it costs nothing.
- 2026-09-29: Tests: debug mode.
- 2026-09-27: Option `fadeEdges` (off by default): tiles next to unexplored areas fade out towards them, instead of ending in a hard square edge. Closes [antique-atlas#87](https://github.com/sisby-folk/antique-atlas/issues/87).
- 2026-09-27: Tests: terrain changes re-tile chunks that are already drawn, the handheld zoom cycle, and a config-screen label for every option.
- 2026-09-27: Tests: structure icon picking and the fade of pinned markers.
- 2026-09-26: An optional minimap in a corner of the screen, with the tiles, markers and other players of the atlas. It is off by default (`minimap`). You can configure the corner, the size, the chunks per tile and the markers. It hides while a screen, F1 or F3 is open, and it obeys `requireItem`. Closes [antique-atlas#254](https://github.com/sisby-folk/antique-atlas/issues/254).
- 2026-09-25: A new bookmark saves the whole explored area of the dimension on screen as a PNG in `screenshots/atlas/`. The image has 16 pixels per tile, and zooms out as necessary to fit in 4096 pixels. The export does not use AWT, so it also works on macOS. Closes [antique-atlas#216](https://github.com/sisby-folk/antique-atlas/issues/216).
- 2026-09-24: Resource packs set how the map reads each dimension, in `atlas/dimension/*.json`: surface or nether-style scan, sea level, scan height, ravines and the tile for the void. Modded dimensions (sky islands, deep caves) can now use their own settings. The built-in files draw the vanilla dimensions as before. Closes [antique-atlas#77](https://github.com/sisby-folk/antique-atlas/issues/77) and [antique-atlas#249](https://github.com/sisby-folk/antique-atlas/issues/249).
- 2026-09-22: Resource packs choose the tile for water, swamp water, ice and lava. They can also add their own features by block, block tag, biome or biome tag, in `atlas/features/*.json`. The built-in rules draw the map as before. Closes [antique-atlas#318](https://github.com/sisby-folk/antique-atlas/issues/318).
- 2026-09-22: Tests: regression tests for the feature rules.
- 2026-09-21: Sneak and use the atlas book to zoom the handheld map out to 1, 2 or 4 chunks per tile. Closes [antique-atlas#353](https://github.com/sisby-folk/antique-atlas/issues/353).
- 2026-09-21: Middle-click a marker to copy its coordinates. Closes [antique-atlas#169](https://github.com/sisby-folk/antique-atlas/issues/169).
- 2026-09-21: The graves of other players show the name of their owner. Closes [antique-atlas#342](https://github.com/sisby-folk/antique-atlas/issues/342).
- 2026-09-21: The wiki explains which players you see on the map. The server decides it with the `networking.positions` option of Surveyor. Answers [antique-atlas#393](https://github.com/sisby-folk/antique-atlas/issues/393).
- 2026-09-21: You can pick structure icons for your own markers. By default this works only on servers without Surveyor, where the game does not discover structures for you. Option `pickStructureMarkers`. Closes [antique-atlas#171](https://github.com/sisby-folk/antique-atlas/issues/171).
- 2026-09-21: A marker for a landmark without its own texture (for example, a waypoint from another mod) uses the texture of its item. Resource packs can list more items and item tags for each marker texture. Partly addresses [antique-atlas#350](https://github.com/sisby-folk/antique-atlas/issues/350): the texture mapping, but no dedicated art for any item.
- 2026-09-19: Tests: regression tests for the coverage and shapes of the tile iterator, the tiling rules and the elevation bands.
- 2026-09-16: Antique Atlas 4 (3.1.2) runs natively on NeoForge 21.1 (Minecraft 1.21.1), without Sinytra Connector or Fabric API. It has the same features, art, resource-pack format and mod id as the original. It needs [Surveyor for NeoForge](https://github.com/Darkona/surveyor-neoforge).
- 2026-09-16: Tests: allocation checks for the code that runs every frame, and source guards for the thread and caching rules.

### Changed
- 2026-09-23: Performance: drawing the map no longer makes an object for every empty tile, and it looks up each tile once.
- 2026-09-23: Performance: markers that arrive while the atlas is open rebuild its bookmark lists at most once per tick.
- 2026-09-16: The configuration file uses the NeoForge format. `structureMarkers` and dimension `scales` are lists of `"name=value"` entries.
- 2026-09-16: Performance: holding the atlas no longer checks for shader mods hundreds of times per frame.
- 2026-09-16: Performance: drawing the map, its markers and the players makes almost no garbage per frame. Markers are drawn in batches.
- 2026-09-16: Performance: the tile for a chunk costs less to calculate, and the map is rebuilt only when something on it changed.

### Fixed
- 2026-09-23: Structure-type tiles work when the id of the structure is different from the id of its type (for example, `woodland_mansion` for the mansion). Before, discovering the structure crashed the game. The atlas now uses `piece/jigsaw/feature` tiles, and tile priorities apply to every tile of a structure, not only to its start chunk.
- 2026-09-23: A biome or structure tile provider with an empty texture list is logged and skipped. Before, it crashed the game every tick while that area was on the map.
- 2026-09-23: Holes to the void and the edges of End islands use the empty tile or the End void tile. Before, they showed a random biome of the region.
- 2026-09-23: A biome outside the palette of its region no longer crashes the game every tick. The atlas leaves that chunk undrawn.
- 2026-09-23: Grave markers show their text on systems set to Turkish. Before, they showed raw translation keys.
- 2026-09-23: The bookmark lists keep their scroll position when markers are added or removed while the atlas is open.
- 2026-09-22: A tile texture without `.mcmeta` in the `tilesToThis` of another texture no longer fails the resource reload. A loop of `parent` textures no longer freezes the game.
- 2026-09-22: A malformed tiling section in the `.mcmeta` of a tile texture is logged, and the texture uses the defaults.
- 2026-09-22: A malformed `.mcmeta` on a marker or dimension icon texture is logged and ignored. Before, it failed the resource reload (which turns every resource pack off) or crashed the game when the atlas opened.
- 2026-09-21: A change of share group redraws the map from the exploration of the new group. Before, the areas of the old group stayed on the map. Closes [antique-atlas#343](https://github.com/sisby-folk/antique-atlas/issues/343), with the new `ExplorationReset` event of Surveyor for NeoForge.
- 2026-09-20: A resource reload (F3+T) redraws the atlas with the new textures and tile rules. Before, the atlas kept the old ones until you joined the world again. Closes [antique-atlas#243](https://github.com/sisby-folk/antique-atlas/issues/243).
- 2026-09-19: The first tile of the drawn area, and the last row and column when zoomed out, were only half drawn. They are inside the margin around the visible map.
- 2026-09-19: The Config button in the mod list opens a config screen with every option. Before, the button was disabled. Closes [antique-atlas#390](https://github.com/sisby-folk/antique-atlas/issues/390) and [antique-atlas#372](https://github.com/sisby-folk/antique-atlas/issues/372).
- 2026-09-19: The soft edge of the book frame over the map is translucent again. Closes [antique-atlas#358](https://github.com/sisby-folk/antique-atlas/issues/358).
- 2026-09-19: Hovering your own player icon no longer moves everything drawn after it in that frame.
- 2026-09-19: A book with the translated atlas name, and the creative atlas in any language, work as an atlas. Before, only the English name worked. Closes [antique-atlas#367](https://github.com/sisby-folk/antique-atlas/issues/367).
- 2026-09-16: Singleplayer is safe from races between the game and its built-in server. These races could crash the game while it drew the map.
- 2026-09-16: Memory from previous singleplayer worlds is released.
- 2026-09-16: The map updates when terrain changes. Before, it kept the old drawing until you joined the world again.
- 2026-09-16: A dimension explored along only one row of chunks now shows in the dimension list.
- 2026-09-16: The list of your markers keeps the same order in every session.
- 2026-09-16: Pinned markers at the edge of the map fade the same way on all four sides.
