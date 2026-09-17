package folk.sisby.antique_atlas;

import folk.sisby.surveyor.client.SurveyorClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

@SuppressWarnings("CanBeFinal")
public class AntiqueAtlasConfig {
	public enum GraveStyle {
		CAUSE,
		GRAVE,
		ITEMS,
		DIED,
		EUPHEMISMS
	}

	public enum FallbackHandling {
		TEST,
		MISSING,
		PLAINS,
		CRASH
	}

	public enum EmptyHandling {
		CLOUDS,
		EMPTY
	}

	public boolean fullscreen = true;
	public boolean requireItem = false;
	public boolean keepZoom = false;
	public boolean keepOffset = false;
	public GraveStyle graveStyle = GraveStyle.EUPHEMISMS;
	public int maxTileChunks = 5;
	public int maxTilePixels = 1;
	public int mapScale = 0;
	public int chunkTickLimit = 100;
	public FallbackHandling fallbackFailHandling = FallbackHandling.MISSING;
	public EmptyHandling emptyHandling = EmptyHandling.EMPTY;
	public Map<String, Boolean> structureMarkers = new LinkedHashMap<>(Map.of("minecraft:type/end_city", false));
	public Dimensions dimensions = new Dimensions();

	public static class Dimensions {
		public Map<String, Integer> scales = new LinkedHashMap<>();

		{
			scales.put("minecraft:overworld", 8);
			scales.put("minecraft:the_nether", 1);
			scales.put("minecraft:the_end", 0);
		}

		private static final long MEMO_MILLIS = 250;
		private ClientPacketListener memoHandler;
		private long memoUntil;
		private List<ResourceKey<Level>> memoOrder = List.of();
		private Map<ResourceKey<Level>, Integer> memoScales = Map.of();

		public List<ResourceKey<Level>> getOrder(ClientPacketListener handler) {
			refresh(handler);
			return memoOrder;
		}

		public Map<ResourceKey<Level>, Integer> getScales(ClientPacketListener handler) {
			refresh(handler);
			return memoScales;
		}

		void invalidate() {
			memoHandler = null;
		}

		private void refresh(ClientPacketListener handler) {
			long now = Util.getMillis();
			if (handler == memoHandler && now < memoUntil) return;
			List<ResourceKey<Level>> dims = new ArrayList<>(SurveyorClient.getSummaries(handler).keySet().stream().sorted(Comparator.comparing(ResourceKey::toString)).toList());
			dims.removeIf(WorldAtlasData::isEmpty);
			scales.keySet().removeIf(v -> handler.levels().stream().noneMatch(d -> d.location().toString().equals(v)));
			dims.stream().filter(dim -> !scales.containsKey(dim.location().toString())).forEach(dim -> scales.put(dim.location().toString(), 0));
			List<String> scaleOrder = List.copyOf(scales.keySet());
			memoOrder = dims.stream().sorted(Comparator.comparing(dim -> scaleOrder.indexOf(dim.location().toString()))).toList();
			memoScales = Collections.unmodifiableMap(dims.stream().collect(Collectors.toMap(k -> k, k -> scales.get(k.location().toString()))));
			memoHandler = handler;
			memoUntil = now + MEMO_MILLIS;
		}
	}

	public static final ModConfigSpec SPEC;
	private static final ModConfigSpec.BooleanValue FULLSCREEN;
	private static final ModConfigSpec.BooleanValue REQUIRE_ITEM;
	private static final ModConfigSpec.BooleanValue KEEP_ZOOM;
	private static final ModConfigSpec.BooleanValue KEEP_OFFSET;
	private static final ModConfigSpec.EnumValue<GraveStyle> GRAVE_STYLE;
	private static final ModConfigSpec.IntValue MAX_TILE_CHUNKS;
	private static final ModConfigSpec.IntValue MAX_TILE_PIXELS;
	private static final ModConfigSpec.IntValue MAP_SCALE;
	private static final ModConfigSpec.IntValue CHUNK_TICK_LIMIT;
	private static final ModConfigSpec.EnumValue<FallbackHandling> FALLBACK_FAIL_HANDLING;
	private static final ModConfigSpec.EnumValue<EmptyHandling> EMPTY_HANDLING;
	private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_MARKERS;
	private static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_SCALES;

