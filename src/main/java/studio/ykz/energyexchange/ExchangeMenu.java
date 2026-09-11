package studio.ykz.energyexchange;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ExchangeMenu extends AbstractContainerMenu {
    public final SimpleContainer input = new SimpleContainer(1);
    public final long nonce;
    private final BlockPos position;
    private final int anchor;
    long lastAction = Long.MIN_VALUE;
    boolean sentCatalog;
    long sequence;
    long catalogRevision = -1;
    public ExchangeMenu(int id, Inventory inventory, long nonce, BlockPos position, int anchor) {
        super(ExchangeContent.MENU, id); this.nonce = nonce; this.position = position; this.anchor = anchor;
        addSlot(new Slot(input, 0, 29, 60));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col + row * 9 + 9, 57 + col * 18, 151 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col, 57 + col * 18, 209);
    }
    private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return index != anchor; }
            @Override public boolean mayPlace(ItemStack stack) { return index != anchor; }
        });
    }
    @Override public boolean stillValid(Player player) {
        if (player.level().isClientSide()) return true;
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) return false;
        if (position != null) return player.level().getBlockState(position).is(ExchangeContent.TABLE)
                && player.distanceToSqr(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5) <= 64;
        return anchor >= 0 && player.getInventory().getItem(anchor).is(ExchangeContent.TABLET)
                && (anchor == 40 || player.getInventory().getSelectedSlot() == anchor);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack old = stack.copy();
        if (!(index == 0 ? moveItemStackTo(stack, 1, 37, true) : moveItemStackTo(stack, 0, 1, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return old;
    }
    @Override public void removed(Player player) { super.removed(player); if (!player.level().isClientSide()) clearContainer(player, input); }
}
