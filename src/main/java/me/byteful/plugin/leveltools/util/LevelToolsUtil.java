package me.byteful.plugin.leveltools.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.api.item.impl.PDCLevelToolsItem;
import me.byteful.plugin.leveltools.api.trigger.TriggerSlot;
import me.byteful.plugin.leveltools.profile.ProfileManager;
import me.byteful.plugin.leveltools.profile.display.DisplayProfile;
import me.byteful.plugin.leveltools.profile.display.ProgressBarConfig;
import me.byteful.plugin.leveltools.profile.item.ItemProfile;
import me.byteful.plugin.leveltools.profile.reward.RewardEntry;
import me.byteful.plugin.leveltools.profile.reward.RewardProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static me.byteful.plugin.leveltools.util.Text.formatMoney;

public final class LevelToolsUtil {
    /**
     * Marks lore lines that belong to LevelTools (stored as component insertion, invisible to the player).
     */
    private static final String LORE_MARKER = "leveltools";

    private static final double[] POWERS_OF_TEN = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000};

    private LevelToolsUtil() {
    }

    public static boolean isSupportedTool(Material material) {
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return false;
        }
        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return false;
        }
        return profileManager.hasMaterialProfile(material);
    }

    public static int clampLevel(@Nullable ItemProfile profile, int level) {
        return profile == null ? level : Math.min(level, profile.getMaxLevel());
    }

    @Nullable
    public static ItemProfile getItemProfile(Material material) {
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return null;
        }
        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return null;
        }
        return profileManager.getProfileForMaterial(material);
    }

    /**
     * Returns a copy of the main hand item. Use {@link #setHand} to write changes back.
     */
    public static ItemStack getHand(Player player) {
        return player.getInventory().getItemInMainHand().clone();
    }

    public static void setHand(Player player, ItemStack stack) {
        player.getInventory().setItemInMainHand(stack);
    }

    public static void setItemInSlot(@NotNull Player player, @Nullable TriggerSlot slot, @NotNull ItemStack stack) {
        if (slot == null || slot == TriggerSlot.HAND) {
            setHand(player, stack);
            return;
        }

        switch (slot) {
            case OFF_HAND -> player.getInventory().setItemInOffHand(stack);
            case HELMET -> player.getInventory().setHelmet(stack);
            case CHESTPLATE -> player.getInventory().setChestplate(stack);
            case LEGGINGS -> player.getInventory().setLeggings(stack);
            case BOOTS -> player.getInventory().setBoots(stack);
            default -> setHand(player, stack);
        }
    }

    @Nullable
    public static ItemStack getItemInSlot(@NotNull Player player, @Nullable TriggerSlot slot) {
        if (slot == null || slot == TriggerSlot.HAND) {
            return getHand(player);
        }

        return switch (slot) {
            case OFF_HAND -> player.getInventory().getItemInOffHand();
            case HELMET -> player.getInventory().getHelmet();
            case CHESTPLATE -> player.getInventory().getChestplate();
            case LEGGINGS -> player.getInventory().getLeggings();
            case BOOTS -> player.getInventory().getBoots();
            default -> getHand(player);
        };
    }

    public static String createProgressBar(double xp, double maxXp, @Nullable DisplayProfile displayProfile) {
        if (displayProfile != null) {
            return displayProfile.getProgressBar().buildProgressBar(xp, maxXp);
        }
        return ProgressBarConfig.defaultConfig().buildProgressBar(xp, maxXp);
    }

    /**
     * Rounds half up. Plain double math instead of BigDecimal, this runs several times per mined block.
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        if (Double.isNaN(value) || Double.isInfinite(value)) return value;

        if (places < POWERS_OF_TEN.length) {
            final double factor = POWERS_OF_TEN[places];
            final double scaled = value * factor;
            if (Math.abs(scaled) < 4.5e15) {
                // Math.round rounds half up for positives; mirror it for negatives so it stays half away from zero.
                return (value < 0 ? -Math.round(-scaled) : Math.round(scaled)) / factor;
            }
        }

        return java.math.BigDecimal.valueOf(value).setScale(places, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    public static int roundDown(double value) {
        return (int) value;
    }

    public static double getMaxXp(@Nullable Player player, @Nullable ItemProfile itemProfile, @NotNull LevelToolsItem tool) {
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null || instance.getXpFormulaRegistry() == null) {
            return tool.getMaxXp();
        }

        return instance.getXpFormulaRegistry().evaluateMaxXp(player, itemProfile, tool.getLevel());
    }

    @NotNull
    public static ItemStack getItemStack(
            @NotNull LevelToolsItem tool,
            @Nullable Player player,
            @Nullable ItemProfile itemProfile
    ) {
        return tool.getItemStack(getMaxXp(player, itemProfile, tool));
    }

    @Nullable
    public static ItemStack getBackingItemStack(@NotNull LevelToolsItem tool) {
        if (tool instanceof PDCLevelToolsItem pdc) {
            return pdc.getStack();
        }
        return null;
    }

    public static boolean rebindLevelToolsItem(@NotNull LevelToolsItem tool, @NotNull ItemStack stack) {
        if (!(tool instanceof PDCLevelToolsItem pdc)) {
            return false;
        }

        final int level = tool.getLevel();
        final double xp = tool.getXp();
        final int lastHandledReward = tool.getLastHandledReward();

        pdc.setStack(stack);
        tool.setLevel(level);
        tool.setXp(xp);
        tool.setLastHandledReward(lastHandledReward);

        return true;
    }

    public static LevelToolsItem createLevelToolsItem(ItemStack stack) {
        return new PDCLevelToolsItem(stack);
    }

    public static String getServerVersion() {
        return Bukkit.getMinecraftVersion();
    }

    /**
     * Writes name and lore of the display profile onto the stack. Works on data components directly, no ItemMeta.
     */
    public static void applyDisplay(@NotNull ItemStack stack, int level, double xp, double maxXp) {
        final DisplayProfile displayProfile = getDisplayProfileForMaterial(stack.getType());
        if (displayProfile == null) {
            return;
        }

        final String progressBar = createProgressBar(xp, maxXp, displayProfile);
        final String levelText = String.valueOf(level);
        final String xpText = String.valueOf(xp);
        final String maxXpText = String.valueOf(maxXp);
        final String xpFormatted = formatMoney(xp);
        final String maxXpFormatted = formatMoney(maxXp);

        final DisplayProfile.NameDisplay nameDisplay = displayProfile.getNameDisplay();
        if (nameDisplay.isEnabled()) {
            final String text = nameDisplay.getText()
                    .replace("{level}", levelText)
                    .replace("{xp}", xpText)
                    .replace("{max_xp}", maxXpText)
                    .replace("{max_xp_formatted}", maxXpFormatted)
                    .replace("{xp_formatted}", xpFormatted)
                    .replace("{progress_bar}", progressBar);

            final Component name = nameDisplay.getText().contains("{item}")
                    ? AdventureHelper.nameWithItem(text, stack.getType())
                    : Text.component(text);
            stack.setData(DataComponentTypes.CUSTOM_NAME, name);
        }

        final DisplayProfile.LoreDisplay loreDisplay = displayProfile.getLoreDisplay();
        if (loreDisplay.isEnabled()) {
            final List<Component> ours = new ArrayList<>(loreDisplay.getLines().size());
            for (String line : loreDisplay.getLines()) {
                ours.add(Text.component(line
                                .replace("{level}", levelText)
                                .replace("{xp}", xpText)
                                .replace("{max_xp}", maxXpText)
                                .replace("{progress_bar}", progressBar)
                                .replace("{max_xp_formatted}", maxXpFormatted)
                                .replace("{xp_formatted}", xpFormatted))
                        .insertion(LORE_MARKER));
            }

            stack.setData(DataComponentTypes.LORE, ItemLore.lore(mergeLore(stack, ours)));
        }
    }

    // Replaces our block of lines and keeps lore added by other plugins or players around it.
    private static List<Component> mergeLore(@NotNull ItemStack stack, @NotNull List<Component> ours) {
        final ItemLore existing = stack.getData(DataComponentTypes.LORE);
        if (existing == null || existing.lines().isEmpty()) {
            return ours;
        }

        final List<Component> lines = existing.lines();
        int start = -1;
        int end = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (LORE_MARKER.equals(lines.get(i).insertion())) {
                if (start == -1) {
                    start = i;
                }
                end = i;
            }
        }

        final List<Component> merged = new ArrayList<>(lines.size() + ours.size());
        if (start == -1) {
            merged.addAll(lines);
            merged.addAll(ours);
            return merged;
        }

        merged.addAll(lines.subList(0, start));
        merged.addAll(ours);
        merged.addAll(lines.subList(end + 1, lines.size()));
        return merged;
    }

    @Nullable
    private static DisplayProfile getDisplayProfileForMaterial(Material material) {
        ItemProfile itemProfile = getItemProfile(material);
        if (itemProfile == null) {
            return null;
        }
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return null;
        }
        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return null;
        }
        return profileManager.getDisplayProfileFor(itemProfile);
    }

    public static void handleReward(LevelToolsItem tool, Player player) {
        Material material = tool.getItemStack().getType();
        ItemProfile itemProfile = getItemProfile(material);
        if (itemProfile == null) {
            return;
        }

        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return;
        }

        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return;
        }

        RewardProfile rewardProfile = profileManager.getRewardProfileFor(itemProfile);
        if (rewardProfile == null) {
            return;
        }

        if (applyRewards(tool, player, rewardProfile)) {
            setHand(player, getItemStack(tool, player, itemProfile));
        }
    }

    public static boolean applyRewards(
            @NotNull LevelToolsItem tool, @NotNull Player player, @NotNull RewardProfile rewardProfile) {
        int level = tool.getLevel();
        if (!rewardProfile.hasRewardsForLevel(level) || tool.getLastHandledReward() == level) {
            return false;
        }

        tool.setLastHandledReward(level);
        for (RewardEntry entry : rewardProfile.getRewardsForLevel(level)) {
            entry.apply(tool, player);
        }

        return true;
    }

    public static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }
}
