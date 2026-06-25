package ru.syntaxteam.teamstorage.storage;

import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.model.City;
import ru.syntaxteam.teamstorage.model.CityMember;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SQLiteStorage {
    private final JavaPlugin plugin;
    private Connection connection;

    public SQLiteStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void init() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IllegalStateException("Cannot create plugin data folder.");
            }

            File databaseFile = new File(plugin.getDataFolder(), "teamstorage.db");
            File legacyDatabaseFile = new File(plugin.getDataFolder(), "citystorage.db");
            if (!databaseFile.exists() && legacyDatabaseFile.isFile()) {
                java.nio.file.Files.copy(legacyDatabaseFile.toPath(), databaseFile.toPath());
                plugin.getLogger().info("Migrated citystorage.db to teamstorage.db.");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA foreign_keys=ON");
                statement.executeUpdate("PRAGMA busy_timeout=5000");
                statement.executeUpdate("PRAGMA journal_mode=WAL");
                // FULL prioritizes inventory durability over a small amount of write throughput.
                statement.executeUpdate("PRAGMA synchronous=FULL");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cities (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                            tag TEXT NOT NULL DEFAULT '',
                            tag_color TEXT NOT NULL DEFAULT 'green'
                        )
                        """);
                addColumnIfMissing(statement, "cities", "tag", "TEXT NOT NULL DEFAULT ''");
                addColumnIfMissing(statement, "cities", "tag_color", "TEXT NOT NULL DEFAULT 'green'");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS city_members (
                            uuid TEXT PRIMARY KEY,
                            last_name TEXT NOT NULL,
                            city_id INTEGER NOT NULL,
                            FOREIGN KEY(city_id) REFERENCES cities(id) ON DELETE CASCADE
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS player_settings (
                            uuid TEXT PRIMARY KEY,
                            city_storage_enabled INTEGER NOT NULL DEFAULT 1,
                            city_chat_enabled INTEGER NOT NULL DEFAULT 0
                        )
                        """);
                addColumnIfMissing(statement, "player_settings", "city_chat_enabled", "INTEGER NOT NULL DEFAULT 0");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS city_storages (
                            city_id INTEGER PRIMARY KEY,
                            data BLOB,
                            updated_at INTEGER NOT NULL,
                            FOREIGN KEY(city_id) REFERENCES cities(id) ON DELETE CASCADE
                        )
                        """);
            }
        } catch (SQLException | java.io.IOException exception) {
            throw new IllegalStateException("Cannot initialize SQLite database.", exception);
        }
    }

    private void addColumnIfMissing(Statement statement, String table, String column, String definition) throws SQLException {
        try {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException exception) {
            if (!exception.getMessage().toLowerCase(java.util.Locale.ROOT).contains("duplicate column name")) {
                throw exception;
            }
        }
    }

    public synchronized Optional<City> findCityByName(String name) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, name, tag, tag_color FROM cities WHERE name = ? COLLATE NOCASE")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readCity(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot find city by name.", exception);
        }
    }

    public synchronized Optional<City> findCityById(int cityId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, name, tag, tag_color FROM cities WHERE id = ?")) {
            statement.setInt(1, cityId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readCity(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot find city by id.", exception);
        }
    }

    public synchronized List<City> findAllCities() {
        List<City> cities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, name, tag, tag_color FROM cities ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                cities.add(readCity(resultSet));
            }
            return cities;
        } catch (SQLException exception) {
            throw new StorageException("Cannot list cities.", exception);
        }
    }

    public synchronized City createCity(String name, String tag) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO cities(name, tag, tag_color) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, tag);
            statement.setString(3, "green");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new City(keys.getInt(1), name, tag, "green");
                }
                throw new StorageException("City id was not generated.");
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot create city.", exception);
        }
    }

    public synchronized Optional<City> findCityByTag(String tag) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, name, tag, tag_color FROM cities WHERE tag = ? COLLATE NOCASE")) {
            statement.setString(1, tag);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readCity(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot find city by tag.", exception);
        }
    }

    public synchronized void updateCityTag(int cityId, String tag) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE cities SET tag = ? WHERE id = ?")) {
            statement.setString(1, tag);
            statement.setInt(2, cityId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot update city tag.", exception);
        }
    }

    public synchronized void updateCityTagColor(int cityId, String color) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE cities SET tag_color = ? WHERE id = ?")) {
            statement.setString(1, color);
            statement.setInt(2, cityId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot update city tag color.", exception);
        }
    }

    public synchronized void deleteCity(int cityId) {
        try (PreparedStatement members = connection.prepareStatement("DELETE FROM city_members WHERE city_id = ?");
             PreparedStatement storage = connection.prepareStatement("DELETE FROM city_storages WHERE city_id = ?");
             PreparedStatement city = connection.prepareStatement("DELETE FROM cities WHERE id = ?")) {
            members.setInt(1, cityId);
            members.executeUpdate();
            storage.setInt(1, cityId);
            storage.executeUpdate();
            city.setInt(1, cityId);
            city.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot delete city.", exception);
        }
    }

    public synchronized void setMember(UUID uuid, String lastName, int cityId) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO city_members(uuid, last_name, city_id) VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET last_name = excluded.last_name, city_id = excluded.city_id
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, lastName);
            statement.setInt(3, cityId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot set city member.", exception);
        }
    }

    public synchronized Optional<CityMember> findMember(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, last_name, city_id FROM city_members WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new CityMember(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("last_name"),
                            resultSet.getInt("city_id")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot find city member.", exception);
        }
    }

    public synchronized List<CityMember> findMembers(int cityId) {
        List<CityMember> members = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, last_name, city_id FROM city_members WHERE city_id = ? ORDER BY last_name")) {
            statement.setInt(1, cityId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    members.add(new CityMember(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("last_name"),
                            resultSet.getInt("city_id")
                    ));
                }
            }
            return members;
        } catch (SQLException exception) {
            throw new StorageException("Cannot find city members.", exception);
        }
    }

    public synchronized void removeMember(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM city_members WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot remove city member.", exception);
        }
    }

    public synchronized boolean isCityStorageEnabled(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT city_storage_enabled FROM player_settings WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next() || resultSet.getInt("city_storage_enabled") == 1;
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot read player settings.", exception);
        }
    }

    public synchronized boolean isCityChatEnabled(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT city_chat_enabled FROM player_settings WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("city_chat_enabled") == 1;
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot read player chat settings.", exception);
        }
    }

    public synchronized void setCityStorageEnabled(UUID uuid, boolean enabled) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_settings(uuid, city_storage_enabled) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET city_storage_enabled = excluded.city_storage_enabled
                """)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, enabled ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot save player settings.", exception);
        }
    }

    public synchronized void setCityChatEnabled(UUID uuid, boolean enabled) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_settings(uuid, city_chat_enabled) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET city_chat_enabled = excluded.city_chat_enabled
                """)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, enabled ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot save player chat settings.", exception);
        }
    }

    public synchronized byte[] loadStorageData(int cityId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT data FROM city_storages WHERE city_id = ?")) {
            statement.setInt(1, cityId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBytes("data");
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new StorageException("Cannot load city storage.", exception);
        }
    }

    public synchronized void saveStorageData(int cityId, byte[] data) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO city_storages(city_id, data, updated_at) VALUES (?, ?, ?)
                ON CONFLICT(city_id) DO UPDATE SET data = excluded.data, updated_at = excluded.updated_at
                """)) {
            statement.setInt(1, cityId);
            statement.setBytes(2, data);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Cannot save city storage.", exception);
        }
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Cannot close SQLite connection: " + exception.getMessage());
        }
    }

    private City readCity(ResultSet resultSet) throws SQLException {
        String tag = resultSet.getString("tag");
        String tagColor = resultSet.getString("tag_color");
        return new City(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                tag == null ? "" : tag,
                tagColor == null || tagColor.isBlank() ? "green" : tagColor
        );
    }
}
