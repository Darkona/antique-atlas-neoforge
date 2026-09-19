package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.util.Rect;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTilingRulesTest {
	private static TileTexture texture(String name, boolean innerBorder) {
		return TileTexture.empty(ResourceLocation.fromNamespaceAndPath("test", name), innerBorder);
	}

	@Test
	@DisplayName("A texture tiles to itself and to what it lists, per direction")
	void tilingDirections() {
		TileTexture forest = texture("forest", false), plains = texture("plains", false), river = texture("river", false), hills = texture("hills", false);
		forest.tilesTo().add(plains);
		forest.tilesToHorizontal().add(river);
		forest.tilesToVertical().add(hills);
		assertTrue(forest.tiles(forest) && forest.tilesHorizontally(forest) && forest.tilesVertically(forest));
		assertTrue(forest.tilesHorizontally(plains) && forest.tilesVertically(plains));
		assertTrue(forest.tilesHorizontally(river));
		assertFalse(forest.tilesVertically(river));
		assertTrue(forest.tilesVertically(hills));
		assertFalse(forest.tilesHorizontally(hills));
		assertFalse(plains.tiles(forest), "tiling is one-way unless both list each other");
	}

	@Test
	@DisplayName("An inner-border texture inverts its list but still tiles to itself")
	void innerBorderInverts() {
		TileTexture road = texture("road", true), grass = texture("grass", false), stone = texture("stone", false);
		road.tilesTo().add(stone);
		assertTrue(road.tiles(road));
		assertTrue(road.tiles(grass), "unlisted textures tile");
		assertFalse(road.tiles(stone), "listed textures keep a border");
	}

	@Test
	@DisplayName("The texture id resolves to the tile texture path and back")
	void textureIds() {
		TileTexture tile = texture("dense_forest", false);
		assertEquals("test:textures/atlas/tile/dense_forest.png", tile.id().toString());
		assertEquals("test:dense_forest", tile.displayId());
	}

	@Test
	@DisplayName("Elevation bands switch exactly at their thresholds")
	void elevationBands() {
		int[] edges = {Integer.MIN_VALUE, 9, 10, 19, 20, 34, 35, 49, 50, Integer.MAX_VALUE};
		TileElevation[] expected = {TileElevation.VALLEY, TileElevation.VALLEY, TileElevation.LOW, TileElevation.LOW, TileElevation.MID, TileElevation.MID, TileElevation.HIGH, TileElevation.HIGH, TileElevation.PEAK, TileElevation.PEAK};
		for (int n = 0; n < edges.length; n++) assertEquals(expected[n], TileElevation.fromBlocksAboveSea(edges[n]), "at " + edges[n]);
	}

	@Test
	@DisplayName("An empty scope grows to exactly the points it is extended to")
	void scopeExtends() {
		Rect scope = new Rect();
		scope.extendTo(-3, 7);
		assertEquals(new Rect(-3, 7, -3, 7), scope, "a single point");
		scope.extendTo(5, -2);
		scope.extendTo(0, 0);
		assertEquals(new Rect(-3, -2, 5, 7), scope);
	}
}
