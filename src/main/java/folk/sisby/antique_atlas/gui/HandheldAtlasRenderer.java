package folk.sisby.antique_atlas.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.util.DrawBatcher;
import folk.sisby.antique_atlas.util.MathUtil;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.antique_atlas.MarkerTexture;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector2d;

public record HandheldAtlasRenderer(int bookX, int bookY, int bookWidth, int bookHeight, int mapWidth, int mapHeight, int tilePixels, int tileChunks, double guiScale, double mapOffsetX, double mapOffsetY, int mapScale, Player player, WorldAtlasData worldAtlasData, ResourceKey<Level> dim) implements AtlasRenderer {
	public static HandheldAtlasRenderer fromContext(Player player) {
		return new HandheldAtlasRenderer(
			0,
			0,
			DEFAULT_BOOK_WIDTH,
			DEFAULT_BOOK_HEIGHT,
			DEFAULT_BOOK_WIDTH - MAP_BORDER_WIDTH * 2,
			DEFAULT_BOOK_HEIGHT - MAP_BORDER_HEIGHT * 2,
			16,
			1,
			1,
			-player.getBlockX(),
			-player.getBlockZ(),
			1,
			player,
			WorldAtlasData.getOrCreate(player.level().dimension()),
			player.level().dimension()
		);
	}

	public void renderHandheldAtlas(PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		matrices.pushPose();
		matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
		matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));
		matrices.scale(0.38F * 142.0F / 218.0F, 0.38F * 142.0F / 218.0F, 0.38F);
		matrices.translate(-1.2D, -0.88D, 0D);
		matrices.scale(1.0F / 128.0F, 1.0F / 128.0F, 1.0F / 128.0F);

		DrawBatcher.drawSingle(matrices, vertexConsumers, AtlasScreen.BOOK, bookWidth, bookHeight, light, bookX, bookY, 0.01F, bookWidth, bookHeight, 0, 0, bookWidth, bookHeight, 0xFFFFFFFF, false);

		if (!(Minecraft.getInstance().screen instanceof AtlasScreen)) {
			renderTiles(matrices, vertexConsumers, light);

			Map<UUID, PlayerSummary> friends = AntiqueAtlas.getOrderedFriends();
			overlays.keySet().forEach(id -> overlays.get(id).onRender(new AtlasOverlay.AtlasRenderContext(this, matrices, vertexConsumers, null, null, light, 1.0F, friends)));

			EDGE_FADE.set(bookX + MAP_BORDER_WIDTH, bookY + MAP_BORDER_HEIGHT, mapWidth, mapHeight);
			for (Map.Entry<Landmark, MarkerTexture> entry : worldAtlasData.getAllMarkers(tileChunks).entrySet()) {
				drawMarker(matrices, vertexConsumers, entry.getKey(), entry.getValue(), -0.02F, light, EDGE_FADE, false, false, 1);
			}

			UUID self = SurveyorClient.getClientUuid();
			for (Map.Entry<UUID, PlayerSummary> entry : friends.entrySet()) {
				renderPlayer(matrices, vertexConsumers, -0.04F, light, entry.getValue(), 1, 1, false, entry.getKey().equals(self));
			}

			DrawBatcher.drawSingle(matrices, vertexConsumers, BOOK_FRAME, bookWidth, bookHeight, light, bookX, bookY, -0.03F, bookWidth, bookHeight, 0, 0, bookWidth, bookHeight, 0xFFFFFFFF, true);
		}

		matrices.popPose();
	}

	private static final class EdgeFade implements MarkerAlpha {
		private int x, y, width, height;

		void set(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		@Override
		public float alpha(double px, double py) {
			double dx = Math.min(px - x, x + width - px);
			double dy = Math.min(py - y, y + height - py);
			return (float) Mth.clamp(Math.min(dx, dy) / 32.0, 0, 1);
		}
	}

	private static final EdgeFade EDGE_FADE = new EdgeFade();

	@Override
	public double getPixelsPerBlock() {
		return 1;
	}
}
