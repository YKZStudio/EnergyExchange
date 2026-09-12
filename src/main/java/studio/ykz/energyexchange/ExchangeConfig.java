package studio.ykz.energyexchange;

import net.fabricmc.loader.api.FabricLoader;
import studio.ykz.energyexchange.core.Energy;
import studio.ykz.energyexchange.core.StrictJson;
import java.nio.file.*;
import java.math.BigInteger;

public record ExchangeConfig(boolean showUnlearned, boolean compactNumbers, boolean xpEnabled, String xpCost, boolean pinyinSearch, studio.ykz.energyexchange.core.CatalogOrder sortOrder) {
    public static final ExchangeConfig DEFAULT = new ExchangeConfig(false, true, true, "128", false, studio.ykz.energyexchange.core.CatalogOrder.ENERGY_DESC);
    public static ExchangeConfig server = DEFAULT;
    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("energyexchange.json");
    public ExchangeConfig(boolean showUnlearned, boolean compactNumbers, boolean xpEnabled, String xpCost, boolean pinyinSearch) {
        this(showUnlearned, compactNumbers, xpEnabled, xpCost, pinyinSearch, studio.ykz.energyexchange.core.CatalogOrder.ENERGY_DESC);
    }
    public ExchangeConfig {
        java.util.Objects.requireNonNull(sortOrder);
        if (Energy.parse(xpCost).signum() <= 0) throw new IllegalArgumentException("energyexchange.error.amount");
    }
    public static ExchangeConfig load() {
        if (!Files.exists(PATH)) return DEFAULT;
        try {
            if (Files.size(PATH) > 4096) throw new IllegalArgumentException();
            var j = StrictJson.parse(Files.readString(PATH)).getAsJsonObject();
            boolean legacy = j.keySet().equals(java.util.Set.of("showUnlearned", "compactNumbers", "xpEnabled", "xpCost"));
            boolean preSort = j.keySet().equals(java.util.Set.of("showUnlearned", "compactNumbers", "xpEnabled", "xpCost", "pinyinSearch"));
            if (!legacy && !preSort && !j.keySet().equals(java.util.Set.of("showUnlearned", "compactNumbers", "xpEnabled", "xpCost", "pinyinSearch", "sortOrder"))) throw new IllegalArgumentException();
            if (!legacy && (!j.get("pinyinSearch").isJsonPrimitive() || !j.getAsJsonPrimitive("pinyinSearch").isBoolean())) throw new IllegalArgumentException();
            for (String field : java.util.List.of("showUnlearned", "compactNumbers", "xpEnabled"))
                if (!j.get(field).isJsonPrimitive() || !j.getAsJsonPrimitive(field).isBoolean()) throw new IllegalArgumentException();
            if (!j.get("xpCost").isJsonPrimitive() || !j.getAsJsonPrimitive("xpCost").isString()) throw new IllegalArgumentException();
            if (j.has("sortOrder") && (!j.get("sortOrder").isJsonPrimitive() || !j.getAsJsonPrimitive("sortOrder").isString())) throw new IllegalArgumentException();
            return new ExchangeConfig(!legacy && j.get("showUnlearned").getAsBoolean(), j.get("compactNumbers").getAsBoolean(),
                    j.get("xpEnabled").getAsBoolean(), j.get("xpCost").getAsString(), !legacy && j.get("pinyinSearch").getAsBoolean(), j.has("sortOrder") ? studio.ykz.energyexchange.core.CatalogOrder.valueOf(j.get("sortOrder").getAsString()) : studio.ykz.energyexchange.core.CatalogOrder.ENERGY_DESC);
        } catch (Exception e) { throw new IllegalStateException("Invalid config / 配置无效: " + PATH, e); }
    }
    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            var j = new com.google.gson.JsonObject();
            j.addProperty("showUnlearned", showUnlearned); j.addProperty("compactNumbers", compactNumbers);
            j.addProperty("sortOrder", sortOrder.name()); j.addProperty("pinyinSearch", pinyinSearch); j.addProperty("xpEnabled", xpEnabled); j.addProperty("xpCost", xpCost);
            Path tmp = PATH.resolveSibling("energyexchange.json.tmp"); Files.writeString(tmp, j.toString());
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot save config / 无法保存配置", e); }
    }
}
