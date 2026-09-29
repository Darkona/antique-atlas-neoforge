# Resource packs

## Texturing

### Tile Textures

> `assets/packid/textures/atlas/tile/*.png|mcmeta`

#### Image

![test tile texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/test.png?raw=true) ![square plateau texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/base/plateau_square.png?raw=true) ![savanna house](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/structure/village/savanna/small_house.png?raw=true) ![nether bridge crossing](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/structure/fortress/nether/nether_fortress_bridge_crossing.png?raw=true)

Tiles use a format like autotiles. Each tile is split into 8x8 "subtiles". The atlas draws each subtile based on which neighbouring textures "tile" to it:

![autotile guide](https://github.com/sisby-folk/antique-atlas/assets/55819817/92ebcd3e-b189-429a-9ad0-667bc26a0ed6)

The top left subtiles are for tight outer corners, the top right subtiles are for inner corners, and the bottom subtiles are for larger connected areas. Because of this, the 4 center subtiles must tile to themselves, or the texture shows seams. The usual way to do this is to leave them almost blank, as above.

#### Metafile

```json5
// base/sand.json
{
	"antique_atlas:tiling": {
		"parent": "antique_atlas:base/flat",
		"tags": [
			"antique_atlas:sand"
		],
		"tilesTo": [
			"#antique_atlas:sand",
			"#antique_atlas:sand_hills_low"
		]
	}
}
```

The metafile only controls which other textures this texture "tiles" to. When a neighbour tiles to it, the atlas draws the "connecting" subtiles instead of the border subtiles.

The `parent` field inherits all fields from another texture. Most biomes inherit from a "base" texture that gives the terrain shape.

The `tags` field adds this texture to a group, which you can use when you specify tiling.

The `tilesTo`, `tilesToVertical` and `tilesToHorizontal` arrays make this texture connect to the textures they name.

The `tilesToThis`, `tilesToThisVertical` and `tilesToThisHorizontal` arrays do the opposite: they make the *named* textures connect to this one.

These options take time to understand, and many of them overlap. Look at the [builtin pack](https://github.com/sisby-folk/antique-atlas/tree/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile) for examples, and always test in game.

### Marker Textures

> `assets/packid/textures/atlas/marker/*.png|mcmeta`

#### Image

![unknown marker texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/unknown.png?raw=true) ![tower marker texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/tower.png?raw=true) ![tower accent texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/tower_accent.png?raw=true) ![x marker texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/red_x_large.png?raw=true) ![x accent](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/red_x_large_accent.png?raw=true)

A marker without a metafile is 32x32 and is drawn centered on the cursor. A marker in the `custom/` subfolder is added to the "add marker" window in game.

An optional `*_accent.png` is drawn directly over the main texture, tinted to the dye colour of the landmark.

For markers that are not custom, the atlas uses the texture ID that is closest to the landmark ID. For example, the landmark `surveyor:player_death/void/29/71/-100` tries `surveyor/textures/atlas/marker/player_death/void.png`, then `surveyor/textures/atlas/marker/player_death.png`, then `surveyor/textures/atlas/marker/default.png`.

#### Metafile

```json5
// end_city.json
{
	"antique_atlas:marker": {
		"textureWidth": 64,
		"textureHeight": 64,
		"mipLevels": 2,
		"offsetY": -48
	}
}
```

The metafile adjusts how the atlas draws the marker.

The `items` field lists item ids or item tags (`#namespace:tag`). A landmark without its own texture (for example, a waypoint from another mod) that carries one of these items is drawn with this texture:

```json5
// custom/tower.png.mcmeta
{
	"antique_atlas:marker": {
		"item": "brick",
		"items": ["minecraft:stone_bricks", "#minecraft:stone_bricks"]
	}
}
```

The `item` field is the item that the "add marker" picker shows for the texture. Like `items`, it also selects the texture.

With these fields you can make very large markers (`textureWidth` and `textureHeight`) or markers that are not centered (`offsetX` and `offsetY`), as above. The offset moves the texture relative to its top left corner. By default it is `-width/2` and `-height/2` (centered).

![end city marker](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/structure/end_city.png?raw=true)

The `mipLevels` field adds power-of-two mipmap textures to the right of the main texture. In the end city example, the first mip level is 32x32 and the second is 16x16, so there are 2 mip levels. The texture is 112x64, and the atlas calculates that size for you.

## Biome Tiles (Biome Tile Providers)

> `assets/namespace/atlas/biome/path.json` for biome `namespace:path`

```json5
// assets/minecraft/atlas/biome/frozen_ocean
{
	"textures": "antique_atlas:biome/nonland/ocean/frozen/ice_ice_spikes"
}
```

The `parent` field uses the full definition of another existing biome for this biome. This is a fast way to configure modpacks.

If there is no `parent`, the `textures` field sets the textures for the biome directly.

The field accepts a tile texture ID (without the `textures/atlas/tile/` part). The map uses that texture for the biome.

It also accepts an array of texture IDs, used at random, or an object of texture -> integer, for random choice with weights.

Finally, the field also accepts an object with elevations: `valley`, `low`, `mid`, `high` and `peak`. Each one accepts the forms above.

The atlas uses them when the terrain is at Y <73, <83, <98, <113 and above, in that order. These values move with the sea level.

```json5
// assets/minecraft/atlas/biome/badlands
{
	"textures": {
		"valley": [
			"antique_atlas:biome/arid/desert/sand_1",
			"antique_atlas:biome/arid/desert/sand_cacti_sparse",
			"antique_atlas:biome/arid/desert/sand_shrubs"
		],
		"low": {
			"antique_atlas:biome/arid/badlands/terracotta_shrubs_tiered": 4,
			"antique_atlas:biome/arid/badlands/terracotta_tiered": 2,
			"antique_atlas:biome/arid/badlands/terracotta_tiered_rough": 2,
			"antique_atlas:biome/arid/badlands/terracotta_tiered_separate": 1
		},
		"peak": [
			"antique_atlas:biome/arid/badlands/shrubs_plateau_low",
			"antique_atlas:biome/arid/badlands/terracotta_plateau_low"
		]
	}
}
```

## Features (Feature Rules)

> `assets/namespace/atlas/features/name.json`, one rule per file

Feature rules choose the tile for special terrain inside a chunk: water, swamp water, ice and lava. Each block column that matches a rule adds its `priority` to the tile of that rule. The tile with the highest total, among biomes and features, draws the chunk.

```json5
// assets/antique_atlas/atlas/features/swamp_water.json
{
	"tile": "antique_atlas:feature/swamp_water",
	"priority": 4,
	"water": true,
	"biomes": ["#c:swamp"]
}
```

| Field      | Meaning                                                                                              |
|------------|------------------------------------------------------------------------------------------------------|
| `tile`     | The biome tile provider to draw (`atlas/biome/...`, e.g. `antique_atlas:feature/ice`). Without it, the rule is off. |
| `priority` | Weight added per matching column. Default `1`. A plain biome column adds 1 (beaches 3, the nether 2). |
| `water`    | `true`: the rule matches columns under water. `false` (default): columns with no water on top.       |
| `blocks`   | Block ids or `#tags` for the top block (under water: the floor). Empty or absent: any block.         |
| `biomes`   | Biome ids or `#tags`. Empty or absent: any biome.                                                    |

A rule that is not `water` needs `blocks` or `biomes`. If more than one rule matches a column, the rule with the highest `priority` wins. On a tie, a rule with `biomes` wins, then a rule with `blocks`, then the file id. The atlas uses at most 63 rules.

The built-in rules are `water` (4), `swamp_water` (4, `#c:swamp`), `ice` (3, `minecraft:ice`) and `lava` (6, `minecraft:lava`). In the nether they apply to the lava sea below Y 50. There, a column that matches no rule is lava shore. Ravines and empty chunks are not rules.

To change a built-in rule, put a file with the same path in your pack. To turn it off, use `{}`. To add a rule, for example for tracks (a future use, antique-atlas#344):

```json5
// assets/mypack/atlas/features/rails.json
{
	"tile": "mypack:feature/rails",
	"priority": 8,
	"blocks": ["#minecraft:rails"]
}
```

The `tile` must be a biome tile provider (`assets/mypack/atlas/biome/feature/rails.json`) with its tile textures.

## Structures

> `assets/namespace/atlas/structure/start/path.json` for structure `namespace:path`

> `assets/namespace/atlas/structure/tag/path.json` for structures of tag `#namespace:path`

> `assets/namespace/atlas/structure/type/path.json` for structures of type `namespace:path`

> `assets/namespace/atlas/structure/piece/type/path.json` for structure pieces of type `namespace:path`

> `assets/namespace/atlas/structure/piece/jigsaw/single/path.json` for single jigsaw piece `namespace:path`

> `assets/namespace/atlas/structure/piece/jigsaw/feature/path.json` for feature jigsaw piece `namespace:path`

### Tiles (Structure Tile Providers)

```json5
// assets/minecraft/atlas/structure/piece/type/necsr.json
{
	"displayId": "minecraft:nether_fortress_corridor_nether_warts_room",
	// for debug mode
	"priority": 50,
	"textures": "antique_atlas:structure/fortress/nether/nether_fortress_corridor_nether_warts_room"
}
```

The `priority` field of a structure decides whether it appears on top of other structures in the same chunk. A lower value is more important.

Structures use the `textures` field for tile textures in the same way as biomes.

Structures have no elevations. They have "Chunk Matchers", each with an ID, which decide whether the texture appears, and on which chunks.

| ID                       | Behavior                                                                                     |
|--------------------------|----------------------------------------------------------------------------------------------|
| center                   | always shown, at the center of the object's bounding box. Used when no matcher is specified. |
| center_above_ground      | only shown if the center of the box is above sea level.                                      |
| center_top_above_ground  | only shown if the top of the box is above sea level.                                         |
| center_horizontal        | only shown if the box is longer horizontally                                                 |
| center_vertical          | only shown if the box is longer vertically                                                   |
| bridge_horizontal        | only shown if multiple chunks are spanned horizontally, over all those chunks.               |
| bridge_vertical          | only shown if multiple chunks are spanned vertically, over all those chunks.                 |
| path_straight_horizontal | only shown if a jigsaw has two junctions and they are aligned horizontally                   |
| path_straight_vertical   | only shown if a jigsaw has two junctions and they are aligned vertically                     |

```json5
// assets/minecraft/atlas/structure/piece/type/iglu.json
{
	"displayId": "minecraft:igloo",
	"textures": {
		"center_top_above_ground": "antique_atlas:structure/igloo/igloo"
	}
}
```

### Markers (Structure Marker Providers)

```json5
// assets/minecraft/atlas/structure/start/village_savanna.json
{
	"markers": "antique_atlas:structure/plains_village"
}
```

```json5
// assets/antique_atlas/lang/en_us.json
{
	"structure.start.minecraft.village_savanna": "Savanna Village"
}
```

The value is a direct reference to the marker texture (without the `textures/atlas/marker` part).

The tooltip uses the translation key, which matches the path of the json file.

## Dimensions

Dimensions get custom bookmark icons from `namespace/textures/atlas/dimension/path.png`, with an optional metafile:
```json5
// minecraft/textures/atlas/dimension/the_end.png.mcmeta
{
	"antique_atlas:dimension": {
		"name": "advancements.end.root.title",
		"color": "#DECF2A"
	}
}
```

Without them, a dimension uses a question mark texture and a random color hashed from its ID.

### Terrain Settings

> `assets/namespace/atlas/dimension/path.json` for dimension `namespace:path`

These settings choose how the map reads the terrain of a dimension. A dimension without a file uses the defaults below: the surface scan at sea level 63, no ravines, and the empty tile for the void. Antique Atlas includes files for the Overworld, the Nether and the End.

```json5
// assets/minecraft/atlas/dimension/the_nether.json
{
	"scanner": "nether",
	"sea_level": 31,
	"scan_top": 126,
	"floor_scan_top": 50,
	"empty_tile": "antique_atlas:feature/bedrock_roof"
}
```

| Field            | Default                       | Meaning                                                                                                                                                                                                  |
|------------------|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `scanner`        | `surface`                     | `surface`: the top floor of each column, with elevations and feature rules. `nether`: floors under a ceiling; low floors are lava sea (feature rules) or lava shore, other floors show their biome.           |
| `sea_level`      | `63`                          | `surface`: the Y that elevations (`valley` ... `peak`) and ravines are measured from. `nether`: the lava sea level; floors under it show their biome.                                                    |
| `scan_top`       | none                          | The highest Y that is read. None: the whole column. A `nether` scanner needs it (its ceiling), or it only sees the roof.                                                                                  |
| `floor_scan_top` | `50`                          | `nether` only: the highest Y of the low floors (lava sea and shore).                                                                                                                                     |
| `ravines`        | `false`                       | `true`: columns more than 7 blocks under `sea_level` are drawn as ravines (the Overworld).                                                                                                               |
| `empty_tile`     | `antique_atlas:feature/empty` | The biome tile provider for columns with no floor (holes to the void). The End uses `antique_atlas:feature/end_void`. Any provider works, for example a sky tile for a dimension of floating islands. |

To change a built-in dimension, put a file with the same path in your pack. For a modded sky dimension:

```json5
// assets/aether/atlas/dimension/the_aether.json
{
	"sea_level": 100,
	"empty_tile": "mypack:feature/sky"
}
```

The changes apply after a resource reload (F3+T), which redraws the map.
