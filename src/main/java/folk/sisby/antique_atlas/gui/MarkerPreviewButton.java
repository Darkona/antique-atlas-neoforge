package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.MarkerTexture;
import net.minecraft.client.gui.GuiGraphics;

public class MarkerPreviewButton extends TexturePreviewButton<MarkerTexture> {
	public MarkerPreviewButton(MarkerTexture markerTexture, float[] tint) {
		super(markerTexture, markerTexture.id(), markerTexture.textureWidth(), markerTexture.textureHeight(), 0, tint);
	}

	@Override
	protected void drawTexture(GuiGraphics graphics, int x, int y) {
		MarkerTexture texture = getValue();
		int size = Math.max(texture.textureWidth(), texture.textureHeight());
		if (size <= FRAME_SIZE - 2) {
			texture.drawIcon(graphics, x, y, tint);
			return;
		}
		// Structure icons are larger than the frame: fit them in it
		float scale = (FRAME_SIZE - 2) / (float) size;
		graphics.pose().pushPose();
		graphics.pose().translate(getGuiX() + 1 + (FRAME_SIZE - 2 - texture.textureWidth() * scale) / 2, getGuiY() + 1 + (FRAME_SIZE - 2 - texture.textureHeight() * scale) / 2, 0);
		graphics.pose().scale(scale, scale, 1);
		texture.drawIcon(graphics, 0, 0, tint);
		graphics.pose().popPose();
	}
}
