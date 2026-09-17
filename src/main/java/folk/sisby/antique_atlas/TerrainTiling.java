package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrain;
import folk.sisby.surveyor.util.RegistryPalette;
import it.unimi.dsi.fastutil.Pair;
import folk.sisby.antique_atlas.util.ConventionalBiomeTagsV1;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;

/**
 * Hottest class in the mod. Might get ugly.
 */
public class TerrainTiling {
	public static final int EMPTY_PRIORITY = 16;
	public static final int RAVINE_PRIORITY = 12;
	public static final int LAVA_PRIORITY = 6;
	public static final int WATER_PRIORITY = 4;
	public static final int ICE_PRIORITY = 3;
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

	public static final int NETHER_SCAN_HEIGHT = 50;
	private static final Reference2IntOpenHashMap<Biome> PRIORITY_CACHE = new Reference2IntOpenHashMap<>();
	private static final Reference2ByteOpenHashMap<Biome> SWAMP_CACHE = new Reference2ByteOpenHashMap<>();
	private static final byte UNKNOWN = 0, NOT_SWAMP = 1, SWAMP = 2;
	private static final int SEA_LEVEL = 63;

	private static final int IDX_BEDROCK_ROOF = CUSTOM_TILES.indexOf(FeatureTiles.BEDROCK_ROOF);
	private static final int IDX_EMPTY = CUSTOM_TILES.indexOf(FeatureTiles.EMPTY);
	private static final int IDX_END_VOID = CUSTOM_TILES.indexOf(FeatureTiles.END_VOID);
	private static final int IDX_WATER = CUSTOM_TILES.indexOf(FeatureTiles.WATER);
	private static final int IDX_ICE = CUSTOM_TILES.indexOf(FeatureTiles.ICE);
	private static final int IDX_RAVINE = CUSTOM_TILES.indexOf(FeatureTiles.TILE_RAVINE);
	private static final int IDX_SWAMP_WATER = CUSTOM_TILES.indexOf(FeatureTiles.SWAMP_WATER);
	private static final int IDX_LAVA = CUSTOM_TILES.indexOf(FeatureTiles.TILE_LAVA);
	private static final int IDX_LAVA_SHORE = CUSTOM_TILES.indexOf(FeatureTiles.TILE_LAVA_SHORE);
	private static final TileElevation[] ELEVATIONS = TileElevation.values();

	static {
		PRIORITY_CACHE.defaultReturnValue(-1);
	}

