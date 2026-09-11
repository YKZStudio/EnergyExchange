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
    private final List<Button> purchases = new ArrayList<>();
    private static final int[] QUANTITIES = {64, 32, 16, 1};
    private Button xp, burn, browse, previous, next;
    private boolean showAll;
    public ExchangeScreen(ExchangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 300, 234); inventoryLabelX = 69; inventoryLabelY = 140;
        try { config = ExchangeConfig.load(); } catch (IllegalStateException e) { config = ExchangeConfig.DEFAULT; }
        showAll = config.showUnlearned();
    }
    private Button button(int x, int y, int w, String text, Runnable action) {
        return addRenderableWidget(Button.builder(text.isEmpty() ? Component.empty() : Messages.text(text), b -> action.run()).bounds(leftPos + x, topPos + y, w, 18).build());
    }
    @Override protected void init() {
        String oldQuery = search == null ? "" : search.getValue();
        super.init(); cells.clear(); purchases.clear();
        search = addRenderableWidget(new EditBox(font, leftPos + 106, topPos + 35, 184, 18, Messages.text("ui.search")));
        search.setMaxLength(128); search.setHint(Messages.text("ui.search")); search.setResponder(s -> { page = 0; filter(); });
        burn = button(8, 84, 88, "ui.burn", () -> send(1, "", 0));
        burn.setTooltip(Tooltip.create(Messages.text("ui.burn_hint")));
        xp = button(8, 106, 88, "ui.xp", () -> send(4, "", 10));
        browse = button(8, 35, 88, showAll ? "ui.all" : "ui.learned_only", this::toggleBrowse);
        for (int i = 0; i < 24; i++) {
            final int index = i;
            cells.add(button(106 + (i % 8) * 23, 58 + (i / 8) * 18, 22, "", () -> {
                int actual = page * 24 + index; if (actual < filtered.size()) { selected = filtered.get(actual).key(); updateButtons(); }
            }));
        }
        previous = button(246, 158, 20, "ui.previous", () -> { if (page > 0) page--; updateButtons(); });
        next = button(270, 158, 20, "ui.next", () -> { if ((page + 1) * 24 < filtered.size()) page++; updateButtons(); });
        for (int i = 0; i < QUANTITIES.length; i++) {
            int count = QUANTITIES[i];
            purchases.add(button(148 + i * 36, 116, 34, "ui.quantity_" + count, () -> send(3, selected, count)));
        }
        search.setValue(oldQuery);
        filter(); send(0, "", 0);
    }
    boolean isReady() { return state != null && !waiting && !catalog.isEmpty(); }
    void toggleBrowse() { showAll = !showAll; page = 0; browse.setMessage(Messages.text(showAll ? "ui.all" : "ui.learned_only")); filter(); }
    void select(String key) { selected = key; updateButtons(); }
    void search(String query) { search.setValue(query); }
    List<String> visibleKeys() { return filtered.stream().map(ExchangeNetwork.Entry::key).toList(); }
    boolean canPurchase(int count) { for (int i = 0; i < 4; i++) if (QUANTITIES[i] == count) return purchases.get(i).active; return false; }
    void request(int action, String key, int count) { send(action, key, count); }
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
        if (learned != null) catalog.put(learned.key(), new ExchangeNetwork.Entry(learned.key(), learned.stack(), learned.value(), learned.burnRate(), true));
        if (!packet.error().isEmpty()) minecraft.player.sendOverlayMessage(Messages.text(packet.error()));
        filter();
    }
    private void filter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        filtered = catalog.values().stream().filter(e -> showAll || e.learned())
                .filter(e -> e.key().contains(query) || e.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                        || config.pinyinSearch() && studio.ykz.energyexchange.core.PinyinSearch.matches(e.stack().getHoverName().getString(), query))
                .sorted(Comparator.comparing(ExchangeNetwork.Entry::learned).reversed().thenComparing(e -> e.stack().getHoverName().getString()).thenComparing(ExchangeNetwork.Entry::key)).toList();
        if (filtered.stream().noneMatch(e -> e.key().equals(selected))) selected = "";
        page = Math.min(page, Math.max(0, (filtered.size() - 1) / 24)); updateButtons();
    }
    private void updateButtons() {
        if (purchases.size() != 4) return;
        var choice = catalog.get(selected);
        boolean canBuy = !waiting && choice != null && choice.learned() && state != null;
        for (int i = 0; i < purchases.size(); i++) {
            int count = QUANTITIES[i];
            purchases.get(i).active = canBuy && studio.ykz.energyexchange.core.PurchaseQuantity.allowed(count, choice.stack().getMaxStackSize())
                    && new java.math.BigInteger(state.balance()).compareTo(new java.math.BigInteger(choice.value()).multiply(java.math.BigInteger.valueOf(count))) >= 0;
        }
        burn.active = !waiting && menu.input.getItem(0).getCount() > 0;
        xp.active = !waiting && state != null && state.xpEnabled() && new java.math.BigInteger(state.balance()).compareTo(new java.math.BigInteger(state.xpCost()).multiply(java.math.BigInteger.TEN)) >= 0;
        if (state != null) xp.setTooltip(Tooltip.create(Messages.text("ui.xp_price", state.xpCost())));
        previous.active = page > 0; next.active = (page + 1) * 24 < filtered.size();
        for (int i = 0; i < cells.size(); i++) {
            int actual = page * 24 + i; Button cell = cells.get(i); cell.active = actual < filtered.size(); cell.visible = cell.active;
            if (cell.active) {
                var e = filtered.get(actual);
                cell.setTooltip(Tooltip.create(Component.empty().append(e.stack().getHoverName()).append("\n" + e.key() + "\n").append(Messages.text("ui.price", e.value())).append("\n")
                        .append(Messages.text("ui.burn_rate", e.burnRate())).append("\n")
                        .append(Messages.text(e.learned() ? "ui.known" : "ui.unknown"))));
            } else cell.setTooltip(null);
        }
    }
    private String number(String value) {
        if (!config.compactNumbers() || value.length() <= 12) return value;
        return value.charAt(0) + "." + value.substring(1, 4) + "e" + (value.length() - 1);
    }
    @Override protected void containerTick() {
        super.containerTick(); updateButtons();
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
        if (!filtered.isEmpty()) for (int i = 0; i < 24; i++) {
            int cx = x + 108 + i % 8 * 23, cy = y + 58 + i / 8 * 18;
            g.fill(cx, cy, cx + 18, cy + 18, 0xFFFFFFFF);
            g.fill(cx, cy, cx + 17, cy + 17, 0xFF373737);
            g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0xFF8B8B8B);
        }
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFFFFFFFF);
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 16, y + slot.y + 16, 0xFF373737);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B8B8B);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        for (int i = 0; i < 24 && page * 24 + i < filtered.size(); i++) {
            var entry = filtered.get(page * 24 + i); int x = leftPos + 109 + i % 8 * 23, y = topPos + 59 + i / 8 * 18;
            g.item(entry.stack(), x, y);
            if (!entry.learned()) g.fill(x, y, x + 16, y + 16, 0x66808080);
            if (entry.key().equals(selected)) g.outline(x - 1, y - 1, 18, 18, 0xFFFFFF00);
        }
        if (state != null) {
            g.text(font, font.plainSubstrByWidth(Messages.text("balance", number(state.balance())).getString(), 284), leftPos + 8, topPos + 22, 0xFF404040, false);
            if (mouseX >= leftPos + 8 && mouseX < leftPos + 268 && mouseY >= topPos + 20 && mouseY < topPos + 33)
                g.setTooltipForNextFrame(font, List.of(Messages.text("balance", state.balance())), Optional.empty(), mouseX, mouseY);
        }
        g.text(font, (page + 1) + "/" + Math.max(1, (filtered.size() + 23) / 24), leftPos + 246, topPos + 143, 0xFF404040, false);
        g.text(font, Messages.text("ui.purchase"), leftPos + 106, topPos + 121, 0xFF404040, false);
        if (filtered.isEmpty()) g.textWithWordWrap(font, Messages.text(search.getValue().isBlank() ? "ui.empty" : "ui.no_results"), leftPos + 110, topPos + 62, 174, 0xFF555555);
        extractTooltip(g, mouseX, mouseY);
    }
}
