package org.flennn.lightkilleffects.effect;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
public enum EffectType {
    
    LIGHTNING_STORM("lightning_storm", "Lightning Storm", Material.LIGHTNING_ROD, 
            Particle.ELECTRIC_SPARK, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 60, 5, 4),
    
    SOLAR_EXPLOSION("solar_explosion", "Solar Explosion", Material.ORANGE_CONCRETE, 
            Particle.FLAME, Sound.ENTITY_GENERIC_EXPLODE, 80, 8, 5),
    
    FROZEN_BURST("frozen_burst", "Frozen Burst", Material.PACKED_ICE, 
            Particle.SNOWFLAKE, Sound.BLOCK_GLASS_BREAK, 70, 6, 4),
    
    NEON_RAVE("neon_rave", "Neon Rave", Material.REDSTONE_LAMP, 
            Particle.DUST, Sound.BLOCK_NOTE_BLOCK_BASS, 100, 10, 6),
    
    STARFALL_CASCADE("starfall_cascade", "Starfall Cascade", Material.NETHER_STAR, 
            Particle.END_ROD, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 120, 7, 8),
    
    PRISMATIC_SHATTER("prismatic_shatter", "Prismatic Shatter", Material.GLASS, 
            Particle.FIREWORK, Sound.BLOCK_GLASS_BREAK, 60, 4, 3),
    
    VOID_CONSUMPTION("void_consumption", "Void Consumption", Material.OBSIDIAN, 
            Particle.PORTAL, Sound.ENTITY_WITHER_SPAWN, 90, 6, 5),
    
    PHOENIX_REBIRTH("phoenix_rebirth", "Phoenix Rebirth", Material.BLAZE_POWDER, 
            Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT, 100, 8, 6),
    
    CELESTIAL_GATEWAY("celestial_gateway", "Celestial Gateway", Material.END_PORTAL_FRAME, 
            Particle.PORTAL, Sound.BLOCK_END_PORTAL_SPAWN, 110, 7, 4),
    
    ELECTRIC_OVERLOAD("electric_overload", "Electric Overload", Material.REDSTONE, 
            Particle.ELECTRIC_SPARK, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 80, 9, 5),
    
    CRYSTAL_GARDEN("crystal_garden", "Crystal Garden", Material.EMERALD_BLOCK, 
            Particle.HAPPY_VILLAGER, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 140, 5, 6),
    
    SPECTRAL_HAUNT("spectral_haunt", "Spectral Haunt", Material.SKELETON_SKULL, 
            Particle.SOUL, Sound.ENTITY_GHAST_AMBIENT, 100, 4, 3),
    
    LASER_LIGHT_SHOW("laser_light_show", "Laser Light Show", Material.BEACON, 
            Particle.END_ROD, Sound.BLOCK_BEACON_ACTIVATE, 120, 8, 7),
    
    METEOR_IMPACT("meteor_impact", "Meteor Impact", Material.MAGMA_BLOCK, 
            Particle.LAVA, Sound.ENTITY_GENERIC_EXPLODE, 100, 6, 5),
    
    BIOLUMINESCENT_BLOOM("bioluminescent_bloom", "Bioluminescent Bloom", Material.GLOWSTONE_DUST, 
            Particle.SPORE_BLOSSOM_AIR, Sound.BLOCK_GRASS_BREAK, 90, 4, 4),
    
    TIME_FRACTURE("time_fracture", "Time Fracture", Material.CLOCK, 
            Particle.ENCHANT, Sound.ENTITY_ENDERMAN_TELEPORT, 80, 6, 4),
    
    DIVINE_ASCENSION("divine_ascension", "Divine Ascension", Material.GOLDEN_APPLE, 
            Particle.TOTEM_OF_UNDYING, Sound.ENTITY_PLAYER_LEVELUP, 130, 7, 5),
    
    TOXIC_MELTDOWN("toxic_meltdown", "Toxic Meltdown", Material.SLIME_BALL, 
            Particle.ITEM_SLIME, Sound.ENTITY_SLIME_SQUISH, 110, 6, 5),
    
