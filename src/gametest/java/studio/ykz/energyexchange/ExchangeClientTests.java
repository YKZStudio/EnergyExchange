package studio.ykz.energyexchange;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;

public final class ExchangeClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL);
                p.getInventory().setItem(p.getInventory().getSelectedSlot(), new ItemStack(ExchangeContent.TABLET));
                ExchangeContent.TABLET.use(p.level(), p, InteractionHand.MAIN_HAND);
                ((ExchangeMenu) p.containerMenu).input.setItem(0, new ItemStack(Items.DIRT, 16));
                p.containerMenu.broadcastChanges();
            });
            context.waitForScreen(ExchangeScreen.class);
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            context.takeScreenshot("transmutation-en_us");
            context.runOnClient(client -> ((ExchangeScreen) client.gui.screen()).request(1, "", 0));
            world.getServer().waitFor(server -> ExchangeService.account(server.getPlayerList().getPlayers().getFirst()).energy().intValue() == 16);
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            context.waitTicks(3);
            context.runOnClient(client -> ((ExchangeScreen) client.gui.screen()).request(3, "minecraft:dirt", 8));
            world.getServer().waitFor(server -> ExchangeService.account(server.getPlayerList().getPlayers().getFirst()).energy().intValue() == 8);
            context.runOnClient(client -> client.gui.screen().onClose());
            context.waitForScreen(null);
            context.setScreen(() -> new ConfigScreen(null));
            context.waitForScreen(ConfigScreen.class);
            context.takeScreenshot("modmenu-settings-en_us");
            context.setScreen(() -> null);
        }
    }
}
