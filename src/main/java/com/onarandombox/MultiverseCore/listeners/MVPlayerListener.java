package com.onarandombox.MultiverseCore.listeners;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.fernferret.allpay.GenericBank;
import com.onarandombox.MultiverseCore.MVTeleport;
import com.onarandombox.MultiverseCore.MVWorld;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.event.MVRespawnEvent;
import com.onarandombox.utils.WorldManager;

public class MVPlayerListener extends PlayerListener {
    MultiverseCore plugin;
    MVTeleport mvteleporter;
    WorldManager worldManager;

    public MVPlayerListener(MultiverseCore plugin) {
        this.plugin = plugin;
        worldManager = plugin.getWorldManager();
    }

    // Taken out until we do persistance.
    // @Override
    // public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
    // Location bedLoc = event.getBed().getLocation();
    // bedLoc = this.plugin.getTeleporter().getSafeBedDestination(bedLoc);
    // this.plugin.getPlayerSession(event.getPlayer()).setRespawnLocation(bedLoc);
    // event.getPlayer().sendMessage("You should come back here when you type '/mv sleep'!");
    // }

    @Override
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        // Check whether the Server is set to prefix the chat with the World name. If not we do nothing, if so we need to check if the World has an Alias.
        if (this.plugin.getConfig().getBoolean("worldnameprefix", true)) {
            this.plugin.getConfig().save();
            String world = event.getPlayer().getWorld().getName();
            String prefix = "";
            // If we're not a MV world, don't do anything
            if (!this.worldManager.isMVWorld(world)) {
                return;
            }
            MVWorld mvworld = this.worldManager.getMVWorld(world);
            prefix = mvworld.getColoredWorldString();
            String format = event.getFormat();
            event.setFormat("[" + prefix + "]" + format);
        }
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        World world = event.getPlayer().getWorld();

        // If it's not a World MV manages we stop.
        if (!this.worldManager.isMVWorld(world.getName())) {
            return;
        }

        if (event.isBedSpawn() && this.plugin.getConfig().getBoolean("bedrespawn", true)) {
            // Handle the Players GameMode setting for the new world.
            if (this.plugin.getConfig().getBoolean("enforcegamemodes", true)) {
                this.handleGameMode(event.getPlayer(), event.getRespawnLocation().getWorld());
            }
            this.plugin.log(Level.FINE, "Spawning " + event.getPlayer().getName() + " at their bed");
            return;
        }

        // Get the MVWorld
        MVWorld mvWorld = this.worldManager.getMVWorld(world.getName());
        // Get the instance of the World the player should respawn at.
        MVWorld respawnWorld = null;
        if (this.worldManager.isMVWorld(mvWorld.getRespawnToWorld())) {
            respawnWorld = this.worldManager.getMVWorld(mvWorld.getRespawnToWorld());
        }

        // If it's null then it either means the World doesn't exist or the value is blank, so we don't handle it.
        // NOW: We'll always handle it to get more accurate spawns
        if (respawnWorld != null) {
            world = respawnWorld.getCBWorld();
        }
        // World has been set to the appropriate world
        Location respawnLocation = getMostAccurateRespawnLocation(world);

        MVRespawnEvent respawnEvent = new MVRespawnEvent(respawnLocation, event.getPlayer(), "compatability");
        this.plugin.getServer().getPluginManager().callEvent(respawnEvent);
        event.setRespawnLocation(respawnEvent.getPlayersRespawnLocation());

