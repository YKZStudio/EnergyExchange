package studio.ykz.energyexchange.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    @Test void fullLoopConservesEnergy() {
        Account original = Account.EMPTY;
        Account burned = original.burn("minecraft:dirt", BigInteger.ONE, 64);
        assertEquals(BigInteger.valueOf(64), burned.energy());
        assertTrue(burned.learned().contains("minecraft:dirt"));
        Account bought = burned.buy("minecraft:dirt", BigInteger.ONE, 64);
        assertEquals(BigInteger.ZERO, bought.energy());
        assertEquals(Account.EMPTY, original);
    }
    @Test void hugeBalancesRemainExact() {
        BigInteger huge = Energy.parse("900719925474099312345678901234567890");
        var account = Account.EMPTY.burn("minecraft:diamond", huge, 2);
        assertEquals(huge, account.buy("minecraft:diamond", huge, 1).energy());
        assertEquals(account, AccountJson.read(AccountJson.write(account)));
    }
    @Test void capRejectsWholeTransaction() {
        Account account = new Account(Energy.MAX, Set.of("minecraft:dirt"));
        assertThrows(IllegalArgumentException.class, () -> account.burn("minecraft:dirt", BigInteger.ONE, 1));
        assertThrows(IllegalArgumentException.class, () -> Energy.total(Energy.MAX, 2));
        assertEquals(Energy.MAX, account.energy());
    }
    @Test void unknownAndInsufficientCannotBuy() {
        assertThrows(IllegalArgumentException.class, () -> Account.EMPTY.buy("minecraft:diamond", BigInteger.ONE, 1));
        var account = Account.EMPTY.learn("minecraft:dirt");
        assertThrows(IllegalArgumentException.class, () -> account.buy("minecraft:dirt", BigInteger.ONE, 1));
        assertEquals(BigInteger.ZERO, account.energy());
    }
    @ParameterizedTest @ValueSource(strings = {"", "-1", "+1", "1.5", "1e3", " 1", "01", "NaN", "１"})
    void rejectsNonCanonicalAmounts(String value) { assertThrows(IllegalArgumentException.class, () -> Energy.parse(value)); }
    @Test void limitsParsingBeforeBigIntegerAllocation() {
        assertEquals(Energy.MAX, Energy.parse("9".repeat(128)));
        assertThrows(IllegalArgumentException.class, () -> Energy.parse("9".repeat(129)));
    }
    @ParameterizedTest @ValueSource(ints = {0, -1, 2305, Integer.MAX_VALUE})
    void rejectsInvalidCounts(int count) { assertThrows(IllegalArgumentException.class, () -> Energy.total(BigInteger.ONE, count)); }
    @Test void knowledgeIsImmutableAndBounded() {
        Set<String> ids = IntStream.range(0, Account.MAX_LEARNED).mapToObj(i -> "test:item_" + i).collect(Collectors.toSet());
        var account = new Account(BigInteger.ZERO, ids);
        ids.clear();
        assertEquals(Account.MAX_LEARNED, account.learned().size());
        assertThrows(UnsupportedOperationException.class, () -> account.learned().clear());
        assertThrows(IllegalArgumentException.class, () -> account.learn("test:extra"));
        assertEquals(account, account.learn("test:item_0"));
    }
    @Test void invalidOrFutureSavesAreNotReset() {
        for (String raw : new String[]{"{}", "not json", "{\"schema\":2,\"energy\":\"1\",\"learned\":[]}",
                "{\"schema\":1,\"energy\":-1,\"learned\":[]}",
                "{\"schema\":1,\"energy\":\"1\",\"learned\":[\"bad id\"]}",
                "{\"schema\":1,\"energy\":\"1\",\"learned\":[\"a:b\",\"a:b\"]}"}) {
            var error = assertThrows(IllegalArgumentException.class, () -> AccountJson.read(raw));
            assertEquals("energyexchange.error.account_corrupt", error.getMessage());
        }
    }
    @Test void serializedDataFitsNbtStringAndIsDeterministic() {
        var account = new Account(BigInteger.TEN, Set.of("minecraft:stone", "minecraft:dirt"));
        assertEquals(account, AccountJson.read(AccountJson.write(account)));
        assertEquals(Account.EMPTY, AccountJson.read(AccountJson.EMPTY));
        var large = new Account(BigInteger.ZERO, IntStream.range(0, 1024)
                .mapToObj(i -> "test:" + "a".repeat(100) + i).collect(Collectors.toSet()));
        assertThrows(IllegalArgumentException.class, () -> AccountJson.write(large));
    }
}
