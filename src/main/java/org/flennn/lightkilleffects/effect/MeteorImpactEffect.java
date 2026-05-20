package org.flennn.lightkilleffects.effect;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

final class MeteorImpactEffect implements KillEffect {
    private final KillEffectManager manager;

    MeteorImpactEffect(KillEffectManager manager) {
        this.manager = manager;
    }

    public void run(Location center, List<Player> viewers) {
        manager.executeMeteorImpact(center, viewers);
    }
}
