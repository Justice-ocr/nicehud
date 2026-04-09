package fi.dy.masa.nicehud.hud;

import fi.dy.masa.nicehud.config.NiceHudConfig;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Drives per-frame rendering of all visible HUD widgets. */
public final class HudRenderer {

    private NiceHudConfig   config;
    private List<HudWidget> widgets;

    public HudRenderer(NiceHudConfig config) { reload(config); }

    public void reload(NiceHudConfig config) {
        this.config  = config;
        this.widgets = WidgetFactory.create(config);
    }

    public List<HudWidget> getWidgets() { return widgets; }
    public NiceHudConfig   getConfig()  { return config; }

    public void render(GuiGraphics gfx) {
        for (HudWidget w : widgets) w.render(gfx, false);
    }
}
