package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.FeatureRules;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrain;
import folk.sisby.surveyor.util.RegistryPalette;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import java.util.List;

/**
 * Hottest class in the mod. Might get ugly.
 */
public class TerrainTiling {
	public static final int EMPTY_PRIORITY = 16;
	public static final int RAVINE_PRIORITY = 12;
	public static final int BEACH_PRIORITY = 3;

	public static final List<ResourceLocation> CUSTOM_TILES = List.of(
		FeatureTiles.BEDROCK_ROOF,
		FeatureTiles.EMPTY,
		FeatureTiles.END_VOID,
		FeatureTiles.WATER,
		FeatureTiles.ICE,
		FeatureTiles.TILE_RAVINE,
		FeatureTiles.SWAMP_WATER,
		FeatureTiles.TILE_LAVA,
		FeatureTiles.TILE_LAVA_SHORE
	);

	private static final Reference2IntOpenHashMap<Biome> PRIORITY_CACHE = new Reference2IntOpenHashMap<>();
	// Feature rule masks per block and biome, for the rule set they were computed from
	private static final Reference2LongOpenHashMap<Block> BLOCK_RULES_CACHE = new Reference2LongOpenHashMap<>();
	private static final Reference2LongOpenHashMap<Biome> BIOME_RULES_CACHE = new Reference2LongOpenHashMap<>();
	// The empty tile's column per dimension settings, for the rule set above (antique-atlas#77, #249)
	private static final Reference2IntOpenHashMap<DimensionSettings> EMPTY_COLUMN_CACHE = new Reference2IntOpenHashMap<>();
	private static FeatureRuleSet cachedRules;
	private static final int RAVINE_DEPTH = 7;

	private static final int IDX_RAVINE = CUSTOM_TILES.indexOf(FeatureTiles.TILE_RAVINE);
	private static final int IDX_LAVA_SHORE = CUSTOM_TILES.indexOf(FeatureTiles.TILE_LAVA_SHORE);
	private static final TileElevation[] ELEVATIONS = TileElevation.values();

	static {
		PRIORITY_CACHE.defaultReturnValue(-1);
		EMPTY_COLUMN_CACHE.defaultReturnValue(-1);
		BLOCK_RULES_CACHE.defaultReturnValue(-1L);
		BIOME_RULES_CACHE.defaultReturnValue(-1L);
	}

	public static void clearCaches() {
		PRIORITY_CACHE.clear();
		BLOCK_RULES_CACHE.clear();
		BIOME_RULES_CACHE.clear();
		EMPTY_COLUMN_CACHE.clear();
		cachedRules = null;
	}

	/**
	 * The current feature rules; drops the cached masks when a reload replaced them.
	 */
	private static FeatureRuleSet featureRules() {
		FeatureRuleSet rules = FeatureRules.getInstance().rules();
		if (rules != cachedRules) {
			BLOCK_RULES_CACHE.clear();
			BIOME_RULES_CACHE.clear();
			EMPTY_COLUMN_CACHE.clear();
			cachedRules = rules;
		}
		return rules;
	}

	private static long blockRules(FeatureRuleSet rules, Block block) {
		long mask = BLOCK_RULES_CACHE.getLong(block);
		if (mask == -1L) {
			Holder<Block> holder = block.builtInRegistryHolder();
			mask = rules.blockMask(BuiltInRegistries.BLOCK.getKey(block), tag -> holder.is(TagKey.create(Registries.BLOCK, tag)));
			BLOCK_RULES_CACHE.put(block, mask);
		}
		return mask;
	}

	private static long biomeRules(FeatureRuleSet rules, Registry<Biome> biomeRegistry, Biome biome) {
		long mask = BIOME_RULES_CACHE.getLong(biome);
		if (mask == -1L) {
			Holder<Biome> holder = biomeRegistry.wrapAsHolder(biome);
			mask = rules.biomeMask(biomeRegistry.getKey(biome), tag -> holder.is(TagKey.create(Registries.BIOME, tag)));
			BIOME_RULES_CACHE.put(biome, mask);
		}
		return mask;
	}

	/**
	 * @return the custom tile column of the dimension's empty tile; one past the rule set's tiles if it isn't one of them.
	 */
	private static int emptyColumn(FeatureRuleSet rules, DimensionSettings settings) {
		int column = EMPTY_COLUMN_CACHE.getInt(settings);
		if (column < 0) {
			column = rules.customTileIndex(settings.emptyTile());
			EMPTY_COLUMN_CACHE.put(settings, column);
		}
		return column;
	}

