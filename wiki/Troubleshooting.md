# Troubleshooting

**Parts of the map show `???`.** The atlas has no art for that biome and couldn't guess one from its tags — usual with modded biomes. Either set `fallbackFailHandling = "PLAINS"` in the [configuration](Configuration), or use a resource pack that covers that mod (see [Resource Packs](Resource-Packs)).

**A structure I've seen isn't on the map.** Structures appear once you've stood inside or looked at them. Some markers only show from a certain zoom level. Check that it isn't set to `false` under `structureMarkers`.

**No markers at all.** The **Hide markers** bookmark may be on — click it again.

**My friends' exploration doesn't show up.** The server needs Surveyor, and sharing must be enabled there — see [Playing Together](Playing-Together).

**Joining a world with a large map stutters for a moment.** The atlas draws the saved map a little at a time; lower `chunkTickLimit` to spread it out more.

**Something else.** Report it at [Darkona/antique-atlas-neoforge](https://github.com/Darkona/antique-atlas-neoforge/issues) with your `logs/latest.log`. Problems with what is explored or shared belong to [Surveyor](https://github.com/Darkona/surveyor-neoforge/issues).
