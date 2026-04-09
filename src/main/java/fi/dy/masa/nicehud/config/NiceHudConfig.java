package fi.dy.masa.nicehud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;

/** Persists all HUD element positions, visibility and scale. */
public class NiceHudConfig {

    private static final Path CONFIG_PATH = Path.of(
        System.getProperty("user.home"), ".minecraft", "config", "nicehud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Inventory / equipment ───────────────────────────────────────────────
    public HudElement inv_hotbar    = on(5, 221);
    public HudElement inv_full      = off(5, 245);
    public HudElement armor         = on(5, 245);
    public HudElement statusEffects = off(5, 270);

    // ── Direct widgets ──────────────────────────────────────────────────────
    public HudElement d_fps         = on(5, 5);
    public HudElement d_memory      = on(5, 22);
    public HudElement d_coordX      = off(5, 39);
    public HudElement d_coordY      = off(5, 56);
    public HudElement d_coordZ      = off(5, 73);
    public HudElement d_facing      = on(5, 39);
    public HudElement d_speed       = on(5, 56);
    public HudElement d_biome       = on(5, 73);
    public HudElement d_dimension   = on(5, 90);
    public HudElement d_time        = on(5, 107);
    public HudElement d_tps         = on(5, 124);
    public HudElement d_ping        = on(5, 141);

    // ── MiniHUD — Position ──────────────────────────────────────────────────
    public HudElement m_BLOCK_POS             = off(5, 401);
    public HudElement m_CHUNK_POS             = off(5, 419);
    public HudElement m_BLOCK_IN_CHUNK        = off(5, 437);
    public HudElement m_REGION_FILE           = off(5, 455);
    public HudElement m_COORDINATES_SCALED    = on(5, 158);
    public HudElement m_ROTATION_YAW          = off(5, 473);
    public HudElement m_ROTATION_PITCH        = on(5, 175);
    public HudElement m_SLIME_CHUNK           = on(5, 192);

    // ── MiniHUD — World ─────────────────────────────────────────────────────
    public HudElement m_LIGHT_LEVEL           = on(5, 209);
    public HudElement m_BIOME_REG_NAME        = off(5, 491);
    public HudElement m_SERVER_TPS            = off(5, 509);
    public HudElement m_LOADED_CHUNKS         = off(5, 527);
    public HudElement m_LOADED_CHUNKS_COUNT   = off(5, 545);
    public HudElement m_MOB_CAPS              = off(5, 563);
    public HudElement m_TILE_ENTITIES         = off(5, 581);
    public HudElement m_ENTITIES_CLIENT_WORLD = off(5, 599);
    public HudElement m_PARTICLE_COUNT        = off(5, 617);
    public HudElement m_CHUNK_SECTIONS        = off(5, 635);
    public HudElement m_CHUNK_SECTIONS_FULL   = off(5, 653);
    public HudElement m_CHUNK_UPDATES         = off(5, 671);

    // ── MiniHUD — Time ──────────────────────────────────────────────────────
    public HudElement m_TIME_REAL             = off(5, 581);
    public HudElement m_TIME_WORLD            = off(5, 599);
    public HudElement m_TIME_WORLD_FORMATTED  = off(5, 617);
    public HudElement m_TIME_IRL              = off(5, 635);
    public HudElement m_TIME_DAY_MODULO       = off(5, 653);
    public HudElement m_TIME_TOTAL_MODULO     = off(5, 671);

    // ── MiniHUD — Player ────────────────────────────────────────────────────
    public HudElement m_PLAYER_EXPERIENCE     = off(5, 689);
    public HudElement m_SPEED_HV              = off(5, 707);
    public HudElement m_SPEED_AXIS            = off(5, 725);
    public HudElement m_MEMORY_USAGE          = off(5, 743);
    public HudElement m_BLOCK_BREAK_SPEED     = off(5, 761);

    // ── MiniHUD — Looking-at block ──────────────────────────────────────────
    public HudElement m_LOOKING_AT_BLOCK       = off(5, 779);
    public HudElement m_LOOKING_AT_BLOCK_CHUNK = off(5, 797);
    public HudElement m_LOOKING_AT_CHUNK       = off(5, 815);
    public HudElement m_BLOCK_PROPS            = off(5, 833);
    public HudElement m_HONEY_LEVEL            = off(5, 851);

    // ── MiniHUD — Looking-at entity ─────────────────────────────────────────
    public HudElement m_LOOKING_AT_ENTITY      = off(5, 869);
    public HudElement m_ENTITY_VARIANT         = off(5, 887);
    public HudElement m_ENTITY_HOME_POS        = off(5, 905);
    public HudElement m_ENTITY_REG_NAME        = off(5, 923);
    public HudElement m_LOOKING_AT_EFFECTS     = off(5, 941);
    public HudElement m_LOOKING_AT_PLAYER_EXP  = off(5, 959);
    public HudElement m_ZOMBIE_CONVERSION      = off(5, 977);
    public HudElement m_DOLPHIN_TREASURE       = off(5, 995);
    public HudElement m_PANDA_GENE             = off(5, 1013);

    // ── MiniHUD — Block entities ────────────────────────────────────────────
    public HudElement m_BEE_COUNT              = off(5, 1031);
    public HudElement m_COMPARATOR_OUTPUT      = off(5, 1049);
    public HudElement m_FURNACE_XP             = off(5, 1067);

    // ── MiniHUD — Horse ─────────────────────────────────────────────────────
    public HudElement m_HORSE_SPEED            = off(5, 1085);
    public HudElement m_HORSE_JUMP             = off(5, 1103);
    public HudElement m_HORSE_MAX_HEALTH       = off(5, 1121);

    // ── Factory helpers ─────────────────────────────────────────────────────
    private static HudElement on(int x, int y)  { return new HudElement(true,  x, y); }
    private static HudElement off(int x, int y) { return new HudElement(false, x, y); }

    // ── Persistence ─────────────────────────────────────────────────────────

    public static NiceHudConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                    NiceHudConfig c = GSON.fromJson(r, NiceHudConfig.class);
                    if (c != null) { c.fixNulls(); return c; }
                }
            }
        } catch (Exception e) {
            System.err.println("[NiceHUD] Failed to load config: " + e.getMessage());
        }
        return new NiceHudConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            System.err.println("[NiceHUD] Failed to save config: " + e.getMessage());
        }
    }

    /** Replace any null fields (new fields added in updates) with defaults. */
    private void fixNulls() {
        NiceHudConfig def = new NiceHudConfig();
        for (var f : NiceHudConfig.class.getDeclaredFields()) {
            if (!f.getType().equals(HudElement.class)) continue;
            try {
                if (f.get(this) == null) f.set(this, f.get(def));
            } catch (Exception ignored) {}
        }
    }
}
