package studio.ykz.energyexchange;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ExchangeClient implements ClientModInitializer {
    public void onInitializeClient() {
        MenuScreens.register(ExchangeContent.MENU, ExchangeScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(ExchangeNetwork.Page.TYPE, (packet, context) -> {
            if (context.client().gui.screen() instanceof ExchangeScreen screen) screen.receive(packet);
        });
        ClientPlayNetworking.registerGlobalReceiver(ExchangeNetwork.State.TYPE, (packet, context) -> {
            if (context.client().gui.screen() instanceof ExchangeScreen screen) screen.receive(packet);
        });
    }
}
