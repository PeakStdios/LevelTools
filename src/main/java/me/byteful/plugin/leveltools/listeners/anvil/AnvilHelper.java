package me.byteful.plugin.leveltools.listeners.anvil;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.AnvilCombineMode;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.model.LevelAndXPModel;
import me.byteful.plugin.leveltools.profile.item.ItemProfile;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AnvilHelper {
    private AnvilHelper() {}

    static boolean isHandledAnvilAction(ItemStack firstItem, ItemStack secondItem, ItemStack result) {
        return result != null
                && LevelToolsUtil.isSupportedTool(result.getType())
                && firstItem != null
                && secondItem != null
                && LevelToolsUtil.isSupportedTool(firstItem.getType());
    }

    static ItemStack createUpdatedResult(
            ItemStack firstItem, ItemStack secondItem, ItemStack result, HumanEntity viewer) {
        final LevelToolsItem finalItem = LevelToolsUtil.createLevelToolsItem(result);

        if (LevelToolsUtil.isSupportedTool(secondItem.getType())) {
            final AnvilCombineMode mode = LevelToolsPlugin.getInstance().getAnvilCombineMode();
            final LevelAndXPModel first =
                    LevelAndXPModel.fromItem(LevelToolsUtil.createLevelToolsItem(firstItem));
            final LevelAndXPModel second =
                    LevelAndXPModel.fromItem(LevelToolsUtil.createLevelToolsItem(secondItem));
            final LevelAndXPModel finished = mode.getHandler().apply(first, second);
            finalItem.setLevel(LevelToolsUtil.clampLevel(LevelToolsUtil.getItemProfile(result.getType()), finished.getLevel()));
            finalItem.setXp(finished.getXp());
        } else {
            final LevelToolsItem original = LevelToolsUtil.createLevelToolsItem(firstItem);
            finalItem.setLevel(original.getLevel());
            finalItem.setXp(original.getXp());
            finalItem.setLastHandledReward(original.getLastHandledReward());
        }

        return getResultItem(finalItem, viewer, result);
    }

    static ItemStack getResultItem(LevelToolsItem item, HumanEntity viewer, ItemStack result) {
        final Player player = viewer instanceof Player ? (Player) viewer : null;
        final ItemProfile profile = LevelToolsUtil.getItemProfile(result.getType());

        return LevelToolsUtil.getItemStack(item, player, profile);
    }

    static boolean shouldBlockEnchantedBookResult(ItemStack firstItem, ItemStack secondItem, ItemStack result) {
        return LevelToolsPlugin.getInstance().getConfigManager().getSettings().isPreventEnchantedBooksOnLevelToolsItems()
            && result != null
            && firstItem != null
            && secondItem != null
            && LevelToolsUtil.isSupportedTool(result.getType())
            && LevelToolsUtil.isSupportedTool(firstItem.getType())
            && secondItem.getType() == Material.ENCHANTED_BOOK;
    }
}
