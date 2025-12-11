package com.example.baltop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.*;

public class PagedBaltopGUI {
    private static final Map<UUID, Integer> playerPages = new HashMap<>();
    private static final Map<UUID, String> playerSortOrders = new HashMap<>();
    private static final Map<UUID, List<PlayerBalance>> cachedBalances = new HashMap<>();
    private static final Map<UUID, Double> vaultOfflineBalances = new HashMap<>(); // Cache for Vault offline player balances
    
    public static void openGUI(Player player, int page) {
        FileConfiguration config = BaltopPlugin.getInstance().getConfig();
        FileConfiguration langConfig = BaltopPlugin.getInstance().getLanguageConfig();
        int playersPerPage = Math.min(config.getInt("player-limit", 45), 45);
        int guiSize = config.getInt("gui.size", 54);
        
        // Get top players with balances using player-specific sorting
        List<PlayerBalance> topPlayers = getTopPlayersForPlayer(player);
        cachedBalances.put(player.getUniqueId(), topPlayers);
        
        // Create GUI
        String guiTitle = langConfig.getString("messages.gui-title", "&6&lRich List &8» &7Page {page}");
        guiTitle = guiTitle.replace('&', '§');
        guiTitle = guiTitle.replace("{page}", String.valueOf(page));
        Inventory gui = Bukkit.createInventory(null, guiSize, guiTitle);
        
        // Add player heads to GUI
        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, topPlayers.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            PlayerBalance pb = topPlayers.get(i);
            int slot = i % playersPerPage;
            if (slot < guiSize - 9) { // Leave last row for controls
                ItemStack head = createPlayerHead(pb.player, pb.balance, i + 1);
                gui.setItem(slot, head);
            }
        }
        
