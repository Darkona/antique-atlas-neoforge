# Troubleshooting

**Parts of the map show `???`.** The atlas has no art for that biome and could not guess one from its tags. This is usual with modded biomes. Set `fallbackFailHandling = "PLAINS"` in the [configuration](Configuration), or use a resource pack for that mod (see [Resource Packs](Resource-Packs)).

**A structure I saw is not on the map.** Structures appear after you stand inside them or look at them. Some markers show only from a certain zoom level. Also make sure that the structure is not set to `false` under `structureMarkers`.

**There are no markers at all.** The **Hide markers** bookmark can be on. Click it again.

**The exploration of my friends does not show.** The server needs Surveyor, with sharing turned on. See [Playing Together](Playing-Together).

**Joining a world with a large map stutters for a moment.** The atlas draws the saved map a little at a time. Make `chunkTickLimit` lower to spread the work over more ticks.

**Something else.** Report it at [Darkona/antique-atlas-neoforge](https://github.com/Darkona/antique-atlas-neoforge/issues) and attach your `logs/latest.log`. Problems with what is explored or shared go to [Surveyor](https://github.com/Darkona/surveyor-neoforge/issues).

## Debug mode

Debug mode writes what the atlas does to `logs/latest.log`. Each line starts with `[Antique Atlas/debug]`.

To turn it on, do one of these:

- Set `debug = true` in `config/antique-atlas.toml`, or turn on **Debug mode** in the config screen.
- Add the JVM argument `-Dantique_atlas.debug=true` to the game. With this argument, debug mode stays on whatever the config says.

Events that are not frequent get one line each:

- rebuilds of the map and their cause (resources reloaded with F3+T, share group changed), with the chunks to draw, structure tiles, markers and terrain scanner of each dimension;
- the feature rules and dimension settings read from resource packs;
- markers drawn with the texture of their item;
- map exports: file, size in pixels, chunks per tile and time;
- the minimap shown or hidden, and the zoom of the handheld atlas.

Busy activity is added up and written as one line every 100 ticks, only when something happened. For example:

```
[Antique Atlas/debug] Last 100 ticks: terrain.queued=812, terrain.queueMax=812, tiles.set=790, tiles.undrawn=22, markers.added=14
```

| Counter | Meaning |
|---|---|
| `terrain.queued`, `terrain.queueMax` | Chunks queued to be drawn, and the longest queue. |
| `tiles.set`, `tiles.removed`, `tiles.undrawn` | Chunks drawn, chunks removed because they have no terrain any more, and chunks left undrawn (for example, a biome that the region does not know). |
| `structures.added` | Structures received from Surveyor. |
| `markers.added`, `markers.removed` | Markers received and removed. |
| `markers.itemTexture`, `markers.defaultTexture` | Markers drawn with the texture of their item, and markers with no texture of their own. |
| `bookmarks.rebuilt` | Rebuilds of the bookmark lists while the atlas is open. |

When debug mode is off, its cost is too small to measure. When it is on, it adds lines to the log. Turn it off again when you are done.

**For a bug report**, turn on debug mode and do the steps that cause the problem again. Then attach `logs/latest.log` and `config/antique-atlas.toml`. If the problem is about what is explored or shared, also turn on the Surveyor debug mode (see [Surveyor's troubleshooting](https://github.com/Darkona/surveyor-neoforge/wiki/Data-and-Troubleshooting#debug-mode)) and attach the log of the server too.
