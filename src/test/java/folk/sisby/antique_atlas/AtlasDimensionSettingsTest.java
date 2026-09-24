package folk.sisby.antique_atlas;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasDimensionSettingsTest {
	private static final Path BUNDLED = Path.of("src/main/resources/assets/minecraft/atlas/dimension");
	private static final Path SRC = Path.of("src/main/java/folk/sisby/antique_atlas");

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("antique_atlas", path);
	}

	private static DimensionSettings parse(String json) {
		return DimensionSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
	}

	private static DimensionSettings bundled(String dimension) throws IOException {
		return parse(Files.readString(BUNDLED.resolve(dimension + ".json")));
	}

	@Test
	@DisplayName("The bundled dimension files reproduce the old hard-coded tiling")
	void bundledMatchHardCoded() throws IOException {
		// Old code: SEA_LEVEL = 63, ravines only in the overworld, END -> END_VOID, else EMPTY
		assertEquals(new DimensionSettings(DimensionSettings.Scanner.SURFACE, 63, Optional.empty(), 50, true, id("feature/empty")), bundled("overworld"));
		assertEquals(new DimensionSettings(DimensionSettings.Scanner.SURFACE, 63, Optional.empty(), 50, false, id("feature/end_void")), bundled("the_end"));
		// Old code: nether scanner, NETHER_SCAN_HEIGHT = 50, logicalTopY = 126, SEA_DEPTH = topY - 31, BEDROCK_ROOF
		assertEquals(new DimensionSettings(DimensionSettings.Scanner.NETHER, 31, Optional.of(126), 50, false, id("feature/bedrock_roof")), bundled("the_nether"));
	}

	@Test
	@DisplayName("A dimension without a file, or an empty file, uses the old generic tiling")
	void defaults() {
		assertEquals(DimensionSettings.DEFAULT, parse("{}"));
		assertEquals(new DimensionSettings(DimensionSettings.Scanner.SURFACE, 63, Optional.empty(), 50, false, id("feature/empty")), DimensionSettings.DEFAULT);
		assertEquals(null, DimensionSettings.DEFAULT.scanTopOrNull(), "no ceiling: the whole column is scanned");
		assertTrue(DimensionSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"scanner\": \"sky\"}")).error().isPresent(), "an unknown scanner is an error");
	}

	@Test
	@DisplayName("The empty tile keeps its old column; an unknown tile gets the extra column")
	void emptyTileColumn() {
		List<ResourceLocation> base = List.of(id("feature/bedrock_roof"), id("feature/empty"), id("feature/end_void"));
		FeatureRuleSet rules = FeatureRuleSet.compile(base, Map.of(), e -> {});
		assertEquals(0, rules.customTileIndex(id("feature/bedrock_roof")));
		assertEquals(1, rules.customTileIndex(id("feature/empty")));
		assertEquals(2, rules.customTileIndex(id("feature/end_void")));
		assertEquals(rules.customTileCount(), rules.customTileIndex(id("feature/clouds")));
	}

	@Test
	@DisplayName("Tiling reads the dimension settings, not hard-coded dimensions")
	void noHardCodedDimensions() throws IOException {
		String tiling = Files.readString(SRC.resolve("TerrainTiling.java"));
		for (String old : new String[]{"Level.END", "Level.OVERWORLD", "Level.NETHER", "SEA_LEVEL", "NETHER_SCAN_HEIGHT", "logicalTopY"}) {
			assertFalse(tiling.contains(old), "TerrainTiling still uses " + old);
		}
		String data = Files.readString(SRC.resolve("WorldAtlasData.java"));
		assertFalse(data.contains("Level.NETHER"), "the scanner comes from the dimension settings");
		assertTrue(data.contains("DimensionConfigs.getInstance().get(k)"), "settings are resolved once, when the dimension's data is created");
		String main = Files.readString(SRC.resolve("AntiqueAtlas.java"));
		assertTrue(main.indexOf("registerReloadListener(DimensionConfigs.getInstance())") < main.indexOf("manager -> rebuildPending = true"), "dimension settings load before the rebuild is requested");
	}
}