	public static void clearCaches() {
		PRIORITY_CACHE.clear();
		SWAMP_CACHE.clear();
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

	public static boolean isSwamp(Registry<Biome> biomeRegistry, Biome biome) {
		byte swamp = SWAMP_CACHE.getByte(biome);
		if (swamp == UNKNOWN) {
			swamp = biomeRegistry.wrapAsHolder(biome).is(ConventionalBiomeTagsV1.SWAMP) ? SWAMP : NOT_SWAMP;
			SWAMP_CACHE.put(biome, swamp);
		}
		return swamp == SWAMP;
	}

	public static Pair<TerrainTileProvider, TileElevation> frequencyToTexture(int[][] possibleTiles, Registry<Biome> biomeRegistry, IdMap<Biome> biomePalette) {
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
		int customTileIndex = biomeIndex - possibleTiles[0].length + CUSTOM_TILES.size();
		ResourceLocation providerId = customTileIndex >= 0 ? CUSTOM_TILES.get(customTileIndex) : biomeRegistry.getKey(biomePalette.byId(biomeIndex));
		if (providerId == null) {
			throw new RuntimeException(customTileIndex >= 0 ? "Custom tile index %s was out of bounds for size %s!".formatted(customTileIndex, CUSTOM_TILES.size()) : "Biome ID was null at index %s and instance %S!".formatted(biomeIndex, biomePalette.byId(biomeIndex)));
		}
		return Pair.of(BiomeTileProviders.getInstance().getTileProvider(providerId), elevationOrdinal == ELEVATIONS.length ? null : ELEVATIONS[elevationOrdinal]);
	}

	public static Pair<TerrainTileProvider, TileElevation> terrainToTile(WorldSummary summary, ChunkPos pos) {
		int defaultTile = summary.dimension() == Level.END ? IDX_END_VOID : IDX_EMPTY;
		boolean checkRavines = summary.dimension() == Level.OVERWORLD;

		int topY = 999;

		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return null;
		ChunkSummary chunk = terrain.get(pos);
		if (chunk == null) return null; // Skip events fired for chunks we don't have yet (e.g. new shares)
		@Nullable LayerSummary.Raw lithograph = chunk.toSingleLayer(null, null, topY);
		RegistryPalette<Biome>.ValueView biomePalette = terrain.getBiomePalette(pos);
		RegistryPalette<Block>.ValueView blockPalette = terrain.getBlockPalette(pos);
		Registry<Biome> biomeRegistry = biomePalette.registry(); // 1.21: ensures server registry is used in singleplayer
		if (lithograph == null) return Pair.of(BiomeTileProviders.getInstance().getTileProvider(CUSTOM_TILES.get(defaultTile)), null);

		int elevationSize = ELEVATIONS.length;
		int elevationCount = elevationSize + 1;
		int biomeCount = biomePalette.size();
		int baseTileCount = biomeCount + CUSTOM_TILES.size();
		int[][] possibleTiles = new int[elevationCount][baseTileCount];

		for (int i = 0; i < lithograph.depths().length; i++) {
			if (!lithograph.exists().get(i)) {
				possibleTiles[elevationSize][defaultTile] += EMPTY_PRIORITY;
				continue;
			}
			int height = topY - lithograph.depths()[i] + lithograph.waterDepths()[i];
			Block block = blockPalette.byId(lithograph.blocks()[i]);
			Biome biome = biomePalette.byId(lithograph.biomes()[i]);

			if (checkRavines && height - SEA_LEVEL < -7) {
				possibleTiles[elevationSize][biomeCount + IDX_RAVINE] += RAVINE_PRIORITY;
			} else if (lithograph.waterDepths()[i] > 0) {
				possibleTiles[elevationSize][biomeCount + (isSwamp(biomeRegistry, biome) ? IDX_SWAMP_WATER : IDX_WATER)] += WATER_PRIORITY;
			} else if (block == Blocks.ICE) {
				possibleTiles[elevationSize][biomeCount + IDX_ICE] += ICE_PRIORITY;
			} else if (block == Blocks.LAVA) {
				possibleTiles[elevationSize][biomeCount + IDX_LAVA] += LAVA_PRIORITY;
			}
			possibleTiles[TileElevation.fromBlocksAboveSea(height - SEA_LEVEL).ordinal()][lithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
		}

		return frequencyToTexture(possibleTiles, biomeRegistry, biomePalette);
	}

	public static Pair<TerrainTileProvider, TileElevation> terrainToTileNether(WorldSummary summary, ChunkPos pos) {
		int defaultTile = IDX_BEDROCK_ROOF;

		int topY = 999;
		int logicalTopY = 126;

		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return null;
		ChunkSummary chunk = terrain.get(pos);
		if (chunk == null) return null; // Skip events fired for chunks we don't have yet (e.g. new shares)
		@Nullable LayerSummary.Raw lowLithograph = chunk.toSingleLayer(null, NETHER_SCAN_HEIGHT, topY);
		@Nullable LayerSummary.Raw fullLithograph = chunk.toSingleLayer(null, logicalTopY, topY);
		RegistryPalette<Biome>.ValueView biomePalette = terrain.getBiomePalette(pos);
		RegistryPalette<Block>.ValueView blockPalette = terrain.getBlockPalette(pos);
		Registry<Biome> biomeRegistry = biomePalette.registry(); // 1.21: ensures server registry is used in singleplayer

		int elevationSize = ELEVATIONS.length;
		int elevationCount = elevationSize + 1;
		int biomeCount = biomePalette.size();
		int baseTileCount = biomeCount + CUSTOM_TILES.size();
		int[][] possibleTiles = new int[elevationCount][baseTileCount];

		if (fullLithograph == null) {
			return Pair.of(BiomeTileProviders.getInstance().getTileProvider(CUSTOM_TILES.get(defaultTile)), null);
		}

		int SEA_DEPTH = topY - 31;

		if (lowLithograph == null) {
			for (int i = 0; i < fullLithograph.depths().length; i++) {
				if (!fullLithograph.exists().get(i)) {
					possibleTiles[elevationSize][defaultTile] += EMPTY_PRIORITY;
				} else {
					Biome biome = biomePalette.byId(fullLithograph.biomes()[i]);
					possibleTiles[elevationSize][fullLithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
				}
			}
		} else {
			for (int i = 0; i < lowLithograph.depths().length; i++) {
				if (!lowLithograph.exists().get(i) || lowLithograph.depths()[i] > SEA_DEPTH) {
					Biome biome = biomePalette.byId(fullLithograph.biomes()[i]);
					possibleTiles[elevationSize][fullLithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
				} else {
					Block block = blockPalette.byId(lowLithograph.blocks()[i]);
					if (block == Blocks.LAVA) { // Lava Sea
						possibleTiles[elevationSize][biomeCount + IDX_LAVA] += LAVA_PRIORITY;
					} else { // Low Floor
						possibleTiles[elevationSize][biomeCount + IDX_LAVA_SHORE] += BEACH_PRIORITY;
					}
				}
			}
		}

		return frequencyToTexture(possibleTiles, biomeRegistry, biomePalette);
	}
}
