package clre20.customLootX.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class CustomGuiHolder implements InventoryHolder {

    protected Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public abstract void handleClick(InventoryClickEvent event);

    public void handleClose(InventoryCloseEvent event) {}

    public void handleDrag(InventoryDragEvent event) {}
}
