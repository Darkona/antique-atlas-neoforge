package folk.sisby.antique_atlas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasMinimapTest {
	private static final Path SRC = Path.of("src/main/java/folk/sisby/antique_atlas");

	@Test
	@DisplayName("The minimap sits in its corner, inside the margin and frame")
	void corners() {
		int inset = AntiqueAtlasConfig.MinimapCorner.MARGIN + AntiqueAtlasConfig.MinimapCorner.FRAME;
		assertEquals(inset, AntiqueAtlasConfig.MinimapCorner.TOP_LEFT.left(480, 96));
		assertEquals(inset, AntiqueAtlasConfig.MinimapCorner.TOP_LEFT.top(270, 96));
		assertEquals(480 - inset - 96, AntiqueAtlasConfig.MinimapCorner.TOP_RIGHT.left(480, 96));
		assertEquals(inset, AntiqueAtlasConfig.MinimapCorner.TOP_RIGHT.top(270, 96));
		assertEquals(inset, AntiqueAtlasConfig.MinimapCorner.BOTTOM_LEFT.left(480, 96));
		assertEquals(270 - inset - 96, AntiqueAtlasConfig.MinimapCorner.BOTTOM_LEFT.top(270, 96));
		assertEquals(480 - inset - 96, AntiqueAtlasConfig.MinimapCorner.BOTTOM_RIGHT.left(480, 96));
		assertEquals(270 - inset - 96, AntiqueAtlasConfig.MinimapCorner.BOTTOM_RIGHT.top(270, 96));
	}

	@Test
	@DisplayName("The minimap is off by default and hidden with F1, F3 or any screen")
	void hiddenWhenItShouldBe() throws IOException {
		String config = Files.readString(SRC.resolve("AntiqueAtlasConfig.java"));
		assertTrue(config.contains("public boolean minimap = false;"), "the minimap is opt-in");
		String layer = Files.readString(SRC.resolve("gui/MinimapLayer.java"));
		String visible = layer.substring(layer.indexOf("static boolean visible("));
		visible = visible.substring(0, visible.indexOf("\n\t}"));
		for (String check : new String[]{"CONFIG.minimap", "options.hideGui", "client.screen != null", "showDebugScreen()", "hasHandheldAtlas("}) {
			assertTrue(visible.contains(check), "visible() must check " + check);
		}
		assertTrue(visible.contains("tick != checkedTick"), "the inventory is checked once per tick, not per frame");
		assertTrue(Files.readString(SRC.resolve("AntiqueAtlas.java")).contains("RegisterGuiLayersEvent.class, e -> e.registerBelow(VanillaGuiLayers.CHAT, id(\"minimap\"), MinimapLayer::render)"));
	}

	@Test
	@DisplayName("A minimap frame reuses one renderer and the atlas's batched drawing")
	void noPerFrameRenderer() throws IOException {
		String layer = Files.readString(SRC.resolve("gui/MinimapLayer.java"));
		String render = layer.substring(layer.indexOf("public static void render("));
		render = render.substring(0, render.indexOf("\n\t}"));
		assertFalse(render.contains("new MinimapRenderer"), "one renderer for every frame");
		assertFalse(render.contains("getPosition(") || render.contains("new Vec3"), "no vectors per frame");
		assertTrue(render.contains("RENDERER.renderTiles(") && render.contains("try (MarkerBatch batch = new MarkerBatch("), "tiles and markers use the batched atlas drawing");
		assertTrue(render.indexOf("graphics.flush()") < render.indexOf("RENDERER.renderTiles("), "buffered fills are flushed before the immediate tile draws");
		assertTrue(render.indexOf("graphics.disableScissor()") > render.indexOf("drawCenteredWithRotation("), "the scissor covers everything drawn on the map");
	}
}
