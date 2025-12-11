package com.example.baltop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SearchGUI {
    private static final List<UUID> playersAwaitingInput = new ArrayList<>();
    // Add a constant to store the back button slot
    private static final int BACK_BUTTON_SLOT = -1; // We'll use a special value to indicate dynamic slot
    
    public static void openSearchGUI(Player player) {
        // This method is no longer used as we changed the search flow
        // The search is now initiated directly from chat after clicking the sign
    }
    
    public static void showSearchResults(Player searcher, String searchTerm) {
        FileConfiguration config = BaltopPlugin.getInstance().getConfig();
        FileConfiguration langConfig = BaltopPlugin.getInstance().getLanguageConfig();
        
        // Get all player balances using player-specific sorting
        List<PagedBaltopGUI.PlayerBalance> allBalances = PagedBaltopGUI.getTopPlayersForPlayer(searcher);
        
        // Find matching players (case insensitive)
        List<PagedBaltopGUI.PlayerBalance> matchingPlayers = new ArrayList<>();
        for (PagedBaltopGUI.PlayerBalance pb : allBalances) {
            if (pb.player.getName().toLowerCase().contains(searchTerm.toLowerCase())) {
                matchingPlayers.add(pb);
            }
        }
        
        // If no players found, send message and return
        if (matchingPlayers.isEmpty()) {
            String notFoundMessage = langConfig.getString("messages.player-not-found", "&cPlayer not found.");
            notFoundMessage = notFoundMessage.replace('&', '§');
            searcher.sendMessage(notFoundMessage);
            return;
        }
        
        // Create search results GUI
        String guiTitle = langConfig.getString("messages.search-results-title", "&6&lSearch Results");
        guiTitle = guiTitle.replace('&', '§');
        // Make GUI size appropriate for number of results (minimum 18 slots to have space for controls)
        int guiSize = Math.min(54, Math.max(18, ((matchingPlayers.size() / 9) + 2) * 9)); // At least 2 rows for results + 1 row for controls
        Inventory gui;
        if (!guiTitle.isEmpty()) {
            gui = Bukkit.createInventory(null, guiSize, guiTitle);
        } else {
            gui = Bukkit.createInventory(null, guiSize);
        }
        
        // Add player heads to GUI (reserve last 9 slots for controls)
        for (int i = 0; i < matchingPlayers.size() && i < guiSize - 9; i++) {
            PagedBaltopGUI.PlayerBalance pb = matchingPlayers.get(i);
            // Find actual rank in complete list
            int rank = getPlayerRank(pb.player, allBalances);
            ItemStack head = createPlayerHead(pb.player, pb.balance, rank);
            gui.setItem(i, head);
        }
        
        // Add back button in the last row (second to last slot)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            String backText = "&a« &fAna Menü";
            backText = backText.replace('&', '§');
            backMeta.setDisplayName(backText);
            
            // Don't add any lore that would be visible to players
            // We'll identify this button by its material and slot position instead
            backButton.setItemMeta(backMeta);
        }

        // Place back button in the second to last slot of the last row (slot depends on guiSize)
        int backSlot = guiSize - 5; // This puts it in a reasonable position in the last row
        gui.setItem(backSlot, backButton);
        
        // Fill empty slots with decorative items for better aesthetics
        boolean fillEmptySlots = config.getBoolean("gui.fill-empty-slots", true);
        if (fillEmptySlots) {
            String emptySlotItemStr = config.getString("gui.empty-slot-item", "BLACK_STAINED_GLASS_PANE");
            String emptySlotName = config.getString("gui.empty-slot-name", "");
            emptySlotName = emptySlotName.replace('&', '§');
            
            Material emptySlotMaterial;
            try {
                emptySlotMaterial = Material.valueOf(emptySlotItemStr);
            } catch (Exception e) {
                // Fallback to BLACK_STAINED_GLASS_PANE if invalid material
                emptySlotMaterial = Material.BLACK_STAINED_GLASS_PANE;
            }
            
            ItemStack emptySlotItem = new ItemStack(emptySlotMaterial);
            ItemMeta emptyMeta = emptySlotItem.getItemMeta();
            if (emptyMeta != null) {
                // Only set display name if it's not empty
                if (!emptySlotName.isEmpty()) {
                    emptyMeta.setDisplayName(emptySlotName);
                }
                emptySlotItem.setItemMeta(emptyMeta);
            }
            
            // Fill empty slots (but not the control slots in the last row)
            for (int i = 0; i < guiSize - 9; i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, emptySlotItem);
                }
            }
        }
        
        // Open GUI for searcher
        searcher.openInventory(gui);
    }
    
    private static int getPlayerRank(OfflinePlayer player, List<PagedBaltopGUI.PlayerBalance> allBalances) {
        for (int i = 0; i < allBalances.size(); i++) {
            if (allBalances.get(i).player.getUniqueId().equals(player.getUniqueId())) {
                return i + 1; // Ranks start at 1
            }
        }
        return -1; // Not found
    }
    
    private static ItemStack createPlayerHead(OfflinePlayer player, double balance, int rank) {
        ItemStack head;
        
        // Use player head item (different materials for different versions)
        try {
            // For newer versions
            head = new ItemStack(Material.PLAYER_HEAD);
        } catch (Exception e) {
            // For older versions
            head = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        }
        
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            
            FileConfiguration langConfig = BaltopPlugin.getInstance().getLanguageConfig();
            // Get rank color based on position
            String rankColor = getRankColor(rank);
            String rankFormat = langConfig.getString("rank-format", "&e#{rank} &f{name}");
            rankFormat = rankFormat.replace('&', '§');
            
            // Handle unknown rank
            String rankStr = (rank > 0) ? String.valueOf(rank) : "?";
            rankFormat = rankFormat.replace("{rank}", rankColor + rankStr);
            rankFormat = rankFormat.replace("{name}", player.getName());
            
            meta.setDisplayName(rankFormat);
            
            // Set lore from config
            List<String> loreLines = langConfig.getStringList("messages.player-head-lore");
            if (loreLines.isEmpty()) {
                // Fallback to default lore based on language
                String lang = BaltopPlugin.getInstance().getConfig().getString("language", "en");
                loreLines.add("");
                if ("tr".equals(lang)) {
                    loreLines.add("&7Sıra: &e#{rank}");
                    loreLines.add("&7İsim: &f{name}");
                    loreLines.add("&7Bakiye: &a{balance}");
                } else {
                    loreLines.add("&7Rank: &e#{rank}");
                    loreLines.add("&7Name: &f{name}");
                    loreLines.add("&7Balance: &a{balance}");
                }
                loreLines.add("");
            }
            
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                line = line.replace('&', '§');
                // Handle unknown rank in lore
                String rankInLore = (rank > 0) ? String.valueOf(rank) : "?";
                line = line.replace("{rank}", rankInLore);
                line = line.replace("{name}", player.getName());
                line = line.replace("{balance}", formatBalance(balance));
                lore.add(line);
            }
            
            meta.setLore(lore);
            
            // Add enchantment glow for searched player head
            meta.setEnchantmentGlintOverride(true);
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    private static String formatBalance(double balance) {
        FileConfiguration config = BaltopPlugin.getInstance().getConfig();
        int decimalPlaces = config.getInt("formatting.decimal-places", 2);
        DecimalFormat formatter = new DecimalFormat("#,###." + "#".repeat(Math.max(0, decimalPlaces)));
        
        double thousands = config.getDouble("formatting.thousands", 1000);
        double millions = config.getDouble("formatting.millions", 1000000);
        double billions = config.getDouble("formatting.billions", 1000000000);
        
        String thousandSymbol = config.getString("formatting.thousand-symbol", "K");
        String millionSymbol = config.getString("formatting.million-symbol", "M");
        String billionSymbol = config.getString("formatting.billion-symbol", "B");
        
        String currencyIcon = config.getString("gui.balance-icons.vault", "&f⛃");
        currencyIcon = currencyIcon.replace('&', '§');
        
        if (balance >= billions) {
            return currencyIcon + " " + formatter.format(balance / billions) + billionSymbol;
        } else if (balance >= millions) {
            return currencyIcon + " " + formatter.format(balance / millions) + millionSymbol;
        } else if (balance >= thousands) {
            return currencyIcon + " " + formatter.format(balance / thousands) + thousandSymbol;
        } else {
            return currencyIcon + " " + formatter.format(balance);
        }
    }
    
    private static String getRankColor(int rank) {
        FileConfiguration config = BaltopPlugin.getInstance().getConfig();
        Map<String, Object> rankColors = config.getConfigurationSection("gui.rank-colors").getValues(false);
        
        String rankStr = String.valueOf(rank);
        if (rankColors.containsKey(rankStr)) {
            // For specific ranks, return just the color part without the number
            String colorCode = rankColors.get(rankStr).toString();
            // Remove the number from the end if it matches the rank
            if (colorCode.endsWith(rankStr)) {
                return colorCode.substring(0, colorCode.length() - rankStr.length()).replace('&', '§');
            }
            return colorCode.replace('&', '§');
        } else {
            // Return default color
            return rankColors.getOrDefault("default", "&e").toString().replace('&', '§');
        }
    }
}