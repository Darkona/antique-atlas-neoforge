package folk.sisby.antique_atlas;

import com.sun.management.ThreadMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Debug mode: counters summed per summary, on from the config or -Dantique_atlas.debug=true, and free when off.
 */
class AtlasDebugTest {
	private static final Path SRC = Path.of("src/main/java/folk/sisby/antique_atlas");

	@AfterEach
	void reset() {
		System.clearProperty(AtlasDebug.PROPERTY);
		AtlasDebug.configure(false);
		AtlasDebug.takeSummary();
	}

	@Test
	@DisplayName("Off, nothing is counted; on, counters are summed and reset by each summary")
	void countsWhenOn() {
		AtlasDebug.configure(false);
		AtlasDebug.count(AtlasDebug.Count.TILES_SET);
		assertNull(AtlasDebug.takeSummary(), "debug mode off records nothing");
		AtlasDebug.configure(true);
		AtlasDebug.count(AtlasDebug.Count.TILES_SET);
		AtlasDebug.count(AtlasDebug.Count.TILES_SET, 4);
		AtlasDebug.max(AtlasDebug.Count.TERRAIN_QUEUE_MAX, 9);
		AtlasDebug.max(AtlasDebug.Count.TERRAIN_QUEUE_MAX, 2);
		assertEquals("terrain.queueMax=9, tiles.set=5", AtlasDebug.takeSummary());
		assertNull(AtlasDebug.takeSummary(), "a summary resets the counters");
	}

	@Test
	@DisplayName("Terrain updates and tiling are counted while debug mode is on")
	void terrainCounted() {
		AtlasDebug.configure(true);
		WorldAtlasData data = new WorldAtlasData();
		java.util.Map<folk.sisby.surveyor.util.RegionPos, java.util.BitSet> chunks = new java.util.HashMap<>();
		java.util.BitSet bits = new java.util.BitSet();
		bits.set(0, 3);
		chunks.put(new folk.sisby.surveyor.util.RegionPos(0, 0), bits);
		data.onTerrainUpdated(null, chunks);
		assertEquals("terrain.queued=3, terrain.queueMax=3", AtlasDebug.takeSummary());
	}

	@Test
	@DisplayName("-Dantique_atlas.debug=true keeps debug mode on whatever the config says")
	void systemPropertyWins() {
		System.setProperty(AtlasDebug.PROPERTY, "true");
		AtlasDebug.configure(false);
		assertTrue(AtlasDebug.on);
		System.clearProperty(AtlasDebug.PROPERTY);
		AtlasDebug.configure(false);
		assertFalse(AtlasDebug.on);
	}

	@Test
	@DisplayName("Counting allocates nothing, on or off")
	void countingIsFree() {
		ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
		long thread = Thread.currentThread().threadId();
		for (boolean on : new boolean[]{false, true}) {
			AtlasDebug.configure(on);
			for (int i = 0; i < 20_000; i++) AtlasDebug.count(AtlasDebug.Count.TILES_SET);
			long before = threads.getThreadAllocatedBytes(thread);
			for (int i = 0; i < 100_000; i++) {
				AtlasDebug.count(AtlasDebug.Count.TILES_SET);
				AtlasDebug.max(AtlasDebug.Count.TERRAIN_QUEUE_MAX, i & 255);
			}
			long allocated = threads.getThreadAllocatedBytes(thread) - before;
			// A few KB may come from the JIT compiling the loop; one object per call would be megabytes.
			assertTrue(allocated < 16 * 1024, "counting allocated " + allocated + " bytes with debug " + (on ? "on" : "off"));
		}
	}

	@Test
	@DisplayName("Every debug log call is behind the debug check, so nothing is built when off")
	void logCallsGuarded() throws IOException {
		int calls = 0;
		try (var files = Files.walk(SRC)) {
			for (Path file : files.filter(p -> p.toString().endsWith(".java") && !p.endsWith("AtlasDebug.java")).toList()) {
				List<String> lines = Files.readAllLines(file);
				for (int i = 0; i < lines.size(); i++) {
					String line = lines.get(i);
					if (!line.contains("AtlasDebug.log(") && !line.contains("AtlasDebug.tick(")) continue;
					calls++;
					boolean guarded = false;
					for (int j = Math.max(0, i - 3); j <= i; j++) guarded |= lines.get(j).contains("if (AtlasDebug.on");
					assertTrue(guarded, SRC.relativize(file) + ": unguarded debug call: " + line.trim());
				}
			}
		}
		assertTrue(calls > 5, "the mod logs its activity in debug mode");
	}
}
