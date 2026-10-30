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
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
		return getFallbackBiome(biome::is);
	}

	public static ResourceLocation getFallbackBiome(Predicate<TagKey<Biome>> biome) {
		if (biome.test(Tags.Biomes.IS_VOID)) {
			return Biomes.THE_VOID.location();
		} else if (biome.test(Tags.Biomes.IS_END) || biome.test(BiomeTags.IS_END)) {
			if (biome.test(Tags.Biomes.IS_SPARSE_VEGETATION) || biome.test(Tags.Biomes.IS_DENSE_VEGETATION)) return Biomes.END_HIGHLANDS.location();
			return Biomes.END_BARRENS.location();
		} else if (biome.test(Tags.Biomes.IS_NETHER_FOREST)) {
			return Biomes.WARPED_FOREST.location();
		} else if (biome.test(Tags.Biomes.IS_NETHER) || biome.test(BiomeTags.IS_NETHER)) {
			return Biomes.SOUL_SAND_VALLEY.location();
		} else if (biome.test(Tags.Biomes.IS_SWAMP)) {
			return Biomes.SWAMP.location();
		} else if (biome.test(Tags.Biomes.IS_OCEAN) || biome.test(Tags.Biomes.IS_RIVER) || biome.test(BiomeTags.IS_OCEAN) || biome.test(BiomeTags.IS_DEEP_OCEAN) || biome.test(BiomeTags.IS_RIVER)) {
			if (biome.test(Tags.Biomes.IS_AQUATIC_ICY) || biome.test(Tags.Biomes.IS_ICY)) return Biomes.FROZEN_RIVER.location();
			return Biomes.RIVER.location();
		} else if (biome.test(Tags.Biomes.IS_STONY_SHORES)) {
			return Biomes.STONY_SHORE.location();
		} else if (biome.test(Tags.Biomes.IS_BEACH) || biome.test(BiomeTags.IS_BEACH)) {
			return Biomes.BEACH.location();
		} else if (biome.test(Tags.Biomes.IS_JUNGLE_TREE) || biome.test(Tags.Biomes.IS_JUNGLE) || biome.test(BiomeTags.IS_JUNGLE)) {
			return Biomes.JUNGLE.location();
		} else if (biome.test(Tags.Biomes.IS_FLOWER_FOREST) || biome.test(Tags.Biomes.IS_FLORAL)) {
			return Biomes.FLOWER_FOREST.location();
		} else if (biome.test(Tags.Biomes.IS_SAVANNA_TREE) || biome.test(Tags.Biomes.IS_SAVANNA) || biome.test(BiomeTags.IS_SAVANNA)) {
			return Biomes.SAVANNA.location();
		} else if (biome.test(Tags.Biomes.IS_BADLANDS) || biome.test(BiomeTags.IS_BADLANDS)) {
			return Biomes.BADLANDS.location();
		} else if (biome.test(Tags.Biomes.IS_CONIFEROUS_TREE) || biome.test(Tags.Biomes.IS_TAIGA) || biome.test(BiomeTags.IS_TAIGA)) {
			if (biome.test(Tags.Biomes.IS_SNOWY) || biome.test(Tags.Biomes.IS_ICY)) return Biomes.SNOWY_TAIGA.location();
			return Biomes.TAIGA.location();
		} else if (biome.test(Tags.Biomes.IS_OLD_GROWTH)) {
			return Biomes.BIRCH_FOREST.location();
		} else if (biome.test(Tags.Biomes.IS_BIRCH_FOREST) || biome.test(Tags.Biomes.IS_DECIDUOUS_TREE)) {
			return Biomes.BIRCH_FOREST.location();
		} else if (biome.test(Tags.Biomes.IS_FOREST) || biome.test(BiomeTags.IS_FOREST)) {
			return Biomes.FOREST.location();
		} else if (biome.test(Tags.Biomes.IS_SNOWY_PLAINS) || biome.test(Tags.Biomes.IS_PLAINS)) {
			if (biome.test(Tags.Biomes.IS_ICY) || biome.test(Tags.Biomes.IS_SNOWY_PLAINS) || biome.test(Tags.Biomes.IS_SNOWY)) return Biomes.SNOWY_PLAINS.location();
			return Biomes.PLAINS.location();
		} else if (biome.test(Tags.Biomes.IS_WASTELAND) || biome.test(Tags.Biomes.IS_DEAD) || biome.test(Tags.Biomes.IS_DESERT)) {
			return Biomes.DESERT.location();
		} else if (biome.test(Tags.Biomes.IS_ICY)) {
			return Biomes.FROZEN_OCEAN.location();
		} else if (biome.test(Tags.Biomes.IS_WINDSWEPT)) {
			return Biomes.WINDSWEPT_HILLS.location();
		} else if (biome.test(Tags.Biomes.IS_MOUNTAIN_PEAK)) {
			return Biomes.JAGGED_PEAKS.location();
		} else if (biome.test(Tags.Biomes.IS_MOUNTAIN_SLOPE) || biome.test(Tags.Biomes.IS_MOUNTAIN) || biome.test(BiomeTags.IS_MOUNTAIN)) {
			return Biomes.STONY_PEAKS.location();
		} else if (biome.test(Tags.Biomes.IS_MUSHROOM)) {
			return Biomes.MUSHROOM_FIELDS.location();
		} else if (biome.test(Tags.Biomes.IS_HILL) || biome.test(BiomeTags.IS_HILL)) {
			return Biomes.WINDSWEPT_GRAVELLY_HILLS.location();
		} else if (biome.test(Tags.Biomes.IS_UNDERGROUND) || biome.test(Tags.Biomes.IS_CAVE)) {
			return Biomes.DRIPSTONE_CAVES.location();
		} else if (biome.test(Tags.Biomes.IS_DENSE_VEGETATION)) {
			return Biomes.FOREST.location();
		} else if (biome.test(Tags.Biomes.IS_SPARSE_VEGETATION)) {
			return Biomes.PLAINS.location();
		} else if (biome.test(Tags.Biomes.IS_HOT)) {
			return Biomes.DESERT.location();
		} else if (biome.test(Tags.Biomes.IS_COLD)) {
			return Biomes.SNOWY_PLAINS.location();
		} else if (biome.test(Tags.Biomes.IS_TEMPERATE)) {
			return Biomes.PLAINS.location();
		} else if (biome.test(Tags.Biomes.IS_DRY)) {
			return Biomes.BADLANDS.location();
		} else if (biome.test(Tags.Biomes.IS_WET)) {
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
		List<TileTexture> list = null;
		if (textureJson instanceof JsonPrimitive texturePrimitive && texturePrimitive.isString()) {
			list = List.of(getTexture(textures, ResourceLocation.tryParse(texturePrimitive.getAsString())));
		} else if (textureJson instanceof JsonArray textureArray) {
			list = textureArray.asList().stream().map(je -> getTexture(textures, ResourceLocation.tryParse(je.getAsString()))).toList();
		} else if (textureJson instanceof JsonObject textureObject && textureObject.keySet().stream().allMatch(k -> textureObject.get(k) instanceof JsonPrimitive jp && jp.isNumber())) {
			Multiset<TileTexture> outList = HashMultiset.create();
			textureObject.entrySet().forEach(e -> outList.add(getTexture(textures, ResourceLocation.tryParse(e.getKey())), e.getValue().getAsInt()));
			list = outList.stream().toList();
		}
		// Fix: an empty list ("textures": [], {} or all weights 0) made the provider divide by zero on every chunk it drew.
		if (list != null && list.isEmpty()) throw new IllegalStateException("textures list is empty!");
		return list;
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
