package fi.dy.masa.nicehud.config;

/** Persisted config for one HUD element. */
public class HudElement {
    public boolean visible     = true;
    public int     x           = 5;
    public int     y           = 5;
    public float   scale       = 1.0f;
    public String  group       = "";        // empty = no group
    public boolean vertical    = false;     // for inventory/armor widgets

    public HudElement() {}
    public HudElement(boolean visible, int x, int y) {
        this.visible = visible; this.x = x; this.y = y;
    }
}
