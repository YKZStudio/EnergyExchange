package studio.ykz.energyexchange.core;

import com.github.promeg.pinyinhelper.Pinyin;
import java.text.Normalizer;
import java.util.*;

/** Bounded dynamic matching: each syllable accepts its full spelling or a prefix.
 * No exponential enumeration of mixed initials/full spellings. */
public final class PinyinSearch {
    private PinyinSearch() {}
    private static final Map<String, List<String>> CACHE = new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, List<String>> e) { return size() > 4096; }
    };
    public static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT).replace("u:", "v").replace('ü', 'v'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}|[\\s'’_-]", "");
    }
    public static synchronized boolean matches(String name, String query) {
        String q = normalize(query);
        if (q.isEmpty()) return true;
        if (q.length() > 128 || name.length() > 512) return false;
        List<String> syllables = CACHE.computeIfAbsent(name, text -> {
            var result = new ArrayList<String>();
            for (char c : text.toCharArray()) {
                String syllable = normalize(Pinyin.toPinyin(c));
                if (!syllable.isEmpty()) result.add(syllable);
            }
            return List.copyOf(result);
        });
        boolean[] positions = new boolean[q.length() + 1];
        for (String syllable : syllables) {
            positions[0] = true; // Match may begin at any character, e.g. yingzhenzhu.
            boolean[] next = new boolean[q.length() + 1];
            for (int pos = 0; pos < q.length(); pos++) if (positions[pos]) {
                for (int n = 1; n <= syllable.length() && pos + n <= q.length(); n++) {
                    if (q.charAt(pos + n - 1) != syllable.charAt(n - 1)) break;
                    if (pos + n == q.length()) return true;
                    next[pos + n] = true;
                }
            }
            positions = next;
        }
        return false;
    }
}
