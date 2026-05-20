package org.flennn.lightkilleffects.preview;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Boat;
import java.util.UUID;

public class PreviewSession {
    private final UUID playerId;
    private final String playerName;
    private final Location originalLocation;
    private final Location previewLocation;
    private Boat boatEntity;
    private long startTime;
    private boolean active;
    private boolean boatPlaced;

    public PreviewSession(Player player, Location previewLoc) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.originalLocation = player.getLocation().clone();
        this.previewLocation = previewLoc.clone();
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.boatPlaced = false;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public Location getPreviewLocation() {
        return previewLocation;
    }

    public Boat getBoatEntity() {
        return boatEntity;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBoatPlaced() {
        return boatPlaced;
    }

    public void setBoatEntity(Boat boat) {
        this.boatEntity = boat;
    }

    public void setBoatPlaced(boolean placed) {
        this.boatPlaced = placed;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBoatValid() {
        return boatEntity != null && !boatEntity.isDead() && boatEntity.isValid();
    }

    public void end() {
        this.active = false;
        if (isBoatValid()) {
            boatEntity.remove();
        }
    }

    @Override
    public String toString() {
        return "PreviewSession{" +
                "player=" + playerName +
                ", active=" + active +
                ", boatPlaced=" + boatPlaced +
                ", elapsed=" + getElapsedTime() + "ms" +
                '}';
    }
}
