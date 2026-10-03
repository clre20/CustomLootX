package clre20.customLootX.gui.wizard;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.LootTemplate;
import org.bukkit.entity.Player;

public class WizardContext {

    private final CustomLootX plugin;
    private final Player player;
    private final LootTemplate template;
    private final String originalName;
    private final boolean isNew;

    public WizardContext(CustomLootX plugin, Player player, LootTemplate template, boolean isNew) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.originalName = isNew ? null : template.getName();
        this.isNew = isNew;
    }

    public CustomLootX getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public LootTemplate getTemplate() {
        return template;
    }

    public String getOriginalName() {
        return originalName;
    }

    public boolean isNew() {
        return isNew;
    }

    public void openStep1() {
        new WizardStep1Gui(this).open();
    }

    public void openStep2() {
        new WizardStep2Gui(this).open();
    }

    public void openStep3() {
        new WizardStep3Gui(this).open();
    }

    public void openStep4() {
        new WizardStep4ConfirmGui(this).open();
    }
}
