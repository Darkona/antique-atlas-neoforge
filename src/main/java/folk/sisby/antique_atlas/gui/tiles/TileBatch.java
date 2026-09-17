package folk.sisby.antique_atlas.gui.tiles;

import folk.sisby.antique_atlas.TileTexture;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

public final class TileBatch {
	public static final int STRIDE = 4;

	private static final Reference2ObjectLinkedOpenHashMap<TileTexture, IntArrayList> BATCHES = new Reference2ObjectLinkedOpenHashMap<>();

	private TileBatch() {
	}

	public static void clear() {
		BATCHES.clear();
	}

	public static Reference2ObjectLinkedOpenHashMap<TileTexture, IntArrayList> collect(TileRenderIterator tiles) {
		for (IntArrayList list : BATCHES.values()) list.clear();
		while (tiles.hasNext()) {
			SubTileQuartet quartet = tiles.next();
			for (int i = 0; i < SubTileQuartet.SIZE; i++) {
				SubTile subtile = quartet.get(i);
				if (subtile == null || subtile.texture == null) continue;
				IntArrayList list = BATCHES.get(subtile.texture);
				if (list == null) {
					list = new IntArrayList();
					BATCHES.put(subtile.texture, list);
				}
				list.add(subtile.x);
				list.add(subtile.y);
				list.add(subtile.getTextureU());
				list.add(subtile.getTextureV());
			}
		}
		return BATCHES;
	}
}
