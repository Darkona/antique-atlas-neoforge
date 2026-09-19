package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.gui.tiles.SubTile;
import folk.sisby.antique_atlas.gui.tiles.SubTileQuartet;
import folk.sisby.antique_atlas.gui.tiles.TileRenderIterator;
import folk.sisby.antique_atlas.gui.tiles.SubTile.Part;
import folk.sisby.antique_atlas.gui.tiles.SubTile.Shape;
import folk.sisby.antique_atlas.util.Rect;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TileRenderIteratorTest {
	private static final TileTexture GRASS = TileTexture.empty(ResourceLocation.fromNamespaceAndPath("test", "grass"), false);
	private static final TileTexture WATER = TileTexture.empty(ResourceLocation.fromNamespaceAndPath("test", "water"), false);

	private static WorldAtlasData fill(int minX, int minZ, int maxX, int maxZ, TileTexture texture) {
		WorldAtlasData data = new WorldAtlasData();
		for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) put(data, x, z, texture);
		return data;
	}

	private static void put(WorldAtlasData data, int x, int z, TileTexture texture) {
		data.biomeTiles.put(ChunkPos.asLong(x, z), texture);
		data.tileScope.extendTo(x, z);
	}

	/** The parts drawn per tile, keyed by the subtile's owning chunk. */
	private static Long2ObjectOpenHashMap<Map<Part, SubTile>> render(WorldAtlasData data, Rect scope) {
		return render(data, scope, 1);
	}

	private static Long2ObjectOpenHashMap<Map<Part, SubTile>> render(WorldAtlasData data, Rect scope, int step) {
		TileRenderIterator tiles = new TileRenderIterator(data);
		tiles.setScope(scope); // the renderer sets the step after the scope
		tiles.setStep(step);
		Long2ObjectOpenHashMap<Map<Part, SubTile>> drawn = new Long2ObjectOpenHashMap<>();
		for (SubTileQuartet quartet : tiles) {
			for (SubTile subtile : quartet) {
				if (subtile.texture == null) continue;
				// the subtile grid starts at (-1, -1): subtiles 2n and 2n + 1 belong to chunk scope.min + n
				long chunk = ChunkPos.asLong(scope.minX + Math.floorDiv(subtile.x, 2) * step, scope.minY + Math.floorDiv(subtile.y, 2) * step);
				SubTile previous = drawn.computeIfAbsent(chunk, k -> new EnumMap<>(Part.class)).put(subtile.part, subtile.copy());
				if (previous != null) throw new AssertionError("part " + subtile.part + " of " + new ChunkPos(chunk) + " drawn twice");
			}
		}
		return drawn;
	}

	@Test
	@DisplayName("Every tile in scope is drawn as its four parts, the first one included")
	void everyTileDrawnWhole() {
		for (Rect scope : new Rect[]{new Rect(0, 0, 5, 4), new Rect(-7, -3, -2, 2), new Rect(3, 3, 3, 3)}) {
			WorldAtlasData data = fill(scope.minX, scope.minY, scope.maxX, scope.maxY, GRASS);
			var drawn = render(data, scope);
			for (int x = scope.minX; x <= scope.maxX; x++) {
				for (int z = scope.minY; z <= scope.maxY; z++) {
					Map<Part, SubTile> parts = drawn.get(ChunkPos.asLong(x, z));
					assertEquals(4, parts == null ? 0 : parts.size(), "tile " + x + "," + z + " in " + scope + " is missing parts: " + (parts == null ? "all" : parts.keySet()));
				}
			}
		}
	}

	@Test
	@DisplayName("A lone tile is four single-object corners")
	void loneTileIsSingleObject() {
		WorldAtlasData data = fill(2, 2, 2, 2, GRASS);
		var parts = render(data, new Rect(0, 0, 4, 4)).get(ChunkPos.asLong(2, 2));
		assertEquals(4, parts.size());
		for (SubTile subtile : parts.values()) assertEquals(Shape.SINGLE_OBJECT, subtile.shape, subtile.part::toString);
	}

	@Test
	@DisplayName("Inside a filled area tiles are full; along its edges they run parallel to the edge")
	void shapesFollowNeighbours() {
		WorldAtlasData data = fill(0, 0, 4, 4, GRASS);
		var drawn = render(data, new Rect(-1, -1, 5, 5));
		for (SubTile subtile : drawn.get(ChunkPos.asLong(2, 2)).values()) assertEquals(Shape.FULL, subtile.shape, "centre " + subtile.part);
		assertEquals(Shape.HORIZONTAL, drawn.get(ChunkPos.asLong(2, 0)).get(Part.TOP_LEFT).shape, "top edge");
		assertEquals(Shape.VERTICAL, drawn.get(ChunkPos.asLong(0, 2)).get(Part.TOP_LEFT).shape, "left edge");
		assertEquals(Shape.CONVEX, drawn.get(ChunkPos.asLong(0, 0)).get(Part.TOP_LEFT).shape, "outer corner");
	}

	@Test
	@DisplayName("Different textures that do not tile together keep their borders")
	void foreignNeighboursKeepBorders() {
		WorldAtlasData data = fill(0, 0, 3, 3, GRASS);
		put(data, 1, 1, WATER);
		var drawn = render(data, new Rect(-1, -1, 4, 4));
		Map<Part, SubTile> water = drawn.get(ChunkPos.asLong(1, 1));
		for (SubTile subtile : water.values()) {
			assertSame(WATER, subtile.texture);
			assertEquals(Shape.SINGLE_OBJECT, subtile.shape, subtile.part::toString);
		}
		assertEquals(Shape.CONCAVE, drawn.get(ChunkPos.asLong(2, 2)).get(Part.TOP_LEFT).shape, "grass wraps around the water's corner");
	}

	@Test
	@DisplayName("Zoomed out (step > 1), every sampled tile is drawn whole and full inside the area")
	void steppedTilesDrawnWhole() {
		Rect scope = new Rect(-8, -8, 8, 8);
		WorldAtlasData data = fill(-8, -8, 8, 8, GRASS);
		var drawn = render(data, scope, 2);
		for (int x = scope.minX; x <= scope.maxX; x += 2) {
			for (int z = scope.minY; z <= scope.maxY; z += 2) {
				Map<Part, SubTile> parts = drawn.get(ChunkPos.asLong(x, z));
				assertEquals(4, parts == null ? 0 : parts.size(), "tile " + x + "," + z);
			}
		}
		for (SubTile subtile : drawn.get(ChunkPos.asLong(0, 0)).values()) assertEquals(Shape.FULL, subtile.shape, subtile.part::toString);
	}

	@Test
	@DisplayName("Re-scoping an iterator restarts it cleanly")
	void rescopeRestarts() {
		WorldAtlasData data = fill(0, 0, 3, 3, GRASS);
		TileRenderIterator tiles = new TileRenderIterator(data);
		tiles.setScope(new Rect(0, 0, 3, 3));
		while (tiles.hasNext()) tiles.next();
		tiles.setScope(new Rect(0, 0, 3, 3));
		SubTileQuartet first = tiles.next();
		assertEquals(-1, first.get(0).x);
		assertEquals(-1, first.get(0).y);
		assertSame(GRASS, first.get(3).texture, "the first quartet's bottom-right corner is tile (0, 0)");
	}
}
