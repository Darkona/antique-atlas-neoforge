# Configuration

Options live in `config/antique-atlas.toml`; you can also edit them in game from the mod list (Mods → Antique Atlas → Config). What is shared with other players is configured in Surveyor instead — see [Surveyor's configuration](https://github.com/Darkona/surveyor-neoforge/wiki/Configuration).

## The screen

| Option | Default | What it does |
|---|---|---|
| `fullscreen` | `true` | Full-screen map (more tiles at once) or the classic book size. |
| `requireItem` | `false` | The **M** key only works while you carry an atlas. |
| `keepZoom` | `false` | Keep the zoom level when you close and reopen the atlas. |
| `keepOffset` | `false` | Keep the map position when you close and reopen the atlas. |

## Zoom and size

| Option | Default | What it does |
|---|---|---|
| `maxTileChunks` | `5` | How far out you can zoom: one tile covers up to 2^N × 2^N chunks (0–6). |
| `maxTilePixels` | `1` | How far in you can zoom: tiles are drawn up to 16 × 2^N pixels (0–3). |
| `mapScale` | `0` | Size of tiles and markers on screen. `0` follows your GUI scale; `-1` / `-2` use half of it (rounded up / down); higher values set a fixed scale. |

## Content

| Option | Default | What it does |
|---|---|---|
| `graveStyle` | `EUPHEMISMS` | How graves are labelled — see [Markers](Markers). |
| `structureMarkers` | — | Show or hide each structure marker — see [Markers](Markers). |
| `emptyHandling` | `EMPTY` | Unexplored areas: blank paper (`EMPTY`) or clouds (`CLOUDS`). |
| `fadeEdges` | `false` | Tiles next to unexplored areas fade out towards them over half a tile, instead of ending in a hard edge. No effect with `emptyHandling = CLOUDS`. |
| `fallbackFailHandling` | `MISSING` | A biome the atlas can't recognise: draw `???` (`MISSING`), draw plains (`PLAINS`), a test pattern (`TEST`), or stop the game with an error (`CRASH`) — see [Troubleshooting](Troubleshooting). |
| `chunkTickLimit` | `100` | Chunks drawn per tick while loading a world's map. Lower it if joining a world stutters. |

## Minimap

A small map in a corner of the screen, centred on you, with markers and other players. It is off by default. It is
hidden while a screen (inventory, chat, the atlas...) is open, and with F1 or F3. With `requireItem = true` it only
shows while you carry an atlas.

| Option | Default | What it does |
|---|---|---|
| `minimap` | `false` | Show the minimap. |
| `minimapCorner` | `TOP_LEFT` | `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT` or `BOTTOM_RIGHT`. |
| `minimapSize` | `96` | Side of the map in GUI pixels (48–256). At one chunk per tile, one pixel is one block. |
| `minimapTileChunks` | `1` | Chunks per tile: `1`, `2` or `4` (`3` counts as `2`). Higher values show a larger area. |
| `minimapMarkers` | `true` | Show markers on the minimap. |

## `[dimensions]`

`scales` lists the dimensions in the order the atlas cycles through them, with their coordinate scale, e.g. `["minecraft:overworld=8", "minecraft:the_nether=1", "minecraft:the_end=0"]`. When two dimensions both have a scale, other players in the other dimension are shown where they would be in yours — so a player in the Nether shows up at the matching Overworld position. `0` means no such conversion.
