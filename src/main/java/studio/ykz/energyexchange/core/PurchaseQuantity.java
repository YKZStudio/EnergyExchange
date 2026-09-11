package studio.ykz.energyexchange.core;

/** The UI and packet handler share the same purchase limits. */
public final class PurchaseQuantity {
    private PurchaseQuantity() {}
    public static boolean allowed(int count, int maxStack) {
        return (count == 1 || count == 16 || count == 32 || count == 64) && count <= maxStack;
    }
}
