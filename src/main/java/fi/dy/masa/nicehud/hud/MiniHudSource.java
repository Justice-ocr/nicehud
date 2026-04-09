package fi.dy.masa.nicehud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calls MiniHUD's InfoLine.parse(InfoLineContext) via reflection.
 * Uses the 7-arg constructor to build context, avoiding Field.set() module issues.
 */
public final class MiniHudSource {

    private static final Logger LOG = LoggerFactory.getLogger("nicehud.source");

    // One-time log per toggle (first call only)
    private static final Set<String> debugged = Collections.synchronizedSet(new HashSet<>());

    // Reflection handles
    private static Constructor<?> ctxCtor7;
    private static Method parseMethod;
    private static Method isTranslated, hasArgs, formatMethod, argsMethod, isEmptyMethod;
    private static boolean ready = false;

    static {
        try {
            Class<?> ctxClass = Class.forName("fi.dy.masa.minihud.info.InfoLineContext");
            for (Constructor<?> c : ctxClass.getDeclaredConstructors())
                if (c.getParameterCount() == 7) { ctxCtor7 = c; break; }
            if (ctxCtor7 != null) ctxCtor7.setAccessible(true);

            Class<?> infoLine = Class.forName("fi.dy.masa.minihud.info.InfoLine");
            parseMethod = infoLine.getMethod("parse", ctxClass);

            Class<?> entry = Class.forName("fi.dy.masa.minihud.info.InfoLine$Entry");
            isTranslated  = entry.getMethod("isTranslated");
            hasArgs       = entry.getMethod("hasArgs");
            formatMethod  = entry.getMethod("format");
            argsMethod    = entry.getMethod("args");
            isEmptyMethod = entry.getMethod("isEmpty");

            ready = ctxCtor7 != null;
        } catch (Exception e) {
            LoggerFactory.getLogger("nicehud").error("[NiceHUD] MiniHudSource init failed", e);
        }
    }

    private MiniHudSource() {}

    // ── Public API ─────────────────────────────────────────────────────────

