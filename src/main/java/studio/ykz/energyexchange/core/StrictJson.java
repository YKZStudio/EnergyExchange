package studio.ykz.energyexchange.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;

/** Reject duplicate keys and lenient syntax before Gson builds a tree.
 * 构建 JSON 树之前拒绝重复键及宽松语法。 */
final class StrictJson {
    private StrictJson() {}

    static JsonElement parse(String raw) {
        try (var reader = new JsonReader(new StringReader(raw))) {
            reader.setStrictness(Strictness.STRICT);
            validate(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing JSON / JSON 尾部存在多余内容");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid JSON / JSON 无效", exception);
        }
        return JsonParser.parseString(raw);
    }

    private static void validate(JsonReader reader, int depth) throws IOException {
        if (depth > 8) throw new IllegalArgumentException("JSON nesting too deep / JSON 嵌套过深");
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject();
                var keys = new HashSet<String>();
                while (reader.hasNext()) {
                    if (!keys.add(reader.nextName())) throw new IllegalArgumentException("Duplicate JSON key / JSON 键重复");
                    validate(reader, depth + 1);
                }
                reader.endObject();
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                while (reader.hasNext()) validate(reader, depth + 1);
                reader.endArray();
            }
            case STRING, NUMBER -> reader.nextString();
            case BOOLEAN -> reader.nextBoolean();
            case NULL -> reader.nextNull();
            default -> throw new IllegalArgumentException("Invalid JSON token / JSON 标记无效");
        }
    }
}
