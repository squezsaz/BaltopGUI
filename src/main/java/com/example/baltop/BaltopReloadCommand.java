package com.example.baltop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BaltopReloadCommand implements CommandExecutor {
    private final BaltopPlugin plugin;
    
    public BaltopReloadCommand(BaltopPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("baltop.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
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