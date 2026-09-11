package studio.ykz.energyexchange;

import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class ExchangeContent {
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(EnergyExchange.ID, path); }
    public static final Block TABLE = Registry.register(BuiltInRegistries.BLOCK, id("transmutation_table"),
            new Block(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion().setId(ResourceKey.create(Registries.BLOCK, id("transmutation_table")))) {
                @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
                    return Block.box(0, 0, 0, 16, 3, 16);
                }
                @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
                    if (player instanceof ServerPlayer server) open(server, pos, -1);
                    return InteractionResult.SUCCESS;
                }
            });
    public static final Item TABLE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("transmutation_table"),
            new BlockItem(TABLE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("transmutation_table"))).useBlockDescriptionPrefix()));
    public static final Item TABLET = Registry.register(BuiltInRegistries.ITEM, id("transmutation_tablet"),
            new Item(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, id("transmutation_tablet")))) {
                @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                    if (player instanceof ServerPlayer server) open(server, null, hand == InteractionHand.OFF_HAND ? 40 : player.getInventory().getSelectedSlot());
                    return InteractionResult.SUCCESS;
                }
            });
    public static final ExtendedMenuType<ExchangeMenu, Opening> MENU = Registry.register(BuiltInRegistries.MENU, id("exchange"),
            new ExtendedMenuType<>((id, inventory, data) -> new ExchangeMenu(id, inventory, data.nonce(), null, data.anchor()), Opening.CODEC));
    public record Opening(long nonce, int anchor) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Opening> CODEC = StreamCodec.of((buf, data) -> {
            buf.writeLong(data.nonce); buf.writeVarInt(data.anchor);
        }, buf -> new Opening(buf.readLong(), buf.readVarInt()));
    }
    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> { entries.accept(TABLE_ITEM); entries.accept(TABLET); });
    }
    private static void open(ServerPlayer player, BlockPos pos, int anchor) {
        try { ExchangeService.checkPlayer(player); }
        catch (IllegalArgumentException e) { player.sendOverlayMessage(Messages.text(e.getMessage())); return; }
        long nonce = player.getRandom().nextLong();
        player.openMenu(new ExtendedMenuProvider<Opening>() {
            public Opening getScreenOpeningData(ServerPlayer ignored) { return new Opening(nonce, anchor); }
            public Component getDisplayName() { return Messages.text("ui.title"); }
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) { return new ExchangeMenu(id, inventory, nonce, pos, anchor); }
        });
    }
}
