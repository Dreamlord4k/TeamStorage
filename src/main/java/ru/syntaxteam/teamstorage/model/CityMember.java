package ru.syntaxteam.teamstorage.model;

import java.util.UUID;

public record CityMember(UUID uuid, String lastName, int cityId) {
}
