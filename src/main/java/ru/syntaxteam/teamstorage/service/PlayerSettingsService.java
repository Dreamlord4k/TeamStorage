package ru.syntaxteam.teamstorage.service;

import ru.syntaxteam.teamstorage.storage.SQLiteStorage;

import java.util.UUID;

public final class PlayerSettingsService {
    private final SQLiteStorage storage;

    public PlayerSettingsService(SQLiteStorage storage) {
        this.storage = storage;
    }

    public boolean isCityStorageMenuEnabled(UUID playerId) {
        return storage.isCityStorageEnabled(playerId);
    }

    public boolean toggleCityStorageMenu(UUID playerId) {
        boolean enabled = !storage.isCityStorageEnabled(playerId);
        storage.setCityStorageEnabled(playerId, enabled);
        return enabled;
    }

    public boolean isCityChatEnabled(UUID playerId) {
        return storage.isCityChatEnabled(playerId);
    }

    public boolean toggleCityChat(UUID playerId) {
        boolean enabled = !storage.isCityChatEnabled(playerId);
        storage.setCityChatEnabled(playerId, enabled);
        return enabled;
    }
}
