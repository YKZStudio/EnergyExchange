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
        reject(helper, "components", () -> ExchangeService.burn(player, 1));
        helper.assertTrue(named.getCount() == 2, "Named stack untouched / 命名物品不变");
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 2));
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
