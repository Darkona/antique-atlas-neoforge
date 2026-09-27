package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.gui.tiles.SubTile;
import folk.sisby.antique_atlas.gui.tiles.SubTile.Part;
import folk.sisby.antique_atlas.gui.tiles.SubTileQuartet;
import folk.sisby.antique_atlas.gui.tiles.TileBatch;
import folk.sisby.antique_atlas.gui.tiles.TileRenderIterator;
import folk.sisby.antique_atlas.util.Rect;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileEdgeFadeTest {
	private static final TileTexture GRASS = TileTexture.empty(ResourceLocation.fromNamespaceAndPath("test", "grass"), false);
	// DrawBatcher's vertex order: (0,0), (0,1), (1,1), (1,0) in subtile space
	private static final int V00 = 1, V01 = 2, V11 = 4, V10 = 8;

	private static WorldAtlasData fill(int minX, int minZ, int maxX, int maxZ) {
		WorldAtlasData data = new WorldAtlasData();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				data.biomeTiles.put(ChunkPos.asLong(x, z), GRASS);
				data.tileScope.extendTo(x, z);
			}
		}
		return data;
	}

	/** The faded vertices of each drawn part, keyed by the subtile's owning chunk. */
	private static Long2ObjectOpenHashMap<Map<Part, Integer>> fades(WorldAtlasData data, Rect scope) {
		TileRenderIterator tiles = new TileRenderIterator(data);
		tiles.setScope(scope);
		Long2ObjectOpenHashMap<Map<Part, Integer>> fades = new Long2ObjectOpenHashMap<>();
		for (SubTileQuartet quartet : tiles) {
			for (SubTile subtile : quartet) {
				if (subtile.texture == null) continue;
				long chunk = ChunkPos.asLong(scope.minX + Math.floorDiv(subtile.x, 2), scope.minY + Math.floorDiv(subtile.y, 2));
				fades.computeIfAbsent(chunk, k -> new EnumMap<>(Part.class)).put(subtile.part, subtile.fadeCorners());
			}
		}
		return fades;
	}

	@Test
	@DisplayName("Tiles inside an explored area don't fade")
	void interiorOpaque() {
		var fades = fades(fill(0, 0, 4, 4), new Rect(-1, -1, 5, 5));
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				for (Map.Entry<Part, Integer> part : fades.get(ChunkPos.asLong(x, z)).entrySet()) {
					assertEquals(0, part.getValue(), "tile " + x + "," + z + " " + part.getKey());
				}
			}
		}
	}

	@Test
	@DisplayName("A tile on the top edge fades its top side; its inner parts stay opaque")
	void topEdge() {
		Map<Part, Integer> tile = fades(fill(0, 0, 4, 4), new Rect(-1, -1, 5, 5)).get(ChunkPos.asLong(2, 0));
		// The top-left part spans from the tile's top-left corner (0,0) to its centre (1,1): the top side is transparent
		assertEquals(V00 | V10, tile.get(Part.TOP_LEFT));
		assertEquals(V00 | V10, tile.get(Part.TOP_RIGHT));
		assertEquals(0, tile.get(Part.BOTTOM_LEFT));
		assertEquals(0, tile.get(Part.BOTTOM_RIGHT));
	}

	@Test
	@DisplayName("A corner tile fades both outer sides; the tile centre never fades")
	void outerCorner() {
		Map<Part, Integer> tile = fades(fill(0, 0, 4, 4), new Rect(-1, -1, 5, 5)).get(ChunkPos.asLong(0, 0));
		assertEquals(V00 | V01 | V10, tile.get(Part.TOP_LEFT), "all but the centre (1,1)");
		assertEquals(V00 | V10, tile.get(Part.TOP_RIGHT), "top side only");
		assertEquals(V00 | V01, tile.get(Part.BOTTOM_LEFT), "left side only");
		assertEquals(0, tile.get(Part.BOTTOM_RIGHT));
	}

	@Test
	@DisplayName("A lone tile fades on every side, keeping only its centre")
	void loneTile() {
		Map<Part, Integer> tile = fades(fill(2, 2, 2, 2), new Rect(0, 0, 4, 4)).get(ChunkPos.asLong(2, 2));
		assertEquals(V00 | V01 | V10, tile.get(Part.TOP_LEFT));
		assertEquals(V00 | V10 | V11, tile.get(Part.TOP_RIGHT));
		assertEquals(V00 | V01 | V11, tile.get(Part.BOTTOM_LEFT));
		assertEquals(V01 | V11 | V10, tile.get(Part.BOTTOM_RIGHT));
	}

	@Test
	@DisplayName("A missing diagonal neighbour fades only the shared corner")
	void diagonalHole() {
		WorldAtlasData data = fill(0, 0, 4, 4);
		data.biomeTiles.remove(ChunkPos.asLong(3, 3));
		Map<Part, Integer> tile = fades(data, new Rect(-1, -1, 5, 5)).get(ChunkPos.asLong(2, 2));
		assertEquals(V11, tile.get(Part.BOTTOM_RIGHT), "the corner touching the hole");
		assertEquals(0, tile.get(Part.TOP_LEFT));
	}

	@Test
	@DisplayName("The batch carries the faded vertices and the renderer honours the option")
	void batchedAndConfigured() throws IOException {
		assertEquals(5, TileBatch.STRIDE);
		String renderer = Files.readString(Path.of("src/main/java/folk/sisby/antique_atlas/gui/AtlasRenderer.java"));
		assertTrue(renderer.contains("boolean fade = AntiqueAtlas.CONFIG.fadeEdges;") && renderer.contains("data[i + 4]"), "renderTiles reads the fade mask behind the option");
		assertTrue(Files.readString(Path.of("src/main/java/folk/sisby/antique_atlas/AntiqueAtlasConfig.java")).contains("public boolean fadeEdges = false;"));
	}
}
