package studio.ykz.energyexchange;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import studio.ykz.energyexchange.core.Account;
import studio.ykz.energyexchange.core.AccountJson;
import studio.ykz.energyexchange.core.ValueRule;
import java.util.ArrayList;
import java.util.List;

/** Called only from the server command dispatcher. No client-provided balances or stacks.
 * 仅由服务端命令分发器调用，不接收客户端余额或物品堆。 */
public final class ExchangeService {
    private ExchangeService() {}

    public static Account account(ServerPlayer player) {
        return AccountJson.read(player.getAttachedOrElse(EnergyExchange.ACCOUNT, AccountJson.EMPTY));
    }

    private static void checkPlayer(ServerPlayer player) {
        if (!player.level().getServer().isSameThread()) throw new IllegalStateException("Server thread required / 必须在服务端线程执行");
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) throw new IllegalArgumentException("energyexchange.error.gamemode");
        if (player.containerMenu != player.inventoryMenu) throw new IllegalArgumentException("energyexchange.error.close_container");
    }

    public static ValueRule heldRule(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) throw new IllegalArgumentException("energyexchange.error.empty_hand");
        if (!ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()))) {
            throw new IllegalArgumentException("energyexchange.error.components");
        }
        return EnergyExchange.RULES.require(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static Account burn(ServerPlayer player, int count) {
        checkPlayer(player);
        var rule = heldRule(player);
        ItemStack held = player.getMainHandItem();
        if (count < 1 || count > held.getCount()) throw new IllegalArgumentException("energyexchange.error.count");
        String item = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        Account next = account(player).burn(item, rule.value(), count);
        String encoded = AccountJson.write(next);
        // Every possible validation precedes either mutation / 所有校验均在变更之前完成。
        held.shrink(count);
        player.setAttached(EnergyExchange.ACCOUNT, encoded);
        changed(player);
        return next;
    }

    public static Account learn(ServerPlayer player) {
        checkPlayer(player);
        heldRule(player);
        ItemStack held = player.getMainHandItem();
        String item = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        Account previous = account(player);
        if (previous.learned().contains(item)) throw new IllegalArgumentException("energyexchange.error.already_learned");
        Account next = previous.learn(item);
        String encoded = AccountJson.write(next);
        held.shrink(1);
        player.setAttached(EnergyExchange.ACCOUNT, encoded);
        changed(player);
        return next;
    }

    public static Account buy(ServerPlayer player, Identifier id, int count) {
        checkPlayer(player);
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("energyexchange.error.no_value");
        Item item = BuiltInRegistries.ITEM.getValue(id);
        ItemStack sample = new ItemStack(item);
        if (sample.isEmpty() || !item.isEnabled(player.level().enabledFeatures())) throw new IllegalArgumentException("energyexchange.error.no_value");
        var rule = EnergyExchange.RULES.require(id);
        Account next = account(player).buy(id.toString(), rule.value(), count);
        String encoded = AccountJson.write(next);
        List<ItemStack> planned = planInsertion(player, sample, count);
        for (int slot = 0; slot < planned.size(); slot++) player.getInventory().setItem(slot, planned.get(slot));
        player.setAttached(EnergyExchange.ACCOUNT, encoded);
        changed(player);
        return next;
    }

    private static List<ItemStack> planInsertion(ServerPlayer player, ItemStack sample, int count) {
        var inventory = player.getInventory();
        var planned = new ArrayList<ItemStack>(36);
        for (int slot = 0; slot < 36; slot++) planned.add(inventory.getItem(slot).copy());
        int remaining = count;
        // Merge first, then empty slots; armor/offhand never used / 先合堆后空格，不使用装备和副手。
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < planned.size() && remaining > 0; slot++) {
                ItemStack stack = planned.get(slot);
                if ((pass == 0 && (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, sample)))
                        || (pass == 1 && !stack.isEmpty())) continue;
                int max = Math.min(sample.getMaxStackSize(), inventory.getMaxStackSize());
                int amount = Math.min(remaining, Math.max(0, max - stack.getCount()));
                if (amount == 0) continue;
                if (stack.isEmpty()) planned.set(slot, sample.copyWithCount(amount));
                else stack.grow(amount);
                remaining -= amount;
            }
        }
        if (remaining != 0) throw new IllegalArgumentException("energyexchange.error.inventory_full");
        return planned;
    }

    private static void changed(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
