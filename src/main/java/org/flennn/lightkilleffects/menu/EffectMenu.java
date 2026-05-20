package org.flennn.lightkilleffects.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.flennn.lightkilleffects.LightKillEffects;
import org.flennn.lightkilleffects.effect.EffectType;
import org.flennn.lightkilleffects.storage.PlayerData;

import java.util.*;
public class EffectMenu implements Listener {
    
    private final LightKillEffects plugin;
    private final Map<UUID, MenuSession> activeSessions;
    private final Map<UUID, Long> guiClickCooldowns;
    private final Map<UUID, Long> previewCooldowns;
    private static final int GUI_SIZE = 54;
    private static final int[] EFFECT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int CATEGORY_SLOT = 49;
    private static final int CURRENT_EFFECT_SLOT = 4;
    private static final int FAVORITES_SLOT = 0;
    private static final int INFO_SLOT = 8;
    
    public EffectMenu(LightKillEffects plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
        this.guiClickCooldowns = new HashMap<>();
        this.previewCooldowns = new HashMap<>();
    }
    public void openMainMenu(Player player) {
        MenuSession session = new MenuSession(player, MenuType.MAIN, 0);
        activeSessions.put(player.getUniqueId(), session);
        
        Inventory inventory = createMainMenu(player, session);
        player.openInventory(inventory);
        if (plugin.getConfig().getBoolean("gui.gui-sounds", true)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }
    public void openCategoryMenu(Player player, String category) {
        if (plugin.isDebugMode()) {
            plugin.logDebug("openCategoryMenu called for player " + player.getName() + " with category: " + category);
        }
        
        MenuSession session = new MenuSession(player, MenuType.CATEGORY, 0);
        session.setCurrentCategory(category);
        activeSessions.put(player.getUniqueId(), session);
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("Updated session for player " + player.getName() + " to CATEGORY menu for: " + category);
            plugin.logDebug("Active sessions count: " + activeSessions.size());
        }
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("Creating category menu for: " + category);
        }
        
        Inventory inventory = createCategoryMenu(player, session);
        
