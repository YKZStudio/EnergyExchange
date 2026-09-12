package studio.ykz.energyexchange;

import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Public prices only: no wallet or knowledge is shared. Bounded pages replace a client snapshot atomically. */
public final class PriceSync {
    public record Price(String key, String value) {}
    public record Page(long revision, int offset, boolean last, List<Price> prices) implements CustomPacketPayload {
        public static final Type<Page> TYPE = new Type<>(Armory.id("prices"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> CODEC = StreamCodec.of((b, p) -> {
            b.writeLong(p.revision); b.writeVarInt(p.offset); b.writeBoolean(p.last); b.writeVarInt(p.prices.size());
            for (var e : p.prices) { b.writeUtf(e.key, 256); b.writeUtf(e.value, 128); }
        }, b -> {
            long revision = b.readLong(); int offset = b.readVarInt(); boolean last = b.readBoolean(); int size = b.readVarInt();
            if (offset < 0 || offset > 16384 || size < 0 || size > 128 || offset + size > 16384) throw new IllegalArgumentException("Invalid price page");
            var entries = new ArrayList<Price>();
            for (int i = 0; i < size; i++) entries.add(new Price(b.readUtf(256), b.readUtf(128)));
            return new Page(revision, offset, last, List.copyOf(entries));
        });
        public Type<Page> type() { return TYPE; }
    }
    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(Page.TYPE, Page.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player));
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> server.getPlayerList().getPlayers().forEach(PriceSync::clear));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> server.getPlayerList().getPlayers().forEach(PriceSync::send));
    }
    private static void clear(ServerPlayer player) { ServerPlayNetworking.send(player, new Page(EnergyExchange.RULES.revision(), 0, true, List.of())); }
    public static void send(ServerPlayer player) {
        var prices = new ArrayList<Price>();
        try {
            EnergyExchange.RULES.checkReady();
            for (var e : Catalog.entries().entrySet()) {
                try {
                    if (e.getValue().getItem().isEnabled(player.level().enabledFeatures())) prices.add(new Price(e.getKey(), EnergyExchange.RULES.require(e.getKey()).value().toString()));
                } catch (IllegalArgumentException ignored) { }
            }
        } catch (IllegalArgumentException ignored) { }
        if (prices.isEmpty() || prices.size() > 16384) { clear(player); return; }
        for (int offset = 0; offset < prices.size(); offset += 128) {
            int end = Math.min(offset + 128, prices.size());
            ServerPlayNetworking.send(player, new Page(EnergyExchange.RULES.revision(), offset, end == prices.size(), List.copyOf(prices.subList(offset, end))));
        }
    }
}
