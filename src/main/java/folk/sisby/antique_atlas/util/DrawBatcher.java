package folk.sisby.antique_atlas.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import folk.sisby.antique_atlas.compat.iris.IrisShaders;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class DrawBatcher implements AutoCloseable {

	protected final Matrix4f matrix4f;
	protected final BufferBuilder bufferBuilder;
	protected final VertexConsumer vertexConsumer;
	protected final float textureWidth;
	protected final float textureHeight;
	protected final int light;
	protected final boolean inWorld;

	public static boolean areWeShadersRightNow() {
		return IrisShaders.inUse();
	}

	public static void drawSingle(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int textureWidth, int textureHeight, int light, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean drawingTransparent) {
		try (DrawBatcher batcher = new DrawBatcher(poseStack, bufferSource, texture, textureWidth, textureHeight, light, drawingTransparent)) {
			batcher.add(x, y, z, width, height, u, v, regionWidth, regionHeight, argb);
		}
	}

	public DrawBatcher(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int textureWidth, int textureHeight, int light, boolean drawingTransparent) {
		this.inWorld = bufferSource != null;
		if (bufferSource == null) {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
			this.bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
			this.vertexConsumer = bufferBuilder;
		} else {
			this.bufferBuilder = null;
			if (IrisShaders.inUse()) {
				if (drawingTransparent) {
					this.vertexConsumer = bufferSource.getBuffer(RenderType.entityNoOutline(texture));
				} else {
					this.vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(texture));
				}
			} else {
				this.vertexConsumer = bufferSource.getBuffer(RenderType.text(texture));
			}
		}
		this.matrix4f = poseStack.last().pose();
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.light = light;
	}

	public void add(int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb) {
		this.innerAdd(x, x + width, y, y + height, z,
			(u + 0.0F) / textureWidth,
			(u + (float) regionWidth) / textureWidth,
			(v + 0.0F) / textureHeight,
			(v + (float) regionHeight) / textureHeight,
			argb
		);
	}

	/**
	 * Like {@link #add}, with the vertices in {@code clearVertices} (bit n = vertex n: (x,y), (x,y2), (x2,y2), (x2,y)) fully
	 * transparent; the colour fades across the quad.
	 */
	public void addFaded(int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, int clearVertices) {
		int clear = argb & 0x00FFFFFF;
		this.innerAdd(x, x + width, y, y + height, z,
			(u + 0.0F) / textureWidth,
			(u + (float) regionWidth) / textureWidth,
			(v + 0.0F) / textureHeight,
			(v + (float) regionHeight) / textureHeight,
			(clearVertices & 1) != 0 ? clear : argb,
			(clearVertices & 2) != 0 ? clear : argb,
			(clearVertices & 4) != 0 ? clear : argb,
			(clearVertices & 8) != 0 ? clear : argb
		);
	}

	public void addScaled(double originX, double originY, float scale, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb) {
		this.innerAdd((float) (originX + scale * x), (float) (originX + scale * (x + width)), (float) (originY + scale * y), (float) (originY + scale * (y + height)), z,
			(u + 0.0F) / textureWidth,
			(u + (float) regionWidth) / textureWidth,
			(v + 0.0F) / textureHeight,
			(v + (float) regionHeight) / textureHeight,
			argb
		);
	}

	protected void innerAdd(float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2, int argb) {
		innerAdd(x1, x2, y1, y2, z, u1, u2, v1, v2, argb, argb, argb, argb);
	}

	protected void innerAdd(float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2, int argb11, int argb12, int argb22, int argb21) {
		if (inWorld) {
			vertexConsumer.addVertex(matrix4f, x1, y1, z).setColor(argb11).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(0,0,0);
			vertexConsumer.addVertex(matrix4f, x1, y2, z).setColor(argb12).setUv(u1, v2).setOverlay(0).setLight(light).setNormal(0,0,0);
			vertexConsumer.addVertex(matrix4f, x2, y2, z).setColor(argb22).setUv(u2, v2).setOverlay(0).setLight(light).setNormal(0,0,0);
			vertexConsumer.addVertex(matrix4f, x2, y1, z).setColor(argb21).setUv(u2, v1).setOverlay(0).setLight(light).setNormal(0,0,0);
		} else {
			vertexConsumer.addVertex(matrix4f, x1, y1, z).setColor(argb11).setUv(u1, v1).setLight(light);
			vertexConsumer.addVertex(matrix4f, x1, y2, z).setColor(argb12).setUv(u1, v2).setLight(light);
			vertexConsumer.addVertex(matrix4f, x2, y2, z).setColor(argb22).setUv(u2, v2).setLight(light);
			vertexConsumer.addVertex(matrix4f, x2, y1, z).setColor(argb21).setUv(u2, v1).setLight(light);
		}
	}

	@Override
	public void close() {
		if (bufferBuilder != null) {
			MeshData bb = bufferBuilder.build();
			if (bb != null) BufferUploader.drawWithShader(bb);
			RenderSystem.disableBlend();
		}
	}
}
