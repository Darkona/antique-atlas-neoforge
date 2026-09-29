package folk.sisby.antique_atlas.reloader;

import folk.sisby.antique_atlas.AtlasDebug;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.DimensionSettings;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-dimension terrain settings (antique-atlas#77, antique-atlas#249): {@code assets/<namespace>/atlas/dimension/<path>.json}
 * for dimension {@code <namespace>:<path>}. Read once per dimension when its atlas data is created, never per chunk.
 */
public class DimensionConfigs extends SimpleJsonResourceReloadListener {
	public static final DimensionConfigs INSTANCE = new DimensionConfigs();
	public static final ResourceLocation ID = AntiqueAtlas.id("dimensions");

	public static DimensionConfigs getInstance() {
		return INSTANCE;
	}

	private volatile Map<ResourceLocation, DimensionSettings> settings = Map.of();

	public DimensionConfigs() {
		super(new Gson(), "atlas/dimension");
	}

	public DimensionSettings get(ResourceKey<Level> dimension) {
		return settings.getOrDefault(dimension.location(), DimensionSettings.DEFAULT);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Dimension Settings...");
		Map<ResourceLocation, DimensionSettings> parsed = new HashMap<>();
		prepared.forEach((id, json) -> DimensionSettings.CODEC.parse(JsonOps.INSTANCE, json)
			.resultOrPartial(error -> AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading dimension settings {}: {}", id, error))
			.ifPresent(dimension -> parsed.put(id, dimension)));
		settings = Map.copyOf(parsed);
		if (AtlasDebug.on) settings.forEach((id, dimension) -> AtlasDebug.log("Dimension settings {}: {}", id, dimension));
	}
}
