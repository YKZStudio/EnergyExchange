package studio.ykz.energyexchange;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.util.*;

/** Server-created prototypes, never client stacks. / 服务端构造模板，绝不接收客户端物品。 */
public final class Catalog {
    private static Map<String, ItemStack> entries = Map.of();
    private static long revision = -1;
    private Catalog() {}
    public static Map<String, ItemStack> entries() {
        if (revision != EnergyExchange.RULES.revision()) {
            var next = new TreeMap<String, ItemStack>();
            BuiltInRegistries.ITEM.forEach(item -> {
                var id = BuiltInRegistries.ITEM.getKey(item);
                if (!id.toString().equals("minecraft:air")) next.put(id.toString(), item.getDefaultInstance());
            });
            TaczBridge.populate(next);
            entries = Collections.unmodifiableMap(next);
            revision = EnergyExchange.RULES.revision();
        }
        return entries;
    }
    public static ItemStack sample(String key) {
        var stack = entries().get(key);
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("energyexchange.error.no_value");
        return stack.copy();
    }
    public static String identify(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("energyexchange.error.empty_hand");
        String base = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        // Model IDs define TaCZ/LR identity. All other component data is disposable.
        String field = switch (base) {
            case "tacz:modern_kinetic_gun" -> "GunId";
            case "tacz:ammo" -> "AmmoId";
            case "tacz:attachment" -> "AttachmentId";
            case "tacz:workbench_a", "tacz:workbench_b", "tacz:workbench_c" -> "BlockId";
            case "lrtactical:throwable" -> "ThrowableId";
            case "lrtactical:melee" -> "MeleeWeaponId";
            case "lrtactical:consumable" -> "ConsumableId";
            default -> "";
        };
        String key = base;
        if (!field.isEmpty()) {
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            key += "#" + tag.getStringOr(field, "");
        }
        sample(key); // Unknown model IDs remain unavailable; never trust arbitrary NBT identifiers.
        return key;
    }
}
