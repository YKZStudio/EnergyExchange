package studio.ykz.energyexchange;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import studio.ykz.energyexchange.core.ValueRule;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** One resource location per item; Minecraft resolves pack priority before parsing.
 * 每个物品一个资源位置；Minecraft 先决定数据包覆盖优先级。 */
public final class Rules {
    private static final String DIRECTORY = "energyexchange/values";
    private volatile Map<String, ValueRule> values = Map.of();
    private volatile long revision;
    private volatile Map<String, ValueRule> defaults = Map.of();
    private volatile boolean valid;
    private volatile boolean ready;

    public void load(ResourceManager manager) {
        ready = false;
        valid = false;
        var next = new HashMap<String, ValueRule>();
        try (var reader = manager.getResourceOrThrow(Identifier.parse("energyexchange:energyexchange/defaults.json")).openAsReader()) {
            String raw = readBounded(reader, 2_000_000);
            if (raw.length() > 2_000_000) throw new IllegalArgumentException("Defaults too large");
            var object = studio.ykz.energyexchange.core.StrictJson.parse(raw).getAsJsonObject();
            for (var entry : object.entrySet()) next.put(entry.getKey(), ValueRule.parse("{\"value\":" + entry.getValue() + "}"));
        } catch (IOException exception) { throw new IllegalStateException(exception); }
        defaults = Map.copyOf(next);
        var resources = manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        if (resources.size() > 16384) throw new IllegalStateException("Too many energy rules / 能量规则过多");
        for (var entry : resources.entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath();
            Identifier item = Identifier.fromNamespaceAndPath(file.getNamespace(), path.substring(DIRECTORY.length() + 1, path.length() - 5));
            try (var reader = entry.getValue().openAsReader()) {
                char[] buffer = new char[4097];
                int count = 0, read;
                while (count < buffer.length && (read = reader.read(buffer, count, buffer.length - count)) != -1) count += read;
                if (count > 4096 || item.toString().length() > 256) throw new IllegalArgumentException("Rule too large / 规则过大");
                next.put(item.toString(), ValueRule.parse(new String(buffer, 0, count)));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Invalid energy rule / 无效能量规则: " + file, exception);
            }
        }
        var variants = manager.listResources("energyexchange/variants", id -> id.getPath().endsWith(".json"));
        if (variants.size() > 16384) throw new IllegalStateException("Too many variant rules");
        for (var entry : variants.entrySet()) {
            String path = entry.getKey().getPath().substring("energyexchange/variants/".length());
            String[] parts = path.substring(0, path.length() - 5).split("/", 3);
            if (parts.length != 3) throw new IllegalArgumentException("Invalid variant rule: " + entry.getKey());
            String key = parts[0] + ":" + parts[1] + "#" + entry.getKey().getNamespace() + ":" + parts[2];
            try (var reader = entry.getValue().openAsReader()) { next.put(key, ValueRule.parse(readBounded(reader, 4096))); }
            catch (IOException exception) { throw new IllegalStateException(exception); }
        }
        values = Map.copyOf(next);
        valid = true;
        EnergyExchange.LOGGER.info("Loaded {} energy rules / 已加载 {} 条能量规则", values.size(), values.size());
    }

    private static String readBounded(java.io.Reader reader, int limit) throws IOException {
        var out = new StringBuilder(); char[] buf = new char[4096]; int n;
        while ((n = reader.read(buf)) != -1) { out.append(buf, 0, n); if (out.length() > limit) throw new IOException("Resource too large"); }
        return out.toString();
    }
    public void pause() { ready = false; }
    public void complete(boolean success) { ready = success && valid; revision++; }
    public void clear() { ready = false; valid = false; values = Map.of(); }

    public ValueRule require(Identifier item) {
        if (!ready) throw new IllegalArgumentException("energyexchange.error.rules_unavailable");
        return require(item.toString());
    }

    public long revision() { return revision; }
    public boolean hasDefault(String key) { return defaults.containsKey(key); }
    public ValueRule require(String key) {
        if (!ready) throw new IllegalArgumentException("energyexchange.error.rules_unavailable");
        String base = key.split("#", 2)[0];
        var parent = values.get(base);
        if (parent != null && !parent.enabled()) throw new IllegalArgumentException("energyexchange.error.no_value");
        var rule = values.get(key);
        if (rule == null && key.contains("#") && (base.startsWith("tacz:") || base.startsWith("lrtactical:"))) rule = parent;
        if (rule == null || !rule.enabled()) throw new IllegalArgumentException("energyexchange.error.no_value");
        return rule;
    }
}
