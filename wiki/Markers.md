# Markers

## Your markers

Place them with the **Add marker** bookmark (see [Using the Atlas](Using-the-Atlas)): each has an icon, a colour and a label, and shows up in the list on the left of the book. Remove them with **Delete marker**.

Markers are Surveyor waypoints: on a server they are shared with your group, and they also show up in other map mods that use Surveyor. Server-wide landmarks placed by operators appear too, but can't be edited from the atlas.

## Markers that appear on their own

| Marker | When |
|---|---|
| **Structures** | Villages, monuments, mansions and other structures, once discovered. Which ones get a marker comes from resource packs. |
| **Nether portals** | Every portal you light. |
| **Graves** | Where you died, with the day number. |
| **Lodestones** | Every lodestone placed. |

Portals, graves and lodestones are Surveyor features — they can be turned off in [Surveyor's configuration](https://github.com/Darkona/surveyor-neoforge/wiki/Configuration).

### Hiding structure markers

Every structure marker the atlas knows is listed under `structureMarkers` in `config/antique-atlas.toml`, as `"structure=true"`. Change one to `false` to stop marking it (end cities are off by default). Some markers only appear from a certain zoom level, to keep the map readable.

### Grave labels

`graveStyle` sets how graves are labelled:

| Style | Example |
|---|---|
| `EUPHEMISMS` (default) | *Wiped Out on Day 12*, *Bricked on Day 3*… |
| `CAUSE` | *Steve fell from a high place on Day 12* (the death message) |
| `DIED` | *Died on Day 12* |
| `GRAVE` | *Grave (Day 12)* |
| `ITEMS` | *Lost Gear (Day 12)*, drawn as a pile of items |
