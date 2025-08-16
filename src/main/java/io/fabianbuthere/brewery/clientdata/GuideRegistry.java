package io.fabianbuthere.brewery.clientdata;

import io.fabianbuthere.brewery.clientdata.model.GuideEntry;

import java.util.*;
import java.util.stream.Collectors;

public final class GuideRegistry {
    private static final Map<String, GuideEntry> ENTRIES = new HashMap<>();

    private GuideRegistry() {}

    public static void clear() {
        ENTRIES.clear();
    }

    public static void register(GuideEntry entry) {
        ENTRIES.put(entry.id(), entry);
    }

    public static List<GuideEntry> getAllOrdered() {
        return ENTRIES.values().stream()
                .sorted(Comparator.<GuideEntry>comparingInt(GuideEntry::order)
                        .thenComparing(GuideEntry::id))
                .collect(Collectors.toList());
    }
}
