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
    private volatile Map<Identifier, ValueRule> values = Map.of();
    private volatile boolean valid;
    private volatile boolean ready;

    public void load(ResourceManager manager) {
        ready = false;
        valid = false;
        var next = new HashMap<Identifier, ValueRule>();
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
                next.put(item, ValueRule.parse(new String(buffer, 0, count)));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Invalid energy rule / 无效能量规则: " + file, exception);
            }
        }
        values = Map.copyOf(next);
        valid = true;
        EnergyExchange.LOGGER.info("Loaded {} energy rules / 已加载 {} 条能量规则", values.size(), values.size());
    }

    public void pause() { ready = false; }
    public void complete(boolean success) { ready = success && valid; }
    public void clear() { ready = false; valid = false; values = Map.of(); }

    public ValueRule require(Identifier item) {
        if (!ready) throw new IllegalArgumentException("energyexchange.error.rules_unavailable");
        var rule = values.get(item);
        if (rule == null || !rule.enabled()) throw new IllegalArgumentException("energyexchange.error.no_value");
        return rule;
    }
}