        if (inventory == null) {
            if (plugin.isDebugMode()) {
                plugin.logDebug("Category menu inventory is null!");
            }
            return;
        }
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("Opening category inventory - attempting to open new GUI");
        }
        
        player.openInventory(inventory);
        
        if (plugin.getConfig().getBoolean("gui.gui-sounds", true)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("Category menu opened successfully");
        }
    }
    public void openFavoritesMenu(Player player) {
        MenuSession session = new MenuSession(player, MenuType.FAVORITES, 0);
        activeSessions.put(player.getUniqueId(), session);
        
        Inventory inventory = createFavoritesMenu(player, session);
        player.openInventory(inventory);
        
        if (plugin.getConfig().getBoolean("gui.gui-sounds", true)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }
    private Inventory createMainMenu(Player player, MenuSession session) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&6&lKill Effects"));
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, net.kyori.adventure.text.Component.text(title));
        
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        fillBorder(inventory);
        ItemStack currentEffectItem = createCurrentEffectItem(playerData);
        inventory.setItem(CURRENT_EFFECT_SLOT, currentEffectItem);
        ItemStack favoritesItem = createFavoritesItem(playerData);
        inventory.setItem(FAVORITES_SLOT, favoritesItem);
        ItemStack infoItem = createInfoItem(playerData);
        inventory.setItem(INFO_SLOT, infoItem);
        org.bukkit.configuration.ConfigurationSection categoriesSection = plugin.getCategoriesConfig().getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.logInfo("&cWarning: Categories section is missing from config.yml!");
            return inventory;
        }
        
        Set<String> categories = categoriesSection.getKeys(false);
        List<String> categoryList = new ArrayList<>(categories);
        int categoriesPerPage = EFFECT_SLOTS.length;
        int startIndex = session.getCurrentPage() * categoriesPerPage;
        int endIndex = Math.min(startIndex + categoriesPerPage, categoryList.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String category = categoryList.get(i);
            ItemStack categoryItem = createCategoryItem(category, playerData);
            inventory.setItem(EFFECT_SLOTS[i - startIndex], categoryItem);
        }
        addPaginationControls(inventory, session, categoryList.size(), categoriesPerPage);
        
        return inventory;
    }
    private Inventory createCategoryMenu(Player player, MenuSession session) {
        String category = session.getCurrentCategory();
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("createCategoryMenu: Creating menu for category: " + category);
        }
        
        String categoryName = plugin.getCategoriesConfig().getString("categories." + category + ".name", category);
        String title = ChatColor.translateAlternateColorCodes('&', categoryName);
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("createCategoryMenu: Category name: " + categoryName + ", Title: " + title);
        }
        
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, net.kyori.adventure.text.Component.text(title));
        
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        fillBorder(inventory);
        ItemStack backItem = createBackItem();
        inventory.setItem(45, backItem);
        List<String> effectKeys = plugin.getCategoriesConfig().getStringList("categories." + category + ".effects");
        List<EffectType> effects = new ArrayList<>();
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("createCategoryMenu: Effect keys for " + category + ": " + effectKeys);
        }
        
        for (String key : effectKeys) {
            EffectType effect = EffectType.fromConfigKey(key);
            if (effect != null) {
                effects.add(effect);
                if (plugin.isDebugMode()) {
                    plugin.logDebug("createCategoryMenu: Added effect: " + effect.getDisplayName());
                }
            } else {
                if (plugin.isDebugMode()) {
                    plugin.logDebug("createCategoryMenu: Effect not found for key: " + key);
                }
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("createCategoryMenu: Total effects loaded: " + effects.size());
        }
        int effectsPerPage = EFFECT_SLOTS.length;
        int startIndex = session.getCurrentPage() * effectsPerPage;
        int endIndex = Math.min(startIndex + effectsPerPage, effects.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            EffectType effect = effects.get(i);
            ItemStack effectItem = createEffectItem(effect, playerData);
            inventory.setItem(EFFECT_SLOTS[i - startIndex], effectItem);
            
            if (plugin.isDebugMode()) {
                plugin.logDebug("createCategoryMenu: Added effect " + effect.getDisplayName() + " to slot " + EFFECT_SLOTS[i - startIndex]);
            }
        }
        addPaginationControls(inventory, session, effects.size(), effectsPerPage);
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("createCategoryMenu: Category menu created successfully with " + (endIndex - startIndex) + " effects");
        }
        
        return inventory;
    }
    private Inventory createFavoritesMenu(Player player, MenuSession session) {
        String title = text("gui.menus.favorites.title", "&6&lFavorite Effects");
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, net.kyori.adventure.text.Component.text(title));
        
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        fillBorder(inventory);
        ItemStack backItem = createBackItem();
        inventory.setItem(45, backItem);
        List<EffectType> favorites = new ArrayList<>(playerData.getFavorites());
        
        if (favorites.isEmpty()) {
            ItemStack noFavoritesItem = menuItem("gui.menus.favorites.empty-item", Material.BARRIER,
                    "&cNo Favorite Effects",
                    Arrays.asList("&7You haven't added any effects", "&7to your favorites yet!", "", "&eRight-click effects in categories", "&eto add them to favorites"));
            inventory.setItem(22, noFavoritesItem);
        } else {
            int effectsPerPage = EFFECT_SLOTS.length;
            int startIndex = session.getCurrentPage() * effectsPerPage;
            int endIndex = Math.min(startIndex + effectsPerPage, favorites.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                EffectType effect = favorites.get(i);
                ItemStack effectItem = createEffectItem(effect, playerData);
                ItemMeta meta = effectItem.getItemMeta();
                List<String> lore = meta.getLore();
                lore.add("");
                lore.add(text("gui.menus.effect-item.favorite-label", "&6* Favorite Effect"));
                lore.add(text("gui.menus.effect-item.remove-favorite-action", "&7Right-click to remove from favorites"));
                meta.setLore(lore);
                effectItem.setItemMeta(meta);
                
                inventory.setItem(EFFECT_SLOTS[i - startIndex], effectItem);
            }
            addPaginationControls(inventory, session, favorites.size(), effectsPerPage);
        }
        
        return inventory;
    }
    private ItemStack createEffectItem(EffectType effect, PlayerData.PlayerEffectData playerData) {
        Material iconMaterial = effect.getIconMaterial();
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        String displayName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", 
                effect.getDisplayName());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        List<String> lore = new ArrayList<>();
        List<String> description = plugin.getEffectsConfig().getStringList("effects." + effect.getConfigKey() + ".description");
        for (String line : description) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        
        lore.add("");
        boolean unlocked = playerData.hasUnlockedEffect(effect);
        Player onlinePlayer = plugin.getServer().getPlayer(playerData.getUuid());
        boolean hasPermission = onlinePlayer != null && plugin.getPlayerData().hasEffectPermission(onlinePlayer, effect);
        boolean selected = effect == playerData.getSelectedEffect();
        boolean favorite = playerData.isFavorite(effect);
        
        if (selected) {
            lore.add(text("gui.menus.effect-item.selected", "&aSelected"));
        } else if (!unlocked) {
            lore.add(text("gui.menus.effect-item.locked", "&cLocked"));
            if (!hasPermission) {
                lore.add(text("gui.menus.effect-item.requires-permission", "&cRequires permission"));
            }
        } else {
            lore.add(text("gui.menus.effect-item.select-action", "&eClick to select"));
        }
        
        if (favorite) {
            lore.add(text("gui.menus.effect-item.favorite", "&6Favorite"));
        }
        int usage = playerData.getEffectUsageCount(effect);
        if (usage > 0) {
            lore.add(text("gui.menus.effect-item.usage", "&7Used {usage} times").replace("{usage}", String.valueOf(usage)));
        }
        
        lore.add("");
        lore.add(text("gui.menus.effect-item.left-click", "&7Left-click: Select effect"));
        if (unlocked) {
            lore.add(text("gui.menus.effect-item.middle-click", "&7Middle-click: Preview effect"));
            if (favorite) {
                lore.add(text("gui.menus.effect-item.right-click-remove", "&7Right-click: Remove from favorites"));
            } else if (playerData.canAddFavorite()) {
                lore.add(text("gui.menus.effect-item.right-click-add", "&7Right-click: Add to favorites"));
            }
        }
        
        meta.setLore(lore);
        if (selected) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        if (!activeSessions.containsKey(playerId)) {
            if (plugin.isDebugMode()) {
                plugin.logDebug("No active session for player " + player.getName() + " - Current inventory: " + 
                              (event.getView().getTitle() != null ? event.getView().getTitle() : "null"));
            }
            return;
        }
        event.setCancelled(true);
        if (isOnGUICooldown(player)) {
            long remaining = getRemainingGUICooldown(player);
            if (plugin.isDebugMode()) {
                plugin.logDebug("Player " + player.getName() + " on GUI cooldown - " + remaining + "ms remaining");
            }
            return;
        }
        setGUICooldown(player);
        MenuSession session = activeSessions.get(playerId);
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getSlot();
        if (plugin.isDebugMode()) {
            plugin.logDebug("GUI Click - Player: " + player.getName() + 
                          ", Menu: " + session.getMenuType() + 
                          ", Slot: " + slot + 
                          ", Item: " + (clickedItem != null ? clickedItem.getType() : "null"));
        }
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        try {
            switch (session.getMenuType()) {
                case MAIN:
                    handleMainMenuClick(player, session, slot, event);
                    break;
                case CATEGORY:
                    handleCategoryMenuClick(player, session, slot, event);
                    break;
                case FAVORITES:
                    handleFavoritesMenuClick(player, session, slot, event);
                    break;
            }
            if (plugin.getConfig().getBoolean("gui.gui-sounds", true)) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling GUI click for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("Inventory close event for " + player.getName() + " - Inventory: " + event.getView().getTitle());
        }
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String currentTitle = player.getOpenInventory().getTitle();
            String mainTitle = plugin.getConfig().getString("gui.title", "&6&lKill Effects");
            String mainTitleStripped = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', mainTitle));
            
            boolean hasPluginGUIOpen = false;
            if (currentTitle != null) {
                String currentTitleStripped = org.bukkit.ChatColor.stripColor(currentTitle);
                if (currentTitleStripped.equals(mainTitleStripped)) {
                    hasPluginGUIOpen = true;
                }
                if (!hasPluginGUIOpen) {
                    org.bukkit.configuration.ConfigurationSection categoriesSection = plugin.getCategoriesConfig().getConfigurationSection("categories");
                    if (categoriesSection != null) {
                        for (String category : categoriesSection.getKeys(false)) {
                            String categoryTitle = plugin.getCategoriesConfig().getString("categories." + category + ".name", category);
                            String categoryTitleStripped = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', categoryTitle));
                            if (currentTitleStripped.equals(categoryTitleStripped)) {
                                hasPluginGUIOpen = true;
                                break;
                            }
                        }
                    }
                }
                if (!hasPluginGUIOpen && currentTitleStripped.equals(org.bukkit.ChatColor.stripColor(text("gui.menus.favorites.title", "&6&lFavorite Effects")))) {
                    hasPluginGUIOpen = true;
                }
            }
            
            if (!hasPluginGUIOpen) {
                activeSessions.remove(playerId);
                if (plugin.isDebugMode()) {
                    plugin.logDebug("Removed session for " + player.getName() + " - no plugin GUI open");
                }
            } else {
                if (plugin.isDebugMode()) {
                    plugin.logDebug("Keeping session for " + player.getName() + " - plugin GUI still open: " + currentTitle);
                }
            }
        }, 1L);
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        if (activeSessions.containsKey(playerId)) {
            event.setCancelled(true);
        }
    }

    private String text(String path, String fallback) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(path, fallback));
    }

    private List<String> textList(String path, List<String> fallback) {
        List<String> raw = plugin.getConfig().getStringList(path);
        if (raw.isEmpty()) {
            raw = fallback;
        }

        List<String> colored = new ArrayList<>();
        for (String line : raw) {
            colored.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return colored;
    }

    private Material material(String path, Material fallback) {
        String value = plugin.getConfig().getString(path, fallback.name());
        try {
            return Material.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return fallback;
        }
    }

    private ItemStack menuItem(String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore) {
        ItemStack item = new ItemStack(material(path + ".material", fallbackMaterial));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(text(path + ".name", fallbackName));
        meta.setLore(textList(path + ".lore", fallbackLore));
        item.setItemMeta(meta);
        return item;
    }
    
    private void fillBorder(Inventory inventory) {
        ItemStack borderItem = new ItemStack(material("gui.menus.border.material", Material.GRAY_STAINED_GLASS_PANE));
        ItemMeta meta = borderItem.getItemMeta();
        meta.setDisplayName(text("gui.menus.border.name", "&8 "));
        borderItem.setItemMeta(meta);
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, borderItem);
            if (inventory.getItem(i + 45) == null) inventory.setItem(i + 45, borderItem);
        }
        for (int i = 1; i < 5; i++) {
            if (inventory.getItem(i * 9) == null) inventory.setItem(i * 9, borderItem);
            if (inventory.getItem(i * 9 + 8) == null) inventory.setItem(i * 9 + 8, borderItem);
        }
    }
    
    private ItemStack createCurrentEffectItem(PlayerData.PlayerEffectData playerData) {
        EffectType currentEffect = playerData.getSelectedEffect();
        if (currentEffect == null) {
            return menuItem("gui.menus.current-effect.empty-item", Material.BARRIER,
                    "&cNo Effect Selected",
                    Arrays.asList("&7You don't have an effect selected", "&7Click on effects below to select one."));
        }
        return createEffectItem(currentEffect, playerData);
    }
    
    private ItemStack createFavoritesItem(PlayerData.PlayerEffectData playerData) {
        ItemStack item = new ItemStack(material("gui.menus.favorites.item.material", Material.NETHER_STAR));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(text("gui.menus.favorites.item.name", "&6Favorite Effects"));
        List<String> lore = textList("gui.menus.favorites.item.lore",
                Arrays.asList("&7View your favorite effects", "&7({favorites}/{max_favorites} favorites)", "", "&eClick to open favorites menu"));
        lore.replaceAll(line -> line
                .replace("{favorites}", String.valueOf(playerData.getFavorites().size()))
                .replace("{max_favorites}", String.valueOf(plugin.getConfig().getInt("general.max-favorites", 5))));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createInfoItem(PlayerData.PlayerEffectData playerData) {
        ItemStack item = new ItemStack(material("gui.menus.info.item.material", Material.BOOK));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(text("gui.menus.info.item.name", "&bYour Statistics"));
        List<String> lore = textList("gui.menus.info.item.lore",
                Arrays.asList("&7Total kills: &f{kills}", "&7Unlocked effects: &f{unlocked}/{total}", "&7Favorites: &f{favorites}", "", "&eClick for detailed statistics"));
        lore.replaceAll(line -> line
                .replace("{kills}", String.valueOf(playerData.getTotalKills()))
                .replace("{unlocked}", String.valueOf(playerData.getUnlockedEffects().size()))
                .replace("{total}", String.valueOf(EffectType.values().length))
                .replace("{favorites}", String.valueOf(playerData.getFavorites().size())));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }    private ItemStack createCategoryItem(String category, PlayerData.PlayerEffectData playerData) {
        String iconMaterialName = plugin.getCategoriesConfig().getString("categories." + category + ".icon", "STONE");
        Material iconMaterial;
        try {
            iconMaterial = Material.valueOf(iconMaterialName);
        } catch (IllegalArgumentException e) {
            iconMaterial = Material.STONE;
        }
        
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = plugin.getCategoriesConfig().getString("categories." + category + ".name", category);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        
        List<String> effectKeys = plugin.getCategoriesConfig().getStringList("categories." + category + ".effects");
        List<String> lore = new ArrayList<>();
        
        lore.add(text("gui.menus.category-item.effects-count", "&7Effects: &f{count}").replace("{count}", String.valueOf(effectKeys.size())));
        int unlockedCount = 0;
        for (String effectKey : effectKeys) {
            EffectType effect = EffectType.fromConfigKey(effectKey);
            if (effect != null && playerData.hasUnlockedEffect(effect)) {
                unlockedCount++;
            }
        }
        
        lore.add(text("gui.menus.category-item.unlocked-count", "&7Unlocked: &f{unlocked}/{total}").replace("{unlocked}", String.valueOf(unlockedCount)).replace("{total}", String.valueOf(effectKeys.size())));
        lore.add("");
        lore.add(text("gui.menus.category-item.action", "&eClick to browse effects"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBackItem() {
        return menuItem("gui.menus.back-item", Material.ARROW, "&aBack", Arrays.asList("&7Return to main menu"));
    }    private void addPaginationControls(Inventory inventory, MenuSession session, int totalItems, int itemsPerPage) {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        
        if (session.getCurrentPage() > 0) {
            ItemStack prevItem = new ItemStack(material("gui.menus.previous-page.material", Material.ARROW));
            ItemMeta meta = prevItem.getItemMeta();
            meta.setDisplayName(text("gui.menus.previous-page.name", "&aPrevious Page"));
            List<String> lore = textList("gui.menus.previous-page.lore", Arrays.asList("&7Page {page}/{pages}"));
            lore.replaceAll(line -> line
                    .replace("{page}", String.valueOf(session.getCurrentPage()))
                    .replace("{pages}", String.valueOf(totalPages)));
            meta.setLore(lore);
            prevItem.setItemMeta(meta);
            inventory.setItem(PREV_PAGE_SLOT, prevItem);
        }
        
        if (session.getCurrentPage() < totalPages - 1) {
            ItemStack nextItem = new ItemStack(material("gui.menus.next-page.material", Material.ARROW));
            ItemMeta meta = nextItem.getItemMeta();
            meta.setDisplayName(text("gui.menus.next-page.name", "&aNext Page"));
            List<String> lore = textList("gui.menus.next-page.lore", Arrays.asList("&7Page {page}/{pages}"));
            lore.replaceAll(line -> line
                    .replace("{page}", String.valueOf(session.getCurrentPage() + 2))
                    .replace("{pages}", String.valueOf(totalPages)));
            meta.setLore(lore);
            nextItem.setItemMeta(meta);
            inventory.setItem(NEXT_PAGE_SLOT, nextItem);
        }
    }
    private void handleMainMenuClick(Player player, MenuSession session, int slot, InventoryClickEvent event) {
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        if (plugin.isDebugMode()) {
            plugin.logDebug("Main menu click - Slot: " + slot + ", Special slots: FAVORITES=" + FAVORITES_SLOT + 
                          ", INFO=" + INFO_SLOT + ", PREV=" + PREV_PAGE_SLOT + ", NEXT=" + NEXT_PAGE_SLOT);
        }
        if (slot == FAVORITES_SLOT) {
            if (plugin.isDebugMode()) plugin.logDebug("Opening favorites menu");
            openFavoritesMenu(player);
            return;
        }
        
        if (slot == INFO_SLOT) {
            player.closeInventory();
            player.sendMessage(plugin.getMessage("current-effect", "effect", 
                    playerData.getSelectedEffect() != null ? playerData.getSelectedEffect().getDisplayName() : "None"));
            player.sendMessage(plugin.getMessage("effects-available", "count", 
                    String.valueOf(playerData.getUnlockedEffects().size())));
            return;
        }
        if (slot == PREV_PAGE_SLOT && session.getCurrentPage() > 0) {
            session.setCurrentPage(session.getCurrentPage() - 1);
            player.openInventory(createMainMenu(player, session));
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT) {
            org.bukkit.configuration.ConfigurationSection categoriesSection = plugin.getCategoriesConfig().getConfigurationSection("categories");
            if (categoriesSection == null) return;
            
            Set<String> categories = categoriesSection.getKeys(false);
            int totalPages = (int) Math.ceil((double) categories.size() / EFFECT_SLOTS.length);
            if (session.getCurrentPage() < totalPages - 1) {
                session.setCurrentPage(session.getCurrentPage() + 1);
                player.openInventory(createMainMenu(player, session));
            }
            return;
        }
        for (int i = 0; i < EFFECT_SLOTS.length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                if (plugin.isDebugMode()) {
                    plugin.logDebug("Effect slot clicked: " + slot + " (index " + i + ")");
                }
                
                org.bukkit.configuration.ConfigurationSection categoriesSection = plugin.getCategoriesConfig().getConfigurationSection("categories");
                if (categoriesSection == null) {
                    if (plugin.isDebugMode()) {
                        plugin.logDebug("Categories section is null!");
                    }
                    return;
                }
                
                Set<String> categories = categoriesSection.getKeys(false);
                List<String> categoryList = new ArrayList<>(categories);
                
                if (plugin.isDebugMode()) {
                    plugin.logDebug("Available categories: " + categoryList);
                }
                
                int categoryIndex = (session.getCurrentPage() * EFFECT_SLOTS.length) + i;
                if (categoryIndex < categoryList.size()) {
                    String category = categoryList.get(categoryIndex);
                    if (plugin.isDebugMode()) {
                        plugin.logDebug("Opening category: " + category);
                    }
                    openCategoryMenu(player, category);
                } else {
                    if (plugin.isDebugMode()) {
                        plugin.logDebug("Category index out of bounds: " + categoryIndex + " >= " + categoryList.size());
                    }
                }
                return;
            }
        }
    }
    
    private void handleCategoryMenuClick(Player player, MenuSession session, int slot, InventoryClickEvent event) {
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("Category menu click - Player: " + player.getName() + ", Slot: " + slot + 
                          ", Category: " + session.getCurrentCategory());
        }
        if (slot == 45) {
            if (plugin.isDebugMode()) {
                plugin.logDebug("Back button clicked, opening main menu");
            }
            openMainMenu(player);
            return;
        }
        if (slot == PREV_PAGE_SLOT && session.getCurrentPage() > 0) {
            session.setCurrentPage(session.getCurrentPage() - 1);
            player.openInventory(createCategoryMenu(player, session));
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT) {
            List<String> effectKeys = plugin.getCategoriesConfig().getStringList("categories." + session.getCurrentCategory() + ".effects");
            int totalPages = (int) Math.ceil((double) effectKeys.size() / EFFECT_SLOTS.length);
            if (session.getCurrentPage() < totalPages - 1) {
                session.setCurrentPage(session.getCurrentPage() + 1);
                player.openInventory(createCategoryMenu(player, session));
            }
            return;
        }
        for (int i = 0; i < EFFECT_SLOTS.length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                if (plugin.isDebugMode()) {
                    plugin.logDebug("Effect slot clicked in category menu: " + slot + " (index " + i + ")");
                }
                
                List<String> effectKeys = plugin.getCategoriesConfig().getStringList("categories." + session.getCurrentCategory() + ".effects");
                List<EffectType> effects = new ArrayList<>();
                
                for (String key : effectKeys) {
                    EffectType effect = EffectType.fromConfigKey(key);
                    if (effect != null) {
                        effects.add(effect);
                    }
                }
                
                if (plugin.isDebugMode()) {
                    plugin.logDebug("Available effects in category: " + effects.size());
                }
                
                int effectIndex = (session.getCurrentPage() * EFFECT_SLOTS.length) + i;
                if (effectIndex < effects.size()) {
                    EffectType effect = effects.get(effectIndex);
                    if (plugin.isDebugMode()) {
                        plugin.logDebug("Handling effect click for: " + effect.getDisplayName());
                    }
                    handleEffectClick(player, playerData, effect, event);
                } else {
                    if (plugin.isDebugMode()) {
                        plugin.logDebug("Effect index out of bounds: " + effectIndex + " >= " + effects.size());
                    }
                }
                return;
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.logDebug("No handler found for category menu slot: " + slot);
        }
    }
    
    private void handleFavoritesMenuClick(Player player, MenuSession session, int slot, InventoryClickEvent event) {
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        if (slot == 45) {
            openMainMenu(player);
            return;
        }
        if (slot == PREV_PAGE_SLOT && session.getCurrentPage() > 0) {
            session.setCurrentPage(session.getCurrentPage() - 1);
            player.openInventory(createFavoritesMenu(player, session));
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT) {
            List<EffectType> favorites = new ArrayList<>(playerData.getFavorites());
            int totalPages = (int) Math.ceil((double) favorites.size() / EFFECT_SLOTS.length);
            if (session.getCurrentPage() < totalPages - 1) {
                session.setCurrentPage(session.getCurrentPage() + 1);
                player.openInventory(createFavoritesMenu(player, session));
            }
            return;
        }
        for (int i = 0; i < EFFECT_SLOTS.length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                List<EffectType> favorites = new ArrayList<>(playerData.getFavorites());
                
                int effectIndex = (session.getCurrentPage() * EFFECT_SLOTS.length) + i;
                if (effectIndex < favorites.size()) {
                    EffectType effect = favorites.get(effectIndex);
                    handleEffectClick(player, playerData, effect, event);
                }
                return;
            }
        }
    }
    
    private void handleEffectClick(Player player, PlayerData.PlayerEffectData playerData, EffectType effect, InventoryClickEvent event) {
        if (event.isLeftClick()) {
            if (!playerData.hasUnlockedEffect(effect)) {
                player.sendMessage(plugin.getMessage("effect-locked", "effect", effect.getDisplayName()));
                return;
            }
            
            if (!plugin.getPlayerData().hasEffectPermission(player, effect)) {
                player.sendMessage(plugin.getMessage("effect-no-permission", "effect", effect.getDisplayName()));
                return;
            }
            
            playerData.setSelectedEffect(effect);
            plugin.getPlayerData().savePlayerData(player.getUniqueId());
            
            String effectName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", effect.getDisplayName());
            player.sendMessage(plugin.getMessage("effect-set", "effect", effectName));
            if (plugin.getConfig().getBoolean("gui.gui-sounds", true)) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
            
        } else if (event.isRightClick()) {
            if (!playerData.hasUnlockedEffect(effect)) {
                player.sendMessage(plugin.getMessage("effect-locked", "effect", effect.getDisplayName()));
                return;
            }
            
            String effectName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", effect.getDisplayName());
            
            if (playerData.isFavorite(effect)) {
                playerData.removeFavorite(effect);
                player.sendMessage(plugin.getMessage("favorite-removed", "effect", effectName));
            } else {
                if (playerData.canAddFavorite()) {
                    playerData.addFavorite(effect);
                    player.sendMessage(plugin.getMessage("favorite-added", "effect", effectName));
                } else {
                    player.sendMessage(plugin.getMessage("favorites-full"));
                }
            }
            
            plugin.getPlayerData().savePlayerData(player.getUniqueId());
            
        } else if (event.isShiftClick() || event.getClick() == ClickType.MIDDLE) {
            if (!plugin.getPermissionManager().canPreview(player)) {
                return;
            }
            
            if (plugin.getPreviewManager().isOnCooldown(player)) {
                long remaining = plugin.getPreviewManager().getRemainingCooldown(player);
                player.sendMessage(plugin.getMessage("preview-cooldown", "seconds", String.valueOf(remaining)));
                return;
            }
            
            if (player.hasPermission("killeffects.preview.all")
                    || (plugin.getPlayerData().hasEffectPermission(player, effect)
                    && (!plugin.getSettings().requirePreviewUnlock() || playerData.hasUnlockedEffect(effect)))) {
                
                if (plugin.getPreviewManager().startPreview(player, effect.getDisplayName())) {
                    org.bukkit.Location effectLoc = player.getLocation().add(
                            player.getLocation().getDirection().multiply(
                                    plugin.getConfig().getInt("gui.preview.location-offset", 3)
                            )
                    );
                    
                    plugin.getEffectManager().executeEffect(player, effectLoc, effect);
                    
                    String effectName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", effect.getDisplayName());
                    player.sendMessage(plugin.getMessage("effect-previewed", "effect", effectName));
                }
            } else {
                player.sendMessage(plugin.getMessage("no-permission"));
            }
        }
    }
    private static class MenuSession {
        private final Player player;
        private final MenuType menuType;
        private int currentPage;
        private String currentCategory;
        
        public MenuSession(Player player, MenuType menuType, int currentPage) {
            this.player = player;
            this.menuType = menuType;
            this.currentPage = currentPage;
        }
        public Player getPlayer() { return player; }
        public MenuType getMenuType() { return menuType; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public String getCurrentCategory() { return currentCategory; }
        public void setCurrentCategory(String currentCategory) { this.currentCategory = currentCategory; }
    }
    private boolean isOnGUICooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!guiClickCooldowns.containsKey(playerId)) {
            return false;
        }
        
        long cooldownEnd = guiClickCooldowns.get(playerId);
        if (System.currentTimeMillis() >= cooldownEnd) {
            guiClickCooldowns.remove(playerId);
            return false;
        }
        
        return true;
    }
    private void setGUICooldown(Player player) {
        long cooldownMs = plugin.getConfig().getLong("gui.click-cooldown-ms", 200);
        guiClickCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMs);
    }
    private long getRemainingGUICooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!guiClickCooldowns.containsKey(playerId)) {
            return 0;
        }
        
        long cooldownEnd = guiClickCooldowns.get(playerId);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    private boolean isOnPreviewCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!previewCooldowns.containsKey(playerId)) {
            return false;
        }
        
        long cooldownEnd = previewCooldowns.get(playerId);
        if (System.currentTimeMillis() >= cooldownEnd) {
            previewCooldowns.remove(playerId);
            return false;
        }
        
        return true;
    }
    private void setPreviewCooldown(Player player) {
        long cooldownSeconds = plugin.getConfig().getLong("gui.preview.cooldown", 5);
        previewCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000));
    }
    private long getRemainingPreviewCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!previewCooldowns.containsKey(playerId)) {
            return 0;
        }
        
        long cooldownEnd = previewCooldowns.get(playerId);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    public boolean isPlayerOnPreviewCooldown(Player player) {
        return isOnPreviewCooldown(player);
    }
    public void setPlayerPreviewCooldown(Player player) {
        setPreviewCooldown(player);
    }
    public long getPlayerRemainingPreviewCooldown(Player player) {
        return getRemainingPreviewCooldown(player);
    }
    
    private enum MenuType {
        MAIN, CATEGORY, FAVORITES
    }
}
