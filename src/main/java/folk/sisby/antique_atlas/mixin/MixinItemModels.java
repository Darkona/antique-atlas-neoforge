package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemModelShaper.class)
public class MixinItemModels {
	@ModifyReturnValue(method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("RETURN"))
	protected BakedModel useAtlasBookModel(BakedModel original, ItemStack stack) {
		if (AntiqueAtlas.isHandheldAtlas(stack)) {
			return ((ItemModelShaper) (Object) this).getModelManager().getModel(AntiqueAtlas.ATLAS_MODEL);
		}
		return original;
	}
}
