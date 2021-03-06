package com.onarandombox.MultiverseCore.commands;

import com.onarandombox.MultiverseCore.MVWorld;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.utils.PurgeWorlds;
import com.onarandombox.utils.WorldManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PurgeCommand extends MultiverseCommand {
    private WorldManager worldManager;

    public PurgeCommand(MultiverseCore plugin) {
        super(plugin);
        this.setName("Purge World of Creatures");
        this.setCommandUsage("/mv purge" + ChatColor.GOLD + " [WORLD|all] " + ChatColor.GREEN + "{all|animals|monsters|MOBNAME}");
        this.setArgRange(1, 2);
        this.addKey("mvpurge");
        this.addKey("mv purge");
        this.setPermission("multiverse.core.purge", "Removed the specified type of mob from the specified world.", PermissionDefault.OP);
        this.worldManager = this.plugin.getWorldManager();
    }

    @Override
    public void runCommand(CommandSender sender, List<String> args) {
        Player p = null;
        if (sender instanceof Player) {
            p = (Player) sender;
        }
        if (args.size() == 1 && p == null) {
            sender.sendMessage("This command requires a WORLD when being run from the console!");
            sender.sendMessage(this.getCommandUsage());
            return;
        }
        String worldName = null;
        String deathName = null;
        if (args.size() == 1) {
            worldName = p.getWorld().getName();
            deathName = args.get(0);
        } else {
            worldName = args.get(0);
            deathName = args.get(1);
        }

        if (!worldName.equalsIgnoreCase("all") && !this.worldManager.isMVWorld(worldName)) {
            ((MultiverseCore) this.plugin).showNotMVWorldMessage(sender, worldName);
            sender.sendMessage("It cannot be purged.");
            return;
        }

        List<MVWorld> worldsToRemoveEntitiesFrom = new ArrayList<MVWorld>();
        // Handle all case any user who names a world "all" should know better...
        if (worldName.equalsIgnoreCase("all")) {
            worldsToRemoveEntitiesFrom.addAll(this.worldManager.getMVWorlds());
        } else {
            worldsToRemoveEntitiesFrom.add(this.worldManager.getMVWorld(worldName));
        }

        PurgeWorlds purger = this.worldManager.getWorldPurger();
        ArrayList<String> thingsToKill = new ArrayList<String>();
        if (deathName.equalsIgnoreCase("all") || deathName.equalsIgnoreCase("animals") || deathName.equalsIgnoreCase("monsters")) {
            thingsToKill.add(deathName.toUpperCase());
        } else {
            Collections.addAll(thingsToKill, deathName.toUpperCase().split(","));
        }
        for (MVWorld w : worldsToRemoveEntitiesFrom) {
            purger.purgeWorld(sender, w, thingsToKill, false, false);
        }

        return;
    }

}
