package fi.dy.masa.nicehud.screen;

import fi.dy.masa.nicehud.hud.HudWidget;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes snap-to-grid lines while dragging a widget.
 * Snaps to: screen edges, screen centre, and edges/centres of other widgets.
 */
public final class SnapLines {

    public  static final int SNAP_DIST = 8;   // pixels within which snapping triggers
    private static final int LINE_COL  = 0xAAFFFF55; // yellow guide line

    private SnapLines() {}

    public record SnapResult(int x, int y, List<int[]> lines) {}

    /**
     * Given the dragged widget's raw position, find the nearest snap point
     * and return snapped x,y plus guide lines to draw.
     *
     * @param w       the widget being dragged
     * @param rawX    raw mouse-computed x
     * @param rawY    raw mouse-computed y
     * @param others  all OTHER widgets (not the dragged one)
     * @param sw      screen width
     * @param sh      screen height
     */
    public static SnapResult snap(HudWidget w, int rawX, int rawY,
                                  List<HudWidget> others, int sw, int sh) {
        int ww = w.getWidth();
        int wh = w.scaledH();

        int sx = rawX, sy = rawY;
        List<int[]> lines = new ArrayList<>();

        // -- Horizontal snap candidates (affects x) --
        int[] xCands = {
            0,          // left screen edge
            sw / 2 - ww / 2,  // screen centre
            sw - ww,    // right screen edge
        };
        // add other widgets' left/right/centre
        List<Integer> xList = new ArrayList<>();
        for (int c : xCands) xList.add(c);
        for (HudWidget o : others) {
            if (!o.element.visible) continue;
            xList.add(o.element.x);
            xList.add(o.element.x + o.getWidth() - ww);
            xList.add(o.element.x + (o.getWidth() - ww) / 2);
        }

        int bestDX = SNAP_DIST + 1;
        for (int c : xList) {
            int d = Math.abs(rawX - c);
            if (d < bestDX) { bestDX = d; sx = c; }
        }
        if (bestDX <= SNAP_DIST) {
            lines.add(new int[]{sx, 0, sx, sh, 0}); // vertical guide line
        }

        // -- Vertical snap candidates (affects y) --
        List<Integer> yList = new ArrayList<>();
        yList.add(0);
        yList.add(sh / 2 - wh / 2);
        yList.add(sh - wh);
        for (HudWidget o : others) {
            if (!o.element.visible) continue;
            yList.add(o.element.y);
            yList.add(o.element.y + o.scaledH() - wh);
            yList.add(o.element.y + o.scaledH()); // stack directly below
        }

        int bestDY = SNAP_DIST + 1;
        for (int c : yList) {
            int d = Math.abs(rawY - c);
            if (d < bestDY) { bestDY = d; sy = c; }
        }
        if (bestDY <= SNAP_DIST) {
            lines.add(new int[]{0, sy, sw, sy, 1}); // horizontal guide line
        }

        return new SnapResult(sx, sy, lines);
    }

    /** Draw guide lines. Each int[] is {x1,y1,x2,y2,type}. */
    public static void draw(GuiGraphics gfx, List<int[]> lines) {
        for (int[] l : lines) {
            if (l[4] == 0) { // vertical
                gfx.fill(l[0], l[1], l[0] + 1, l[3], LINE_COL);
            } else {          // horizontal
                gfx.fill(l[0], l[1], l[2], l[1] + 1, LINE_COL);
            }
        }
    }
}
