package studio.ykz.energyexchange;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnergyExchange implements ModInitializer {
    public static final String ID = "energyexchange";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final Rules RULES = new Rules();
    public static final AttachmentType<String> ACCOUNT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ID, "account"), builder -> builder.persistent(Codec.STRING).copyOnDeath());

    @Override
    public void onInitialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                Identifier.fromNamespaceAndPath(ID, "values"), (ResourceManagerReloadListener) RULES::load);
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> RULES.pause());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> RULES.complete(success));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> RULES.complete(true));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RULES.clear());
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> ExchangeCommands.register(dispatcher));
    }
}
