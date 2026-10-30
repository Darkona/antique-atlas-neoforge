package folk.sisby.antique_atlas.reloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeFallbackTest {
	private static final Pattern TAG_FILE = Pattern.compile("data/([a-z0-9_.-]+)/tags/worldgen/biome/([a-z0-9_./-]+)\\.json");
	private static final Map<ResourceLocation, List<String>> TAG_ENTRIES = new HashMap<>();

	private static final Map<String, String> EXPECTED = new TreeMap<>(Map.ofEntries(
		Map.entry("badlands", "badlands"),
		Map.entry("bamboo_jungle", "jungle"),
		Map.entry("basalt_deltas", "soul_sand_valley"),
		Map.entry("beach", "beach"),
		Map.entry("birch_forest", "birch_forest"),
		Map.entry("cherry_grove", "flower_forest"),
		Map.entry("cold_ocean", "river"),
		Map.entry("crimson_forest", "warped_forest"),
		Map.entry("dark_forest", "birch_forest"),
		Map.entry("deep_cold_ocean", "river"),
		Map.entry("deep_dark", "dripstone_caves"),
		Map.entry("deep_frozen_ocean", "frozen_river"),
		Map.entry("deep_lukewarm_ocean", "river"),
		Map.entry("deep_ocean", "river"),
		Map.entry("desert", "desert"),
		Map.entry("dripstone_caves", "dripstone_caves"),
		Map.entry("end_barrens", "end_barrens"),
		Map.entry("end_highlands", "end_barrens"),
		Map.entry("end_midlands", "end_barrens"),
		Map.entry("eroded_badlands", "badlands"),
		Map.entry("flower_forest", "flower_forest"),
		Map.entry("forest", "birch_forest"),
		Map.entry("frozen_ocean", "frozen_river"),
		Map.entry("frozen_peaks", "frozen_ocean"),
		Map.entry("frozen_river", "frozen_river"),
		Map.entry("grove", "snowy_taiga"),
		Map.entry("ice_spikes", "frozen_ocean"),
		Map.entry("jagged_peaks", "jagged_peaks"),
		Map.entry("jungle", "jungle"),
		Map.entry("lukewarm_ocean", "river"),
		Map.entry("lush_caves", "dripstone_caves"),
		Map.entry("mangrove_swamp", "swamp"),
		Map.entry("meadow", "flower_forest"),
		Map.entry("mushroom_fields", "mushroom_fields"),
		Map.entry("nether_wastes", "soul_sand_valley"),
		Map.entry("ocean", "river"),
		Map.entry("old_growth_birch_forest", "birch_forest"),
		Map.entry("old_growth_pine_taiga", "taiga"),
		Map.entry("old_growth_spruce_taiga", "taiga"),
		Map.entry("plains", "plains"),
		Map.entry("river", "river"),
		Map.entry("savanna", "savanna"),
		Map.entry("savanna_plateau", "savanna"),
		Map.entry("small_end_islands", "end_barrens"),
		Map.entry("snowy_beach", "beach"),
		Map.entry("snowy_plains", "snowy_plains"),
		Map.entry("snowy_slopes", "stony_peaks"),
		Map.entry("snowy_taiga", "snowy_taiga"),
		Map.entry("soul_sand_valley", "soul_sand_valley"),
		Map.entry("sparse_jungle", "jungle"),
		Map.entry("stony_peaks", "jagged_peaks"),
		Map.entry("stony_shore", "stony_shore"),
		Map.entry("sunflower_plains", "flower_forest"),
		Map.entry("swamp", "swamp"),
		Map.entry("taiga", "taiga"),
		Map.entry("the_end", "end_barrens"),
		Map.entry("the_void", "the_void"),
		Map.entry("warm_ocean", "river"),
		Map.entry("warped_forest", "warped_forest"),
		Map.entry("windswept_forest", "birch_forest"),
		Map.entry("windswept_gravelly_hills", "windswept_hills"),
		Map.entry("windswept_hills", "windswept_hills"),
		Map.entry("windswept_savanna", "savanna"),
		Map.entry("wooded_badlands", "badlands")
	));

	@BeforeAll
	static void loadBiomeTags() throws IOException {
		for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
			File file = new File(entry);
			if (file.isDirectory()) {
				Path root = file.toPath();
				try (Stream<Path> paths = Files.walk(root)) {
					for (Path path : (Iterable<Path>) paths::iterator) {
						String name = root.relativize(path).toString().replace(File.separatorChar, '/');
						Matcher matcher = TAG_FILE.matcher(name);
						if (matcher.matches()) readTag(matcher, Files.readString(path));
					}
				}
			} else if (file.isFile() && entry.endsWith(".jar")) {
				try (ZipFile zip = new ZipFile(file)) {
					for (ZipEntry zipEntry : zip.stream().map(ZipEntry.class::cast).toList()) {
						Matcher matcher = TAG_FILE.matcher(zipEntry.getName());
						if (!matcher.matches()) continue;
						try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(zipEntry), StandardCharsets.UTF_8)) {
							readTag(matcher, JsonParser.parseReader(reader).toString());
						}
					}
				}
			}
		}
	}

	private static void readTag(Matcher matcher, String json) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(matcher.group(1), matcher.group(2));
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		List<String> entries = TAG_ENTRIES.computeIfAbsent(id, k -> new ArrayList<>());
		if (object.has("replace") && object.get("replace").getAsBoolean()) entries.clear();
		for (JsonElement value : object.getAsJsonArray("values")) {
			entries.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
		}
	}

	private static boolean contains(ResourceLocation tag, ResourceLocation biome, Set<ResourceLocation> visiting) {
		List<String> entries = TAG_ENTRIES.get(tag);
		if (entries == null || !visiting.add(tag)) return false;
		for (String entry : entries) {
			if (entry.startsWith("#") ? contains(ResourceLocation.parse(entry.substring(1)), biome, visiting) : ResourceLocation.parse(entry).equals(biome)) return true;
		}
		return false;
	}

	private static List<ResourceKey<Biome>> vanillaBiomes() throws IllegalAccessException {
		List<ResourceKey<Biome>> biomes = new ArrayList<>();
		for (Field field : Biomes.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && field.get(null) instanceof ResourceKey<?> key && key.isFor(Registries.BIOME)) {
				biomes.add(key.cast(Registries.BIOME).orElseThrow());
			}
		}
		return biomes;
	}

	@Test
	@DisplayName("Every vanilla biome falls back to the same tile biome as before")
	void vanillaFallbacksUnchanged() throws IllegalAccessException {
		assertTrue(TAG_ENTRIES.containsKey(ResourceLocation.withDefaultNamespace("is_badlands")), "no vanilla biome tags on the test classpath");
		assertTrue(TAG_ENTRIES.containsKey(ResourceLocation.fromNamespaceAndPath("c", "is_badlands")), "no NeoForge biome tags on the test classpath");
		Map<String, String> actual = new TreeMap<>();
		for (ResourceKey<Biome> biome : vanillaBiomes()) {
			ResourceLocation fallback = BiomeTileProviders.getFallbackBiome((TagKey<Biome> tag) -> contains(tag.location(), biome.location(), new HashSet<>()));
			actual.put(biome.location().getPath(), fallback == null ? "none" : fallback.getPath());
		}
		assertEquals(EXPECTED, actual);
	}
}
