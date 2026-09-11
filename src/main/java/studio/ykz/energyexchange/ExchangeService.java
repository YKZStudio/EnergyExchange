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

/** Commands and menu requests settle on the server thread. / 命令和菜单请求均在服务端线程结算。 */
public final class ExchangeService {
    private ExchangeService() {}

    public static Account account(ServerPlayer player) {
        return AccountJson.read(player.getAttachedOrElse(EnergyExchange.ACCOUNT, AccountJson.EMPTY));
    }

    public static void checkPlayer(ServerPlayer player) {
        if (!player.level().getServer().isSameThread()) throw new IllegalStateException("Server thread required / 必须在服务端线程执行");
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) throw new IllegalArgumentException("energyexchange.error.gamemode");
        if (player.containerMenu != player.inventoryMenu && !(player.containerMenu instanceof ExchangeMenu menu && menu.stillValid(player))) throw new IllegalArgumentException("energyexchange.error.close_container");
    }

    public static ValueRule heldRule(ServerPlayer player) {
        return EnergyExchange.RULES.require(Catalog.identify(input(player)));
    }

    private static ItemStack input(ServerPlayer player) {
        return player.containerMenu instanceof ExchangeMenu menu ? menu.input.getItem(0) : player.getMainHandItem();
    }

    public static Account burn(ServerPlayer player, int count) {
        checkPlayer(player);
        heldRule(player);
        ItemStack held = input(player);
        if (count < 1 || count > held.getCount()) throw new IllegalArgumentException("energyexchange.error.count");
        String item = Catalog.identify(held);
        Account previous = account(player).learn(item);
        Account next = new Account(studio.ykz.energyexchange.core.Energy.checked(previous.energy().add(EnergyExchange.RULES.burnCredit(item, count))), previous.learned());
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
        ItemStack held = input(player);
        String item = Catalog.identify(held);
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
        return buy(player, id.toString(), count);
    }

    public static Account buy(ServerPlayer player, String key, int count) {
        checkPlayer(player);
        ItemStack sample = Catalog.sample(key);
        if (!sample.getItem().isEnabled(player.level().enabledFeatures())) throw new IllegalArgumentException("energyexchange.error.no_value");
        var rule = EnergyExchange.RULES.require(key);
        Account next = account(player).buy(key, rule.value(), count);
        String encoded = AccountJson.write(next);
        List<ItemStack> planned = planInsertion(player, sample, count);
        for (int slot = 0; slot < planned.size(); slot++) player.getInventory().setItem(slot, planned.get(slot));
        player.setAttached(EnergyExchange.ACCOUNT, encoded);
        changed(player);
        return next;
    }

    public static Account buyExperience(ServerPlayer player, int points) {
        checkPlayer(player);
        EnergyExchange.RULES.checkReady();
        if (!ExchangeConfig.server.xpEnabled()) throw new IllegalArgumentException("energyexchange.error.xp_disabled");
        if (points < 1 || points > 1000 || player.totalExperience < 0 || player.totalExperience > Integer.MAX_VALUE - points
                || player.experienceLevel > 10000) throw new IllegalArgumentException("energyexchange.error.xp_limit");
        var previous = account(player);
        var cost = studio.ykz.energyexchange.core.Energy.total(studio.ykz.energyexchange.core.Energy.parse(ExchangeConfig.server.xpCost()), points);
        if (previous.energy().compareTo(cost) < 0) throw new IllegalArgumentException("energyexchange.error.insufficient");
        var next = new Account(previous.energy().subtract(cost), previous.learned());
        String encoded = AccountJson.write(next);
        player.giveExperiencePoints(points);
        player.setAttached(EnergyExchange.ACCOUNT, encoded);
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
        player.containerMenu.broadcastChanges();
    }
}
