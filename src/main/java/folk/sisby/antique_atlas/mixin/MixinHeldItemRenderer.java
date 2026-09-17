package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.gui.HandheldAtlasRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinHeldItemRenderer {
	@Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
	protected void renderFirstPersonAtlas(PoseStack matrices, MultiBufferSource vertexConsumers, int light, ItemStack stack, CallbackInfo ci) {
		if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return;
		if (!(AntiqueAtlas.isHandheldAtlas(stack))) return;
		HandheldAtlasRenderer.fromContext(Minecraft.getInstance().player).renderHandheldAtlas(matrices, vertexConsumers, light);
		ci.cancel();
	}

	// Fix: NeoForge patches the FILLED_MAP check into instanceof MapItem.
	@ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/item/MapItem", ordinal = 0))
	protected boolean enableFirstPersonAtlasRendering(boolean original, AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		return original || AntiqueAtlas.isHandheldAtlas(stack);
	}
}
