package studio.ykz.energyexchange.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.TreeSet;

/** Raw persistent string keeps corrupt/future data intact instead of silently resetting it.
 * 原始存档字符串保留损坏或未来版本数据，禁止静默清零。 */
public final class AccountJson {
    public static final int MAX_LENGTH = 60_000;
    public static final String EMPTY = "{\"schema\":1,\"energy\":\"0\",\"learned\":[]}";
    private AccountJson() {}

    public static Account read(String raw) {
        try {
            if (raw.length() > MAX_LENGTH) throw new IllegalArgumentException();
            var object = JsonParser.parseString(raw).getAsJsonObject();
            if (!object.keySet().equals(java.util.Set.of("schema", "energy", "learned"))
                    || !object.get("schema").toString().equals("1")
                    || !object.getAsJsonPrimitive("energy").isString()) throw new IllegalArgumentException();
            var ids = new TreeSet<String>();
            var entries = object.getAsJsonArray("learned");
            if (entries.size() > Account.MAX_LEARNED) throw new IllegalArgumentException();
            for (var entry : entries) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) throw new IllegalArgumentException();
                String id = entry.getAsString();
                if (id.length() > 256 || !id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || !ids.add(id)) throw new IllegalArgumentException();
            }
            return new Account(Energy.parse(object.get("energy").getAsString()), ids);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("energyexchange.error.account_corrupt", exception);
        }
    }

    public static String write(Account account) {
        var object = new JsonObject();
        object.addProperty("schema", 1);
        object.addProperty("energy", account.energy().toString());
        var ids = new com.google.gson.JsonArray();
        new TreeSet<>(account.learned()).forEach(ids::add);
        object.add("learned", ids);
        String raw = object.toString();
        if (raw.length() > MAX_LENGTH) throw new IllegalArgumentException("energyexchange.error.knowledge_full");
        return raw;
    }
}
