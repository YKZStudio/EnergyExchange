package studio.ykz.energyexchange.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    @Test void bothLanguagesHaveMatchingKeysAndPlaceholders() throws Exception {
        JsonObject english = language("en_us"), chinese = language("zh_cn");
        assertEquals(english.keySet(), chinese.keySet());
        for (String key : english.keySet()) {
            String en = english.get(key).getAsString(), zh = chinese.get(key).getAsString();
            assertFalse(en.isBlank(), key);
            assertFalse(zh.isBlank(), key);
            assertEquals(en.split("%s", -1).length, zh.split("%s", -1).length, key);
        }
    }
    private static JsonObject language(String name) throws Exception {
        try (var reader = new InputStreamReader(LocalizationTest.class.getResourceAsStream(
                "/assets/energyexchange/lang/" + name + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
