package fi.dy.masa.nicehud.screen;

import fi.dy.masa.nicehud.config.NiceHudConfig;
import fi.dy.masa.nicehud.hud.HudWidget;
import fi.dy.masa.nicehud.hud.WidgetFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

public class EditorScreen extends Screen {

    private static final int PANEL_W = 200;
    private static final int ROW_H   = 14;
    private static final int HDR_H   = 20;

    private final NiceHudConfig          config;
    private final List<HudWidget>        widgets;
    private boolean                        panelVisible = true;
    private final Consumer<NiceHudConfig> onSave;

    // ── single drag ───────────────────────────────────────────────────────────
    private HudWidget       dragged   = null;
    private List<HudWidget> dragGroup = new ArrayList<>();
    private final Map<HudWidget, int[]> groupOffsets = new HashMap<>();

    // ── box-select ────────────────────────────────────────────────────────────
    private boolean boxSelecting  = false;
    private int     boxStartX, boxStartY, boxEndX, boxEndY;
    private final List<HudWidget> selected = new ArrayList<>(); // currently box-selected
    private boolean draggingSelected = false; // true when dragging a selection
    private int     selDragRefX, selDragRefY; // ref point for group move
    private final Map<HudWidget, int[]> selOffsets = new HashMap<>();

    // ── snap / panel ─────────────────────────────────────────────────────────
    private List<int[]> snapGuides = new ArrayList<>();
    private int         scrollOff  = 0;

    // ── context menu ─────────────────────────────────────────────────────────
    private HudWidget ctxWidget     = null;  // single ctx
    private boolean   ctxSelection  = false; // ctx on selection
    private int       ctxX, ctxY;

    public EditorScreen(NiceHudConfig config, Consumer<NiceHudConfig> onSave) {
        super(Component.literal("NiceHUD 编辑器"));
        this.config  = config;
        this.widgets = WidgetFactory.create(config);
        this.onSave  = onSave;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ═════════════════════════════════════════════════════════════════════════
    // RENDER
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        int panelX = panelVisible ? width - PANEL_W : width;

        // dim canvas
        gfx.fill(0, 0, panelX, height, 0x60000000);

        // snap guides
        SnapLines.draw(gfx, snapGuides);

        // widgets
        for (HudWidget w : widgets)
            if (w.element.visible) w.render(gfx, true);

        // selection highlight
        for (HudWidget w : selected) {
            if (!w.element.visible) continue;
            int bw = w.getWidth(), bh = w.scaledH();
            // glowing cyan border
            gfx.fill(w.element.x - 1, w.element.y - 1,
                     w.element.x + bw + 1, w.element.y,        0xFF00FFFF);
            gfx.fill(w.element.x - 1, w.element.y + bh,
                     w.element.x + bw + 1, w.element.y + bh + 1, 0xFF00FFFF);
            gfx.fill(w.element.x - 1, w.element.y,
                     w.element.x,          w.element.y + bh,    0xFF00FFFF);
            gfx.fill(w.element.x + bw, w.element.y,
                     w.element.x + bw + 1, w.element.y + bh,    0xFF00FFFF);
        }

        // rubber-band selection rect
        if (boxSelecting) {
            int x1 = Math.min(boxStartX, boxEndX), y1 = Math.min(boxStartY, boxEndY);
            int x2 = Math.max(boxStartX, boxEndX), y2 = Math.max(boxStartY, boxEndY);
            gfx.fill(x1, y1, x2, y2, 0x3000FFFF);          // fill
            gfx.fill(x1, y1, x2, y1 + 1, 0xAA00FFFF);      // top
            gfx.fill(x1, y2 - 1, x2, y2, 0xAA00FFFF);      // bottom
            gfx.fill(x1, y1, x1 + 1, y2, 0xAA00FFFF);      // left
            gfx.fill(x2 - 1, y1, x2, y2, 0xAA00FFFF);      // right
        }

        // selection toolbar (shown when something is selected)
        if (!selected.isEmpty() && !boxSelecting)
            renderSelectionToolbar(gfx, mx, my);

        // right panel (only when visible)
        if (panelVisible) renderPanel(gfx, mx, my, panelX);

        // header
        gfx.fill(0, 0, width, HDR_H, 0xB8000000);
        gfx.drawString(minecraft.font,
            "§eNiceHUD §7— 拖拽移动 · 框选多选 · 右键菜单 · 滚轮缩放 · §aH §7保存",
            4, (HDR_H - minecraft.font.lineHeight) / 2, 0xFFFFFFFF, false);
        // Panel toggle button — fixed 50px wide at top-right
        String panelBtnTxt = panelVisible ? "§c隐藏面板" : "§a显示面板";
        gfx.fill(width - 54, 2, width - 2, HDR_H - 2, panelVisible ? 0xBB442222 : 0xBB224422);
        int btnTxtX = width - 54 + (52 - minecraft.font.width("隐藏面板")) / 2;
        gfx.drawString(minecraft.font, panelBtnTxt, btnTxtX, (HDR_H - minecraft.font.lineHeight) / 2, 0xFFFFFFFF, false);

        // context menu (drawn on top)
        if (ctxWidget != null || ctxSelection) renderContextMenu(gfx, mx, my);

        super.render(gfx, mx, my, delta);
    }

