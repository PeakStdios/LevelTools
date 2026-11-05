package me.byteful.plugin.leveltools.api.trigger.impl;

import me.byteful.plugin.leveltools.api.trigger.Trigger;
import me.byteful.plugin.leveltools.api.trigger.TriggerContext;
import me.byteful.plugin.leveltools.api.trigger.TriggerIds;
import me.byteful.plugin.leveltools.profile.trigger.TriggerProfile;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class BlockBreakTrigger implements Trigger {
    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.BLOCK_BREAK;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return false;
        }

        TriggerProfile profile = context.getTriggerProfile();
        Material blockType = block.getType();

        if (!profile.isSourceAllowed(blockType.name())) {
            return false;
        }

        if (profile.getSettings().isRequireCorrectTool()
                && !isCorrectTool(context.getItem().getType(), blockType)) {
            return false;
        }

        return true;
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return 0;
        }

        TriggerProfile profile = context.getTriggerProfile();
        return profile.calculateXpModifier(block.getType().name());
    }

    /**
     * Checks the vanilla "mineable" block tags. Tools without a matching tag (swords, shears, ...) are not restricted.
     */
    public static boolean isCorrectTool(@NotNull Material tool, @NotNull Material block) {
        final Tag<Material> tag = getMineableTag(tool);
        return tag == null || tag.isTagged(block);
    }

    @Nullable
    private static Tag<Material> getMineableTag(@NotNull Material tool) {
        final String name = tool.name();
        if (name.endsWith("_PICKAXE")) {
            return Tag.MINEABLE_PICKAXE;
        }
        if (name.endsWith("_AXE")) {
            return Tag.MINEABLE_AXE;
        }
        if (name.endsWith("_SHOVEL")) {
            return Tag.MINEABLE_SHOVEL;
        }
        if (name.endsWith("_HOE")) {
            return Tag.MINEABLE_HOE;
        }
        return null;
    }
}
