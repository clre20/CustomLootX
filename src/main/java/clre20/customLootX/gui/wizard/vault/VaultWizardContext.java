package clre20.customLootX.gui.wizard.vault;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.VaultTemplate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 試煉寶庫步驟精靈上下文
 */
public class VaultWizardContext {

    private final CustomLootX plugin;
    private final Player player;
    private final String originalName;
    private VaultTemplate template;
    private ItemStack itemInHand;
    private boolean editingExisting;

    public VaultWizardContext(CustomLootX plugin, Player player, VaultTemplate template, ItemStack itemInHand, boolean editingExisting) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.itemInHand = itemInHand;
        this.editingExisting = editingExisting;
        this.originalName = (editingExisting && template != null) ? template.getName() : null;
    }

    public CustomLootX getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public VaultTemplate getTemplate() {
        return template;
    }

    public void setTemplate(VaultTemplate template) {
        this.template = template;
    }

    public ItemStack getItemInHand() {
        return itemInHand;
    }

    public void setItemInHand(ItemStack itemInHand) {
        this.itemInHand = itemInHand;
    }

    public boolean isEditingExisting() {
        return editingExisting;
    }

    public void setEditingExisting(boolean editingExisting) {
        this.editingExisting = editingExisting;
    }

    public String getOriginalName() {
        return originalName;
    }
}
