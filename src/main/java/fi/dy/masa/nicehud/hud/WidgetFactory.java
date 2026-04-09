package fi.dy.masa.nicehud.hud;

import fi.dy.masa.nicehud.config.NiceHudConfig;
import java.util.ArrayList;
import java.util.List;

/** Creates all HudWidget instances from config. */
public final class WidgetFactory {

    private WidgetFactory() {}

    public static List<HudWidget> create(NiceHudConfig c) {
        List<HudWidget> list = new ArrayList<>();

        // ── Inventory / equipment widgets ─────────────────────────────────────────
        list.add(new InventoryWidget(c.inv_hotbar, false));
        list.add(new InventoryWidget(c.inv_full,   true));
        list.add(new ArmorWidget(c.armor));
        list.add(new StatusEffectWidget(c.statusEffects));

        // ── Direct widgets (no MiniHUD) ───────────────────────────────────────
        list.add(new DirectWidgets.FpsWidget    (c.d_fps));
        list.add(new DirectWidgets.MemoryWidget (c.d_memory));
        list.add(new DirectWidgets.CoordXWidget (c.d_coordX));
        list.add(new DirectWidgets.CoordYWidget (c.d_coordY));
        list.add(new DirectWidgets.CoordZWidget (c.d_coordZ));
        list.add(new DirectWidgets.FacingWidget (c.d_facing));
        list.add(new DirectWidgets.SpeedWidget  (c.d_speed));
        list.add(new DirectWidgets.BiomeWidget  (c.d_biome));
        list.add(new DirectWidgets.DimWidget    (c.d_dimension));
        list.add(new DirectWidgets.TimeWidget   (c.d_time));
        list.add(new DirectWidgets.TpsWidget    (c.d_tps));
        list.add(new DirectWidgets.PingWidget   (c.d_ping));

        // ── MiniHUD InfoLine widgets ───────────────────────────────────────────
        // Position
        list.add(m(c.m_BLOCK_POS,            "方块坐标",     "BLOCK_POS"));
        list.add(m(c.m_CHUNK_POS,            "区块坐标",     "CHUNK_POS"));
        list.add(m(c.m_BLOCK_IN_CHUNK,       "区块内位置",      "BLOCK_IN_CHUNK"));
        list.add(m(c.m_REGION_FILE,          "区域文件",   "REGION_FILE"));
        list.add(m(c.m_COORDINATES_SCALED,   "下界坐标", "COORDINATES_SCALED"));
        list.add(m(c.m_ROTATION_YAW,         "偏航角",           "ROTATION_YAW"));
        list.add(m(c.m_ROTATION_PITCH,       "俯仰角",         "ROTATION_PITCH"));
        list.add(m(c.m_SLIME_CHUNK,          "史莱姆区块",   "SLIME_CHUNK"));
        // Light / biome
        list.add(m(c.m_LIGHT_LEVEL,          "亮度",         "LIGHT_LEVEL"));
        list.add(m(c.m_BIOME_REG_NAME,       "群系ID",      "BIOME_REG_NAME"));
        // World / server
        list.add(m(c.m_SERVER_TPS,           "服务器TPS",           "SERVER_TPS"));
        list.add(m(c.m_LOADED_CHUNKS,        "已加载区块",        "LOADED_CHUNKS"));
        list.add(m(c.m_LOADED_CHUNKS_COUNT,  "区块数量",   "LOADED_CHUNKS_COUNT"));
        list.add(m(c.m_MOB_CAPS,             "生物上限",      "MOB_CAPS"));
        list.add(m(c.m_TILE_ENTITIES,        "方块实体", "TILE_ENTITIES"));
        list.add(m(c.m_ENTITIES_CLIENT_WORLD,"实体数量",      "ENTITIES_CLIENT_WORLD"));
        list.add(m(c.m_PARTICLE_COUNT,       "粒子数量",     "PARTICLE_COUNT"));
        list.add(m(c.m_CHUNK_SECTIONS,       "区块截面",   "CHUNK_SECTIONS"));
        list.add(m(c.m_CHUNK_SECTIONS_FULL,  "截面详情",    "CHUNK_SECTIONS_FULL"));
        list.add(m(c.m_CHUNK_UPDATES,        "区块更新",    "CHUNK_UPDATES"));
        // Time
        list.add(m(c.m_TIME_REAL,            "真实时间",     "TIME_REAL"));
        list.add(m(c.m_TIME_WORLD,           "世界时间",    "TIME_WORLD"));
        list.add(m(c.m_TIME_WORLD_FORMATTED, "时间(格式化)",    "TIME_WORLD_FORMATTED"));
        list.add(m(c.m_TIME_IRL,             "现实时间",      "TIME_IRL"));
        list.add(m(c.m_TIME_DAY_MODULO,      "日余数",    "TIME_DAY_MODULO"));
        list.add(m(c.m_TIME_TOTAL_MODULO,    "总余数",  "TIME_TOTAL_MODULO"));
        // Player
        list.add(m(c.m_PLAYER_EXPERIENCE,    "玩家经验",            "PLAYER_EXPERIENCE"));
        list.add(m(c.m_SPEED_HV,             "水平/垂直速度",     "SPEED_HV"));
        list.add(m(c.m_SPEED_AXIS,           "轴向速度",    "SPEED_AXIS"));
        list.add(m(c.m_MEMORY_USAGE,         "内存使用",        "MEMORY_USAGE"));
        list.add(m(c.m_BLOCK_BREAK_SPEED,    "挖掘速度",   "BLOCK_BREAK_SPEED"));
        // Looking-at block
        list.add(m(c.m_LOOKING_AT_BLOCK,     "注视方块",    "LOOKING_AT_BLOCK"));
        list.add(m(c.m_LOOKING_AT_BLOCK_CHUNK,"注视区块", "LOOKING_AT_BLOCK_CHUNK"));
        list.add(m(c.m_LOOKING_AT_CHUNK,     "注视区块位置",    "LOOKING_AT_CHUNK"));
        list.add(m(c.m_BLOCK_PROPS,          "方块属性",   "BLOCK_PROPS"));
        list.add(m(c.m_HONEY_LEVEL,          "蜂蜜等级",   "HONEY_LEVEL"));
        // Looking-at entity
        list.add(m(c.m_LOOKING_AT_ENTITY,    "注视实体",   "LOOKING_AT_ENTITY"));
        list.add(m(c.m_ENTITY_VARIANT,       "实体变体",   "ENTITY_VARIANT"));
        list.add(m(c.m_ENTITY_HOME_POS,      "实体归宿",      "ENTITY_HOME_POS"));
        list.add(m(c.m_ENTITY_REG_NAME,      "实体ID",     "ENTITY_REG_NAME"));
        list.add(m(c.m_LOOKING_AT_EFFECTS,   "药水效果",       "LOOKING_AT_EFFECTS"));
        list.add(m(c.m_LOOKING_AT_PLAYER_EXP,"目标经验",     "LOOKING_AT_PLAYER_EXP"));
        list.add(m(c.m_ZOMBIE_CONVERSION,    "僵尸转化",  "ZOMBIE_CONVERSION"));
        list.add(m(c.m_DOLPHIN_TREASURE,     "海豚宝藏",       "DOLPHIN_TREASURE"));
        list.add(m(c.m_PANDA_GENE,           "熊猫基因",    "PANDA_GENE"));
        // Block entities
        list.add(m(c.m_BEE_COUNT,            "蜜蜂数量",     "BEE_COUNT"));
        list.add(m(c.m_COMPARATOR_OUTPUT,    "比较器输出",    "COMPARATOR_OUTPUT"));
        list.add(m(c.m_FURNACE_XP,           "熔炉经验",    "FURNACE_XP"));
        // Horse
        list.add(m(c.m_HORSE_SPEED,          "马速度",   "HORSE_SPEED"));
        list.add(m(c.m_HORSE_JUMP,           "马跳跃",    "HORSE_JUMP"));
        list.add(m(c.m_HORSE_MAX_HEALTH,     "马生命值",      "HORSE_MAX_HEALTH"));

        return list;
    }

    private static MiniHudWidget m(fi.dy.masa.nicehud.config.HudElement el,
                                    String label, String toggle) {
        return new MiniHudWidget(el, label, toggle);
    }
}
