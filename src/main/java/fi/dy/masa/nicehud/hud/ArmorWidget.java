package fi.dy.masa.nicehud.hud;

import fi.dy.masa.nicehud.config.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Equipment durability — compact continuous bar style.
 * Each row: [icon 16x16] [████████ single progress bar]
 */
public class ArmorWidget extends HudWidget {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST,
        EquipmentSlot.LEGS, EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private static final int ICON    = 16;
    private static final int ICON_GAP= 4;
    private static final int BAR_W   = 100;  // continuous bar width
    private static final int BAR_H   = 6;    // slim bar height
    private static final int ROW_H   = ICON;
    private static final int ROW_GAP = 3;
    private static final int PAD     = 4;

    public ArmorWidget(HudElement el) { super(el, "装备耐久"); }

    public void setVertical(boolean v) { element.vertical = v; }
    public boolean isVertical()        { return element.vertical; }

    @Override public String getValue() { return null; }
    @Override public String getLabel() { return "装备耐久"; }

    private int rawW() {
        return element.vertical
            ? PAD + ICON + ICON_GAP + BAR_W + PAD
            : PAD + SLOTS.length * (ICON + ICON_GAP + BAR_W + ROW_GAP) - ROW_GAP + PAD;
    }
    private int rawH() {
        return element.vertical
            ? PAD + SLOTS.length * (ROW_H + ROW_GAP) - ROW_GAP + PAD
            : PAD + ROW_H + PAD;
    }

    @Override public int getWidth() { return Math.round(rawW() * getScale()); }
    @Override public int scaledH()  { return Math.round(rawH() * getScale()); }

    @Override
    public void render(GuiGraphics gfx, boolean editMode) {
        if (!element.visible && !editMode) return;
        Minecraft mc = Minecraft.getInstance();
        float sc = getScale();
        int rw = rawW(), rh = rawH();

        gfx.pose().pushMatrix();
        gfx.pose().translate((float) element.x, (float) element.y);
        gfx.pose().scale(sc, sc);

        RoundedRect.fill(gfx, 0, 0, rw, rh, BG_COLOR, CORNER);

        for (int i = 0; i < SLOTS.length; i++) {
            int rx = PAD + (element.vertical ? 0 : i * (ICON + ICON_GAP + BAR_W + ROW_GAP));
            int ry = PAD + (element.vertical ? i * (ROW_H + ROW_GAP) : 0);

            ItemStack stack = mc.player != null
                ? mc.player.getItemBySlot(SLOTS[i])
                : ItemStack.EMPTY;

            // Item icon
            if (!stack.isEmpty()) {
                gfx.renderItem(stack, rx, ry);
            } else {
                gfx.fill(rx + 4, ry + 4, rx + ICON - 4, ry + ICON - 4, 0x22FFFFFF);
            }

            // Continuous bar
            int barX = rx + ICON + ICON_GAP;
            int barY = ry + (ROW_H - BAR_H) / 2;

            // Bar background (dark track)
            gfx.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0xFF1A1A1A);

            if (!stack.isEmpty()) {
                float pct;
                int fillColor;

                if (stack.isDamageableItem()) {
                    pct = stack.isDamaged()
                        ? (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()
                        : 1f;
                    fillColor = pct > 0.6f ? 0xFF44DD44 : pct > 0.3f ? 0xFFDDAA00 : 0xFFDD3333;
                } else {
                    pct = 1f;
                    fillColor = 0xFF44AA44;  // unbreakable — full dim green
                }

                int fillW = Math.max(1, Math.round(BAR_W * pct));
                // Main fill
                gfx.fill(barX, barY, barX + fillW, barY + BAR_H, fillColor);
                // Top highlight (1px bright edge)
                int bright = brighten(fillColor, 60);
                gfx.fill(barX, barY, barX + fillW, barY + 1, bright);
                // Subtle inner shadow at bottom
                gfx.fill(barX, barY + BAR_H - 1, barX + fillW, barY + BAR_H,
                    darken(fillColor, 40));
            }
        }

        if (editMode) RoundedRect.fill(gfx, 0, 0, rw, rh, 0x28FFFFFF, CORNER);
        gfx.pose().popMatrix();
    }

    private static int brighten(int col, int amt) {
        return 0xFF000000
            | (Math.min(255, ((col >> 16) & 0xFF) + amt) << 16)
            | (Math.min(255, ((col >>  8) & 0xFF) + amt) <<  8)
            |  Math.min(255, ( col        & 0xFF) + amt);
    }
    private static int darken(int col, int amt) {
        return 0xFF000000
            | (Math.max(0, ((col >> 16) & 0xFF) - amt) << 16)
            | (Math.max(0, ((col >>  8) & 0xFF) - amt) <<  8)
            |  Math.max(0, ( col        & 0xFF) - amt);
    }

    @Override
    public boolean contains(double mx, double my) {
        return mx >= element.x && mx < element.x + getWidth()
            && my >= element.y && my < element.y + scaledH();
    }
}
