package folk.sisby.antique_atlas;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;


public class AntiqueAtlasKeybindings {
	public static final KeyMapping ATLAS_KEYMAPPING = new KeyMapping("key.antique_atlas.open", InputConstants.Type.KEYSYM, 77, "key.antique_atlas.category");

	public static void init(IEventBus modBus) {
		modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(ATLAS_KEYMAPPING));
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> onClientTick(Minecraft.getInstance()));
	}

	public static void onClientTick(Minecraft client) {
		while (ATLAS_KEYMAPPING.consumeClick()) AntiqueAtlas.openAtlasScreen();
	}
}
