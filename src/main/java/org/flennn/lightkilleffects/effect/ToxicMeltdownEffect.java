package org.flennn.lightkilleffects.effect;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

final class ToxicMeltdownEffect implements KillEffect {
    private final KillEffectManager manager;

    ToxicMeltdownEffect(KillEffectManager manager) {
        this.manager = manager;
    }

    public void run(Location center, List<Player> viewers) {
        manager.executeToxicMeltdown(center, viewers);
    }
}
