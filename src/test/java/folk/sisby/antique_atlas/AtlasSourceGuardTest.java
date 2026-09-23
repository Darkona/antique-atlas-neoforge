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
		assertEquals(5, count, "expected the five Surveyor client listeners");
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
		String tick = src.substring(src.indexOf("ClientTickEvent.Post.class, e ->"));
		assertTrue(tick.substring(0, tick.indexOf(");")).contains("clientTicks++"), "the tick counter must advance per client tick");
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

	@Test
	@DisplayName("A resource reload (F3+T) rebuilds the atlas and drops stale providers")
	void reloadRebuilds() throws IOException {
		String main = read("AntiqueAtlas.java");
		assertTrue(main.contains("(ResourceManagerReloadListener) manager -> rebuildPending = true"), "a reload must request a rebuild");
		assertTrue(main.contains("WorldAtlasData.rebuildAll();"), "the rebuild runs on the client tick");
		String rebuild = read("WorldAtlasData.java");
		rebuild = rebuild.substring(rebuild.indexOf("public static void rebuildAll()"));
		rebuild = rebuild.substring(0, rebuild.indexOf("\n\t}"));
		for (String step : new String[]{"WORLDS.clear()", "TerrainTiling.clearCaches()", "registerFallbacks(", "onTerrainUpdated(", "onStructuresAdded(", "onLandmarksAdded("}) {
			assertTrue(rebuild.contains(step), "rebuildAll() must call " + step);
		}
		String providers = read("reloader/BiomeTileProviders.java");
		providers = providers.substring(providers.indexOf("protected void apply("));
		assertTrue(providers.contains("tileProviders.clear()"), "providers removed from a pack must go");
	}

	@Test
	@DisplayName("Middle-click copies a marker's coordinates; graves show their owner; sneak-use zooms the handheld atlas")
	void smallFeatures() throws IOException {
		String screen = read("gui/AtlasScreen.java");
		assertTrue(screen.contains("GLFW.GLFW_MOUSE_BUTTON_MIDDLE") && screen.contains("keyboardHandler.setClipboard(coordinates)"));
		assertTrue(screen.contains("\"gui.antique_atlas.marker.death.owner\""), "other players' graves name their owner");
		assertTrue(read("gui/HandheldAtlasRenderer.java").contains("AntiqueAtlas.handheldZoom()"));
		assertTrue(read("AntiqueAtlas.java").contains("isShiftKeyDown() && isHandheldAtlas("));
	}

	@Test
	@DisplayName("A share group change rebuilds the atlas; a tile without terrain is removed")
	void groupChangeRebuilds() throws IOException {
		assertTrue(read("AntiqueAtlas.java").contains("SurveyorClientEvents.Register.explorationReset(id(\"world_data\"), () -> onClientThread(WorldAtlasData::rebuildAll));"));
		assertTrue(read("WorldAtlasData.java").contains("biomeTiles.remove(pos.toLong())"), "tick must drop tiles whose chunk has no terrain any more");
	}

	@Test
	@DisplayName("The marker picker offers structure icons per config, and every check uses the same rule")
	void structureIconsPickable() throws IOException {
		String src = read("gui/MarkerModal.java");
		assertTrue(src.contains("static boolean isPickable(MarkerTexture texture)") && src.contains("AntiqueAtlas.CONFIG.pickStructureMarkers"));
		assertFalse(src.contains("startsWith(\"custom/\")) continue") || src.contains("filter(t -> t.keyId().getPath().startsWith(\"custom/\"))"), "a check bypasses isPickable");
	}

	@Test
	@DisplayName("A landmark without a texture of its own is drawn with its item's texture")
	void itemMarkerTextures() throws IOException {
		String src = read("reloader/MarkerTextures.java");
		assertTrue(src.contains("fromStack(landmark.get(LandmarkComponentTypes.STACK))"));
		assertTrue(src.contains("optionalFieldOf(\"items\")"), "resource packs can list items and tags per texture");
	}

	@Test
	@DisplayName("A malformed .mcmeta is logged and skipped, never thrown past the metadata catch")
	void badMetadataCaught() throws IOException {
		assertTrue(read("reloader/TileTextures.java").contains("catch (IOException | RuntimeException ex)"), "tile texture metadata");
		assertTrue(read("reloader/MarkerTextures.java").contains("catch (IOException | RuntimeException ex)"), "marker texture metadata");
		assertTrue(read("gui/AtlasScreen.java").contains("catch (RuntimeException | IOException e)"), "dimension icon metadata");
	}
}
