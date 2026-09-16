package studio.ykz.energyexchange;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Minecraft-version bridge; no reflection in UI dispatch. */
final class ClientPlatform {
    private ClientPlatform() {}
    static Screen screen(Minecraft client) { return client.gui.screen(); }
    static void setScreen(Minecraft client, Screen screen) { client.gui.setScreen(screen); }
    static boolean overlayActive(Minecraft client) { return client.gui.overlay() != null; }
}
