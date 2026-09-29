# Antique Atlas — NeoForge

> [!IMPORTANT]
> **This is an unofficial fork.** It is a NeoForge port of [Antique Atlas 4](https://github.com/sisby-folk/antique-atlas) by **Sisby folk** ([Modrinth](https://modrinth.com/mod/antique-atlas-4)), which is itself a rewrite of Hunternif's original Antique Atlas. This fork has its own maintainer and has no affiliation with the original authors. All credit for the mod and its art goes to them. Report problems with this version [here](https://github.com/Darkona/antique-atlas-neoforge/issues), not to the original project.

**A hand-drawn world map that fills itself in as you explore.**

Antique Atlas draws the world you explore as an old hand-inked map. Forests become small trees, mountains become peaks and water gets ripples. Villages and other structures appear when you find them. You can mark places with waypoints, see your friends and hold the atlas in your hands.

![The atlas](wiki/images/atlas-map.png)

Minecraft **1.21.1**, **NeoForge 21.1**. Antique Atlas is a client-side mod. It needs [Surveyor Map Framework](https://github.com/Darkona/surveyor-neoforge), which records what you explore.

## Using the atlas

- Press **M** to open the atlas. **Drag** to move the map and **scroll** to zoom.
- The map shows only the areas you explored. Structures appear when you find them.
- The bookmarks on the side of the book add and remove markers. Each marker has an icon, a colour and a label.

![Adding a marker](wiki/images/atlas-marker-editor.png)

- To carry the map in your hands, rename a **book** to `Antique Atlas` at an anvil. Right-click it to open the atlas.

![Holding the atlas](wiki/images/atlas-in-hand.png)

## Installation

Put Antique Atlas and Surveyor in `mods/` (NeoForge 21.1, Minecraft 1.21.1). For a shared map on a server, the server needs Surveyor too. It does not need Antique Atlas.

## Documentation

The [wiki](https://github.com/Darkona/antique-atlas-neoforge/wiki) has the rest: [using the atlas](https://github.com/Darkona/antique-atlas-neoforge/wiki/Using-the-Atlas) · [markers](https://github.com/Darkona/antique-atlas-neoforge/wiki/Markers) · [playing together](https://github.com/Darkona/antique-atlas-neoforge/wiki/Playing-Together) · [configuration](https://github.com/Darkona/antique-atlas-neoforge/wiki/Configuration) · [resource packs](https://github.com/Darkona/antique-atlas-neoforge/wiki/Resource-Packs) · [troubleshooting](https://github.com/Darkona/antique-atlas-neoforge/wiki/Troubleshooting)

## Differences from the original

This fork keeps the features, art, resource-pack format and mod id of Antique Atlas. It runs on [Surveyor for NeoForge](https://github.com/Darkona/surveyor-neoforge). These are the changes.

**Platform**
- Runs natively on NeoForge 21.1, without Sinytra Connector or Fabric API.
- The configuration file uses the NeoForge format. The options that were tables (`structureMarkers`, dimension `scales`) are now lists of `"name=value"` entries.
- The Config button in the mod list opens a config screen with every option.

**Fixes**

These are bugs of the original, fixed here. When there is an original issue for a fix, the commit of the fix gives its number.

- Singleplayer is safe from races between the game and its built-in server. These races could crash the game while it drew the map.
- The map now updates when terrain changes. Before, an area that was already drawn stayed the same until you joined the world again. This was most visible at the edge of explored land.
- The list of your markers keeps the same order in every session.
- Pinned markers at the edge of the map fade the same way on all four sides.
- A dimension explored along only one row of chunks now shows in the dimension list.
- Memory from previous singleplayer worlds is released.
- The first tile of the drawn area, and the last row and column when zoomed out, were only half drawn. They are inside the margin drawn around the visible map, so this did not show in normal play.
- A change of share group redraws the map from the exploration of the new group. A resource reload (F3+T) redraws it with the new textures and tile rules.
- The soft edge of the book frame over the map is translucent again. Hovering your own player icon no longer moves what the atlas draws after it.
- Holes to the void and the edges of End islands use the empty tile or the End void tile. A biome outside the palette of its region no longer crashes the game.
- A book with the translated atlas name works as an atlas, in any language.
- These no longer fail the resource reload or crash the game: a malformed `.mcmeta` on a texture, a loop of `parent` textures, a tile provider with an empty texture list, and a structure whose id is different from the id of its type.
- The bookmark lists keep their scroll position when markers change. Grave markers show their text on systems set to Turkish.

**New features**
- **Handheld zoom**: sneak and use the atlas book to zoom the handheld map out to 1, 2 or 4 chunks per tile.
- **Marker coordinates**: middle-click a marker to copy its coordinates. The graves of other players show the name of their owner.
- **Structure icons for your markers**: you can pick structure icons for your own markers. By default this works only on servers without Surveyor. Option `pickStructureMarkers`.
- **Item textures**: a marker without its own texture, for example a waypoint from another mod, uses the texture of its item. Resource packs can list more items and item tags for each marker texture.
- **Feature rules**: resource packs choose the tile for water, swamp water, ice and lava. They can also add their own features by block, block tag, biome or biome tag, in `atlas/features/*.json`.
- **Dimension settings**: resource packs set how the map reads the terrain of each dimension, in `atlas/dimension/*.json`. The settings are surface or nether-style scan, sea level, scan height, ravines and the tile for the void. Modded dimensions can use their own settings.
- **Map export**: the atlas saves the whole explored area of a dimension as a PNG image (`screenshots/atlas/`). The export does not use AWT, so it also works on macOS.
- **Minimap**: an optional minimap in a corner of the screen, with tiles, markers and other players. Option `minimap`, off by default.
- **Edge fade**: tiles next to unexplored areas can fade out instead of ending in a hard edge. Option `fadeEdges`, off by default.
- **Debug mode**: `debug`, or `-Dantique_atlas.debug=true`, logs what the atlas does, for bug reports and test runs.

**Performance**
- Holding the atlas no longer runs an expensive check for shader mods hundreds of times per frame.
- Drawing the map, its markers and the players makes almost no garbage per frame. Markers are drawn in batches.
- The tile for a chunk costs less to calculate. The map is rebuilt only when something on it changed.

## Credits and license

**Hunternif** made Antique Atlas, and Kenkron, asiekierka and tyra314 continued it. This version is based on **Antique Atlas 4** by **Sisby folk** ([sisby-folk/antique-atlas](https://github.com/sisby-folk/antique-atlas)). The art is by **Hunternif** and **lumiscosity**. The full art credits are in [CREDITS](CREDITS). This repository is an unofficial NeoForge fork of Antique Atlas 4.

- Code: **GNU LGPL v3.0 or later**.
- Textures: **CC BY-NC-SA 4.0**. You can share and adapt them with attribution, under the same license, **not for commercial use**.

See [LICENSE](LICENSE).
