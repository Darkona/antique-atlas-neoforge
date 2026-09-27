package folk.sisby.antique_atlas.gui.tiles;

import folk.sisby.antique_atlas.TileTexture;

/**
 * A quarter of a tile, containing the following information:
 * <ul>
 * <li><b>tile</b>, containing the texture file and the variation number</li>
 * <li><b>offset</b> from the top left corner to the appropriate sub-tile part
 * 		of the texture</li>
 * <li><b>x, y</b> coordinates of the subtile on the grid, measured in subtiles,
 * 		starting from (0,0) in the top left corner</li>
 * <li><b>shape</b> of the subtile</li>
 * <li>which <b>part</b> of the whole tile this subtile constitutes</li>
 * </ul>
 *
 * @author Hunternif
 */
public class SubTile {
	public TileTexture texture;
	/**
	 * coordinates of the subtile on the grid, measured in subtiles,
	 * starting from (0,0) in the top left corner.
	 */
	public int x, y;
	public Shape shape;
	public final Part part;
	/**
	 * Which of this subtile's neighbours are unexplored: {@link #FADE_HORIZONTAL}, {@link #FADE_VERTICAL}, {@link #FADE_CORNER}.
	 */
	public int fade;

	/**
	 * The neighbour beside this subtile's outer side is unexplored.
	 */
	public static final int FADE_HORIZONTAL = 1;
	/**
	 * The neighbour above or below this subtile's outer side is unexplored.
	 */
	public static final int FADE_VERTICAL = 2;
	/**
	 * Any of the three neighbours at this subtile's outer corner is unexplored.
	 */
	public static final int FADE_CORNER = 4;

	public SubTile(Part part) {
		this.part = part;
	}

	/**
	 * Texture offset from to the respective subtile section, in subtiles.
	 */
	public int getTextureU() {
		return switch (shape) {
			case SINGLE_OBJECT -> part.u;
			case CONCAVE -> 2 + part.u;
			case VERTICAL, CONVEX -> part.u * 3;
			case HORIZONTAL, FULL -> 2 - part.u;
		};
	}

	/**
	 * Texture offset from to the respective subtile section, in subtiles.
	 */
	public int getTextureV() {
		return switch (shape) {
			case SINGLE_OBJECT, CONCAVE -> part.v;
			case CONVEX, HORIZONTAL -> 2 + part.v * 3;
			case FULL, VERTICAL -> 4 - part.v;
		};
	}

	/**
	 * @return the fade flags for a subtile whose horizontal, vertical and diagonal neighbours (at its outer corner) are these.
	 */
	public static int fadeFlags(Object horizontal, Object vertical, Object diagonal) {
		int flags = 0;
		if (horizontal == null) flags |= FADE_HORIZONTAL;
		if (vertical == null) flags |= FADE_VERTICAL;
		if (horizontal == null || vertical == null || diagonal == null) flags |= FADE_CORNER;
		return flags;
	}

	/**
	 * The vertices drawn transparent so the subtile fades towards unexplored neighbours (antique-atlas#87): bit n is
	 * vertex n in {@code DrawBatcher}'s order, (0,0), (0,1), (1,1), (1,0) in subtile space. The tile's centre stays opaque;
	 * the midpoint of a side, or the outer corner, is transparent when the neighbour there is unexplored.
	 */
	public int fadeCorners() {
		if (fade == 0) return 0;
		int mask = 0;
		if ((fade & FADE_CORNER) != 0) mask |= vertexBit(part.u, part.v);
		if ((fade & FADE_HORIZONTAL) != 0) mask |= vertexBit(part.u, 1 - part.v);
		if ((fade & FADE_VERTICAL) != 0) mask |= vertexBit(1 - part.u, part.v);
		return mask;
	}

	private static int vertexBit(int a, int b) {
		return 1 << (a == 0 ? b : 3 - b);
	}

	public SubTile copy() {
		SubTile copy = new SubTile(part);
		copy.texture = this.texture;
		copy.fade = this.fade;
		copy.x = this.x;
		copy.y = this.y;
		copy.shape = this.shape;
		return copy;
	}

	public enum Shape {
		CONVEX, CONCAVE, HORIZONTAL, VERTICAL, FULL, SINGLE_OBJECT
	}

	public enum Part {
		TOP_LEFT(0, 0), TOP_RIGHT(1, 0), BOTTOM_LEFT(0, 1), BOTTOM_RIGHT(1, 1);
		/**
		 * Texture offset from a whole-tile-section to the respective part, in subtiles.
		 */
		final int u, v;

		Part(int u, int v) {
			this.u = u;
			this.v = v;
		}
	}
}
