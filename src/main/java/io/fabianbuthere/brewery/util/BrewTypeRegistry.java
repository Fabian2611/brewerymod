package io.fabianbuthere.brewery.util;

import java.util.HashMap;
import java.util.Map;

public class BrewTypeRegistry {
    private static final Map<String, BrewType> REGISTRY = new HashMap<>();

    public static void register(BrewType brewType) {
        REGISTRY.put(brewType.id(), brewType);
    }

    public static BrewType get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }

    public static void clear() {
        REGISTRY.clear();
    }

    public static Map<String, BrewType> getAll() {
        return REGISTRY;
    }
}

