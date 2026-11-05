package me.byteful.plugin.leveltools.api.item.impl;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.persistence.PersistentDataContainerView;
import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import me.byteful.plugin.leveltools.util.Lookups;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Stores level/xp in the item's persistent data. Reads go through the read-only PDC view and writes through
 * data components, so no ItemMeta copy is made per update.
 */
public class PDCLevelToolsItem implements LevelToolsItem {
    @NotNull
    public static final NamespacedKey
            LEVEL_KEY = new NamespacedKey(LevelToolsPlugin.getInstance(), "levelToolsLevel"),
            XP_KEY = new NamespacedKey(LevelToolsPlugin.getInstance(), "levelToolsXp"),
            LAST_REWARD_KEY = new NamespacedKey(LevelToolsPlugin.getInstance(), "levelToolsReward");

    private static final String MODIFIER_NAMESPACE = "leveltools";

    @NotNull
    private ItemStack stack;
    @NotNull
    private Map<Enchantment, Integer> enchantments = new HashMap<>();
    @NotNull
    private Map<String, Double> attributes = new LinkedHashMap<>();
    private int level;
    private double xp;
    private int lastHandledReward;

    public PDCLevelToolsItem(@NotNull ItemStack stack) {
        this.stack = stack;
        readState();
    }

    private void readState() {
        final PersistentDataContainerView pdc = stack.getPersistentDataContainer();
        final Integer storedLevel = pdc.get(LEVEL_KEY, PersistentDataType.INTEGER);
        final Double storedXp = pdc.get(XP_KEY, PersistentDataType.DOUBLE);
        final Integer storedLastReward = pdc.get(LAST_REWARD_KEY, PersistentDataType.INTEGER);
        level = storedLevel == null ? 0 : Math.max(storedLevel, 0);
        xp = storedXp == null ? 0.0D : Math.max(storedXp, 0.0D);
        lastHandledReward = storedLastReward == null ? -1 : storedLastReward;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        return getItemStack(getMaxXp());
    }

    @Override
    public @NotNull ItemStack getItemStack(double maxXp) {
        final ItemStack built = stack.clone();

        built.editPersistentDataContainer(pdc -> {
            pdc.set(LEVEL_KEY, PersistentDataType.INTEGER, level);
            pdc.set(XP_KEY, PersistentDataType.DOUBLE, xp);
            pdc.set(LAST_REWARD_KEY, PersistentDataType.INTEGER, lastHandledReward);
        });

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            built.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        applyAttributes(built);
        LevelToolsUtil.applyDisplay(built, level, xp, maxXp);

        return built;
    }

    private void applyAttributes(@NotNull ItemStack built) {
        if (attributes.isEmpty()) {
            return;
        }

        // Start from the effective modifiers (includes the item's vanilla defaults like attack damage),
        // otherwise setting the component would wipe them.
        final ItemAttributeModifiers current = built.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();

        if (current != null) {
            for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
                if (!MODIFIER_NAMESPACE.equals(entry.modifier().getKey().getNamespace())) {
                    builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup());
                }
            }
        }

        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            final Attribute attribute = Lookups.attribute(entry.getKey());
            if (attribute == null) {
                LevelToolsPlugin.getInstance().getLogger().warning("Unknown attribute in reward: " + entry.getKey());
                continue;
            }

            final String path = entry.getKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            builder.addModifier(
                    attribute,
                    new AttributeModifier(
                            new NamespacedKey(MODIFIER_NAMESPACE, path),
                            entry.getValue(),
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.ANY),
                    EquipmentSlotGroup.ANY);
        }

        built.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(level, 0);
    }

    @Override
    public double getXp() {
        return xp;
    }

    @Override
    public void setXp(double xp) {
        this.xp = Math.max(xp, 0.0D);
    }

    @Override
    public int getLastHandledReward() {
        return lastHandledReward;
    }

    @Override
    public void setLastHandledReward(int rewardKey) {
        this.lastHandledReward = rewardKey;
    }

    @Override
    public void enchant(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }

    @Override
    public int getEnchantmentLevel(@NotNull Enchantment enchantment) {
        final Integer queued = enchantments.get(enchantment);
        return queued != null ? queued : stack.getEnchantmentLevel(enchantment);
    }

    @Override
    public void modifyAttribute(String attribute, double modifier) {
        attributes.put(attribute, modifier);
    }

    @NotNull
    public ItemStack getStack() {
        return stack;
    }

    public void setStack(@NotNull ItemStack stack) {
        this.stack = stack;
        readState();
    }

    @NotNull
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(@NotNull Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public @NotNull Map<String, Double> getAttributes() {
        return attributes;
    }

    public void setAttributes(@NotNull Map<String, Double> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PDCLevelToolsItem that)) return false;
        return level == that.level
                && Double.compare(that.xp, xp) == 0
                && lastHandledReward == that.lastHandledReward
                && stack.equals(that.stack)
                && enchantments.equals(that.enchantments)
                && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, enchantments, attributes, level, xp, lastHandledReward);
    }

    @Override
    public String toString() {
        return "PDCLevelToolsItem{stack=" + stack + ", enchantments=" + enchantments + ", attributes=" + attributes
                + ", level=" + level + ", xp=" + xp + ", lastHandledReward=" + lastHandledReward + '}';
    }
}