	public static int priorityForBiome(Registry<Biome> biomeRegistry, Biome biome) {
		int priority = PRIORITY_CACHE.getInt(biome);
		if (priority < 0) {
			Holder<Biome> biomeEntry = biomeRegistry.wrapAsHolder(biome);
			priority = biomeEntry.is(BiomeTags.IS_BEACH) ? BEACH_PRIORITY : biomeEntry.is(BiomeTags.IS_NETHER) ? 2 : 1;
			PRIORITY_CACHE.put(biome, priority);
		}
		return priority;
	}

	private static boolean nullProviderLogged = false;

	/**
	 * @param possibleTiles votes per elevation and column: the biome palette, then the rule set's custom tiles, then one
	 *                      column for a dimension empty tile that isn't a custom tile.
	 */
	public static Pair<TerrainTileProvider, TileElevation> frequencyToTexture(int[][] possibleTiles, FeatureRuleSet rules, DimensionSettings settings, Registry<Biome> biomeRegistry, IdMap<Biome> biomePalette) {
		int elevationOrdinal = -1;
		int biomeIndex = -1;
		int bestFrequency = 0;
		for (int i = 0; i < possibleTiles.length; i++) {
			for (int j = 0; j < possibleTiles[i].length; j++) {
				if (possibleTiles[i][j] > bestFrequency) {
					elevationOrdinal = i;
					biomeIndex = j;
					bestFrequency = possibleTiles[i][j];
				}
			}
		}
		if (bestFrequency == 0) return null;
		int customTileIndex = biomeIndex - possibleTiles[0].length + rules.customTileCount() + 1;
		ResourceLocation providerId = customTileIndex >= rules.customTileCount() ? settings.emptyTile() : customTileIndex >= 0 ? rules.customTile(customTileIndex) : biomeRegistry.getKey(biomePalette.byId(biomeIndex));
		if (providerId == null) { // Fix: threw from the client tick, every tick, for the same chunk
			if (!nullProviderLogged) AntiqueAtlas.LOGGER.error("[Antique Atlas] " + (customTileIndex >= 0 ? "Custom tile index %s was out of bounds for size %s!".formatted(customTileIndex, rules.customTileCount()) : "Biome ID was null at index %s and instance %S!".formatted(biomeIndex, biomePalette.byId(biomeIndex))) + " Leaving such chunks undrawn.");
			nullProviderLogged = true;
			return null;
		}
		return Pair.of(BiomeTileProviders.getInstance().getTileProvider(providerId), elevationOrdinal == ELEVATIONS.length ? null : ELEVATIONS[elevationOrdinal]);
	}

	public static Pair<TerrainTileProvider, TileElevation> terrainToTile(WorldSummary summary, ChunkPos pos, DimensionSettings settings) {
		boolean checkRavines = settings.ravines();
		int seaLevel = settings.seaLevel();

		int topY = 999;

		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return null;
		ChunkSummary chunk = terrain.get(pos);
		if (chunk == null) return null; // Skip events fired for chunks we don't have yet (e.g. new shares)
		@Nullable LayerSummary.Raw lithograph = chunk.toSingleLayer(null, settings.scanTopOrNull(), topY);
		RegistryPalette<Biome>.ValueView biomePalette = terrain.getBiomePalette(pos);
		RegistryPalette<Block>.ValueView blockPalette = terrain.getBlockPalette(pos);
		Registry<Biome> biomeRegistry = biomePalette.registry(); // 1.21: ensures server registry is used in singleplayer
		if (lithograph == null) return Pair.of(BiomeTileProviders.getInstance().getTileProvider(settings.emptyTile()), null);

		FeatureRuleSet rules = featureRules();
		int defaultTile = emptyColumn(rules, settings);
		int elevationSize = ELEVATIONS.length;
		int elevationCount = elevationSize + 1;
		int biomeCount = biomePalette.size();
		int baseTileCount = biomeCount + rules.customTileCount() + 1;
		int[][] possibleTiles = new int[elevationCount][baseTileCount];

		for (int i = 0; i < lithograph.depths().length; i++) {
			if (!lithograph.exists().get(i)) {
				possibleTiles[elevationSize][biomeCount + defaultTile] += EMPTY_PRIORITY; // Fix: without biomeCount this voted for a biome
				continue;
			}
			int height = topY - lithograph.depths()[i] + lithograph.waterDepths()[i];
			Block block = blockPalette.byId(lithograph.blocks()[i]);
			Biome biome = biomePalette.byId(lithograph.biomes()[i]);

			if (checkRavines && height - seaLevel < -RAVINE_DEPTH) {
				possibleTiles[elevationSize][biomeCount + IDX_RAVINE] += RAVINE_PRIORITY;
			} else {
				// Water, ice, lava...: see atlas/features/*.json (antique-atlas#318)
				int rule = rules.match(lithograph.waterDepths()[i] > 0, blockRules(rules, block), biomeRules(rules, biomeRegistry, biome));
				if (rule >= 0) possibleTiles[elevationSize][biomeCount + rules.column(rule)] += rules.priority(rule);
			}
			if (lithograph.biomes()[i] < biomeCount) possibleTiles[TileElevation.fromBlocksAboveSea(height - seaLevel).ordinal()][lithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome); // Fix: an index past the palette threw every tick
		}

		return frequencyToTexture(possibleTiles, rules, settings, biomeRegistry, biomePalette);
	}

