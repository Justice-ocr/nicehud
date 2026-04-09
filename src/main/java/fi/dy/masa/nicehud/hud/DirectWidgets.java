package fi.dy.masa.nicehud.hud;

import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.nicehud.config.HudElement;
import net.minecraft.client.Minecraft;

/** All widgets that read data directly (no MiniHUD InfoLine needed). */
public final class DirectWidgets {

    private DirectWidgets() {}

    public static class FpsWidget extends HudWidget {
        public FpsWidget(HudElement e) { super(e, "帧率"); }
        @Override public String getValue() { return String.valueOf(Minecraft.getInstance().getFps()); }
        @Override protected int getTextColor() {
            int f = Minecraft.getInstance().getFps();
            return f >= 60 ? 0xFF55FF55 : f >= 30 ? 0xFFFFFF55 : 0xFFFF5555;
        }
    }

    public static class MemoryWidget extends HudWidget {
        public MemoryWidget(HudElement e) { super(e, "内存"); }
        @Override public String getValue() {
            Runtime rt = Runtime.getRuntime();
            long used  = (rt.totalMemory() - rt.freeMemory()) >> 20;
            long max   = rt.maxMemory() >> 20;
            return used + "/" + max + " MB";
        }
        @Override protected int getTextColor() {
            Runtime rt = Runtime.getRuntime();
            double p = (double)(rt.totalMemory()-rt.freeMemory()) / rt.maxMemory();
            return p < 0.6 ? 0xFF55FF55 : p < 0.85 ? 0xFFFFFF55 : 0xFFFF5555;
        }
    }

    public static class CoordXWidget extends HudWidget {
        public CoordXWidget(HudElement e) { super(e, "X"); }
        @Override public String getValue() {
            var p = Minecraft.getInstance().player;
            return p == null ? null : String.format("%.1f", p.getX());
        }
        @Override protected int getTextColor() { return 0xFFFF5555; }
    }

    public static class CoordYWidget extends HudWidget {
        public CoordYWidget(HudElement e) { super(e, "Y"); }
        @Override public String getValue() {
            var p = Minecraft.getInstance().player;
            return p == null ? null : String.format("%.1f", p.getY());
        }
        @Override protected int getTextColor() { return 0xFF55FF55; }
    }

    public static class CoordZWidget extends HudWidget {
        public CoordZWidget(HudElement e) { super(e, "Z"); }
        @Override public String getValue() {
            var p = Minecraft.getInstance().player;
            return p == null ? null : String.format("%.1f", p.getZ());
        }
        @Override protected int getTextColor() { return 0xFF5555FF; }
    }

    public static class FacingWidget extends HudWidget {
        public FacingWidget(HudElement e) { super(e, "朝向"); }
        @Override public String getValue() {
            var p = Minecraft.getInstance().player;
            if (p == null) return null;
            float y = ((p.getYRot() % 360) + 360) % 360;
            String d;
            if (y < 22.5 || y >= 337.5) d = "S";
            else if (y < 67.5)  d = "SW";
            else if (y < 112.5) d = "W";
            else if (y < 157.5) d = "NW";
            else if (y < 202.5) d = "N";
            else if (y < 247.5) d = "NE";
            else if (y < 292.5) d = "E";
            else                d = "SE";
            return String.format("%s %.0f°", d, y);
        }
    }

    public static class SpeedWidget extends HudWidget {
        public SpeedWidget(HudElement e) { super(e, "速度"); }
        @Override public String getValue() {
            var p = Minecraft.getInstance().player;
            if (p == null) return null;
            double dx = p.getDeltaMovement().x, dz = p.getDeltaMovement().z;
            return String.format("%.2f m/s", Math.sqrt(dx*dx+dz*dz)*20);
        }
    }

    public static class BiomeWidget extends HudWidget {
        public BiomeWidget(HudElement e) { super(e, "群系"); }
        @Override public String getValue() {
            var mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return null;
            var h = mc.level.getBiome(mc.player.blockPosition());
            String key = h.unwrapKey().map(k -> k.location().getPath()).orElse("unknown");
            return translateBiome(key);
        }

