package folk.sisby.antique_atlas.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class ConventionalBiomeTagsV1 {
	public static final TagKey<Biome> IN_OVERWORLD = tag("in_overworld");
	public static final TagKey<Biome> IN_THE_END = tag("in_the_end");
	public static final TagKey<Biome> IN_NETHER = tag("in_nether");
	public static final TagKey<Biome> TAIGA = tag("taiga");
	public static final TagKey<Biome> EXTREME_HILLS = tag("extreme_hills");
	public static final TagKey<Biome> WINDSWEPT = tag("windswept");
	public static final TagKey<Biome> JUNGLE = tag("jungle");
	public static final TagKey<Biome> MESA = tag("mesa");
	public static final TagKey<Biome> PLAINS = tag("plains");
	public static final TagKey<Biome> SAVANNA = tag("savanna");
	public static final TagKey<Biome> ICY = tag("icy");
	public static final TagKey<Biome> AQUATIC_ICY = tag("aquatic_icy");
	public static final TagKey<Biome> BEACH = tag("beach");
	public static final TagKey<Biome> FOREST = tag("forest");
	public static final TagKey<Biome> BIRCH_FOREST = tag("birch_forest");
	public static final TagKey<Biome> OCEAN = tag("ocean");
	public static final TagKey<Biome> DESERT = tag("desert");
	public static final TagKey<Biome> RIVER = tag("river");
	public static final TagKey<Biome> SWAMP = tag("swamp");
	public static final TagKey<Biome> MUSHROOM = tag("mushroom");
	public static final TagKey<Biome> UNDERGROUND = tag("underground");
	public static final TagKey<Biome> MOUNTAIN = tag("mountain");
	public static final TagKey<Biome> CLIMATE_HOT = tag("climate_hot");
	public static final TagKey<Biome> CLIMATE_TEMPERATE = tag("climate_temperate");
	public static final TagKey<Biome> CLIMATE_COLD = tag("climate_cold");
	public static final TagKey<Biome> CLIMATE_WET = tag("climate_wet");
	public static final TagKey<Biome> CLIMATE_DRY = tag("climate_dry");
	public static final TagKey<Biome> VEGETATION_SPARSE = tag("vegetation_sparse");
	public static final TagKey<Biome> VEGETATION_DENSE = tag("vegetation_dense");
	public static final TagKey<Biome> TREE_CONIFEROUS = tag("tree_coniferous");
	public static final TagKey<Biome> TREE_SAVANNA = tag("tree_savanna");
	public static final TagKey<Biome> TREE_JUNGLE = tag("tree_jungle");
	public static final TagKey<Biome> TREE_DECIDUOUS = tag("tree_deciduous");
	public static final TagKey<Biome> VOID = tag("void");
	public static final TagKey<Biome> MOUNTAIN_PEAK = tag("mountain_peak");
	public static final TagKey<Biome> MOUNTAIN_SLOPE = tag("mountain_slope");
	public static final TagKey<Biome> AQUATIC = tag("aquatic");
	public static final TagKey<Biome> WASTELAND = tag("wasteland");
	public static final TagKey<Biome> DEAD = tag("dead");
	public static final TagKey<Biome> FLORAL = tag("floral");
	public static final TagKey<Biome> SNOWY = tag("snowy");
	public static final TagKey<Biome> BADLANDS = tag("badlands");
	public static final TagKey<Biome> CAVES = tag("caves");
	public static final TagKey<Biome> END_ISLANDS = tag("end_islands");
	public static final TagKey<Biome> NETHER_FORESTS = tag("nether_forests");
	public static final TagKey<Biome> SNOWY_PLAINS = tag("snowy_plains");
	public static final TagKey<Biome> STONY_SHORES = tag("stony_shores");
	public static final TagKey<Biome> FLOWER_FORESTS = tag("flower_forests");
	public static final TagKey<Biome> DEEP_OCEAN = tag("deep_ocean");
	public static final TagKey<Biome> SHALLOW_OCEAN = tag("shallow_ocean");

	private ConventionalBiomeTagsV1() {
	}

	private static TagKey<Biome> tag(String path) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", path));
	}
}
