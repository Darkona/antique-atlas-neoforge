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
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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

	// Fix: probe Iris once instead of reflecting (and throwing without Iris) for every batcher.
	private static final MethodHandle IRIS_SHADERS_IN_USE = bindIrisProbe();

	private static MethodHandle bindIrisProbe() {
		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			Object api = lookup.findStatic(apiClass, "getInstance", MethodType.methodType(apiClass)).invoke();
			return lookup.findVirtual(apiClass, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(api);
		} catch (Throwable e) {
			return null;
		}
	}

	public static boolean areWeShadersRightNow() {
		if (IRIS_SHADERS_IN_USE == null) return false;
		try {
			return (boolean) IRIS_SHADERS_IN_USE.invokeExact();
		} catch (Throwable e) {
			return false;
		}
	}

	public static void drawSingle(PoseStack matrices, MultiBufferSource vertexConsumers, ResourceLocation texture, int textureWidth, int textureHeight, int light, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean drawingTransparent) {
		try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, texture, textureWidth, textureHeight, light, drawingTransparent)) {
			batcher.add(x, y, z, width, height, u, v, regionWidth, regionHeight, argb);
		}
	}

	public DrawBatcher(PoseStack matrices, MultiBufferSource vertexConsumers, ResourceLocation texture, int textureWidth, int textureHeight, int light, boolean drawingTransparent) {
		this.inWorld = vertexConsumers != null;
		if (vertexConsumers == null) {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
			this.bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
			this.vertexConsumer = bufferBuilder;
		} else {
			this.bufferBuilder = null;
			if (areWeShadersRightNow()) {
				if (drawingTransparent) {
					this.vertexConsumer = vertexConsumers.getBuffer(RenderType.entityNoOutline(texture));
				} else {
					this.vertexConsumer = vertexConsumers.getBuffer(RenderType.entitySolid(texture));
				}
			} else {
				this.vertexConsumer = vertexConsumers.getBuffer(RenderType.text(texture));
			}
		}
		this.matrix4f = matrices.last().pose();
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
		if (inWorld) {
			vertexConsumer.addVertex(matrix4f, x1, y1, z).setColor(argb).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(0,0,0);
			vertexConsumer.addVertex(matrix4f, x1, y2, z).setColor(argb).setUv(u1, v2).setOverlay(0).setLight(light).setNormal(0,0,0);
			vertexConsumer.addVertex(matrix4f, x2, y2, z).setColor(argb).setUv(u2, v2).setOverlay(0).setLight(light).setNormal(0,0,0);
			vertexConsumer.addVertex(matrix4f, x2, y1, z).setColor(argb).setUv(u2, v1).setOverlay(0).setLight(light).setNormal(0,0,0);
		} else {
			vertexConsumer.addVertex(matrix4f, x1, y1, z).setColor(argb).setUv(u1, v1).setLight(light);
			vertexConsumer.addVertex(matrix4f, x1, y2, z).setColor(argb).setUv(u1, v2).setLight(light);
			vertexConsumer.addVertex(matrix4f, x2, y2, z).setColor(argb).setUv(u2, v2).setLight(light);
			vertexConsumer.addVertex(matrix4f, x2, y1, z).setColor(argb).setUv(u2, v1).setLight(light);
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
