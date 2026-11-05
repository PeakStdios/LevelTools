# LevelTools 2.2.1 Paper fork

Target: Paper/Purpur 26.3, Java 25. Built on upstream v2.2.0.

## Behaviour changes
- /leveltools xp, addxp and normal XP gain level up in a loop (several levels at once), rewards for every level.
- New /leveltools addxp <n>.
- BLOCK_BREAK gives XP only with the intended tool (Tag.MINEABLE_*). Per trigger profile:
  `require_correct_tool: true|false` (default true).
- Name and lore are written as components (italic off). Our lore lines carry an invisible marker, other lore stays.
- Reward names: enchants and sounds accept old enum names (DIG_SPEED, ENTITY_PLAYER_LEVELUP) and namespaced keys.
  Attributes: generic.attack_speed, GENERIC_ATTACK_SPEED, attack_speed, minecraft:attack_speed.
- Attribute rewards keep the item's vanilla attack damage/speed.

## Removed
Folia scheduler, legacy anvil listener, NBT storage (force_nbt), NBT-API, XSeries, bStats, update checker,
MorePersistentDataTypes, pre 1.13 code paths.

## Perf
Level/XP are read through the read-only PDC view and written through data components, no ItemMeta copy.
Materials in farming trigger use EnumSet.contains instead of XMaterial string lookups.
`LevelToolsUtil.round` uses plain double math instead of BigDecimal.

## Old test items
Items rendered by upstream 2.2.0 keep their old lore block (legacy marker). Reset them with /leveltools reset
or take a fresh tool, otherwise the lore block appears twice.
