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
            var purchase = world.getServer().computeOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var menu = (ExchangeMenu) p.containerMenu;
                return new ExchangeNetwork.Action(menu.containerId, menu.nonce, EnergyExchange.RULES.revision(), menu.sequence, 3, "minecraft:dirt", 8);
            });
            context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(purchase));
            world.getServer().waitFor(server -> ExchangeService.account(server.getPlayerList().getPlayers().getFirst()).energy().intValue() == 8);
            context.waitTicks(4);
            context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(purchase));
            context.waitTicks(4);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                if (ExchangeService.account(p).energy().intValue() != 8 || p.getInventory().countItem(Items.DIRT) != 8)
                    throw new AssertionError("Replayed request settled twice / 重复请求重复结算");
            });
            context.runOnClient(client -> client.gui.screen().onClose());
            context.waitForScreen(null);
            context.setScreen(() -> new ConfigScreen(null));
            context.waitForScreen(ConfigScreen.class);
            context.takeScreenshot("modmenu-settings-en_us");
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                client.getLanguageManager().setSelected("zh_cn"); client.options.languageCode = "zh_cn"; client.reloadResourcePacks();
            });
            context.waitFor(client -> net.minecraft.client.resources.language.I18n.get("energyexchange.ui.title").equals("转化"));
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); ExchangeContent.TABLET.use(p.level(), p, InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(ExchangeScreen.class);
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            context.takeScreenshot("transmutation-zh_cn");
            context.runOnClient(client -> client.gui.screen().onClose());
            context.setScreen(() -> new ConfigScreen(null));
            context.takeScreenshot("modmenu-settings-zh_cn");
            context.setScreen(() -> null);
        }
    }
}
