package fi.dy.masa.nicehud.init;

import fi.dy.masa.nicehud.config.NiceHudConfig;
import fi.dy.masa.nicehud.hud.HudRenderer;
import fi.dy.masa.nicehud.screen.EditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class NiceHUDInit implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("nicehud");

    private static NiceHudConfig config;
    private static HudRenderer   renderer;
    private static KeyMapping    editorKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[NiceHUD] init");

        config   = NiceHudConfig.load();
        renderer = new HudRenderer(config);

        registerHudHook();
        registerKeybinding();
        registerLifecycle();

        LOGGER.info("[NiceHUD] ready — press H to open/close editor");
    }

    // ── HUD render hook (via fabric-api HudElementRegistry) ──────────────────

    private void registerHudHook() {
        try {
            Class<?> reg     = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry");
            Class<?> vanilla = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements");
            Method   attach  = null;
            Class<?> layerIf = null;
            for (Method m : reg.getMethods())
                if (m.getName().equals("attachElementBefore") && m.getParameterCount() == 3)
                    { attach = m; layerIf = m.getParameterTypes()[2]; break; }
            if (attach == null) { LOGGER.error("[NiceHUD] attachElementBefore not found"); return; }

            Object hotbar = vanilla.getField("HOTBAR").get(null);
            Object id     = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nicehud", "widgets");
            final Class<?> li = layerIf;

            Object layer = java.lang.reflect.Proxy.newProxyInstance(
                li.getClassLoader(), new Class<?>[]{ li },
                (proxy, method, args) -> {
                    if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                    if (method.getDeclaringClass() == Object.class) return null;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.options.hideGui) return null;
                    if (mc.screen instanceof EditorScreen) return null;
                    // args[0] = DrawContext/GuiGraphics — same class at runtime
                    renderer.render((net.minecraft.client.gui.GuiGraphics) args[0]);
                    return null;
                });
            attach.invoke(null, hotbar, id, layer);
            LOGGER.info("[NiceHUD] HUD hook registered");
        } catch (Exception e) {
            LOGGER.error("[NiceHUD] HUD hook failed: {}", e.getMessage());
        }
    }

    // ── Keybinding (H) ────────────────────────────────────────────────────────

    private void registerKeybinding() {
        try {
            editorKey = new KeyMapping("key.nicehud.toggle_editor",
                GLFW.GLFW_KEY_H, "key.categories.nicehud");

            Class<?> helper = Class.forName("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper");
            for (Method m : helper.getMethods())
                if (m.getName().equals("registerKeyBinding") && m.getParameterCount() == 1)
                    { editorKey = (KeyMapping) m.invoke(null, editorKey); break; }

            // Tick listener
            Class<?> endTickIface = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents$EndTick");
            Class<?> tickClass    = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
            Object   endTickEvent = tickClass.getField("END_CLIENT_TICK").get(null);
            Class<?> eventApi     = Class.forName("net.fabricmc.fabric.api.event.Event");
            Method   register     = eventApi.getMethod("register", Object.class);
            final KeyMapping key  = editorKey;

            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                endTickIface.getClassLoader(), new Class<?>[]{ endTickIface },
                (proxy, method, args) -> {
                    if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                    if (method.getDeclaringClass() == Object.class) return null;
                    Minecraft mc = (Minecraft) args[0];
                    while (key.consumeClick()) {
                        if (mc.screen instanceof EditorScreen) {
                            mc.setScreen(null); // close = save handled by onClose
                        } else {
                            mc.setScreen(new EditorScreen(config, cfg -> {
                                cfg.save();
                                renderer.reload(cfg);
                                LOGGER.info("[NiceHUD] config saved");
                            }));
                        }
                    }
                    return null;
                });
            register.invoke(endTickEvent, listener);
            LOGGER.info("[NiceHUD] keybinding H registered");
        } catch (Exception e) {
            LOGGER.error("[NiceHUD] keybinding failed: {}", e.getMessage(), e);
        }
    }

    // ── Save on exit ──────────────────────────────────────────────────────────

    private void registerLifecycle() {
        try {
            Class<?> stoppingIface = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents$ClientStopping");
            Class<?> lcClass       = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents");
            Object   stoppingEvent = lcClass.getField("CLIENT_STOPPING").get(null);
            Class<?> eventApi      = Class.forName("net.fabricmc.fabric.api.event.Event");
            Method   register      = eventApi.getMethod("register", Object.class);
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                stoppingIface.getClassLoader(), new Class<?>[]{ stoppingIface },
                (proxy, method, args) -> {
                    if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                    if (method.getDeclaringClass() == Object.class) return null;
                    config.save();
                    return null;
                });
            register.invoke(stoppingEvent, listener);
        } catch (Exception e) {
            LOGGER.warn("[NiceHUD] lifecycle failed: {}", e.getMessage());
        }
    }
}
