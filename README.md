# Antique Atlas — NeoForge

> [!IMPORTANT]
> **This is an unofficial fork.** It is a NeoForge port of [Antique Atlas 4](https://github.com/sisby-folk/antique-atlas) by **Sisby folk** ([Modrinth](https://modrinth.com/mod/antique-atlas-4)), itself a rewrite of Hunternif's original Antique Atlas. It is maintained separately and not affiliated with the original authors. All credit for the mod and its art goes to them. Please report problems with this version [here](https://github.com/Darkona/antique-atlas-neoforge/issues), not to the original project.

**A hand-drawn world map that fills itself in as you explore.**

Antique Atlas turns the world you explore into an old-fashioned, hand-inked map: forests become little trees, mountains become peaks, water gets its ripples, and villages and other structures appear as you find them. Mark places with waypoints, see your friends, and carry the atlas in your hands.

![The atlas](wiki/images/atlas-map.png)

Minecraft **1.21.1**, **NeoForge 21.1**. A client-side mod; it needs [Surveyor Map Framework](https://github.com/Darkona/surveyor-neoforge), which records what you explore.

## Using the atlas

- Press **M** to open it. **Drag** to move around, **scroll** to zoom.
- The map shows only what you have explored; structures appear once you find them.
- Use the bookmarks on the side of the book to add and remove markers — pick an icon, a colour and a label.

![Adding a marker](wiki/images/atlas-marker-editor.png)

- Rename a **book** to `Antique Atlas` at an anvil to carry the map in your hands; right-click it to open the atlas.

![Holding the atlas](wiki/images/atlas-in-hand.png)

## Installation

Put Antique Atlas and Surveyor in `mods/` (NeoForge 21.1, Minecraft 1.21.1). For a shared map on a server, the server needs Surveyor too — but not Antique Atlas.

## Documentation

Everything else is in the [wiki](https://github.com/Darkona/antique-atlas-neoforge/wiki): [using the atlas](https://github.com/Darkona/antique-atlas-neoforge/wiki/Using-the-Atlas) · [markers](https://github.com/Darkona/antique-atlas-neoforge/wiki/Markers) · [playing together](https://github.com/Darkona/antique-atlas-neoforge/wiki/Playing-Together) · [configuration](https://github.com/Darkona/antique-atlas-neoforge/wiki/Configuration) · [resource packs](https://github.com/Darkona/antique-atlas-neoforge/wiki/Resource-Packs) · [troubleshooting](https://github.com/Darkona/antique-atlas-neoforge/wiki/Troubleshooting)

## Differences from the original

This fork keeps Antique Atlas' features, art, resource-pack format and mod id. It runs on [Surveyor for NeoForge](https://github.com/Darkona/surveyor-neoforge). What changed:

**Platform**
- Runs natively on NeoForge 21.1 — no Sinytra Connector or Fabric API.
- The configuration file uses NeoForge's format; the options that were tables (`structureMarkers`, dimension `scales`) are lists of `"name=value"` entries.
- The mod list's Config button opens a config screen with every option.

**Fixes** — bugs of the original, fixed here. Each fix's commit references the original issue number where there is one.
- Singleplayer is safe from races between the game and its built-in server, which could crash the game while the map was being drawn.
- The map now updates when terrain changes — previously an already drawn area stayed as it was until you rejoined (most visible at the edge of explored land).
- The list of your markers keeps the same order every session.
- Pinned markers at the edge of the map fade the same way on all four sides.
- A dimension explored only along one row of chunks is no longer hidden from the dimension list.
- Memory is no longer kept from previous singleplayer worlds.
- The first tile of the drawn area, and the last row and column when zoomed out, were only half drawn (inside the margin drawn around the visible map, so it did not show in normal play).
- Joining another share group redraws the map from the new group's exploration, and reloading resources (F3+T) redraws it with the new textures and tile rules.
- The book frame's soft edge over the map is translucent again, and hovering your own player icon no longer shifts what is drawn after it.
- Holes to the void and the edges of End islands use the empty or End void tile, and a biome outside a region's palette no longer crashes the game.
- A book named with the translated atlas name works as an atlas, in any language.
- A malformed `.mcmeta` on a texture, a loop of `parent` textures, a tile provider with an empty texture list, and structures whose id differs from their type's no longer fail the resource reload or crash the game.
- The bookmark lists keep their scroll position when markers change, and grave markers show their text on systems set to Turkish.

**New features**
- Sneak and use the atlas book to zoom the handheld map out: 1, 2 or 4 chunks per tile.
- Middle-click a marker to copy its coordinates. Other players' graves show their owner's name.
- Structure icons can be picked for your own markers (by default only on servers without Surveyor). Option `pickStructureMarkers`.
- Markers without their own texture, such as waypoints from other mods, use their item's texture; resource packs can list more items and item tags per marker texture.
- Resource packs choose which tile draws water, swamp water, ice and lava, and can add their own features by block, block tag, biome or biome tag in `atlas/features/*.json`.
- Resource packs set how each dimension's terrain is read in `atlas/dimension/*.json`: surface or nether-style scan, sea level, scan height, ravines and the tile for the void. Modded dimensions can be drawn with their own settings.
- The atlas can save the whole explored area of a dimension as a PNG image (`screenshots/atlas/`), without AWT, so it also works on macOS.
- An optional minimap in a corner of the screen, with tiles, markers and other players. Option `minimap`, off by default.
- Tiles next to unexplored areas can fade out instead of ending in a hard edge. Option `fadeEdges`, off by default.
- Debug mode (`debug`, or `-Dantique_atlas.debug=true`) logs what the atlas does, for bug reports and test runs.

**Performance**
- Holding the atlas no longer triggers an expensive check for shader mods hundreds of times per frame.
- Drawing the map, its markers and players creates next to no garbage per frame, and markers are drawn in batches.
- Working out a chunk's tile is cheaper, and the map is only rebuilt when something on it changed.

## Credits and license

Antique Atlas was created by **Hunternif** and continued by Kenkron, asiekierka and tyra314; this version is based on **Antique Atlas 4** by **Sisby folk** ([sisby-folk/antique-atlas](https://github.com/sisby-folk/antique-atlas)). Art by **Hunternif** and **lumiscosity** — full art credits in [CREDITS](CREDITS). This repository is an unofficial NeoForge fork of it.

- Code: **GNU LGPL v3.0 or later**.
- Textures: **CC BY-NC-SA 4.0** — free to share and adapt with attribution, **not for commercial use**, under the same license.

See [LICENSE](LICENSE).
