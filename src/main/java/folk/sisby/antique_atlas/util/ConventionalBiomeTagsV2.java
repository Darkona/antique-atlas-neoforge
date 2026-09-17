package folk.sisby.antique_atlas.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class ConventionalBiomeTagsV2 {
	public static final TagKey<Biome> NO_DEFAULT_MONSTERS = tag("no_default_monsters");
	public static final TagKey<Biome> HIDDEN_FROM_LOCATOR_SELECTION = tag("hidden_from_locator_selection");
	public static final TagKey<Biome> IS_VOID = tag("is_void");
	public static final TagKey<Biome> IS_OVERWORLD = tag("is_overworld");
	public static final TagKey<Biome> IS_HOT = tag("is_hot");
	public static final TagKey<Biome> IS_HOT_OVERWORLD = tag("is_hot/overworld");
	public static final TagKey<Biome> IS_HOT_NETHER = tag("is_hot/nether");
	public static final TagKey<Biome> IS_HOT_END = tag("is_hot/end");
	public static final TagKey<Biome> IS_TEMPERATE = tag("is_temperate");
	public static final TagKey<Biome> IS_TEMPERATE_OVERWORLD = tag("is_temperate/overworld");
	public static final TagKey<Biome> IS_TEMPERATE_NETHER = tag("is_temperate/nether");
	public static final TagKey<Biome> IS_TEMPERATE_END = tag("is_temperate/end");
	public static final TagKey<Biome> IS_COLD = tag("is_cold");
	public static final TagKey<Biome> IS_COLD_OVERWORLD = tag("is_cold/overworld");
	public static final TagKey<Biome> IS_COLD_NETHER = tag("is_cold/nether");
	public static final TagKey<Biome> IS_COLD_END = tag("is_cold/end");
	public static final TagKey<Biome> IS_WET = tag("is_wet");
	public static final TagKey<Biome> IS_WET_OVERWORLD = tag("is_wet/overworld");
	public static final TagKey<Biome> IS_WET_NETHER = tag("is_wet/nether");
	public static final TagKey<Biome> IS_WET_END = tag("is_wet/end");
	public static final TagKey<Biome> IS_DRY = tag("is_dry");
	public static final TagKey<Biome> IS_DRY_OVERWORLD = tag("is_dry/overworld");
	public static final TagKey<Biome> IS_DRY_NETHER = tag("is_dry/nether");
	public static final TagKey<Biome> IS_DRY_END = tag("is_dry/end");
	public static final TagKey<Biome> IS_VEGETATION_SPARSE = tag("is_sparse_vegetation");
	public static final TagKey<Biome> IS_VEGETATION_SPARSE_OVERWORLD = tag("is_sparse_vegetation/overworld");
	public static final TagKey<Biome> IS_VEGETATION_SPARSE_NETHER = tag("is_sparse_vegetation/nether");
	public static final TagKey<Biome> IS_VEGETATION_SPARSE_END = tag("is_sparse_vegetation/end");
	public static final TagKey<Biome> IS_VEGETATION_DENSE = tag("is_dense_vegetation");
	public static final TagKey<Biome> IS_VEGETATION_DENSE_OVERWORLD = tag("is_dense_vegetation/overworld");
	public static final TagKey<Biome> IS_VEGETATION_DENSE_NETHER = tag("is_dense_vegetation/nether");
	public static final TagKey<Biome> IS_VEGETATION_DENSE_END = tag("is_dense_vegetation/end");
	public static final TagKey<Biome> IS_CONIFEROUS_TREE = tag("is_tree/coniferous");
	public static final TagKey<Biome> IS_SAVANNA_TREE = tag("is_tree/savanna");
	public static final TagKey<Biome> IS_JUNGLE_TREE = tag("is_tree/jungle");
	public static final TagKey<Biome> IS_DECIDUOUS_TREE = tag("is_tree/deciduous");
	public static final TagKey<Biome> IS_MOUNTAIN = tag("is_mountain");
	public static final TagKey<Biome> IS_MOUNTAIN_PEAK = tag("is_mountain/peak");
	public static final TagKey<Biome> IS_MOUNTAIN_SLOPE = tag("is_mountain/slope");
	public static final TagKey<Biome> IS_PLAINS = tag("is_plains");
	public static final TagKey<Biome> IS_SNOWY_PLAINS = tag("is_snowy_plains");
	public static final TagKey<Biome> IS_FOREST = tag("is_forest");
	public static final TagKey<Biome> IS_BIRCH_FOREST = tag("is_birch_forest");
	public static final TagKey<Biome> IS_FLOWER_FOREST = tag("is_flower_forest");
	public static final TagKey<Biome> IS_TAIGA = tag("is_taiga");
	public static final TagKey<Biome> IS_OLD_GROWTH = tag("is_old_growth");
	public static final TagKey<Biome> IS_HILL = tag("is_hill");
	public static final TagKey<Biome> IS_WINDSWEPT = tag("is_windswept");
	public static final TagKey<Biome> IS_JUNGLE = tag("is_jungle");
	public static final TagKey<Biome> IS_SAVANNA = tag("is_savanna");
	public static final TagKey<Biome> IS_SWAMP = tag("is_swamp");
	public static final TagKey<Biome> IS_DESERT = tag("is_desert");
	public static final TagKey<Biome> IS_BADLANDS = tag("is_badlands");
	public static final TagKey<Biome> IS_BEACH = tag("is_beach");
	public static final TagKey<Biome> IS_STONY_SHORES = tag("is_stony_shores");
	public static final TagKey<Biome> IS_MUSHROOM = tag("is_mushroom");
	public static final TagKey<Biome> IS_RIVER = tag("is_river");
	public static final TagKey<Biome> IS_OCEAN = tag("is_ocean");
	public static final TagKey<Biome> IS_DEEP_OCEAN = tag("is_deep_ocean");
	public static final TagKey<Biome> IS_SHALLOW_OCEAN = tag("is_shallow_ocean");
	public static final TagKey<Biome> IS_UNDERGROUND = tag("is_underground");
	public static final TagKey<Biome> IS_CAVE = tag("is_cave");
	public static final TagKey<Biome> IS_WASTELAND = tag("is_wasteland");
	public static final TagKey<Biome> IS_DEAD = tag("is_dead");
	public static final TagKey<Biome> IS_LUSH = tag("is_lush");
	public static final TagKey<Biome> IS_MAGICAL = tag("is_magical");
	public static final TagKey<Biome> IS_RARE = tag("is_rare");
	public static final TagKey<Biome> IS_PLATEAU = tag("is_plateau");
	public static final TagKey<Biome> IS_SPOOKY = tag("is_spooky");
	public static final TagKey<Biome> IS_FLORAL = tag("is_floral");
	public static final TagKey<Biome> IS_SANDY = tag("is_sandy");
	public static final TagKey<Biome> IS_SNOWY = tag("is_snowy");
	public static final TagKey<Biome> IS_ICY = tag("is_icy");
	public static final TagKey<Biome> IS_AQUATIC = tag("is_aquatic");
	public static final TagKey<Biome> IS_AQUATIC_ICY = tag("is_aquatic_icy");
	public static final TagKey<Biome> IS_NETHER = tag("is_nether");
	public static final TagKey<Biome> IS_NETHER_FOREST = tag("is_nether_forest");
	public static final TagKey<Biome> IS_END = tag("is_end");
	public static final TagKey<Biome> IS_OUTER_END_ISLAND = tag("is_outer_end_island");

	private ConventionalBiomeTagsV2() {
	}

	private static TagKey<Biome> tag(String path) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", path));
	}
}
