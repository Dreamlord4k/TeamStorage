package ru.syntaxteam.teamstorage.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.model.CityMember;
import ru.syntaxteam.teamstorage.storage.SQLiteStorage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CityService {
    private final SQLiteStorage storage;
    private final StorageService storageService;

    public CityService(SQLiteStorage storage, StorageService storageService) {
        this.storage = storage;
        this.storageService = storageService;
    }

    public Optional<City> findCity(String name) {
        return storage.findCityByName(name);
    }

    public Optional<City> findCityByTag(String tag) {
        return storage.findCityByTag(tag);
    }

    public Optional<City> findCity(int id) {
        return storage.findCityById(id);
    }

    public Optional<City> findPlayerCity(UUID playerId) {
        return storage.findMember(playerId).flatMap(member -> storage.findCityById(member.cityId()));
    }

    public List<City> listCities() {
        return storage.findAllCities();
    }

    public City createCity(String name, String tag) {
        City city = storage.createCity(name, tag);
        storageService.createEmptyStorage(city);
        return city;
    }

    public Optional<City> updateTag(City city, String tag) {
        storage.updateCityTag(city.id(), tag);
        Optional<City> updated = storage.findCityById(city.id());
        updated.ifPresent(storageService::refreshInventoryMetadata);
        return updated;
    }

    public Optional<City> updateTagColor(City city, String color) {
        storage.updateCityTagColor(city.id(), color);
        Optional<City> updated = storage.findCityById(city.id());
        updated.ifPresent(storageService::refreshInventoryMetadata);
        return updated;
    }

    public boolean canDeleteCity(City city) {
        return storageService.isStorageEmpty(city);
    }

    public void deleteCity(City city) {
        storageService.closeCityStorageViewers(city.id());
        storageService.dropStorage(city.id());
        storage.deleteCity(city.id());
    }

    public void addPlayerToCity(UUID playerId, String lastName, City city) {
        storage.setMember(playerId, lastName, city.id());
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && !storageService.isViewingCityStorage(playerId, city.id())) {
            storageService.closeAnyCityStorage(player);
        }
    }

    public boolean removePlayer(UUID playerId) {
        Optional<CityMember> member = storage.findMember(playerId);
        if (member.isEmpty()) {
            return false;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            storageService.closeAnyCityStorage(player);
        }
        storage.removeMember(playerId);
        return true;
    }

    public List<CityMember> members(City city) {
        return storage.findMembers(city.id());
    }
}