    public static String getData(String toggleName) {
        if (!ready) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        boolean firstCall = debugged.add(toggleName);

        Object toggle = getToggle(toggleName);
        if (toggle == null) {
            if (firstCall) LOG.warn("[NiceHUD] toggle not found: {}", toggleName);
            return null;
        }
        Object ctx = buildCtx(toggleName, mc);
        if (ctx == null) {
            if (firstCall) LOG.warn("[NiceHUD] ctx=null for: {}", toggleName);
            return null;
        }
        Object parser = initParser(toggle);
        if (parser == null) {
            if (firstCall) LOG.warn("[NiceHUD] parser=null for: {}", toggleName);
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            List<Object> entries = (List<Object>) parseMethod.invoke(parser, ctx);
            if (firstCall) LOG.info("[NiceHUD] {} → {} entries", toggleName,
                entries == null ? "null" : entries.size());
            if (entries == null || entries.isEmpty()) return null;

            var sb = new StringBuilder();
            for (Object e : entries) {
                if ((Boolean) isEmptyMethod.invoke(e)) continue;
                String txt = buildText(e);
                txt = txt.replaceAll("§[0-9a-fk-or]", "").strip();
                if (!txt.isBlank()) {
                    if (sb.length() > 0) sb.append("  ");
                    sb.append(txt);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;

        } catch (Exception e) {
            if (firstCall) LOG.error("[NiceHUD] {} parse failed: {}", toggleName, e.getMessage());
            return null;
        }
    }

    // ── Context building ────────────────────────────────────────────────────

    private static Object buildCtx(String name, Minecraft mc) {
        Entity   player = mc.player;
        Level    level  = mc.level;
        Level    best   = bestWorld(mc);
        BlockPos pp     = player != null ? player.blockPosition() : BlockPos.ZERO;
        ChunkPos pc     = new ChunkPos(pp);

        try {
            return switch (name) {
                // Camera / movement
                case "ROTATION_YAW", "ROTATION_PITCH", "SPEED_HV", "SPEED_AXIS",
                     "COORDINATES_SCALED", "LOOKING_AT_CHUNK",
                     "PLAYER_EXPERIENCE", "PLAYER_EXP" ->
                    ctx(level, player, null, null, null, null, null);

                // Block position
                case "BLOCK_POS", "CHUNK_POS", "BLOCK_IN_CHUNK",
                     "REGION_FILE", "BIOME_REG_NAME", "LIGHT_LEVEL", "SLIME_CHUNK" ->
                    ctx(level, null, null, pp, null, pc, null);

                // World / server
                case "SERVER_TPS", "MOB_CAPS", "LOADED_CHUNKS", "LOADED_CHUNKS_COUNT",
                     "TIME_REAL", "TIME_WORLD", "TIME_WORLD_FORMATTED", "TIME_IRL",
                     "TIME_DAY_MODULO", "TIME_TOTAL_MODULO", "PARTICLE_COUNT",
                     "CHUNK_SECTIONS", "CHUNK_SECTIONS_FULL", "CHUNK_UPDATES",
                     "BLOCK_BREAK_SPEED", "MEMORY_USAGE" ->
                    ctx(best, null, null, null, null, null, null);

                // Client world
                case "ENTITIES_CLIENT_WORLD", "TILE_ENTITIES" ->
                    ctx(level, null, null, null, null, null, null);

                case "DIFFICULTY" ->
                    ctx(best, null, null, pp, null, null, null);

                // Looking-at block
                case "LOOKING_AT_BLOCK", "LOOKING_AT_BLOCK_CHUNK", "BLOCK_PROPS" -> {
                    if (!isLookingAtBlock(mc)) yield null;
                    BlockPos lp = ((BlockHitResult) mc.hitResult).getBlockPos();
                    yield ctx(best, null, null, lp, level.getBlockState(lp), new ChunkPos(lp), null);
                }
                case "HONEY_LEVEL" -> {
                    if (!isLookingAtBlock(mc)) yield null;
                    BlockPos lp = ((BlockHitResult) mc.hitResult).getBlockPos();
                    yield ctx(level, null, null, null, level.getBlockState(lp), null, null);
                }

                // Block entity
                case "BEE_COUNT", "COMPARATOR_OUTPUT", "FURNACE_XP", "FURNACE_EXP" -> {
                    if (!isLookingAtBlock(mc)) yield null;
                    BlockPos lp = ((BlockHitResult) mc.hitResult).getBlockPos();
                    BlockEntity be = best.getBlockEntity(lp);
                    if (be == null) yield null;
                    yield ctx(best, null, be, null, null, new ChunkPos(lp), beNbt(be, best));
                }

                // Looking-at entity
                case "LOOKING_AT_ENTITY", "ENTITY_VARIANT", "ENTITY_HOME_POS",
                     "ENTITY_REG_NAME", "ENTITY_REG", "LOOKING_AT_EFFECTS",
                     "LOOKING_AT_PLAYER_EXP", "ZOMBIE_CONVERSION",
                     "DOLPHIN_TREASURE", "PANDA_GENE" -> {
                    if (!isLookingAtEntity(mc)) yield null;
                    Entity target = ((EntityHitResult) mc.hitResult).getEntity();
                    yield ctx(level, target, null, null, null, null, entityNbt(target));
                }

                // Horse / vehicle
                case "HORSE_SPEED", "HORSE_JUMP", "HORSE_MAX_HEALTH" -> {
                    if (player == null || player.getVehicle() == null) yield null;
                    Entity v = player.getVehicle();
                    yield ctx(best, v, null, null, null, null, entityNbt(v));
                }

                default -> ctx(level, null, null, null, null, null, null);
            };
        } catch (Exception e) {
            LOG.error("[NiceHUD] buildCtx({}) failed: {}", name, e.getMessage());
            return null;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static Object ctx(Object world, Object ent, Object be,
                               Object pos, Object state, Object cp, Object nbt) {
        try { return ctxCtor7.newInstance(world, ent, be, pos, state, cp, nbt); }
        catch (Exception e) { LOG.error("[NiceHUD] ctx() failed: {}", e.getMessage()); return null; }
    }

    private static String buildText(Object entry) throws Exception {
        if ((Boolean) isTranslated.invoke(entry))
            return (String) formatMethod.invoke(entry);
        if ((Boolean) hasArgs.invoke(entry))
            return net.minecraft.client.resources.language.I18n.get(
                (String) formatMethod.invoke(entry), (Object[]) argsMethod.invoke(entry));
        return net.minecraft.client.resources.language.I18n.get(
            (String) formatMethod.invoke(entry));
    }

    private static boolean isLookingAtBlock(Minecraft mc) {
        return mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
    }
    private static boolean isLookingAtEntity(Minecraft mc) {
        return mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY;
    }

    /** Entity NBT via malilib's INbtEntityInvoker mixin. */
    private static CompoundTag entityNbt(Entity ent) {
        try {
            var invoker = (fi.dy.masa.malilib.util.nbt.INbtEntityInvoker) ent;
            var opt = invoker.malilib$getNbtDataWithId(0);
            if (opt != null && opt.isPresent() && opt.get() instanceof CompoundTag tag) return tag;
        } catch (Exception ignored) {}
        return null;
    }

    /** BlockEntity NBT via reflection (save API changed in 1.21.8). */
    private static CompoundTag beNbt(BlockEntity be, Level level) {
        CompoundTag tag = new CompoundTag();
        // 1-arg CompoundTag variants
        for (Method m : be.getClass().getMethods()) {
            String n = m.getName();
            if (m.getParameterCount() == 1
                    && m.getParameterTypes()[0].getSimpleName().contains("CompoundTag")
                    && (n.equals("saveWithoutMetadata") || n.equals("save")
                        || n.equals("serializeNBT") || n.equals("saveAdditional"))) {
                try { m.invoke(be, tag); if (!tag.isEmpty()) return tag; } catch (Exception ignored) {}
            }
        }
        // 2-arg CompoundTag + registry variants
        for (Method m : be.getClass().getMethods()) {
            if (m.getParameterCount() == 2
                    && m.getParameterTypes()[0].getSimpleName().contains("CompoundTag")) {
                try { m.invoke(be, tag, level.registryAccess()); if (!tag.isEmpty()) return tag; }
                catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static Object getToggle(String name) {
        try { return Class.forName("fi.dy.masa.minihud.config.InfoToggle").getField(name).get(null); }
        catch (Exception e) { return null; }
    }

    private static Object initParser(Object toggle) {
        try { return toggle.getClass().getMethod("initParser").invoke(toggle); }
        catch (Exception e) { return null; }
    }

    private static Level bestWorld(Minecraft mc) {
        MinecraftServer srv = mc.getSingleplayerServer();
        if (srv != null) {
            ServerLevel ow = srv.overworld();
            if (ow != null) return ow;
            if (mc.level != null) {
                ServerLevel dim = srv.getLevel(mc.level.dimension());
                if (dim != null) return dim;
            }
        }
        return mc.level;
    }
}
