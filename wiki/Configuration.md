# Configuration

The options are in `config/antique-atlas.toml`. You can also change them in game from the mod list (Mods → Antique Atlas → Config). Surveyor controls what the game shares with other players. See [Surveyor's configuration](https://github.com/Darkona/surveyor-neoforge/wiki/Configuration).

## The screen

| Option | Default | What it does |
|---|---|---|
| `fullscreen` | `true` | Full-screen map (more tiles at once), or the classic book size. |
| `requireItem` | `false` | The **M** key works only while you carry an atlas. |
| `keepZoom` | `false` | Keeps the zoom level when you close and open the atlas again. |
| `keepOffset` | `false` | Keeps the map position when you close and open the atlas again. |

## Zoom and size

| Option | Default | What it does |
|---|---|---|
| `maxTileChunks` | `5` | The maximum zoom out: one tile covers up to 2^N × 2^N chunks (0–6). |
| `maxTilePixels` | `1` | The maximum zoom in: tiles are drawn up to 16 × 2^N pixels (0–3). |
| `mapScale` | `0` | The size of tiles and markers on screen. `0` follows your GUI scale. `-1` / `-2` use half of it (rounded up / down). Higher values set a fixed scale. |

## Content

| Option | Default | What it does |
|---|---|---|
| `graveStyle` | `EUPHEMISMS` | The label of graves. See [Markers](Markers). |
| `structureMarkers` | — | Shows or hides each structure marker. See [Markers](Markers). |
| `emptyHandling` | `EMPTY` | Unexplored areas: blank paper (`EMPTY`) or clouds (`CLOUDS`). |
| `fadeEdges` | `false` | Tiles next to unexplored areas fade out towards them over half a tile, instead of ending in a hard edge. It has no effect with `emptyHandling = CLOUDS`. |
| `fallbackFailHandling` | `MISSING` | What to draw for a biome the atlas cannot recognise: `???` (`MISSING`), plains (`PLAINS`), a test pattern (`TEST`), or stop the game with an error (`CRASH`). See [Troubleshooting](Troubleshooting). |
| `chunkTickLimit` | `100` | Chunks drawn per tick while the map of a world loads. If joining a world stutters, make it lower. |

## Minimap

The minimap is a small map in a corner of the screen, with you at the center, and with markers and other players. It is off by default. It hides while a screen (inventory, chat, the atlas...) is open, and with F1 or F3. With `requireItem = true` it shows only while you carry an atlas.

| Option | Default | What it does |
|---|---|---|
| `minimap` | `false` | Shows the minimap. |
| `minimapCorner` | `TOP_LEFT` | `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT` or `BOTTOM_RIGHT`. |
| `minimapSize` | `96` | The side of the map in GUI pixels (48–256). At one chunk per tile, one pixel is one block. |
| `minimapTileChunks` | `1` | Chunks per tile: `1`, `2` or `4` (`3` counts as `2`). Higher values show a larger area. |
| `minimapMarkers` | `true` | Shows markers on the minimap. |

## Troubleshooting

| Option | Default | What it does |
|---|---|---|
| `debug` | `false` | Logs what the atlas does to `latest.log`, for bug reports. See [Troubleshooting](Troubleshooting#debug-mode). |

## `[dimensions]`

`scales` lists the dimensions in the order the atlas cycles through them, each with its coordinate scale. For example: `["minecraft:overworld=8", "minecraft:the_nether=1", "minecraft:the_end=0"]`. When two dimensions both have a scale, the atlas shows a player in the other dimension at the matching position in yours. So a player in the Nether shows at the matching Overworld position. `0` means no conversion.
