package folk.sisby.antique_atlas;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public record TerrainTileProvider(ResourceLocation id, Map<TileElevation, List<TileTexture>> textures) {
	private static final TerrainTileProvider MISSING = new TerrainTileProvider(AntiqueAtlas.id("default"), List.of(TileTexture.MISSING));
	private static final TerrainTileProvider TEST = new TerrainTileProvider(AntiqueAtlas.id("default"), List.of(TileTexture.TEST));
	@Deprecated
	public static TerrainTileProvider DEFAULT = MISSING;

	public static TerrainTileProvider fallback() {
		return TileTexture.fallback() == TileTexture.TEST ? TEST : MISSING;
	}

	public TerrainTileProvider(ResourceLocation id, List<TileTexture> textures) {
		this(id, Arrays.stream(TileElevation.values()).collect(Collectors.toMap(e -> e, e -> textures)));
	}

	public TileTexture getTexture(ChunkPos pos, @Nullable TileElevation elevation) {
		int variation = (int) (Mth.getSeed(pos.x, pos.z, pos.x * pos.z) & 0x7FFFFFFF);
		TileElevation usedElevation = elevation == null ? TileElevation.VALLEY : elevation;
		return textures.get(usedElevation).get(variation % textures.get(usedElevation).size());
	}
}
