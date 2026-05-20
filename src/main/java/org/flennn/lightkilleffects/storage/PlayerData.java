package org.flennn.lightkilleffects.storage;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.flennn.lightkilleffects.LightKillEffects;
import org.flennn.lightkilleffects.effect.EffectType;

import java.util.*;
public class PlayerData {
    
    private final LightKillEffects plugin;
    private final StorageHandler storageHandler;
    private final Map<UUID, PlayerEffectData> playerDataCache;
    
    public PlayerData(LightKillEffects plugin, StorageHandler storageHandler) {
        this.plugin = plugin;
        this.storageHandler = storageHandler;
        this.playerDataCache = new HashMap<>();
        loadAllPlayerData();
    }
    private void loadAllPlayerData() {
        ConfigurationSection playersSection = storageHandler.getPlayerDataConfig().getConfigurationSection("");
        if (playersSection == null) return;
        
        for (String uuidString : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                loadPlayerData(uuid);
            } catch (IllegalArgumentException e) {
                plugin.logDebug("Invalid UUID in player data: " + uuidString);
            }
        }
        
        plugin.logInfo("&aLoaded data for " + playerDataCache.size() + " players");
    }
    public PlayerEffectData loadPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }
        
        ConfigurationSection playerSection = storageHandler.getPlayerDataConfig().getConfigurationSection(uuid.toString());
        PlayerEffectData data = new PlayerEffectData(uuid);
        
        if (playerSection != null) {
            data.setName(playerSection.getString("name", "Unknown"));
            String selectedEffect = playerSection.getString("selected-effect");
            if (selectedEffect != null && !selectedEffect.equals("none")) {
                data.setSelectedEffect(EffectType.fromConfigKey(selectedEffect));
            }
            List<String> unlockedEffects = playerSection.getStringList("unlocked-effects");
            for (String effectKey : unlockedEffects) {
                EffectType effect = EffectType.fromConfigKey(effectKey);
                if (effect != null) {
                    data.addUnlockedEffect(effect);
                }
            }
            List<String> favorites = playerSection.getStringList("favorites");
            for (String effectKey : favorites) {
                EffectType effect = EffectType.fromConfigKey(effectKey);
                if (effect != null) {
                    data.addFavorite(effect);
                }
            }
            data.setLastUsed(playerSection.getLong("last-used", System.currentTimeMillis()));
            data.setTotalKills(playerSection.getInt("total-kills", 0));
            data.setViewingEffects(playerSection.getBoolean("viewing-effects", true));
            
            ConfigurationSection effectStats = playerSection.getConfigurationSection("effect-stats");
            if (effectStats != null) {
                for (String effectKey : effectStats.getKeys(false)) {
                    EffectType effect = EffectType.fromConfigKey(effectKey);
                    if (effect != null) {
                        data.setEffectUsageCount(effect, effectStats.getInt(effectKey, 0));
                    }
                }
            }
        } else {
            for (EffectType effect : EffectType.values()) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (!effect.requiresPermission() || (player != null && hasEffectPermission(player, effect))) {
                    data.addUnlockedEffect(effect);
                }
            }
            EffectType defaultEffect = plugin.getDefaultEffect();
            if (defaultEffect != null && data.hasUnlockedEffect(defaultEffect)) {
                data.setSelectedEffect(defaultEffect);
            }
        }
        
        playerDataCache.put(uuid, data);
        return data;
    }
    public void savePlayerData(UUID uuid) {
        PlayerEffectData data = playerDataCache.get(uuid);
        if (data == null) return;
        
        String path = uuid.toString();
        
        storageHandler.getPlayerDataConfig().set(path + ".name", data.getName());
        storageHandler.getPlayerDataConfig().set(path + ".selected-effect", 
                data.getSelectedEffect() != null ? data.getSelectedEffect().getConfigKey() : "none");
        List<String> unlockedEffects = new ArrayList<>();
        for (EffectType effect : data.getUnlockedEffects()) {
            unlockedEffects.add(effect.getConfigKey());
        }
        storageHandler.getPlayerDataConfig().set(path + ".unlocked-effects", unlockedEffects);
        List<String> favorites = new ArrayList<>();
        for (EffectType effect : data.getFavorites()) {
            favorites.add(effect.getConfigKey());
        }
        storageHandler.getPlayerDataConfig().set(path + ".favorites", favorites);
        storageHandler.getPlayerDataConfig().set(path + ".last-used", data.getLastUsed());
        storageHandler.getPlayerDataConfig().set(path + ".total-kills", data.getTotalKills());
        storageHandler.getPlayerDataConfig().set(path + ".viewing-effects", data.isViewingEffects());
        for (Map.Entry<EffectType, Integer> entry : data.getEffectStats().entrySet()) {
            storageHandler.getPlayerDataConfig().set(path + ".effect-stats." + entry.getKey().getConfigKey(), entry.getValue());
        }
        storageHandler.savePlayerData();
    }
    public void saveAllPlayerData() {
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerData(uuid);
        }
        plugin.logDebug("Saved data for " + playerDataCache.size() + " players");
    }
    public PlayerEffectData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }
    public PlayerEffectData getPlayerData(UUID uuid, String name) {
        PlayerEffectData data = playerDataCache.get(uuid);
        if (data == null) {
            data = loadPlayerData(uuid);
        }
        if (name != null && !name.equals(data.getName())) {
            data.setName(name);
        }
        
        return data;
    }
    public boolean hasEffectPermission(Player player, EffectType effect) {
        if (!effect.requiresPermission()) {
            return true;
        }
        if (player == null) {
            return false;
        }
        return player.hasPermission(effect.getPermissionNode()) || player.hasPermission("killeffects.use.*");
    }
    public void unlockEffect(UUID uuid, EffectType effect) {
        PlayerEffectData data = getPlayerData(uuid, null);
        data.addUnlockedEffect(effect);
        savePlayerData(uuid);
    }
    public void lockEffect(UUID uuid, EffectType effect) {
        PlayerEffectData data = playerDataCache.get(uuid);
        if (data != null) {
            data.removeUnlockedEffect(effect);
            data.removeFavorite(effect);
            if (data.getSelectedEffect() == effect) {
                data.setSelectedEffect(null);
            }
            
            savePlayerData(uuid);
        }
    }
    public void recordKill(UUID uuid, EffectType effect) {
        PlayerEffectData data = getPlayerData(uuid, null);
        data.incrementTotalKills();
        if (effect != null) {
            data.incrementEffectUsage(effect);
        }
        data.setLastUsed(System.currentTimeMillis());
        savePlayerData(uuid);
    }
    public List<PlayerEffectData> getTopPlayersByKills(int limit) {
        return playerDataCache.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalKills(), a.getTotalKills()))
                .limit(limit)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    public int getTotalPlayers() {
        return playerDataCache.size();
    }
    public void clearPlayerCache(UUID uuid) {
        playerDataCache.remove(uuid);
    }
    public static class PlayerEffectData {
        private final UUID uuid;
        private String name;
        private EffectType selectedEffect;
        private final Set<EffectType> unlockedEffects;
        private final Set<EffectType> favorites;
        private final Map<EffectType, Integer> effectStats;
        private long lastUsed;
        private int totalKills;
        private boolean viewingEffects;
        
        public PlayerEffectData(UUID uuid) {
            this.uuid = uuid;
            this.name = "Unknown";
            this.selectedEffect = null;
            this.unlockedEffects = new HashSet<>();
            this.favorites = new HashSet<>();
            this.effectStats = new HashMap<>();
            this.lastUsed = System.currentTimeMillis();
            this.totalKills = 0;
            this.viewingEffects = true;
        }
        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public EffectType getSelectedEffect() { return selectedEffect; }
        public void setSelectedEffect(EffectType selectedEffect) { this.selectedEffect = selectedEffect; }
        
        public Set<EffectType> getUnlockedEffects() { return new HashSet<>(unlockedEffects); }
        public void addUnlockedEffect(EffectType effect) { unlockedEffects.add(effect); }
        public void removeUnlockedEffect(EffectType effect) { unlockedEffects.remove(effect); }
        public boolean hasUnlockedEffect(EffectType effect) { return unlockedEffects.contains(effect); }
        
        public Set<EffectType> getFavorites() { return new HashSet<>(favorites); }
        public void addFavorite(EffectType effect) { favorites.add(effect); }
        public void removeFavorite(EffectType effect) { favorites.remove(effect); }
        public boolean isFavorite(EffectType effect) { return favorites.contains(effect); }
        public boolean canAddFavorite() { return favorites.size() < LightKillEffects.getInstance().getConfig().getInt("general.max-favorites", 5); }
        
        public Map<EffectType, Integer> getEffectStats() { return new HashMap<>(effectStats); }
        public int getEffectUsageCount(EffectType effect) { return effectStats.getOrDefault(effect, 0); }
        public void setEffectUsageCount(EffectType effect, int count) { effectStats.put(effect, count); }
        public void incrementEffectUsage(EffectType effect) { effectStats.put(effect, getEffectUsageCount(effect) + 1); }
        
        public long getLastUsed() { return lastUsed; }
        public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }
        
        public int getTotalKills() { return totalKills; }
        public void setTotalKills(int totalKills) { this.totalKills = totalKills; }
        public void incrementTotalKills() { this.totalKills++; }

        public boolean isViewingEffects() { return viewingEffects; }
        public void setViewingEffects(boolean viewingEffects) { this.viewingEffects = viewingEffects; }
        public boolean toggleViewingEffects() {
            this.viewingEffects = !this.viewingEffects;
            return this.viewingEffects;
        }
    }
}
