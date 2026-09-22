package folk.sisby.antique_atlas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Feature rules (water, ice, lava...) from {@code atlas/features/*.json}, compiled at reload into bitmasks.
 * Rules are sorted so the lowest matching bit wins: the highest priority, then rules with a biome list, then with a block list, then by id.
 * Registry-free, so it can be tested without a Minecraft bootstrap; {@link TerrainTiling} caches the masks per block and biome.
 */
public final class FeatureRuleSet {
	public static final int MAX_RULES = Long.SIZE - 1; // bit 63 stays free, so no mask is ever -1 (the cache's "unknown")
	public static final int DEFAULT_PRIORITY = 1;

	public record Rule(Optional<ResourceLocation> tile, int priority, boolean water, List<String> blocks, List<String> biomes) {
		public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.optionalFieldOf("tile").forGetter(Rule::tile),
			Codec.INT.optionalFieldOf("priority", DEFAULT_PRIORITY).forGetter(Rule::priority),
			Codec.BOOL.optionalFieldOf("water", false).forGetter(Rule::water),
			Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(Rule::blocks),
			Codec.STRING.listOf().optionalFieldOf("biomes", List.of()).forGetter(Rule::biomes)
		).apply(instance, Rule::new));
	}

	private final ResourceLocation[] customTiles;
	private final ResourceLocation[] ids;
	private final int[] columns;
	private final int[] priorities;
	private final long waterRules;
	private final long landRules;
	private final long anyBlock;
	private final long anyBiome;
	private final Set<ResourceLocation>[] blockIds, blockTags, biomeIds, biomeTags;

	@SuppressWarnings("unchecked")
	private FeatureRuleSet(List<ResourceLocation> baseTiles, List<Map.Entry<ResourceLocation, Rule>> rules) {
		List<ResourceLocation> tiles = new ArrayList<>(baseTiles);
		int size = rules.size();
		ids = new ResourceLocation[size];
		columns = new int[size];
		priorities = new int[size];
		blockIds = new Set[size];
		blockTags = new Set[size];
		biomeIds = new Set[size];
		biomeTags = new Set[size];
		long water = 0, land = 0, noBlocks = 0, noBiomes = 0;
		for (int r = 0; r < size; r++) {
			Rule rule = rules.get(r).getValue();
			ResourceLocation tile = rule.tile().orElseThrow();
			int column = tiles.indexOf(tile);
			if (column < 0) {
				column = tiles.size();
				tiles.add(tile);
			}
			ids[r] = rules.get(r).getKey();
			columns[r] = column;
			priorities[r] = rule.priority();
			blockIds[r] = new HashSet<>();
			blockTags[r] = new HashSet<>();
			biomeIds[r] = new HashSet<>();
			biomeTags[r] = new HashSet<>();
			split(rule.blocks(), blockIds[r], blockTags[r]);
			split(rule.biomes(), biomeIds[r], biomeTags[r]);
			long bit = 1L << r;
			if (rule.water()) water |= bit;
			else land |= bit;
			if (rule.blocks().isEmpty()) noBlocks |= bit;
			if (rule.biomes().isEmpty()) noBiomes |= bit;
		}
		customTiles = tiles.toArray(ResourceLocation[]::new);
		waterRules = water;
		landRules = land;
		anyBlock = noBlocks;
		anyBiome = noBiomes;
	}

	private static void split(List<String> entries, Set<ResourceLocation> outIds, Set<ResourceLocation> outTags) {
		for (String entry : entries) {
			boolean tag = entry.startsWith("#");
			ResourceLocation id = ResourceLocation.tryParse(tag ? entry.substring(1) : entry);
			if (id == null) throw new IllegalArgumentException("Invalid id " + entry);
			(tag ? outTags : outIds).add(id);
		}
	}

	/**
	 * @param baseTiles the fixed custom tiles; rule tiles that aren't among them get new columns after them.
	 * @param errors receives one message per rule that was skipped.
	 */
	public static FeatureRuleSet compile(List<ResourceLocation> baseTiles, Map<ResourceLocation, Rule> rules, Consumer<String> errors) {
		List<Map.Entry<ResourceLocation, Rule>> valid = new ArrayList<>();
		for (Map.Entry<ResourceLocation, Rule> entry : rules.entrySet()) {
			Rule rule = entry.getValue();
			if (rule.tile().isEmpty()) continue; // a pack turned this rule off
			if (!rule.water() && rule.blocks().isEmpty() && rule.biomes().isEmpty()) {
				errors.accept("Feature rule %s has no \"water\", \"blocks\" or \"biomes\"; it would match every column. Skipping.".formatted(entry.getKey()));
				continue;
			}
			try {
				split(rule.blocks(), new HashSet<>(), new HashSet<>());
				split(rule.biomes(), new HashSet<>(), new HashSet<>());
			} catch (IllegalArgumentException e) {
				errors.accept("Feature rule %s: %s. Skipping.".formatted(entry.getKey(), e.getMessage()));
				continue;
			}
			valid.add(entry);
		}
		valid.sort(Comparator.<Map.Entry<ResourceLocation, Rule>>comparingInt(e -> -e.getValue().priority())
			.thenComparing(e -> e.getValue().biomes().isEmpty())
			.thenComparing(e -> e.getValue().blocks().isEmpty())
			.thenComparing(Map.Entry::getKey));
		while (valid.size() > MAX_RULES) {
			errors.accept("Too many feature rules (max %s). Skipping %s.".formatted(MAX_RULES, valid.removeLast().getKey()));
		}
		return new FeatureRuleSet(baseTiles, valid);
	}

	public static FeatureRuleSet empty(List<ResourceLocation> baseTiles) {
		return new FeatureRuleSet(baseTiles, List.of());
	}

	/**
	 * Rules whose block list allows this block. Called once per block, then cached.
	 */
	public long blockMask(ResourceLocation blockId, Predicate<ResourceLocation> inTag) {
		return mask(anyBlock, blockIds, blockTags, blockId, inTag);
	}

	/**
	 * Rules whose biome list allows this biome. Called once per biome, then cached.
	 */
	public long biomeMask(ResourceLocation biomeId, Predicate<ResourceLocation> inTag) {
		return mask(anyBiome, biomeIds, biomeTags, biomeId, inTag);
	}

	private static long mask(long any, Set<ResourceLocation>[] ids, Set<ResourceLocation>[] tags, ResourceLocation id, Predicate<ResourceLocation> inTag) {
		long mask = any;
		for (int r = 0; r < ids.length; r++) {
			if (ids[r].contains(id) || tags[r].stream().anyMatch(inTag)) mask |= 1L << r;
		}
		return mask;
	}

	/**
	 * @return the winning rule for a column, or -1. Allocation-free.
	 */
	public int match(boolean underWater, long blockMask, long biomeMask) {
		long candidates = (underWater ? waterRules : landRules) & blockMask & biomeMask;
		return candidates == 0 ? -1 : Long.numberOfTrailingZeros(candidates);
	}

	/**
	 * @return the rule's custom tile index (offset from the biome palette size).
	 */
	public int column(int rule) {
		return columns[rule];
	}

	public int priority(int rule) {
		return priorities[rule];
	}

	public ResourceLocation id(int rule) {
		return ids[rule];
	}

	public int size() {
		return ids.length;
	}

	public int customTileCount() {
		return customTiles.length;
	}

	public ResourceLocation customTile(int index) {
		return customTiles[index];
	}
}
