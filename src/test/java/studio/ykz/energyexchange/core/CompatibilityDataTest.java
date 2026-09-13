package studio.ykz.energyexchange.core;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityDataTest {
    private static final Map<String, Integer> MODULES = Map.of(
            "farmersdelight", 186, "moredelight", 33, "rusticdelight", 154,
            "ubesdelight", 122, "betternether", 708, "betterend", 850);

    @Test
    void ecosystemProfilesAreCompleteAndSafeForBothTargets() throws Exception {
        var common = Path.of("src/main/resources/data/energyexchange/energyexchange/compat/modules");
        var older = Path.of("versions/26.1.2/resources/data/energyexchange/energyexchange/compat/modules");
        for (var module : MODULES.entrySet()) {
            JsonObject current = validate(common.resolve(module.getKey() + ".json"), module.getKey(), module.getValue());
            JsonObject legacy = validate(older.resolve(module.getKey() + ".json"), module.getKey(), module.getValue());
            assertEquals(current.getAsJsonObject("values").keySet(), legacy.getAsJsonObject("values").keySet());
        }
    }

    private static JsonObject validate(Path path, String namespace, int expected) throws Exception {
        JsonObject data = StrictJson.parse(Files.readString(path)).getAsJsonObject();
        assertEquals(java.util.Set.of("values", "salvage"), data.keySet());
        JsonObject values = data.getAsJsonObject("values"), salvage = data.getAsJsonObject("salvage");
        assertEquals(expected, values.size());
        for (var entry : values.entrySet()) {
            assertTrue(entry.getKey().startsWith(namespace + ":"));
            assertTrue(new BigInteger(entry.getValue().getAsString()).signum() > 0);
        }
        for (var entry : salvage.entrySet()) {
            assertTrue(values.has(entry.getKey()));
            var pair = entry.getValue().getAsJsonArray();
            assertEquals(2, pair.size());
            BigInteger numerator = new BigInteger(pair.get(0).getAsString());
            BigInteger denominator = new BigInteger(pair.get(1).getAsString());
            BigInteger price = new BigInteger(values.get(entry.getKey()).getAsString());
            assertTrue(numerator.signum() > 0 && denominator.signum() > 0);
            assertTrue(numerator.compareTo(price.multiply(denominator)) <= 0);
        }
        return data;
    }
}
