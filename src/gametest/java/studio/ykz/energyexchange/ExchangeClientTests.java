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
            context.waitFor(client -> "1".equals(EnergyTooltip.value(new ItemStack(Items.DIRT))));
            context.runOnClient(client -> {
                var stack = new ItemStack(Items.DIRT);
                var lines = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level), client.player, net.minecraft.world.item.TooltipFlag.ADVANCED);
                int id = -1; for (int i = 0; i < lines.size(); i++) if (lines.get(i).getString().equals("minecraft:dirt")) id = i;
                if (id <= 0 || !lines.get(id - 1).getString().equals("ENERGY 1")) throw new AssertionError("ENERGY immediately above advanced item ID");
                var normal = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level), client.player, net.minecraft.world.item.TooltipFlag.NORMAL);
                if (normal.stream().anyMatch(c -> c.getString().startsWith("ENERGY "))) throw new AssertionError("Normal tooltip must not show ENERGY");
            });
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL);
                p.getInventory().setItem(p.getInventory().getSelectedSlot(), new ItemStack(ExchangeContent.TABLET));
                ExchangeContent.TABLET.use(p.level(), p, InteractionHand.MAIN_HAND);
                ((ExchangeMenu) p.containerMenu).input.setItem(0, new ItemStack(Items.DIRT, 32));
                p.containerMenu.broadcastChanges();
            });
            context.waitForScreen(ExchangeScreen.class);
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            context.runOnClient(client -> {
                if (!((ExchangeScreen) client.gui.screen()).visibleKeys().isEmpty()) throw new AssertionError("New accounts must not show the entire catalog");
            });
            context.takeScreenshot("transmutation-empty-en_us");
            context.runOnClient(client -> ((ExchangeScreen) client.gui.screen()).request(1, "", 0));
            world.getServer().waitFor(server -> ExchangeService.account(server.getPlayerList().getPlayers().getFirst()).energy().intValue() == 32);
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            context.runOnClient(client -> {
                var screen = (ExchangeScreen) client.gui.screen();
                if (!screen.visibleKeys().equals(java.util.List.of("minecraft:dirt"))) throw new AssertionError("Conversion must automatically learn and show the input");
                screen.toggleBrowse();
                if (screen.visibleKeys().size() < 2 || !screen.visibleKeys().getFirst().equals("minecraft:dirt")) throw new AssertionError("All view must sort learned items first");
                screen.toggleBrowse();
                screen.select("minecraft:dirt");
            });
            context.takeScreenshot("transmutation-en_us");
            context.waitTicks(3);
            var purchase = world.getServer().computeOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var menu = (ExchangeMenu) p.containerMenu;
                return new ExchangeNetwork.Action(menu.containerId, menu.nonce, EnergyExchange.RULES.revision(), menu.sequence, 3, "minecraft:dirt", 16);
            });
            context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(purchase));
            world.getServer().waitFor(server -> ExchangeService.account(server.getPlayerList().getPlayers().getFirst()).energy().intValue() == 16);
            context.waitTicks(4);
            context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(purchase));
            context.waitTicks(4);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                if (ExchangeService.account(p).energy().intValue() != 16 || p.getInventory().countItem(Items.DIRT) != 16)
                    throw new AssertionError("Replayed request settled twice / 重复请求重复结算");
            });
            context.runOnClient(client -> client.gui.screen().onClose());
            context.waitForScreen(null);
            context.setScreen(() -> new ConfigScreen(null));
            context.waitForScreen(ConfigScreen.class);
            context.takeScreenshot("modmenu-settings-en_us");
            context.setScreen(() -> null);
            context.runOnClient(client -> new ExchangeConfig(false, true, true, "128", true).save());
            context.runOnClient(client -> {
                client.getLanguageManager().setSelected("zh_cn"); client.options.languageCode = "zh_cn"; client.reloadResourcePacks();
            });
            context.waitFor(client -> net.minecraft.client.resources.language.I18n.get("energyexchange.ui.title").equals("转化") && client.gui.overlay() == null);
            context.waitTicks(3);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.setAttached(EnergyExchange.ACCOUNT, studio.ykz.energyexchange.core.AccountJson.write(new studio.ykz.energyexchange.core.Account(java.math.BigInteger.valueOf(65536), java.util.Set.of("minecraft:dirt", "minecraft:ender_pearl", "minecraft:green_wool", "minecraft:mace"))));
                ExchangeContent.TABLET.use(p.level(), p, InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(ExchangeScreen.class);
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            context.runOnClient(client -> {
                var screen = (ExchangeScreen) client.gui.screen();
                if (!screen.visibleKeys().equals(java.util.List.of("minecraft:mace", "minecraft:ender_pearl", "minecraft:green_wool", "minecraft:dirt"))) throw new AssertionError("Default energy descending order");
                for (String query : java.util.List.of("myzz", "moyingzhenzhu", "moyzz", "MO YING ZHEN ZHU", "末影zz", "mo影z珠", "ｍｙｚｚ", "mò yǐng zhēn zhū")) {
                    screen.search(query);
                    if (!screen.visibleKeys().equals(java.util.List.of("minecraft:ender_pearl"))) throw new AssertionError("Pinyin failed: " + query);
                }
                for (String query : java.util.List.of("lvse", "lüse", "lǜsè", "lu:se", "ｌｖｓｅ", "绿se")) {
                    screen.search(query);
                    if (!screen.visibleKeys().equals(java.util.List.of("minecraft:green_wool"))) throw new AssertionError("Umlaut failed: " + query);
                }
                for (String query : java.util.List.of("zhongchui", "chongchui", "重chui")) {
                    screen.search(query);
                    if (!screen.visibleKeys().equals(java.util.List.of("minecraft:mace"))) throw new AssertionError("Polyphonic search failed: " + query);
                }
                screen.search("myzz"); screen.select("minecraft:ender_pearl");
                if (screen.canPurchase(64) || screen.canPurchase(32) || !screen.canPurchase(16) || !screen.canPurchase(1)) throw new AssertionError("Pearl stack limits");
            });
            context.takeScreenshot("transmutation-zh_cn");
            context.waitTicks(3);
            context.runOnClient(client -> ((ExchangeScreen) client.gui.screen()).request(3, "minecraft:ender_pearl", 64));
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                if (ExchangeService.account(p).energy().intValue() != 65536 || p.getInventory().countItem(Items.ENDER_PEARL) != 0) throw new AssertionError("Forged oversized purchase accepted");
            });
            context.waitTicks(3);
            context.runOnClient(client -> ((ExchangeScreen) client.gui.screen()).request(3, "minecraft:ender_pearl", 16));
            world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().getInventory().countItem(Items.ENDER_PEARL) == 16);
            context.runOnClient(client -> client.gui.screen().onClose());
            context.setScreen(() -> new ConfigScreen(null));
            context.takeScreenshot("modmenu-settings-zh_cn");
            context.runOnClient(client -> new ExchangeConfig(false, true, true, "128", false, studio.ykz.energyexchange.core.CatalogOrder.NAME).save());
            context.setScreen(() -> null);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); ExchangeContent.TABLET.use(p.level(), p, InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(ExchangeScreen.class);
            context.waitFor(client -> ((ExchangeScreen) client.gui.screen()).isReady());
            context.runOnClient(client -> {
                var screen = (ExchangeScreen) client.gui.screen();
                if (!screen.visibleKeys().equals(java.util.List.of("minecraft:ender_pearl", "minecraft:dirt", "minecraft:green_wool", "minecraft:mace"))) throw new AssertionError("Configured name order");
                screen.search("myzz");
                if (!screen.visibleKeys().isEmpty()) throw new AssertionError("Disabled pinyin must not affect search");
                screen.search("末影珍珠");
                if (!screen.visibleKeys().equals(java.util.List.of("minecraft:ender_pearl"))) throw new AssertionError("Literal search must still work");
                screen.onClose();
                client.player.sendOverlayMessage(net.minecraft.network.chat.Component.empty());
            });
            context.setScreen(() -> null);
            var tablePos = world.getServer().computeOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var pos = p.blockPosition().offset(0, 0, 3);
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++)
                    p.level().setBlockAndUpdate(pos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                p.level().setBlockAndUpdate(pos, ExchangeContent.TABLE.defaultBlockState());
                return pos;
            });
            context.runOnClient(client -> {
                client.player.setPos(tablePos.getX() + .5, tablePos.getY(), tablePos.getZ() - 2.5);
                client.player.setYRot(0); client.player.setXRot(30);
            });
            context.waitTicks(20);
            context.takeScreenshot("transmutation-table-model");
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); int slot = 0;
                for (var item : Armory.ITEMS.values()) p.getInventory().setItem(slot++, new ItemStack(item));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Armory.ITEMS.get("infinity_helmet")));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Armory.ITEMS.get("infinity_chestplate")));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Armory.ITEMS.get("infinity_leggings")));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Armory.ITEMS.get("infinity_boots")));
                p.inventoryMenu.broadcastChanges();
            });
            context.waitTicks(10);
            context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.takeScreenshot("armory-infinity-zh_cn");

        }
    }
}
