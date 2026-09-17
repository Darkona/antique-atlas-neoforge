package folk.sisby.antique_atlas.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import folk.sisby.antique_atlas.util.DrawBatcher;
import net.minecraft.resources.ResourceLocation;

public final class MarkerBatch implements AutoCloseable {
	private final PoseStack matrices;
	private final int light;
	private DrawBatcher open;
	private ResourceLocation openTexture;
	private int openWidth, openHeight;

	public MarkerBatch(PoseStack matrices, int light) {
		this.matrices = matrices;
		this.light = light;
	}

	public DrawBatcher batcher(ResourceLocation texture, int textureWidth, int textureHeight) {
		if (open != null && texture.equals(openTexture) && textureWidth == openWidth && textureHeight == openHeight) return open;
		flush();
		open = new DrawBatcher(matrices, null, texture, textureWidth, textureHeight, light, false);
		openTexture = texture;
		openWidth = textureWidth;
		openHeight = textureHeight;
		return open;
	}

	public void flush() {
		if (open != null) {
			open.close();
			open = null;
			openTexture = null;
		}
	}

	@Override
	public void close() {
		flush();
	}
}
