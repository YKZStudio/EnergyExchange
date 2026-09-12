package studio.ykz.energyexchange.core;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CatalogOrderTest {
    record Entry(boolean known, String energy, String name, String id) {}
    private List<String> sorted(CatalogOrder order) {
        return List.of(new Entry(true,"9","C","nine"),new Entry(true,"10000000000000000000000000000000000000000","B","huge"),new Entry(true,"10","A","ten"),new Entry(false,"99999999999999999999999999999999999999999999","0","unknown"))
                .stream().sorted(order.comparator(Entry::known, Entry::energy, Entry::name, Entry::id)).map(Entry::id).toList();
    }
    @Test void descendingUsesExactNumbersAndKeepsKnowledgeFirst() { assertEquals(List.of("huge","ten","nine","unknown"),sorted(CatalogOrder.ENERGY_DESC)); }
    @Test void ascendingAndNameModes() {
        assertEquals(List.of("nine","ten","huge","unknown"),sorted(CatalogOrder.ENERGY_ASC));
        assertEquals(List.of("ten","huge","nine","unknown"),sorted(CatalogOrder.NAME));
        assertEquals(CatalogOrder.ENERGY_DESC,CatalogOrder.NAME.next());
    }
}
