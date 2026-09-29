package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AtlasDebug;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasConfig;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.util.DrawUtil;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Optional HUD minimap (antique-atlas#254), off by default. Draws with the atlas's own tile, marker and player code
 * through one reused renderer: per frame it allocates only what the handheld atlas already does.
 */
public final class MinimapLayer {
	private static final int FRAME = AntiqueAtlasConfig.MinimapCorner.FRAME;
	private static final int FRAME_COLOR = 0xFF5A3E1B;
	private static final int PAPER_COLOR = 0xFFE7CEA2;
	private static final float MARKER_SCALE = 0.5F;
	private static final MarkerAlpha OPAQUE = (x, y) -> 1.0F;
	private static final MinimapRenderer RENDERER = new MinimapRenderer();

	private static long checkedTick = Long.MIN_VALUE;
	private static boolean carrying;

	private MinimapLayer() {
	}

	/**
	 * @return whether the minimap is drawn this frame. Cheap: the inventory is only checked once per tick.
	 */
	static boolean visible(Minecraft client) {
		if (!AntiqueAtlas.CONFIG.minimap || client.options.hideGui || client.screen != null || client.player == null || client.level == null || client.getConnection() == null) return false;
		if (client.getDebugOverlay().showDebugScreen()) return false;
		if (!AntiqueAtlas.CONFIG.requireItem) return true;
		long tick = client.level.getGameTime();
		if (tick != checkedTick) {
			checkedTick = tick;
			carrying = AntiqueAtlas.hasHandheldAtlas(client.player);
		}
		return carrying;
	}

	private static boolean debugShown;

	public static void render(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		boolean shown = visible(client);
		if (AtlasDebug.on && shown != debugShown) {
			debugShown = shown;
			AtlasDebug.log("Minimap {}", shown ? "shown" : "hidden");
		}
		if (!shown) return;
		LocalPlayer player = client.player;
		ResourceKey<Level> dim = client.level.dimension();
		if (WorldAtlasData.isEmpty(dim)) return;
		WorldAtlasData data = WorldAtlasData.getOrCreate(dim);

		AntiqueAtlasConfig config = AntiqueAtlas.CONFIG;
		int size = config.minimapSize;
		int x = config.minimapCorner.left(graphics.guiWidth(), size);
		int y = config.minimapCorner.top(graphics.guiHeight(), size);
		float partialTick = delta.getGameTimeDeltaPartialTick(true);
		double playerX = Mth.lerp(partialTick, player.xo, player.getX());
		double playerZ = Mth.lerp(partialTick, player.zo, player.getZ());
		RENDERER.set(x, y, size, config.minimapTileChunks, playerX, playerZ, player, data, dim);

		graphics.fill(x - FRAME, y - FRAME, x + size + FRAME, y + size + FRAME, FRAME_COLOR);
		graphics.fill(x, y, x + size, y + size, PAPER_COLOR);
		graphics.flush(); // the tiles below draw immediately; the fills above are buffered

		graphics.enableScissor(x, y, x + size, y + size);
		RENDERER.renderTiles(graphics.pose(), null, AtlasRenderer.MAX_LIGHT);

		graphics.pose().pushPose();
		graphics.pose().translate(RENDERER.bookX(), RENDERER.bookY(), 0);
		if (config.minimapMarkers) {
			try (MarkerBatch batch = new MarkerBatch(graphics.pose(), AtlasRenderer.MAX_LIGHT)) {
				for (Map.Entry<Landmark, MarkerTexture> entry : data.getAllMarkers(RENDERER.tileChunks()).entrySet()) {
					RENDERER.drawMarker(graphics.pose(), null, entry.getKey(), entry.getValue(), 0, AtlasRenderer.MAX_LIGHT, OPAQUE, false, false, MARKER_SCALE, batch);
				}
			}
		}
		UUID self = SurveyorClient.getClientUuid();
		for (Map.Entry<UUID, PlayerSummary> entry : AntiqueAtlas.getOrderedFriends().entrySet()) {
			if (!entry.getKey().equals(self)) RENDERER.renderPlayer(graphics.pose(), null, 0, AtlasRenderer.MAX_LIGHT, entry.getValue(), 1, 1, false, false);
		}
		graphics.pose().popPose();
		// You, at the centre, with the live position and facing (your summary lags a little behind)
		DrawUtil.drawCenteredWithRotation(graphics.pose(), null, AtlasRenderer.PLAYER, x + size / 2.0, y + size / 2.0, 0, 1, AtlasRenderer.PLAYER_ICON_WIDTH, AtlasRenderer.PLAYER_ICON_HEIGHT, player.getViewYRot(partialTick), AtlasRenderer.MAX_LIGHT, 0xFFFFFFFF);
		graphics.disableScissor();
	}

	/**
	 * One renderer for every frame; the map fills the square at (x, y), centred on the player.
	 */
	static final class MinimapRenderer implements AtlasRenderer {
		private int x, y, size, tileChunks;
		private double centerX, centerZ;
		private Player player;
		private WorldAtlasData data;
		private ResourceKey<Level> dim;

		void set(int x, int y, int size, int tileChunks, double centerX, double centerZ, Player player, WorldAtlasData data, ResourceKey<Level> dim) {
			this.x = x;
			this.y = y;
			this.size = size;
			this.tileChunks = tileChunks;
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.player = player;
			this.data = data;
			this.dim = dim;
		}

		@Override
		public int bookX() {
			return x - MAP_BORDER_WIDTH;
		}

		@Override
		public int bookY() {
			return y - MAP_BORDER_HEIGHT;
		}

		@Override
		public int bookWidth() {
			return size + MAP_BORDER_WIDTH * 2;
		}

		@Override
		public int bookHeight() {
			return size + MAP_BORDER_HEIGHT * 2;
		}

		@Override
		public int mapWidth() {
			return size;
		}

		@Override
		public int mapHeight() {
			return size;
		}

		@Override
		public double mapOffsetX() {
			return -centerX * getPixelsPerBlock();
		}

		@Override
		public double mapOffsetY() {
			return -centerZ * getPixelsPerBlock();
		}

		@Override
		public int tilePixels() {
			return 16;
		}

		@Override
		public int tileChunks() {
			return tileChunks;
		}

		@Override
		public int mapScale() {
			return 1;
		}

		@Override
		public Player player() {
			return player;
		}

		@Override
		public WorldAtlasData worldAtlasData() {
			return data;
		}

		@Override
		public double getPixelsPerBlock() {
			return 1.0 / tileChunks;
		}

		@Override
		public double guiScale() {
			return 1;
		}

		@Override
		public ResourceKey<Level> dim() {
			return dim;
		}
	}
}