        private static String translateBiome(String key) {
            return switch (key) {
                // 森林
                case "forest"              -> "森林";
                case "birch_forest"        -> "白桦树森林";
                case "dark_forest"         -> "黑暗森林";
                case "flower_forest"       -> "繁花森林";
                case "old_growth_birch_forest" -> "原始白桦树森林";
                case "old_growth_pine_taiga"   -> "原始松木针叶林";
                case "old_growth_spruce_taiga" -> "原始云杉针叶林";
                // 平原
                case "plains"              -> "平原";
                case "sunflower_plains"    -> "向日葵平原";
                case "meadow"              -> "草甸";
                case "cherry_grove"        -> "樱花树丛";
                // 山地
                case "windswept_hills"     -> "风袭丘陵";
                case "windswept_gravelly_hills" -> "风袭砾石丘陵";
                case "windswept_forest"    -> "风袭森林";
                case "jagged_peaks"        -> "参差山峰";
                case "frozen_peaks"        -> "冻结山峰";
                case "stony_peaks"         -> "石质山峰";
                case "snowy_slopes"        -> "积雪山坡";
                // 雨林/热带
                case "jungle"              -> "丛林";
                case "sparse_jungle"       -> "稀疏丛林";
                case "bamboo_jungle"       -> "竹林";
                // 沙漠/干旱
                case "desert"              -> "沙漠";
                case "savanna"             -> "热带草原";
                case "savanna_plateau"     -> "热带草原高原";
                case "windswept_savanna"   -> "风袭热带草原";
                case "badlands"            -> "恶地";
                case "eroded_badlands"     -> "侵蚀恶地";
                case "wooded_badlands"     -> "林地恶地";
                // 积雪/寒冷
                case "snowy_plains"        -> "积雪平原";
                case "snowy_beach"         -> "积雪海滩";
                case "snowy_taiga"         -> "积雪针叶林";
                case "grove"               -> "雪林";
                case "ice_spikes"          -> "冰刺";
                case "frozen_river"        -> "冻结河流";
                case "frozen_ocean"        -> "冻结海洋";
                case "deep_frozen_ocean"   -> "深冻结海洋";
                // 海洋
                case "ocean"               -> "海洋";
                case "deep_ocean"          -> "深海";
                case "warm_ocean"          -> "温暖海洋";
                case "lukewarm_ocean"      -> "温带海洋";
                case "cold_ocean"          -> "寒冷海洋";
                case "deep_lukewarm_ocean" -> "深温带海洋";
                case "deep_cold_ocean"     -> "深寒冷海洋";
                // 海岸/湿地
                case "beach"               -> "海滩";
                case "stony_shore"         -> "石岸";
                case "swamp"               -> "沼泽";
                case "mangrove_swamp"      -> "红树林沼泽";
                case "mushroom_fields"     -> "蘑菇岛";
                // 河流/地下
                case "river"               -> "河流";
                case "dripstone_caves"     -> "溶岩洞穴";
                case "lush_caves"          -> "繁茂洞穴";
                case "deep_dark"           -> "幽深黑暗";
                // 针叶林
                case "taiga"               -> "针叶林";
                case "stony_peaks_taiga"   -> "石峰针叶林";
                // 下界
                case "nether_wastes"       -> "下界荒地";
                case "soul_sand_valley"    -> "灵魂沙峡谷";
                case "crimson_forest"      -> "绯红森林";
                case "warped_forest"       -> "诡异森林";
                case "basalt_deltas"       -> "玄武岩三角洲";
                // 末地
                case "the_end"             -> "末地中心";
                case "small_end_islands"   -> "末地小岛";
                case "end_midlands"        -> "末地中部";
                case "end_highlands"       -> "末地高地";
                case "end_barrens"         -> "末地荒地";
                case "the_void"            -> "虚空";
                // 未知：按下划线分词首字母大写
                default -> {
                    if (key.equals("unknown")) yield "未知";
                    var parts = key.split("_");
                    var sb = new StringBuilder();
                    for (var p : parts) {
                        if (p.isEmpty()) continue;
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
                    }
                    yield sb.toString();
                }
            };
        }
    }

    public static class DimWidget extends HudWidget {
        public DimWidget(HudElement e) { super(e, "维度"); }
        @Override public String getValue() {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return null;
            return switch (mc.level.dimension().location().getPath()) {
                case "overworld"  -> "主世界";
                case "the_nether" -> "下界";
                case "the_end"    -> "末地";
                default -> mc.level.dimension().location().getPath();
            };
        }
        @Override protected int getTextColor() {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return 0xFFFFFFFF;
            return switch (mc.level.dimension().location().getPath()) {
                case "overworld"  -> 0xFF88FF88;
                case "the_nether" -> 0xFFFF8830;
                case "the_end"    -> 0xFFCC88FF;
                default -> 0xFFFFFFFF;
            };
        }
    }

    public static class TimeWidget extends HudWidget {
        public TimeWidget(HudElement e) { super(e, "时间"); }
        @Override public String getValue() {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return null;
            long t    = mc.level.getDayTime() % 24000;
            long hour = (t / 1000 + 6) % 24;
            long min  = (t % 1000) * 60 / 1000;
            return String.format("%02d:%02d", hour, min);
        }
    }

    public static class TpsWidget extends HudWidget {
        public TpsWidget(HudElement e) { super(e, "TPS"); }
        @Override public String getValue() {
            if (Minecraft.getInstance().level == null) return null;
            return String.format("%.1f", DataStorage.getInstance().getServerTPS());
        }
        @Override protected int getTextColor() {
            double t = DataStorage.getInstance().getServerTPS();
            return t >= 19 ? 0xFF55FF55 : t >= 15 ? 0xFFFFFF55 : 0xFFFF5555;
        }
    }

    public static class PingWidget extends HudWidget {
        public PingWidget(HudElement e) { super(e, "延迟"); }
        @Override public String getValue() {
            var mc = Minecraft.getInstance();
            if (mc.getConnection() == null || mc.player == null) return null;
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return info == null ? null : info.getLatency() + " ms";
        }
        @Override protected int getTextColor() {
            var mc = Minecraft.getInstance();
            if (mc.getConnection() == null || mc.player == null) return 0xFFFFFFFF;
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info == null) return 0xFFFFFFFF;
            int ms = info.getLatency();
            return ms <= 80 ? 0xFF55FF55 : ms <= 150 ? 0xFFFFFF55 : 0xFFFF5555;
        }
    }
}
