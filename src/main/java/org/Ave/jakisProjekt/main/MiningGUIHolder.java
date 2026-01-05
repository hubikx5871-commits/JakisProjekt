package org.Ave.jakisProjekt.main;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
public class MiningGUIHolder implements InventoryHolder {
    private Inventory inv;
    @Override public @NotNull Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}