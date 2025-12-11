package com.example.baltop;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class BaltopListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Get the language configuration
        FileConfiguration langConfig = BaltopPlugin.getInstance().getLanguageConfig();
        
        // Check if this is our Baltop GUI by comparing the title
        String configTitle = langConfig.getString("messages.gui-title", "&6&lRich List &8» &7Page {page}");
        configTitle = configTitle.replace('&', '§');
        
        // Check if title starts with the base title (to match any page)
        if (title.startsWith(configTitle.substring(0, Math.min(configTitle.length(), 20)))) {
            // Cancel the event to prevent players from taking items
            event.setCancelled(true);
            
            // Handle navigation buttons
            if (event.getCurrentItem() != null && event.getRawSlot() < event.getInventory().getSize()) {
                PagedBaltopGUI.handleClick(player, event.getRawSlot());
            }
            return;
        }
        
        // Check if this is our search results GUI
        String searchTitle = langConfig.getString("messages.search-results-title", "&6&lSearch Results");
        searchTitle = searchTitle.replace('&', '§');
        if (title.equals(searchTitle)) {
            // Cancel the event to prevent players from taking items
            event.setCancelled(true);
            
            // Handle back button by checking if the clicked item is the back button
            // We identify the back button by its slot position (guiSize - 5) in the search results GUI
            // First, we need to determine the GUI size to calculate the expected back button slot
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                // Get the expected back button slot based on the inventory size
                int inventorySize = event.getInventory().getSize();
                int expectedBackSlot = inventorySize - 5; // Same calculation used in SearchGUI
                
                // Check if the clicked slot matches the expected back button slot
                if (event.getRawSlot() == expectedBackSlot) {
                    // This is the back button, open main Baltop GUI
                    PagedBaltopGUI.openGUI(player, 1);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if the inventory title matches our Baltop GUI
        String title = event.getView().getTitle();
        String configTitle = BaltopPlugin.getInstance().getConfig().getString("messages.gui-title", "&6&lZenginler Listesi &8» &7Sayfa {page}");
        configTitle = configTitle.replace('&', '§');
        
        // Check if title starts with the base title (to match any page)
        if (title.startsWith(configTitle.substring(0, Math.min(configTitle.length(), 20)))) {
            // Cancel the event to prevent players from placing items
            event.setCancelled(true);
        }
        
        // Check if it's the search GUI
        String searchTitle = BaltopPlugin.getInstance().getConfig().getString("messages.search-results-title", "&6&lArama Sonuçları");
        searchTitle = searchTitle.replace('&', '§');
        if (title.equals(searchTitle)) {
            // Cancel the event to prevent players from placing items
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in search mode
        if (BaltopPlugin.getInstance().isPlayerSearching(player.getUniqueId())) {
            event.setCancelled(true);
            
            // Process search
            String playerName = event.getMessage();
            SearchGUI.showSearchResults(player, playerName);
            
            // Remove player from search mode
            BaltopPlugin.getInstance().removeSearchingPlayer(player.getUniqueId());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Update offline balance cache when player disconnects
        PagedBaltopGUI.updateOfflineBalance(player);
        
        // Clean up player data when they quit
        PagedBaltopGUI.removePlayerData(player.getUniqueId());
    }
}