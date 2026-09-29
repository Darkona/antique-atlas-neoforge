package folk.sisby.antique_atlas.reloader;

import folk.sisby.antique_atlas.AtlasDebug;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.util.CodecUtil;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MarkerTextures extends SimplePreparableReloadListener<Map<ResourceLocation, MarkerTextures.MarkerTextureMeta>> {
	public static final MarkerTextures INSTANCE = new MarkerTextures();
	public static final ResourceLocation ID = AntiqueAtlas.id("marker_textures");

	public static MarkerTextures getInstance() {
		return INSTANCE;
	}

	protected final Map<ResourceLocation, MarkerTexture> textures = new HashMap<>();
	// antique-atlas#350: textures for landmarks that only carry an item stack (e.g. waypoints made by other mods)
	protected final Map<ResourceLocation, MarkerTexture> itemTextures = new HashMap<>();
	protected final List<Pair<TagKey<Item>, MarkerTexture>> tagTextures = new ArrayList<>();

	public MarkerTexture get(ResourceLocation id) {
		return textures.get(id);
	}

	public MarkerTexture getOrDefault(ResourceLocation id) {
		return getOrDefault(id, MarkerTexture.DEFAULT);
	}

	public MarkerTexture getOrDefault(ResourceLocation id, MarkerTexture defaultTexture) {
		return textures.getOrDefault(id, defaultTexture);
	}

	public ResourceLocation minimumId(ResourceLocation id) {
		while (get(id) == null && id.getPath().contains("/")) {
			id = id.withPath(id.getPath().substring(0, id.getPath().lastIndexOf('/')));
		}
		return get(id) == null ? id.withPath("default") : id;
	}

	public MarkerTexture fromLandmark(Landmark landmark) {
		ResourceLocation id = minimumId(landmark.id());
		if (id.getPath().equals("default") && landmark.contains(LandmarkComponentTypes.STACK)) { // no texture of its own
			MarkerTexture texture = fromStack(landmark.get(LandmarkComponentTypes.STACK));
			if (texture != null) {
				if (AtlasDebug.on) AtlasDebug.log("Marker {} of {} drawn with the texture of its item {}: {}", landmark.id(), landmark.owner(), landmark.get(LandmarkComponentTypes.STACK).getItem(), texture.keyId());
				AtlasDebug.count(AtlasDebug.Count.MARKERS_ITEM_TEXTURE);
				return texture;
			}
		}
		if (id.getPath().equals("default")) AtlasDebug.count(AtlasDebug.Count.MARKERS_DEFAULT_TEXTURE);
		return getOrDefault(id);
	}

	public @Nullable MarkerTexture fromStack(ItemStack stack) {
		if (stack.isEmpty()) return null;
		MarkerTexture texture = itemTextures.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
		if (texture != null) return texture;
		for (Pair<TagKey<Item>, MarkerTexture> entry : tagTextures) {
			if (stack.is(entry.getFirst())) return entry.getSecond();
		}
		return null;
	}

	public MarkerTexture fromLandmark(Landmark landmark, String variant) {
		ResourceLocation id = minimumId(landmark.id());
		return getOrDefault(id.withPath(p -> p + "/" + variant), getOrDefault(id));
	}

	public Map<ResourceLocation, MarkerTexture> asMap() {
		return new HashMap<>(textures);
	}

	@Override
	protected Map<ResourceLocation, MarkerTextureMeta> prepare(ResourceManager manager, ProfilerFiller profiler) {
		Map<ResourceLocation, MarkerTextures.MarkerTextureMeta> textureMeta = new HashMap<>();
		for (Map.Entry<ResourceLocation, Resource> e : manager.listResources("textures/atlas/marker", id -> id.getPath().endsWith(".png")).entrySet()) {
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(e.getKey().getNamespace(), e.getKey().getPath().substring("textures/atlas/marker/".length(), e.getKey().getPath().length() - ".png".length()));
			try {
				ResourceMetadata metadata = e.getValue().metadata();
				textureMeta.put(id, metadata.getSection(MarkerTextures.MarkerTextureMeta.METADATA).orElse(MarkerTextureMeta.DEFAULT));
			} catch (IOException | RuntimeException ex) { // Fix: a malformed .mcmeta threw past this and failed the whole reload
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Failed to access marker texture metadata for {}", e.getKey(), ex);
				textureMeta.put(id, MarkerTextures.MarkerTextureMeta.DEFAULT);
			}
		}
		return textureMeta;
	}

	@Override
	protected void apply(Map<ResourceLocation, MarkerTextureMeta> prepared, ResourceManager manager, ProfilerFiller profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Marker Textures...");
		textures.clear();
		itemTextures.clear();
		tagTextures.clear();
		prepared.forEach((id, meta) -> {
			if (id.getPath().endsWith("_accent")) {
				ResourceLocation mainId = id.withPath(s -> s.substring(0, s.length() - "_accent".length()));
				MarkerTextureMeta main = prepared.get(mainId);
				if (main != null) {
					textures.put(mainId, main.build(mainId, true));
				} else {
					AntiqueAtlas.LOGGER.error("[Antique Atlas] Marker accent {} has no main texture! Discarding.", id);
				}
			}
		});
		prepared.forEach((id, meta) -> {
			if (!textures.containsKey(id) && !id.getPath().endsWith("_accent")) {
				textures.put(id, meta.build(id, false));
			}
		});
		// Items and tags that pick each texture: its picker item, plus any listed under "items"
		prepared.keySet().stream().sorted().forEach(id -> {
			MarkerTextureMeta meta = prepared.get(id);
			MarkerTexture texture = textures.get(id);
			if (texture == null) return;
			meta.item().ifPresent(item -> itemTextures.putIfAbsent(item, texture));
			for (String entry : meta.items().orElse(List.of())) {
				if (entry.startsWith("#")) {
					ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
					if (tag != null) tagTextures.add(Pair.of(TagKey.create(Registries.ITEM, tag), texture));
				} else {
					ResourceLocation item = ResourceLocation.tryParse(entry);
					if (item != null) itemTextures.put(item, texture);
				}
			}
		});
	}


	public record MarkerTextureMeta(Optional<ResourceLocation> item, Optional<List<String>> items, Optional<Integer> textureWidth, Optional<Integer> textureHeight, Optional<Integer> mipLevels, Optional<Integer> offsetX, Optional<Integer> offsetY, Optional<Integer> nearClip, Optional<Integer> farClip) {
		public static final MarkerTextureMeta DEFAULT = new MarkerTextureMeta(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

		public static final Codec<MarkerTextureMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.optionalFieldOf("item").forGetter(MarkerTextureMeta::item),
			Codec.STRING.listOf().optionalFieldOf("items").forGetter(MarkerTextureMeta::items),
			Codec.INT.optionalFieldOf("textureWidth").forGetter(MarkerTextureMeta::textureWidth),
			Codec.INT.optionalFieldOf("textureHeight").forGetter(MarkerTextureMeta::textureHeight),
			Codec.INT.optionalFieldOf("mipLevels").forGetter(MarkerTextureMeta::mipLevels),
			Codec.INT.optionalFieldOf("offsetX").forGetter(MarkerTextureMeta::offsetX),
			Codec.INT.optionalFieldOf("offsetY").forGetter(MarkerTextureMeta::offsetY),
			Codec.INT.optionalFieldOf("nearClip").forGetter(MarkerTextureMeta::nearClip),
			Codec.INT.optionalFieldOf("farClip").forGetter(MarkerTextureMeta::farClip)
		).apply(instance, MarkerTextureMeta::new));

		public static final MetadataSectionSerializer<MarkerTextureMeta> METADATA = new CodecUtil.CodecResourceMetadataSerializer<>(CODEC, AntiqueAtlas.id("marker"));

		public MarkerTexture build(ResourceLocation id, boolean accent) {
			int textureWidth = this.textureWidth.orElse(32);
			int textureHeight = this.textureHeight.orElse(32);
			int mipLevels = this.mipLevels.orElse(0);
			int offsetX = this.offsetX.orElse(-textureWidth / 2);
			int offsetY = this.offsetY.orElse(-textureHeight / 2);
			int nearClip = this.nearClip.orElse(1);
			int farClip = this.farClip.orElse(Integer.MAX_VALUE);
			ResourceLocation item = this.item.orElse(null);
			return MarkerTexture.ofId(id, item, offsetX, offsetY, textureWidth, textureHeight, mipLevels, nearClip, farClip, accent);
		}
	}
}
