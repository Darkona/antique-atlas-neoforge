package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.gui.AtlasScreen;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.reloader.StructureTileProviders;
import folk.sisby.antique_atlas.reloader.TileTextures;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.util.RegionPos;
import java.util.BitSet;
import java.util.HashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import java.util.Collections;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod(value = AntiqueAtlas.ID, dist = Dist.CLIENT)
public class AntiqueAtlas {
	public static final String ID = "antique_atlas";
	public static final String NAME = "Antique Atlas";

	public static final Logger LOGGER = LogManager.getLogger(NAME);

	public static final AntiqueAtlasConfig CONFIG = new AntiqueAtlasConfig();
	public static final ScreenState<AtlasScreen> lastState = new ScreenState<>();

	public static final ModelResourceLocation ATLAS_MODEL = ModelResourceLocation.standalone(AntiqueAtlas.id("item/atlas"));

	public static final List<String> ATLAS_NAMES = List.of(
		"Antique Atlas"
	);

	public static ResourceLocation id(String path) {
		return path.contains(":") ? ResourceLocation.tryParse(path) : ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	public static ItemStack getHandheldAtlas() {
		ItemStack stack = Items.BOOK.getDefaultInstance().copy();
		stack.set(DataComponents.ITEM_NAME, Component.translatable("item.antique_atlas.atlas"));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			Component.translatable("item.antique_atlas.atlas.lore").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)),
			Component.translatable("item.antique_atlas.atlas.hint", Component.translatable("item.antique_atlas.atlas")).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false))
		)));
		return stack;
	}

	public static AtlasScreen openAtlasScreen() {
		if (Minecraft.getInstance().screen == null && (!AntiqueAtlas.CONFIG.requireItem || (Minecraft.getInstance().player != null && AntiqueAtlas.hasHandheldAtlas(Minecraft.getInstance().player)))) {
			AtlasScreen screen = new AtlasScreen();
			screen.init();
			screen.prepareToOpen();
			screen.tick();
			Minecraft.getInstance().setScreen(screen);
			return screen;
		}
		return null;
	}

	public static boolean isHandheldAtlas(ItemStack stack) {
		return stack.is(Items.BOOK) && ATLAS_NAMES.stream().anyMatch(n -> stack.getHoverName().getString().toLowerCase().contains(n.toLowerCase()));
	}

	public static boolean hasHandheldAtlas(Player player) {
		if (isHandheldAtlas(player.getOffhandItem())) return true;
		for (ItemStack itemStack : player.getInventory().items) {
			if (isHandheldAtlas(itemStack)) {
				return true;
			}
		}
		return false;
	}

	private static long clientTicks = 0;
	private static long friendsTick = -1;
	private static ClientPacketListener friendsConnection;
	private static Map<UUID, PlayerSummary> friendsMemo = Map.of();

	public static Map<UUID, PlayerSummary> getOrderedFriends() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (friendsTick == clientTicks && friendsConnection == connection) return friendsMemo;
		Map<UUID, PlayerSummary> friends = SurveyorClient.getFriends();
		PlayerSummary playerSummary = friends.remove(SurveyorClient.getClientUuid());
		Map<UUID, PlayerSummary> orderedFriends = new LinkedHashMap<>(friends);
		if (playerSummary != null) orderedFriends.put(SurveyorClient.getClientUuid(), playerSummary);
		friendsMemo = Collections.unmodifiableMap(orderedFriends);
		friendsTick = clientTicks;
		friendsConnection = connection;
		return friendsMemo;
	}

	public AntiqueAtlas(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, AntiqueAtlasConfig.SPEC, "antique-atlas.toml");
		modBus.addListener(ModConfigEvent.Loading.class, e -> onConfig(e.getConfig()));
		modBus.addListener(ModConfigEvent.Reloading.class, e -> onConfig(e.getConfig()));
		AntiqueAtlasKeybindings.init(modBus);
		modBus.addListener(RegisterClientReloadListenersEvent.class, e -> {
			e.registerReloadListener(TileTextures.getInstance());
			e.registerReloadListener(MarkerTextures.getInstance());
			e.registerReloadListener(StructureTileProviders.getInstance());
			e.registerReloadListener(BiomeTileProviders.getInstance());
		});
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, e -> {
			if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.insertAfter(Items.MAP.getDefaultInstance(), getHandheldAtlas(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		});
		modBus.addListener(ModelEvent.RegisterAdditional.class, e -> e.register(ATLAS_MODEL));
		NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickItem.class, e -> {
			if (e.getLevel().isClientSide() && isHandheldAtlas(e.getItemStack()) && openAtlasScreen() != null) {
				e.setCancellationResult(InteractionResult.SUCCESS);
				e.setCanceled(true);
			}
		});
		modBus.addListener(FMLClientSetupEvent.class, e -> e.enqueueWork(() ->
			ItemProperties.register(Items.BOOK, AntiqueAtlas.id("atlas"), ((stack, world, entity, seed) -> isHandheldAtlas(stack) ? 1.0F : 0.0F))
		));

		// Fix: Surveyor may call these from the integrated server thread; hand off to the client thread.
		SurveyorClientEvents.Register.terrainUpdated(id("world_data"), (s, k) -> {
			Map<RegionPos, BitSet> chunks = new HashMap<>(k.size());
			k.forEach((region, bits) -> chunks.put(region, (BitSet) bits.clone()));
			onClientThread(() -> WorldAtlasData.getOrCreate(s.dimension()).onTerrainUpdated(s, chunks));
		});
		SurveyorClientEvents.Register.structuresAdded(id("world_data"), (s, k) -> {
			Multimap<ResourceKey<Structure>, ChunkPos> starts = ImmutableMultimap.copyOf(k);
			onClientThread(() -> WorldAtlasData.getOrCreate(s.dimension()).onStructuresAdded(s, starts));
		});
		SurveyorClientEvents.Register.landmarksAdded(id("world_data"), (s, k) -> {
			Multimap<UUID, ResourceLocation> landmarks = ImmutableMultimap.copyOf(k);
			onClientThread(() -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksAdded(s, landmarks));
		});
		SurveyorClientEvents.Register.landmarksRemoved(id("world_data"), (s, k) -> {
			Multimap<UUID, ResourceLocation> landmarks = ImmutableMultimap.copyOf(k);
			onClientThread(() -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksRemoved(s, landmarks));
		});
		NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, e -> {
			if (e.getLevel() instanceof ClientLevel && WorldAtlasData.hasPendingWork()) SurveyorClient.getSummaries(Minecraft.getInstance().getConnection()).values().forEach(s -> WorldAtlasData.getOrCreate(s.dimension()).tick(s));
		});
		// Fix: only the client cause, which runs on the client thread.
		NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, e -> {
			if (e.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) BiomeTileProviders.getInstance().registerFallbacks(e.getRegistryAccess().registryOrThrow(Registries.BIOME));
		});
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> resetSession());
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> clientTicks++);

		WorldSummary.enableTerrain();
		WorldSummary.enableStructures();
		WorldSummary.enableLandmarks();
	}

	public static void onClientThread(Runnable task) {
		Minecraft client = Minecraft.getInstance();
		if (client.isSameThread()) {
			task.run();
		} else {
			client.execute(task);
		}
	}

	// Fix: clear world-session caches between singleplayer worlds.
	public static void resetSession() {
		BiomeTileProviders.getInstance().clearFallbacks();
		WorldAtlasData.WORLDS.clear();
		TerrainTiling.clearCaches();
		friendsMemo = Map.of();
		friendsTick = -1;
		friendsConnection = null;
	}

	private static void onConfig(ModConfig config) {
		if (config.getSpec() == AntiqueAtlasConfig.SPEC) CONFIG.bake();
	}
}
