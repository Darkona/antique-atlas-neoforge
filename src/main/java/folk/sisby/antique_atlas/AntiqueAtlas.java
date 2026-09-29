package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.gui.AtlasScreen;
import folk.sisby.antique_atlas.gui.MinimapLayer;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.DimensionConfigs;
import folk.sisby.antique_atlas.reloader.FeatureRules;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.reloader.StructureTileProviders;
import folk.sisby.antique_atlas.reloader.TileTextures;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.util.RegionPos;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Locale;
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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
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
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
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
		stack.set(DataComponents.ITEM_NAME, Component.translatable(ATLAS_KEY));
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

	public static final String ATLAS_KEY = "item.antique_atlas.atlas";
	private static Component lastCustomName;
	private static String lastLanguage;
	private static boolean lastCustomNameIsAtlas;

	// Fix: the creative atlas is named by translation key, so in other languages its name didn't contain "Antique Atlas"
	// and it stopped working; a book renamed to the translated name (as the item's hint says) didn't work either.
	// Also runs per frame (item model predicate) and per inventory slot, so it only builds strings when the name changes.
	public static boolean isHandheldAtlas(ItemStack stack) {
		if (!stack.is(Items.BOOK)) return false;
		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		if (customName != null) return isAtlasName(customName);
		Component itemName = stack.get(DataComponents.ITEM_NAME);
		return itemName != null && itemName.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(ATLAS_KEY);
	}

	private static boolean isAtlasName(Component name) {
		String language = Minecraft.getInstance().getLanguageManager().getSelected();
		if (name == lastCustomName && language.equals(lastLanguage)) return lastCustomNameIsAtlas;
		boolean atlas;
		if (name.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(ATLAS_KEY)) {
			atlas = true;
		} else {
			String text = name.getString().toLowerCase(Locale.ROOT);
			String translated = I18n.get(ATLAS_KEY).toLowerCase(Locale.ROOT);
			atlas = text.contains(translated) || ATLAS_NAMES.stream().anyMatch(n -> text.contains(n.toLowerCase(Locale.ROOT)));
		}
		lastCustomName = name;
		lastLanguage = language;
		lastCustomNameIsAtlas = atlas;
		return atlas;
	}

	public static int handheldZoom() {
		return handheldZoom;
	}

	static int nextHandheldZoom(int zoom) {
		return zoom >= 4 ? 1 : zoom * 2;
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

	private static int handheldZoom = 1;
	private static long clientTicks = 0;
	private static volatile boolean rebuildPending = false;
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
		// Fix: the mod list's Config button was disabled; NeoForge builds the screen from the config spec.
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		modBus.addListener(ModConfigEvent.Loading.class, e -> onConfig(e.getConfig()));
		modBus.addListener(ModConfigEvent.Reloading.class, e -> onConfig(e.getConfig()));
		AntiqueAtlasKeybindings.init(modBus);
		modBus.addListener(RegisterClientReloadListenersEvent.class, e -> {
			e.registerReloadListener(TileTextures.getInstance());
			e.registerReloadListener(MarkerTextures.getInstance());
			e.registerReloadListener(StructureTileProviders.getInstance());
			e.registerReloadListener(BiomeTileProviders.getInstance());
			e.registerReloadListener(FeatureRules.getInstance());
			e.registerReloadListener(DimensionConfigs.getInstance());
			// Fix (F3+T): rebuild the atlas from the reloaded resources on the next client tick, after every listener applied.
			e.registerReloadListener((ResourceManagerReloadListener) manager -> rebuildPending = true);
		});
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, e -> {
			if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.insertAfter(Items.MAP.getDefaultInstance(), getHandheldAtlas(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		});
		modBus.addListener(ModelEvent.RegisterAdditional.class, e -> e.register(ATLAS_MODEL));
		// Optional HUD minimap (antique-atlas#254); draws nothing unless enabled in the config
		modBus.addListener(RegisterGuiLayersEvent.class, e -> e.registerBelow(VanillaGuiLayers.CHAT, id("minimap"), MinimapLayer::render));
		NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickItem.class, e -> {
			if (e.getLevel().isClientSide() && e.getEntity().isShiftKeyDown() && isHandheldAtlas(e.getItemStack())) {
				// Sneak + use zooms the handheld atlas out: 1, 2, 4 chunks per tile
				handheldZoom = nextHandheldZoom(handheldZoom);
				if (AtlasDebug.on) AtlasDebug.log("Handheld atlas zoom: {} chunks per tile", handheldZoom);
				e.getEntity().displayClientMessage(Component.translatable("gui.antique_atlas.handheldZoom", handheldZoom), true);
				e.setCancellationResult(InteractionResult.SUCCESS);
				e.setCanceled(true);
			} else if (e.getLevel().isClientSide() && isHandheldAtlas(e.getItemStack()) && openAtlasScreen() != null) {
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
		// Fix: after a share group change, tiles the new group hasn't explored stayed on the map.
		SurveyorClientEvents.Register.explorationReset(id("world_data"), () -> onClientThread(WorldAtlasData::rebuildAll));
		SurveyorClientEvents.Register.explorationReset(id("debug"), () -> {
			if (AtlasDebug.on) AtlasDebug.log("Share group changed: rebuilding the atlas from the new exploration");
		});
		NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, e -> {
			if (e.getLevel() instanceof ClientLevel && WorldAtlasData.hasPendingWork()) SurveyorClient.getSummaries(Minecraft.getInstance().getConnection()).values().forEach(s -> WorldAtlasData.getOrCreate(s.dimension()).tick(s));
		});
		// Fix: only the client cause, which runs on the client thread.
		NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, e -> {
			if (e.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) BiomeTileProviders.getInstance().registerFallbacks(e.getRegistryAccess().registryOrThrow(Registries.BIOME));
		});
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> resetSession());
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
			clientTicks++;
			if (AtlasDebug.on) AtlasDebug.tick(clientTicks);
			if (rebuildPending) {
				rebuildPending = false;
				if (AtlasDebug.on) AtlasDebug.log("Resources reloaded: rebuilding the atlas");
				WorldAtlasData.rebuildAll();
			}
		});

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
