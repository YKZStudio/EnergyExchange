package studio.ykz.energyexchange.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrictJsonTest {
    @ParameterizedTest @ValueSource(strings = {"{\"value\":\"1\",\"value\":\"2\"}",
            "{value:'1'}", "{/* comment */\"value\":\"1\"}", "{\"value\":\"1\",}",
            "{\"value\":\"1\"} {}", "[[[[[[[[[[0]]]]]]]]]]"})
    void rejectsAmbiguousSyntaxAndExcessiveNesting(String text) {
        assertThrows(IllegalArgumentException.class, () -> StrictJson.parse(text));
    }
}
