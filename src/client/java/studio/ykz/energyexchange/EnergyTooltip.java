package studio.ykz.energyexchange;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

public final class EnergyTooltip {
    private static Map<String, String> values = Map.of();
    private static final Map<String, String> pending = new HashMap<>();
    private static long revision;
    private static int nextOffset;
    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayNetworking.registerGlobalReceiver(PriceSync.Page.TYPE, (packet, context) -> receive(packet));
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> append(stack, flag, lines));
    }
    public static void reset() { values = Map.of(); pending.clear(); nextOffset = 0; revision = Long.MIN_VALUE; }
    public static void receive(PriceSync.Page page) {
        if (page.offset() == 0) { pending.clear(); values = Map.of(); nextOffset = 0; revision = page.revision(); }
        if (page.revision() != revision || page.offset() != nextOffset) { reset(); return; }
        for (var entry : page.prices()) {
            try { studio.ykz.energyexchange.core.Energy.parse(entry.value()); }
            catch (IllegalArgumentException e) { reset(); return; }
            if (pending.putIfAbsent(entry.key(), entry.value()) != null) { reset(); return; }
        }
        nextOffset += page.prices().size();
        if (page.last()) { values = Map.copyOf(pending); pending.clear(); }
    }
    public static String value(ItemStack stack) { return values.get(Catalog.key(stack)); }
    public static void append(ItemStack stack, TooltipFlag flag, List<Component> lines) {
        if (!flag.isAdvanced() || stack.isEmpty()) return;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).getString().equals(id)) {
            String value = value(stack);
            lines.add(i, Component.translatable("energyexchange.tooltip.energy", value == null ? "—" : value).withStyle(ChatFormatting.AQUA));
            return;
        }
    }
}
