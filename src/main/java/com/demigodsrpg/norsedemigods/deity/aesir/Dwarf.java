package com.demigodsrpg.norsedemigods.deity.aesir;

import com.demigodsrpg.norsedemigods.DMisc;
import com.demigodsrpg.norsedemigods.Deity;
import com.demigodsrpg.norsedemigods.Setting;
import com.demigodsrpg.norsedemigods.deity.AD;
import com.demigodsrpg.norsedemigods.saveable.PlayerDataSaveable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Dwarf implements Deity, Listener {
    private static final int SKILLCOST = 200;
    private static final int ULTIMATECOST = 9000;
    private static final int ULTIMATECOOLDOWNMAX = 1800; // seconds
    private static final int ULTIMATECOOLDOWNMIN = 900;

    private static final String skillname = "Reforge";
    private static final String ult = "Shatter";

    @Override
    public String getName() {
        return "Dwarf";
    }

    @Override
    public String getDefaultAlliance() {
        return "AEsir";
    }

    @Override
    public void printInfo(Player p) {
        if (DMisc.isFullParticipant(p) && DMisc.hasDeity(p, getName())) {
            int devotion = DMisc.getDevotion(p, getName());
            //
            int passiverange = (int) Math.round(20 * Math.pow(devotion, 0.15));
            int repairamt = (int) Math.ceil(10 * Math.pow(devotion, 0.09)); // percent
            int ultrange = (int) Math.ceil(15 * Math.pow(devotion, 0.09));
            int ultdamage = (int) Math.ceil(200 * Math.pow(devotion, 0.17));
            int t = (int) (ULTIMATECOOLDOWNMAX - ((ULTIMATECOOLDOWNMAX - ULTIMATECOOLDOWNMIN) * ((double) DMisc.getAscensions(p) / Setting.ASCENSION_CAP)));
            //
            p.sendMessage("--" + ChatColor.GOLD + getName() + ChatColor.GRAY + "[" + devotion + "]");
            p.sendMessage(":Furnaces up to " + passiverange + " blocks away produce double yields.");
            p.sendMessage(":Immune to fire damage.");
            p.sendMessage(":Repair the item in hand by up to " + repairamt + "% of its durability.");
            p.sendMessage(ChatColor.GREEN + " /reforge " + ChatColor.YELLOW + "Costs " + SKILLCOST + " Favor.");
            p.sendMessage(":Your dwarven blood cripples the durability of enemy weapons and armor.");
            p.sendMessage("Range: " + ultrange + " Damage: " + ultdamage + "" + ChatColor.GREEN + " /shatter");
            p.sendMessage(ChatColor.YELLOW + "Costs " + ULTIMATECOST + " Favor. Cooldown time: " + t + " seconds.");
            return;
        }
        p.sendMessage("--" + getName());
        p.sendMessage("Passive: Doubles the output of nearby furnaces.");
        p.sendMessage("Passive: Immune to fire damage.");
        p.sendMessage("Active: Repair the durability of an item in hand. " + ChatColor.GREEN + "/reforge");
        p.sendMessage(ChatColor.YELLOW + "Costs " + SKILLCOST + " Favor.");
        p.sendMessage("Ultimate: Your dwarven blood un-forges the weapons and armor of your");
        p.sendMessage("opponents. " + ChatColor.GREEN + "/shatter");
        p.sendMessage(ChatColor.YELLOW + "Select item: furnace");
    }

    @Override
    public void onEvent(Event ee) {
        // Nothing.
    }

    @Override
    public void onCommand(Player P, String str, String[] args, boolean bind) {
        if (DMisc.hasDeity(P, getName())) {
            if (str.equalsIgnoreCase(skillname)) {
                if (DMisc.getFavor(P) < SKILLCOST) {
                    P.sendMessage(ChatColor.YELLOW + "" + skillname + " requires " + SKILLCOST + " Favor to use.");
                    return;
                }
                int durability = P.getItemInHand().getDurability();
                if (durability == 0) {
                    P.sendMessage(ChatColor.YELLOW + "This item cannot be repaired.");
                    return;
                }
                double repairamt = Math.ceil(10 * Math.pow(DMisc.getDevotion(P, getName()), 0.09)) / 100;
                short num = (short) (P.getItemInHand().getDurability() * (1 - repairamt));
                P.sendMessage(ChatColor.RED + "Your dwarven powers" + ChatColor.WHITE + " have increased the item's durability by " + (P.getItemInHand().getDurability() - num) + ".");
                P.getItemInHand().setDurability(num);
                DMisc.setFavor(P, DMisc.getFavor(P) - SKILLCOST);
            } else if (str.equalsIgnoreCase(ult)) {
                PlayerDataSaveable save = getBackend().getPlayerDataRegistry().fromPlayer(P);
                double TIME = save.getAbilityData(ult, AD.TIME, (double) System.currentTimeMillis());
                if (System.currentTimeMillis() < TIME) {
                    P.sendMessage(ChatColor.YELLOW + "You cannot use " + ult + " again for " + ((((TIME) / 1000) - (System.currentTimeMillis() / 1000))) / 60 + " minutes");
                    P.sendMessage(ChatColor.YELLOW + "and " + ((((TIME) / 1000) - (System.currentTimeMillis() / 1000)) % 60) + " seconds.");
                    return;
                }
                if (DMisc.getFavor(P) >= ULTIMATECOST) {
                    if (!DMisc.canTarget(P, P.getLocation())) {
                        P.sendMessage(ChatColor.YELLOW + "You can't do that from a no-PVP zone.");
                        return;
                    }
                    int t = (int) (ULTIMATECOOLDOWNMAX - ((ULTIMATECOOLDOWNMAX - ULTIMATECOOLDOWNMIN) * ((double) DMisc.getAscensions(P) / Setting.ASCENSION_CAP)));
                    save.setAbilityData(ult, AD.TIME, System.currentTimeMillis() + (t * 1000));
                    int num = shatter(P);
                    if (num > 0) {
                        P.sendMessage(ChatColor.RED + "Your dwarven powers" + ChatColor.WHITE + " have un-forged the equipment of " + num + " enemy players.");
                        DMisc.setFavor(P, DMisc.getFavor(P) - ULTIMATECOST);
                    } else P.sendMessage(ChatColor.YELLOW + "No targets found.");
                } else P.sendMessage(ChatColor.YELLOW + "" + ult + " requires " + ULTIMATECOST + " Favor.");
            }
        }
    }

    @Override
    public void onSyncTick(long timeSent) {
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent e) {
        if (e.getBlock() == null) return;
        if (e.getBlock().getType() != Material.BLAST_FURNACE) return;
        for (UUID s : getPlayerIds()) {
            Player p = Bukkit.getPlayer(s);
            if ((p == null) || p.isDead()) continue;
            if (p.getLocation().getWorld().equals(e.getBlock().getLocation().getWorld())) {
                if (p.getLocation().distance(e.getBlock().getLocation()) < (int) Math.round(20 * Math.pow(DMisc.getDevotion(p, "Dwarf"), 0.15))) {
                    int amount = e.getResult().getAmount() * 2;
                    ItemStack out = e.getResult();
                    out.setAmount(amount);
                    e.setResult(out);
                    return;
                }
            }
        }
    }

    private int shatter(Player p) {
        int ultrange = (int) Math.ceil(15 * Math.pow(DMisc.getDevotion(p, getName()), 0.09));
        int ultdamage = (int) Math.ceil(200 * Math.pow(DMisc.getDevotion(p, getName()), 0.17));
        if (ultdamage > 2000) ultdamage = 2000;
        int i = 0;
        for (Player pl : p.getWorld().getPlayers()) {
            if (pl.getLocation().distance(p.getLocation()) <= ultrange) {
                if (!DMisc.canTarget(pl, pl.getLocation())) continue;
                if (DMisc.isFullParticipant(pl)) {
                    if (DMisc.getAllegiance(pl).equalsIgnoreCase(getDefaultAlliance())) {
                        i++;
                        pl.sendMessage(ChatColor.RED + "Dwarven powers" + ChatColor.WHITE + " have unforged your equipment.");
                        if (p.getItemInHand() != null) p.getItemInHand().setDurability((short) ultdamage);
                        for (ItemStack ii : pl.getInventory().getArmorContents())
                            if (ii != null) ii.setDurability((short) ultdamage);
                    }
                }
            }
        }
        return i;
    }

    @Override
    public boolean canTribute() {
        return false;
    }
}
