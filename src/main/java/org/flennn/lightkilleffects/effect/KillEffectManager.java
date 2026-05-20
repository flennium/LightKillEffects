package org.flennn.lightkilleffects.effect;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.flennn.lightkilleffects.LightKillEffects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class KillEffectManager {
    
    private final LightKillEffects plugin;
    private final Map<UUID, Long> cooldowns;
    private final Map<Location, Set<Block>> temporaryBlocks;
    private final Set<BukkitTask> activeTasks;
    private final Map<EffectType, KillEffect> effects;
    private final Random random;
    private final int maxParticlesPerEffect;
    private final int particleRenderDistance;
    private final boolean performanceMode;
    private final boolean soundsEnabled;
    private final ThreadLocal<Integer> currentEffectParticleCount = ThreadLocal.withInitial(() -> 0);
    
    public KillEffectManager(LightKillEffects plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
        this.temporaryBlocks = new ConcurrentHashMap<>();
        this.activeTasks = ConcurrentHashMap.newKeySet();
        this.effects = new EnumMap<>(EffectType.class);
        this.random = new Random();
        this.maxParticlesPerEffect = plugin.getSettings().maxParticlesPerEffect();
        this.particleRenderDistance = plugin.getSettings().particleRenderDistance();
        this.performanceMode = plugin.getConfig().getBoolean("performance.performance-mode", false);
        this.soundsEnabled = plugin.getConfig().getBoolean("general.sounds-enabled", true);
        
        registerEffects();
        startCleanupTask();
        
        plugin.logDebug("KillEffectManager initialized with performance mode: " + performanceMode);
    }

    private void registerEffects() {
        this.effects.put(EffectType.LIGHTNING_STORM, new LightningStormEffect(this));
        this.effects.put(EffectType.SOLAR_EXPLOSION, new SolarExplosionEffect(this));
        this.effects.put(EffectType.FROZEN_BURST, new FrozenBurstEffect(this));
        this.effects.put(EffectType.NEON_RAVE, new NeonRaveEffect(this));
        this.effects.put(EffectType.STARFALL_CASCADE, new StarfallCascadeEffect(this));
        this.effects.put(EffectType.PRISMATIC_SHATTER, new PrismaticShatterEffect(this));
        this.effects.put(EffectType.VOID_CONSUMPTION, new VoidConsumptionEffect(this));
        this.effects.put(EffectType.PHOENIX_REBIRTH, new PhoenixRebirthEffect(this));
        this.effects.put(EffectType.CELESTIAL_GATEWAY, new CelestialGatewayEffect(this));
        this.effects.put(EffectType.ELECTRIC_OVERLOAD, new ElectricOverloadEffect(this));
        this.effects.put(EffectType.CRYSTAL_GARDEN, new CrystalGardenEffect(this));
        this.effects.put(EffectType.SPECTRAL_HAUNT, new SpectralHauntEffect(this));
        this.effects.put(EffectType.LASER_LIGHT_SHOW, new LaserLightShowEffect(this));
        this.effects.put(EffectType.METEOR_IMPACT, new MeteorImpactEffect(this));
        this.effects.put(EffectType.BIOLUMINESCENT_BLOOM, new BioluminescentBloomEffect(this));
        this.effects.put(EffectType.TIME_FRACTURE, new TimeFractureEffect(this));
        this.effects.put(EffectType.DIVINE_ASCENSION, new DivineAscensionEffect(this));
        this.effects.put(EffectType.TOXIC_MELTDOWN, new ToxicMeltdownEffect(this));
        this.effects.put(EffectType.QUANTUM_COLLAPSE, new QuantumCollapseEffect(this));
        this.effects.put(EffectType.SUPERNOVA, new SupernovaEffect(this));
        this.effects.put(EffectType.STORM_CAGE, new StormCageEffect(this));
        this.effects.put(EffectType.PHANTOM_WALTZ, new PhantomWaltzEffect(this));
        this.effects.put(EffectType.MIRROR_BREAK, new MirrorBreakEffect(this));
        this.effects.put(EffectType.ROYAL_EXECUTION, new RoyalExecutionEffect(this));
        this.effects.put(EffectType.SAKURA_COLLAPSE, new SakuraCollapseEffect(this));
        this.effects.put(EffectType.ALCHEMY_FAILURE, new AlchemyFailureEffect(this));
        this.effects.put(EffectType.RUNIC_FORGE, new RunicForgeEffect(this));
        this.effects.put(EffectType.CELESTIAL_BLOOM, new CelestialBloomEffect(this));
        this.effects.put(EffectType.ASTRAL_VERDICT, new AstralVerdictEffect(this));
        this.effects.put(EffectType.TIME_SNAP, new TimeSnapEffect(this));
    }
    public void executeEffect(Player killer, Location deathLocation, EffectType effect) {
        if (effect == null || deathLocation == null) return;
        if (isOnCooldown(killer)) {
            long remaining = getRemainingCooldown(killer);
            plugin.sendMessage(killer, "on-cooldown", "seconds", String.valueOf(remaining));
            return;
        }
        setCooldown(killer);
        currentEffectParticleCount.set(0);
        try {
            List<Player> nearbyPlayers = getNearbyPlayers(deathLocation);
            if (nearbyPlayers.isEmpty()) return;

            plugin.logDebug("Executing " + effect.getDisplayName() + " at " + formatLocation(deathLocation) +
                    " (max particles: " + maxParticlesPerEffect + ")");

            KillEffect handler = this.effects.get(effect);
            if (handler == null) return;
            handler.run(deathLocation, nearbyPlayers);
            if (soundsEnabled) {
                playSound(deathLocation, effect.getSound());
            }
            int finalParticleCount = currentEffectParticleCount.get();
            if (plugin.isDebugMode()) {
                plugin.logDebug("Effect " + effect.getDisplayName() + " completed - Total particles used: " +
                        finalParticleCount + "/" + maxParticlesPerEffect);
            }
        } finally {
            currentEffectParticleCount.remove();
        }
    }
    void executeLightningStorm(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("lightning_storm", 60);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
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
                        spawnParticles(viewers, boltLoc, Particle.ELECTRIC_SPARK, 20, 0.1, 2, 0.1, 0.1);
                        spawnParticles(viewers, boltLoc, Particle.FIREWORKS_SPARK, 10, 0.2, 3, 0.2, 0.0);
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
    void executeSolarExplosion(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("solar_explosion", 80);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                double radius = 0.5 + (progress * 4);
                spawnParticles(viewers, center, Particle.FLAME, 
                        (int) (30 * (1 - progress)), 0.5, 0.5, 0.5, 0.05);
                spawnParticles(viewers, center, Particle.FIREWORKS_SPARK, 
                        (int) (20 * (1 - progress)), 0.3, 0.3, 0.3, 0.1);
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
    void executeFrozenBurst(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("frozen_burst", 70);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
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
                for (int i = 0; i < (performanceMode ? 2 : 4); i++) {
                    double angle = (ticks * 0.2) + (i * Math.PI / 2);
                    double radius = progress * 4;
                    double height = Math.sin(ticks * 0.1) * 2;
                    
                    Location spiralLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    spawnParticles(viewers, spiralLoc, Particle.REDSTONE, 5, 0.1, 0.1, 0.1, 0.0,
                            new Particle.DustOptions(Color.fromRGB(173, 216, 230), 1.5f));
                    spawnParticles(viewers, spiralLoc, Particle.SNOWFLAKE, 8, 0.2, 0.2, 0.2, 0.05);
                }
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
    void executeNeonRave(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("neon_rave", 100);
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
                if (ticks % 3 == 0) {
                    Color currentColor = neonColors[ticks / 3 % neonColors.length];
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
    void executeStarfallCascade(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("starfall_cascade", 120);
            final List<Star> stars = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
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
                Iterator<Star> iterator = stars.iterator();
                while (iterator.hasNext()) {
                    Star star = iterator.next();
                    star.update();
                    
                    if (star.location.getY() < center.getY() - 2) {
                        spawnParticles(viewers, star.location, Particle.FIREWORKS_SPARK, 15, 0.3, 0.1, 0.3, 0.1);
                        spawnParticles(viewers, star.location, Particle.END_ROD, 8, 0.2, 0.2, 0.2, 0.05);
                        iterator.remove();
                    } else {
                        spawnParticles(viewers, star.location, Particle.END_ROD, 3, 0.1, 0.1, 0.1, 0.02);
                        spawnParticles(viewers, star.location, Particle.FIREWORKS_SPARK, 2, 0.05, 0.05, 0.05, 0.01);
                        Location trailLoc = star.location.clone().subtract(star.velocity.clone().multiply(3));
                        spawnParticles(viewers, trailLoc, Particle.END_ROD, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    void executePrismaticShatter(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("prismatic_shatter", 60);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                for (int i = 0; i < (performanceMode ? 50 : 100); i++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double radius = progress * 5;
                    double height = random.nextDouble() * 3;
                    
                    Location shardLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    Color[] rainbowColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.GREEN, Color.AQUA, Color.BLUE, Color.PURPLE};
                    Color rainbowColor = rainbowColors[(ticks + i * 10) % rainbowColors.length];
                    
                    spawnParticles(viewers, shardLoc, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0,
                            new Particle.DustOptions(rainbowColor, 1.0f));
                    spawnParticles(viewers, shardLoc, Particle.FIREWORKS_SPARK, 2, 0.1, 0.1, 0.1, 0.05);
                }
                if (ticks < 40) {
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.END_ROD, 10, 0.2, 1, 0.2, 0.1);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    private void spawnParticles(List<Player> viewers, Location location, Particle particle, int count, 
                               double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticles(viewers, location, particle, count, offsetX, offsetY, offsetZ, extra, null);
    }
    
    private void spawnParticles(List<Player> viewers, Location location, Particle particle, int count, 
                               double offsetX, double offsetY, double offsetZ, double extra, Object data) {
        if (location == null || location.getWorld() == null || count <= 0) {
            return;
        }
        int currentCount = currentEffectParticleCount.get();
        if (currentCount >= maxParticlesPerEffect) {
            if (plugin.isDebugMode()) {
                plugin.logDebug("Particle limit reached (" + maxParticlesPerEffect + "), skipping " + count + " particles");
            }
            return;
        }
        int remainingParticles = maxParticlesPerEffect - currentCount;
        if (count > remainingParticles) {
            count = remainingParticles;
            if (plugin.isDebugMode()) {
                plugin.logDebug("Reducing particle count to " + count + " (remaining budget: " + remainingParticles + ")");
            }
        }
        Object particleData = getParticleData(particle, data);
        if (particleData == null && particle.getDataType() != Void.class) {
            if (plugin.isDebugMode()) {
                plugin.logDebug("Skipping " + particle.name() + " because it requires " + particle.getDataType().getSimpleName());
            }
            return;
        }

        currentEffectParticleCount.set(currentCount + count);
        
        for (Player player : viewers) {
            if (player.getWorld().equals(location.getWorld()) && player.getLocation().distanceSquared(location) <= particleRenderDistance * particleRenderDistance) {
                try {
                    player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, particleData);
                } catch (IllegalArgumentException e) {
                    if (plugin.isDebugMode()) {
                        plugin.logDebug("Could not spawn " + particle.name() + " for " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        int newCount = currentEffectParticleCount.get();
        if (plugin.isDebugMode() && newCount > maxParticlesPerEffect * 0.8) {
            plugin.logDebug("Particle usage: " + newCount + "/" + maxParticlesPerEffect + 
                          " (" + String.format("%.1f", (newCount * 100.0 / maxParticlesPerEffect)) + "%)");
        }
    }

    private Object getParticleData(Particle particle, Object data) {
        if (data != null) {
            return data;
        }

        Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
            return null;
        }
        if (dataType == Color.class) {
            return Color.WHITE;
        }
        if (dataType == Particle.DustOptions.class) {
            return new Particle.DustOptions(Color.WHITE, 1.0f);
        }
        if (dataType == Particle.DustTransition.class) {
            return new Particle.DustTransition(Color.WHITE, Color.YELLOW, 1.0f);
        }
        if (dataType == BlockData.class) {
            return Material.STONE.createBlockData();
        }
        if (dataType == ItemStack.class) {
            return new ItemStack(Material.STONE);
        }
        return null;
    }

    private Particle.DustOptions dust(Color color, float size) {
        return new Particle.DustOptions(color, size);
    }

    private Color blend(Color start, Color end, double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        int red = (int) Math.round(start.getRed() + ((end.getRed() - start.getRed()) * clamped));
        int green = (int) Math.round(start.getGreen() + ((end.getGreen() - start.getGreen()) * clamped));
        int blue = (int) Math.round(start.getBlue() + ((end.getBlue() - start.getBlue()) * clamped));
        return Color.fromRGB(red, green, blue);
    }

    private int effectDuration(String effectKey, int fallback) {
        return Math.max(20, plugin.getEffectsConfig().getInt("effects." + effectKey + ".duration", fallback));
    }
    
    private List<Player> getNearbyPlayers(Location location) {
        List<Player> players = new ArrayList<>();
        if (location == null || location.getWorld() == null) {
            return players;
        }
        for (Entity entity : location.getWorld().getNearbyEntities(location, particleRenderDistance, 
                                                                   particleRenderDistance, particleRenderDistance)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (plugin.getPlayerData().getPlayerData(player).isViewingEffects()) {
                    players.add(player);
                }
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
                long now = System.currentTimeMillis();
                cooldowns.entrySet().removeIf(entry -> 
                    now - entry.getValue() > plugin.getConfig().getInt("general.global-cooldown", 3) * 1000L);
                Iterator<Map.Entry<Location, Set<Block>>> iterator = temporaryBlocks.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Location, Set<Block>> entry = iterator.next();
                    iterator.remove();
                }
            }
        }.runTaskTimer(plugin, 0, plugin.getSettings().cleanupIntervalTicks());
        activeTasks.add(cleanupTask);
    }
    
    public void cleanup() {
        for (BukkitTask task : activeTasks) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        activeTasks.clear();
        clearAllTemporaryBlocks();
        
        plugin.logDebug("KillEffectManager cleaned up");
    }
    
    public void clearAllTemporaryBlocks() {
        for (Set<Block> blocks : temporaryBlocks.values()) {
            for (Block block : blocks) {
            }
        }
        temporaryBlocks.clear();
    }
    
    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
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
            velocity.multiply(0.98);
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
            location.add(0, 0.02, 0);
        }
    }
    private static class MirrorShard {
        Location location;
        Vector velocity;
        Color color;
        int age;

        MirrorShard(Location location, Vector velocity, Color color) {
            this.location = location;
            this.velocity = velocity;
            this.color = color;
            this.age = 0;
        }

        void update() {
            age++;
            location.add(velocity);
            velocity.multiply(0.97);
            velocity.setY(velocity.getY() - 0.01);
        }
    }

    private static class Petal {
        double angle;
        double radius;
        double height;
        double drift;
        Color color;
        int age;

        Petal(double angle, double radius, double height, double drift, Color color) {
            this.angle = angle;
            this.radius = radius;
            this.height = height;
            this.drift = drift;
            this.color = color;
            this.age = 0;
        }

        void update(double angleStep, double radiusStep, double heightStep) {
            age++;
            angle += angleStep;
            radius += radiusStep;
            height += heightStep;
        }
    }
    void executeStormCage(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("storm_cage", 90);

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    for (int point = 0; point < 18; point++) {
                        double angle = (2 * Math.PI * point) / 18;
                        Location burst = center.clone().add(Math.cos(angle) * 3.8, 1.1, Math.sin(angle) * 3.8);
                        spawnParticles(viewers, burst, Particle.ELECTRIC_SPARK, 4, 0.1, 0.2, 0.1, 0.02);
                    }
                    spawnParticles(viewers, center.clone().add(0, 1.2, 0), Particle.FLASH, 2, 0.4, 0.4, 0.4, 0.05);
                    cancel();
                    return;
                }

                double radius = 3.2;
                double pulseHeight = 1.3 + (Math.sin(ticks * 0.25) * 0.5);
                int posts = performanceMode ? 5 : 6;
                List<Location> corners = new ArrayList<>();

                for (int i = 0; i < posts; i++) {
                    double angle = (2 * Math.PI * i) / posts + (ticks * 0.02);
                    Location base = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                    corners.add(base.clone());

                    for (double y = 0.0; y <= 3.2; y += 0.45) {
                        Location post = base.clone().add(0, y, 0);
                        spawnParticles(viewers, post, Particle.ELECTRIC_SPARK, 2, 0.04, 0.08, 0.04, 0.01);
                        if (ticks % 8 == 0) {
                            spawnParticles(viewers, post, Particle.END_ROD, 1, 0.02, 0.02, 0.02, 0.0);
                        }
                    }
                }

                if (ticks % 6 == 0) {
                    for (int i = 0; i < corners.size(); i++) {
                        Location start = corners.get(i).clone().add(0, pulseHeight, 0);
                        Location end = corners.get((i + 1) % corners.size()).clone().add(0, pulseHeight, 0);
                        Vector between = end.toVector().subtract(start.toVector());
                        int segments = performanceMode ? 5 : 8;
                        for (int segment = 0; segment <= segments; segment++) {
                            double progress = (double) segment / segments;
                            Location arc = start.clone().add(between.clone().multiply(progress));
                            arc.add(0, Math.sin(progress * Math.PI) * 0.2, 0);
                            spawnParticles(viewers, arc, Particle.ELECTRIC_SPARK, 2, 0.02, 0.02, 0.02, 0.01);
                        }
                    }
                }

                if (ticks > 45 && ticks % 10 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1.2, 0), Particle.ELECTRIC_SPARK, 14, 0.8, 1.0, 0.8, 0.08);
                    spawnParticles(viewers, center.clone().add(0, 1.2, 0), Particle.FIREWORKS_SPARK, 8, 0.4, 0.6, 0.4, 0.04);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executePhantomWaltz(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("phantom_waltz", 110);

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.SOUL, 18, 0.5, 0.8, 0.5, 0.03);
                    cancel();
                    return;
                }

                double progress = (double) ticks / maxTicks;
                double baseRadius = progress < 0.65 ? 2.6 : 2.6 * (1.0 - ((progress - 0.65) / 0.35));
                baseRadius = Math.max(0.35, baseRadius);

                for (int spirit = 0; spirit < 3; spirit++) {
                    double angle = (ticks * 0.12) + (spirit * ((2 * Math.PI) / 3.0));
                    Location anchor = center.clone().add(
                            Math.cos(angle) * baseRadius,
                            0.9 + Math.sin((ticks * 0.09) + spirit) * 0.3,
                            Math.sin(angle) * baseRadius
                    );

                    for (double y = 0.0; y <= 1.6; y += 0.3) {
                        double width = 0.12 + (Math.sin((y * 2.5) + ticks * 0.08) * 0.03);
                        Location ghost = anchor.clone().add(Math.cos(angle + y) * width, y, Math.sin(angle + y) * width);
                        spawnParticles(viewers, ghost, Particle.SOUL, 2, 0.02, 0.03, 0.02, 0.01);
                    }

                    Location trail = center.clone().add(
                            Math.cos(angle - 0.35) * (baseRadius + 0.15),
                            0.7,
                            Math.sin(angle - 0.35) * (baseRadius + 0.15)
                    );
                    spawnParticles(viewers, trail, Particle.ENCHANTMENT_TABLE, 2, 0.04, 0.04, 0.04, 0.01);
                }

                if (ticks % 18 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1.2, 0), Particle.SOUL, 10, 0.35, 0.6, 0.35, 0.02);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeMirrorBreak(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("mirror_break", 80);
            boolean shattered = false;
            final List<MirrorShard> shards = new ArrayList<>();
            final Color[] glassColors = {
                    Color.fromRGB(230, 245, 255),
                    Color.fromRGB(180, 220, 255),
                    Color.fromRGB(215, 205, 255)
            };

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                if (!shattered) {
                    double panelRadius = 2.0 + (Math.sin(ticks * 0.08) * 0.2);
                    for (int panel = 0; panel < 4; panel++) {
                        double angle = (Math.PI / 2 * panel) + (ticks * 0.03);
                        Location panelCenter = center.clone().add(
                                Math.cos(angle) * panelRadius,
                                1.2,
                                Math.sin(angle) * panelRadius
                        );

                        for (double y = -0.8; y <= 0.8; y += 0.35) {
                            for (double width = -0.55; width <= 0.55; width += 0.3) {
                                Location piece = panelCenter.clone().add(
                                        Math.cos(angle + Math.PI / 2) * width,
                                        y,
                                        Math.sin(angle + Math.PI / 2) * width
                                );
                                Color color = glassColors[(panel + ((int) Math.round((y + 0.8) * 3))) % glassColors.length];
                                spawnParticles(viewers, piece, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(color, 1.0f));
                            }
                        }
                    }

                    if (ticks % 8 == 0) {
                        spawnParticles(viewers, center.clone().add(0, 1.1, 0), Particle.END_ROD, 6, 0.25, 0.6, 0.25, 0.02);
                    }

                    if (ticks >= 28) {
                        shattered = true;
                        for (int i = 0; i < (performanceMode ? 14 : 24); i++) {
                            double angle = random.nextDouble() * 2 * Math.PI;
                            Vector velocity = new Vector(
                                    Math.cos(angle) * (0.12 + random.nextDouble() * 0.12),
                                    0.08 + random.nextDouble() * 0.14,
                                    Math.sin(angle) * (0.12 + random.nextDouble() * 0.12)
                            );
                            Color color = glassColors[i % glassColors.length];
                            shards.add(new MirrorShard(center.clone().add(0, 1.1, 0), velocity, color));
                        }
                        spawnParticles(viewers, center.clone().add(0, 1.1, 0), Particle.FIREWORKS_SPARK, 14, 0.5, 0.6, 0.5, 0.05);
                    }
                } else {
                    Iterator<MirrorShard> iterator = shards.iterator();
                    while (iterator.hasNext()) {
                        MirrorShard shard = iterator.next();
                        shard.update();
                        if (shard.age > 26 || shard.location.getY() < center.getY() - 1.0) {
                            iterator.remove();
                            continue;
                        }

                        spawnParticles(viewers, shard.location, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(shard.color, 1.05f));
                        if (shard.age % 4 == 0) {
                            spawnParticles(viewers, shard.location, Particle.END_ROD, 1, 0.02, 0.02, 0.02, 0.0);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeRoyalExecution(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("royal_execution", 100);

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.TOTEM, 12, 0.5, 0.8, 0.5, 0.04);
                    cancel();
                    return;
                }

                double progress = (double) ticks / maxTicks;
                Color gold = Color.fromRGB(255, 210, 90);
                Color ruby = Color.fromRGB(190, 30, 55);

                for (int point = 0; point < 20; point++) {
                    double angle = (2 * Math.PI * point) / 20;
                    Location floor = center.clone().add(Math.cos(angle) * 2.4, 0.1, Math.sin(angle) * 2.4);
                    Color floorColor = point % 2 == 0 ? gold : ruby;
                    spawnParticles(viewers, floor, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(floorColor, 1.15f));
                }

                double crownY = 3.1 - (progress * 1.3);
                for (int point = 0; point < 16; point++) {
                    double angle = (2 * Math.PI * point) / 16;
                    Location band = center.clone().add(Math.cos(angle) * 1.05, crownY, Math.sin(angle) * 1.05);
                    spawnParticles(viewers, band, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(gold, 1.3f));
                }

                for (int tip = 0; tip < 5; tip++) {
                    double angle = (2 * Math.PI * tip) / 5;
                    for (double h = 0.0; h <= 0.8; h += 0.2) {
                        double taper = 1.0 - (h / 1.1);
                        Location spike = center.clone().add(
                                Math.cos(angle) * 1.05 * taper,
                                crownY + h,
                                Math.sin(angle) * 1.05 * taper
                        );
                        spawnParticles(viewers, spike, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(gold, 1.15f));
                    }

                    Location jewel = center.clone().add(Math.cos(angle) * 0.62, crownY + 0.25, Math.sin(angle) * 0.62);
                    spawnParticles(viewers, jewel, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(ruby, 1.0f));
                }

                if (ticks > 55 && ticks % 10 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1.3, 0), Particle.TOTEM, 8, 0.2, 0.8, 0.2, 0.03);
                    spawnParticles(viewers, center.clone().add(0, 1.3, 0), Particle.FIREWORKS_SPARK, 6, 0.4, 0.6, 0.4, 0.04);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeSakuraCollapse(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("sakura_collapse", 100);
            final List<Petal> petals = new ArrayList<>();
            final Color[] petalColors = {
                    Color.fromRGB(255, 196, 214),
                    Color.fromRGB(255, 215, 230),
                    Color.fromRGB(255, 240, 245)
            };

            {
                int total = performanceMode ? 16 : 28;
                for (int i = 0; i < total; i++) {
                    petals.add(new Petal(
                            (2 * Math.PI * i) / total,
                            0.7 + random.nextDouble() * 0.6,
                            0.3 + random.nextDouble() * 1.4,
                            (random.nextDouble() - 0.5) * 0.08,
                            petalColors[i % petalColors.length]
                    ));
                }
            }

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.END_ROD, 10, 0.35, 0.5, 0.35, 0.03);
                    cancel();
                    return;
                }

                boolean collapsing = ticks > 58;
                for (Petal petal : petals) {
                    double radiusStep = collapsing ? -0.06 : 0.02;
                    double heightStep = collapsing ? -0.012 : 0.004;
                    petal.update(0.18 + petal.drift, radiusStep, heightStep);
                    petal.radius = Math.max(0.15, Math.min(3.6, petal.radius));
                    petal.height = Math.max(0.2, petal.height);

                    Location loc = center.clone().add(
                            Math.cos(petal.angle) * petal.radius,
                            petal.height + Math.sin((ticks * 0.08) + petal.angle) * 0.2,
                            Math.sin(petal.angle) * petal.radius
                    );
                    spawnParticles(viewers, loc, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(petal.color, 0.95f));
                    if (petal.age % 9 == 0) {
                        spawnParticles(viewers, loc, Particle.FIREWORKS_SPARK, 1, 0.03, 0.03, 0.03, 0.0);
                    }
                }

                if (collapsing && ticks % 8 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 0.9, 0), Particle.REDSTONE, 8, 0.25, 0.45, 0.25, 0.01,
                            dust(Color.fromRGB(255, 245, 250), 1.1f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeAlchemyFailure(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("alchemy_failure", 110);
            final Color[] colors = {
                    Color.fromRGB(117, 255, 143),
                    Color.fromRGB(140, 210, 255),
                    Color.fromRGB(195, 120, 255),
                    Color.fromRGB(255, 190, 90)
            };

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.SMOKE_LARGE, 10, 0.5, 0.8, 0.5, 0.03);
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.FIREWORKS_SPARK, 10, 0.4, 0.7, 0.4, 0.05);
                    cancel();
                    return;
                }

                double swirlRadius = 1.0 + (Math.sin(ticks * 0.09) * 0.4);
                for (int orb = 0; orb < 4; orb++) {
                    double angle = (ticks * 0.15) + (orb * (Math.PI / 2.0));
                    for (double h = 0.2; h <= 2.6; h += 0.45) {
                        Location mote = center.clone().add(
                                Math.cos(angle + h) * (swirlRadius + (h * 0.08)),
                                h,
                                Math.sin(angle + h) * (swirlRadius + (h * 0.08))
                        );
                        spawnParticles(viewers, mote, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(colors[orb], 1.0f));
                    }
                }

                spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.SPELL_WITCH, 4, 0.2, 0.8, 0.2, 0.02);
                if (ticks % 7 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 0.6, 0), Particle.SMOKE_NORMAL, 5, 0.3, 0.5, 0.3, 0.02);
                }
                if (ticks % 16 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1.2, 0), Particle.ITEM_CRACK, 2, 0.15, 0.2, 0.15, 0.01,
                            new ItemStack(Material.GLASS_BOTTLE));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeRunicForge(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("runic_forge", 95);
            final Color ember = Color.fromRGB(255, 120, 30);
            final Color gold = Color.fromRGB(255, 210, 70);

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    spawnParticles(viewers, center.clone().add(0, 1.1, 0), Particle.FLAME, 14, 0.5, 0.7, 0.5, 0.05);
                    cancel();
                    return;
                }

                for (int point = 0; point < 18; point++) {
                    double angle = (2 * Math.PI * point) / 18 + (ticks * 0.02);
                    Location ring = center.clone().add(Math.cos(angle) * 2.2, 0.08, Math.sin(angle) * 2.2);
                    spawnParticles(viewers, ring, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(ember, 1.1f));
                }

                for (int rune = 0; rune < 4; rune++) {
                    double angle = (Math.PI / 2 * rune) + (ticks * 0.03);
                    for (double d = 0.2; d <= 1.1; d += 0.2) {
                        Location line = center.clone().add(Math.cos(angle) * d, 0.1, Math.sin(angle) * d);
                        spawnParticles(viewers, line, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(gold, 1.0f));
                    }
                }

                if (ticks % 6 == 0) {
                    for (int spark = 0; spark < (performanceMode ? 4 : 7); spark++) {
                        Location forge = center.clone().add(
                                (random.nextDouble() - 0.5) * 0.9,
                                0.5 + random.nextDouble() * 0.6,
                                (random.nextDouble() - 0.5) * 0.9
                        );
                        spawnParticles(viewers, forge, Particle.FLAME, 2, 0.04, 0.1, 0.04, 0.01);
                        spawnParticles(viewers, forge, Particle.LAVA, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }

                if (ticks > 50 && ticks % 12 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.SMOKE_LARGE, 6, 0.3, 0.4, 0.3, 0.03);
                    spawnParticles(viewers, center.clone().add(0, 1.2, 0), Particle.FIREWORKS_SPARK, 6, 0.35, 0.5, 0.35, 0.04);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeCelestialBloom(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("celestial_bloom", 120);
            final Color[] bloomColors = {
                    Color.fromRGB(255, 240, 255),
                    Color.fromRGB(190, 245, 255),
                    Color.fromRGB(255, 225, 180),
                    Color.fromRGB(220, 200, 255),
                    Color.fromRGB(255, 205, 225)
            };

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                double progress = (double) ticks / maxTicks;
                double petalLength = Math.min(2.8, progress * 4.5);
                for (int petal = 0; petal < 5; petal++) {
                    double angle = (2 * Math.PI * petal) / 5 + (ticks * 0.01);
                    Color color = bloomColors[petal % bloomColors.length];
                    for (double step = 0.2; step <= petalLength; step += 0.22) {
                        double lift = Math.sin((step / Math.max(0.4, petalLength)) * Math.PI) * (1.0 + progress);
                        Location loc = center.clone().add(
                                Math.cos(angle) * step,
                                0.5 + lift,
                                Math.sin(angle) * step
                        );
                        spawnParticles(viewers, loc, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(color, 1.0f));
                    }

                    if (ticks % 10 == 0) {
                        Location tip = center.clone().add(
                                Math.cos(angle) * petalLength,
                                0.8 + Math.sin(progress * Math.PI) * 1.5,
                                Math.sin(angle) * petalLength
                        );
                        spawnParticles(viewers, tip, Particle.END_ROD, 2, 0.05, 0.05, 0.05, 0.01);
                    }
                }

                if (ticks % 14 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1.2, 0), Particle.END_ROD, 8, 0.25, 0.7, 0.25, 0.02);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeAstralVerdict(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("astral_verdict", 120);
            final List<Location> stars = Arrays.asList(
                    center.clone().add(-2.6, 5.4, -0.8),
                    center.clone().add(-1.2, 6.0, 1.6),
                    center.clone().add(0.6, 5.6, -1.9),
                    center.clone().add(2.1, 6.2, 0.7),
                    center.clone().add(1.1, 5.2, 2.0)
            );

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                Color arcane = Color.fromRGB(165, 205, 255);
                Color gold = Color.fromRGB(255, 220, 110);

                for (int point = 0; point < 18; point++) {
                    double angle = (2 * Math.PI * point) / 18;
                    Location outer = center.clone().add(Math.cos(angle) * 2.8, 0.08, Math.sin(angle) * 2.8);
                    spawnParticles(viewers, outer, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(arcane, 1.0f));
                }

                for (int edge = 0; edge < 3; edge++) {
                    double angle = (2 * Math.PI * edge) / 3 - (Math.PI / 2);
                    Location a = center.clone().add(Math.cos(angle) * 1.4, 0.1, Math.sin(angle) * 1.4);
                    Location b = center.clone().add(Math.cos(angle + (2 * Math.PI / 3)) * 1.4, 0.1,
                            Math.sin(angle + (2 * Math.PI / 3)) * 1.4);
                    Vector between = b.toVector().subtract(a.toVector());
                    for (int segment = 0; segment <= 7; segment++) {
                        Location line = a.clone().add(between.clone().multiply(segment / 7.0));
                        spawnParticles(viewers, line, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0, dust(gold, 0.95f));
                    }
                }

                if (ticks % 8 == 0) {
                    for (Location star : stars) {
                        spawnParticles(viewers, star, Particle.END_ROD, 2, 0.04, 0.04, 0.04, 0.01);
                    }

                    for (int i = 0; i < stars.size() - 1; i++) {
                        Location start = stars.get(i);
                        Location end = stars.get(i + 1);
                        Vector between = end.toVector().subtract(start.toVector());
                        for (int segment = 0; segment <= 5; segment++) {
                            Location link = start.clone().add(between.clone().multiply(segment / 5.0));
                            spawnParticles(viewers, link, Particle.ENCHANTMENT_TABLE, 1, 0.02, 0.02, 0.02, 0.0);
                        }
                    }
                }

                if (ticks > 75) {
                    double beamY = 6.0 - ((ticks - 75) * 0.22);
                    if (beamY > 0.5) {
                        Location beam = center.clone().add(0, beamY, 0);
                        spawnParticles(viewers, beam, Particle.END_ROD, 4, 0.08, 0.12, 0.08, 0.01);
                        spawnParticles(viewers, beam, Particle.REDSTONE, 2, 0.0, 0.0, 0.0, 0.0, dust(gold, 1.1f));
                    }
                }

                if (ticks == 104) {
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.FLASH, 4, 0.35, 0.6, 0.35, 0.03);
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.FIREWORKS_SPARK, 20, 0.8, 1.2, 0.8, 0.08);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }

    void executeTimeSnap(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("time_snap", 90);
            final Color start = Color.fromRGB(170, 220, 255);
            final Color end = Color.fromRGB(255, 255, 255);

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                if (ticks < 34) {
                    double progress = ticks / 34.0;
                    double radius = 0.8 + (progress * 2.6);
                    for (int ring = 0; ring < 3; ring++) {
                        double y = 0.4 + (ring * 0.55);
                        for (int point = 0; point < 18; point++) {
                            double angle = (2 * Math.PI * point) / 18 + (ring * 0.12);
                            Location afterImage = center.clone().add(
                                    Math.cos(angle) * radius,
                                    y,
                                    Math.sin(angle) * radius
                            );
                            spawnParticles(viewers, afterImage, Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.0,
                                    dust(blend(start, end, progress), 0.95f));
                        }
                    }
                } else if (ticks < 48) {
                    if (ticks % 4 == 0) {
                        spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.REVERSE_PORTAL, 8, 0.2, 0.5, 0.2, 0.02);
                        spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.ENCHANTMENT_TABLE, 6, 0.3, 0.6, 0.3, 0.02);
                    }
                } else if (ticks == 48) {
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.FLASH, 5, 0.4, 0.4, 0.4, 0.04);
                    spawnParticles(viewers, center.clone().add(0, 1.0, 0), Particle.END_ROD, 18, 0.8, 0.8, 0.8, 0.05);
                } else {
                    double waveRadius = 0.8 + ((ticks - 48) * 0.12);
                    for (int point = 0; point < 18; point++) {
                        double angle = (2 * Math.PI * point) / 18;
                        Location ripple = center.clone().add(
                                Math.cos(angle) * waveRadius,
                                0.8,
                                Math.sin(angle) * waveRadius
                        );
                        spawnParticles(viewers, ripple, Particle.END_ROD, 1, 0.02, 0.02, 0.02, 0.0);
                        if (ticks % 6 == 0) {
                            spawnParticles(viewers, ripple, Particle.ENCHANTMENT_TABLE, 1, 0.02, 0.02, 0.02, 0.0);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        activeTasks.add(task);
    }
    void executeVoidConsumption(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("void_consumption", 90);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                for (int spiral = 0; spiral < (performanceMode ? 2 : 4); spiral++) {
                    double angle = (ticks * 0.3) + (spiral * Math.PI / 2);
                    double radius = 6 - (progress * 5);
                    double height = Math.sin(ticks * 0.2) * 1.5;
                    
                    Location spiralLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    spawnParticles(viewers, spiralLoc, Particle.PORTAL, 8, 0.2, 0.2, 0.2, 0.1);
                    spawnParticles(viewers, spiralLoc, Particle.SPELL_WITCH, 3, 0.1, 0.1, 0.1, 0.05);
                }
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
    void executePhoenixRebirth(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("phoenix_rebirth", 100);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                for (int wing = 0; wing < 2; wing++) {
                    double wingDirection = wing == 0 ? 1 : -1;
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
                        spawnParticles(viewers, wingLoc, Particle.FLAME, 3, 0.1, 0.1, 0.1, 0.02);
                        spawnParticles(viewers, wingLoc, Particle.LAVA, 1, 0.05, 0.05, 0.05, 0.0);
                        if (wingProgress > 0.7) {
                            spawnParticles(viewers, wingLoc, Particle.FIREWORKS_SPARK, 2, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                }
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
    void executeCelestialGateway(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("celestial_gateway", 110);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                double portalSize = 2.5;
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
                        spawnParticles(viewers, ringLoc, Particle.PORTAL, 2, 0.05, 0.05, 0.05, 0.02);
                        spawnParticles(viewers, ringLoc, Particle.END_ROD, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                Location centerPortal = center.clone().add(0, 2, 0);
                spawnParticles(viewers, centerPortal, Particle.PORTAL, 15, 0.5, 0.1, 0.5, 0.1);
                spawnParticles(viewers, centerPortal, Particle.ENCHANTMENT_TABLE, 8, 0.3, 0.3, 0.3, 0.05);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    void executeElectricOverload(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("electric_overload", 80);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
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
                if (ticks % 5 == 0) {
                    spawnParticles(viewers, center.clone().add(0, 1, 0), Particle.ELECTRIC_SPARK, 20, 1, 1, 1, 0.2);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    void executeCrystalGarden(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("crystal_garden", 140);
            final List<CrystalSpire> crystals = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
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
                for (CrystalSpire crystal : crystals) {
                    crystal.update();
                    crystal.render(viewers);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        activeTasks.add(task);
    }
    
    void executeSpectralHaunt(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("spectral_haunt", 100);
            @Override
            public void run() {
                if (ticks >= maxTicks) { cancel(); return; }
                double opacity = 1.0 - ((double) ticks / maxTicks);
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
    
    void executeLaserLightShow(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("laser_light_show", 120);
            final Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.PURPLE};
            @Override
            public void run() {
                if (ticks >= maxTicks) { cancel(); return; }
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
    
    void executeMeteorImpact(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("meteor_impact", 100);
            boolean impacted = false;
            @Override
            public void run() {
                if (ticks >= maxTicks) { cancel(); return; }
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
    
    void executeBioluminescentBloom(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("bioluminescent_bloom", 90);
            final List<Spore> spores = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
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
                Iterator<Spore> iterator = spores.iterator();
                while (iterator.hasNext()) {
                    Spore spore = iterator.next();
                    spore.update();
                    
                    if (spore.age > 60 || spore.location.getY() < center.getY() - 1) {
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
                        spawnParticles(viewers, spore.location, Particle.SPORE_BLOSSOM_AIR, 2, 0.05, 0.05, 0.05, 0.01);
                        if (spore.age % 10 == 0) {
                            spawnParticles(viewers, spore.location, Particle.VILLAGER_HAPPY, 1, 0.02, 0.02, 0.02, 0.01);
                        }
                    }
                }
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
    
    void executeTimeFracture(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("time_fracture", 80);
            final List<Fracture> fractures = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
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
                for (Fracture fracture : fractures) {
                    fracture.update();
                    fracture.render(viewers);
                }
                if (ticks % 5 == 0) {
                    Location timeCenter = center.clone().add(0, 2, 0);
                    spawnParticles(viewers, timeCenter, Particle.ENCHANTMENT_TABLE, 15, 1, 1, 1, 0.1);
                    spawnParticles(viewers, timeCenter, Particle.PORTAL, 8, 0.5, 0.5, 0.5, 0.05);
                }
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
    
    void executeDivineAscension(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("divine_ascension", 130);
            @Override
            public void run() {
                if (ticks >= maxTicks) { cancel(); return; }
                double progress = (double) ticks / maxTicks;
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
    
    void executeToxicMeltdown(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("toxic_meltdown", 110);
            final List<ToxicBubble> bubbles = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / maxTicks;
                if (ticks % 6 == 0 && bubbles.size() < (performanceMode ? 15 : 30)) {
                    bubbles.add(new ToxicBubble(
                            center.clone().add(
                                    (random.nextDouble() - 0.5) * progress * 10,
                                    0.1,
                                    (random.nextDouble() - 0.5) * progress * 10
                            )
                    ));
                }
                Iterator<ToxicBubble> iterator = bubbles.iterator();
                while (iterator.hasNext()) {
                    ToxicBubble bubble = iterator.next();
                    bubble.update();
                    
                    if (bubble.age > 50) {
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
                        spawnParticles(viewers, bubble.location, Particle.SLIME, 3, 0.1, 0.1, 0.1, 0.01);
                        if (bubble.age % 15 == 0) {
                            spawnParticles(viewers, bubble.location, Particle.ITEM_CRACK, 1, 0.05, 0.05, 0.05, 0.01,
                                    new ItemStack(Material.SLIME_BALL));
                        }
                    }
                }
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
    
    void executeQuantumCollapse(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("quantum_collapse", 90);
            @Override
            public void run() {
                if (ticks >= maxTicks) { cancel(); return; }
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
    
    void executeSupernova(Location center, List<Player> viewers) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration("supernova", 60);
            boolean exploded = false;
            @Override
            public void run() {
                if (ticks >= maxTicks) { cancel(); return; }
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
