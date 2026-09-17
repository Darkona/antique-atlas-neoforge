package folk.sisby.antique_atlas;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

public record TileTexture(ResourceLocation id, boolean innerBorder, Set<TileTexture> tilesTo, Set<TileTexture> tilesToHorizontal, Set<TileTexture> tilesToVertical) {
	public static TileTexture empty(ResourceLocation id, boolean innerBorder) {
		return new TileTexture(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/atlas/tile/%s.png".formatted(id.getPath())), innerBorder, new ReferenceOpenHashSet<>(), new ReferenceOpenHashSet<>(), new ReferenceOpenHashSet<>());
	}

	public static final TileTexture MISSING = empty(AntiqueAtlas.id("missing"), false);
	public static final TileTexture TEST = empty(AntiqueAtlas.id("test"), false);
	@Deprecated
	public static final TileTexture DEFAULT = MISSING;

	public static TileTexture fallback() {
		return AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.TEST ? TEST : MISSING;
	}

	public String displayId() {
		ResourceLocation trimmed = id.withPath(p -> p.substring("textures/atlas/tile/".length(), id.getPath().length() - 4));
		return id.getNamespace().equals(AntiqueAtlas.ID) ? trimmed.getPath() : trimmed.toString();
	}

	public boolean tiles(TileTexture other) {
		return this == other || (innerBorder ^ (tilesTo.contains(other) || tilesToHorizontal.contains(other) || tilesToVertical.contains(other)));
	}

	public boolean tilesHorizontally(TileTexture other) {
		return this == other || (innerBorder ^ (tilesTo.contains(other) || tilesToHorizontal.contains(other)));
	}

	public boolean tilesVertically(TileTexture other) {
		return this == other || (innerBorder ^ (tilesTo.contains(other) || tilesToVertical.contains(other)));
	}

	public record Builder(ResourceLocation id, boolean innerBorder, Set<ResourceLocation> tilesTo, Set<ResourceLocation> tilesToHorizontal, Set<ResourceLocation> tilesToVertical) {
		public void build(Map<ResourceLocation, TileTexture> emptyTextures) {
			if (!tilesTo.isEmpty()) emptyTextures.get(id).tilesTo.addAll(tilesTo.stream().map(emptyTextures::get).collect(Collectors.toSet()));
			if (!tilesToHorizontal.isEmpty()) emptyTextures.get(id).tilesToHorizontal.addAll(tilesToHorizontal.stream().map(emptyTextures::get).collect(Collectors.toSet()));
			if (!tilesToVertical.isEmpty()) emptyTextures.get(id).tilesToVertical.addAll(tilesToVertical.stream().map(emptyTextures::get).collect(Collectors.toSet()));
		}
	}
}
