package folk.sisby.antique_atlas;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import folk.sisby.surveyor.util.RegionPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of fixes and features that run without the game: terrain re-tiling, the handheld zoom cycle and the config
 * screen labels.
 */
class AtlasBehaviourTest {
	private static Map<RegionPos, BitSet> chunks(ChunkPos... positions) {
		Map<RegionPos, BitSet> map = new java.util.HashMap<>();
		for (ChunkPos pos : positions) map.computeIfAbsent(RegionPos.of(pos), r -> new BitSet()).set(RegionPos.chunkToBit(pos));
		return map;
	}

	@Test
	@DisplayName("Changed terrain re-tiles chunks already drawn, once per update burst")
	void changedTerrainRequeued() {
		WorldAtlasData data = new WorldAtlasData();
		ChunkPos drawn = new ChunkPos(3, -7);
		ChunkPos fresh = new ChunkPos(40, 12);
		data.biomeTiles.put(drawn.toLong(), TileTexture.empty(ResourceLocation.fromNamespaceAndPath("test", "old"), false));
		data.onTerrainUpdated(null, chunks(drawn, fresh));
		assertTrue(data.terrainDeque.contains(drawn), "a chunk already on the map must be tiled again when its terrain changes");
		assertTrue(data.terrainDeque.contains(fresh));
		data.onTerrainUpdated(null, chunks(drawn));
		assertEquals(2, data.terrainDeque.size(), "a chunk waiting to be tiled is queued once");
	}

	@Test
	@DisplayName("Sneak-use cycles the handheld zoom through 1, 2 and 4 chunks per tile")
	void handheldZoomCycle() {
		assertEquals(2, AntiqueAtlas.nextHandheldZoom(1));
		assertEquals(4, AntiqueAtlas.nextHandheldZoom(2));
		assertEquals(1, AntiqueAtlas.nextHandheldZoom(4));
	}

	private static void keys(UnmodifiableConfig config, List<String> out) {
		for (UnmodifiableConfig.Entry entry : config.entrySet()) {
			out.add(entry.getKey());
			if (entry.getValue() instanceof UnmodifiableConfig section) keys(section, out);
		}
	}

	@Test
	@DisplayName("Every config option and section has a config-screen label")
	void everyOptionLabelled() throws IOException {
		JsonObject json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/antique_atlas/lang/en_us.json"))).getAsJsonObject();
		List<String> keys = new ArrayList<>();
		keys(AntiqueAtlasConfig.SPEC.getValues(), keys);
		assertFalse(keys.isEmpty());
		for (String key : keys) assertTrue(json.has("antique_atlas.configuration." + key), "no label for " + key);
	}
}
