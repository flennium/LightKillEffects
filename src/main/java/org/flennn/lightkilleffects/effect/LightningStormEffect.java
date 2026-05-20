package org.flennn.lightkilleffects.effect;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

final class LightningStormEffect implements KillEffect {
    private final KillEffectManager manager;

    LightningStormEffect(KillEffectManager manager) {
        this.manager = manager;
    }

    public void run(Location center, List<Player> viewers) {
        manager.executeLightningStorm(center, viewers);
    }
}
