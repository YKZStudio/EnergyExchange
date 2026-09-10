package studio.ykz.energyexchange.core;

import java.math.BigInteger;
import java.util.Set;

public record ValueRule(BigInteger value, boolean enabled) {
    public static ValueRule parse(String json) {
        var object = StrictJson.parse(json).getAsJsonObject();
        if (object.keySet().stream().anyMatch(k -> !Set.of("value", "enabled").contains(k))) {
            throw new IllegalArgumentException("Unknown rule field / 未知规则字段");
        }
        boolean enabled = true;
        if (object.has("enabled")) {
            var flag = object.getAsJsonPrimitive("enabled");
            if (!flag.isBoolean()) throw new IllegalArgumentException("enabled must be boolean / enabled 必须为布尔值");
            enabled = flag.getAsBoolean();
        }
        if (!enabled) {
            if (object.has("value")) throw new IllegalArgumentException("Disabled rule must omit value / 禁用规则不能含 value");
            return new ValueRule(BigInteger.ZERO, false);
        }
        var number = object.getAsJsonPrimitive("value");
        if (number == null || !number.isString()) throw new IllegalArgumentException("value must be a decimal string / value 必须为十进制字符串");
        var value = Energy.parse(number.getAsString());
        if (value.signum() <= 0) throw new IllegalArgumentException("value must be positive / value 必须大于零");
        return new ValueRule(value, true);
    }
}
