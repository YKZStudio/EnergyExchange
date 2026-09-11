package studio.ykz.energyexchange.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/** Offline, bounded matching of literal characters and any Mandarin reading.
 * Each syllable accepts a prefix, so full spelling and initials can be mixed.
 * Dynamic programming avoids enumerating exponentially many pronunciations. */
public final class PinyinSearch {
    private PinyinSearch() {}
    private record Token(String literal, List<String> readings) {}
    private static final Map<String, List<Token>> CACHE = new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, List<Token>> e) { return size() > 4096; }
    };
    private static final class Dictionary {
        static final Map<Integer, List<String>> READINGS = load();
        private static Map<Integer, List<String>> load() {
            var result = new HashMap<Integer, List<String>>();
            var stream = PinyinSearch.class.getResourceAsStream("/energyexchange/pinyin.tsv");
            if (stream == null) throw new IllegalStateException("Missing bundled Mandarin index");
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.US_ASCII))) {
                for (String line; (line = reader.readLine()) != null;) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] parts = line.split("\t", 2);
                    result.put(Integer.parseInt(parts[0], 16), List.of(parts[1].split(",")));
                }
            } catch (IOException | RuntimeException e) { throw new IllegalStateException("Invalid bundled Mandarin index", e); }
            return Map.copyOf(result);
        }
    }
    public static String normalize(String text) {
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD)
                .replace("u:", "u\u0308").replace("u\u0308", "v");
        var result = new StringBuilder();
        decomposed.codePoints().filter(Character::isLetterOrDigit).forEach(result::appendCodePoint);
        return result.toString();
    }
    public static synchronized boolean matches(String name, String query) {
        // Bound raw input before normalization, including strings made only of separators.
        if (query.length() > 128 || name.length() > 512) return false;
        String q = normalize(query);
        if (q.isEmpty()) return true;
        List<Token> tokens = CACHE.computeIfAbsent(name, text -> normalize(text).codePoints()
                .mapToObj(cp -> new Token(new String(Character.toChars(cp)), Dictionary.READINGS.getOrDefault(cp, List.of())))
                .toList());
        boolean[] positions = new boolean[q.length() + 1];
        for (Token token : tokens) {
            positions[0] = true; // A query may start at any character, but cannot skip interior characters.
            boolean[] next = new boolean[q.length() + 1];
            for (int pos = 0; pos < q.length(); pos++) if (positions[pos]) {
                if (q.startsWith(token.literal(), pos)) next[pos + token.literal().length()] = true;
                for (String syllable : token.readings()) {
                    for (int n = 1; n <= syllable.length() && pos + n <= q.length(); n++) {
                        if (q.charAt(pos + n - 1) != syllable.charAt(n - 1)) break;
                        next[pos + n] = true;
                    }
                }
            }
            if (next[q.length()]) return true;
            positions = next;
        }
        return false;
    }
}
