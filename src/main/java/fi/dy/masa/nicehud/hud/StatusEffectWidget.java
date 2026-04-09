package fi.dy.masa.nicehud.hud;

import fi.dy.masa.nicehud.config.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Status effects: [icon 9×9] [mm:ss]
 * Texture path: namespace:textures/mob_effect/name.png  (same as StarHUD)
 * Rendered with RenderPipelines.GUI_TEXTURED (same pipeline StarHUD uses)
 */
public class StatusEffectWidget extends HudWidget {

    private static final int ICON_SZ   = 18;  // full 18x18 icon display
    private static final int ROW_H     = ICON_SZ;
    private static final int ROW_GAP   = 2;
    private static final int INNER_PAD = 4;
    private static final int TIME_GAP  = 5;

    // Cache effect textures (namespace:textures/mob_effect/name.png)
    private static final java.util.Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, ResourceLocation>
        TEX_CACHE = new java.util.HashMap<>();

    public StatusEffectWidget(HudElement el) { super(el, "状态效果"); }

    @Override public String getValue() { return null; }
    @Override public String getLabel() { return "状态效果"; }

    private List<MobEffectInstance> effects() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return List.of();
        List<MobEffectInstance> list = new ArrayList<>(mc.player.getActiveEffects());
        list.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        return list;
    }

    private static ResourceLocation effectTexture(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return TEX_CACHE.computeIfAbsent(effect, e -> {
            // Mojang mapping: Holder.unwrapKey() returns Optional<ResourceKey>
            return e.unwrapKey()
                .map(k -> ResourceLocation.fromNamespaceAndPath(
                    k.location().getNamespace(),
                    "textures/mob_effect/" + k.location().getPath() + ".png"))
                .orElse(ResourceLocation.withDefaultNamespace("missingno"));
        });
    }

    private int rawW(List<MobEffectInstance> fx) {
        Minecraft mc = Minecraft.getInstance();
        int maxW = mc.font.width("00:00");
        for (MobEffectInstance e : fx) maxW = Math.max(maxW, mc.font.width(fmt(e)));
        return INNER_PAD + ICON_SZ + TIME_GAP + maxW + INNER_PAD;
    }
    private int rawH(List<MobEffectInstance> fx) {
        if (fx.isEmpty()) return ICON_SZ + INNER_PAD * 2;
        return INNER_PAD + fx.size() * ROW_H + (fx.size() - 1) * ROW_GAP + INNER_PAD;
    }

    @Override public int getWidth() { return Math.round(rawW(effects()) * getScale()); }
    @Override public int scaledH()  { return Math.round(rawH(effects()) * getScale()); }

    @Override
    public void render(GuiGraphics gfx, boolean editMode) {
        if (!element.visible && !editMode) return;
        Minecraft mc = Minecraft.getInstance();
        float sc = getScale();
        List<MobEffectInstance> fx = effects();
        if (fx.isEmpty() && !editMode) return;

        int rw = rawW(fx), rh = rawH(fx);
        gfx.pose().pushMatrix();
        gfx.pose().translate((float) element.x, (float) element.y);
        gfx.pose().scale(sc, sc);

        RoundedRect.fill(gfx, 0, 0, rw, rh, BG_COLOR, CORNER);

        if (fx.isEmpty()) {
            gfx.drawString(mc.font, "§7状态效果", INNER_PAD, INNER_PAD + 2, 0xFFAAAAAA, false);
        } else {
            for (int i = 0; i < fx.size(); i++) {
                MobEffectInstance inst = fx.get(i);
                int ry = INNER_PAD + i * (ROW_H + ROW_GAP);

                // Icon: 18x18 texture scaled to 9x9, rendered in 13x13 box (StarHUD style)
                ResourceLocation tex = effectTexture(inst.getEffect());
// alpha flash for expiring effects (< 10s)
                int alpha = 0xFF;
                if (!inst.isInfiniteDuration() && inst.getDuration() < 200) {
                    float a = Math.max(0.2f, (float) Math.abs(
                        Math.cos(inst.getDuration() * Math.PI / 5.0)));
                    alpha = (int)(a * 255) & 0xFF;
                }
                int color = (alpha << 24) | 0xFFFFFF;

                // Render 18x18 effect icon at full size
                int iconY = ry + (ROW_H - ICON_SZ) / 2;
                try {
                    gfx.blit(RenderPipelines.GUI_TEXTURED, tex,
                        INNER_PAD, iconY,
                        0, 0, ICON_SZ, ICON_SZ, ICON_SZ, ICON_SZ, color);
                } catch (Exception e2) {
                    gfx.fill(INNER_PAD, iconY, INNER_PAD + ICON_SZ, iconY + ICON_SZ, 0xFF555555);
                }

                // Duration text centred vertically
                String time = fmt(inst);
                int textY = ry + (ROW_H - mc.font.lineHeight) / 2;
                gfx.drawString(mc.font, time,
                    INNER_PAD + ICON_SZ + TIME_GAP, textY, durCol(inst.getDuration()), false);
            }
        }

        if (editMode) RoundedRect.fill(gfx, 0, 0, rw, rh, 0x28FFFFFF, CORNER);
        gfx.pose().popMatrix();
    }

    private static String fmt(MobEffectInstance inst) {
        if (inst.isInfiniteDuration()) return "--:--";
        int s = inst.getDuration() / 20, m = s / 60; s %= 60;
        return String.format("%02d:%02d", m, s);
    }

    private static int durCol(int ticks) {
        return ticks > 600 ? 0xFF55FF55 : ticks > 200 ? 0xFFFFFF55 : 0xFFFF5555;
    }

    @Override
    public boolean contains(double mx, double my) {
        return mx >= element.x && mx < element.x + getWidth()
            && my >= element.y && my < element.y + scaledH();
    }
}
