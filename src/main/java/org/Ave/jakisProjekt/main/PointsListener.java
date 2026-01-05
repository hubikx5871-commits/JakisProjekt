package org.Ave.jakisProjekt.main;

import net.kyori.adventure.title.Title;
import org.Ave.jakisProjekt.commands.PointsCommand;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PointsListener implements Listener {
    private final GornikSuno plugin;

    public PointsListener(GornikSuno plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerData data = plugin.getPlayerData(e.getPlayer().getUniqueId());
        PointsCommand.applySpeed(e.getPlayer(), data.speedLevel, plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.removePlayerData(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        String block = e.getBlock().getType().name();
        if (plugin.getConfig().contains("settings.blocks." + block)) {
            int base = plugin.getConfig().getInt("settings.blocks." + block);
            Player p = e.getPlayer();
            PlayerData data = plugin.getPlayerData(p.getUniqueId());

            int xpL = plugin.getConfig().getInt("settings.xp-per-level", 1000);
            int oldLvl = data.xp / xpL;
            int earned = (int) (base * (1 + (data.prestige * 0.2)));
            data.xp += earned;
            int newLvl = data.xp / xpL;

            p.sendActionBar(plugin.getMM().deserialize(plugin.getConfig().getString("messages.actionbar-xp").replace("<amount>", String.valueOf(earned))));
            p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.xp-gain")), 0.5f, 1.2f);

            if (newLvl > oldLvl) {
                p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.levelup")), 1.0f, 1.0f);
                p.getWorld().spawnParticle(Particle.valueOf(plugin.getConfig().getString("particles.levelup")), p.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.1);

                Title title = Title.title(
                        plugin.getMM().deserialize(plugin.getConfig().getString("messages.levelup-title")),
                        plugin.getMM().deserialize(plugin.getConfig().getString("messages.levelup-subtitle").replace("<level>", String.valueOf(newLvl)))
                );
                p.showTitle(title);
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MiningGUIHolder holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory().getHolder() instanceof Player) return;

        Player p = (Player) e.getWhoClicked();
        PlayerData data = plugin.getPlayerData(p.getUniqueId());

        if (e.getRawSlot() == 13) handleUpgrade(p, data, holder);
        else if (e.getRawSlot() == 15) handlePrestige(p, data, holder);
    }

    private void handleUpgrade(Player p, PlayerData data, MiningGUIHolder holder) {
        int cost = plugin.getConfig().getInt("settings.speed-upgrade-cost");
        if (data.speedLevel >= 5) {
            p.sendMessage(plugin.getMessage("messages.max-speed-reached"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        } else if (data.xp < cost) {
            p.sendMessage(plugin.getMessage("messages.not-enough-xp").replaceText(b -> b.match("<amount>").replacement(String.valueOf(cost - data.xp))));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        } else {
            data.xp -= cost;
            data.speedLevel++;
            p.sendMessage(plugin.getMessage("messages.speed-upgraded").replaceText(b -> b.match("<level>").replacement(String.valueOf(data.speedLevel))));
            p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.upgrade-buy")), 1.0f, 1.2f);
            p.getWorld().spawnParticle(Particle.valueOf(plugin.getConfig().getString("particles.upgrade")), p.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.1);
            PointsCommand.applySpeed(p, data.speedLevel, plugin);
            plugin.getPointsCommand().updateInventoryItems(holder.getInventory(), data);
        }
    }

    private void handlePrestige(Player p, PlayerData data, MiningGUIHolder holder) {
        int xpL = plugin.getConfig().getInt("settings.xp-per-level", 1000);
        if (data.xp / xpL < 10) {
            p.sendMessage(plugin.getMessage("messages.not-enough-lvl-prestige"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        } else {
            data.xp = 0;
            data.prestige++;
            p.sendMessage(plugin.getMessage("messages.prestige-success"));
            p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.prestige")), 1.0f, 0.8f);
            p.getWorld().spawnParticle(Particle.valueOf(plugin.getConfig().getString("particles.prestige")), p.getLocation().add(0, 1, 0), 100, 0.6, 1.0, 0.6, 0.2);
            plugin.getPointsCommand().updateInventoryItems(holder.getInventory(), data);
        }
    }
}