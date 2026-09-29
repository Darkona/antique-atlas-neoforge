package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AtlasDebug;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.util.ExportLayout;
import folk.sisby.antique_atlas.util.Rect;
import folk.sisby.surveyor.landmark.Landmark;
import java.io.File;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Exports a dimension's whole explored map as a PNG (antique-atlas#216), drawn with the atlas's own tile and marker code
 * into an offscreen target. The file is written off the render thread. No AWT, so it works on macOS.
 */
public record AtlasExporter(ExportLayout layout, WorldAtlasData worldAtlasData, ResourceKey<Level> dim, Player player) implements AtlasRenderer {
	private static final int PAPER = 0xE7CEA2; // book.png's page colour
	private static final MarkerAlpha OPAQUE = (x, y) -> 1.0F;

	/**
	 * Renders the map and saves it. Render thread only (called from the atlas screen's input handling).
	 */
	public static void export(ResourceKey<Level> dim, WorldAtlasData data, int tileChunks, boolean markers, Consumer<Component> messages) {
		Minecraft client = Minecraft.getInstance();
		Rect scope = data.getScope();
		ExportLayout layout = ExportLayout.of(scope.minX, scope.minY, scope.maxX, scope.maxY, tileChunks, Math.min(ExportLayout.MAX_PIXELS, RenderSystem.maxSupportedTextureSize()));
		if (layout == null) {
			messages.accept(Component.translatable("gui.antique_atlas.export.empty"));
			return;
		}
		long started = AtlasDebug.on ? Util.getMillis() : 0;
		NativeImage image;
		try {
			image = new AtlasExporter(layout, data, dim, client.player).draw(markers);
		} catch (RuntimeException e) {
			AntiqueAtlas.LOGGER.error("[Antique Atlas] Couldn't draw the map export", e);
			messages.accept(Component.translatable("gui.antique_atlas.export.failure", e.getMessage()));
			return;
		}
		File directory = new File(new File(client.gameDirectory, "screenshots"), "atlas");
		String name = ExportLayout.fileName(dim.location().toString(), Util.getFilenameFormattedDateTime());
		Util.ioPool().execute(() -> {
			try {
				if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("can't create " + directory);
				File file = uniqueFile(directory, name);
				image.writeToFile(file);
				if (AtlasDebug.on) AtlasDebug.log("Exported {} to {}: {}x{} pixels, {} chunks per tile, {} ms", dim.location(), file, layout.width(), layout.height(), layout.tileChunks(), Util.getMillis() - started);
				Component link = Component.literal("screenshots/atlas/" + file.getName()).withStyle(ChatFormatting.UNDERLINE)
					.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())));
				client.execute(() -> messages.accept(Component.translatable("gui.antique_atlas.export.success", link, layout.width(), layout.height(), layout.tileChunks())));
			} catch (Exception e) {
				AntiqueAtlas.LOGGER.warn("[Antique Atlas] Couldn't save the map export", e);
				client.execute(() -> messages.accept(Component.translatable("gui.antique_atlas.export.failure", String.valueOf(e.getMessage()))));
			} finally {
				image.close();
			}
		});
	}

	private static File uniqueFile(File directory, String name) {
		for (int i = 1; ; i++) {
			File file = new File(directory, name + (i == 1 ? "" : "_" + i) + ".png");
			if (!file.exists()) return file;
		}
	}

	private NativeImage draw(boolean markers) {
		int width = layout.width();
		int height = layout.height();
		Minecraft client = Minecraft.getInstance();
		RenderTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
		RenderSystem.backupProjectionMatrix();
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		try {
			target.setClearColor(((PAPER >> 16) & 0xFF) / 255.0F, ((PAPER >> 8) & 0xFF) / 255.0F, (PAPER & 0xFF) / 255.0F, 1.0F);
			target.clear(Minecraft.ON_OSX);
			target.bindWrite(true);
			RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, width, height, 0.0F, -1000.0F, 1000.0F), VertexSorting.ORTHOGRAPHIC_Z);
			modelView.identity();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.disableDepthTest();
			RenderSystem.setShaderColor(1, 1, 1, 1);

			PoseStack pose = new PoseStack();
			renderTiles(pose, null, MAX_LIGHT);
			if (markers) {
				try (MarkerBatch batch = new MarkerBatch(pose, MAX_LIGHT)) {
					for (Map.Entry<Landmark, MarkerTexture> entry : worldAtlasData.getAllMarkers(layout.tileChunks()).entrySet()) {
						drawMarker(pose, null, entry.getKey(), entry.getValue(), 0, MAX_LIGHT, OPAQUE, false, false, 1.0F, batch);
					}
				}
			}

			NativeImage image = new NativeImage(width, height, false);
			RenderSystem.bindTexture(target.getColorTextureId());
			image.downloadTexture(0, true);
			image.flipY();
			return image;
		} finally {
			RenderSystem.enableDepthTest();
			modelView.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.restoreProjectionMatrix();
			target.destroyBuffers();
			client.getMainRenderTarget().bindWrite(true);
		}
	}

	// The map fills the image: its top-left corner is the layout's first chunk, at pixel (0, 0).
	@Override
	public int bookX() {
		return -MAP_BORDER_WIDTH;
	}

	@Override
	public int bookY() {
		return -MAP_BORDER_HEIGHT;
	}

	@Override
	public int bookWidth() {
		return layout.width() + MAP_BORDER_WIDTH * 2;
	}

	@Override
	public int bookHeight() {
		return layout.height() + MAP_BORDER_HEIGHT * 2;
	}

	@Override
	public int mapWidth() {
		return layout.width();
	}

	@Override
	public int mapHeight() {
		return layout.height();
	}

	@Override
	public double mapOffsetX() {
		return -(layout.originChunkX() * 16.0) * layout.pixelsPerBlock() - layout.width() / 2.0;
	}

	@Override
	public double mapOffsetY() {
		return -(layout.originChunkZ() * 16.0) * layout.pixelsPerBlock() - layout.height() / 2.0;
	}

	@Override
	public int tilePixels() {
		return ExportLayout.TILE_PIXELS;
	}

	@Override
	public int tileChunks() {
		return layout.tileChunks();
	}

	@Override
	public int mapScale() {
		return 1;
	}

	@Override
	public double guiScale() {
		return 1;
	}

	@Override
	public double getPixelsPerBlock() {
		return layout.pixelsPerBlock();
	}
}
