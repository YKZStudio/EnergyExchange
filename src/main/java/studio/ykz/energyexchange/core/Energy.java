package studio.ykz.energyexchange.core;

import java.math.BigInteger;

/** Exact bounded arithmetic / 有界精确整数运算. */
public final class Energy {
    public static final int MAX_DIGITS = 128;
    public static final BigInteger MAX = BigInteger.TEN.pow(MAX_DIGITS).subtract(BigInteger.ONE);

    private Energy() {}

    public static BigInteger parse(String text) {
        if (text == null || text.length() > MAX_DIGITS || !text.matches("0|[1-9][0-9]*")) {
            throw new IllegalArgumentException("energyexchange.error.amount");
        }
        return new BigInteger(text);
    }

    public static BigInteger checked(BigInteger value) {
        if (value.signum() < 0 || value.compareTo(MAX) > 0) {
            throw new IllegalArgumentException("energyexchange.error.overflow");
        }
        return value;
    }

    public static BigInteger total(BigInteger unit, int count) {
        if (unit.signum() <= 0 || count < 1 || count > 2304) {
            throw new IllegalArgumentException("energyexchange.error.amount");
        }
        return checked(unit.multiply(BigInteger.valueOf(count)));
    }
}
