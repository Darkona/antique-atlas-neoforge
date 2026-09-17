package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.util.ColorUtil;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.util.RegionPos;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.BitSet;
import java.util.Map;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

public record MarkerGeometry(long @Nullable [] chunks, @Nullable LongOpenHashSet chunkSet, float @Nullable [] accent, float[] fill, float[] fillHovered) {
	public static final float HOVER_TINT = 0.8f;
	private static final float[] WHITE = ColorUtil.componentsFromRgb(0xFFFFFF);

	public static MarkerGeometry of(Landmark landmark) {
		Integer color = landmark.get(LandmarkComponentTypes.COLOR);
		float[] accent = color == null ? null : ColorUtil.componentsFromRgb(color);
		float[] fill = accent == null ? WHITE : accent;
		float[] fillHovered = accent == null ? WHITE : new float[]{HOVER_TINT * accent[0], HOVER_TINT * accent[1], HOVER_TINT * accent[2]};
		if (landmark.get(LandmarkComponentTypes.POS) != null) return new MarkerGeometry(null, null, accent, fill, fillHovered);
		Map<RegionPos, BitSet> regions = landmark.get(LandmarkComponentTypes.CHUNKS);
		LongOpenHashSet set = new LongOpenHashSet();
		if (regions != null) for (ChunkPos chunk : RegionPos.regionsToChunks(regions)) set.add(chunk.toLong());
		return new MarkerGeometry(set.toLongArray(), set, accent, fill, fillHovered);
	}

	public boolean hasChunk(int x, int z) {
		return chunkSet != null && chunkSet.contains(ChunkPos.asLong(x, z));
	}
}
