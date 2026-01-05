package org.Ave.jakisProjekt.commands;

import org.Ave.jakisProjekt.main.GornikSuno;
import org.Ave.jakisProjekt.main.MiningGUIHolder;
import org.Ave.jakisProjekt.main.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class PointsCommand extends Command {
    private final GornikSuno plugin;

    public PointsCommand(GornikSuno plugin, String name) {
        super(name);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            openMiningGUI(p);
        }
        return true;
    }

    public void openMiningGUI(Player p) {
        PlayerData data = plugin.getPlayerData(p.getUniqueId());
        MiningGUIHolder holder = new MiningGUIHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, plugin.getMM().deserialize(plugin.getConfig().getString("gui.title")));
        holder.setInventory(inv);

        updateInventoryItems(inv, data);

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.gui-open")), 1.0f, 1.0f);
    }

    public void updateInventoryItems(Inventory inv, PlayerData data) {
        int xpL = plugin.getConfig().getInt("settings.xp-per-level", 1000);
        inv.setItem(11, createItem(data, "gui.stats-item", "gui.stats-name", "gui.stats-lore", xpL));
        inv.setItem(13, createItem(data, "gui.speed-item", "gui.speed-name", "gui.speed-lore", xpL));
        inv.setItem(15, createItem(data, "gui.prestige-item", "gui.prestige-name", "gui.prestige-lore", xpL));
    }

    private ItemStack createItem(PlayerData d, String matP, String nameP, String loreP, int xpL) {
        ItemStack i = new ItemStack(Material.matchMaterial(plugin.getConfig().getString(matP, "STONE")));
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.displayName(plugin.getMM().deserialize(plugin.getConfig().getString(nameP)));
            m.lore(plugin.getConfig().getStringList(loreP).stream().map(s -> plugin.getMM().deserialize(s
                    .replace("<level>", String.valueOf(d.xp / xpL)).replace("<xp>", String.valueOf(d.xp % xpL))
                    .replace("<maxxp>", String.valueOf(xpL)).replace("<prestige>", String.valueOf(d.prestige))
                    .replace("<speed_level>", String.valueOf(d.speedLevel)).replace("<bonus>", String.valueOf(d.speedLevel * 10))
                    .replace("<cost>", String.valueOf(plugin.getConfig().getInt("settings.speed-upgrade-cost")))
            )).toList());
            i.setItemMeta(m);
        }
        return i;
    }

    public static void applySpeed(Player p, int lvl, GornikSuno pl) {
        AttributeInstance ai = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (ai == null) return;
        ai.removeModifier(pl.getSpeedKey());
        if (lvl > 0) {
            double multiplier = pl.getConfig().getDouble("settings.speed-multiplier-per-level", 0.1);
            ai.addModifier(new AttributeModifier(pl.getSpeedKey(), lvl * multiplier, AttributeModifier.Operation.ADD_NUMBER));
        }
    }
}