package folk.sisby.antique_atlas;

import com.sun.management.ThreadMXBean;
import folk.sisby.antique_atlas.gui.tiles.TileBatch;
import folk.sisby.antique_atlas.gui.tiles.TileRenderIterator;
import folk.sisby.antique_atlas.util.DrawBatcher;
import folk.sisby.antique_atlas.util.Rect;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasAllocationTest {
	private static final double MAX_BYTES_PER_CALL = 4.0;
	private static final int SIZE = 48;

	private WorldAtlasData data;
	private TileTexture[] textures;

	@BeforeEach
	void explore() {
		data = new WorldAtlasData();
		textures = new TileTexture[4];
		for (int i = 0; i < textures.length; i++) textures[i] = TileTexture.empty(ResourceLocation.fromNamespaceAndPath("test", "t" + i), false);
		textures[0].tilesTo().add(textures[1]);
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				if (x > 20 && x < 26 && z > 20 && z < 26) continue;
				data.biomeTiles.put(ChunkPos.asLong(x, z), textures[(x / 6 + z / 9) & 3]);
				data.tileScope.extendTo(x, z);
			}
		}
		data.structureTiles.put(ChunkPos.asLong(3, 3), textures[2]);
	}

	@Test
	@DisplayName("Tile lookups (4–6 per visible tile per frame) do not allocate")
	void tileLookupsDoNotAllocate() {
		int calls = (SIZE + 8) * (SIZE + 8);
		double perCall = allocatedPerCall(() -> {
			int found = 0;
			for (int x = -4; x < SIZE + 4; x++) {
				for (int z = -4; z < SIZE + 4; z++) {
					if (data.getTile(x, z) != null) found++;
				}
			}
			if (found == 0) throw new AssertionError("nothing explored");
		}, calls);
		assertTrue(perCall <= MAX_BYTES_PER_CALL, "getTile allocates " + perCall + " bytes per call");
		assertSame(textures[2], data.getTile(3, 3), "a structure tile covers the biome tile");
	}

	@Test
	@DisplayName("Batching a frame's sub-tiles by texture does not allocate per sub-tile")
	void subTileBatchingDoesNotAllocate() {
		int subtiles = 4 * (SIZE + 2) * (SIZE + 2);
		double perSubtile = allocatedPerCall(() -> {
			TileRenderIterator tiles = new TileRenderIterator(data);
			tiles.setScope(new Rect(0, 0, SIZE, SIZE));
			TileBatch.collect(tiles);
		}, subtiles);
		assertTrue(perSubtile <= MAX_BYTES_PER_CALL, "sub-tile batching allocates " + perSubtile + " bytes per sub-tile");
		int total = 0;
		for (var list : TileBatch.collect(iteratorOverAll()).values()) {
			assertEquals(0, list.size() % TileBatch.STRIDE, "each sub-tile is exactly " + TileBatch.STRIDE + " ints");
			total += list.size() / TileBatch.STRIDE;
		}
		assertTrue(total > 0, "the frame drew nothing");
	}

	@Test
	@DisplayName("The Iris probe is resolved once and costs nothing per batcher")
	void irisProbeDoesNotAllocate() {
		int calls = 10_000;
		double perCall = allocatedPerCall(() -> {
			for (int i = 0; i < calls; i++) DrawBatcher.areWeShadersRightNow();
		}, calls);
		assertTrue(perCall <= MAX_BYTES_PER_CALL, "the shader probe allocates " + perCall + " bytes per call (reflection or a thrown exception)");
	}

	@Test
	@DisplayName("The marker snapshot is reused until markers or zoom change")
	void markerSnapshotIsReused() {
		var first = data.getAllMarkers(1);
		assertSame(first, data.getAllMarkers(1), "same markers, same zoom: same snapshot");
		double perCall = allocatedPerCall(() -> {
			for (int i = 0; i < 1000; i++) data.getAllMarkers(1);
		}, 1000);
		assertTrue(perCall <= MAX_BYTES_PER_CALL, "getAllMarkers allocates " + perCall + " bytes per call");
		data.markerVersion++;
		assertTrue(first != data.getAllMarkers(1), "a marker change rebuilds the snapshot");
	}

	@Test
	@DisplayName("A dimension explored only along chunk row z = 0 is not empty")
	void singleRowIsNotEmpty() {
		WorldAtlasData row = new WorldAtlasData();
		row.biomeTiles.put(ChunkPos.asLong(5, 0), textures[0]);
		row.tileScope.extendTo(5, 0);
		WorldAtlasData.WORLDS.clear();
		WorldAtlasData.WORLDS.put(net.minecraft.world.level.Level.OVERWORLD, row);
		try {
			assertTrue(!WorldAtlasData.isEmpty(net.minecraft.world.level.Level.OVERWORLD));
		} finally {
			WorldAtlasData.WORLDS.clear();
		}
	}

	@Test
	@DisplayName("Editable markers are listed in a stable order")
	void editableMarkersStableOrder() throws IOException {
		String src = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/folk/sisby/antique_atlas/WorldAtlasData.java"));
		assertTrue(src.contains("new TreeMap<>(EDITABLE_ORDER)"), "Landmark hashes by identity: a HashMap reorders the list every session");
	}

	private TileRenderIterator iteratorOverAll() {
		TileRenderIterator tiles = new TileRenderIterator(data);
		tiles.setScope(new Rect(0, 0, SIZE, SIZE));
		return tiles;
	}

	private static double allocatedPerCall(Runnable pass, int callsPerPass) {
		ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
		long thread = Thread.currentThread().threadId();
		for (int warm = 0; warm < 20; warm++) pass.run();
		long before = threads.getThreadAllocatedBytes(thread);
		pass.run();
		return (double) (threads.getThreadAllocatedBytes(thread) - before) / callsPerPass;
	}
}
