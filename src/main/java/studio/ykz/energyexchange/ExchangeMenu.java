package studio.ykz.energyexchange;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ExchangeMenu extends AbstractContainerMenu {
    public static final int INPUT_LIMIT = 256;
    public final SimpleContainer input = new SimpleContainer(1) {
        @Override public int getMaxStackSize() { return INPUT_LIMIT; }
        @Override public int getMaxStackSize(ItemStack stack) { return INPUT_LIMIT; }
    };
    public final long nonce;
    private final BlockPos position;
    private final int anchor;
    long lastAction = Long.MIN_VALUE;
    boolean sentCatalog;
    long sequence;
    long catalogRevision = -1;
    public ExchangeMenu(int id, Inventory inventory, long nonce, BlockPos position, int anchor) {
        super(ExchangeContent.MENU, id); this.nonce = nonce; this.position = position; this.anchor = anchor;
        addSlot(new Slot(input, 0, 43, 60) {
            @Override public int getMaxStackSize() { return INPUT_LIMIT; }
            @Override public int getMaxStackSize(ItemStack stack) { return INPUT_LIMIT; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col + row * 9 + 9, 69 + col * 18, 151 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col, 69 + col * 18, 209);
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
    private int deposit(ItemStack source, int requested) {
        ItemStack target = input.getItem(0);
        if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, source)) return 0;
        int amount = Math.min(Math.min(requested, source.getCount()), INPUT_LIMIT - target.getCount());
        if (amount <= 0) return 0;
        if (target.isEmpty()) input.setItem(0, source.copyWithCount(amount)); else target.grow(amount);
        source.shrink(amount); input.setChanged(); return amount;
    }
    @Override public boolean canDragTo(Slot slot) { return slot.index != 0 && super.canDragTo(slot); }
    @Override public void clicked(int index, int button, ContainerInput type, Player player) {
        if (index != 0) { super.clicked(index, button, type, player); return; }
        if (!stillValid(player)) return;
        ItemStack stack = input.getItem(0), carried = getCarried();
        if (type == ContainerInput.PICKUP && (button == 0 || button == 1)) {
            if (carried.isEmpty()) {
                if (!stack.isEmpty()) setCarried(input.removeItem(0, Math.min(stack.getMaxStackSize(), button == 0 ? stack.getCount() : (stack.getCount() + 1) / 2)));
            } else if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, carried)) {
                deposit(carried, button == 0 ? carried.getCount() : 1);
                if (carried.isEmpty()) setCarried(ItemStack.EMPTY);
            } else if (stack.getCount() <= stack.getMaxStackSize() && carried.getCount() <= INPUT_LIMIT) {
                input.setItem(0, carried); setCarried(stack);
            }
        } else if (type == ContainerInput.SWAP && (button >= 0 && button < 9 || button == 40) && button != anchor) {
            ItemStack hotbar = player.getInventory().getItem(button);
            if (hotbar.isEmpty()) player.getInventory().setItem(button, input.removeItem(0, stack.getMaxStackSize()));
            else if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, hotbar)) deposit(hotbar, hotbar.getCount());
            else if (stack.getCount() <= stack.getMaxStackSize() && hotbar.getCount() <= INPUT_LIMIT) {
                input.setItem(0, hotbar); player.getInventory().setItem(button, stack);
            }
        } else if (type == ContainerInput.THROW && carried.isEmpty() && !stack.isEmpty() && (button == 0 || button == 1)) {
            int remaining = button == 0 ? 1 : stack.getCount();
            while (remaining > 0) { int count = Math.min(remaining, stack.getMaxStackSize()); ServerPlatform.drop(player, input.removeItem(0, count)); remaining -= count; }
        } else if (type == ContainerInput.QUICK_MOVE) quickMoveStack(player, 0);
        else if (type == ContainerInput.PICKUP_ALL) super.clicked(index, button, type, player);
        input.setChanged(); player.getInventory().setChanged(); broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack old = stack.copy();
        if (index == 0) {
            boolean changed = false;
            while (!stack.isEmpty() && moveItemStackTo(stack, 1, 37, true)) changed = true;
            if (!changed) return ItemStack.EMPTY;
        } else if (deposit(stack, stack.getCount()) == 0) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return old;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            ItemStack remaining = input.removeItemNoUpdate(0);
            while (!remaining.isEmpty()) {
                var part = new SimpleContainer(1);
                part.setItem(0, remaining.split(Math.min(remaining.getCount(), remaining.getMaxStackSize())));
                clearContainer(player, part);
            }
        }
    }
}
