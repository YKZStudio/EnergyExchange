package studio.ykz.energyexchange;

import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Consumer;

public final class MatterTool extends Item {
    private final int tier;
    private final String kind;
    public MatterTool(Properties properties, int tier, String kind) { super(properties); this.tier = tier; this.kind = kind; }
    private boolean combat() { return kind.equals("sword") || kind.equals("katar"); }
    private boolean appropriate(BlockState state) {
        return switch (kind) {
            case "axe", "katar" -> state.is(BlockTags.MINEABLE_WITH_AXE);
            case "shovel" -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case "morning_star" -> state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case "pickaxe" -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            default -> false;
        };
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return appropriate(state) ? tier == 3 ? 128 : tier * 16 : super.getDestroySpeed(stack, state); }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return appropriate(state) && !state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL) || super.isCorrectToolForDrops(stack, state); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (kind.equals("sword")) return super.useOn(context);
        if (context.getPlayer() instanceof ServerPlayer p && context.getHand() == InteractionHand.MAIN_HAND) mineArea(p, context.getClickedPos(), context.getClickedFace());
        return InteractionResult.SUCCESS;
    }
    public int mineArea(ServerPlayer p, BlockPos center, Direction face) {
        if (p.getMainHandItem().getItem() != this || !p.isAlive() || p.gameMode.getGameModeForPlayer() != GameType.SURVIVAL || !p.isWithinBlockInteractionRange(center, 1) || p.getCooldowns().isOnCooldown(p.getMainHandItem())) return 0;
        var level = p.level(); int radius = p.isShiftKeyDown() ? 0 : tier; int broken = 0;
        p.getCooldowns().addCooldown(p.getMainHandItem(), 10);
        for (int a = -radius; a <= radius; a++) for (int b = -radius; b <= radius; b++) {
            var pos = switch (face.getAxis()) { case X -> center.offset(0, a, b); case Y -> center.offset(a, 0, b); case Z -> center.offset(a, b, 0); };
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.getServer().isUnderSpawnProtection(level, pos, p) || !level.mayInteract(p, pos)) continue;
            var state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0 || !appropriate(state) || !p.hasCorrectToolForDrops(state)) continue;
            // Uses vanilla + Fabric break callbacks; never raw removeBlock or synthetic drops.
            if (p.gameMode.destroyBlock(pos)) broken++;
        }
        return broken;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!combat()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) sweep(p);
        return InteractionResult.SUCCESS;
    }
    public int sweep(ServerPlayer p) {
        if (!combat() || p.getMainHandItem().getItem() != this || !p.isAlive() || p.gameMode.getGameModeForPlayer() != GameType.SURVIVAL || p.getCooldowns().isOnCooldown(p.getMainHandItem())) return 0;
        p.getCooldowns().addCooldown(p.getMainHandItem(), 20); int hits = 0, examined = 0; double radius = tier * 2 + 1;
        for (var mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius), e -> e instanceof Enemy && !(e instanceof TamableAnimal tame && tame.isTame()) && e.isAlive() && p.distanceToSqr(e) <= radius * radius && p.hasLineOfSight(e))) {
            if (examined++ >= 64) break;
            if (mob.hurtServer(p.level(), p.damageSources().playerAttack(p), tier == 3 ? 1024 : tier * 12)) hits++;
        }
        return hits;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> text, TooltipFlag flag) {
        if (!kind.equals("sword")) text.accept(Messages.text("gear.mine", tier * 2 + 1, tier * 2 + 1).copy().withStyle(ChatFormatting.GRAY));
        if (combat()) text.accept(Messages.text("gear.sweep", tier * 2 + 1).copy().withStyle(ChatFormatting.GRAY));
    }
}
