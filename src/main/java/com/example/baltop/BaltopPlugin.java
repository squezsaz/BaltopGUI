package com.example.baltop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class BaltopPlugin extends JavaPlugin {
    private static BaltopPlugin instance;
    private Economy economy;
    private boolean economyEnabled = false;
    private final ConcurrentHashMap<UUID, Long> searchingPlayers = new ConcurrentHashMap<>();
    private final Logger logger = getLogger();
    private FileConfiguration languageConfig;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Load language configuration
        loadLanguageConfig();
        
        // Setup Vault economy
        setupEconomy();
        
        // Register commands
        getCommand("baltop").setExecutor(new BaltopCommand(this));

        // Register events
        getServer().getPluginManager().registerEvents(new BaltopListener(), this);
        
        // Schedule auto-refresh task
        long refreshInterval = getConfig().getLong("refresh-interval", 300) * 20L; // Convert seconds to ticks
        if (refreshInterval > 0) {
            getServer().getScheduler().runTaskTimer(this, PagedBaltopGUI::clearCache, refreshInterval, refreshInterval);
        }
        
        logger.info("[BaltopGUI] Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        logger.info("[BaltopGUI] Plugin disabled!");
    }
    
    public void loadLanguageConfig() {
        String language = getConfig().getString("language", "en");
        String fileName = "lang_" + language + ".yml";
        
        // Try to load the language file from resources
        InputStream inputStream = getResource(fileName);
        if (inputStream != null) {
            languageConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
            logger.info("[BaltopGUI] Loaded language file: " + fileName);
        } else {
            // Fallback to default English if language file not found
            languageConfig = getConfig();
            logger.warning("[BaltopGUI] Language file not found: " + fileName + ", using default config");
        }
    }
    
    public FileConfiguration getLanguageConfig() {
        return languageConfig;
    }
    
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.severe("Vault plugin not found! This plugin requires Vault to function.");
            economyEnabled = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.severe("No economy plugin found! Please install an economy plugin like EssentialsX.");
            economyEnabled = false;
            return;
        }
        
        economy = rsp.getProvider();
        economyEnabled = true;
        logger.info("Vault economy hooked successfully!");
    }
    
    public static BaltopPlugin getInstance() {
        return instance;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public boolean isEconomyEnabled() {
        return economyEnabled;
    }
    
    public void setSearchingPlayer(UUID playerId) {
        searchingPlayers.put(playerId, System.currentTimeMillis());
    }
    
    public boolean isPlayerSearching(UUID playerId) {
        return searchingPlayers.containsKey(playerId);
    }
    
    public void removeSearchingPlayer(UUID playerId) {
        searchingPlayers.remove(playerId);
    }
    
    public Long getSearchStartTime(UUID playerId) {
        return searchingPlayers.get(playerId);
    }
}