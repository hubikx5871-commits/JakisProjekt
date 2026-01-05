package org.Ave.jakisProjekt.commands;

import org.Ave.jakisProjekt.main.GornikSuno;
import org.Ave.jakisProjekt.main.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class AdminCommand extends Command {
    private final GornikSuno plugin;

    public AdminCommand(GornikSuno plugin) {
        super("gr");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("gornik.admin")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMM().deserialize(plugin.getConfig().getString("messages.admin-usage")));
            return true;
        }

        Player t = Bukkit.getPlayer(args[1]);
        if (t == null) {
            sender.sendMessage(plugin.getMessage("messages.player-not-found").replaceText(b -> b.match("<target>").replacement(args[1])));
            return true;
        }

        PlayerData d = plugin.getPlayerData(t.getUniqueId());
        int xpL = plugin.getConfig().getInt("settings.xp-per-level", 1000);

        if (args[2].equalsIgnoreCase("sprawdz")) {
            sender.sendMessage(plugin.getMM().deserialize(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.admin-check")
                    .replace("<target>", t.getName()).replace("<xp>", String.valueOf(d.xp)).replace("<level>", String.valueOf(d.xp / xpL))
                    .replace("<prestige>", String.valueOf(d.prestige)).replace("<speed>", String.valueOf(d.speedLevel))));
            return true;
        }

        if (args.length < 4) return true;

        try {
            int val = Integer.parseInt(args[3]);
            int change = args[0].equalsIgnoreCase("lvl") ? val * xpL : val;
            String type = args[0].toUpperCase();

            if (args[2].equalsIgnoreCase("daj")) {
                d.xp += change;
                sender.sendMessage(formatMsg("messages.admin-gave", t.getName(), sender.getName(), val, type));
                t.sendMessage(formatMsg("messages.player-received", t.getName(), sender.getName(), val, type));
                t.playSound(t.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.admin-give")), 1.0f, 1.0f);
                t.getWorld().spawnParticle(Particle.valueOf(plugin.getConfig().getString("particles.admin-give")), t.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            } else if (args[2].equalsIgnoreCase("usun")) {
                d.xp = Math.max(0, d.xp - change);
                sender.sendMessage(formatMsg("messages.admin-removed", t.getName(), sender.getName(), val, type));
                t.sendMessage(formatMsg("messages.player-lost", t.getName(), sender.getName(), val, type));
            }

            PointsCommand.applySpeed(t, d.speedLevel, plugin);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMM().deserialize("<red>Blad: Wartosc musi byc liczba!</red>"));
        }
        return true;
    }

    private net.kyori.adventure.text.Component formatMsg(String p, String t, String a, int v, String type) {
        String msg = plugin.getConfig().getString(p, "").replace("<target>", t).replace("<admin>", a).replace("<amount>", String.valueOf(v)).replace("<type>", type);
        return plugin.getMM().deserialize(plugin.getConfig().getString("messages.prefix", "") + msg);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender s, @NotNull String a, String[] args) {
        if (args.length == 1) return List.of("xp", "lvl");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 3) return List.of("daj", "usun", "sprawdz");
        return List.of();
    }
}