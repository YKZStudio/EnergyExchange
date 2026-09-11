package studio.ykz.energyexchange;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreen extends Screen {
    private final Screen parent;
    private ExchangeConfig value;
    private EditBox xpCost;
    private String error = "";
    public ConfigScreen(Screen parent) {
        super(Messages.text("config.title")); this.parent = parent;
        try { value = ExchangeConfig.load(); } catch (IllegalStateException e) { value = ExchangeConfig.DEFAULT; error = "config.invalid"; }
    }
    @Override protected void init() {
        int x = width / 2 - 110;
        addRenderableWidget(Button.builder(label("config.unknown", value.showUnlearned()), b -> {
            value = new ExchangeConfig(!value.showUnlearned(), value.compactNumbers(), value.xpEnabled(), value.xpCost(), value.pinyinSearch()); b.setMessage(label("config.unknown", value.showUnlearned()));
        }).bounds(x, 38, 220, 20).build());
        addRenderableWidget(Button.builder(label("config.compact", value.compactNumbers()), b -> {
            value = new ExchangeConfig(value.showUnlearned(), !value.compactNumbers(), value.xpEnabled(), value.xpCost(), value.pinyinSearch()); b.setMessage(label("config.compact", value.compactNumbers()));
        }).bounds(x, 62, 220, 20).build());
        addRenderableWidget(Button.builder(label("config.pinyin", value.pinyinSearch()), b -> {
            value = new ExchangeConfig(value.showUnlearned(), value.compactNumbers(), value.xpEnabled(), value.xpCost(), !value.pinyinSearch());
            b.setMessage(label("config.pinyin", value.pinyinSearch()));
        }).bounds(x, 86, 220, 20).tooltip(Tooltip.create(Messages.text("config.pinyin_hint"))).build());
        addRenderableWidget(Button.builder(label("config.xp", value.xpEnabled()), b -> {
            value = new ExchangeConfig(value.showUnlearned(), value.compactNumbers(), !value.xpEnabled(), value.xpCost(), value.pinyinSearch()); b.setMessage(label("config.xp", value.xpEnabled()));
        }).bounds(x, 110, 220, 20).build());
        xpCost = addRenderableWidget(new EditBox(font, x, 145, 220, 20, Messages.text("config.xp_cost")));
        xpCost.setMaxLength(128); xpCost.setValue(value.xpCost());
        addRenderableWidget(Button.builder(Messages.text("config.save"), b -> {
            try { new ExchangeConfig(value.showUnlearned(), value.compactNumbers(), value.xpEnabled(), xpCost.getValue(), value.pinyinSearch()).save(); onClose(); }
            catch (RuntimeException e) { error = "config.invalid"; }
        }).bounds(x, height - 28, 108, 20).build());
        addRenderableWidget(Button.builder(Messages.text("config.cancel"), b -> onClose()).bounds(x + 112, height - 28, 108, 20).build());
    }
    private Component label(String key, boolean enabled) { return Messages.text(key, Messages.text(enabled ? "config.on" : "config.off")); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        g.text(font, Messages.text("config.xp_cost"), width / 2 - 110, 134, 0xFFFFFFFF);
        g.textWithWordWrap(font, Messages.text("config.server_note"), width / 2 - 140, 174, 280, 0xFFAAAAAA);
        if (!error.isEmpty()) g.centeredText(font, Messages.text(error), width / 2, height - 42, 0xFFFF5555);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
