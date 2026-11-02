package com.example.kitechin.common;

import com.example.kitechin.exception.ApiException;

import java.util.List;

public final class ServiceCities {

    public static final List<String> ALL = List.of("Lucknow", "Patna", "Kanpur", "Delhi", "Varanasi");

    private ServiceCities() {
    }

    public static String canonical(String city) {
        if (city == null) {
            throw ApiException.badRequest("City is required");
        }
        return ALL.stream()
                .filter(item -> item.equalsIgnoreCase(city.trim()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("City must be one of: " + String.join(", ", ALL)));
    }
}
