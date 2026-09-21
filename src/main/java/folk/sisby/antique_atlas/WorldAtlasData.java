package folk.sisby.antique_atlas;

import com.google.common.collect.Multimap;
import folk.sisby.antique_atlas.gui.AtlasScreen;
import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.reloader.StructureTileProviders;
import folk.sisby.antique_atlas.reloader.TileTextures;
import folk.sisby.antique_atlas.util.Rect;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentMap;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.util.RegionPos;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import folk.sisby.antique_atlas.gui.MarkerGeometry;
import java.util.Collections;
import java.util.TreeMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

public class WorldAtlasData {
	public static final Map<ResourceKey<Level>, WorldAtlasData> WORLDS = new HashMap<>();

	public static WorldAtlasData getOrCreate(ResourceKey<Level> dimension) {
		return WORLDS.computeIfAbsent(dimension, k -> new WorldAtlasData());
	}

	/**
	 * Fix: after a resource reload (F3+T) the atlas kept tiles and markers from the old resources until you rejoined.
	 * Drops every dimension's tiles and markers and resolves them again from Surveyor's data. Client thread only.
	 */
	public static void rebuildAll() {
		WORLDS.clear();
		TerrainTiling.clearCaches();
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null || Minecraft.getInstance().level == null) return;
		BiomeTileProviders.getInstance().clearFallbacks();
		BiomeTileProviders.getInstance().registerFallbacks(connection.registryAccess().registryOrThrow(Registries.BIOME));
		SurveyorExploration exploration = SurveyorClient.getExploration();
		for (WorldSummary summary : SurveyorClient.getSummaries(connection).values()) {
			WorldAtlasData data = getOrCreate(summary.dimension());
			if (summary.terrain() != null) data.onTerrainUpdated(summary, summary.terrain().bitSet(exploration));
			if (summary.structures() != null) data.onStructuresAdded(summary, summary.structures().keySet(exploration));
			if (summary.landmarks() != null) data.onLandmarksAdded(summary, summary.landmarks().keySet(exploration));
		}
	}

	public static boolean hasPendingWork() {
		for (WorldAtlasData data : WORLDS.values()) {
			if (!data.isFinished || !data.terrainDeque.isEmpty()) return true;
		}
		return false;
	}

	public static boolean isEmpty(ResourceKey<Level> dimension) {
		return !WORLDS.containsKey(dimension) || WORLDS.get(dimension).isEmpty();
	}

	protected final Long2ObjectOpenHashMap<TileTexture> biomeTiles = new Long2ObjectOpenHashMap<>();
	protected final Long2ObjectOpenHashMap<TileTexture> structureTiles = new Long2ObjectOpenHashMap<>();
	protected final Map<UUID, Map<ResourceLocation, Pair<Landmark, MarkerTexture>>> landmarkMarkers = new ConcurrentHashMap<>();
	protected final Map<Landmark, MarkerTexture> structureMarkers = new ConcurrentHashMap<>();

	private static final Comparator<Landmark> EDITABLE_ORDER = Comparator.comparing(Landmark::id).thenComparing(Landmark::owner);
	private static final Comparator<Map.Entry<Landmark, MarkerTexture>> MARKER_DRAW_ORDER = Comparator.comparing((Map.Entry<Landmark, MarkerTexture> e) -> e.getValue().id());
	protected int markerVersion = 0;
	private int snapshotVersion = -1;
	private int snapshotTileChunks = -1;
	private Map<Landmark, MarkerTexture> markerSnapshot = Map.of();
	private final Reference2ObjectOpenHashMap<Landmark, MarkerGeometry> markerGeometry = new Reference2ObjectOpenHashMap<>();

	protected final Rect tileScope = new Rect(0, 0, 0, 0);
	protected final Set<ChunkPos> terrainDequeHash = new HashSet<>();
	protected final Deque<ChunkPos> terrainDeque = new ConcurrentLinkedDeque<>();
	protected boolean isFinished = false;

	// Debug Display Info
	protected final Map<ChunkPos, String> debugBiomePredicates = new HashMap<>();
	protected final Map<ChunkPos, String> debugStructurePredicates = new HashMap<>();
	protected final Map<ChunkPos, TerrainTileProvider> debugBiomes = new HashMap<>();
	protected final Map<ChunkPos, StructureTileProvider> debugStructures = new HashMap<>();


	private boolean isEmpty() {
		// Fix: a dimension explored along a single chunk row counted as empty.
		return biomeTiles.isEmpty() && terrainDequeHash.isEmpty();
	}

	public void onTerrainUpdated(WorldSummary summary, Map<RegionPos, BitSet> chunks) {
		// Fix: re-tile chunks whose terrain changed, not only new ones.
		for (ChunkPos pos : RegionPos.regionsToChunks(chunks)) {
			if (!terrainDequeHash.contains(pos)) {
				terrainDequeHash.add(pos);
				terrainDeque.add(pos);
			}
		}
	}

	public void onStructuresAdded(WorldSummary summary, Multimap<ResourceKey<Structure>, ChunkPos> starts) {
		starts.forEach((key, pos) -> StructureTileProviders.getInstance().resolve(structureTiles, debugStructures, debugStructurePredicates, structureMarkers, summary, key, pos, summary.structures().get(key, pos), summary.structures().getType(key), summary.structures().getTags(key)));
		markersChanged();
	}

	private void markersChanged() {
		markerVersion++;
		markerGeometry.clear();
	}

	public MarkerGeometry markerGeometry(Landmark landmark) {
		MarkerGeometry geometry = markerGeometry.get(landmark);
		if (geometry == null) {
			geometry = MarkerGeometry.of(landmark);
			markerGeometry.put(landmark, geometry);
		}
		return geometry;
	}

	public void tick(WorldSummary summary) {
		if (!BiomeTileProviders.getInstance().hasFallbacks()) return;
		for (int i = 0; i < AntiqueAtlas.CONFIG.chunkTickLimit; i++) {
			ChunkPos pos = terrainDeque.pollFirst();
			terrainDequeHash.remove(pos);
			if (pos == null) break;
			Pair<TerrainTileProvider, TileElevation> tile = summary.dimension() == Level.NETHER ? TerrainTiling.terrainToTileNether(summary, pos) : TerrainTiling.terrainToTile(summary, pos);
			if (tile != null) {
				tileScope.extendTo(pos.x, pos.z);
				biomeTiles.put(pos.toLong(), tile.left().getTexture(pos, tile.right()));
				debugBiomes.put(pos, tile.left());
				debugBiomePredicates.put(pos, tile.right() == null ? null : tile.right().getName());
			} else if (biomeTiles.remove(pos.toLong()) != null) { // the chunk no longer has terrain to draw
				debugBiomes.remove(pos);
				debugBiomePredicates.remove(pos);
			}
		}
		if (!isFinished && terrainDeque.isEmpty()) {
			isFinished = true;
			AntiqueAtlas.LOGGER.info("[Antique Atlas] Finished loading terrain for {} - {} tiles.", summary.dimension(), biomeTiles.size());
		}
	}

	public Rect getScope() {
		return tileScope;
	}

	public TileTexture getTile(int x, int z) {
		long key = ChunkPos.asLong(x, z);
		if (!biomeTiles.containsKey(key)) return AntiqueAtlas.CONFIG.emptyHandling == AntiqueAtlasConfig.EmptyHandling.CLOUDS ? TileTextures.getInstance().getTextures().get(AntiqueAtlas.id("clouds")) : null;
		return structureTiles.containsKey(key) ? structureTiles.get(key) : biomeTiles.get(key);
	}

	public TileTexture getTile(ChunkPos pos) {
		return getTile(pos.x, pos.z);
	}

	public ResourceLocation getProvider(ChunkPos pos) {
		if (structureTiles.containsKey(pos.toLong())) {
			return debugStructures.get(pos).id();
		} else {
			return debugBiomes.containsKey(pos) ? debugBiomes.get(pos).id() : null;
		}
	}

	public String getTilePredicate(ChunkPos pos) {
		if (structureTiles.containsKey(pos.toLong())) {
			return debugStructurePredicates.get(pos);
		} else {
			return debugBiomePredicates.get(pos);
		}
	}

	public void addLandmarkMarker(Landmark landmark, MarkerTexture texture) {
		landmarkMarkers.computeIfAbsent(landmark.owner(), t -> new ConcurrentHashMap<>()).put(landmark.id(), Pair.of(landmark, texture));
		markersChanged();
	}

	public static Landmark copyLandmarkWith(Landmark landmark, ResourceLocation id, Consumer<LandmarkComponentMap> modifier) {
		LandmarkComponentMap copy = LandmarkComponentMap.builder().build();
		landmark.components().keySet().forEach(t -> copy.set(t, landmark.components().get(t)));
		modifier.accept(copy);
		return new Landmark(landmark.owner(), id, copy);
	}

	public void addLandmark(Landmark landmark) {
		if (landmark == null) return;
		if (landmark.id().getPath().startsWith("grave")) {
			AntiqueAtlasConfig.GraveStyle style = AntiqueAtlas.CONFIG.graveStyle;
			Component name = landmark.get(LandmarkComponentTypes.NAME);
			if (name == null && style == AntiqueAtlasConfig.GraveStyle.CAUSE) style = AntiqueAtlasConfig.GraveStyle.DIED;
			MutableComponent timeText = Component.literal(String.valueOf(1 + (landmark.getOrDefault(LandmarkComponentTypes.TIME, 0L) / 24000L))).withStyle(ChatFormatting.WHITE);
			String key = "gui.antique_atlas.marker.death.%s".formatted(style.toString().toLowerCase());
			MutableComponent text = switch (style) {
				case CAUSE -> Component.translatable(key, name.copy().withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.RED), timeText).withStyle(ChatFormatting.GRAY);
				case GRAVE, ITEMS, DIED -> Component.translatable(key, Component.translatable("gui.antique_atlas.marker.death.%s.verb".formatted(style.toString().toLowerCase())).withStyle(ChatFormatting.RED), timeText).withStyle(ChatFormatting.GRAY);
				case EUPHEMISMS -> Component.translatable(key, Component.translatable("gui.antique_atlas.marker.death.%s.verb.%s".formatted(style.toString().toLowerCase(), new Random(landmark.getOrDefault(LandmarkComponentTypes.SEED, 0)).nextInt(11))).withStyle(ChatFormatting.RED), timeText).withStyle(ChatFormatting.GRAY);
			};
			addLandmarkMarker(copyLandmarkWith(landmark, landmark.id(), m -> {
				m.set(LandmarkComponentTypes.COLOR, DyeColor.GRAY.getTextureDiffuseColor());
				m.set(LandmarkComponentTypes.NAME, text);
			}), MarkerTextures.getInstance().fromLandmark(landmark, style == AntiqueAtlasConfig.GraveStyle.ITEMS ? "items" : null));
		} else {
			addLandmarkMarker(landmark, MarkerTextures.getInstance().fromLandmark(landmark));
		}
	}

	public void onLandmarksAdded(WorldSummary summary, Multimap<UUID, ResourceLocation> landmarks) {
		landmarks.forEach((type, pos) -> this.addLandmark(summary.landmarks().get(type, pos)));
		if (Minecraft.getInstance().screen instanceof AtlasScreen as) as.updateBookmarkerList();
	}

	public void onLandmarksRemoved(WorldSummary summary, Multimap<UUID, ResourceLocation> landmarks) {
		landmarks.forEach((type, pos) -> {
			if (landmarkMarkers.containsKey(type)) {
				landmarkMarkers.get(type).remove(pos);
				if (landmarkMarkers.get(type).isEmpty()) landmarkMarkers.remove(type);
			}
		});
		markersChanged();
		if (Minecraft.getInstance().screen instanceof AtlasScreen as) as.updateBookmarkerList();
	}

	public boolean deleteLandmark(ResourceKey<Level> dimension, Landmark landmark) {
		WorldSummary summary = SurveyorClient.tryGetSummary(dimension);
		if (summary == null || summary.landmarks() == null || landmark.owner().equals(WorldLandmarks.GLOBAL) || !SurveyorClient.canModify(landmark.owner())) return false;
		summary.landmarks().remove(landmark.owner(), landmark.id());
		return true;
	}

	// Fix: stable order; Landmark hashes by identity.
	public Map<Landmark, MarkerTexture> getEditableLandmarks() {
		Map<Landmark, MarkerTexture> map = new TreeMap<>(EDITABLE_ORDER);
		landmarkMarkers.forEach((type, landmarks) -> landmarks.forEach((pos, pair) -> { // Don't allow editing global landmarks via GUI.
			if (!pair.left().owner().equals(WorldLandmarks.GLOBAL) && SurveyorClient.canModify(pair.left().owner())) map.put(pair.left(), pair.right());
		}));
		return map;
	}

	public Map<Landmark, MarkerTexture> getAllMarkers(int tileChunks) {
		if (snapshotVersion != markerVersion || snapshotTileChunks != tileChunks) {
			Map<Landmark, MarkerTexture> map = new HashMap<>();
			landmarkMarkers.forEach((type, landmarks) -> landmarks.forEach((pos, pair) -> map.put(pair.left(), pair.right())));
			structureMarkers.forEach((landmark, texture) -> {
				if (tileChunks >= texture.nearClip() && tileChunks <= texture.farClip()) map.put(landmark, texture);
			});
			List<Map.Entry<Landmark, MarkerTexture>> entries = new ArrayList<>(map.entrySet());
			entries.sort(MARKER_DRAW_ORDER);
			Map<Landmark, MarkerTexture> ordered = new LinkedHashMap<>(entries.size() * 2);
			for (Map.Entry<Landmark, MarkerTexture> entry : entries) ordered.put(entry.getKey(), entry.getValue());
			markerSnapshot = Collections.unmodifiableMap(ordered);
			snapshotVersion = markerVersion;
			snapshotTileChunks = tileChunks;
		}
		return markerSnapshot;
	}

	public MarkerTexture getMarkerTexture(Landmark landmark) {
		return landmarkMarkers.containsKey(landmark.owner()) && landmarkMarkers.get(landmark.owner()).containsKey(landmark.id()) ? landmarkMarkers.get(landmark.owner()).get(landmark.id()).right() : structureMarkers.get(landmark);
	}

	public boolean isLoading() {
		return terrainDequeHash.size() > 20;
	}
}
