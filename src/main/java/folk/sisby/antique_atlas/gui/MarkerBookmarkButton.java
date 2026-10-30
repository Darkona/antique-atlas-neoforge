package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.MarkerTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class MarkerBookmarkButton extends BookmarkButton {
	protected final MarkerTexture markerTexture;

	public MarkerBookmarkButton(Component title, MarkerTexture markerTexture, int accent, boolean backwards, boolean vertical) {
		super(title, markerTexture.id(), accent, accent, markerTexture.textureWidth(), markerTexture.textureHeight(), backwards, vertical);
		this.markerTexture = markerTexture;
	}

	@Override
	public void drawIcon(GuiGraphics graphics, int x, int y) {
		markerTexture.drawIcon(graphics, x, y, iconTint);
	}
}