	static {
		AntiqueAtlasConfig d = new AntiqueAtlasConfig();
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();
		FULLSCREEN = b.comment("Whether to display the map in full-screen", "The background is slightly less stylish, but more tiles are shown at once").define("fullscreen", d.fullscreen);
		REQUIRE_ITEM = b.comment("Whether to require an item to display the map").define("requireItem", d.requireItem);
		KEEP_ZOOM = b.comment("Whether to keep scale after closing the map").define("keepZoom", d.keepZoom);
		KEEP_OFFSET = b.comment("Whether to keep offset after closing the map").define("keepOffset", d.keepOffset);
		GRAVE_STYLE = b.comment("How to depict player death locations.").defineEnum("graveStyle", d.graveStyle);
		MAX_TILE_CHUNKS = b.comment("The maximum number of chunks to represent as a tile, as a power of 2", "Effectively the 'minimum zoom'", "0: 1x1 chunk = 1 tile | 6: 64x64 chunks = 1 tile").defineInRange("maxTileChunks", d.maxTileChunks, 0, 6);
		MAX_TILE_PIXELS = b.comment("The maximum size to render a tile at, as a power of 2 multiplier", "Effectively the 'maximum zoom'", "0: 1 tile = 16x16 | 3: 1 tile = 128x128").defineInRange("maxTilePixels", d.maxTilePixels, 0, 3);
		MAP_SCALE = b.comment(
			"The effective GUI scale for tiles and markers - independent of the overall GUI scale.",
			"0 will match your GUI scale - pixels will be the same size as the background & buttons",
			"-1 will use half your GUI scale, rounding up.",
			"-2 will use half your GUI scale, rounding down."
		).defineInRange("mapScale", d.mapScale, -2, 10);
		CHUNK_TICK_LIMIT = b.comment("The maximum number of chunks to load onto the map per tick after entering a world").defineInRange("chunkTickLimit", d.chunkTickLimit, 1, Integer.MAX_VALUE);
		FALLBACK_FAIL_HANDLING = b.comment("How to handle biomes that aren't in any minecraft, conventional, or forge biome tags").defineEnum("fallbackFailHandling", d.fallbackFailHandling);
		EMPTY_HANDLING = b.comment("How to display areas that aren't explored yet").defineEnum("emptyHandling", d.emptyHandling);
		STRUCTURE_MARKERS = b.comment("Whether to show each structure marker, as \"structure=true|false\". Unlisted structures are shown.", "Structures found in loaded resource packs are added here automatically.")
			.defineListAllowEmpty("structureMarkers", toEntries(d.structureMarkers), () -> "", AntiqueAtlasConfig::isEntry);
		b.push("dimensions");
		DIMENSION_SCALES = b.comment("Cycle order and coordinate scales of each dimension, as \"dimension=scale\".", "If not 0, the relative position of the player will be shown.")
			.defineListAllowEmpty("scales", toEntries(d.dimensions.scales), () -> "", AntiqueAtlasConfig::isEntry);
		b.pop();
		SPEC = b.build();
	}

	public void bake() {
		fullscreen = FULLSCREEN.get();
		requireItem = REQUIRE_ITEM.get();
		keepZoom = KEEP_ZOOM.get();
		keepOffset = KEEP_OFFSET.get();
		graveStyle = GRAVE_STYLE.get();
		maxTileChunks = MAX_TILE_CHUNKS.get();
		maxTilePixels = MAX_TILE_PIXELS.get();
		mapScale = MAP_SCALE.get();
		chunkTickLimit = CHUNK_TICK_LIMIT.get();
		fallbackFailHandling = FALLBACK_FAIL_HANDLING.get();
		emptyHandling = EMPTY_HANDLING.get();
		structureMarkers = fromEntries(STRUCTURE_MARKERS.get(), Boolean::parseBoolean);
		dimensions.scales = fromEntries(DIMENSION_SCALES.get(), Integer::parseInt);
		dimensions.invalidate();
	}

	public void saveStructureMarkers() {
		List<String> entries = toEntries(structureMarkers);
		if (SPEC.isLoaded() && !entries.equals(STRUCTURE_MARKERS.get())) {
			STRUCTURE_MARKERS.set(entries);
			SPEC.save();
		}
	}

	private static boolean isEntry(Object o) {
		return o instanceof String s && s.indexOf('=') > 0;
	}

	private static <V> List<String> toEntries(Map<String, V> map) {
		return map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();
	}

	private static <V> Map<String, V> fromEntries(List<? extends String> entries, Function<String, V> parser) {
		Map<String, V> map = new LinkedHashMap<>();
		for (String entry : entries) {
			int split = entry.lastIndexOf('=');
			try {
				map.put(entry.substring(0, split).trim(), parser.apply(entry.substring(split + 1).trim()));
			} catch (RuntimeException e) {
				AntiqueAtlas.LOGGER.warn("[Antique Atlas] Ignoring malformed config entry '{}'", entry);
			}
		}
		return map;
	}
}