	public static Pair<TerrainTileProvider, TileElevation> terrainToTileNether(WorldSummary summary, ChunkPos pos, DimensionSettings settings) {
		int topY = 999;

		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return null;
		ChunkSummary chunk = terrain.get(pos);
		if (chunk == null) return null; // Skip events fired for chunks we don't have yet (e.g. new shares)
		@Nullable LayerSummary.Raw lowLithograph = chunk.toSingleLayer(null, settings.floorScanTop(), topY);
		@Nullable LayerSummary.Raw fullLithograph = chunk.toSingleLayer(null, settings.scanTopOrNull(), topY);
		RegistryPalette<Biome>.ValueView biomePalette = terrain.getBiomePalette(pos);
		RegistryPalette<Block>.ValueView blockPalette = terrain.getBlockPalette(pos);
		Registry<Biome> biomeRegistry = biomePalette.registry(); // 1.21: ensures server registry is used in singleplayer

		if (fullLithograph == null) {
			return Pair.of(BiomeTileProviders.getInstance().getTileProvider(settings.emptyTile()), null);
		}

		FeatureRuleSet rules = featureRules();
		int defaultTile = emptyColumn(rules, settings);
		int elevationSize = ELEVATIONS.length;
		int elevationCount = elevationSize + 1;
		int biomeCount = biomePalette.size();
		int baseTileCount = biomeCount + rules.customTileCount() + 1;
		int[][] possibleTiles = new int[elevationCount][baseTileCount];

		int SEA_DEPTH = topY - settings.seaLevel();

		if (lowLithograph == null) {
			for (int i = 0; i < fullLithograph.depths().length; i++) {
				if (!fullLithograph.exists().get(i)) {
					possibleTiles[elevationSize][biomeCount + defaultTile] += EMPTY_PRIORITY; // Fix: without biomeCount this voted for a biome
				} else {
					Biome biome = biomePalette.byId(fullLithograph.biomes()[i]);
					if (fullLithograph.biomes()[i] < biomeCount) possibleTiles[elevationSize][fullLithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
				}
			}
		} else {
			for (int i = 0; i < lowLithograph.depths().length; i++) {
				if (!lowLithograph.exists().get(i) || lowLithograph.depths()[i] > SEA_DEPTH) {
					Biome biome = biomePalette.byId(fullLithograph.biomes()[i]);
					if (fullLithograph.biomes()[i] < biomeCount) possibleTiles[elevationSize][fullLithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
				} else {
					Block block = blockPalette.byId(lowLithograph.blocks()[i]);
					Biome biome = biomePalette.byId(lowLithograph.biomes()[i]);
					int rule = rules.match(false, blockRules(rules, block), biomeRules(rules, biomeRegistry, biome));
					if (rule >= 0) { // Lava Sea (or another feature rule)
						possibleTiles[elevationSize][biomeCount + rules.column(rule)] += rules.priority(rule);
					} else { // Low Floor
						possibleTiles[elevationSize][biomeCount + IDX_LAVA_SHORE] += BEACH_PRIORITY;
					}
				}
			}
		}

		return frequencyToTexture(possibleTiles, rules, settings, biomeRegistry, biomePalette);
	}
}
