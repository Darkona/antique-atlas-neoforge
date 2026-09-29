# Markers

## Your markers

Add them with the **Add marker** bookmark (see [Using the Atlas](Using-the-Atlas)). Each marker has an icon, a colour and a label, and it shows in the list on the left of the book. Remove markers with **Delete marker**.

Markers are Surveyor waypoints. On a server your group shares them, and they also show in other map mods that use Surveyor. Server-wide landmarks from operators appear too, but you cannot edit them from the atlas.

## Markers that appear on their own

| Marker | When |
|---|---|
| **Structures** | Villages, monuments, mansions and other structures, after you discover them. Resource packs decide which structures get a marker. |
| **Nether portals** | Every portal you light. |
| **Graves** | Where you or a friend died, with the day number. The graves of other players show their name. |
| **Lodestones** | Every lodestone placed. |

Portals, graves and lodestones are Surveyor features. You can turn them off in [Surveyor's configuration](https://github.com/Darkona/surveyor-neoforge/wiki/Configuration).

### Hiding structure markers

`structureMarkers` in `config/antique-atlas.toml` lists every structure marker the atlas knows, as `"structure=true"`. Change a value to `false` to stop the marker for that structure. End cities are off by default. Some markers appear only from a certain zoom level, so the map stays easy to read.

### Grave labels

`graveStyle` sets the label of graves:

| Style | Example |
|---|---|
| `EUPHEMISMS` (default) | *Wiped Out on Day 12*, *Bricked on Day 3*… |
| `CAUSE` | *Steve fell from a high place on Day 12* (the death message) |
| `DIED` | *Died on Day 12* |
| `GRAVE` | *Grave (Day 12)* |
| `ITEMS` | *Lost Gear (Day 12)*, drawn as a pile of items |
