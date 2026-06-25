package ru.syntaxteam.teamstorage.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import ru.syntaxteam.teamstorage.model.City;

import java.util.UUID;

public final class SelectionMenuHolder implements InventoryHolder {
    private final UUID playerId;
    private final City city;

    public SelectionMenuHolder(UUID playerId, City city) {
        this.playerId = playerId;
        this.city = city;
    }

    public UUID playerId() {
        return playerId;
    }

    public City city() {
        return city;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("SelectionMenuHolder does not own a static inventory.");
    }
}