        // Add control buttons in the last row
        // Previous page button
        if (page > 1) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                String prevText = langConfig.getString("messages.previous-page", "&a« &7Previous Page");
                prevText = prevText.replace('&', '§');
                prevMeta.setDisplayName(prevText);
                prevButton.setItemMeta(prevMeta);
            }
            int prevSlot = config.getInt("gui.previous-page-slot", 45);
            gui.setItem(Math.min(prevSlot, guiSize - 1), prevButton);
        }
        
        // Next page button
        int totalPages = (int) Math.ceil((double) topPlayers.size() / playersPerPage);
        if (page < totalPages) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                String nextText = langConfig.getString("messages.next-page", "&7Next Page &a»");
                nextText = nextText.replace('&', '§');
                nextMeta.setDisplayName(nextText);
                nextButton.setItemMeta(nextMeta);
            }
            int nextSlot = config.getInt("gui.next-page-slot", 53);
            gui.setItem(Math.min(nextSlot, guiSize - 1), nextButton);
        }
        
        // Sorting toggle button
        ItemStack sortButton = new ItemStack(Material.HOPPER);
        ItemMeta sortMeta = sortButton.getItemMeta();
        if (sortMeta != null) {
            String currentSort = playerSortOrders.getOrDefault(player.getUniqueId(), 
                config.getString("default-sort-order", "richest_first"));
            
            // Set different display names based on current sort order
            if (currentSort.equals("richest_first")) {
                // Rich to poor sorting
                String sortText = langConfig.getString("messages.sort-poorest-first", "&eSort Poor to Rich");
                sortText = sortText.replace('&', '§');
                sortMeta.setDisplayName(sortText);
            } else {
                // Poor to rich sorting
                String sortText = langConfig.getString("messages.sort-richest-first", "&eSort Rich to Poor");
                sortText = sortText.replace('&', '§');
                sortMeta.setDisplayName(sortText);
            }
            
            sortButton.setItemMeta(sortMeta);
        }
        int sortSlot = config.getInt("gui.sort-toggle-slot", 48);
        gui.setItem(Math.min(sortSlot, guiSize - 1), sortButton);
        
        // Refresh button
        ItemStack refreshButton = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refreshButton.getItemMeta();
        if (refreshMeta != null) {
            String refreshText = langConfig.getString("messages.refresh-button", "&aRefresh");
            refreshText = refreshText.replace('&', '§');
            refreshMeta.setDisplayName(refreshText);
            refreshButton.setItemMeta(refreshMeta);
        }
        int refreshSlot = config.getInt("gui.refresh-slot", 50);
        gui.setItem(Math.min(refreshSlot, guiSize - 1), refreshButton);
        
        // Search button
        ItemStack searchButton = new ItemStack(Material.OAK_SIGN);
        ItemMeta searchMeta = searchButton.getItemMeta();
        if (searchMeta != null) {
            String searchText = langConfig.getString("messages.search-button", "&bSearch Player");
            searchText = searchText.replace('&', '§');
            searchMeta.setDisplayName(searchText);
            searchButton.setItemMeta(searchMeta);
        }
        int searchSlot = config.getInt("gui.search-slot", 51);
        gui.setItem(Math.min(searchSlot, guiSize - 1), searchButton);
        
        // Add player's own head in the center of the bottom row
        String selfInfoTitle = langConfig.getString("messages.self-info-title", "&6&lYour Information");
        selfInfoTitle = selfInfoTitle.replace('&', '§');

        List<String> selfInfoLore = langConfig.getStringList("messages.self-info-lore");
        selfInfoLore.replaceAll(s -> s.replace('&', '§'));

        PlayerBalance playerInfo = getPlayerBalance(player, topPlayers);
        if (playerInfo != null) {
            ItemStack playerHead = createSelfInfoHead(player, playerInfo.balance, getPlayerRank(player, topPlayers));
            int selfSlot = config.getInt("gui.self-info-slot", 49);
            gui.setItem(Math.min(selfSlot, guiSize - 1), playerHead);
        }
        
        // Fill empty slots with decorative items for better aesthetics
        boolean fillEmptySlots = config.getBoolean("gui.fill-empty-slots", true);
        if (fillEmptySlots) {
            String emptySlotItemStr = config.getString("gui.empty-slot-item", "BLACK_STAINED_GLASS_PANE");
            String emptySlotName = langConfig.getString("gui.empty-slot-name", "");
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
            
            // Fill empty slots in the main area (excluding control row)
            for (int i = 0; i < guiSize - 9; i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, emptySlotItem);
                }
            }
        }
        
        // Store current page for player
        playerPages.put(player.getUniqueId(), page);
        
        // Open GUI for player
        player.openInventory(gui);
    }
    
    public static void handleClick(Player player, int slot) {
        FileConfiguration config = BaltopPlugin.getInstance().getConfig();
        int guiSize = config.getInt("gui.size", 54);
        
        // Calculate slots
        int prevSlot = config.getInt("gui.previous-page-slot", 45);
        int nextSlot = config.getInt("gui.next-page-slot", 53);
        int toggleSlot = config.getInt("gui.sort-toggle-slot", 48);
        int refreshSlot = config.getInt("gui.refresh-slot", 50);
        int searchSlot = config.getInt("gui.search-slot", 51);
        int selfSlot = config.getInt("gui.self-info-slot", 49);
        
        int playersPerPage = Math.min(config.getInt("player-limit", 45), 45);
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);
        List<PlayerBalance> topPlayers = cachedBalances.getOrDefault(player.getUniqueId(), new ArrayList<>());
        int totalPages = (int) Math.ceil((double) topPlayers.size() / playersPerPage);
        
        if (slot == prevSlot && currentPage > 1) {
            openGUI(player, currentPage - 1);
        } else if (slot == nextSlot && currentPage < totalPages) {
            openGUI(player, currentPage + 1);
        } else if (slot == toggleSlot) { // Toggle sort order
            String currentSort = playerSortOrders.getOrDefault(player.getUniqueId(), 
                config.getString("default-sort-order", "richest_first"));
            String newSort = currentSort.equals("richest_first") ? "poorest_first" : "richest_first";
            playerSortOrders.put(player.getUniqueId(), newSort);
            openGUI(player, 1); // Refresh with new sort order
        } else if (slot == refreshSlot) { // Refresh GUI and rankings
            // Clear the cache to force refresh
            clearCache();
            openGUI(player, 1); // Open first page after refresh
        } else if (slot == searchSlot) { // Open search GUI
            // Close the GUI and prompt player to enter name in chat
            FileConfiguration langConfig = BaltopPlugin.getInstance().getLanguageConfig();
            String searchPrompt = langConfig.getString("messages.search-prompt", "&ePlease enter the name of the player you want to search for:");
            searchPrompt = searchPrompt.replace('&', '§');
            player.closeInventory();
            player.sendMessage(searchPrompt);
            BaltopPlugin.getInstance().setSearchingPlayer(player.getUniqueId());
        } else if (slot == selfSlot) {
            // Do nothing for self info slot
        }
    }
    
    public static List<PlayerBalance> getTopPlayers() {
        List<PlayerBalance> balances = new ArrayList<>();
        
        // Use Vault economy
        balances = getVaultBalances();
        
        // Get sort order - first check if there's a cached player to get their preference
        // Since this is a static method, we can't directly access player preferences
        // We'll use the default sort order for now, but the sorting will be updated when the GUI is opened
    
        // Default sort order from config
        String defaultSortOrder = BaltopPlugin.getInstance().getConfig().getString("default-sort-order", "richest_first");
        
        // Sort by balance
        if (defaultSortOrder.equals("richest_first")) {
            // Highest first
            balances.sort((a, b) -> Double.compare(b.balance, a.balance));
        } else {
            // Lowest first
            balances.sort((a, b) -> Double.compare(a.balance, b.balance));
        }
        
        return balances;
    }
    
    private static List<PlayerBalance> getVaultBalances() {
        Economy economy = BaltopPlugin.getInstance().getEconomy();
        List<PlayerBalance> balances = new ArrayList<>();
        
        // Get all online players and check their balances
        for (Player player : Bukkit.getOnlinePlayers()) {
            double balance = economy.getBalance(player);
            balances.add(new PlayerBalance(player, balance));
            // Cache the balance for offline use
            vaultOfflineBalances.put(player.getUniqueId(), balance);
        }
        
        // Also include offline players who have played before and have cached balances
        for (Map.Entry<UUID, Double> entry : vaultOfflineBalances.entrySet()) {
            UUID playerId = entry.getKey();
            double balance = entry.getValue();
            
            // Check if this player is not already in the list (i.e., not online)
            boolean alreadyAdded = false;
            for (PlayerBalance pb : balances) {
                if (pb.player.getUniqueId().equals(playerId)) {
                    alreadyAdded = true;
                    break;
                }
            }
            
            if (!alreadyAdded) {
                // Get the offline player
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
                balances.add(new PlayerBalance(offlinePlayer, balance));
            }
        }
        
        // Additionally, try to get all known players from the server to ensure comprehensive offline support
        // This helps ensure that even players not in our cache are included if they have played before
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            // Skip if already in the list
            boolean alreadyAdded = false;
            for (PlayerBalance pb : balances) {
                if (pb.player.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                    alreadyAdded = true;
                    break;
                }
            }
            
            if (!alreadyAdded && offlinePlayer.hasPlayedBefore()) {
                double balance = economy.getBalance(offlinePlayer);
                balances.add(new PlayerBalance(offlinePlayer, balance));
                // Cache this balance for future use
                vaultOfflineBalances.put(offlinePlayer.getUniqueId(), balance);
            }
        }
        
        return balances;
    }
    
    private static PlayerBalance getPlayerBalance(Player player, List<PlayerBalance> allBalances) {
        for (PlayerBalance pb : allBalances) {
            if (pb.player.getUniqueId().equals(player.getUniqueId())) {
                return pb;
            }
        }
        return null;
    }
    
    private static int getPlayerRank(Player player, List<PlayerBalance> allBalances) {
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
                line = line.replace("{rank}", String.valueOf(rank));
                line = line.replace("{name}", player.getName());
                line = line.replace("{balance}", formatBalance(balance));
                lore.add(line);
            }
            
            meta.setLore(lore);
            
            // Add enchantment glow for top 3 players
            if (rank <= 3) {
                meta.setEnchantmentGlintOverride(true);
            }
            
            head.setItemMeta(meta);
        }
        
        return head;
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
    
    private static ItemStack createSelfInfoHead(Player player, double balance, int rank) {
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
            String title = langConfig.getString("messages.self-info-title", "&6&lYour Information");
            title = title.replace('&', '§');
            meta.setDisplayName(title);
            
            // Set lore from config
            List<String> loreLines = langConfig.getStringList("messages.self-info-lore");
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
                String rankStr = (rank > 0) ? String.valueOf(rank) : "N/A";
                line = line.replace("{rank}", rankStr);
                line = line.replace("{name}", player.getName());
                line = line.replace("{balance}", formatBalance(balance));
                lore.add(line);
            }
            
            meta.setLore(lore);
            
            // Add enchantment glow effect for visual appeal
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
    
    public static class PlayerBalance {
        public final OfflinePlayer player;
        public final double balance;
        
        public PlayerBalance(OfflinePlayer player, double balance) {
            this.player = player;
            this.balance = balance;
        }
    }
    
    public static void clearCache() {
        cachedBalances.clear();
        vaultOfflineBalances.clear();
    }
    
    public static List<PlayerBalance> getTopPlayersForPlayer(Player player) {
        List<PlayerBalance> balances = new ArrayList<>();
        
        // Use Vault economy
        balances = getVaultBalances();
        
        // Get sort order for this specific player
        String playerSortOrder = playerSortOrders.getOrDefault(player.getUniqueId(), 
            BaltopPlugin.getInstance().getConfig().getString("default-sort-order", "richest_first"));
        
        // Sort by balance based on player's preference
        if (playerSortOrder.equals("richest_first")) {
            // Highest first
            balances.sort((a, b) -> Double.compare(b.balance, a.balance));
        } else {
            // Lowest first
            balances.sort((a, b) -> Double.compare(a.balance, b.balance));
        }
        
        return balances;
    }
    
    // Method to update offline balances when a player disconnects
    public static void updateOfflineBalance(Player player) {
        // Update Vault balances if available
        Economy economy = BaltopPlugin.getInstance().getEconomy();
        double vaultBalance = economy.getBalance(player);
        vaultOfflineBalances.put(player.getUniqueId(), vaultBalance);
    }
    
    // Method to remove player data when they disconnect
    public static void removePlayerData(UUID playerId) {
        playerPages.remove(playerId);
        playerSortOrders.remove(playerId);
        cachedBalances.remove(playerId);
        vaultOfflineBalances.remove(playerId);
    }
}