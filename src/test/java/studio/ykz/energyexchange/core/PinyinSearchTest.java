package studio.ykz.energyexchange.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PinyinSearchTest {
    @Test void fullInitialMixedAndNormalizedQueries() {
        for (String query : java.util.List.of("myzz", "moyingzhenzhu", "moyzz", "moyingzz", "MO YING ZHEN ZHU", "yingzhenzhu", "moyingzhenzh"))
            assertTrue(PinyinSearch.matches("末影珍珠", query), query);
        assertFalse(PinyinSearch.matches("末影珍珠", "diamond"));
        assertFalse(PinyinSearch.matches("末影珍珠", "myxz"));
        assertFalse(PinyinSearch.matches("末影珍珠", "mzz"));
    }
    @Test void rejectsUnboundedInputAndHandlesNonChinese() {
        assertFalse(PinyinSearch.matches("末影珍珠", "a".repeat(129)));
        assertFalse(PinyinSearch.matches("末".repeat(513), "mo"));
        assertTrue(PinyinSearch.matches("AK-47", "ak47"));
        assertTrue(PinyinSearch.matches("", ""));
        assertFalse(PinyinSearch.matches("", "m"));
    }
    @Test void quantitiesRespectStackLimits() {
        assertTrue(PurchaseQuantity.allowed(64, 64));
        assertTrue(PurchaseQuantity.allowed(16, 16));
        assertTrue(PurchaseQuantity.allowed(1, 1));
        for (int count : new int[]{-1, 0, 2, 8, 65, Integer.MAX_VALUE}) assertFalse(PurchaseQuantity.allowed(count, 64));
        assertFalse(PurchaseQuantity.allowed(32, 16));
        assertFalse(PurchaseQuantity.allowed(64, 16));
        assertFalse(PurchaseQuantity.allowed(16, 1));
    }
}
