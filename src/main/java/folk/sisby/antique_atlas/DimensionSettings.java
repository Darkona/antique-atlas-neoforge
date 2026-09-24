package folk.sisby.antique_atlas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/**
 * How the map reads one dimension's terrain, from {@code atlas/dimension/<path>.json} for dimension {@code <namespace>:<path>}
 * (antique-atlas#77, antique-atlas#249). A dimension without a file uses {@link #DEFAULT}.
 * Registry-free, so it can be tested without a Minecraft bootstrap.
 *
 * @param scanner      which terrain scan to run.
 * @param seaLevel     surface: the Y that elevations and ravines are measured from; nether: the lava sea level, floors under it are drawn as their biome.
 * @param scanTop      the highest Y scanned (none: the whole column); for the nether scanner, its logical ceiling.
 * @param floorScanTop nether scanner only: the highest Y of the low scan that finds lava seas and shores.
 * @param ravines      whether columns far below sea level are drawn as ravines.
 * @param emptyTile    the tile for columns with no floor (void).
 */
public record DimensionSettings(Scanner scanner, int seaLevel, Optional<Integer> scanTop, int floorScanTop, boolean ravines, ResourceLocation emptyTile) {
	public static final int DEFAULT_SEA_LEVEL = 63;
	public static final int DEFAULT_FLOOR_SCAN_TOP = 50;
	public static final ResourceLocation DEFAULT_EMPTY_TILE = ResourceLocation.fromNamespaceAndPath("antique_atlas", "feature/empty");

	public enum Scanner implements StringRepresentable {
		SURFACE("surface"),
		NETHER("nether");

		public static final Codec<Scanner> CODEC = StringRepresentable.fromEnum(Scanner::values);
		private final String name;

		Scanner(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final Codec<DimensionSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Scanner.CODEC.optionalFieldOf("scanner", Scanner.SURFACE).forGetter(DimensionSettings::scanner),
		Codec.INT.optionalFieldOf("sea_level", DEFAULT_SEA_LEVEL).forGetter(DimensionSettings::seaLevel),
		Codec.INT.optionalFieldOf("scan_top").forGetter(DimensionSettings::scanTop),
		Codec.INT.optionalFieldOf("floor_scan_top", DEFAULT_FLOOR_SCAN_TOP).forGetter(DimensionSettings::floorScanTop),
		Codec.BOOL.optionalFieldOf("ravines", false).forGetter(DimensionSettings::ravines),
		ResourceLocation.CODEC.optionalFieldOf("empty_tile", DEFAULT_EMPTY_TILE).forGetter(DimensionSettings::emptyTile)
	).apply(instance, DimensionSettings::new));

	/**
	 * A dimension without a file: the surface scanner at sea level 63, no ravines, the empty tile for the void.
	 */
	public static final DimensionSettings DEFAULT = new DimensionSettings(Scanner.SURFACE, DEFAULT_SEA_LEVEL, Optional.empty(), DEFAULT_FLOOR_SCAN_TOP, false, DEFAULT_EMPTY_TILE);

	/**
	 * @return the scan ceiling as the boxed argument {@code ChunkSummary.toSingleLayer} takes, or null for none.
	 */
	public Integer scanTopOrNull() {
		return scanTop.orElse(null);
	}
}
