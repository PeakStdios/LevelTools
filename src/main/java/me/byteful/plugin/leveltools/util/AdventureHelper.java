package me.byteful.plugin.leveltools.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;

final class AdventureHelper {
    private AdventureHelper() {
    }

    /**
     * Builds a name where {item} is replaced by the client translated item name.
     */
    static Component nameWithItem(String text, Material type) {
        final Component translatable = Component.translatable(type.translationKey());
        final TextComponent parsed = LegacyComponentSerializer.legacySection().deserialize(Text.colorize(text));

        final TextComponent.Builder result = Component.text();
        processComponent(parsed, translatable, result);

        return result.build().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static void processComponent(TextComponent component, Component translatable, TextComponent.Builder result) {
        final String content = component.content();
        if (content.contains("{item}")) {
            final String[] parts = content.split("\\{item}", -1);
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    result.append(Component.text(parts[i]).style(component.style()));
                }
                if (i < parts.length - 1) {
                    result.append(translatable.style(component.style()));
                }
            }
        } else if (!content.isEmpty()) {
            result.append(Component.text(content).style(component.style()));
        }

        for (Component child : component.children()) {
            if (child instanceof TextComponent text) {
                processComponent(text, translatable, result);
            } else {
                result.append(child);
            }
        }
    }
}
