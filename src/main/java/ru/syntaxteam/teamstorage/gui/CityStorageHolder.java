package ru.syntaxteam.teamstorage.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class CityStorageHolder implements InventoryHolder {
    private final int cityId;

    public CityStorageHolder(int cityId) {
        this.cityId = cityId;
    }

    public int cityId() {
        return cityId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("CityStorageHolder does not own a static inventory.");
    }
}
