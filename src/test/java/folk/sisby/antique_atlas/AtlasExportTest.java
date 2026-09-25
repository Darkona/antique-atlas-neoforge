package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.util.ExportLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasExportTest {
	private static final Path SRC = Path.of("src/main/java/folk/sisby/antique_atlas");

	@Test
	@DisplayName("A small area is exported at the current zoom, aligned to the tile grid")
	void smallArea() {
		ExportLayout layout = ExportLayout.of(-3, 5, 10, 7, 1, 4096);
		assertEquals(1, layout.tileChunks());
		assertEquals(-3, layout.originChunkX());
		assertEquals(5, layout.originChunkZ());
		assertEquals(14 * 16, layout.width());
		assertEquals(3 * 16, layout.height());
		assertEquals(1.0, layout.pixelsPerBlock());

		ExportLayout zoomed = ExportLayout.of(-3, 5, 10, 7, 4, 4096);
		assertEquals(4, zoomed.tileChunks());
		assertEquals(-4, zoomed.originChunkX(), "the origin is floored to the tile grid, also for negative chunks");
		assertEquals(4, zoomed.originChunkZ());
		assertEquals(4 * 16, zoomed.width(), "tiles -4, 0, 4 and 8");
		assertEquals(16, zoomed.height());
	}

	@Test
	@DisplayName("A large area zooms out until it fits the image limit")
	void largeAreaZoomsOut() {
		ExportLayout layout = ExportLayout.of(-1000, -200, 999, 199, 1, 4096);
		assertEquals(8, layout.tileChunks(), "2000 chunks: 1 -> 32000 px, 2 -> 16000, 4 -> 8000, 8 -> 4000");
		assertTrue(layout.width() <= 4096 && layout.height() <= 4096);
		assertEquals(250 * 16, layout.width());
		assertEquals(50 * 16, layout.height());
	}

	@Test
	@DisplayName("Nothing explored exports nothing; a zoom that isn't a power of two is rounded down")
	void edges() {
		assertNull(ExportLayout.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 1, 4096), "an empty scope");
		assertEquals(2, ExportLayout.of(0, 0, 0, 0, 3, 4096).tileChunks());
		assertEquals(1, ExportLayout.of(0, 0, 0, 0, 0, 4096).tileChunks());
		ExportLayout huge = ExportLayout.of(-30_000_000 / 16, -30_000_000 / 16, 30_000_000 / 16, 30_000_000 / 16, 1, 4096);
		assertTrue(huge.width() <= 4096 && huge.height() <= 4096, "the whole world border fits");
	}

	@Test
	@DisplayName("Export file names are safe on every file system")
	void fileNames() {
		assertEquals("minecraft_overworld-2026-09-28_14.03.22", ExportLayout.fileName("minecraft:overworld", "2026-09-28_14.03.22"));
		assertEquals("mod_deep_down-x", ExportLayout.fileName("mod:deep/down", "x"));
	}

	@Test
	@DisplayName("The export writes the file off the render thread, without AWT")
	void exportOffThreadWithoutAwt() throws IOException {
		String src = Files.readString(SRC.resolve("gui/AtlasExporter.java"));
		int pool = src.indexOf("Util.ioPool().execute(");
		assertTrue(pool > 0 && src.indexOf("image.writeToFile(") > pool, "writeToFile runs in the IO pool");
		assertTrue(src.contains("target.destroyBuffers();") && src.contains("getMainRenderTarget().bindWrite(true)"), "the offscreen target is freed and the main target rebound");
		assertTrue(src.contains("RenderSystem.restoreProjectionMatrix();") && src.contains("modelView.popMatrix();"), "the projection and model-view are restored");
		try (var files = Files.walk(SRC)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				assertFalse(Files.readString(file).contains("java.awt"), SRC.relativize(file) + " uses AWT, which breaks macOS");
			}
		}
		assertTrue(Files.readString(SRC.resolve("gui/AtlasScreen.java")).contains("AtlasExporter.export("), "the atlas screen has an export button");
	}
}