    // ── Selection toolbar ────────────────────────────────────────────────────

    private static final String[] SEL_BTNS = {"隐藏", "放大", "缩小", "重置", "设为A组", "设为B组", "设为C组", "清除组", "取消选择"};
    private static final int BTN_H = 16, BTN_PAD = 6;

    private int[] selToolbarBounds() {
        int totalW = 0;
        for (String s : SEL_BTNS) totalW += minecraft.font.width(s) + BTN_PAD * 2 + 2;
        int tx = ((panelVisible ? width - PANEL_W : width) - totalW) / 2;
        int ty = height - BTN_H - 6;
        return new int[]{tx, ty, totalW, BTN_H};
    }

    private void renderSelectionToolbar(GuiGraphics gfx, int mx, int my) {
        int[] tb = selToolbarBounds();
        int tx = tb[0], ty = tb[1];
        // backdrop
        gfx.fill(tx - 4, ty - 4, tx + tb[2] + 4, ty + BTN_H + 4, 0xCC111111);
        int cx = tx;
        for (String s : SEL_BTNS) {
            int bw = minecraft.font.width(s) + BTN_PAD * 2;
            boolean hov = mx >= cx && mx < cx + bw && my >= ty && my < ty + BTN_H;
            gfx.fill(cx, ty, cx + bw, ty + BTN_H, hov ? 0xFF3A3A3A : 0xFF222222);
            gfx.fill(cx, ty, cx + bw, ty + 1, 0xFF666666);
            gfx.fill(cx, ty + BTN_H - 1, cx + bw, ty + BTN_H, 0xFF666666);
            gfx.drawString(minecraft.font, s, cx + BTN_PAD,
                ty + (BTN_H - minecraft.font.lineHeight) / 2, 0xFFEEEEEE, false);
            cx += bw + 2;
        }
        // count badge
        gfx.drawString(minecraft.font, "§7已选 §e" + selected.size() + " §7项",
            tx - 4, ty - 14, 0xFFFFFFFF, false);
    }

    private boolean handleSelectionToolbarClick(int mx, int my) {
        if (selected.isEmpty()) return false;
        int[] tb = selToolbarBounds();
        int tx = tb[0], ty = tb[1];
        if (my < ty || my > ty + BTN_H) return false;
        int cx = tx;
        for (int i = 0; i < SEL_BTNS.length; i++) {
            int bw = minecraft.font.width(SEL_BTNS[i]) + BTN_PAD * 2;
            if (mx >= cx && mx < cx + bw) {
                applySelectionAction(i);
                return true;
            }
            cx += bw + 2;
        }
        return false;
    }

    private void applySelectionAction(int idx) {
        switch (idx) {
            case 0 -> selected.forEach(w -> w.element.visible = false);
            case 1 -> selected.forEach(w -> w.element.scale = Math.min(3f, w.element.scale + 0.1f));
            case 2 -> selected.forEach(w -> w.element.scale = Math.max(0.5f, w.element.scale - 0.1f));
            case 3 -> selected.forEach(w -> w.element.scale = 1f);
            case 4 -> selected.forEach(w -> w.element.group = "A");
            case 5 -> selected.forEach(w -> w.element.group = "B");
            case 6 -> selected.forEach(w -> w.element.group = "C");
            case 7 -> selected.forEach(w -> w.element.group = "");
            case 8 -> selected.clear();
        }
    }

    // ── Right panel ──────────────────────────────────────────────────────────

