package folk.sisby.antique_atlas;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasFeatureRulesTest {
	private static final Path DEFAULTS = Path.of("src/main/resources/assets/antique_atlas/atlas/features");
	private static final ResourceLocation SWAMP_TAG = ResourceLocation.fromNamespaceAndPath("c", "swamp");
	private static final ResourceLocation PLAINS = ResourceLocation.withDefaultNamespace("plains");
	private static final ResourceLocation SWAMP = ResourceLocation.withDefaultNamespace("swamp");

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("antique_atlas", path);
	}

	// Mirrors TerrainTiling.CUSTOM_TILES, whose class can't load outside FML; the source guard below keeps them in step
	private static final List<ResourceLocation> BASE_TILES = List.of(id("feature/bedrock_roof"), id("feature/empty"), id("feature/end_void"), id("feature/water"), id("feature/ice"), id("feature/ravine"), id("feature/swamp_water"), id("feature/lava"), id("feature/lava_shore"));

	private static FeatureRuleSet defaults(List<String> errors) throws IOException {
		Map<ResourceLocation, FeatureRuleSet.Rule> rules = new HashMap<>();
		try (var files = Files.list(DEFAULTS)) {
			for (Path file : files.toList()) {
				String name = file.getFileName().toString();
				rules.put(id(name.substring(0, name.length() - ".json".length())), FeatureRuleSet.Rule.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file))).getOrThrow());
			}
		}
		return FeatureRuleSet.compile(BASE_TILES, rules, errors::add);
	}

	private static int column(FeatureRuleSet rules, boolean water, String block, ResourceLocation biome) {
		ResourceLocation blockId = ResourceLocation.withDefaultNamespace(block);
		int rule = rules.match(water, rules.blockMask(blockId, tag -> false), rules.biomeMask(biome, tag -> biome.equals(SWAMP) && tag.equals(SWAMP_TAG)));
		return rule < 0 ? -1 : rules.column(rule) * 100 + rules.priority(rule);
	}

	@Test
	@DisplayName("The bundled feature rules pick today's tiles and weights")
	void defaultsMatchHardCodedTiling() throws IOException {
		List<String> errors = new ArrayList<>();
		FeatureRuleSet rules = defaults(errors);
		assertTrue(errors.isEmpty(), errors.toString());
		assertEquals(BASE_TILES.size(), rules.customTileCount(), "the defaults only use the built-in feature tiles");
		assertEquals(BASE_TILES.indexOf(id("feature/water")) * 100 + 4, column(rules, true, "sand", PLAINS));
		assertEquals(BASE_TILES.indexOf(id("feature/swamp_water")) * 100 + 4, column(rules, true, "mud", SWAMP));
		assertEquals(BASE_TILES.indexOf(id("feature/water")) * 100 + 4, column(rules, true, "ice", PLAINS), "water wins over the block under it");
		assertEquals(BASE_TILES.indexOf(id("feature/ice")) * 100 + 3, column(rules, false, "ice", PLAINS));
		assertEquals(BASE_TILES.indexOf(id("feature/lava")) * 100 + 6, column(rules, false, "lava", SWAMP));
		assertEquals(-1, column(rules, false, "stone", PLAINS));
		assertEquals(-1, column(rules, false, "packed_ice", PLAINS));
	}

	@Test
	@DisplayName("Pack rules add tiles, match tags, and win by priority")
	void packRules() {
		Map<ResourceLocation, FeatureRuleSet.Rule> rules = new HashMap<>();
		ResourceLocation rails = id("feature/rails");
		ResourceLocation railTag = ResourceLocation.withDefaultNamespace("rails");
		rules.put(id("lava"), new FeatureRuleSet.Rule(Optional.of(id("feature/lava")), 6, false, List.of("minecraft:lava"), List.of()));
		rules.put(id("rails"), new FeatureRuleSet.Rule(Optional.of(rails), 8, false, List.of("#minecraft:rails"), List.of()));
		rules.put(id("off"), new FeatureRuleSet.Rule(Optional.empty(), 9, true, List.of(), List.of()));
		List<String> errors = new ArrayList<>();
		rules.put(id("everything"), new FeatureRuleSet.Rule(Optional.of(rails), 1, false, List.of(), List.of()));
		FeatureRuleSet set = FeatureRuleSet.compile(BASE_TILES, rules, errors::add);
		assertEquals(1, errors.size(), "a land rule without blocks or biomes is skipped");
		assertEquals(2, set.size(), "a rule without a tile is off");
		assertEquals(BASE_TILES.size() + 1, set.customTileCount());
		assertEquals(rails, set.customTile(BASE_TILES.size()));
		int rule = set.match(false, set.blockMask(ResourceLocation.withDefaultNamespace("rail"), railTag::equals), set.biomeMask(PLAINS, tag -> false));
		assertEquals(id("rails"), set.id(rule));
		assertEquals(BASE_TILES.size(), set.column(rule));
		assertEquals(-1, set.match(true, set.blockMask(ResourceLocation.withDefaultNamespace("rail"), railTag::equals), -1L), "land rules never match under water");
	}

	@Test
	@DisplayName("The test's tile list matches TerrainTiling.CUSTOM_TILES")
	void baseTilesInStep() throws IOException {
		String src = Files.readString(Path.of("src/main/java/folk/sisby/antique_atlas/TerrainTiling.java"));
		String list = src.substring(src.indexOf("CUSTOM_TILES = List.of("), src.indexOf(");", src.indexOf("CUSTOM_TILES = List.of(")));
		String fields = Files.readString(Path.of("src/main/java/folk/sisby/antique_atlas/FeatureTiles.java"));
		List<ResourceLocation> parsed = new ArrayList<>();
		for (String name : list.substring(list.indexOf('(') + 1).split(",")) {
			String field = name.trim().substring("FeatureTiles.".length());
			String marker = " " + field + " = AntiqueAtlas.id(\"";
			String decl = fields.substring(fields.indexOf(marker) + marker.length());
			parsed.add(id(decl.substring(0, decl.indexOf('"'))));
		}
		assertEquals(BASE_TILES, parsed);
	}
}
