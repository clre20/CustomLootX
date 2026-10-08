package clre20.customLootX.gui.wizard;

import clre20.customLootX.CustomLootX;
import clre20.customLootX.model.DraftSession;
import clre20.customLootX.model.DraftType;
import clre20.customLootX.model.LootTemplate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WizardContext {

    private final CustomLootX plugin;
    private final Player player;
    private final LootTemplate template;
    private final String originalName;
    private final boolean isNew;
    private ItemStack itemInHand;
    private String draftId;
    private boolean isDraft;

    private boolean transitioning = false;
    private boolean savedSuccessfully = false;
    private boolean draftAbandoned = false;

    public WizardContext(CustomLootX plugin, Player player, LootTemplate template, boolean isNew) {
        this(plugin, player, template, isNew, player.getInventory().getItemInMainHand(), null, false);
    }

    public WizardContext(CustomLootX plugin, Player player, LootTemplate template, boolean isNew, ItemStack itemInHand, String draftId, boolean isDraft) {
        this(plugin, player, template, isNew, itemInHand, isNew ? null : template.getName(), draftId, isDraft);
    }

    public WizardContext(CustomLootX plugin, Player player, LootTemplate template, boolean isNew, ItemStack itemInHand, String originalName, String draftId, boolean isDraft) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.originalName = (originalName != null && !originalName.trim().isEmpty())
                ? originalName
                : (isNew ? null : template.getName());
        this.isNew = isNew;
        this.itemInHand = itemInHand;
        this.draftId = draftId;
        this.isDraft = isDraft;
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

    public ItemStack getItemInHand() {
        return itemInHand;
    }

    public void setItemInHand(ItemStack itemInHand) {
        this.itemInHand = itemInHand;
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
     * 當使用者按 ESC / X 退出時，自動暫存手持物品或儲存草稿
     */
    public void saveAsDraft() {
        if (savedSuccessfully || draftAbandoned) return;

        if (draftId == null || draftId.trim().isEmpty()) {
            draftId = plugin.getDraftManager().generateDraftId();
        }
        isDraft = true;

        DraftSession session = plugin.getDraftManager().saveDraft(DraftType.SUSPICIOUS, draftId, originalName, template);
        if (session != null) {
            ItemStack held = itemInHand;
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (plugin.getItemManager().isCustomLootItem(mainHand)) {
                held = mainHand;
            } else if (held == null || held.getType().isAir()) {
                held = mainHand;
            }

            boolean itemUpdated = false;
            if (plugin.getItemManager().isCustomLootItem(held)) {
                String dName = template.getDisplayName();
                if (dName == null || dName.trim().isEmpty()) {
                    dName = template.getName();
                }
                plugin.getItemManager().applyDraft(held, DraftType.SUSPICIOUS, draftId, originalName, session.getExpireTime(), dName);
                if (plugin.getItemManager().isCustomLootItem(player.getInventory().getItemInMainHand())) {
                    player.getInventory().setItemInMainHand(held);
                } else if (plugin.getItemManager().isCustomLootItem(player.getInventory().getItemInOffHand())) {
                    player.getInventory().setItemInOffHand(held);
                }
                player.updateInventory();
                itemUpdated = true;
            }
            if (itemUpdated) {
                plugin.getConfigManager().send(player, "draft-saved", "%days%", String.valueOf(plugin.getDraftManager().getExpireDays()));
            } else {
                plugin.getConfigManager().send(player, "draft-saved-cmd", "%days%", String.valueOf(plugin.getDraftManager().getExpireDays()), "%name%", template.getName());
            }
            plugin.getConfigManager().playSound(player, "click");
        }
    }

    public void openStep1() {
        this.transitioning = true;
        new WizardStep1Gui(this).open();
        this.transitioning = false;
    }

    public void openStep2() {
        this.transitioning = true;
        new WizardStep2Gui(this).open();
        this.transitioning = false;
    }

    public void openStep3() {
        this.transitioning = true;
        new WizardStep3Gui(this).open();
        this.transitioning = false;
    }

    public void openStep4() {
        this.transitioning = true;
        new WizardStep4ConfirmGui(this).open();
        this.transitioning = false;
    }
}
