package folk.sisby.antique_atlas.gui.tiles;

import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.tiles.SubTile.Part;
import folk.sisby.antique_atlas.gui.tiles.SubTile.Shape;
import folk.sisby.antique_atlas.util.Rect;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Iterates through a tile storage for the purpose of rendering their textures.
 * Returned is an array of 4 {@link SubTile}s which constitute a whole Tile.
 * The SubTile objects are generated on the fly and not retained in memory.
 * May return null!
 *
 * @author Hunternif
 */
public class TileRenderIterator implements Iterator<SubTileQuartet>, Iterable<SubTileQuartet> {

	protected final WorldAtlasData tiles;

	/**
	 * How many chunks a tile spans. Used for viewing the map at a scale below
	 * the threshold at which the tile texture is of minimum size and no longer
	 * scales down. Can't be less than 1.
	 */
	protected int step = 1;

	public void setStep(int step) {
		if (step >= 1) {
			this.step = step;
		}
	}

	/**
	 * The scope of iteration.
	 */
	protected final Rect scope = new Rect();

	public void setScope(Rect scope) {
		this.scope.set(scope);
		chunkX = scope.minX;
		chunkY = scope.minY;
		subtileX = -1;
		subtileY = -1;
	}

	/**
	 * The group of adjacent tiles used for traversing the storage.
	 * <pre>
	 *   a | b
	 * c d | e f
	 * ---------
	 * g h | i j
	 *   k | l
	 * </pre>
	 * 'i' is at (x, y).
	 * The returned array of subtiles represents the corner 'd-e-h-i'
	 */
	protected TileTexture a, b, c, d, e, f, g, h, i, j, k, l;

	/**
	 * Shortcuts for the quartet.
	 */
	protected final SubTile _d = new SubTile(Part.BOTTOM_RIGHT),
		_e = new SubTile(Part.BOTTOM_LEFT),
		_h = new SubTile(Part.TOP_RIGHT),
		_i = new SubTile(Part.TOP_LEFT);
	protected final SubTileQuartet quartet = new SubTileQuartet(_d, _e, _h, _i);

	/**
	 * Current index into the tile storage, which presumably has every tile spanning exactly 1 chunk.
	 */
	protected int chunkX, chunkY;
	/**
	 * Current index into the grid of subtiles, starting at (-1, -1).
	 */
	protected int subtileX = -1, subtileY = -1;

	public TileRenderIterator(WorldAtlasData tiles) {
		this.tiles = tiles;
		setScope(tiles.getScope());
	}

	@Override
	public @NotNull Iterator<SubTileQuartet> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return chunkX >= scope.minX && chunkX <= scope.maxX + step &&
			chunkY >= scope.minY && chunkY <= scope.maxY + step;
	}

	@Override
	public SubTileQuartet next() {
		if (chunkX == scope.minX) {
			// Row start, the first row included: nothing to the left, prime the lookahead column
			a = b = c = d = e = g = h = i = k = l = null;
			f = tiles.getTile(chunkX, chunkY - step);
			j = tiles.getTile(chunkX, chunkY);
		}
		a = b;
		b = tiles.getTile(chunkX, chunkY - step * 2);
		c = d;
		d = e;
		e = f;
		f = tiles.getTile(chunkX + step, chunkY - step);
		g = h;
		h = i;
		i = j;
		j = tiles.getTile(chunkX + step, chunkY);
		k = l;
		l = tiles.getTile(chunkX, chunkY + step);

		quartet.setCoords(subtileX, subtileY);
		_d.texture = d;
		_e.texture = e;
		_h.texture = h;
		_i.texture = i;

		// At first assume all convex:
		_d.shape = Shape.CONVEX;
		_e.shape = Shape.CONVEX;
		_h.shape = Shape.CONVEX;
		_i.shape = Shape.CONVEX;

		// Connect horizontally:
		if (tilesToHorizontal(d, e)) {
			applyTilingHorizontally(_d);
		}
		if (tilesToHorizontal(e, d)) {
			applyTilingHorizontally(_e);
		}
		if (tilesToHorizontal(h, i)) {
			applyTilingHorizontally(_h);
		}
		if (tilesToHorizontal(i, h)) {
			applyTilingHorizontally(_i);
		}

		// Connect vertically:
		if (tilesToVertical(d, h)) {
			applyTilingVertically(_d);
			if (_d.shape == Shape.CONCAVE && tilesTo(d, i)) {
				_d.shape = Shape.FULL;
			}
		}
		if (tilesToVertical(h, d)) {
			applyTilingVertically(_h);
			if (_h.shape == Shape.CONCAVE && tilesTo(h, e)) {
				_h.shape = Shape.FULL;
			}
		}
		if (tilesToVertical(e, i)) {
			applyTilingVertically(_e);
			if (_e.shape == Shape.CONCAVE && tilesTo(e, h)) {
				_e.shape = Shape.FULL;
			}
		}
		if (tilesToVertical(i, e)) {
			applyTilingVertically(_i);
			if (_i.shape == Shape.CONCAVE && tilesTo(i, d)) {
				_i.shape = Shape.FULL;
			}
		}

		// For any convex subtile check for single-object:
		if (_d.shape == Shape.CONVEX && !tilesToVertical(d, a) && !tilesToHorizontal(d, c)) {
			_d.shape = Shape.SINGLE_OBJECT;
		}
		if (_e.shape == Shape.CONVEX && !tilesToVertical(e, b) && !tilesToHorizontal(e, f)) {
			_e.shape = Shape.SINGLE_OBJECT;
		}
		if (_h.shape == Shape.CONVEX && !tilesToHorizontal(h, g) && !tilesToVertical(h, k)) {
			_h.shape = Shape.SINGLE_OBJECT;
		}
		if (_i.shape == Shape.CONVEX && !tilesToHorizontal(i, j) && !tilesToVertical(i, l)) {
			_i.shape = Shape.SINGLE_OBJECT;
		}

		chunkX += step;
		subtileX += 2;
		if (chunkX > scope.maxX + step) {
			chunkX = scope.minX;
			subtileX = -1;
			chunkY += step;
			subtileY += 2;
		}
		return quartet;
	}

	public static boolean tilesTo(TileTexture tile, TileTexture to) {
		return tile != null && to != null && tile.tiles(to);
	}

	public static boolean tilesToHorizontal(TileTexture tile, TileTexture to) {
		return tile != null && to != null && tile.tilesHorizontally(to);
	}

	public static boolean tilesToVertical(TileTexture tile, TileTexture to) {
		return tile != null && to != null && tile.tilesVertically(to);
	}

	public static void applyTilingHorizontally(SubTile subtile) {
		if (subtile.shape == Shape.VERTICAL) subtile.shape = Shape.CONCAVE;
		else if (subtile.shape == Shape.CONVEX) subtile.shape = Shape.HORIZONTAL;
	}

	public static void applyTilingVertically(SubTile subtile) {
		if (subtile.shape == Shape.HORIZONTAL) subtile.shape = Shape.CONCAVE;
		else if (subtile.shape == Shape.CONVEX) subtile.shape = Shape.VERTICAL;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("cannot remove subtiles from tile storage");
	}

}
