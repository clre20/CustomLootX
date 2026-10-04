package clre20.customLootX.gui.wizard.spawner;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.DraftSession;
import clre20.customLootX.model.DraftType;
import clre20.customLootX.model.SpawnerTemplate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 試煉生怪磚步驟精靈上下文 (支援草稿自動暫存與生命週期)
 */
public class SpawnerWizardContext {

    private final CustomLootX plugin;
    private final Player player;
    private final String originalName;
    private SpawnerTemplate template;
    private ItemStack itemInHand;
    private boolean editingExisting;

    private String draftId;
    private boolean isDraft;
    private boolean transitioning = false;
    private boolean savedSuccessfully = false;
    private boolean draftAbandoned = false;

    public SpawnerWizardContext(CustomLootX plugin, Player player, SpawnerTemplate template, ItemStack itemInHand, boolean editingExisting) {
        this(plugin, player, template, itemInHand, editingExisting, null, false);
    }

    public SpawnerWizardContext(CustomLootX plugin, Player player, SpawnerTemplate template, ItemStack itemInHand, boolean editingExisting, String draftId, boolean isDraft) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.itemInHand = itemInHand;
        this.editingExisting = editingExisting;
        this.originalName = (editingExisting && template != null) ? template.getName() : null;
        this.draftId = draftId;
        this.isDraft = isDraft;
    }

    public CustomLootX getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public SpawnerTemplate getTemplate() {
        return template;
    }

    public void setTemplate(SpawnerTemplate template) {
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

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public void setTransitioning(boolean transitioning) {
        this.transitioning = transitioning;
    }

    public boolean isSavedSuccessfully() {
        return savedSuccessfully;
    }

    public void setSavedSuccessfully(boolean savedSuccessfully) {
        this.savedSuccessfully = savedSuccessfully;
    }

    public boolean isDraftAbandoned() {
        return draftAbandoned;
    }

    public void setDraftAbandoned(boolean draftAbandoned) {
        this.draftAbandoned = draftAbandoned;
    }

    /**
     * 當使用者按 ESC / X 退出時，自動暫存手持物品
     */
    public void saveAsDraft() {
        if (savedSuccessfully || draftAbandoned) return;

        if (draftId == null || draftId.trim().isEmpty()) {
            draftId = plugin.getDraftManager().generateDraftId();
        }
        isDraft = true;

        DraftSession session = plugin.getDraftManager().saveDraft(DraftType.SPAWNER, draftId, originalName, template);
        if (session != null) {
            ItemStack held = itemInHand;
            if (held == null || held.getType().isAir()) {
                held = player.getInventory().getItemInMainHand();
            }
            if (plugin.getItemManager().isCustomSpawnerItem(held)) {
                String dName = template.getDisplayName();
                if (dName == null || dName.trim().isEmpty()) {
                    dName = template.getName();
                }
                plugin.getItemManager().applyDraft(held, DraftType.SPAWNER, draftId, originalName, session.getExpireTime(), dName);
            }
            plugin.getConfigManager().send(player, "draft-saved", "%days%", String.valueOf(plugin.getDraftManager().getExpireDays()));
            plugin.getConfigManager().playSound(player, "click");
        }
    }
}
