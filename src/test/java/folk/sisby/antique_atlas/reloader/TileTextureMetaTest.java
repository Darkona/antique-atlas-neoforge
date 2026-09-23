package folk.sisby.antique_atlas.reloader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileTextureMetaTest {
	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("test", path);
	}

	private static TileTextures.TileTextureMeta meta(ResourceLocation parent, Set<ResourceLocation> tags, Set<ExtraCodecs.TagOrElementLocation> tilesToThis) {
		return new TileTextures.TileTextureMeta(parent, null, new HashSet<>(tags), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(tilesToThis), new HashSet<>(), new HashSet<>());
	}

	@Test
	@DisplayName("tilesToThis may name textures without metadata; their defaults stay separate")
	void tilesToThisOnDefaults() {
		Map<ResourceLocation, TileTextures.TileTextureMeta> prepared = new HashMap<>();
		prepared.put(id("a"), TileTextures.TileTextureMeta.defaults());
		prepared.put(id("b"), TileTextures.TileTextureMeta.defaults());
		prepared.put(id("c"), meta(null, Set.of(), Set.of(new ExtraCodecs.TagOrElementLocation(id("a"), false))));
		prepared.forEach((id, meta) -> meta.applyTilesToThis(id, prepared));
		assertEquals(Set.of(new ExtraCodecs.TagOrElementLocation(id("c"), false)), prepared.get(id("a")).tilesTo());
		assertTrue(prepared.get(id("b")).tilesTo().isEmpty(), "one texture's default metadata leaked into another's");
	}

	@Test
	@Timeout(5)
	@DisplayName("A parent loop is broken instead of freezing the reload")
	void parentLoop() {
		Map<ResourceLocation, TileTextures.TileTextureMeta> prepared = new HashMap<>();
		prepared.put(id("a"), meta(id("b"), Set.of(id("tag_a")), Set.of()));
		prepared.put(id("b"), meta(id("a"), Set.of(id("tag_b")), Set.of()));
		prepared.put(id("self"), meta(id("self"), Set.of(), Set.of()));
		prepared.put(id("child"), meta(id("a"), Set.of(), Set.of()));
		TileTextures.inheritFromParents(prepared);
		assertTrue(prepared.get(id("child")).tags().containsAll(Set.of(id("tag_a"), id("tag_b"))), "ancestors up to the loop are still inherited");
	}
}