    QUANTUM_COLLAPSE("quantum_collapse", "Quantum Collapse", Material.ENDER_PEARL, 
            Particle.REVERSE_PORTAL, Sound.ENTITY_ENDERMAN_DEATH, 90, 5, 4),
    
    SUPERNOVA("supernova", "Supernova", Material.GLOWSTONE, 
            Particle.FLASH, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 60, 10, 8),

    STORM_CAGE("storm_cage", "Storm Cage", Material.LIGHTNING_ROD,
            Particle.ELECTRIC_SPARK, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 100, 8, 5),

    PHANTOM_WALTZ("phantom_waltz", "Phantom Waltz", Material.SOUL_LANTERN,
            Particle.SOUL, Sound.ENTITY_GHAST_AMBIENT, 120, 5, 4),

    MIRROR_BREAK("mirror_break", "Mirror Break", Material.TINTED_GLASS,
            Particle.END_ROD, Sound.BLOCK_GLASS_BREAK, 90, 6, 4),

    ROYAL_EXECUTION("royal_execution", "Royal Execution", Material.GOLDEN_HELMET,
            Particle.TOTEM_OF_UNDYING, Sound.ENTITY_PLAYER_LEVELUP, 110, 6, 4),

    SAKURA_COLLAPSE("sakura_collapse", "Sakura Collapse", Material.PINK_PETALS,
            Particle.HAPPY_VILLAGER, Sound.BLOCK_GRASS_BREAK, 110, 6, 5),

    ALCHEMY_FAILURE("alchemy_failure", "Alchemy Failure", Material.BREWING_STAND,
            Particle.WITCH, Sound.BLOCK_BREWING_STAND_BREW, 120, 7, 5),

    RUNIC_FORGE("runic_forge", "Runic Forge", Material.ANVIL,
            Particle.FLAME, Sound.BLOCK_ANVIL_PLACE, 95, 7, 4),

    CELESTIAL_BLOOM("celestial_bloom", "Celestial Bloom", Material.AMETHYST_SHARD,
            Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 130, 6, 5),

    ASTRAL_VERDICT("astral_verdict", "Astral Verdict", Material.END_CRYSTAL,
            Particle.ENCHANT, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 130, 8, 6),

    TIME_SNAP("time_snap", "Time Snap", Material.CLOCK,
            Particle.ENCHANT, Sound.ENTITY_ENDERMAN_TELEPORT, 100, 6, 4);
    
    private final String configKey;
    private final String displayName;
    private final Material iconMaterial;
    private final Particle primaryParticle;
    private final Sound sound;
    private final int duration;
    private final int intensity;
    private final int radius;
    
    EffectType(String configKey, String displayName, Material iconMaterial, 
               Particle primaryParticle, Sound sound, int duration, int intensity, int radius) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.primaryParticle = primaryParticle;
        this.sound = sound;
        this.duration = duration;
        this.intensity = intensity;
        this.radius = radius;
    }
    
    public String getConfigKey() {
        return configKey;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getIconMaterial() {
        return iconMaterial;
    }
    
    public Particle getPrimaryParticle() {
        return primaryParticle;
    }
    
    public Sound getSound() {
        return sound;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public int getIntensity() {
        return intensity;
    }
    
    public int getRadius() {
        return radius;
    }
    
    public String getPermissionNode() {
        return "killeffects.use." + configKey;
    }
    public static EffectType fromConfigKey(String configKey) {
        for (EffectType effect : values()) {
            if (effect.getConfigKey().equalsIgnoreCase(configKey)) {
                return effect;
            }
        }
        return null;
    }
    public static EffectType fromDisplayName(String displayName) {
        for (EffectType effect : values()) {
            if (effect.getDisplayName().equalsIgnoreCase(displayName)) {
                return effect;
            }
        }
        return null;
    }
    public boolean requiresPermission() {
        return this != PRISMATIC_SHATTER && this != BIOLUMINESCENT_BLOOM && this != STARFALL_CASCADE;
    }
}
