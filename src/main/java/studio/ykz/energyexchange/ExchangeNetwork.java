package studio.ykz.energyexchange;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class ExchangeNetwork {
    private ExchangeNetwork() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EnergyExchange.ID, path));
    }
    public record Action(int menu, long nonce, long revision, long sequence, int action, String key, int count) implements CustomPacketPayload {
        public static final Type<Action> TYPE = ExchangeNetwork.type("action");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeLong(p.nonce); b.writeLong(p.revision); b.writeLong(p.sequence);
            b.writeByte(p.action); b.writeUtf(p.key, 256); b.writeVarInt(p.count);
        }, b -> new Action(b.readVarInt(), b.readLong(), b.readLong(), b.readLong(), b.readUnsignedByte(), b.readUtf(256), b.readVarInt()));
        public Type<Action> type() { return TYPE; }
    }
    public record Entry(String key, ItemStack stack, String value, String burnRate, boolean learned) {}
    public record Page(int menu, long nonce, int offset, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Page> TYPE = ExchangeNetwork.type("catalog");
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeLong(p.nonce); b.writeVarInt(p.offset); b.writeVarInt(p.entries.size());
            for (var e : p.entries) { b.writeUtf(e.key, 256); ItemStack.STREAM_CODEC.encode(b, e.stack); b.writeUtf(e.value, 128); b.writeUtf(e.burnRate, 260); b.writeBoolean(e.learned); }
        }, b -> {
            int menu = b.readVarInt(); long nonce = b.readLong(); int offset = b.readVarInt(); int size = b.readVarInt();
            if (size < 0 || size > 64 || offset < 0 || offset > 16384) throw new IllegalArgumentException("Invalid catalog bounds");
            var entries = new ArrayList<Entry>();
            for (int i = 0; i < size; i++) entries.add(new Entry(b.readUtf(256), ItemStack.STREAM_CODEC.decode(b), b.readUtf(128), b.readUtf(260), b.readBoolean()));
            return new Page(menu, nonce, offset, List.copyOf(entries));
        });
        public Type<Page> type() { return TYPE; }
    }
    public record State(int menu, long nonce, long revision, long sequence, String balance, String learned, String error, String xpCost, boolean xpEnabled) implements CustomPacketPayload {
        public static final Type<State> TYPE = ExchangeNetwork.type("state");
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeLong(p.nonce); b.writeLong(p.revision); b.writeLong(p.sequence);
            b.writeUtf(p.balance, 128); b.writeUtf(p.learned, 256); b.writeUtf(p.error, 128); b.writeUtf(p.xpCost, 128); b.writeBoolean(p.xpEnabled);
        }, b -> new State(b.readVarInt(), b.readLong(), b.readLong(), b.readLong(), b.readUtf(128), b.readUtf(256), b.readUtf(128), b.readUtf(128), b.readBoolean()));
        public Type<State> type() { return TYPE; }
    }
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Page.TYPE, Page.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (packet, context) -> handle(context.player(), packet));
    }
    public static void handle(ServerPlayer player, Action packet) {
        if (!(player.containerMenu instanceof ExchangeMenu menu) || menu.containerId != packet.menu || menu.nonce != packet.nonce || !menu.stillValid(player)) return;
        long now = player.level().getGameTime();
        if (menu.lastAction != Long.MIN_VALUE && now - menu.lastAction < 2) { state(player, menu, "", "energyexchange.error.busy"); return; }
        menu.lastAction = now;
        String learned = "", error = "";
        try {
            ExchangeService.checkPlayer(player);
            if (packet.sequence != menu.sequence) throw new IllegalArgumentException("energyexchange.error.stale");
            if (packet.action == 0) {
                if (!menu.sentCatalog || menu.catalogRevision != EnergyExchange.RULES.revision()) catalog(player, menu);
            } else {
                if (!menu.sentCatalog || packet.revision != EnergyExchange.RULES.revision()) {
                    catalog(player, menu); throw new IllegalArgumentException("energyexchange.error.stale");
                }
                switch (packet.action) {
                    case 1 -> { learned = Catalog.identify(menu.input.getItem(0)); ExchangeService.burn(player, menu.input.getItem(0).getCount()); }
                    case 2 -> { learned = Catalog.identify(menu.input.getItem(0)); ExchangeService.learn(player); }
                    case 3 -> ExchangeService.buy(player, packet.key, packet.count);
                    case 4 -> ExchangeService.buyExperience(player, packet.count);
                    default -> throw new IllegalArgumentException("energyexchange.error.count");
                }
            }
        } catch (IllegalArgumentException exception) {
            learned = ""; error = exception.getMessage();
            if (error == null || !error.startsWith("energyexchange.error.")) error = "energyexchange.error.internal";
        }
        menu.sequence++;
        state(player, menu, learned, error);
    }
    private static void catalog(ServerPlayer player, ExchangeMenu menu) {
        var known = ExchangeService.account(player).learned();
        var entries = new ArrayList<Entry>();
        for (var entry : Catalog.entries().entrySet()) {
            try {
                var value = EnergyExchange.RULES.require(entry.getKey());
                if (!entry.getValue().getItem().isEnabled(player.level().enabledFeatures())) continue;
                entries.add(new Entry(entry.getKey(), entry.getValue().copy(), value.value().toString(), EnergyExchange.RULES.burnRate(entry.getKey()), known.contains(entry.getKey())));
            } catch (IllegalArgumentException e) { if (!"energyexchange.error.no_value".equals(e.getMessage())) throw e; }
        }
        if (entries.size() > 16384) throw new IllegalArgumentException("energyexchange.error.knowledge_full");
        if (entries.isEmpty()) ServerPlayNetworking.send(player, new Page(menu.containerId, menu.nonce, 0, List.of()));
        for (int offset = 0; offset < entries.size(); offset += 64)
            ServerPlayNetworking.send(player, new Page(menu.containerId, menu.nonce, offset, List.copyOf(entries.subList(offset, Math.min(offset + 64, entries.size())))));
        menu.sentCatalog = true; menu.catalogRevision = EnergyExchange.RULES.revision();
    }
    private static void state(ServerPlayer player, ExchangeMenu menu, String learned, String error) {
        String balance = "0";
        try { balance = ExchangeService.account(player).energy().toString(); }
        catch (IllegalArgumentException e) { error = "energyexchange.error.account_corrupt"; }
        ServerPlayNetworking.send(player, new State(menu.containerId, menu.nonce, EnergyExchange.RULES.revision(), menu.sequence,
                balance, learned, error, ExchangeConfig.server.xpCost(), ExchangeConfig.server.xpEnabled()));
    }
}
