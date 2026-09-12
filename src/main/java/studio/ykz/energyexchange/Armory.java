package studio.ykz.energyexchange;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.util.Unit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import java.util.*;

/** Independent Fabric implementation; see the asset provenance in THIRD_PARTY_NOTICES.md. */
public final class Armory {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<Item, Integer> ARMOR_TIERS = new IdentityHashMap<>();
    private static final Set<ServerPlayer> GRANTED_FLIGHT = Collections.newSetFromMap(new WeakHashMap<>());
    private Armory() {}
    static Identifier id(String name) { return Identifier.fromNamespaceAndPath(EnergyExchange.ID, name); }
    private static Item.Properties properties(String name) { return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name))).fireResistant().rarity(Rarity.EPIC); }
    private static Item add(String name, Item item) { ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, id(name), item)); return item; }
    public static void init() {
        for (String name : List.of("dark_matter", "red_matter", "crystal_matrix_ingot", "neutronium_ingot", "infinity_catalyst", "infinity_ingot")) add(name, new Item(properties(name)));
        String[] tiers = {"dark_matter", "red_matter", "infinity"};
        for (int tier = 1; tier <= 3; tier++) {
            String prefix = tiers[tier - 1];
            var material = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 8192, tier == 3 ? 128 : tier * 16, 0, 25, ItemTags.NETHERITE_TOOL_MATERIALS);
            for (String kind : List.of("sword", "pickaxe", "axe", "shovel")) tool(prefix + "_" + kind, kind, tier, material);
            var armor = new ArmorMaterial(512, Map.of(ArmorType.HELMET, 4 + tier, ArmorType.CHESTPLATE, 8 + tier * 2, ArmorType.LEGGINGS, 6 + tier * 2, ArmorType.BOOTS, 4 + tier), 25,
                    SoundEvents.ARMOR_EQUIP_NETHERITE, tier * 4, tier * .1F, ItemTags.NETHERITE_TOOL_MATERIALS, ResourceKey.create(EquipmentAssets.ROOT_ID, id(prefix)));
            for (var type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
                String suffix = switch (type) { case HELMET -> "helmet"; case CHESTPLATE -> "chestplate"; case LEGGINGS -> "leggings"; default -> "boots"; };
                String name = prefix + "_" + suffix;
                final int rank = tier;
                var item = add(name, new Item(properties(name).humanoidArmor(armor, type).component(DataComponents.UNBREAKABLE, Unit.INSTANCE)) {
                    @Override public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> text, TooltipFlag flag) {
                        text.accept(Messages.text("gear.armor." + rank).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                });
                ARMOR_TIERS.put(item, tier);
            }
        }
        var red = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 8192, 48, 0, 25, ItemTags.NETHERITE_TOOL_MATERIALS);
        tool("red_matter_katar", "katar", 2, red); tool("red_matter_morning_star", "morning_star", 2, red);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> ITEMS.values().forEach(entries::accept));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(Armory::tick));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> revokeFlight(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer p) revokeFlight(p); });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer p && p.isAlive() && fullTier(p) == 3 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)));
    }
    private static void tool(String name, String kind, int tier, ToolMaterial material) {
        var p = properties(name); if (tier == 3) p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); float damage = tier == 3 ? 1023 : tier == 2 ? 23 : 15;
        switch (kind) {
            case "sword", "katar" -> p.sword(material, damage, -2.4F);
            case "axe" -> p.axe(material, damage, -2.8F);
            case "shovel" -> p.shovel(material, damage / 2, -3F);
            default -> p.pickaxe(material, damage / 2, -2.8F);
        }
        add(name, new MatterTool(p.component(DataComponents.UNBREAKABLE, Unit.INSTANCE), tier, kind));
    }
    public static int fullTier(ServerPlayer p) {
        int min = 3;
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) min = Math.min(min, ARMOR_TIERS.getOrDefault(p.getItemBySlot(slot).getItem(), 0));
        return min;
    }
    public static void tick(ServerPlayer p) {
        int tier = p.isAlive() && !p.isSpectator() ? fullTier(p) : 0;
        if (tier == 3 && !p.getAbilities().mayfly && !p.isCreative()) { p.getAbilities().mayfly = true; GRANTED_FLIGHT.add(p); p.onUpdateAbilities(); }
        if (tier != 3) revokeFlight(p);
        if (tier > 0 && p.tickCount % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
            if (tier >= 2) {
                p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
            }
            if (tier == 3) { p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false)); p.getFoodData().eat(1, .5F); }
        }
    }
    private static void revokeFlight(ServerPlayer p) {
        if (!GRANTED_FLIGHT.remove(p) || p.isCreative() || p.isSpectator()) return;
        boolean flying = p.getAbilities().flying;
        p.getAbilities().mayfly = false; p.getAbilities().flying = false; p.onUpdateAbilities();
        if (flying && p.isAlive()) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
    }
}
