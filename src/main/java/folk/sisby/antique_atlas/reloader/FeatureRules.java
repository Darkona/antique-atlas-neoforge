package folk.sisby.antique_atlas.reloader;

import folk.sisby.antique_atlas.AtlasDebug;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.FeatureRuleSet;
import folk.sisby.antique_atlas.TerrainTiling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Which tile marks water, ice, lava and similar features (antique-atlas#318). One rule per {@code atlas/features/*.json}.
 */
public class FeatureRules extends SimpleJsonResourceReloadListener {
	public static final FeatureRules INSTANCE = new FeatureRules();
	public static final ResourceLocation ID = AntiqueAtlas.id("features");

	public static FeatureRules getInstance() {
		return INSTANCE;
	}

	private volatile FeatureRuleSet rules = FeatureRuleSet.empty(TerrainTiling.CUSTOM_TILES);

	public FeatureRules() {
		super(new Gson(), "atlas/features");
	}

	public FeatureRuleSet rules() {
		return rules;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Feature Rules...");
		Map<ResourceLocation, FeatureRuleSet.Rule> parsed = new HashMap<>();
		prepared.forEach((id, json) -> FeatureRuleSet.Rule.CODEC.parse(JsonOps.INSTANCE, json)
			.resultOrPartial(error -> AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading feature rule {}: {}", id, error))
			.ifPresent(rule -> parsed.put(id, rule)));
		rules = FeatureRuleSet.compile(TerrainTiling.CUSTOM_TILES, parsed, error -> AntiqueAtlas.LOGGER.error("[Antique Atlas] {}", error));
		if (AtlasDebug.on) AtlasDebug.log("Feature rules: {} files read, {} feature tiles ({})", parsed.size(), rules.customTileCount(), parsed.keySet());
	}
}
