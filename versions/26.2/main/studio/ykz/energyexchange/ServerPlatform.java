package studio.ykz.energyexchange;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Version-specific server inventory operations. */
final class ServerPlatform {
    private ServerPlatform() {}
    static void drop(Player player, ItemStack stack) { player.drop(stack, true); }
}
