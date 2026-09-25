package folk.sisby.antique_atlas.util;

/**
 * Size and scale of a map export (antique-atlas#216): the whole explored area at 16 pixels per tile, zoomed out until it
 * fits the largest image allowed. Pure logic, so it can be tested without a Minecraft bootstrap.
 *
 * @param tileChunks   chunks per tile (a power of two).
 * @param originChunkX the first chunk column, on the tile grid.
 * @param originChunkZ the first chunk row, on the tile grid.
 * @param width        image width in pixels.
 * @param height       image height in pixels.
 */
public record ExportLayout(int tileChunks, int originChunkX, int originChunkZ, int width, int height) {
	public static final int TILE_PIXELS = 16;
	public static final int MAX_PIXELS = 4096;
	private static final int MAX_TILE_CHUNKS = 1 << 16;

	/**
	 * The explored chunks are {@code minChunkX..maxChunkX} by {@code minChunkZ..maxChunkZ}, inclusive.
	 *
	 * @param startTileChunks the current zoom, in chunks per tile; the export starts at this scale and zooms out only as far as it must.
	 * @param maxPixels the largest side allowed.
	 * @return the layout, or null if nothing is explored.
	 */
	public static ExportLayout of(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, int startTileChunks, int maxPixels) {
		if (maxChunkX < minChunkX || maxChunkZ < minChunkZ) return null;
		int tileChunks = Math.max(1, Integer.highestOneBit(Math.max(1, startTileChunks)));
		while (true) {
			int originX = Math.floorDiv(minChunkX, tileChunks) * tileChunks;
			int originZ = Math.floorDiv(minChunkZ, tileChunks) * tileChunks;
			long tilesX = Math.floorDiv(maxChunkX, tileChunks) - Math.floorDiv(minChunkX, tileChunks) + 1L;
			long tilesZ = Math.floorDiv(maxChunkZ, tileChunks) - Math.floorDiv(minChunkZ, tileChunks) + 1L;
			long width = tilesX * TILE_PIXELS;
			long height = tilesZ * TILE_PIXELS;
			if ((width <= maxPixels && height <= maxPixels) || tileChunks >= MAX_TILE_CHUNKS) {
				return new ExportLayout(tileChunks, originX, originZ, (int) Math.min(width, maxPixels), (int) Math.min(height, maxPixels));
			}
			tileChunks <<= 1;
		}
	}

	/**
	 * @return map pixels per block.
	 */
	public double pixelsPerBlock() {
		return TILE_PIXELS / (tileChunks * 16.0);
	}

	/**
	 * @return a file name for the export, without the extension: {@code namespace_path-date}, safe on every file system.
	 */
	public static String fileName(String dimensionId, String date) {
		StringBuilder name = new StringBuilder(dimensionId.length() + date.length() + 1);
		for (int i = 0; i < dimensionId.length(); i++) {
			char c = dimensionId.charAt(i);
			name.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' ? c : '_');
		}
		return name.append('-').append(date).toString();
	}
}
