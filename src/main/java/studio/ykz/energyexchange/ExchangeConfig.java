package studio.ykz.energyexchange;

import net.fabricmc.loader.api.FabricLoader;
import studio.ykz.energyexchange.core.Energy;
import studio.ykz.energyexchange.core.StrictJson;
import java.nio.file.*;
import java.math.BigInteger;

public record ExchangeConfig(boolean showUnlearned, boolean compactNumbers, boolean xpEnabled, String xpCost) {
    public static final ExchangeConfig DEFAULT = new ExchangeConfig(true, true, true, "128");
    public static ExchangeConfig server = DEFAULT;
    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("energyexchange.json");
    public ExchangeConfig {
        if (Energy.parse(xpCost).signum() <= 0) throw new IllegalArgumentException("energyexchange.error.amount");
    }
    public static ExchangeConfig load() {
        if (!Files.exists(PATH)) return DEFAULT;
        try {
            if (Files.size(PATH) > 4096) throw new IllegalArgumentException();
            var j = StrictJson.parse(Files.readString(PATH)).getAsJsonObject();
            return new ExchangeConfig(j.get("showUnlearned").getAsBoolean(), j.get("compactNumbers").getAsBoolean(),
                    j.get("xpEnabled").getAsBoolean(), j.get("xpCost").getAsString());
        } catch (Exception e) { throw new IllegalStateException("Invalid config / 配置无效: " + PATH, e); }
    }
    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            var j = new com.google.gson.JsonObject();
            j.addProperty("showUnlearned", showUnlearned); j.addProperty("compactNumbers", compactNumbers);
            j.addProperty("xpEnabled", xpEnabled); j.addProperty("xpCost", xpCost);
            Path tmp = PATH.resolveSibling("energyexchange.json.tmp"); Files.writeString(tmp, j.toString());
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot save config / 无法保存配置", e); }
    }
}
