package com.example.baltop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BaltopCommand implements CommandExecutor {
    private final BaltopPlugin plugin;
    
    public BaltopCommand(BaltopPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if this is a reload command
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Handle reload command
            return handleReloadCommand(sender);
        }
        
        // Handle regular baltop command (open GUI)
        if (!(sender instanceof Player)) {
            String playersOnlyMessage = plugin.getLanguageConfig().getString("messages.only-players", "&cThis command can only be used by players!");
            sender.sendMessage(playersOnlyMessage);
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("baltop.use")) {
            String noPermissionMessage = plugin.getLanguageConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            noPermissionMessage = noPermissionMessage.replace('&', '§');
            player.sendMessage(noPermissionMessage);
            return true;
        }
        
        // Open the first page of the paged GUI
        PagedBaltopGUI.openGUI(player, 1);
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        // Check permission
        if (!sender.hasPermission("baltop.admin")) {
            String noPermissionMessage = plugin.getLanguageConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            noPermissionMessage = noPermissionMessage.replace('&', '§');
            sender.sendMessage(noPermissionMessage);
            return true;
        }
        
        // Reload the plugin configuration
        plugin.reloadConfig();
        
        // Reload language configuration
        plugin.loadLanguageConfig();
        
        // Send confirmation message
        String reloadMessage = plugin.getLanguageConfig().getString("messages.config-reloaded", "&aConfiguration file and plugin successfully reloaded!");
        reloadMessage = reloadMessage.replace('&', '§');
        sender.sendMessage(reloadMessage);
        
        return true;
    }
}