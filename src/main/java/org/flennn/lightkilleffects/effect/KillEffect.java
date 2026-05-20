package org.flennn.lightkilleffects.effect;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

interface KillEffect {
    void run(Location center, List<Player> viewers);
}
