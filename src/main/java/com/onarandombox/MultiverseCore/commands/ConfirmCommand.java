package com.onarandombox.MultiverseCore.commands;

import com.onarandombox.MultiverseCore.MultiverseCore;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class ConfirmCommand extends MultiverseCommand {

    public ConfirmCommand(MultiverseCore plugin) {
        super(plugin);
        // Any command that is dangerous should require op
        this.setName("Confirms a command that could destroy life, the universe and everything.");
        this.setCommandUsage("/mv confirm");
        this.setArgRange(0, 0);
        this.addKey("mvconfirm");
        this.addKey("mv confirm");
        this.setPermission("multiverse.core.confirm", "If you have not been prompted to use this, it will not do anything.", PermissionDefault.OP);

    }

    @Override
    public void runCommand(CommandSender sender, List<String> args) {
        this.plugin.getCommandHandler().confirmQueuedCommand(sender);
    }

}
