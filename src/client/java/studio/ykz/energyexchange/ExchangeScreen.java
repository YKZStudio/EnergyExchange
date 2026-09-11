package studio.ykz.energyexchange;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

public final class ExchangeScreen extends AbstractContainerScreen<ExchangeMenu> {
    private final Map<String, ExchangeNetwork.Entry> catalog = new LinkedHashMap<>();
    private List<ExchangeNetwork.Entry> filtered = List.of();
    private final List<Button> cells = new ArrayList<>();
    private ExchangeNetwork.State state;
    private ExchangeConfig config;
    private EditBox search;
    private String selected = "";
    private int page;
    private boolean waiting;
    private int retryTicks;
    private Button buyOne, buyStack, xp, previous, next;
    public ExchangeScreen(ExchangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 276, 234); inventoryLabelX = 57; inventoryLabelY = 140;
        try { config = ExchangeConfig.load(); } catch (IllegalStateException e) { config = ExchangeConfig.DEFAULT; }
    }
    private Button button(int x, int y, int w, String text, Runnable action) {
        return addRenderableWidget(Button.builder(Messages.text(text), b -> action.run()).bounds(leftPos + x, topPos + y, w, 18).build());
    }
    @Override protected void init() {
        super.init(); cells.clear();
        search = addRenderableWidget(new EditBox(font, leftPos + 86, topPos + 35, 174, 18, Messages.text("ui.search")));
        search.setMaxLength(128); search.setHint(Messages.text("ui.search")); search.setResponder(s -> { page = 0; filter(); });
        button(6, 84, 70, "ui.burn", () -> send(1, "", 0));
        button(6, 104, 70, "ui.learn", () -> send(2, "", 0));
        xp = button(6, 124, 70, "ui.xp", () -> send(4, "", 10));
        for (int i = 0; i < 24; i++) {
            final int index = i;
            cells.add(button(86 + (i % 8) * 22, 58 + (i / 8) * 18, 20, "ui.empty", () -> {
                int actual = page * 24 + index; if (actual < filtered.size()) { selected = filtered.get(actual).key(); updateButtons(); }
            }));
        }
        previous = button(86, 116, 20, "ui.previous", () -> { if (page > 0) page--; updateButtons(); });
        next = button(108, 116, 20, "ui.next", () -> { if ((page + 1) * 24 < filtered.size()) page++; updateButtons(); });
        buyOne = button(134, 116, 54, "ui.buy_one", () -> send(3, selected, 1));
        buyStack = button(190, 116, 72, "ui.buy_stack", () -> {
            var entry = catalog.get(selected); if (entry != null) send(3, selected, entry.stack().getMaxStackSize());
        });
        filter(); send(0, "", 0);
    }
    private void send(int action, String key, int count) {
        if (waiting) return;
        waiting = true; retryTicks = 0;
        ClientPlayNetworking.send(new ExchangeNetwork.Action(menu.containerId, menu.nonce, state == null ? -1 : state.revision(), state == null ? 0 : state.sequence(), action, key, count));
        updateButtons();
    }
    public void receive(ExchangeNetwork.Page packet) {
        if (packet.menu() != menu.containerId || packet.nonce() != menu.nonce) return;
        if (packet.offset() == 0) catalog.clear();
        for (var entry : packet.entries()) catalog.put(entry.key(), entry);
        filter();
    }
    public void receive(ExchangeNetwork.State packet) {
        if (packet.menu() != menu.containerId || packet.nonce() != menu.nonce) return;
        state = packet; waiting = false;
        var learned = catalog.get(packet.learned());
        if (learned != null) catalog.put(learned.key(), new ExchangeNetwork.Entry(learned.key(), learned.stack(), learned.value(), true));
        if (!packet.error().isEmpty()) minecraft.player.displayClientMessage(Messages.text(packet.error()), true);
        filter();
    }
    private void filter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        filtered = catalog.values().stream().filter(e -> config.showUnlearned() || e.learned())
                .filter(e -> e.key().contains(query) || e.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        page = Math.min(page, Math.max(0, (filtered.size() - 1) / 24)); updateButtons();
    }
    private void updateButtons() {
        if (buyOne == null) return;
        var choice = catalog.get(selected);
        boolean canBuy = !waiting && choice != null && choice.learned() && state != null;
        buyOne.active = canBuy; buyStack.active = canBuy;
        xp.active = !waiting && state != null && state.xpEnabled();
        if (state != null) xp.setTooltip(Tooltip.create(Messages.text("ui.xp_price", state.xpCost())));
        previous.active = page > 0; next.active = (page + 1) * 24 < filtered.size();
        for (int i = 0; i < cells.size(); i++) {
            int actual = page * 24 + i; Button cell = cells.get(i); cell.active = actual < filtered.size();
            if (cell.active) {
                var e = filtered.get(actual);
                cell.setTooltip(Tooltip.create(Component.empty().append(e.stack().getHoverName()).append("\n" + e.key() + "\n" + e.value() + " Energy\n")
                        .append(Messages.text(e.learned() ? "ui.known" : "ui.unknown"))));
            } else cell.setTooltip(null);
        }
    }
    private String number(String value) {
        if (!config.compactNumbers() || value.length() <= 12) return value;
        return value.charAt(0) + "." + value.substring(1, 4) + "e" + (value.length() - 1);
    }
    @Override protected void containerTick() {
        super.containerTick();
        // A sync retry never repeats a purchase. / 超时仅请求同步，不重发购买。
        if (waiting && ++retryTicks > 100) { waiting = false; send(0, "", 0); }
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (search.isFocused() && event.key() != 256) return search.keyPressed(event);
        return super.keyPressed(event);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC6C6C6);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFFFFFFFF);
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 16, y + slot.y + 16, 0xFF373737);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B8B8B);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        for (int i = 0; i < 24 && page * 24 + i < filtered.size(); i++) {
            var entry = filtered.get(page * 24 + i); int x = leftPos + 88 + i % 8 * 22, y = topPos + 59 + i / 8 * 18;
            g.item(entry.stack(), x, y);
            if (!entry.learned()) g.fill(x, y, x + 16, y + 16, 0x66808080);
            if (entry.key().equals(selected)) g.outline(x - 1, y - 1, 18, 18, 0xFFFFFF00);
        }
        if (state != null) {
            g.text(font, font.plainSubstrByWidth("Energy: " + number(state.balance()), 256), leftPos + 8, topPos + 22, 0xFF404040, false);
            if (mouseX >= leftPos + 8 && mouseX < leftPos + 268 && mouseY >= topPos + 20 && mouseY < topPos + 33)
                g.setTooltipForNextFrame(font, List.of(Component.literal(state.balance() + " Energy")), Optional.empty(), mouseX, mouseY);
        }
        g.text(font, (page + 1) + "/" + Math.max(1, (filtered.size() + 23) / 24), leftPos + 8, topPos + 40, 0xFF404040, false);
        extractTooltip(g, mouseX, mouseY);
    }
}
