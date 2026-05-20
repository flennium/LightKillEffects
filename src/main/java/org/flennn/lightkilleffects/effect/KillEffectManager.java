package org.flennn.lightkilleffects.effect;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.flennn.lightkilleffects.LightKillEffects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all kill effect animations and particle systems
 * Handles the stunning visual effects with optimized performance
 */
public class KillEffectManager {
    
    private final LightKillEffects plugin;
    private final Map<UUID, Long> cooldowns;
    private final Map<Location, Set<Block>> temporaryBlocks;
    private final Set<BukkitTask> activeTasks;
    private final Random random;
    
    // Performance settings
    private final int maxParticlesPerEffect;
    private final int particleRenderDistance;
    private final boolean performanceMode;
    private final boolean soundsEnabled;
    
    // Particle tracking - using ThreadLocal for thread safety
    private final ThreadLocal<Integer> currentEffectParticleCount = ThreadLocal.withInitial(() -> 0);
    
    public KillEffectManager(LightKillEffects plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
        this.temporaryBlocks = new ConcurrentHashMap<>();
        this.activeTasks = ConcurrentHashMap.newKeySet();
        this.random = new Random();
        
        // Load performance settings
        this.maxParticlesPerEffect = plugin.getSettings().maxParticlesPerEffect();
        this.particleRenderDistance = plugin.getSettings().particleRenderDistance();
        this.performanceMode = plugin.getConfig().getBoolean("performance.performance-mode", false);
        this.soundsEnabled = plugin.getConfig().getBoolean("general.sounds-enabled", true);
        
        // Start cleanup task
        startCleanupTask();
        
        plugin.logDebug("KillEffectManager initialized with performance mode: " + performanceMode);
    }
    
    /**
     * Execute a kill effect at the specified location
     */
    public void executeEffect(Player killer, Location deathLocation, EffectType effect) {
        if (effect == null || deathLocation == null) return;
        
        // Check cooldown
        if (isOnCooldown(killer)) {
            long remaining = getRemainingCooldown(killer);
            killer.sendMessage(plugin.getMessage("on-cooldown", "seconds", String.valueOf(remaining)));
            return;
        }
        
        // Set cooldown
        setCooldown(killer);
        
        // Reset particle counter for this effect
        currentEffectParticleCount.set(0);
        
        // Get nearby players for particle rendering
        List<Player> nearbyPlayers = getNearbyPlayers(deathLocation);
        if (nearbyPlayers.isEmpty()) return;
        
        plugin.logDebug("Executing " + effect.getDisplayName() + " at " + formatLocation(deathLocation) + 
                       " (max particles: " + maxParticlesPerEffect + ")");
        
        // Execute the specific effect
        switch (effect) {
            case LIGHTNING_STORM:
                executeLightningStorm(deathLocation, nearbyPlayers);
                break;
            case SOLAR_EXPLOSION:
                executeSolarExplosion(deathLocation, nearbyPlayers);
                break;
            case FROZEN_BURST:
                executeFrozenBurst(deathLocation, nearbyPlayers);
                break;
            case NEON_RAVE:
                executeNeonRave(deathLocation, nearbyPlayers);
                break;
            case STARFALL_CASCADE:
                executeStarfallCascade(deathLocation, nearbyPlayers);
                break;
            case PRISMATIC_SHATTER:
                executePrismaticShatter(deathLocation, nearbyPlayers);
                break;
            case VOID_CONSUMPTION:
                executeVoidConsumption(deathLocation, nearbyPlayers);
                break;
            case PHOENIX_REBIRTH:
                executePhoenixRebirth(deathLocation, nearbyPlayers);
                break;
            case CELESTIAL_GATEWAY:
                executeCelestialGateway(deathLocation, nearbyPlayers);
                break;
            case ELECTRIC_OVERLOAD:
                executeElectricOverload(deathLocation, nearbyPlayers);
                break;
            case CRYSTAL_GARDEN:
                executeCrystalGarden(deathLocation, nearbyPlayers);
                break;
            case SPECTRAL_HAUNT:
                executeSpectralHaunt(deathLocation, nearbyPlayers);
                break;
            case LASER_LIGHT_SHOW:
                executeLaserLightShow(deathLocation, nearbyPlayers);
                break;
            case METEOR_IMPACT:
                executeMeteorImpact(deathLocation, nearbyPlayers);
                break;
            case BIOLUMINESCENT_BLOOM:
                executeBioluminescentBloom(deathLocation, nearbyPlayers);
                break;
            case TIME_FRACTURE:
                executeTimeFracture(deathLocation, nearbyPlayers);
                break;
            case DIVINE_ASCENSION:
                executeDivineAscension(deathLocation, nearbyPlayers);
                break;
            case TOXIC_MELTDOWN:
                executeToxicMeltdown(deathLocation, nearbyPlayers);
                break;
            case QUANTUM_COLLAPSE:
                executeQuantumCollapse(deathLocation, nearbyPlayers);
                break;
            case SUPERNOVA:
                executeSupernova(deathLocation, nearbyPlayers);
                break;
        }
        
        // Play sound effect
        if (soundsEnabled) {
            playSound(deathLocation, effect.getSound());
        }
        
        // Log final particle usage and cleanup
        int finalParticleCount = currentEffectParticleCount.get();
        if (plugin.isDebugMode()) {
            plugin.logDebug("Effect " + effect.getDisplayName() + " completed - Total particles used: " + 
                          finalParticleCount + "/" + maxParticlesPerEffect);
        }
        
        // Clean up ThreadLocal to prevent memory leaks
        currentEffectParticleCount.remove();
    }
    
