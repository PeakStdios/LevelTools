package me.byteful.plugin.leveltools.api.trigger.impl;

import me.byteful.plugin.leveltools.api.trigger.Trigger;
import me.byteful.plugin.leveltools.api.trigger.TriggerContext;
import me.byteful.plugin.leveltools.api.trigger.TriggerIds;
import me.byteful.plugin.leveltools.profile.trigger.TriggerProfile;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public final class FarmingTrigger implements Trigger {
    private static final Set<Material> TILLABLE_BLOCKS = EnumSet.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.DIRT_PATH,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT
    );

    // Crops without a meaningful growth age: harvesting them is always a full harvest.
    // Sugar cane, cactus, kelp and bamboo do have an age, but it resets as they grow taller.
    private static final Set<Material> AGELESS_CROPS = EnumSet.of(
            Material.MELON,
            Material.PUMPKIN,
            Material.SUGAR_CANE,
            Material.CACTUS,
            Material.BAMBOO,
            Material.KELP,
            Material.KELP_PLANT,
            Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM
    );

    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.FARMING;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return false;
        }

        TriggerProfile profile = context.getTriggerProfile();
        Material blockType = block.getType();

        if (context.getOriginalEventAs(PlayerInteractEvent.class) != null) {
            if (!isTillable(block)) {
                return false;
            }
            return profile.isSourceAllowed("TILL");
        }

        if (context.getOriginalEventAs(BlockBreakEvent.class) != null) {
            if (!isAgelessCrop(block)) {
                if (!(block.getBlockData() instanceof Ageable ageable)
                        || ageable.getAge() < ageable.getMaximumAge()) {
                    return false;
                }
            }

            return profile.isSourceAllowed(blockType.name());
        }

        return false;
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return 0;
        }

        TriggerProfile profile = context.getTriggerProfile();

        if (context.getOriginalEventAs(PlayerInteractEvent.class) != null) {
            return profile.calculateXpModifier("TILL");
        }

        return profile.calculateXpModifier(block.getType().name());
    }

    public static boolean isTillable(@NotNull Block block) {
        return matches(TILLABLE_BLOCKS, block);
    }

    private static boolean isAgelessCrop(@NotNull Block block) {
        return matches(AGELESS_CROPS, block);
    }

    private static boolean matches(@NotNull Set<Material> materials, @NotNull Block block) {
        return materials.contains(block.getType());
    }

    public static boolean isMatureCropSource(@NotNull Block block) {
        // Player-placed melons, pumpkins, etc. are the placed block itself, so they never count as grown.
        if (isAgelessCrop(block)) {
            return false;
        }

        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }

        return false;
    }
}