    private void renderPanel(GuiGraphics gfx, int mx, int my, int panelX) {
        gfx.fill(panelX, 0, width, height, 0xD4111111);
        gfx.vLine(panelX, 0, height, 0xFF444444);
        gfx.drawString(minecraft.font, "§e全部信息项", panelX + 5, 4, 0xFFFFFFFF, false);
        gfx.drawString(minecraft.font, "§7单击开关", panelX + 5, 13, 0xFF888888, false);

        int listTop = HDR_H + 4;
        gfx.enableScissor(panelX, listTop, width, height);
        int ry = listTop - scrollOff;
        for (HudWidget w : widgets) {
            if (ry + ROW_H > listTop && ry < height) {
                boolean vis = w.element.visible;
                boolean sel = selected.contains(w);
                boolean hov = mx >= panelX && mx < width && my >= ry && my < ry + ROW_H;
                if (sel)  gfx.fill(panelX, ry, width, ry + ROW_H, 0x5000FFFF);
                else if (hov) gfx.fill(panelX, ry, width, ry + ROW_H, 0x40FFFFFF);
                else if ((ry / ROW_H) % 2 == 0) gfx.fill(panelX, ry, width, ry + ROW_H, 0x10FFFFFF);

                gfx.drawString(minecraft.font, vis ? "§a✔" : "§7✗", panelX + 4, ry + 3, 0xFFFFFFFF, false);
                String grpTag = !w.element.group.isEmpty() ? " §8[" + w.element.group + "]" : "";
                gfx.drawString(minecraft.font, (vis ? "§f" : "§7") + w.getLabel() + grpTag,
                    panelX + 16, ry + 3, 0xFFFFFFFF, false);
                if (Math.abs(w.element.scale - 1f) > 0.05f) {
                    String sc = String.format("§8×%.1f", w.element.scale);
                    gfx.drawString(minecraft.font, sc, width - minecraft.font.width(sc) - 4, ry + 3, 0xFFFFFFFF, false);
                }
            }
            ry += ROW_H;
        }
        gfx.disableScissor();
    }

    // ── Context menu ─────────────────────────────────────────────────────────

    private static final String[] CTX_SINGLE = {"隐藏", "放大(+10%)", "缩小(-10%)", "重置大小", "切换横/纵向", "设为A组", "设为B组", "设为C组", "清除分组"};
    private static final String[] CTX_SEL    = {"隐藏所选", "放大所选", "缩小所选", "重置大小", "设为A组", "设为B组", "设为C组", "清除分组", "取消选择"};
    private static final int CTX_W = 130, CTX_RH = 13;

    private void renderContextMenu(GuiGraphics gfx, int mx, int my) {
        String[] opts = ctxSelection ? CTX_SEL : CTX_SINGLE;
        int h = opts.length * CTX_RH + 4;
        gfx.fill(ctxX, ctxY, ctxX + CTX_W, ctxY + h, 0xF0222222);
        gfx.fill(ctxX, ctxY, ctxX + CTX_W, ctxY + 1, 0xFF888888);
        gfx.fill(ctxX, ctxY + h - 1, ctxX + CTX_W, ctxY + h, 0xFF888888);
        gfx.fill(ctxX, ctxY, ctxX + 1, ctxY + h, 0xFF888888);
        gfx.fill(ctxX + CTX_W - 1, ctxY, ctxX + CTX_W, ctxY + h, 0xFF888888);
        if (ctxSelection) {
            gfx.drawString(minecraft.font, "§7所选 " + selected.size() + " 项", ctxX + 5, ctxY + 2, 0xFF888888, false);
        }
        for (int i = 0; i < opts.length; i++) {
            int iy = ctxY + 2 + i * CTX_RH;
            boolean hov = mx >= ctxX && mx < ctxX + CTX_W && my >= iy && my < iy + CTX_RH;
            if (hov) gfx.fill(ctxX + 1, iy, ctxX + CTX_W - 1, iy + CTX_RH, 0x40FFFFFF);
            gfx.drawString(minecraft.font, opts[i], ctxX + 5, iy + 2, 0xFFEEEEEE, false);
        }
    }

