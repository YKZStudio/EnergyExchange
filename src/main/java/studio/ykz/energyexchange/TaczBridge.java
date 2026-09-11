package studio.ykz.energyexchange;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Optional public-API adapter, independently implemented. / 独立实现的可选公开 API 适配。 */
final class TaczBridge {
    private TaczBridge() {}
    static void populate(Map<String, ItemStack> output) {
        if (!FabricLoader.getInstance().isModLoaded("tacz")) return;
        try {
            Class<?> api = Class.forName("com.tacz.guns.api.TimelessAPI");
            for (String kind : List.of("Gun", "Ammo", "Attachment")) {
                var indexes = (Set<?>) api.getMethod("getAllCommon" + kind + "Index").invoke(null);
                for (Object raw : indexes) {
                    var entry = (Map.Entry<?, ?>) raw;
                    Object builder = Class.forName("com.tacz.guns.api.item.builder." + kind + "ItemBuilder").getMethod("create").invoke(null);
                    builder.getClass().getMethod("setId", Identifier.class).invoke(builder, entry.getKey());
                    if (kind.equals("Gun")) {
                        Object data = entry.getValue().getClass().getMethod("getGunData").invoke(entry.getValue());
                        var modes = (List<?>) data.getClass().getMethod("getFireModeSet").invoke(data);
                        if (!modes.isEmpty()) builder.getClass().getMethod("setFireMode", Class.forName("com.tacz.guns.api.item.gun.FireMode")).invoke(builder, modes.getFirst());
                    }
                    add(output, (ItemStack) builder.getClass().getMethod("build").invoke(builder), entry.getKey().toString());
                }
            }
            // Block indexes supply their own registered base item through the index data.
            var blocks = (Set<?>) api.getMethod("getAllCommonBlockIndex").invoke(null);
            for (Object raw : blocks) {
                var entry = (Map.Entry<?, ?>) raw;
                Object block = entry.getValue().getClass().getMethod("getBlock").invoke(entry.getValue());
                Class<?> builderClass = Class.forName("com.tacz.guns.api.item.builder.BlockItemBuilder");
                Object builder = builderClass.getMethod("create", net.minecraft.world.level.ItemLike.class).invoke(null, block);
                builderClass.getMethod("setId", Identifier.class).invoke(builder, entry.getKey());
                add(output, (ItemStack) builderClass.getMethod("build").invoke(builder), entry.getKey().toString());
            }
            for (String base : List.of("tacz:modern_kinetic_gun", "tacz:ammo", "tacz:attachment", "tacz:gun_smith_table", "tacz:workbench_a", "tacz:workbench_b", "tacz:workbench_c", "lrtactical:throwable", "lrtactical:melee", "lrtactical:consumable")) output.remove(base);
            Class<?> lr = Class.forName("me.xjqsh.lrtactical.api.LrTacticalAPI");
            for (String method : List.of("getThrowableIndexes", "getMeleeIndexes", "getConsumableIndexes")) {
                for (Object index : (Collection<?>) lr.getMethod(method).invoke(null)) {
                    add(output, (ItemStack) index.getClass().getMethod("createItemStack").invoke(index), index.getClass().getMethod("getId").invoke(index).toString());
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            // Disable partial compatibility rather than exchanging malformed prototypes.
            output.keySet().removeIf(key -> key.startsWith("tacz:") || key.startsWith("lrtactical:"));
            EnergyExchange.LOGGER.error("TaCZ API incompatible; its exchanges disabled / TaCZ API 不兼容，已禁用其交易", exception);
        }
    }
    private static void add(Map<String, ItemStack> output, ItemStack stack, String variant) {
        if (!stack.isEmpty()) output.put(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#" + variant, stack.copyWithCount(1));
    }
}
