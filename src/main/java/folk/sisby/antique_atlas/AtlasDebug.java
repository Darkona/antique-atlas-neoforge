package folk.sisby.antique_atlas;

import java.util.concurrent.atomic.AtomicLongArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug mode: logs what Antique Atlas does, for tests, smoke runs and bug reports. On with the {@code debug} option or
 * {@code -Dantique_atlas.debug=true}.
 * <p>
 * Costs nothing when off: per-tick and per-frame paths only call {@link #count} (a boolean check, no allocation) and
 * every {@link #log} call site is guarded with {@code if (AtlasDebug.on)}. Per-chunk activity is summed and logged once
 * every {@value #SUMMARY_TICKS} client ticks, as one line with the counters that changed.
 */
public final class AtlasDebug {
	public static final String PROPERTY = "antique_atlas.debug";
	public static final String PREFIX = "[Antique Atlas/debug] ";
	public static final int SUMMARY_TICKS = 100;
	private static final Logger LOGGER = LoggerFactory.getLogger("antique_atlas/debug");

	/**
	 * Whether debug mode is on: the config option or the system property. Read on hot paths.
	 */
	public static boolean on = Boolean.getBoolean(PROPERTY);

	public enum Count {
		TERRAIN_QUEUED("terrain.queued"),
		TERRAIN_QUEUE_MAX("terrain.queueMax"),
		TILES_SET("tiles.set"),
		TILES_REMOVED("tiles.removed"),
		TILES_UNDRAWN("tiles.undrawn"),
		STRUCTURES_ADDED("structures.added"),
		MARKERS_ADDED("markers.added"),
		MARKERS_ITEM_TEXTURE("markers.itemTexture"),
		MARKERS_DEFAULT_TEXTURE("markers.defaultTexture"),
		MARKERS_REMOVED("markers.removed"),
		BOOKMARK_REBUILDS("bookmarks.rebuilt");

		final String key;

		Count(String key) {
			this.key = key;
		}
	}

	private static final Count[] COUNTS = Count.values();
	private static final AtomicLongArray VALUES = new AtomicLongArray(COUNTS.length);

	private AtlasDebug() {
	}

	/**
	 * Sets debug mode from the config; the system property keeps it on.
	 */
	public static void configure(boolean config) {
		boolean wasOn = on;
		on = config || Boolean.getBoolean(PROPERTY);
		if (on && !wasOn) LOGGER.info(PREFIX + "Debug mode on; activity is summed up every {} ticks.", SUMMARY_TICKS);
	}

	public static void count(Count count) {
		if (on) VALUES.incrementAndGet(count.ordinal());
	}

	public static void count(Count count, int amount) {
		if (on) VALUES.addAndGet(count.ordinal(), amount);
	}

	public static void max(Count count, int value) {
		if (!on) return;
		int i = count.ordinal();
		for (long old = VALUES.get(i); value > old && !VALUES.compareAndSet(i, old, value); old = VALUES.get(i)) ;
	}

	/**
	 * Logs one event; guard the call with {@code if (AtlasDebug.on)} so nothing is built when off.
	 */
	public static void log(String format, Object... args) {
		LOGGER.info(PREFIX + format, args);
	}

	/**
	 * Called every client tick; logs the summary every {@value #SUMMARY_TICKS} ticks.
	 */
	public static void tick(long tick) {
		if (!on || tick % SUMMARY_TICKS != 0) return;
		String summary = takeSummary();
		if (summary != null) LOGGER.info(PREFIX + "Last {} ticks: {}", SUMMARY_TICKS, summary);
	}

	/**
	 * The counters since the last call, then resets them; null when nothing happened.
	 */
	static String takeSummary() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < COUNTS.length; i++) {
			long value = VALUES.getAndSet(i, 0);
			if (value != 0) builder.append(builder.isEmpty() ? "" : ", ").append(COUNTS[i].key).append('=').append(value);
		}
		return builder.isEmpty() ? null : builder.toString();
	}
}