    private void handleContextClick(int mx, int my) {
        String[] opts = ctxSelection ? CTX_SEL : CTX_SINGLE;
        int h = opts.length * CTX_RH + 4;
        if (mx < ctxX || mx > ctxX + CTX_W || my < ctxY || my > ctxY + h) {
            ctxWidget = null; ctxSelection = false; return;
        }
        int idx = (my - ctxY - 2) / CTX_RH;
        if (idx < 0 || idx >= opts.length) { ctxWidget = null; ctxSelection = false; return; }

        if (ctxSelection) {
            applySelectionAction(idx);
        } else if (ctxWidget != null) {
            switch (idx) {
                case 0 -> ctxWidget.element.visible = false;
                case 1 -> ctxWidget.element.scale = Math.min(3f, ctxWidget.element.scale + 0.1f);
                case 2 -> ctxWidget.element.scale = Math.max(0.5f, ctxWidget.element.scale - 0.1f);
                case 3 -> ctxWidget.element.scale = 1f;
                case 4 -> {
                    if (ctxWidget instanceof fi.dy.masa.nicehud.hud.InventoryWidget iw) {
                        iw.setLayout(iw.getLayout() == fi.dy.masa.nicehud.hud.InventoryWidget.Layout.HORIZONTAL
                            ? fi.dy.masa.nicehud.hud.InventoryWidget.Layout.VERTICAL
                            : fi.dy.masa.nicehud.hud.InventoryWidget.Layout.HORIZONTAL);
                    } else if (ctxWidget instanceof fi.dy.masa.nicehud.hud.ArmorWidget aw) {
                        aw.setVertical(!aw.isVertical());
                    }
                }
                case 5 -> ctxWidget.element.group = "A";
                case 6 -> ctxWidget.element.group = "B";
                case 7 -> ctxWidget.element.group = "C";
                case 8 -> ctxWidget.element.group = "";
            }
        }
        ctxWidget = null; ctxSelection = false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MOUSE EVENTS
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int panelX = panelVisible ? width - PANEL_W : width;

        // ── Context menu open ─────────────────────────────────────────────────
        if (ctxWidget != null || ctxSelection) {
            handleContextClick((int)mx, (int)my);
            return true;
        }

        // ── Panel toggle button (top-right, always reachable) ────────────────
        if (btn == 0 && mx >= width - 54 && mx < width - 2 && my >= 2 && my < HDR_H - 2) {
            panelVisible = !panelVisible;
            return true;
        }

        // ── Panel click → toggle visibility ──────────────────────────────────
        if (mx >= panelX) {
            if ((btn == 0 || btn == 1) && panelVisible) {
                int listTop = HDR_H + 4;
                int idx = ((int)my - listTop + scrollOff) / ROW_H;
                if (idx >= 0 && idx < widgets.size()) {
                    var w = widgets.get(idx);
                    w.element.visible ^= true;
                    // If just enabled and position is off-screen, move to visible area
                    if (w.element.visible) {
                        int sw = panelVisible ? width - PANEL_W : width, sh = height;
                        if (w.element.x >= sw || w.element.y >= sh
                                || w.element.x < 0 || w.element.y < 0) {
                            w.element.x = Math.max(5, (sw - w.getWidth()) / 2);
                            w.element.y = Math.max(5, (sh - w.scaledH()) / 2);
                        }
                    }
                    return true;
                }
            }
            return true;
        }

        // ── Selection toolbar click ───────────────────────────────────────────
        if (btn == 0 && !selected.isEmpty()) {
            if (handleSelectionToolbarClick((int)mx, (int)my)) return true;
        }

        // ── Right-click on canvas ─────────────────────────────────────────────
        if (btn == 1) {
            // If something is selected and we right-click on it → selection ctx menu
            for (int i = widgets.size() - 1; i >= 0; i--) {
                HudWidget w = widgets.get(i);
                if (w.element.visible && w.isHovered(mx, my)) {
                    if (selected.contains(w) && selected.size() > 1) {
                        ctxSelection = true;
                    } else {
                        ctxWidget = w;
                        selected.clear();
                    }
                    ctxX = Math.min((int)mx, panelX - CTX_W - 2);
                    ctxY = Math.min((int)my, height - (CTX_SEL.length * CTX_RH + 4));
                    return true;
                }
            }
            return false;
        }

        // ── Left-click ────────────────────────────────────────────────────────
        if (btn == 0) {
            // Click on a selected widget → start group drag
            if (!selected.isEmpty()) {
                for (HudWidget w : selected) {
                    if (w.element.visible && w.isHovered(mx, my)) {
                        draggingSelected = true;
                        selDragRefX = (int)mx;
                        selDragRefY = (int)my;
                        selOffsets.clear();
                        for (HudWidget s : selected)
                            selOffsets.put(s, new int[]{s.element.x, s.element.y});
                        return true;
                    }
                }
                // Clicked outside selection → clear it
                selected.clear();
            }

            // Click on a widget → single drag
            for (int i = widgets.size() - 1; i >= 0; i--) {
                HudWidget w = widgets.get(i);
                if (w.element.visible && w.mousePressed(mx, my)) {
                    dragged = w;
                    dragGroup.clear(); groupOffsets.clear();
                    if (!w.element.group.isEmpty()) {
                        for (HudWidget o : widgets) {
                            if (o != w && o.element.visible && o.element.group.equals(w.element.group)) {
                                dragGroup.add(o);
                                groupOffsets.put(o, new int[]{o.element.x - w.element.x, o.element.y - w.element.y});
                            }
                        }
                    }
                    return true;
                }
            }

            // Clicked on empty canvas → start box-select
            boxSelecting = true;
            boxStartX = boxEndX = (int)mx;
            boxStartY = boxEndY = (int)my;
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        int panelX = panelVisible ? width - PANEL_W : width;

        // ── Box select ────────────────────────────────────────────────────────
        if (boxSelecting) {
            boxEndX = Math.min((int)mx, panelX - 1);
            boxEndY = (int)my;
            return true;
        }

        // ── Drag selection group ──────────────────────────────────────────────
        if (draggingSelected) {
            int ddx = (int)mx - selDragRefX;
            int ddy = (int)my - selDragRefY;
            for (HudWidget w : selected) {
                int[] orig = selOffsets.get(w);
                if (orig != null) {
                    w.element.x = Math.max(0, orig[0] + ddx);
                    w.element.y = Math.max(0, orig[1] + ddy);
                }
            }
            return true;
        }

        // ── Drag single widget ────────────────────────────────────────────────
        if (dragged != null) {
            int rawX = Math.max(0, (int)mx - dragged.dragOffX);
            int rawY = Math.max(0, (int)my - dragged.dragOffY);
            List<HudWidget> others = new ArrayList<>(widgets);
            others.remove(dragged); dragGroup.forEach(others::remove);
            SnapLines.SnapResult snap = SnapLines.snap(dragged, rawX, rawY, others, panelX, height);
            snapGuides = snap.lines();
            dragged.element.x = snap.x();
            dragged.element.y = snap.y();
            for (HudWidget o : dragGroup) {
                int[] off = groupOffsets.get(o);
                if (off != null) { o.element.x = Math.max(0, snap.x() + off[0]); o.element.y = Math.max(0, snap.y() + off[1]); }
            }
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        // ── Finish box select ─────────────────────────────────────────────────
        if (boxSelecting) {
            boxSelecting = false;
            int x1 = Math.min(boxStartX, boxEndX), y1 = Math.min(boxStartY, boxEndY);
            int x2 = Math.max(boxStartX, boxEndX), y2 = Math.max(boxStartY, boxEndY);
            if (x2 - x1 > 4 || y2 - y1 > 4) {
                selected.clear();
                for (HudWidget w : widgets) {
                    if (!w.element.visible) continue;
                    int wx = w.element.x, wy = w.element.y;
                    int ww = w.getWidth(), wh = w.scaledH();
                    // intersects box?
                    if (wx < x2 && wx + ww > x1 && wy < y2 && wy + wh > y1)
                        selected.add(w);
                }
            }
            return true;
        }

        // ── Finish selection drag ─────────────────────────────────────────────
        if (draggingSelected) { draggingSelected = false; selOffsets.clear(); return true; }

        // ── Finish single drag ────────────────────────────────────────────────
        if (dragged != null) {
            dragged.mouseReleased();
            dragGroup.forEach(HudWidget::mouseReleased);
            dragged = null; dragGroup.clear(); snapGuides.clear();
            return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int panelX = panelVisible ? width - PANEL_W : width;
        if (mx >= panelX) {
            int maxScroll = Math.max(0, widgets.size() * ROW_H - (height - HDR_H - 4));
            scrollOff = (int)Math.max(0, Math.min(maxScroll, scrollOff - vScroll * ROW_H * 3));
            return true;
        }
        // Scale hovered or selected widgets
        float delta = (float)(vScroll * 0.05f);
        if (!selected.isEmpty()) {
            for (HudWidget w : selected) {
                if (w.isHovered(mx, my)) {
                    selected.forEach(s -> s.element.scale = Math.max(0.5f, Math.min(3f, s.element.scale + delta)));
                    return true;
                }
            }
        }
        for (int i = widgets.size() - 1; i >= 0; i--) {
            HudWidget w = widgets.get(i);
            if (w.element.visible && w.isHovered(mx, my)) {
                if (!w.element.group.isEmpty())
                    widgets.stream().filter(o -> o.element.group.equals(w.element.group))
                        .forEach(o -> o.element.scale = Math.max(0.5f, Math.min(3f, o.element.scale + delta)));
                else
                    w.element.scale = Math.max(0.5f, Math.min(3f, w.element.scale + delta));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public void onClose() { onSave.accept(config); super.onClose(); }
}
