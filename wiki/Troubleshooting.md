# Troubleshooting

**Parts of the map show `???`.** The atlas has no art for that biome and couldn't guess one from its tags — usual with modded biomes. Either set `fallbackFailHandling = "PLAINS"` in the [configuration](Configuration), or use a resource pack that covers that mod (see [Resource Packs](Resource-Packs)).

**A structure I've seen isn't on the map.** Structures appear once you've stood inside or looked at them. Some markers only show from a certain zoom level. Check that it isn't set to `false` under `structureMarkers`.

**No markers at all.** The **Hide markers** bookmark may be on — click it again.

**My friends' exploration doesn't show up.** The server needs Surveyor, and sharing must be enabled there — see [Playing Together](Playing-Together).

**Joining a world with a large map stutters for a moment.** The atlas draws the saved map a little at a time; lower `chunkTickLimit` to spread it out more.

**Something else.** Report it at [Darkona/antique-atlas-neoforge](https://github.com/Darkona/antique-atlas-neoforge/issues) with your `logs/latest.log`. Problems with what is explored or shared belong to [Surveyor](https://github.com/Darkona/surveyor-neoforge/issues).

## Debug mode

Debug mode writes what the atlas does to `logs/latest.log`. Each line starts with `[Antique Atlas/debug]`.

To turn it on, do one of these:

- Set `debug = true` in `config/antique-atlas.toml`, or turn on **Debug mode** in the config screen.
- Add the JVM argument `-Dantique_atlas.debug=true` to the game. This keeps debug mode on whatever the config says.

Events that are not frequent get one line each:

- rebuilds of the map and their cause (resources reloaded with F3+T, share group changed), with each dimension's chunks to draw, structure tiles, markers and terrain scanner;
- the feature rules and dimension settings read from resource packs;
- markers drawn with the texture of their item;
- map exports: file, size in pixels, chunks per tile and time;
- the minimap shown or hidden, and the handheld atlas zoom.

Busy activity is summed and written as one line every 100 ticks, only when something happened. For example:

```
[Antique Atlas/debug] Last 100 ticks: terrain.queued=812, terrain.queueMax=812, tiles.set=790, tiles.undrawn=22, markers.added=14
```

| Counter | Meaning |
|---|---|
| `terrain.queued`, `terrain.queueMax` | Chunks queued to be drawn, and the longest queue. |
| `tiles.set`, `tiles.removed`, `tiles.undrawn` | Chunks drawn, chunks removed because they have no terrain any more, and chunks left undrawn (for example a biome the region doesn't know). |
| `structures.added` | Structures received from Surveyor. |
| `markers.added`, `markers.removed` | Markers received and removed. |
| `markers.itemTexture`, `markers.defaultTexture` | Markers drawn with their item's texture, and markers with no texture of their own. |
| `bookmarks.rebuilt` | Rebuilds of the bookmark lists while the atlas is open. |

Debug mode off costs nothing that you can measure. On, it adds some lines to the log; turn it off again when you are done.

**For a bug report**, turn on debug mode, do the steps that cause the problem again, and attach `logs/latest.log` and `config/antique-atlas.toml`. If the problem is about what is explored or shared, also turn on Surveyor's debug mode (see [Surveyor's troubleshooting](https://github.com/Darkona/surveyor-neoforge/wiki/Data-and-Troubleshooting#debug-mode)), and attach the server's log too.
