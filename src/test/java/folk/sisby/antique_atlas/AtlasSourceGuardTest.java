package folk.sisby.antique_atlas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasSourceGuardTest {
	private static final Path SRC = Path.of("src/main/java/folk/sisby/antique_atlas");

	private static String read(String file) throws IOException {
		return Files.readString(SRC.resolve(file));
	}

	@Test
	@DisplayName("Every Surveyor client listener hands off to the client thread")
	void surveyorListenersHandOff() throws IOException {
		String src = read("AntiqueAtlas.java");
		Matcher registration = Pattern.compile("SurveyorClientEvents\\.Register\\.\\w+\\(.*?\\n\\t\\t}\\);", Pattern.DOTALL).matcher(src);
		int count = 0;
		while (registration.find()) {
			count++;
			assertTrue(registration.group().contains("onClientThread("), "listener touches atlas state off the client thread:\n" + registration.group());
		}
		assertEquals(4, count, "expected the four Surveyor client listeners");
	}

	@Test
	@DisplayName("Tag fallbacks are only rebuilt from the client-thread cause")
	void tagsOnlyFromClientCause() throws IOException {
		String src = read("AntiqueAtlas.java");
		assertTrue(src.contains("TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED"), "TagsUpdatedEvent must filter to the client cause");
	}

	@Test
	@DisplayName("Logging out clears every session cache")
	void logoutResetsSession() throws IOException {
		String src = read("AntiqueAtlas.java");
		assertTrue(src.contains("LoggingOut.class, e -> resetSession()"), "LoggingOut must call resetSession()");
		String reset = src.substring(src.indexOf("public static void resetSession()"));
		reset = reset.substring(0, reset.indexOf("\n\t}"));
		for (String clear : new String[]{"clearFallbacks()", "WORLDS.clear()", "TerrainTiling.clearCaches()"}) {
			assertTrue(reset.contains(clear), "resetSession() must call " + clear);
		}
	}

	@Test
	@DisplayName("The tiling column loops use hashed primitive caches and constant indices")
	void tilingColumnsStayPrimitive() throws IOException {
		String src = read("TerrainTiling.java");
		assertFalse(src.contains("ArrayMap"), "array maps search linearly per column");
		assertFalse(src.contains("computeIfAbsent"), "get first, compute on a miss");
		for (String line : src.split("\n")) {
			if (line.contains("indexOf(") && !line.contains("private static final int")) throw new AssertionError("List#indexOf outside a constant: " + line.trim());
			if (line.contains("TileElevation.values()") && !line.contains("private static final")) throw new AssertionError("values() clones per call: " + line.trim());
		}
	}

	@Test
	@DisplayName("The optional-mod probe is bound once")
	void irisProbeBoundOnce() throws IOException {
		String src = read("util/DrawBatcher.java");
		assertEquals(1, src.split("Class\\.forName", -1).length - 1, "Class.forName must only appear in the one-time binder");
		assertTrue(src.contains("private static final MethodHandle IRIS_SHADERS_IN_USE = bindIrisProbe();"));
	}

	@Test
	@DisplayName("Per-frame marker drawing reads cached geometry and reused alpha objects")
	void markersDrawnFromCachedGeometry() throws IOException {
		for (String file : new String[]{"gui/AtlasScreen.java", "gui/HandheldAtlasRenderer.java", "gui/AtlasRenderer.java"}) {
			String src = read(file);
			assertFalse(src.contains("regionsToChunks"), file + " rebuilds region chunk sets per frame");
			assertFalse(src.contains("new Vector2d("), file + " allocates a vector per marker");
			assertFalse(src.contains("componentsFromRgb"), file + " allocates colour arrays per frame");
			assertFalse(Pattern.compile("[^.\\w]renderMarker\\(").matcher(src.replace("default void renderMarker(", "")).find(), file + " calls the boxing renderMarker overload");
		}
	}

	@Test
	@DisplayName("No config value is frozen by a static initialiser")
	void noConfigInStaticInitialisers() throws IOException {
		try (var files = Files.walk(SRC)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				for (String line : Files.readAllLines(file)) {
					if (line.contains("static") && line.contains("=") && line.contains("AntiqueAtlas.CONFIG.")) {
						throw new AssertionError(SRC.relativize(file) + " reads the config in a static initialiser: " + line.trim());
					}
				}
			}
		}
	}

	@Test
	@DisplayName("Friends are rebuilt once per client tick, not per frame")
	void friendsMemoisedPerTick() throws IOException {
		String src = read("AntiqueAtlas.java");
		String method = src.substring(src.indexOf("public static Map<UUID, PlayerSummary> getOrderedFriends()"));
		method = method.substring(0, method.indexOf("\n\t}"));
		assertTrue(method.contains("friendsTick == clientTicks"), "getOrderedFriends must return the tick's memo");
		assertTrue(src.contains("ClientTickEvent.Post.class, e -> clientTicks++"), "the tick counter must advance per client tick");
	}

	@Test
	@DisplayName("Screen markers are drawn in texture runs; hover tests use primitive centres")
	void screenMarkersBatched() throws IOException {
		String screen = read("gui/AtlasScreen.java");
		assertTrue(screen.contains("try (MarkerBatch batch = new MarkerBatch("), "the screen marker loop must batch");
		assertFalse(screen.contains("getCenter("), "getCenter allocates a vector per marker per frame");
		String renderer = read("gui/AtlasRenderer.java");
		assertTrue(renderer.contains("if (batch != null) batch.flush();"), "region fills share the Tesselator: flush the batch first");
	}

	@Test
	@DisplayName("The mod list's Config button opens NeoForge's config screen")
	void configScreenRegistered() throws IOException {
		assertTrue(read("AntiqueAtlas.java").contains("registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new)"));
	}

	@Test
	@DisplayName("The book frame's translucent edge is drawn with blending")
	void frameBlended() throws IOException {
		String src = read("gui/AtlasScreen.java");
		String frame = src.substring(src.indexOf("// Overlay the frame so that edges of the map are smooth:"));
		frame = frame.substring(0, frame.indexOf("context.pose().pushPose();"));
		assertTrue(frame.indexOf("RenderSystem.enableBlend()") < frame.indexOf("BOOK_FRAME"), "blend must be on before the frame is drawn");
	}

	@Test
	@DisplayName("Every pushPose of the hover tooltip is popped")
	void tooltipPosePopped() throws IOException {
		String src = read("gui/AtlasScreen.java");
		String tooltip = src.substring(src.indexOf("context.pose().translate(getMouseX(), getMouseY(), 0);"));
		tooltip = tooltip.substring(0, tooltip.indexOf("context.pose().popPose();"));
		assertFalse(tooltip.contains("return;"), "a return between pushPose and popPose leaks the pose");
	}

	@Test
	@DisplayName("The atlas is recognised by its translation key, and the per-frame check builds no strings")
	void atlasNameTranslated() throws IOException {
		String src = read("AntiqueAtlas.java");
		String method = src.substring(src.indexOf("public static boolean isHandheldAtlas(ItemStack stack) {"));
		method = method.substring(0, method.indexOf("\n\t}"));
		assertTrue(method.contains("DataComponents.ITEM_NAME") && method.contains("ATLAS_KEY"), "the creative atlas is named by translation key");
		assertFalse(method.contains("getHoverName()") || method.contains("toLowerCase") || method.contains("stream()"), "no strings per frame");
		assertTrue(src.contains("I18n.get(ATLAS_KEY)"), "a book renamed to the translated name is an atlas");
	}
}
