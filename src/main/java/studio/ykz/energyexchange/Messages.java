package studio.ykz.energyexchange;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class Messages {
    private static final JsonObject ENGLISH;
    static {
        try (var reader = new InputStreamReader(Objects.requireNonNull(Messages.class.getResourceAsStream(
                "/assets/energyexchange/lang/en_us.json")), StandardCharsets.UTF_8)) {
            ENGLISH = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
    private Messages() {}
    public static Component text(String key, Object... args) {
        String full = key.startsWith("energyexchange.") ? key : "energyexchange." + key;
        String fallback = ENGLISH.has(full) ? ENGLISH.get(full).getAsString() : ENGLISH.get("energyexchange.error.internal").getAsString();
        return Component.translatableWithFallback(full, fallback, args);
    }
}
