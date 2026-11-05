package me.byteful.plugin.leveltools.api.item;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface LevelToolsItem {
    @NotNull
    ItemStack getItemStack();

    @NotNull
    default ItemStack getItemStack(double maxXp) {
        return getItemStack();
    }

    int getLevel();

    void setLevel(int level);

    double getXp();

    void setXp(double xp);

    int getLastHandledReward();

    void setLastHandledReward(int rewardKey);

    default double getMaxXp() {
        return LevelToolsPlugin.getInstance().getXpFormulaRegistry().evaluateGlobalMaxXp(getLevel());
    }

    void enchant(Enchantment enchantment, int level);

    /**
     * Current level of the enchantment, including enchants that were queued by rewards but are not on the stack yet.
     */
    default int getEnchantmentLevel(@NotNull Enchantment enchantment) {
        return getItemStack().getEnchantmentLevel(enchantment);
    }

    void modifyAttribute(String attribute, double modifier);
}
