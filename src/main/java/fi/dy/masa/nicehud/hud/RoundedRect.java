package fi.dy.masa.nicehud.hud;

import net.minecraft.client.gui.GuiGraphics;

/** Draws filled rounded rectangles using layered fill calls. */
public final class RoundedRect {

    private RoundedRect() {}

    /**
     * Fills a rounded rectangle.
     * @param r corner radius (2–4 looks good at Minecraft scale)
     */
    public static void fill(GuiGraphics gfx, int x, int y, int w, int h, int color, int r) {
        if (r <= 0 || w < r * 2 || h < r * 2) {
            gfx.fill(x, y, x + w, y + h, color);
            return;
        }
        gfx.fill(x,     y + r,   x + w,     y + h - r, color);  // centre
        gfx.fill(x + r, y,       x + w - r, y + r,     color);  // top strip
        gfx.fill(x + r, y + h - r, x + w - r, y + h,  color);  // bottom strip
        if (r >= 2) {
            gfx.fill(x + 1,     y + 1,     x + r,     y + r,     color); // TL
            gfx.fill(x + w - r, y + 1,     x + w - 1, y + r,     color); // TR
            gfx.fill(x + 1,     y + h - r, x + r,     y + h - 1, color); // BL
            gfx.fill(x + w - r, y + h - r, x + w - 1, y + h - 1, color); // BR
        }
    }
}
