package com.demigodsrpg.norsedemigods.deity.jotunn;

import com.demigodsrpg.norsedemigods.DMisc;
import com.demigodsrpg.norsedemigods.Deity;
import com.demigodsrpg.norsedemigods.deity.AD;
import com.demigodsrpg.norsedemigods.saveable.PlayerDataSaveable;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class Dis implements Deity {
    private static final int SKILLCOST = 310;
    private static final int SKILLDELAY = 2400; // milliseconds
    private static final int ULTIMATECOST = 6000;
    private static final int ULTIMATECOOLDOWNMAX = 1200; // seconds
    private static final int ULTIMATECOOLDOWNMIN = 500;

    private static final String ult = "Congregate";

    @Override
    public String getName() {
        return "Dis";
    }

    @Override
    public String getDefaultAlliance() {
        return "Jotunn";
    }

    @Override
    public void printInfo(Player p) {
        if (DMisc.isFullParticipant(p) && DMisc.hasDeity(p, getName())) {
            PlayerDataSaveable save = getBackend().getPlayerDataRegistry().fromPlayer(p);
            int devotion = DMisc.getDevotion(p, getName());
            int t = (int) (ULTIMATECOOLDOWNMAX - ((ULTIMATECOOLDOWNMAX - ULTIMATECOOLDOWNMIN) * ((double) DMisc.getAscensions(p) / 100)));
            p.sendMessage("--" + ChatColor.GOLD + getName() + ChatColor.GRAY + "[" + devotion + "]");
            p.sendMessage(":Use " + ChatColor.YELLOW + "qd <name>" + ChatColor.WHITE + " for detailed information about any player");
            p.sendMessage(":Fly at half speed while it is night time.");
            p.sendMessage(":Call all AEsir and Jotunn together for an assembly at your location.");
            p.sendMessage("Players will be temporarily immune to damage after teleporting.");
            p.sendMessage("Only consenting players will be teleported. " + ChatColor.GREEN + "/congregate");
            p.sendMessage(ChatColor.YELLOW + "Costs " + ULTIMATECOST + " Favor. Cooldown time: " + t + " seconds.");
            return;
        }
        p.sendMessage("--" + getName());
        p.sendMessage("Passive: " + ChatColor.YELLOW + "qd" + ChatColor.WHITE + " gives more detail on targets.");
        p.sendMessage("Passive: Ability to fly at night.");
        p.sendMessage("Ultimate: Your dís calls together all AEsir and Jotunn to you.");
        p.sendMessage("Requires other players' consent." + ChatColor.GREEN + "/congregate " + ChatColor.YELLOW + "Costs " + ULTIMATECOST + " Favor. Has cooldown.");
        p.sendMessage(ChatColor.YELLOW + "Select item: compass");
    }

    @Override
    public void onEvent(Event ee) {
        if (ee instanceof PlayerMoveEvent) {
            PlayerMoveEvent move = (PlayerMoveEvent) ee;
            Player p = move.getPlayer();
            if (!DMisc.isFullParticipant(p)) return;
            if (!DMisc.hasDeity(p, "Dis")) return;
            if (p.getGameMode() != GameMode.CREATIVE) {
                if (p.getWorld().getTime() > 12500) {
                    if (!p.getAllowFlight()) {
                        p.setFlySpeed(0.05f);
                        p.setAllowFlight(true);
                    }
                }
                else if (p.getAllowFlight()) {
                    if (p.isFlying()) {
                        p.setFlying(false);
                    }
                    p.setAllowFlight(false);
                    p.setFlySpeed(1);
                }
            }
        }
    }

    @Override
    public void onCommand(Player P, String str, String[] args, boolean bind) {
        if (DMisc.hasDeity(P, getName())) {
            PlayerDataSaveable save = getBackend().getPlayerDataRegistry().fromPlayer(P);
            if (str.equalsIgnoreCase(ult)) {
                double TIME = save.getAbilityData(ult, AD.TIME, (double) System.currentTimeMillis());
                if (System.currentTimeMillis() < TIME) {
                    P.sendMessage(ChatColor.YELLOW + "You cannot use " + ult + " again for " + ((((TIME) / 1000) - (System.currentTimeMillis() / 1000))) / 60 + " minutes");
                    P.sendMessage(ChatColor.YELLOW + "and " + ((((TIME) / 1000) - (System.currentTimeMillis() / 1000)) % 60) + " seconds.");
                    return;
                }
                if (DMisc.getFavor(P) >= ULTIMATECOST) {
                    for (Player pl : P.getWorld().getPlayers()) {
                        if (DMisc.isFullParticipant(pl) && DMisc.getActiveEffectsList(pl.getUniqueId()).contains("Congregate")) {
                            P.sendMessage(ChatColor.YELLOW + "Congregate is already in effect.");
                            return;
                        }
                    }
                    int t = (int) (ULTIMATECOOLDOWNMAX - ((ULTIMATECOOLDOWNMAX - ULTIMATECOOLDOWNMIN) * ((double) DMisc.getAscensions(P) / 100)));
                    save.setAbilityData(ult, AD.TIME, System.currentTimeMillis() + (t * 1000));
                    int n = congregate(P);
                    if (n > 0) {
                        P.sendMessage(ChatColor.GOLD + "A dís has called upon " + n + " players to assemble at your location.");
                        DMisc.setFavor(P, DMisc.getFavor(P) - ULTIMATECOST);
                    } else P.sendMessage(ChatColor.YELLOW + "There are no players to assemble.");
                } else P.sendMessage(ChatColor.YELLOW + "" + ult + " requires " + ULTIMATECOST + " Favor.");
            }
        }
    }

    @Override
    public void onSyncTick(long timeSent) {
    }

    private int congregate(Player p) {
        DMisc.addActiveEffect(p.getUniqueId(), "Congregate Call", 60);
        int count = 0;
        for (Player pl : p.getWorld().getPlayers()) {
            if (DMisc.isFullParticipant(pl)) {
                count++;
                if (!p.equals(pl) && !DMisc.getActiveEffectsList(pl.getUniqueId()).contains("Congregate")) {
                    pl.sendMessage(ChatColor.GOLD + "A dís has called for an assembly of deities at " + p.getName() + "'s location.");
                    pl.sendMessage(ChatColor.GOLD + "Type " + ChatColor.WHITE + "/assemble" + ChatColor.GOLD + " to be teleported.");
                    pl.sendMessage(ChatColor.GOLD + "You will be immune to damage upon arrival for a short time.");
                    pl.sendMessage(ChatColor.GRAY + "You have one minute to answer the invitation.");
                    pl.sendMessage(ChatColor.GRAY + "To see how much time is left to respond, use " + ChatColor.WHITE + "qd" + ChatColor.GRAY + ".");
                    DMisc.addActiveEffect(pl.getUniqueId(), "Congregate", 60);
                }
            }
        }
        return count;
    }

    @Override
    public boolean canTribute() {
        return false;
    }
}
