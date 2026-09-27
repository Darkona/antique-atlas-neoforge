package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasConfig;
import folk.sisby.antique_atlas.MarkerTexture;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * antique-atlas#171: structure icons can be picked for your own markers, per the pickStructureMarkers option.
 */
class MarkerModalTest {
	private static MarkerTexture texture(String path) {
		return new MarkerTexture(ResourceLocation.fromNamespaceAndPath(AntiqueAtlas.ID, "textures/atlas/marker/" + path + ".png"), null, null, 0, 0, 32, 32, 0, 1, Integer.MAX_VALUE);
	}

	@Test
	@DisplayName("Custom icons are always pickable, structure icons follow the option, others never")
	void pickableRule() {
		AntiqueAtlasConfig.StructureMarkerPicking old = AntiqueAtlas.CONFIG.pickStructureMarkers;
		try {
			AntiqueAtlas.CONFIG.pickStructureMarkers = AntiqueAtlasConfig.StructureMarkerPicking.ON;
			assertTrue(MarkerModal.isPickable(texture("custom/tower")));
			assertTrue(MarkerModal.isPickable(texture("structure/village")));
			assertFalse(MarkerModal.isPickable(texture("landmark/death")));
			AntiqueAtlas.CONFIG.pickStructureMarkers = AntiqueAtlasConfig.StructureMarkerPicking.OFF;
			assertTrue(MarkerModal.isPickable(texture("custom/tower")));
			assertFalse(MarkerModal.isPickable(texture("structure/village")));
		} finally {
			AntiqueAtlas.CONFIG.pickStructureMarkers = old;
		}
	}
}
