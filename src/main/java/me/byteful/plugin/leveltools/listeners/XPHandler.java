package me.byteful.plugin.leveltools.listeners;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.event.LevelToolsLevelIncreaseEvent;
import me.byteful.plugin.leveltools.api.event.LevelToolsXPIncreaseEvent;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.api.trigger.TriggerSlot;
import me.byteful.plugin.leveltools.config.ConfigManager;
import me.byteful.plugin.leveltools.profile.ProfileManager;
import me.byteful.plugin.leveltools.profile.display.DisplayProfile;
import me.byteful.plugin.leveltools.profile.item.ItemProfile;
import me.byteful.plugin.leveltools.profile.reward.RewardProfile;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import me.byteful.plugin.leveltools.util.Text;
import me.byteful.plugin.leveltools.util.XPBooster;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.byteful.plugin.leveltools.util.Text.formatMoney;

public final class XPHandler {
    private final ProfileManager profileManager;

    public XPHandler(@NotNull ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public void handle(
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            @Nullable TriggerSlot slot,
            @NotNull LevelToolsItem tool,
            double modifier,
            boolean deferCommit
    ) {
        modifier = Math.max(0, XPBooster.apply(player, modifier));
        double newXp = LevelToolsUtil.round(tool.getXp() + modifier, 1);

        LevelToolsXPIncreaseEvent xpEvent = new LevelToolsXPIncreaseEvent(tool, player, newXp, newXp, false);
        Bukkit.getPluginManager().callEvent(xpEvent);

        if (xpEvent.isCancelled()) {
            return;
        }

        tool.setXp(xpEvent.getNewXp());

        double maxXp = processLevelUps(player, itemProfile, tool);

        if (deferCommit) {
            commitDeferred(player, slot, tool, maxXp);
        } else {
            updateItem(player, slot, tool.getItemStack(maxXp));
        }

        showActionBar(tool, player, itemProfile, maxXp);
    }

    private void commitDeferred(
            @NotNull Player player,
            @Nullable TriggerSlot slot,
            @NotNull LevelToolsItem tool,
            double maxXp
    ) {
        ItemStack backing = LevelToolsUtil.getBackingItemStack(tool);
        if (backing == null) {
            updateItem(player, slot, tool.getItemStack(maxXp));
            return;
        }

        Material expectedType = backing.getType();
        LevelToolsPlugin.getInstance().getScheduler().locationDelayed(() -> {
            if (!player.isOnline()) {
                return;
            }

            ItemStack live = LevelToolsUtil.getItemInSlot(player, slot);
            if (live == null || live.getType() != expectedType) {
                return;
            }

            if (LevelToolsUtil.rebindLevelToolsItem(tool, live)) {
                updateItem(player, slot, tool.getItemStack(maxXp));
            }
        }, player.getLocation(), 1);
    }

    /**
     * Levels the tool up as long as its XP reaches the required XP (handles multiple levels at once,
     * e.g. after /leveltools xp or a big XP boost). Rewards are applied for every level reached.
     *
     * @return the required XP for the tool's (new) current level
     */
    public double processLevelUps(
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            @NotNull LevelToolsItem tool
    ) {
        double maxXp = getMaxXp(player, itemProfile, tool);
        boolean leveled = false;
        int guard = 0;

        while (maxXp > 0 && tool.getXp() >= maxXp && guard++ < 10000) {
            if (!handleLevelUp(tool, player, itemProfile, maxXp)) {
                break;
            }
            leveled = true;
            handleReward(tool, player, itemProfile);
            maxXp = getMaxXp(player, itemProfile, tool);
        }

        // Catch-up for rewards of the current level (unchanged behaviour)
        handleReward(tool, player, itemProfile);

        if (leveled) {
            playLevelUpSound(player);
        }

        return maxXp;
    }

    private boolean handleLevelUp(
            @NotNull LevelToolsItem tool,
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            double maxXp
    ) {
        int newLevel = tool.getLevel() + 1;
        int maxLevel = itemProfile.getMaxLevel();

        if (newLevel > maxLevel) {
            tool.setXp(maxXp);
            return false;
        }

        LevelToolsLevelIncreaseEvent levelEvent = new LevelToolsLevelIncreaseEvent(tool, player, newLevel, false);
        Bukkit.getPluginManager().callEvent(levelEvent);

        if (levelEvent.isCancelled()) {
            return false;
        }

        tool.setXp(LevelToolsUtil.round(Math.abs(tool.getXp() - maxXp), 1));
        tool.setLevel(levelEvent.getNewLevel());

        if (levelEvent.getNewLevel() == maxLevel) {
            tool.setXp(0);
            tool.setLevel(maxLevel);
        }

        return true;
    }

    private void playLevelUpSound(@NotNull Player player) {
        ConfigManager.LevelUpSound sound = LevelToolsPlugin.getInstance().getConfigManager().getSettings().getLevelUpSound();
        if (sound != null) {
            sound.play(player);
        }
    }

    private void showActionBar(
            @NotNull LevelToolsItem tool,
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            double maxXp
    ) {
        DisplayProfile displayProfile = profileManager.getDisplayProfileFor(itemProfile);
        if (displayProfile == null) {
            return;
        }

        DisplayProfile.ActionBarDisplay actionBar = displayProfile.getActionBarDisplay();
        if (!actionBar.isEnabled()) {
            return;
        }

        String progressBar = displayProfile.getProgressBar().buildProgressBar(tool.getXp(), maxXp);
        Component text = Text.component(actionBar.getText()
                .replace("{progress_bar}", progressBar)
                .replace("{xp}", String.valueOf(tool.getXp()))
                .replace("{max_xp}", String.valueOf(maxXp))
                .replace("{level}", String.valueOf(tool.getLevel()))
                .replace("{max_xp_formatted}", formatMoney(maxXp))
                .replace("{xp_formatted}", formatMoney(tool.getXp())));

        LevelToolsUtil.sendActionBar(player, text);
    }

    private void handleReward(
            @NotNull LevelToolsItem tool,
            @NotNull Player player,
            @NotNull ItemProfile itemProfile
    ) {
        RewardProfile rewardProfile = profileManager.getRewardProfileFor(itemProfile);
        if (rewardProfile == null) {
            return;
        }

        LevelToolsUtil.applyRewards(tool, player, rewardProfile);
    }

    private double getMaxXp(@NotNull Player player, @NotNull ItemProfile itemProfile, @NotNull LevelToolsItem tool) {
        return LevelToolsUtil.getMaxXp(player, itemProfile, tool);
    }

    private void updateItem(@NotNull Player player, @Nullable TriggerSlot slot, @NotNull ItemStack itemStack) {
        LevelToolsUtil.setItemInSlot(player, slot, itemStack);
    }
}