    /**
     * Lightning Storm Effect - Multiple lightning bolts in circular patterns
     */
    private void executeLightningStorm(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 60;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Create lightning bolt pattern
                if (ticks % 12 == 0) {
                    int bolts = performanceMode ? 3 : 5;
                    for (int i = 0; i < bolts; i++) {
                        double angle = (2 * Math.PI * i) / bolts + (ticks * 0.1);
                        double radius = 2 + (ticks * 0.05);
                        
                        Location boltLoc = center.clone().add(
                                Math.cos(angle) * radius,
                                0,
                                Math.sin(angle) * radius
                        );
                        
                        // Spawn lightning effect
                        spawnParticles(viewers, boltLoc, Particle.ELECTRIC_SPARK, 20, 0.1, 2, 0.1, 0.1);
                        spawnParticles(viewers, boltLoc, Particle.FIREWORKS_SPARK, 10, 0.2, 3, 0.2, 0.0);
                        
                        // Occasional real lightning for dramatic effect
                        if (ticks % 24 == 0 && random.nextBoolean()) {
                            center.getWorld().strikeLightningEffect(boltLoc);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    /**
     * Solar Explosion Effect - Golden sphere with radiating light rays
     */
    private void executeSolarExplosion(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 80;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                double radius = 0.5 + (progress * 4);
                
                // Central golden sphere
                spawnParticles(viewers, center, Particle.FLAME, 
                        (int) (30 * (1 - progress)), 0.5, 0.5, 0.5, 0.05);
                spawnParticles(viewers, center, Particle.FIREWORKS_SPARK, 
                        (int) (20 * (1 - progress)), 0.3, 0.3, 0.3, 0.1);
                
                // Radiating light rays
                for (int i = 0; i < (performanceMode ? 8 : 16); i++) {
                    double angle = (2 * Math.PI * i) / (performanceMode ? 8 : 16);
                    
                    for (double r = 0.5; r <= radius; r += 0.3) {
                        Location rayLoc = center.clone().add(
                                Math.cos(angle) * r,
                                Math.sin(ticks * 0.1) * 0.5,
                                Math.sin(angle) * r
                        );
                        
                        spawnParticles(viewers, rayLoc, Particle.FLAME, 2, 0.1, 0.1, 0.1, 0.02);
                        if (r > 2) {
                            spawnParticles(viewers, rayLoc, Particle.LAVA, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    /**
     * Frozen Burst Effect - Ice-blue spiraling particles with shattering ice
     */
    private void executeFrozenBurst(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 70;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    // Create ice shatter effect
                    for (int i = 0; i < (performanceMode ? 50 : 100); i++) {
                        Location shatterLoc = center.clone().add(
                                (random.nextDouble() - 0.5) * 8,
                                random.nextDouble() * 3,
                                (random.nextDouble() - 0.5) * 8
                        );
                        spawnParticles(viewers, shatterLoc, Particle.BLOCK_CRACK, 3, 0.1, 0.1, 0.1, 0.1, 
                                Material.PACKED_ICE.createBlockData());
                    }
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                
                // Spiral ice particles
                for (int i = 0; i < (performanceMode ? 2 : 4); i++) {
                    double angle = (ticks * 0.2) + (i * Math.PI / 2);
                    double radius = progress * 4;
                    double height = Math.sin(ticks * 0.1) * 2;
                    
                    Location spiralLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    
                    // Create custom blue-tinted particles
                    spawnParticles(viewers, spiralLoc, Particle.REDSTONE, 5, 0.1, 0.1, 0.1, 0.0,
                            new Particle.DustOptions(Color.fromRGB(173, 216, 230), 1.5f));
                    spawnParticles(viewers, spiralLoc, Particle.SNOWFLAKE, 8, 0.2, 0.2, 0.2, 0.05);
                }
                
                // Freezing ground effect
                if (ticks % 5 == 0) {
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            Location groundLoc = center.clone().add(x, -1, z);
                            if (groundLoc.getBlock().getType().isSolid()) {
                                spawnParticles(viewers, groundLoc.add(0.5, 1, 0.5), Particle.SNOWFLAKE, 3, 0.3, 0.1, 0.3, 0.01);
                            }
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    /**
     * Neon Rave Effect - Rapidly cycling colored lights in disco fashion
     */
    private void executeNeonRave(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;
            final Color[] neonColors = {
                    Color.RED, Color.LIME, Color.BLUE, Color.YELLOW, 
                    Color.PURPLE, Color.AQUA, Color.FUCHSIA, Color.ORANGE
            };
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Strobe effect
                if (ticks % 3 == 0) {
                    Color currentColor = neonColors[ticks / 3 % neonColors.length];
                    
                    // Disco ball effect in center
                    for (int i = 0; i < (performanceMode ? 20 : 40); i++) {
                        double phi = Math.acos(1 - 2 * random.nextDouble());
                        double theta = 2 * Math.PI * random.nextDouble();
                        
                        double x = Math.sin(phi) * Math.cos(theta) * 1.5;
                        double y = Math.cos(phi) * 1.5;
                        double z = Math.sin(phi) * Math.sin(theta) * 1.5;
                        
                        Location ballLoc = center.clone().add(x, y + 2, z);
                        spawnParticles(viewers, ballLoc, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0,
                                new Particle.DustOptions(currentColor, 1.0f));
                    }
                    
                    // Ground light beams
                    for (int beam = 0; beam < (performanceMode ? 4 : 8); beam++) {
                        double angle = (2 * Math.PI * beam) / (performanceMode ? 4 : 8) + (ticks * 0.1);
                        
                        for (double r = 1; r <= 6; r += 0.5) {
                            Location beamLoc = center.clone().add(
                                    Math.cos(angle) * r,
                                    0.1,
                                    Math.sin(angle) * r
                            );
                            
                            Color beamColor = neonColors[(beam + ticks / 5) % neonColors.length];
                            spawnParticles(viewers, beamLoc, Particle.REDSTONE, 3, 0.1, 0.0, 0.1, 0.0,
                                    new Particle.DustOptions(beamColor, 2.0f));
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    /**
     * Starfall Cascade Effect - Glowing particles falling like shooting stars
     */
    private void executeStarfallCascade(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 120;
            final List<Star> stars = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Create new stars
                if (ticks % 8 == 0 && stars.size() < (performanceMode ? 15 : 30)) {
                    stars.add(new Star(
                            center.clone().add(
                                    (random.nextDouble() - 0.5) * 16,
                                    8 + random.nextDouble() * 4,
                                    (random.nextDouble() - 0.5) * 16
                            ),
                            new Vector(
                                    (random.nextDouble() - 0.5) * 0.1,
                                    -0.15 - random.nextDouble() * 0.1,
                                    (random.nextDouble() - 0.5) * 0.1
                            )
                    ));
                }
                
                // Update and render stars
                Iterator<Star> iterator = stars.iterator();
                while (iterator.hasNext()) {
                    Star star = iterator.next();
                    star.update();
                    
                    if (star.location.getY() < center.getY() - 2) {
                        // Star impact effect
                        spawnParticles(viewers, star.location, Particle.FIREWORKS_SPARK, 15, 0.3, 0.1, 0.3, 0.1);
                        spawnParticles(viewers, star.location, Particle.END_ROD, 8, 0.2, 0.2, 0.2, 0.05);
                        iterator.remove();
                    } else {
                        // Render star trail
                        spawnParticles(viewers, star.location, Particle.END_ROD, 3, 0.1, 0.1, 0.1, 0.02);
                        spawnParticles(viewers, star.location, Particle.FIREWORKS_SPARK, 2, 0.05, 0.05, 0.05, 0.01);
                        
                        // Trail effect
                        Location trailLoc = star.location.clone().subtract(star.velocity.clone().multiply(3));
                        spawnParticles(viewers, trailLoc, Particle.END_ROD, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    // Continue with more effect implementations...
    // Due to length constraints, I'll implement the remaining effects in the next part
    
    /**
     * Prismatic Shatter Effect - Rainbow glass particles with beacon
     */
    private void executePrismaticShatter(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 60;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                
                // Rainbow shards expanding outward
                for (int i = 0; i < (performanceMode ? 50 : 100); i++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double radius = progress * 5;
                    double height = random.nextDouble() * 3;
                    
                    Location shardLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    
                    // Rainbow colors
                    Color[] rainbowColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.GREEN, Color.AQUA, Color.BLUE, Color.PURPLE};
                    Color rainbowColor = rainbowColors[(ticks + i * 10) % rainbowColors.length];
                    
                    spawnParticles(viewers, shardLoc, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0,
                            new Particle.DustOptions(rainbowColor, 1.0f));
                    spawnParticles(viewers, shardLoc, Particle.FIREWORKS_SPARK, 2, 0.1, 0.1, 0.1, 0.05);
                }
                
                // Central beacon effect
                if (ticks < 40) {
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.END_ROD, 10, 0.2, 1, 0.2, 0.1);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    // Helper methods
    
    private void spawnParticles(List<Player> viewers, Location location, Particle particle, int count, 
                               double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticles(viewers, location, particle, count, offsetX, offsetY, offsetZ, extra, null);
    }
    
    private void spawnParticles(List<Player> viewers, Location location, Particle particle, int count, 
                               double offsetX, double offsetY, double offsetZ, double extra, Object data) {
        if (location == null || location.getWorld() == null || count <= 0) {
            return;
        }

        // Check if we've exceeded the total particle limit for this effect
        int currentCount = currentEffectParticleCount.get();
        if (currentCount >= maxParticlesPerEffect) {
            if (plugin.isDebugMode()) {
                plugin.logDebug("Particle limit reached (" + maxParticlesPerEffect + "), skipping " + count + " particles");
            }
            return;
        }
        
        // Limit count to remaining particle budget
        int remainingParticles = maxParticlesPerEffect - currentCount;
        if (count > remainingParticles) {
            count = remainingParticles;
            if (plugin.isDebugMode()) {
                plugin.logDebug("Reducing particle count to " + count + " (remaining budget: " + remainingParticles + ")");
            }
        }
        
        // Update particle counter
        currentEffectParticleCount.set(currentCount + count);
        
        for (Player player : viewers) {
            if (player.getWorld().equals(location.getWorld()) && player.getLocation().distanceSquared(location) <= particleRenderDistance * particleRenderDistance) {
                player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
            }
        }
        
        int newCount = currentEffectParticleCount.get();
        if (plugin.isDebugMode() && newCount > maxParticlesPerEffect * 0.8) {
            plugin.logDebug("Particle usage: " + newCount + "/" + maxParticlesPerEffect + 
                          " (" + String.format("%.1f", (newCount * 100.0 / maxParticlesPerEffect)) + "%)");
        }
    }
    
    private List<Player> getNearbyPlayers(Location location) {
        List<Player> players = new ArrayList<>();
        if (location == null || location.getWorld() == null) {
            return players;
        }
        for (Entity entity : location.getWorld().getNearbyEntities(location, particleRenderDistance, 
                                                                   particleRenderDistance, particleRenderDistance)) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }
        return players;
    }
    
    private void playSound(Location location, Sound sound) {
        if (location == null || location.getWorld() == null || sound == null) {
            return;
        }
        location.getWorld().playSound(location, sound, 1.0f, 1.0f);
    }
    
    private boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) return false;
        
        long cooldownTime = plugin.getConfig().getInt("general.global-cooldown", 3) * 1000L;
        return System.currentTimeMillis() - cooldowns.get(uuid) < cooldownTime;
    }
    
    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    private long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) return 0;
        
        long cooldownTime = plugin.getConfig().getInt("general.global-cooldown", 3) * 1000L;
        long elapsed = System.currentTimeMillis() - cooldowns.get(uuid);
        return Math.max(0, (cooldownTime - elapsed) / 1000);
    }
    
    private void startCleanupTask() {
        BukkitTask cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Clean expired cooldowns
                long now = System.currentTimeMillis();
                cooldowns.entrySet().removeIf(entry -> 
                    now - entry.getValue() > plugin.getConfig().getInt("general.global-cooldown", 3) * 1000L);
                
                // Clean temporary blocks
                Iterator<Map.Entry<Location, Set<Block>>> iterator = temporaryBlocks.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Location, Set<Block>> entry = iterator.next();
                    // This would be implemented with actual temporary block management
                    iterator.remove();
                }
            }
        }.runTaskTimer(plugin, 0, plugin.getSettings().cleanupIntervalTicks());
        activeTasks.add(cleanupTask);
    }
    
    public void cleanup() {
        // Cancel all active tasks
        for (BukkitTask task : activeTasks) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        activeTasks.clear();
        
        // Clear temporary blocks
        clearAllTemporaryBlocks();
        
        plugin.logDebug("KillEffectManager cleaned up");
    }
    
    public void clearAllTemporaryBlocks() {
        for (Set<Block> blocks : temporaryBlocks.values()) {
            for (Block block : blocks) {
                // Reset blocks to original state
                // Implementation would depend on how temporary blocks are stored
            }
        }
        temporaryBlocks.clear();
    }
    
    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
    
    // Inner classes for effect data
    private static class Star {
        Location location;
        Vector velocity;
        
        Star(Location location, Vector velocity) {
            this.location = location;
            this.velocity = velocity;
        }
        
        void update() {
            location.add(velocity);
        }
    }
    
    private class CrystalSpire {
        Location location;
        double targetHeight;
        double currentHeight;
        int age;
        
        CrystalSpire(Location location, double targetHeight) {
            this.location = location;
            this.targetHeight = targetHeight;
            this.currentHeight = 0;
            this.age = 0;
        }
        
        void update() {
            age++;
            if (currentHeight < targetHeight) {
                currentHeight += 0.05;
            }
        }
        
        void render(List<Player> viewers) {
            for (double h = 0; h <= currentHeight; h += 0.2) {
                Location crystalLoc = location.clone().add(0, h, 0);
                spawnParticles(viewers, crystalLoc, Particle.VILLAGER_HAPPY, 2, 0.1, 0.05, 0.1, 0.01);
                if (h > currentHeight * 0.8) {
                    spawnParticles(viewers, crystalLoc, Particle.ENCHANTMENT_TABLE, 1, 0.05, 0.05, 0.05, 0.005);
                }
            }
        }
    }
    
    private static class Spore {
        Location location;
        Vector velocity;
        int age;
        
        Spore(Location location, Vector velocity) {
            this.location = location;
            this.velocity = velocity;
            this.age = 0;
        }
        
        void update() {
            age++;
            location.add(velocity);
            velocity.multiply(0.98); // Gradual slowdown
        }
    }
    
    private class Fracture {
        Location location;
        double angle;
        int age;
        
        Fracture(Location location, double angle) {
            this.location = location;
            this.angle = angle;
            this.age = 0;
        }
        
        void update() {
            age++;
        }
        
        void render(List<Player> viewers) {
            double fracLength = 2.0;
            for (double d = 0; d <= fracLength; d += 0.15) {
                Location fracLoc = location.clone().add(
                        Math.cos(angle) * d,
                        Math.sin(d * 2) * 0.2,
                        Math.sin(angle) * d
                );
                
                spawnParticles(viewers, fracLoc, Particle.SPELL_WITCH, 1, 0.02, 0.02, 0.02, 0.01);
                if (d % 0.5 < 0.2) {
                    spawnParticles(viewers, fracLoc, Particle.ENCHANTMENT_TABLE, 1, 0.01, 0.01, 0.01, 0.005);
                }
            }
        }
    }
    
    private static class ToxicBubble {
        Location location;
        int age;
        
        ToxicBubble(Location location) {
            this.location = location;
            this.age = 0;
        }
        
        void update() {
            age++;
            location.add(0, 0.02, 0); // Slow rise
        }
    }
    
    /**
     * Void Consumption Effect - Dark purple spiral black hole effect
     */
    private void executeVoidConsumption(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 90;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                
                // Inward spiraling void effect
                for (int spiral = 0; spiral < (performanceMode ? 2 : 4); spiral++) {
                    double angle = (ticks * 0.3) + (spiral * Math.PI / 2);
                    double radius = 6 - (progress * 5); // Spiral inward
                    double height = Math.sin(ticks * 0.2) * 1.5;
                    
                    Location spiralLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    
                    // Dark purple void particles
                    spawnParticles(viewers, spiralLoc, Particle.PORTAL, 8, 0.2, 0.2, 0.2, 0.1);
                    spawnParticles(viewers, spiralLoc, Particle.SPELL_WITCH, 3, 0.1, 0.1, 0.1, 0.05);
                }
                
                // Central void core
                if (progress > 0.3) {
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.SMOKE_LARGE, 
                            (int) (15 * progress), 0.3, 0.5, 0.3, 0.02);
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.PORTAL, 
                            (int) (20 * progress), 0.2, 0.3, 0.2, 0.1);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    /**
     * Phoenix Rebirth Effect - Flame particles in wing patterns
     */
    private void executePhoenixRebirth(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                
                // Phoenix wing pattern
                for (int wing = 0; wing < 2; wing++) {
                    double wingDirection = wing == 0 ? 1 : -1;
                    
                    // Wing shape calculations
                    for (int point = 0; point < (performanceMode ? 8 : 15); point++) {
                        double wingProgress = (double) point / (performanceMode ? 8 : 15);
                        double wingSpan = 3 * wingProgress;
                        double wingHeight = Math.sin(wingProgress * Math.PI) * 2.5;
                        double wingFlap = Math.sin(ticks * 0.2) * 0.5;
                        
                        Location wingLoc = center.clone().add(
                                wingDirection * wingSpan,
                                wingHeight + wingFlap,
                                wingProgress * 1.5 - 0.75
                        );
                        
                        // Fire particles for wings
                        spawnParticles(viewers, wingLoc, Particle.FLAME, 3, 0.1, 0.1, 0.1, 0.02);
                        spawnParticles(viewers, wingLoc, Particle.LAVA, 1, 0.05, 0.05, 0.05, 0.0);
                        
                        // Wing tips glow
                        if (wingProgress > 0.7) {
                            spawnParticles(viewers, wingLoc, Particle.FIREWORKS_SPARK, 2, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                }
                
                // Rising phoenix body
                if (progress > 0.4) {
                    Location bodyLoc = center.clone().add(0, progress * 4, 0);
                    spawnParticles(viewers, bodyLoc, Particle.FLAME, 10, 0.3, 0.3, 0.3, 0.05);
                    spawnParticles(viewers, bodyLoc, Particle.SMOKE_LARGE, 5, 0.2, 0.2, 0.2, 0.02);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    /**
     * Celestial Gateway Effect - Rotating light portal
     */
    private void executeCelestialGateway(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 110;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                double portalSize = 2.5;
                
                // Rotating portal rings
                for (int ring = 0; ring < 3; ring++) {
                    double ringRadius = portalSize - (ring * 0.5);
                    double ringHeight = ring * 0.3;
                    double ringRotation = (ticks * 0.15) + (ring * Math.PI / 3);
                    
                    for (int point = 0; point < (performanceMode ? 12 : 24); point++) {
                        double angle = (2 * Math.PI * point) / (performanceMode ? 12 : 24) + ringRotation;
                        
                        Location ringLoc = center.clone().add(
                                Math.cos(angle) * ringRadius,
                                ringHeight + 2,
                                Math.sin(angle) * ringRadius
                        );
                        
                        // Portal particles
                        spawnParticles(viewers, ringLoc, Particle.PORTAL, 2, 0.05, 0.05, 0.05, 0.02);
                        spawnParticles(viewers, ringLoc, Particle.END_ROD, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                
                // Portal center energy
                Location centerPortal = center.clone().add(0, 2, 0);
                spawnParticles(viewers, centerPortal, Particle.PORTAL, 15, 0.5, 0.1, 0.5, 0.1);
                spawnParticles(viewers, centerPortal, Particle.ENCHANTMENT_TABLE, 8, 0.3, 0.3, 0.3, 0.05);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    /**
     * Electric Overload Effect - Blue electrical jumping particles
     */
    private void executeElectricOverload(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 80;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Electric sparks jumping around
                for (int spark = 0; spark < (performanceMode ? 8 : 16); spark++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double radius = 1 + random.nextDouble() * 4;
                    double height = random.nextDouble() * 3;
                    
                    Location sparkLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    
                    spawnParticles(viewers, sparkLoc, Particle.ELECTRIC_SPARK, 3, 0.1, 0.1, 0.1, 0.05);
                    if (random.nextBoolean()) {
                        spawnParticles(viewers, sparkLoc, Particle.FIREWORKS_SPARK, 1, 0.02, 0.02, 0.02, 0.02);
                    }
                }
                
                // Central overload
                if (ticks % 5 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.ELECTRIC_SPARK, 20, 1, 1, 1, 0.2);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    private void executeCrystalGarden(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 140;
            final List<CrystalSpire> crystals = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Spawn new crystals periodically
                if (ticks % 20 == 0 && crystals.size() < (performanceMode ? 8 : 15)) {
                    crystals.add(new CrystalSpire(
                            center.clone().add(
                                    (random.nextDouble() - 0.5) * 12,
                                    0,
                                    (random.nextDouble() - 0.5) * 12
                            ),
                            1 + random.nextDouble() * 3
                    ));
                }
                
                // Update and render crystals
                for (CrystalSpire crystal : crystals) {
                    crystal.update();
                    crystal.render(viewers);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    private void executeSpectralHaunt(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 100) { cancel(); return; }
                double opacity = 1.0 - ((double) ticks / 100);
                for (double y = 0; y <= 1.8; y += 0.3) {
                    for (int angle = 0; angle < 360; angle += 60) {
                        double rad = Math.toRadians(angle);
                        Location ghostLoc = center.clone().add(Math.cos(rad) * 0.3, y, Math.sin(rad) * 0.3);
                        spawnParticles(viewers, ghostLoc, Particle.SOUL, (int)(3 * opacity), 0.05, 0.05, 0.05, 0.01);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        activeTasks.add(task);
    }
    
    private void executeLaserLightShow(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.PURPLE};
            @Override
            public void run() {
                if (ticks >= 120) { cancel(); return; }
                for (int laser = 0; laser < 6; laser++) {
                    double angle = (2 * Math.PI * laser / 6) + (ticks * 0.1);
                    Color color = colors[laser % colors.length];
                    for (double d = 0.5; d <= 6; d += 0.3) {
                        Location beamLoc = center.clone().add(Math.cos(angle) * d, 1.5, Math.sin(angle) * d);
                        spawnParticles(viewers, beamLoc, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(color, 1.5f));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        activeTasks.add(task);
    }
    
    private void executeMeteorImpact(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            boolean impacted = false;
            @Override
            public void run() {
                if (ticks >= 100) { cancel(); return; }
                if (!impacted && ticks < 30) {
                    Location meteorLoc = center.clone().add(0, 10 - (ticks * 0.4), 0);
                    spawnParticles(viewers, meteorLoc, Particle.FLAME, 10, 0.3, 0.3, 0.3, 0.1);
                    spawnParticles(viewers, meteorLoc, Particle.SMOKE_LARGE, 5, 0.5, 0.5, 0.5, 0.05);
                } else if (!impacted) {
                    impacted = true;
                    spawnParticles(viewers, center, Particle.EXPLOSION_LARGE, 5, 1, 0.5, 1, 0.0);
                    spawnParticles(viewers, center, Particle.FLAME, 30, 2, 0.5, 2, 0.2);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        activeTasks.add(task);
    }
    
    private void executeBioluminescentBloom(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 90;
            final List<Spore> spores = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Generate new spores
                if (ticks % 8 == 0 && spores.size() < (performanceMode ? 20 : 40)) {
                    spores.add(new Spore(
                            center.clone().add(0, 0.5, 0),
                            new Vector(
                                    (random.nextDouble() - 0.5) * 0.1,
                                    random.nextDouble() * 0.05,
                                    (random.nextDouble() - 0.5) * 0.1
                            )
                    ));
                }
                
                // Update and render spores
                Iterator<Spore> iterator = spores.iterator();
                while (iterator.hasNext()) {
                    Spore spore = iterator.next();
                    spore.update();
                    
                    if (spore.age > 60 || spore.location.getY() < center.getY() - 1) {
                        // Spore lands and blooms
                        for (int bloom = 0; bloom < 5; bloom++) {
                            Location bloomLoc = spore.location.clone().add(
                                    (random.nextDouble() - 0.5) * 0.3,
                                    random.nextDouble() * 0.2,
                                    (random.nextDouble() - 0.5) * 0.3
                            );
                            spawnParticles(viewers, bloomLoc, Particle.VILLAGER_HAPPY, 3, 0.1, 0.1, 0.1, 0.02);
                        }
                        iterator.remove();
                    } else {
                        // Render floating spore
                        spawnParticles(viewers, spore.location, Particle.SPORE_BLOSSOM_AIR, 2, 0.05, 0.05, 0.05, 0.01);
                        if (spore.age % 10 == 0) {
                            spawnParticles(viewers, spore.location, Particle.VILLAGER_HAPPY, 1, 0.02, 0.02, 0.02, 0.01);
                        }
                    }
                }
                
                // Ground bloom effect
                if (ticks % 15 == 0) {
                    for (int ground = 0; ground < (performanceMode ? 8 : 16); ground++) {
                        double angle = random.nextDouble() * 2 * Math.PI;
                        double distance = random.nextDouble() * 5;
                        
                        Location groundBloom = center.clone().add(
                                Math.cos(angle) * distance,
                                0.1,
                                Math.sin(angle) * distance
                        );
                        
                        spawnParticles(viewers, groundBloom, Particle.VILLAGER_HAPPY, 5, 0.3, 0.1, 0.3, 0.02);
                        spawnParticles(viewers, groundBloom, Particle.ENCHANTMENT_TABLE, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    private void executeTimeFracture(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 80;
            final List<Fracture> fractures = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Create reality fractures
                if (ticks % 10 == 0 && fractures.size() < (performanceMode ? 6 : 12)) {
                    fractures.add(new Fracture(
                            center.clone().add(
                                    (random.nextDouble() - 0.5) * 8,
                                    random.nextDouble() * 4,
                                    (random.nextDouble() - 0.5) * 8
                            ),
                            random.nextDouble() * 2 * Math.PI
                    ));
                }
                
                // Render fractures
                for (Fracture fracture : fractures) {
                    fracture.update();
                    fracture.render(viewers);
                }
                
                // Central time distortion
                if (ticks % 5 == 0) {
                    Location timeCenter = center.clone().add(0, 2, 0);
                    spawnParticles(viewers, timeCenter, Particle.ENCHANTMENT_TABLE, 15, 1, 1, 1, 0.1);
                    spawnParticles(viewers, timeCenter, Particle.PORTAL, 8, 0.5, 0.5, 0.5, 0.05);
                }
                
                // Reality ripples
                if (ticks % 8 == 0) {
                    for (int ripple = 1; ripple <= 3; ripple++) {
                        double rippleRadius = ripple * 2;
                        
                        for (int point = 0; point < 16; point++) {
                            double angle = (2 * Math.PI * point) / 16;
                            Location rippleLoc = center.clone().add(
                                    Math.cos(angle) * rippleRadius,
                                    1,
                                    Math.sin(angle) * rippleRadius
                            );
                            
                            spawnParticles(viewers, rippleLoc, Particle.SPELL_WITCH, 2, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    private void executeDivineAscension(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 130) { cancel(); return; }
                double progress = (double) ticks / 130;
                for (int helix = 0; helix < 2; helix++) {
                    for (double h = 0; h <= progress * 6; h += 0.3) {
                        double angle = (h * 0.8) + (helix * Math.PI);
                        Location helixLoc = center.clone().add(Math.cos(angle) * 1.5, h, Math.sin(angle) * 1.5);
                        spawnParticles(viewers, helixLoc, Particle.TOTEM, 2, 0.05, 0.05, 0.05, 0.02);
                        spawnParticles(viewers, helixLoc, Particle.FIREWORKS_SPARK, 1, 0.02, 0.02, 0.02, 0.01);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        activeTasks.add(task);
    }
    
    private void executeToxicMeltdown(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 110;
            final List<ToxicBubble> bubbles = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                
                // Generate toxic bubbles
                if (ticks % 6 == 0 && bubbles.size() < (performanceMode ? 15 : 30)) {
                    bubbles.add(new ToxicBubble(
                            center.clone().add(
                                    (random.nextDouble() - 0.5) * progress * 10,
                                    0.1,
                                    (random.nextDouble() - 0.5) * progress * 10
                            )
                    ));
                }
                
                // Update bubbles
                Iterator<ToxicBubble> iterator = bubbles.iterator();
                while (iterator.hasNext()) {
                    ToxicBubble bubble = iterator.next();
                    bubble.update();
                    
                    if (bubble.age > 50) {
                        // Bubble pops
                        for (int pop = 0; pop < 8; pop++) {
                            Location popLoc = bubble.location.clone().add(
                                    (random.nextDouble() - 0.5) * 0.5,
                                    random.nextDouble() * 0.5,
                                    (random.nextDouble() - 0.5) * 0.5
                            );
                            spawnParticles(viewers, popLoc, Particle.SLIME, 2, 0.1, 0.1, 0.1, 0.02);
                        }
                        iterator.remove();
                    } else {
                        // Render bubble
                        spawnParticles(viewers, bubble.location, Particle.SLIME, 3, 0.1, 0.1, 0.1, 0.01);
                        if (bubble.age % 15 == 0) {
                            spawnParticles(viewers, bubble.location, Particle.ITEM_CRACK, 1, 0.05, 0.05, 0.05, 0.01,
                                    new ItemStack(Material.SLIME_BALL));
                        }
                    }
                }
                
                // Toxic ground spread
                double spreadRadius = progress * 5;
                if (ticks % 8 == 0) {
                    for (int spread = 0; spread < (performanceMode ? 12 : 24); spread++) {
                        double angle = (2 * Math.PI * spread) / (performanceMode ? 12 : 24);
                        double distance = random.nextDouble() * spreadRadius;
                        
                        Location toxicGround = center.clone().add(
                                Math.cos(angle) * distance,
                                0.05,
                                Math.sin(angle) * distance
                        );
                        
                        spawnParticles(viewers, toxicGround, Particle.SLIME, 2, 0.2, 0.05, 0.2, 0.01);
                        if (random.nextInt(3) == 0) {
                            spawnParticles(viewers, toxicGround, Particle.SMOKE_NORMAL, 1, 0.1, 0.1, 0.1, 0.005);
                        }
                    }
                }
                
                // Central toxic fountain
                if (ticks % 4 == 0) {
                    Location fountain = center.clone().add(0, 0.5, 0);
                    spawnParticles(viewers, fountain, Particle.SLIME, 8, 0.3, 0.5, 0.3, 0.05);
                    spawnParticles(viewers, fountain, Particle.DRIP_LAVA, 3, 0.2, 0.3, 0.2, 0.02);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    private void executeQuantumCollapse(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 90) { cancel(); return; }
                for (int phase = 0; phase < 4; phase++) {
                    double visibility = (Math.sin((ticks + phase * 15) * 0.2) + 1) / 2;
                    for (double y = 0; y <= 1.8; y += 0.2) {
                        for (int angle = 0; angle < 360; angle += 90) {
                            double rad = Math.toRadians(angle);
                            Location quantumLoc = center.clone().add(Math.cos(rad) * 0.3 * visibility, y, Math.sin(rad) * 0.3 * visibility);
                            spawnParticles(viewers, quantumLoc, Particle.REVERSE_PORTAL, (int)(2 * visibility), 0.02, 0.02, 0.02, 0.01);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        activeTasks.add(task);
    }
    
    private void executeSupernova(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            boolean exploded = false;
            @Override
            public void run() {
                if (ticks >= 60) { cancel(); return; }
                if (!exploded && ticks < 20) {
                    double intensity = (double) ticks / 20;
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.END_ROD, (int)(10 * intensity), 0.5, 0.5, 0.5, 0.1);
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.FLASH, (int)(3 * intensity), 0.2, 0.2, 0.2, 0.1);
                } else if (!exploded) {
                    exploded = true;
                    spawnParticles(viewers, center, Particle.FLASH, 15, 2, 1, 2, 0.3);
                    spawnParticles(viewers, center, Particle.FIREWORKS_SPARK, 60, 3, 2, 3, 0.4);
                    spawnParticles(viewers, center, Particle.END_ROD, 30, 2, 1, 2, 0.2);
                    for (int wave = 1; wave <= 4; wave++) {
                        for (int point = 0; point < 16; point++) {
                            double angle = (2 * Math.PI * point) / 16;
                            Location waveLoc = center.clone().add(Math.cos(angle) * wave * 2, 0.5, Math.sin(angle) * wave * 2);
                            spawnParticles(viewers, waveLoc, Particle.EXPLOSION_LARGE, 1, 0.1, 0.1, 0.1, 0.0);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        activeTasks.add(task);
    }
}
