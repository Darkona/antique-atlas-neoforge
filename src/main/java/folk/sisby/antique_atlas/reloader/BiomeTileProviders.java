package folk.sisby.antique_atlas.reloader;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasConfig;
import folk.sisby.antique_atlas.TerrainTileProvider;
import folk.sisby.antique_atlas.TileElevation;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.util.ForgeTags;
import folk.sisby.antique_atlas.util.ConventionalBiomeTagsV1;
import folk.sisby.antique_atlas.util.ConventionalBiomeTagsV2;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BiomeTileProviders extends SimpleJsonResourceReloadListener {
	public static final BiomeTileProviders INSTANCE = new BiomeTileProviders();
	public static final ResourceLocation ID = AntiqueAtlas.id("tile_provider/biome");

	public static BiomeTileProviders getInstance() {
		return INSTANCE;
	}

	protected final Map<ResourceLocation, TerrainTileProvider> tileProviders = new HashMap<>();
	protected final Map<ResourceLocation, ResourceLocation> biomeFallbacks = new HashMap<>();
	protected boolean hasFallbacks = false;

	public BiomeTileProviders() {
		super(new Gson(), "atlas/biome");
	}

	public TerrainTileProvider getTileProvider(ResourceLocation providerId) {
		TerrainTileProvider provider = tileProviders.get(providerId);
		if (provider != null) return provider;
		ResourceLocation fallbackId = biomeFallbacks.get(providerId);
		provider = fallbackId == null ? null : tileProviders.get(fallbackId);
		if (provider != null) return provider;
		if (AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.PLAINS && !providerId.equals(Biomes.PLAINS.location())) return getTileProvider(Biomes.PLAINS.location());
		return TerrainTileProvider.fallback();
	}

	/**
	 * Register fallbacks for any biomes present in the client world that don't have explicit sets.
	 * Doing this on world join catches data-biomes that might not be registered in other worlds.
	 */
	public void registerFallbacks(Registry<Biome> biomeRegistry) {
		for (Biome biome : biomeRegistry) {
			ResourceLocation biomeId = biomeRegistry.getKey(biome);
			if (tileProviders.containsKey(biomeId)) continue;
			ResourceLocation fallbackBiome = getFallbackBiome(biomeRegistry.wrapAsHolder(biome));
			if (fallbackBiome != null && tileProviders.containsKey(fallbackBiome)) {
				biomeFallbacks.put(biomeId, fallbackBiome);
				AntiqueAtlas.LOGGER.info("[Antique Atlas] Set fallback biome for {} to {}. You can set a more fitting texture using a resource pack!", biomeId, fallbackBiome);
			} else if (fallbackBiome != null) {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Fallback biome for {} is {}, which has no defined tile provider.", biomeId, fallbackBiome);
			} else {
				AntiqueAtlas.LOGGER.warn("[Antique Atlas] No fallback could be found for {}. This shouldn't happen! This means the biome is not in ANY conventional or vanilla tag on the client!", biomeId);
				if (AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.CRASH) throw new IllegalStateException("Antique Atlas fallback biome registration failed! Fix the missing biome or change fallbackFailHandling in antique_atlas.toml");
			}
		}
		hasFallbacks = true;
	}

	public void clearFallbacks() {
		hasFallbacks = false;
		biomeFallbacks.clear();
	}

	public boolean hasFallbacks() {
		return hasFallbacks;
	}

	public static ResourceLocation getFallbackBiome(Holder<Biome> biome) {
		if (biome.is(ConventionalBiomeTagsV2.IS_VOID) || biome.is(ConventionalBiomeTagsV1.VOID) || biome.is(ForgeTags.Biomes.IS_VOID)) {
			return Biomes.THE_VOID.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_END) || biome.is(BiomeTags.IS_END) || biome.is(ConventionalBiomeTagsV1.IN_THE_END) || biome.is(ConventionalBiomeTagsV1.END_ISLANDS)) {
			if (biome.is(ConventionalBiomeTagsV2.IS_VEGETATION_SPARSE) || biome.is(ConventionalBiomeTagsV2.IS_VEGETATION_DENSE) || biome.is(ConventionalBiomeTagsV1.VEGETATION_DENSE) || biome.is(ConventionalBiomeTagsV1.VEGETATION_SPARSE) || biome.is(ForgeTags.Biomes.IS_LUSH)) return Biomes.END_HIGHLANDS.location();
			return Biomes.END_BARRENS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_NETHER_FOREST) || biome.is(ConventionalBiomeTagsV1.NETHER_FORESTS)) {
			return Biomes.WARPED_FOREST.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_NETHER) || biome.is(BiomeTags.IS_NETHER) || biome.is(ConventionalBiomeTagsV1.IN_NETHER)) {
			return Biomes.SOUL_SAND_VALLEY.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_SWAMP) || biome.is(ConventionalBiomeTagsV1.SWAMP) || biome.is(ForgeTags.Biomes.IS_SWAMP)) {
			return Biomes.SWAMP.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_OCEAN) || biome.is(ConventionalBiomeTagsV2.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(ConventionalBiomeTagsV1.DEEP_OCEAN) || biome.is(ConventionalBiomeTagsV1.OCEAN) || biome.is(ConventionalBiomeTagsV1.SHALLOW_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(ConventionalBiomeTagsV1.RIVER) || biome.is(ConventionalBiomeTagsV1.AQUATIC) || biome.is(ConventionalBiomeTagsV1.AQUATIC_ICY) || biome.is(ForgeTags.Biomes.IS_WATER)) {
			if (biome.is(ConventionalBiomeTagsV2.IS_AQUATIC_ICY) || biome.is(ConventionalBiomeTagsV2.IS_ICY) || biome.is(ConventionalBiomeTagsV1.ICY) || biome.is(ConventionalBiomeTagsV1.AQUATIC_ICY)) return Biomes.FROZEN_RIVER.location();
			return Biomes.RIVER.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_STONY_SHORES) || biome.is(ConventionalBiomeTagsV1.STONY_SHORES)) {
			return Biomes.STONY_SHORE.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_BEACH) || biome.is(BiomeTags.IS_BEACH) || biome.is(ConventionalBiomeTagsV1.BEACH)) {
			return Biomes.BEACH.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_JUNGLE_TREE) || biome.is(ConventionalBiomeTagsV2.IS_JUNGLE) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(ConventionalBiomeTagsV1.JUNGLE) || biome.is(ConventionalBiomeTagsV1.TREE_JUNGLE)) {
			return Biomes.JUNGLE.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_FLOWER_FOREST) || biome.is(ConventionalBiomeTagsV2.IS_FLORAL) || biome.is(ConventionalBiomeTagsV1.FLOWER_FORESTS) || biome.is(ConventionalBiomeTagsV1.FLORAL)) {
			return Biomes.FLOWER_FOREST.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_SAVANNA_TREE) || biome.is(ConventionalBiomeTagsV2.IS_SAVANNA) || biome.is(BiomeTags.IS_SAVANNA) || biome.is(ConventionalBiomeTagsV1.SAVANNA) || biome.is(ConventionalBiomeTagsV1.TREE_SAVANNA)) {
			return Biomes.SAVANNA.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_BADLANDS) || biome.is(BiomeTags.IS_BADLANDS) || biome.is((ConventionalBiomeTagsV1.BADLANDS)) || biome.is((ConventionalBiomeTagsV1.MESA))) {
			return Biomes.BADLANDS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_CONIFEROUS_TREE) || biome.is(ConventionalBiomeTagsV2.IS_TAIGA) || biome.is(ConventionalBiomeTagsV1.TREE_CONIFEROUS) || biome.is(ForgeTags.Biomes.IS_CONIFEROUS) || biome.is(BiomeTags.IS_TAIGA) || biome.is(ConventionalBiomeTagsV1.TAIGA)) {
			if (biome.is(ConventionalBiomeTagsV2.IS_SNOWY) || biome.is(ConventionalBiomeTagsV2.IS_ICY) || biome.is(ConventionalBiomeTagsV1.ICY) || biome.is(ConventionalBiomeTagsV1.SNOWY)) return Biomes.SNOWY_TAIGA.location();
			return Biomes.TAIGA.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_OLD_GROWTH)) {
			return Biomes.BIRCH_FOREST.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_BIRCH_FOREST) || biome.is(ConventionalBiomeTagsV2.IS_DECIDUOUS_TREE) || biome.is(ConventionalBiomeTagsV1.BIRCH_FOREST) || biome.is(ConventionalBiomeTagsV1.TREE_DECIDUOUS)) {
			return Biomes.BIRCH_FOREST.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_FOREST) || biome.is(BiomeTags.IS_FOREST) || biome.is(ConventionalBiomeTagsV1.FOREST)) {
			return Biomes.FOREST.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_SNOWY_PLAINS) || biome.is(ConventionalBiomeTagsV2.IS_PLAINS) || biome.is(ConventionalBiomeTagsV1.PLAINS) || biome.is(ConventionalBiomeTagsV1.SNOWY_PLAINS) || biome.is(ForgeTags.Biomes.IS_PLAINS) || biome.is(ConventionalBiomeTagsV1.SNOWY) || biome.is(ForgeTags.Biomes.IS_SNOWY)) {
			if (biome.is(ConventionalBiomeTagsV2.IS_ICY) || biome.is(ConventionalBiomeTagsV2.IS_SNOWY_PLAINS) || biome.is(ConventionalBiomeTagsV2.IS_SNOWY) || biome.is(ConventionalBiomeTagsV1.SNOWY_PLAINS) || biome.is(ConventionalBiomeTagsV1.ICY) || biome.is(ConventionalBiomeTagsV1.SNOWY)) return Biomes.SNOWY_PLAINS.location();
			return Biomes.PLAINS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_WASTELAND) || biome.is(ConventionalBiomeTagsV2.IS_DEAD) || biome.is(ConventionalBiomeTagsV2.IS_DESERT) || biome.is(ConventionalBiomeTagsV1.DESERT) || biome.is(ConventionalBiomeTagsV1.WASTELAND) || biome.is(ConventionalBiomeTagsV1.DEAD) || biome.is(ForgeTags.Biomes.IS_SANDY) || biome.is(ForgeTags.Biomes.IS_DESERT) || biome.is(ForgeTags.Biomes.IS_DEAD) || biome.is(ForgeTags.Biomes.IS_WASTELAND)) {
			return Biomes.DESERT.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_ICY) || biome.is(ConventionalBiomeTagsV1.ICY)) {
			return Biomes.FROZEN_OCEAN.location();
		} else if (biome.is(ForgeTags.Biomes.IS_PLATEAU)) {
			return Biomes.MEADOW.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_WINDSWEPT) || biome.is(ConventionalBiomeTagsV1.EXTREME_HILLS) || biome.is(ConventionalBiomeTagsV1.WINDSWEPT)) {
			return Biomes.WINDSWEPT_HILLS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_MOUNTAIN_PEAK) || biome.is(ConventionalBiomeTagsV1.MOUNTAIN_PEAK) || biome.is(ForgeTags.Biomes.IS_PEAK)) {
			return Biomes.JAGGED_PEAKS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_MOUNTAIN_SLOPE) || biome.is(ConventionalBiomeTagsV2.IS_MOUNTAIN) || biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(ConventionalBiomeTagsV1.MOUNTAIN) || biome.is(ConventionalBiomeTagsV1.MOUNTAIN_SLOPE) || biome.is(ForgeTags.Biomes.IS_SLOPE) || biome.is(ForgeTags.Biomes.IS_MOUNTAIN)) {
			return Biomes.STONY_PEAKS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_MUSHROOM) || biome.is(ConventionalBiomeTagsV1.MUSHROOM) || biome.is(ForgeTags.Biomes.IS_MUSHROOM)) {
			return Biomes.MUSHROOM_FIELDS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_HILL) || biome.is(BiomeTags.IS_HILL)) {
			return Biomes.WINDSWEPT_GRAVELLY_HILLS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_UNDERGROUND) || biome.is(ConventionalBiomeTagsV2.IS_CAVE) || biome.is(ConventionalBiomeTagsV1.CAVES) || biome.is(ConventionalBiomeTagsV1.UNDERGROUND) || biome.is(ForgeTags.Biomes.IS_UNDERGROUND) || biome.is(ForgeTags.Biomes.IS_CAVE)) {
			return Biomes.DRIPSTONE_CAVES.location();
		} else if (biome.is(ForgeTags.Biomes.IS_SPOOKY)) {
			return Biomes.DARK_FOREST.location();
		} else if (biome.is(ForgeTags.Biomes.IS_MAGICAL)) {
			return Biomes.MUSHROOM_FIELDS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_VEGETATION_DENSE) || biome.is(ConventionalBiomeTagsV1.VEGETATION_DENSE) || biome.is(ForgeTags.Biomes.IS_DENSE)) {
			return Biomes.FOREST.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_VEGETATION_SPARSE) || biome.is(ConventionalBiomeTagsV1.VEGETATION_SPARSE) || biome.is(ForgeTags.Biomes.IS_SPARSE)) {
			return Biomes.PLAINS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_HOT) || biome.is(ConventionalBiomeTagsV1.CLIMATE_HOT) || biome.is(ForgeTags.Biomes.IS_HOT)) {
			return Biomes.DESERT.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_COLD) || biome.is(ConventionalBiomeTagsV1.CLIMATE_COLD) || biome.is(ForgeTags.Biomes.IS_COLD)) {
			return Biomes.SNOWY_PLAINS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_TEMPERATE) || biome.is(ConventionalBiomeTagsV1.CLIMATE_TEMPERATE)) {
			return Biomes.PLAINS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_DRY) || biome.is(ConventionalBiomeTagsV1.CLIMATE_DRY) || biome.is(ForgeTags.Biomes.IS_DRY)) {
			return Biomes.BADLANDS.location();
		} else if (biome.is(ConventionalBiomeTagsV2.IS_WET) || biome.is(ConventionalBiomeTagsV1.CLIMATE_WET) || biome.is(ForgeTags.Biomes.IS_WET)) {
			return Biomes.SWAMP.location();
		}
		return null;
	}

	public static TileTexture getTexture(Map<ResourceLocation, TileTexture> textures, ResourceLocation id) {
		if (textures.containsKey(id)) {
			return textures.get(id);
		} else {
			throw new IllegalStateException("texture %s is not present!".formatted(id));
		}
	}

	public static @Nullable List<TileTexture> resolveTextureJson(Map<ResourceLocation, TileTexture> textures, JsonElement textureJson) {
		if (textureJson instanceof JsonPrimitive texturePrimitive && texturePrimitive.isString()) {
			return List.of(getTexture(textures, ResourceLocation.tryParse(texturePrimitive.getAsString())));
		} else if (textureJson instanceof JsonArray textureArray) {
			return textureArray.asList().stream().map(je -> getTexture(textures, ResourceLocation.tryParse(je.getAsString()))).toList();
		} else if (textureJson instanceof JsonObject textureObject && textureObject.keySet().stream().allMatch(k -> textureObject.get(k) instanceof JsonPrimitive jp && jp.isNumber())) {
			Multiset<TileTexture> outList = HashMultiset.create();
			textureObject.entrySet().forEach(e -> outList.add(getTexture(textures, ResourceLocation.tryParse(e.getKey())), e.getValue().getAsInt()));
			return outList.stream().toList();
		}
		return null;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Biome Tile Providers...");
		tileProviders.clear(); // Fix: providers removed from a resource pack stayed after a reload
		Map<ResourceLocation, TileTexture> textures = TileTextures.getInstance().getTextures();
		Set<TileTexture> unusedTextures = new HashSet<>(textures.values().stream().filter(t -> t.id().getPath().startsWith("biome")).toList());
		Map<ResourceLocation, ResourceLocation> providerParents = new HashMap<>();
		for (Map.Entry<ResourceLocation, JsonElement> fileEntry : prepared.entrySet()) {
			ResourceLocation fileId = fileEntry.getKey();
			try {
				JsonObject fileJson = fileEntry.getValue().getAsJsonObject();
				if (fileJson.has("parent")) {
					ResourceLocation parentId = ResourceLocation.tryParse(fileJson.getAsJsonPrimitive("parent").getAsString());
					providerParents.put(fileId, parentId);
					continue;
				}
				JsonElement textureJson = fileJson.get("textures");
				List<TileTexture> defaultTextures = resolveTextureJson(textures, textureJson);
				if (defaultTextures != null) {
					defaultTextures.forEach(unusedTextures::remove);
					tileProviders.put(fileId, new TerrainTileProvider(fileId, defaultTextures));
				} else {
					JsonObject textureObject = textureJson.getAsJsonObject();
					Map<TileElevation, List<TileTexture>> textureElevations = new HashMap<>();
					Set<TileElevation> skippedElevations = new HashSet<>();
					List<TileTexture> elevationTextures = null;
					for (TileElevation elevation : TileElevation.values()) {
						if (textureObject.has(elevation.getName())) {
							elevationTextures = resolveTextureJson(textures, textureObject.get(elevation.getName()));
							if (elevationTextures == null) throw new IllegalStateException("Malformed object %s in textures object!".formatted(elevation.getName()));
							elevationTextures.forEach(unusedTextures::remove);
							textureElevations.put(elevation, elevationTextures);
							for (TileElevation skipped : skippedElevations) {
								textureElevations.put(skipped, elevationTextures);
							}
							skippedElevations.clear();
						} else {
							skippedElevations.add(elevation);
						}
					}
					if (textureElevations.isEmpty()) {
						throw new IllegalStateException("No elevation keys were found in the textures object!");
					}
					for (TileElevation elevation : skippedElevations) {
						textureElevations.put(elevation, elevationTextures);
					}
					tileProviders.put(fileId, new TerrainTileProvider(fileId, textureElevations));
				}
			} catch (Exception e) {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading biome tile provider {}!", fileId, e);
			}
		}
		providerParents.forEach((id, parentId) -> {
			if (tileProviders.containsKey(parentId)) {
				tileProviders.put(id, tileProviders.get(parentId));
			} else {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading biome tile provider {}!", id, new IllegalStateException("Parent id %s doesn't exist".formatted(parentId)));
			}
		});

		for (TileTexture texture : unusedTextures) {
			if (texture.displayId().startsWith("test") || texture.displayId().startsWith("base")) continue;
			AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} isn't referenced by any biome tile provider!", texture.displayId());
		}
	}


}
