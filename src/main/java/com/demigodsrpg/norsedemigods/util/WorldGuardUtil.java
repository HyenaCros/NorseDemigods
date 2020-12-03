/*
 * Copyright 2014 Alex Bennett & Alexander Chauncey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.demigodsrpg.norsedemigods.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom flags will not require reflection in WorldGuard 6+, until then we'll use it.
 */
public class WorldGuardUtil implements Listener {
    private static boolean ENABLED;
    private static final ConcurrentMap<String, ProtoPVPListener> protoPVPListeners = Maps.newConcurrentMap();

    public WorldGuardUtil(final Plugin plugin) {
        final WorldGuardUtil th = this;
        try {
            ENABLED = Bukkit.getPluginManager().getPlugin("WorldGuard") instanceof WorldGuardPlugin;
        } catch (Exception error) {
            ENABLED = false;
        }

        if (plugin.isEnabled()) {
            Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin,
                    () -> Bukkit.getPluginManager().registerEvents(th, plugin), 40);
        }
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, () -> {
                // process proto-listeners
                Iterator<ProtoPVPListener> protoPVPListenerIterator = protoPVPListeners.values().iterator();
                while (worldGuardEnabled() && protoPVPListenerIterator.hasNext()) {
                    ProtoPVPListener queued = protoPVPListenerIterator.next();
                    queued.register();
                    protoPVPListeners.remove(queued.plugin.getName());
                }
            }, 0, 5);
        }
    }

    /**
     * @return WorldGuard is enabled.
     */
    public static boolean worldGuardEnabled() {
        return ENABLED;
    }

    /**
     * @param id The name of a WorldGuard flag.
     * @deprecated If you don't have WorldGuard installed this will error.
     */
    @Deprecated
    public static Flag<?> getFlag(String id) {
        return Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), id);
    }

    /**
     * Check that a ProtectedRegion exists at a Location.
     *
     * @param name     The name of the region.
     * @param location The location being checked.
     * @return The region does exist at the provided location.
     */
    public static boolean checkForRegion(final String name, Location location) {
        return Iterators
                .any(WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(location.getWorld()))
                        .getApplicableRegions(BukkitAdapter.asBlockVector(location))
                        .iterator(), region -> region.getId().toLowerCase().contains(name));
    }

    /**
     * Check for a flag at a given location.
     *
     * @param flag     The flag being checked.
     * @param location The location being checked.
     * @return The flag does exist at the provided location.
     */
    public static boolean checkForFlag(final Flag flag, Location location) {
        return Iterators
                .any(WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(location.getWorld()))
                        .getApplicableRegions(BukkitAdapter.asBlockVector(location))
                        .iterator(), region -> {
                    try {
                        return region.getFlags().containsKey(flag);
                    } catch (Exception ignored) {
                    }
                    return false;
                });
    }

    /**
     * Check if a StateFlag is enabled at a given location.
     *
     * @param flag     The flag being checked.
     * @param location The location being checked.
     * @return The flag is enabled.
     */
    public static boolean checkStateFlagAllows(final StateFlag flag, Location location, RegionAssociable associable) {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testState(BukkitAdapter.adapt(location), associable, flag);
    }

    /**
     * Check if a StateFlag is enabled at a given location.
     *
     * @param flag     The flag being checked.
     * @param location The location being checked.
     * @return The flag is enabled.
     */
    public static boolean checkStateFlagAllows(final StateFlag flag, Location location, Player player) {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testState(BukkitAdapter.adapt(location), WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

    /**
     * Check if a StateFlag is enabled at a given location.
     *
     * @param flag     The flag being checked.
     * @param location The location being checked.
     * @return The flag is enabled.
     * @Deprecated
     */
    @Deprecated
    public static boolean checkStateFlagAllows(final StateFlag flag, Location location) {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testState(BukkitAdapter.adapt(location), null, flag);
    }

    /**
     * Check for a flag-value at a given location.
     *
     * @param flag     The flag being checked.
     * @param value    The value (marshalled) as a String.
     * @param location The location being checked.
     * @return The flag-value does exist at the provided location.
     */
    public static boolean checkForFlagValue(final Flag flag, final String value, Location location) {
        return Iterators
                .any(WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(location.getWorld()))
                        .getApplicableRegions(BukkitAdapter.asBlockVector(location))
                        .iterator(), region -> {
                    try {
                        return flag.marshal(region.getFlag(flag)).equals(value);
                    } catch (Exception ignored) {
                    }
                    return false;
                });
    }

    /**
     * @param player   Given player.
     * @param location Given location.
     * @return The player can build here.
     */
    public static boolean canBuild(Player player, Location location) {
        return checkStateFlagAllows(Flags.BUILD, location, player);
    }

    /**
     * @param location Given location.
     * @return PVP is allowed here.
     */
    public static boolean canPVP(Player player, Location location) {
        return checkStateFlagAllows(Flags.PVP, location, player);
    }

    @Deprecated
    public static boolean canPVP(Location location) {
        return checkStateFlagAllows(Flags.PVP, location);
    }

    public static void setWhenToOverridePVP(Plugin plugin, Predicate<Event> checkPVP) {
        if (!worldGuardEnabled()) {
            protoPVPListeners.put(plugin.getName(), new ProtoPVPListener(plugin, checkPVP));
        } else {
            new WorldGuardPVPListener(plugin, checkPVP);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPluginEnable(PluginEnableEvent event) {
        if (ENABLED || !event.getPlugin().getName().equals("WorldGuard")) return;
        try {
            ENABLED = event.getPlugin() instanceof WorldGuardPlugin;
        } catch (Exception ignored) {
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPluginDisable(PluginDisableEvent event) {
        if (!ENABLED || event.getPlugin().getName().equals("WorldGuard")) return;
        try {
            ENABLED = false;
        } catch (Exception ignored) {
        }
    }

    static class ProtoPVPListener {
        private final Plugin plugin;
        private final Predicate<Event> checkPVP;

        ProtoPVPListener(Plugin plugin, Predicate<Event> checkPVP) {
            this.plugin = plugin;
            this.checkPVP = checkPVP;
        }

        void register() {
            new WorldGuardPVPListener(plugin, checkPVP);
        }
    }

    public static class WorldGuardPVPListener implements Listener {
        private final Predicate<Event> checkPVP;

        WorldGuardPVPListener(Plugin plugin, Predicate<Event> checkPVP) {
            this.checkPVP = checkPVP;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        @EventHandler(priority = EventPriority.LOWEST)
        void onDisallowedPVP(DisallowedPVPEvent event) {
            if (checkPVP.apply(event.getCause())) event.setCancelled(true);
        }
    }
}