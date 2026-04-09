package fi.dy.masa.nicehud.hud;

import fi.dy.masa.nicehud.config.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for all NiceHUD elements.
 * Handles: background, label+value text, drag state.
 */
public abstract class HudWidget {

    // ── Shared style constants ───────────────────────────────────────────────
    public static final int PAD      = 4;
    public static final int CORNER   = 3;
    public static final int BG_COLOR = 0x80000000;

    private static final int ICON_SZ  = 9;
    private static final int GAP      = 4;
    private static final int H_BASE   = ICON_SZ + PAD * 2;

    // Legacy alias
    public static final int H = H_BASE;

    // ── Per-widget state ─────────────────────────────────────────────────────
    public final HudElement element;
    protected final String  label;

    /** When false, render raw getValue() without "label: " prefix (MiniHUD widgets). */
    protected boolean showLabelInHud = true;

    // Drag state (managed by EditorScreen)
    public boolean dragging = false;
    public int     dragOffX, dragOffY;

    protected HudWidget(HudElement element, String label) {
        this.element = element;
        this.label   = label;
    }

    // ── Subclass API ─────────────────────────────────────────────────────────

    /** Current value string, or null if unavailable. */
    public abstract String getValue();

    public String getLabel()      { return label; }
    public int    getTextColor()  { return 0xDDFFFFFF; }

    // ── Layout ───────────────────────────────────────────────────────────────

    public float getScale() { return Math.max(0.5f, Math.min(3.0f, element.scale)); }

    public int scaledH() { return Math.round(H_BASE * getScale()); }

    public int getWidth() {
        String v    = getValue(); if (v == null) v = "—";
        String disp = showLabelInHud ? label + ": " + v : v;
        return Math.round((PAD + Minecraft.getInstance().font.width(disp) + PAD) * getScale());
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, boolean editMode) {
        if (!element.visible && !editMode) return;
        String v = getValue();
        if (v == null) { if (!editMode) return; v = "—"; }

        var   font   = Minecraft.getInstance().font;
        float sc     = getScale();
        String disp  = showLabelInHud ? label + ": " + v : v;
        int   innerW = PAD + font.width(disp) + PAD;

        gfx.pose().pushMatrix();
        gfx.pose().translate((float) element.x, (float) element.y);
        gfx.pose().scale(sc, sc);

        RoundedRect.fill(gfx, 0, 0, innerW, H_BASE, BG_COLOR, CORNER);
        if (editMode) RoundedRect.fill(gfx, 0, 0, innerW, H_BASE, 0x28FFFFFF, CORNER);

        int textY = (H_BASE - font.lineHeight) / 2;
        gfx.drawString(font, disp, PAD, textY, getTextColor(), false);

        gfx.pose().popMatrix();
    }

    // ── Hit testing ──────────────────────────────────────────────────────────

    public boolean contains(double mx, double my) {
        return mx >= element.x && mx < element.x + getWidth()
            && my >= element.y && my < element.y + scaledH();
    }

    public boolean isHovered(double mx, double my) { return contains(mx, my); }

    // ── Drag helpers ─────────────────────────────────────────────────────────

    public boolean mousePressed(double mx, double my) {
        if (!contains(mx, my)) return false;
        dragging = true;
        dragOffX = (int)(mx - element.x);
        dragOffY = (int)(my - element.y);
        return true;
    }

    public void mouseDragged(double mx, double my) {
        if (!dragging) return;
        element.x = Math.max(0, (int)mx - dragOffX);
        element.y = Math.max(0, (int)my - dragOffY);
    }

    public void mouseReleased() { dragging = false; }
}
