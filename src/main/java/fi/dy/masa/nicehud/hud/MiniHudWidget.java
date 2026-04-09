package fi.dy.masa.nicehud.hud;

import fi.dy.masa.nicehud.config.HudElement;

/**
 * A HUD widget backed by MiniHUD's InfoLine system.
 * The InfoLine output already contains its own label, so we skip the "label: " prefix.
 */
public class MiniHudWidget extends HudWidget {

    private final String toggleName;

    public MiniHudWidget(HudElement element, String label, String toggleName) {
        super(element, label);
        this.toggleName    = toggleName;
        this.showLabelInHud = false;
    }

    @Override
    public String getValue() { return MiniHudSource.getData(toggleName); }
}
