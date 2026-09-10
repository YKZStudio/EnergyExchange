package studio.ykz.energyexchange.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;

class ValueRuleTest {
    @Test void acceptsExactStringAndExplicitDisable() {
        assertEquals(BigInteger.valueOf(8192), ValueRule.parse("{\"value\":\"8192\"}").value());
        assertFalse(ValueRule.parse("{\"enabled\":false}").enabled());
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "[]", "null", "{\"value\":1}", "{\"value\":\"0\"}",
            "{\"value\":\"-1\"}", "{\"value\":\"1.1\"}", "{\"value\":\"1e8\"}", "{\"value\":\"1\",\"typo\":true}",
            "{\"enabled\":\"false\"}", "{\"enabled\":false,\"value\":\"1\"}"})
    void rejectsAmbiguousAndUnsafeRules(String json) { assertThrows(RuntimeException.class, () -> ValueRule.parse(json)); }
}
