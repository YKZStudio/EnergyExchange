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
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String key = base;
        for (String field : List.of("GunId", "AmmoId", "AttachmentId", "BlockId", "ThrowableId", "MeleeWeaponId", "ConsumableId")) {
            if (tag.contains(field)) { key = base + "#" + tag.getStringOr(field, ""); break; }
        }
        ItemStack expected = sample(key);
        if (!ItemStack.isSameItemSameComponents(normalize(stack), normalize(expected)))
            throw new IllegalArgumentException("energyexchange.error.components");
        return key;
    }
    private static ItemStack normalize(ItemStack original) {
        ItemStack result = original.copy();
        if (BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals("tacz:modern_kinetic_gun")) {
            var tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            // Fire selector is harmless; ammunition, attachments and arbitrary fields remain compared.
            // 射击模式不携带价值；弹药、配件及未知字段仍严格比较。
            tag.remove("GunFireMode");
            for (String field : List.of("GunCurrentAmmoCount", "GunLevelExp", "HeatAmount"))
                if (tag.getDoubleOr(field, -1) == 0) tag.remove(field);
            for (String field : List.of("HasBulletInBarrel", "OverHeated"))
                if (tag.contains(field) && !tag.getBooleanOr(field, true)) tag.remove(field);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return result;
    }
}
