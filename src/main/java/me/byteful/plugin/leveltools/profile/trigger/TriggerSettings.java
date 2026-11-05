package me.byteful.plugin.leveltools.profile.trigger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TriggerSettings {
    private final ClickMode clickMode;
    private final boolean requireCorrectTool;

    private TriggerSettings(@Nullable ClickMode clickMode, boolean requireCorrectTool) {
        this.clickMode = clickMode;
        this.requireCorrectTool = requireCorrectTool;
    }

    @NotNull
    public static TriggerSettings empty() {
        return new TriggerSettings(null, false);
    }

    @NotNull
    public static TriggerSettings forClick(@NotNull ClickMode mode) {
        return new TriggerSettings(mode, false);
    }

    @NotNull
    public static TriggerSettings forBlockBreak(boolean requireCorrectTool) {
        return new TriggerSettings(null, requireCorrectTool);
    }

    @Nullable
    public ClickMode getClickMode() {
        return clickMode;
    }

    /**
     * BLOCK_BREAK only: if true, XP is only granted when the held tool is the
     * vanilla-intended tool for the block (minecraft:mineable/* block tags).
     */
    public boolean isRequireCorrectTool() {
        return requireCorrectTool;
    }

    public enum ClickMode {
        ANY,
        ON_BLOCK,
        ON_ENTITY,
        ON_AIR;

        @NotNull
        public static ClickMode fromString(@NotNull String value) {
            try {
                return valueOf(value.toUpperCase().replace("-", "_"));
            } catch (IllegalArgumentException e) {
                return ANY;
            }
        }
    }
}
