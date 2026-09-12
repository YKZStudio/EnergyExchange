package studio.ykz.energyexchange.core;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

public enum CatalogOrder {
    ENERGY_DESC, ENERGY_ASC, NAME;
    public CatalogOrder next() { return values()[(ordinal() + 1) % values().length]; }
    public <T> Comparator<T> comparator(Predicate<T> known, Function<T, String> energy, Function<T, String> name, Function<T, String> id) {
        Comparator<T> order = Comparator.comparing(e -> new BigInteger(energy.apply(e)));
        if (this == ENERGY_DESC) order = order.reversed();
        if (this == NAME) order = Comparator.comparing(name);
        return Comparator.<T, Boolean>comparing(known::test).reversed().thenComparing(order).thenComparing(name).thenComparing(id);
    }
}