        // Handle the Players GameMode setting for the new world.
        if (this.plugin.getConfig().getBoolean("enforcegamemodes", true)) {
            this.handleGameMode(event.getPlayer(), respawnEvent.getPlayersRespawnLocation().getWorld());
        }
    }

    private Location getMostAccurateRespawnLocation(World w) {
        MVWorld mvw = this.worldManager.getMVWorld(w.getName());
        if (mvw != null) {
            return mvw.getSpawnLocation();
        }
        return w.getSpawnLocation();
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.worldManager.getMVWorlds().size() == 0 && this.plugin.getPermissions().hasPermission(event.getPlayer(), "multiverse.core.import", true)) {
            event.getPlayer().sendMessage("You don't have any worlds imported into Multiverse!");
            event.getPlayer().sendMessage("You can import your current worlds with " + ChatColor.AQUA + "/mvimport");
            event.getPlayer().sendMessage("or you can create new ones with " + ChatColor.GOLD + "/mvcreate");
            event.getPlayer().sendMessage("If you just wanna see all of the Multiverse Help, type: " + ChatColor.GREEN + "/mv");
        }
        // Handle the Players GameMode setting for the new world.
        if (this.plugin.getConfig().getBoolean("enforcegamemodes", true)) {
            this.handleGameMode(event.getPlayer(), event.getPlayer().getWorld());
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.removePlayerSession(event.getPlayer());
    }

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        MVWorld fromWorld = this.worldManager.getMVWorld(event.getTo().getWorld().getName());
        MVWorld toWorld = this.worldManager.getMVWorld(event.getTo().getWorld().getName());
        if (toWorld != null) {
            if (!this.plugin.getPermissions().canEnterWorld(event.getPlayer(), toWorld)) {
                event.getPlayer().sendMessage("You don't have access to go here...");
                event.setCancelled(true);
                return;
            }
        }
        if (fromWorld != null) {
            if (fromWorld.getWorldBlacklist().contains(toWorld.getName())) {
                event.getPlayer().sendMessage("You don't have access to go to " + toWorld.getColoredWorldString() + " from " + fromWorld.getColoredWorldString());
                event.setCancelled(true);
                return;
            }
        }
        if (toWorld == null) {
            // The toworld is not handled by MV, we don't care about payments
            return;
        }
        // Only check payments if it's a different world:
        if (!event.getTo().getWorld().equals(event.getFrom().getWorld())) {
            // Handle the Players GameMode setting for the new world.
            if (this.plugin.getConfig().getBoolean("enforcegamemodes", true)) {
                this.handleGameMode(event.getPlayer(), toWorld);
            }

            // If the player does not have to pay, return now.
            if (toWorld.isExempt(event.getPlayer())) {
                return;
            }
            GenericBank bank = plugin.getBank();
            if (!bank.hasEnough(event.getPlayer(), toWorld.getPrice(), toWorld.getCurrency(), "You need " + bank.getFormattedAmount(event.getPlayer(), toWorld.getPrice(), toWorld.getCurrency()) + " to enter " + toWorld.getColoredWorldString())) {
                event.setCancelled(true);
            } else {
                bank.pay(event.getPlayer(), toWorld.getPrice(), toWorld.getCurrency());
            }
        }
    }

    // FOLLOWING 2 Methods and Private class handle Per Player GameModes.
    private void handleGameMode(Player player, World world) {
        MVWorld mvWorld = this.worldManager.getMVWorld(world.getName());
        if (mvWorld != null) {
            this.handleGameMode(player, mvWorld);
        }
    }

    public void handleGameMode(Player player, MVWorld world) {
        // We perform this task one tick later to MAKE SURE that the player actually reaches the
        // destination world, otherwise we'd be changing the player mode if they havent moved anywhere.
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new HandleGameMode(player, world), 1L);
    }

    /**
     * The following private class is used to handle player game mode changes within a scheduler.
     */
    private class HandleGameMode implements Runnable {

        private Player player;
        private MVWorld world;

        private HandleGameMode(Player player, MVWorld world) {
            this.player = player;
            this.world = world;
        }

        @Override
        public void run() {
            // Check that the player is in the new world and they haven't been teleported elsewhere or the event cancelled.
            if (player.getWorld().getName().equals(world.getCBWorld().getName())) {
                player.setGameMode(world.getGameMode());
            }
        }
    }
}
