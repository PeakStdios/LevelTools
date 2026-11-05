package me.byteful.plugin.leveltools.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves config names to registry entries. Accepts old enum style names (ENTITY_PLAYER_LEVELUP, DIG_SPEED,
 * generic.attack_speed) as well as namespaced keys (minecraft:efficiency).
 */
public final class Lookups {
    private static final Map<String, String> ENCHANT_ALIASES = Map.ofEntries(
            Map.entry("PROTECTION_ENVIRONMENTAL", "protection"),
            Map.entry("PROTECTION_FIRE", "fire_protection"),
            Map.entry("PROTECTION_FALL", "feather_falling"),
            Map.entry("PROTECTION_EXPLOSIONS", "blast_protection"),
            Map.entry("PROTECTION_PROJECTILE", "projectile_protection"),
            Map.entry("OXYGEN", "respiration"),
            Map.entry("WATER_WORKER", "aqua_affinity"),
            Map.entry("DAMAGE_ALL", "sharpness"),
            Map.entry("DAMAGE_UNDEAD", "smite"),
            Map.entry("DAMAGE_ARTHROPODS", "bane_of_arthropods"),
            Map.entry("LOOT_BONUS_MOBS", "looting"),
            Map.entry("SWEEPING_EDGE", "sweeping_edge"),
            Map.entry("DIG_SPEED", "efficiency"),
            Map.entry("DURABILITY", "unbreaking"),
            Map.entry("LOOT_BONUS_BLOCKS", "fortune"),
            Map.entry("ARROW_DAMAGE", "power"),
            Map.entry("ARROW_KNOCKBACK", "punch"),
            Map.entry("ARROW_FIRE", "flame"),
            Map.entry("ARROW_INFINITE", "infinity"),
            Map.entry("LUCK", "luck_of_the_sea")
    );

    private static final Map<String, Sound> SOUNDS_BY_ENUM_NAME = new HashMap<>();

    private Lookups() {
    }

    @Nullable
    public static Material material(@NotNull String name) {
        final String trimmed = name.trim();
        final Material material = Material.matchMaterial(trimmed);
        return material != null ? material : Material.getMaterial(trimmed.toUpperCase(Locale.ROOT));
    }

    @Nullable
    public static Enchantment enchantment(@NotNull String name) {
        final String trimmed = name.trim();
        final String alias = ENCHANT_ALIASES.get(trimmed.toUpperCase(Locale.ROOT));
        final NamespacedKey key = NamespacedKey.fromString(alias != null ? alias : trimmed.toLowerCase(Locale.ROOT));
        return key == null ? null : Registry.ENCHANTMENT.get(key);
    }

    @Nullable
    public static Attribute attribute(@NotNull String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        final int colon = normalized.indexOf(':');
        final String namespace = colon >= 0 ? normalized.substring(0, colon) : NamespacedKey.MINECRAFT;
        String path = colon >= 0 ? normalized.substring(colon + 1) : normalized;

        // generic.attack_speed, GENERIC_ATTACK_SPEED, player.block_interaction_range ...
        for (String prefix : new String[]{"generic.", "generic_", "player.", "player_", "zombie.", "zombie_"}) {
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                break;
            }
        }
        path = path.replace('.', '_');

        final NamespacedKey key = NamespacedKey.fromString(namespace + ":" + path);
        return key == null ? null : Registry.ATTRIBUTE.get(key);
    }

    @Nullable
    public static synchronized Sound sound(@NotNull String name) {
        if (SOUNDS_BY_ENUM_NAME.isEmpty()) {
            for (Sound sound : Registry.SOUNDS) {
                final NamespacedKey key = Registry.SOUNDS.getKey(sound);
                if (key != null) {
                    SOUNDS_BY_ENUM_NAME.put(key.getKey().replace('.', '_').toUpperCase(Locale.ROOT), sound);
                }
            }
        }

        final String trimmed = name.trim();
        final Sound byEnumName = SOUNDS_BY_ENUM_NAME.get(trimmed.toUpperCase(Locale.ROOT));
        if (byEnumName != null) {
            return byEnumName;
        }

        final NamespacedKey key = NamespacedKey.fromString(trimmed.toLowerCase(Locale.ROOT));
        return key == null ? null : Registry.SOUNDS.get(key);
    }

    public static boolean isNumber(@NotNull String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int parseInt(@NotNull String value) {
        return (int) Double.parseDouble(value);
    }
}
