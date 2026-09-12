package studio.ykz.energyexchange;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import studio.ykz.energyexchange.core.Account;
import studio.ykz.energyexchange.core.AccountJson;
import studio.ykz.energyexchange.core.Energy;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ExchangeGameTests {
    private static final Identifier DIRT = Identifier.parse("minecraft:dirt");

    private static ServerPlayer player(GameTestHelper helper) {
        // Vanilla's helper hardcodes gameMode() to CREATIVE; use an actual server player.
        // 原版辅助玩家将 gameMode() 写死为创造，故使用真正的服务端玩家。
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "exchange-test"), false);
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        return player;
    }

    @GameTest
    public void pre1SettingsMigration(GameTestHelper helper) throws Exception {
        var path = ExchangeConfig.PATH;
        byte[] previous = java.nio.file.Files.exists(path) ? java.nio.file.Files.readAllBytes(path) : null;
        try {
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, "{\"showUnlearned\":true,\"compactNumbers\":false,\"xpEnabled\":false,\"xpCost\":\"256\"}");
            var config = ExchangeConfig.load();
            helper.assertTrue(!config.showUnlearned() && !config.pinyinSearch() && !config.compactNumbers() && !config.xpEnabled() && config.xpCost().equals("256"), "Legacy settings migration");
            java.nio.file.Files.writeString(path, "{\"showUnlearned\":true,\"compactNumbers\":false,\"xpEnabled\":false,\"xpCost\":\"256\",\"pinyinSearch\":true}");
            helper.assertTrue(ExchangeConfig.load().sortOrder() == studio.ykz.energyexchange.core.CatalogOrder.ENERGY_DESC && ExchangeConfig.load().pinyinSearch(), "0.2 settings preserve preferences and default to energy descending");
            new ExchangeConfig(true, false, false, "256", true, studio.ykz.energyexchange.core.CatalogOrder.NAME).save();
            var saved = ExchangeConfig.load();
            helper.assertTrue(saved.showUnlearned() && saved.pinyinSearch() && saved.xpCost().equals("256") && saved.sortOrder() == studio.ykz.energyexchange.core.CatalogOrder.NAME, "New settings round trip");
            helper.assertTrue(ExchangeContent.TABLE.defaultBlockState().getShape(helper.getLevel(), net.minecraft.core.BlockPos.ZERO).max(net.minecraft.core.Direction.Axis.Y) == 3.0 / 16, "Table collision matches low model");
        } finally {
            if (previous == null) java.nio.file.Files.deleteIfExists(path); else java.nio.file.Files.write(path, previous);
        }
        helper.succeed();
    }

    @GameTest
    public void commandsAndIsolatedWallets(GameTestHelper helper) throws Exception {
        var first = player(helper);
        var second = player(helper);
        first.getInventory().setItem(0, new ItemStack(Items.DIRT, 64));
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        int result = dispatcher.execute("ee burn all", first.createCommandSourceStack());
        helper.assertTrue(result == 1, "Non-OP burn command / 非 OP 分解命令");
        helper.assertTrue(first.getMainHandItem().isEmpty(), "Burn removes items / 分解扣除物品");
        helper.assertTrue(ExchangeService.account(first).energy().equals(BigInteger.valueOf(64)), "Burn credit / 分解入账");
        helper.assertTrue(ExchangeService.account(second).equals(Account.EMPTY), "Wallet isolation / 钱包隔离");
        dispatcher.execute("energyexchange buy minecraft:dirt 16", first.createCommandSourceStack());
        helper.assertTrue(first.getInventory().getItem(0).getCount() == 16, "Buy returns items / 兑换返还物品");
        helper.assertTrue(ExchangeService.account(first).energy().equals(BigInteger.valueOf(48)), "Buy debit / 兑换扣款");
        helper.succeed();
    }

    @GameTest
    public void inventoryPreflightAndOverflow(GameTestHelper helper) {
        var player = player(helper);
        var account = new Account(BigInteger.valueOf(100), Set.of(DIRT.toString()));
        player.setAttached(EnergyExchange.ACCOUNT, AccountJson.write(account));
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        reject(helper, "inventory_full", () -> ExchangeService.buy(player, DIRT, 1));
        helper.assertTrue(ExchangeService.account(player).equals(account), "Full inventory does not debit / 背包满不扣款");
        player.getInventory().setItem(7, new ItemStack(Items.DIRT, 63));
        reject(helper, "inventory_full", () -> ExchangeService.buy(player, DIRT, 2));
        helper.assertTrue(player.getInventory().getItem(7).getCount() == 63, "Failed preflight must not partially insert / 预检失败不得部分插入");
        ExchangeService.buy(player, DIRT, 1);
        helper.assertTrue(player.getInventory().getItem(7).getCount() == 64, "Merge into existing slot / 合并已有物品堆");
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 1));
        player.setAttached(EnergyExchange.ACCOUNT, AccountJson.write(new Account(Energy.MAX, Set.of(DIRT.toString()))));
        reject(helper, "overflow", () -> ExchangeService.burn(player, 1));
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && ExchangeService.account(player).energy().equals(Energy.MAX), "Overflow consumes nothing / 超限无损");
        helper.succeed();
    }

    @GameTest
    public void sampleComponentsAndModes(GameTestHelper helper) {
        var player = player(helper);
        var named = new ItemStack(Items.DIRT, 2);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Protected / 受保护"));
        player.getInventory().setItem(0, named);
        ExchangeService.burn(player, 2);
        ExchangeService.buy(player, DIRT, 2);
        helper.assertTrue(!player.getMainHandItem().has(DataComponents.CUSTOM_NAME), "Purchases discard sample name");
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
        ExchangeService.learn(player);
        reject(helper, "already_learned", () -> ExchangeService.learn(player));
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && ExchangeService.account(player).energy().signum() == 0, "Learning consumes exactly one and no credit / 学习只消耗一个且不入账");
        player.setGameMode(GameType.CREATIVE);
        reject(helper, "gamemode", () -> ExchangeService.burn(player, 1));
        player.setGameMode(GameType.SPECTATOR);
        reject(helper, "gamemode", () -> ExchangeService.burn(player, 1));
        helper.succeed();
    }

    @GameTest
    public void persistentAttachmentAndDeathCopy(GameTestHelper helper) {
        var original = player(helper);
        original.getInventory().setItem(0, new ItemStack(Items.DIRT, 8));
        ExchangeService.burn(original, 4);
        var expected = ExchangeService.account(original);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, original.registryAccess());
        original.saveWithoutId(output);
        var restored = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "save-probe"), ClientInformation.createDefault());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, original.registryAccess(), output.buildResult()));
        helper.assertTrue(ExchangeService.account(restored).equals(expected), "Account NBT round trip / 账户 NBT 往返");
        helper.assertTrue(restored.getInventory().getItem(0).getCount() == 4, "Inventory saved alongside account / 物品和账户同时存档");
        original.setHealth(0);
        var respawned = helper.getLevel().getServer().getPlayerList().respawn(
                original, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        helper.assertTrue(ExchangeService.account(respawned).equals(expected), "Death retains attachment / 死亡保留账户");
        helper.succeed();
    }

    @GameTest
    public void corruptAccountIsPreserved(GameTestHelper helper) {
        var player = player(helper);
        String raw = "{\"schema\":999,\"energy\":\"1234\",\"learned\":[]}";
        player.setAttached(EnergyExchange.ACCOUNT, raw);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 2));
        reject(helper, "account_corrupt", () -> ExchangeService.burn(player, 1));
        helper.assertTrue(raw.equals(player.getAttached(EnergyExchange.ACCOUNT)), "Invalid account preserved / 保留无效账户");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "No items lost / 不丢物品");
        helper.succeed();
    }

    @GameTest
    public void reloadSnapshotsFailClosed(GameTestHelper helper) {
        Rules rules = new Rules();
        rules.load(resources(helper, "{\"value\":\"2\"}"));
        rules.complete(true);
        helper.assertTrue(rules.require(DIRT).value().equals(BigInteger.TWO), "Loaded price / 已加载价格");
        rules.pause();
        reject(helper, "rules_unavailable", () -> rules.require(DIRT));
        try {
            rules.load(resources(helper, "{\"value\":\"-1\"}"));
            helper.fail("Invalid rule accepted / 接受了无效规则");
        } catch (IllegalStateException expected) {
            rules.complete(false);
        }
        reject(helper, "rules_unavailable", () -> rules.require(DIRT));
        rules.load(resources(helper, "{\"value\":\"3\"}"));
        rules.complete(true);
        helper.assertTrue(rules.require(DIRT).value().equals(BigInteger.valueOf(3)), "Recovery installs new snapshot / 修复后使用新快照");
        rules.load(resources(helper, "{\"enabled\":false}"));
        rules.complete(true);
        reject(helper, "no_value", () -> rules.require(DIRT));
        helper.succeed();
    }

    @GameTest
    public void tableInputAndPortableValidity(GameTestHelper helper) {
        var p = player(helper);
        p.getInventory().setItem(0, new ItemStack(ExchangeContent.TABLET));
        var menu = new ExchangeMenu(20, p.getInventory(), 123L, null, 0);
        p.containerMenu = menu;
        helper.assertTrue(menu.stillValid(p), "Held tablet opens / 手持可用");
        menu.input.setItem(0, new ItemStack(Items.DIRT, 16));
        ExchangeService.burn(p, 16);
        helper.assertTrue(menu.input.isEmpty() && p.getMainHandItem().is(ExchangeContent.TABLET), "Input is consumed, tablet retained / 消耗输入并保留转化桌");
        ExchangeService.buy(p, DIRT, 8);
        helper.assertTrue(p.getMainHandItem().is(ExchangeContent.TABLET), "Purchase preserves anchor / 购买保留手持槽");
        menu.input.setItem(0, new ItemStack(Items.DIAMOND, 3));
        menu.removed(p);
        helper.assertTrue(menu.input.isEmpty() && p.getInventory().countItem(Items.DIAMOND) == 3, "Closing returns input / 关闭返还输入");
        p.getInventory().setSelectedSlot(1);
        helper.assertTrue(!menu.stillValid(p), "Switching held slot invalidates menu / 切换手持槽关闭菜单");
        reject(helper, "close_container", () -> ExchangeService.buy(p, DIRT, 1));
        p.containerMenu = p.inventoryMenu;
        helper.succeed();
    }

    @GameTest
    public void placedTableAndExperience(GameTestHelper helper) {
        var p = player(helper);
        var pos = p.blockPosition(); p.level().setBlockAndUpdate(pos, ExchangeContent.TABLE.defaultBlockState());
        var menu = new ExchangeMenu(21, p.getInventory(), 321L, pos, -1); p.containerMenu = menu;
        helper.assertTrue(menu.stillValid(p), "Placed table reachable / 转化桌可达");
        p.setAttached(EnergyExchange.ACCOUNT, AccountJson.write(new Account(BigInteger.valueOf(1280), Set.of())));
        int before = p.totalExperience;
        ExchangeService.buyExperience(p, 10);
        helper.assertTrue(p.totalExperience == before + 10 && ExchangeService.account(p).energy().signum() == 0, "Exact XP points and debit / 精确经验点与扣费");
        reject(helper, "insufficient", () -> ExchangeService.buyExperience(p, 1));
        helper.assertTrue(p.totalExperience == before + 10, "Failed XP does not award / 失败不发经验");
        p.level().removeBlock(pos, false);
        helper.assertTrue(!menu.stillValid(p), "Broken table invalidates / 拆桌失效");
        p.containerMenu = p.inventoryMenu; helper.succeed();
    }

    @GameTest
    public void completeCatalogAndKnowledgeRoundTrip(GameTestHelper helper) {
        var keys = new java.util.TreeSet<String>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
            if (!id.startsWith("minecraft:") || item == Items.AIR) continue;
            helper.assertTrue(EnergyExchange.RULES.require(id).value().signum() > 0, "Vanilla priced / 原版定价: " + id);
            keys.add(id);
        }
        keys.add("tacz:modern_kinetic_gun#tacz:ak47");
        var account = new Account(BigInteger.ONE, keys);
        helper.assertTrue(AccountJson.read(AccountJson.write(account)).equals(account), "Full vanilla knowledge fits persistent account / 全原版知识可持久化");
        helper.succeed();
    }

    @GameTest
    public void fractionalConversionAndFuelPrices(GameTestHelper helper) {
        var p = player(helper);
        p.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE_SLAB, 1));
        reject(helper, "small_batch", () -> ExchangeService.burn(p, 1));
        helper.assertTrue(p.getMainHandItem().getCount() == 1 && ExchangeService.account(p).equals(Account.EMPTY), "Tiny batch unchanged / 小批量不消耗");
        p.getMainHandItem().setCount(2); ExchangeService.burn(p, 2);
        helper.assertTrue(ExchangeService.account(p).energy().equals(BigInteger.ONE), "Two slabs earn one Energy / 两半砖换一点能量");
        helper.assertTrue(EnergyExchange.RULES.require("minecraft:charcoal").value().compareTo(EnergyExchange.RULES.require("minecraft:oak_log").value()) <= 0,
                "Charcoal does not multiply log value / 木炭不放大原木价值");
        helper.succeed();
    }

    @GameTest
    public void taczModelsRoundTripAndDataDiscard(GameTestHelper helper) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("tacz")) { helper.succeed(); return; }
        var p = player(helper);
        int variants = 0;
        var families = new java.util.HashSet<String>();
        for (var entry : Catalog.entries().entrySet()) {
            if (!entry.getKey().contains("#")) continue;
            families.add(entry.getKey().split("#")[0]); variants++;
            try { helper.assertTrue(Catalog.identify(entry.getValue()).equals(entry.getKey()), "Prototype identity / 模板身份: " + entry.getKey()); }
            catch (IllegalArgumentException e) { helper.fail("Prototype identity failed / 模板身份失败: " + entry.getKey() + " components=" + entry.getValue().getComponents() + " reason=" + e.getMessage()); }
            helper.assertTrue(EnergyExchange.RULES.require(entry.getKey()).value().signum() > 0, "Variant priced / 型号定价: " + entry.getKey());
        }
        helper.assertTrue(variants > 100 && families.containsAll(Set.of("tacz:modern_kinetic_gun", "tacz:ammo", "tacz:attachment", "lrtactical:throwable", "lrtactical:melee", "lrtactical:consumable")),
                "TaCZ and LR model families loaded / TaCZ 和 LR 型号已加载");
        String key = "tacz:modern_kinetic_gun#tacz:ak47";
        var sample = Catalog.sample(key); p.getInventory().setItem(0, sample);
        ExchangeService.burn(p, 1); ExchangeService.buy(p, key, 1);
        helper.assertTrue(Catalog.identify(p.getMainHandItem()).equals(key), "Gun exchange round trip / 枪械交换往返");
        var loaded = p.getMainHandItem();
        var tag = loaded.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt("GunCurrentAmmoCount", 1); loaded.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        ExchangeService.burn(p, 1); ExchangeService.buy(p, key, 1);
        helper.assertTrue(ItemStack.isSameItemSameComponents(p.getMainHandItem(), Catalog.sample(key)), "Loaded data is discarded; model retained");
        var powder = EnergyExchange.RULES.burnCredit("minecraft:gunpowder", 3);
        var materials = EnergyExchange.RULES.require("minecraft:flint").value().add(EnergyExchange.RULES.require("minecraft:sugar").value().multiply(BigInteger.TWO)).add(EnergyExchange.RULES.require("minecraft:charcoal").value().multiply(BigInteger.valueOf(3)));
        helper.assertTrue(powder.compareTo(materials) <= 0, "TaCZ powder recipe cannot mint energy");
        helper.succeed();
    }

    @GameTest
    public void decoratedItemsAndContainersLoseData(GameTestHelper helper) {
        var p = player(helper);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(50); sword.set(DataComponents.CUSTOM_NAME, Component.literal("Sample"));
        sword.enchant(p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 3);
        p.getInventory().setItem(0, sword);
        ExchangeService.burn(p, 1); ExchangeService.buy(p, "minecraft:diamond_sword", 1);
        helper.assertTrue(ItemStack.isSameItemSameComponents(p.getMainHandItem(), new ItemStack(Items.DIAMOND_SWORD)), "Enchantment/name/damage are not copied");
        var box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.DIAMOND, 64))));
        var spoof = new net.minecraft.nbt.CompoundTag(); spoof.putString("GunId", "tacz:ak47");
        box.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(spoof));
        p.getInventory().setItem(0, box);
        var before = ExchangeService.account(p).energy();
        ExchangeService.burn(p, 1);
        helper.assertTrue(ExchangeService.account(p).energy().subtract(before).equals(EnergyExchange.RULES.burnCredit("minecraft:shulker_box", 1)), "Only shell value, unrelated model fields ignored");
        ExchangeService.buy(p, "minecraft:shulker_box", 1);
        helper.assertTrue(ItemStack.isSameItemSameComponents(p.getMainHandItem(), new ItemStack(Items.SHULKER_BOX)) && p.getInventory().countItem(Items.DIAMOND) == 0, "Contents discarded without duplication");
        helper.succeed();
    }

    @GameTest
    public void travelersBackpackCoverageAndEmptyPurchases(GameTestHelper helper) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("travelersbackpack")) { helper.succeed(); return; }
        int count = 0;
        for (var entry : Catalog.entries().entrySet()) if (entry.getKey().startsWith("travelersbackpack:")) {
            count++; helper.assertTrue(EnergyExchange.RULES.require(entry.getKey()).value().signum() > 0, "Backpack item priced: " + entry.getKey());
        }
        helper.assertTrue(count == 80, "Pinned backpack registry coverage: " + count);
        var p = player(helper); var bag = Catalog.sample("travelersbackpack:standard");
        @SuppressWarnings("unchecked")
        var container = (net.minecraft.core.component.DataComponentType<net.minecraft.world.item.component.ItemContainerContents>) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse("travelersbackpack:backpack_container"));
        helper.assertTrue(container != null, "Real backpack component registered");
        bag.set(container, net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.DIAMOND, 64))));
        bag.set(DataComponents.CUSTOM_NAME, Component.literal("Filled backpack"));
        p.getInventory().setItem(0, bag);
        ExchangeService.burn(p, 1); ExchangeService.buy(p, "travelersbackpack:standard", 1);
        helper.assertTrue(ItemStack.isSameItemSameComponents(p.getMainHandItem(), Catalog.sample("travelersbackpack:standard")), "Backpack returns at default tier with no contents");
        helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0, "Stored diamonds do not escape or duplicate");
        helper.succeed();
    }

    @GameTest
    public void armoryCoverageAndFlightLifecycle(GameTestHelper helper) {
        helper.assertTrue(Armory.ITEMS.size() == 32, "32 registered armory items");
        for (var e : Armory.ITEMS.entrySet()) {
            String key = "energyexchange:" + e.getKey();
            helper.assertTrue(EnergyExchange.RULES.require(key).value().signum() > 0, "Armory priced: " + key);
            helper.assertTrue(Catalog.identify(e.getValue().getDefaultInstance()).equals(key), "Armory identity");
            if (e.getValue() instanceof MatterTool || e.getKey().matches(".*_(helmet|chestplate|leggings|boots)"))
                helper.assertTrue(e.getValue().getDefaultInstance().has(DataComponents.UNBREAKABLE), "Unbreakable equipment");
        }
        var p = player(helper);
        var slots = java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET);
        var names = java.util.List.of("helmet", "chestplate", "leggings", "boots");
        for (int i = 0; i < slots.size(); i++) p.setItemSlot(slots.get(i), new ItemStack(Armory.ITEMS.get("infinity_" + names.get(i))));
        Armory.tick(p);
        helper.assertTrue(Armory.fullTier(p) == 3 && p.getAbilities().mayfly, "Complete infinity suit grants flight");
        var callback = net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.invoker();
        helper.assertTrue(!callback.allowDamage(p, p.damageSources().generic(), 20), "Infinity damage protection");
        helper.assertTrue(callback.allowDamage(p, p.damageSources().fellOutOfWorld(), 20), "Void remains authoritative");
        p.getAbilities().flying = true; p.setItemSlot(slots.getFirst(), ItemStack.EMPTY); Armory.tick(p);
        helper.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying && p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "Unequipping revokes flight and allows safe landing");
        helper.assertTrue(callback.allowDamage(p, p.damageSources().generic(), 20), "Incomplete suit has no invulnerability");
        p.getAbilities().mayfly = true; Armory.tick(p);
        helper.assertTrue(p.getAbilities().mayfly, "Pre-existing external flight preserved");
        helper.succeed();
    }

    @GameTest
    public void areaMiningHonorsProtectionAndBounds(GameTestHelper helper) {
        var p = player(helper);
        var center = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        p.setPos(center.getX() + .5, center.getY() + 1, center.getZ() + .5);
        var tool = (MatterTool) Armory.ITEMS.get("dark_matter_pickaxe"); p.getInventory().setItem(0, new ItemStack(tool));
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) p.level().setBlockAndUpdate(center.offset(a, 0, b), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var denied = center.offset(-1, 0, -1); var chest = center.offset(1, 0, 1); var bedrock = center.offset(-1, 0, 1);
        p.level().setBlockAndUpdate(chest, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        p.level().setBlockAndUpdate(bedrock, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState());
        var active = new java.util.concurrent.atomic.AtomicBoolean(true);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> !active.get() || player != p || !pos.equals(denied));
        try {
            int count = tool.mineArea(p, center, net.minecraft.core.Direction.UP);
            helper.assertTrue(count == 6, "3x3 skips denied block, chest and bedrock: " + count);
            helper.assertTrue(p.level().getBlockState(denied).is(net.minecraft.world.level.block.Blocks.STONE) && p.level().getBlockState(chest).hasBlockEntity() && p.level().getBlockState(bedrock).is(net.minecraft.world.level.block.Blocks.BEDROCK), "Protected blocks preserved");
            helper.assertTrue(tool.mineArea(p, center, net.minecraft.core.Direction.UP) == 0, "Ability cooldown");
            helper.assertTrue(p.getMainHandItem().getDamageValue() == 0, "Mining consumes no durability");
        } finally { active.set(false); }
        helper.succeed();
    }

    @GameTest
    public void armoryCraftsOnVanillaTable(GameTestHelper helper) throws Exception {
        for (String name : Armory.ITEMS.keySet()) {
            var grid = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
            try (var in = getClass().getResourceAsStream("/data/energyexchange/recipe/" + name + ".json")) {
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                if (json.has("pattern")) {
                    var rows = json.getAsJsonArray("pattern"); helper.assertTrue(rows.size() <= 3, "3x3 height");
                    for (int y = 0; y < rows.size(); y++) {
                        String row = rows.get(y).getAsString(); helper.assertTrue(row.length() <= 3, "3x3 width");
                        for (int x = 0; x < row.length(); x++) if (row.charAt(x) != ' ') grid.set(y * 3 + x, Catalog.sample(json.getAsJsonObject("key").get(String.valueOf(row.charAt(x))).getAsString()));
                    }
                } else {
                    var inputs = json.getAsJsonArray("ingredients"); helper.assertTrue(inputs.size() <= 9, "At most nine ingredients");
                    for (int i = 0; i < inputs.size(); i++) grid.set(i, Catalog.sample(inputs.get(i).getAsString()));
                }
            }
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
            var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
            helper.assertTrue(recipe.value().assemble(input).getItem() == Armory.ITEMS.get(name), "Vanilla 3x3 recipe output: " + name);
        }
        helper.succeed();
    }

    private static ResourceManager resources(GameTestHelper helper, String value) {
        var pack = helper.getLevel().getServer().getResourceManager().listPacks().findFirst().orElseThrow();
        var resource = new Resource(pack, () -> new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
        var map = Map.of(Identifier.parse("minecraft:energyexchange/values/dirt.json"), resource);
        return (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(), new Class<?>[]{ResourceManager.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("listResources")) return args[0].equals("energyexchange/values") ? map : Map.of();
                    if (method.getName().equals("getResourceOrThrow")) return helper.getLevel().getServer().getResourceManager().getResourceOrThrow((Identifier) args[0]);
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void reject(GameTestHelper helper, String key, Runnable action) {
        try {
            action.run();
            helper.fail("Expected rejection / 预期拒绝: " + key);
        } catch (IllegalArgumentException exception) {
            helper.assertTrue(("energyexchange.error." + key).equals(exception.getMessage()), "Correct rejection / 正确拒绝: " + key + "; actual / 实际: " + exception.getMessage());
        }
    }
}
