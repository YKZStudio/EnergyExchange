package studio.ykz.energyexchange.core;

import java.math.BigInteger;
import java.util.Set;
import java.util.TreeSet;

/** Immutable player account / 不可变玩家账户. */
public record Account(BigInteger energy, Set<String> learned) {
    public static final int MAX_LEARNED = 1024;
    public static final Account EMPTY = new Account(BigInteger.ZERO, Set.of());

    public Account {
        Energy.checked(energy);
        learned = Set.copyOf(learned);
        if (learned.size() > MAX_LEARNED) throw new IllegalArgumentException("energyexchange.error.knowledge_full");
    }

    public Account learn(String item) {
        var next = new TreeSet<>(learned);
        next.add(item);
        return new Account(energy, next);
    }

    public Account burn(String item, BigInteger unit, int count) {
        Account next = learn(item);
        return new Account(Energy.checked(energy.add(Energy.total(unit, count))), next.learned);
    }

    public Account buy(String item, BigInteger unit, int count) {
        if (!learned.contains(item)) throw new IllegalArgumentException("energyexchange.error.not_learned");
        BigInteger cost = Energy.total(unit, count);
        if (energy.compareTo(cost) < 0) throw new IllegalArgumentException("energyexchange.error.insufficient");
        return new Account(energy.subtract(cost), learned);
    }
}
