package fi.dy.masa.nicehud.hud;

import fi.dy.masa.nicehud.config.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * StarHUD-style inventory display.
 * - No outer background box
 * - Slots: semi-transparent dark square, no rounded corners
 * - Items rendered at 16×16, centred in slot
 * - Durability bar at slot bottom
 * - Item count bottom-right
 * - Horizontal (9×N) or Vertical (N×9) layout
 */
public class InventoryWidget extends HudWidget {

    public enum Layout { HORIZONTAL, VERTICAL }

    // StarHUD uses 18px slots with 1px gaps — matches vanilla hotbar proportions
    private static final int ICON   = 16;
    private static final int PAD    = 1;   // inner padding each side
    private static final int SLOT   = ICON + PAD * 2;   // 18
    private static final int SEP    = 1;   // gap between slots
    private static final int BAR_H  = 2;
    private static final int SLOT_COLOR = 0x88000000; // semi-transparent dark, same as BG_COLOR

    private final boolean fullInventory;
    public InventoryWidget(HudElement el, boolean fullInventory) {
        super(el, fullInventory ? "背包" : "快捷栏");
        this.fullInventory = fullInventory;
    }

    public void setLayout(Layout l) { element.vertical = (l == Layout.VERTICAL); }
    public Layout getLayout()       { return element.vertical ? Layout.VERTICAL : Layout.HORIZONTAL; }

    @Override public String getValue() { return null; }
    @Override public String getLabel() { return fullInventory ? "背包" : "快捷栏"; }

    private int slots() { return fullInventory ? 27 : 9; }

    private int cols() {
        if (getLayout() == Layout.VERTICAL) return fullInventory ? 3 : 1;
        return 9;
    }
    private int rows() {
        return (slots() + cols() - 1) / cols();
    }

    // Total pixel dimensions (unscaled), no outer padding
    private int rawW() { return cols() * SLOT + (cols() - 1) * SEP; }
    private int rawH() { return rows() * (SLOT + BAR_H) + (rows() - 1) * SEP; }

    @Override public int getWidth() { return Math.round(rawW() * getScale()); }
    @Override public int scaledH()  { return Math.round(rawH() * getScale()); }

    @Override
    public void render(GuiGraphics gfx, boolean editMode) {
        if (!element.visible && !editMode) return;

        Minecraft mc = Minecraft.getInstance();
        float sc = getScale();

        gfx.pose().pushMatrix();
        gfx.pose().translate((float) element.x, (float) element.y);
        gfx.pose().scale(sc, sc);

        int cols = cols();

        int rowCount = rows();
        for (int i = 0; i < slots(); i++) {
            // VERTICAL: fill down first (col-major), HORIZONTAL: fill across first (row-major)
            int col, row;
            if (getLayout() == Layout.VERTICAL) {
                col = i / rowCount;
                row = i % rowCount;
            } else {
                col = i % cols;
                row = i / cols;
            }
            int invSlot = fullInventory ? i + 9 : i;

            int sx = col * (SLOT + SEP);
            int sy = row * (SLOT + BAR_H + SEP);

            // Slot background — plain dark square, no rounding (StarHUD style)
            gfx.fill(sx, sy, sx + SLOT, sy + SLOT, SLOT_COLOR);

            if (mc.player != null) {
                ItemStack stack = mc.player.getInventory().getItem(invSlot);
                if (!stack.isEmpty()) {
                    // Item icon — rendered at (sx+PAD, sy+PAD) = 16×16
                    gfx.renderItem(stack, sx + PAD, sy + PAD);

                    // Item count — bottom-right, scaled down
                    if (stack.getCount() > 1) {
                        String cnt = String.valueOf(stack.getCount());
                        gfx.pose().pushMatrix();
                        float cs = 0.6f;
                        gfx.pose().translate(sx + SLOT - 1f, sy + SLOT - mc.font.lineHeight * cs + 1f);
                        gfx.pose().scale(cs, cs);
                        gfx.drawString(mc.font, cnt, -mc.font.width(cnt), 0, 0xFFFFFFFF, true);
                        gfx.pose().popMatrix();
                    }

                    // Durability bar — immediately below slot
                    if (stack.isDamaged()) {
                        float pct = (float)(stack.getMaxDamage() - stack.getDamageValue())
                                    / stack.getMaxDamage();
                        int bw = Math.max(1, Math.round((SLOT - 2) * pct));
                        // dark track
                        gfx.fill(sx + 1, sy + SLOT, sx + SLOT - 1, sy + SLOT + BAR_H, 0xFF111111);
                        // colored fill
                        gfx.fill(sx + 1, sy + SLOT, sx + 1 + bw, sy + SLOT + BAR_H,
                            pct > 0.6f ? 0xFF55FF55 : pct > 0.3f ? 0xFFFFFF55 : 0xFFFF5555);
                    }
                }
            }
        }

        // Faint edit-mode overlay — helps user see bounds without obscuring items
        if (editMode) {
            gfx.fill(0, 0, rawW(), rawH(), 0x28FFFFFF);
        }

        gfx.pose().popMatrix();
    }

    @Override
    public boolean contains(double mx, double my) {
        double w = getWidth(), h = scaledH();
        return mx >= element.x && mx < element.x + w && my >= element.y && my < element.y + h;
    }
}
