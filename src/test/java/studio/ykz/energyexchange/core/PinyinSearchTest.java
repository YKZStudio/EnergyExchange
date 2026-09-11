package studio.ykz.energyexchange.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PinyinSearchTest {
    @Test void fullInitialMixedAndNormalizedQueries() {
        for (String query : java.util.List.of("myzz", "moyingzhenzhu", "moyzz", "moyingzz", "MO YING ZHEN ZHU", "yingzhenzhu", "moyingzhenzh", "末影zz", "mo影z珠", "ｍｙｚｚ", "mò yǐng zhēn zhū"))
            assertTrue(PinyinSearch.matches("末影珍珠", query), query);
        assertFalse(PinyinSearch.matches("末影珍珠", "diamond"));
        assertFalse(PinyinSearch.matches("末影珍珠", "myxz"));
        assertFalse(PinyinSearch.matches("末影珍珠", "mzz"));
    }
    @Test void polyphonicCharactersAndTraditionalForms() {
        for (String name : java.util.List.of("长剑", "長劍"))
            for (String q : java.util.List.of("changjian", "zhangjian", "cj", "zj")) assertTrue(PinyinSearch.matches(name, q), name + ":" + q);
        for (String q : java.util.List.of("zhongchui", "chongchui", "zc", "cc", "重chui")) assertTrue(PinyinSearch.matches("重锤", q), q);
        assertTrue(PinyinSearch.matches("音乐盒", "yinyuehe"));
        assertTrue(PinyinSearch.matches("音乐盒", "yinlehe"));
        assertTrue(PinyinSearch.matches("银行", "yinhang"));
        assertTrue(PinyinSearch.matches("银行", "yinxing"));
    }
    @Test void umlautSurvivesToneAndWidthNormalization() {
        for (String q : java.util.List.of("lvse", "lüse", "lǜsè", "lu:se", "ｌｖｓｅ", "lu\u0308\u0300se", "绿se"))
            assertTrue(PinyinSearch.matches("绿色羊毛", q), q);
        assertEquals("lv", PinyinSearch.normalize("lǜ"));
        assertEquals("lv", PinyinSearch.normalize("ｌｕ："));
    }
    @Test void rejectsUnboundedInputAndHandlesNonChinese() {
        assertFalse(PinyinSearch.matches("末影珍珠", "a".repeat(129)));
        assertFalse(PinyinSearch.matches("末影珍珠", " ".repeat(129)));
        assertFalse(PinyinSearch.matches("末".repeat(513), "mo"));
        assertTrue(PinyinSearch.matches("AK-47", "ak47"));
        assertTrue(PinyinSearch.matches("𠀀石", "𠀀shi"));
        assertTrue(PinyinSearch.matches("", ""));
        assertFalse(PinyinSearch.matches("", "m"));
    }
    @Test void repeatedPolyphonicCharactersDoNotEnumerateCombinations() {
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), () -> {
            String name = "重长行乐".repeat(100);
            for (int i = 0; i < 20; i++) {
                assertFalse(PinyinSearch.matches(name, "zhongchangxingyue".repeat(7) + "x"));
                assertTrue(PinyinSearch.matches(name, "chongzhanghangle".repeat(7)));
            }
        });
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
