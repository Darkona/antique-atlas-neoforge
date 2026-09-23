package folk.sisby.antique_atlas.reloader;

import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextureJsonTest {
	@Test
	@DisplayName("An empty texture list is rejected when loading, instead of dividing by zero when drawing")
	void emptyTextureListsRejected() {
		for (String json : new String[]{"[]", "{}"}) {
			assertThrows(IllegalStateException.class, () -> BiomeTileProviders.resolveTextureJson(Map.of(), JsonParser.parseString(json)), json);
		}
		assertNull(BiomeTileProviders.resolveTextureJson(Map.of(), JsonParser.parseString("{\"valley\": \"x\"}")), "an elevation object is resolved by the caller");
	}
}
