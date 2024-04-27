package cn.superiormc.manyouitems.methods;

import cn.superiormc.manyouitems.ErrorManager;
import cn.superiormc.manyouitems.TextUtil;
import cn.superiormc.manyouitems.hooks.ItemsHook;
import cn.superiormc.manyouitems.util.CommonUtil;
import com.google.common.base.Enums;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;
import java.util.*;

public class BuildItem {
    public static ItemStack buildItemStack(Player player,
                                           ConfigurationSection section,
                                           int amount,
                                           Map<String, ItemStack> savedItemMap,
                                           String... args) {
        ItemStack item = new ItemStack(Material.STONE);

        // Material
        String materialKey = section.getString("material");
        if (materialKey != null) {
            Material material = Material.getMaterial(materialKey.toUpperCase());
            if (material != null) {
                item.setType(material);
            } else {
                ItemStack savedItem = savedItemMap.get(section.getString("material", ""));
                if (savedItem != null) {
                    item = savedItem;
                }
            }
        } else {
            String pluginName = section.getString("hook-plugin");
            String itemID = section.getString("hook-item");
            if (pluginName != null && itemID != null) {
                if (pluginName.equals("MMOItems") && !itemID.contains(";;")) {
                    itemID = section.getString("hook-item-type") + ";;" + itemID;
                } else if (pluginName.equals("EcoArmor") && !itemID.contains(";;")) {
                    itemID = itemID + ";;" + section.getString("hook-item-type");
                }
                ItemStack hookItem = ItemsHook.getHookItem(pluginName, itemID);
                if (hookItem != null) {
                    item = hookItem;
                }
            }
        }

        // Amount
        if (amount > 0) {
            item.setAmount(amount);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Custom Name
        String displayNameKey = section.getString("name", section.getString("display"));
        if (displayNameKey != null) {
            meta.setDisplayName(TextUtil.parse(player, CommonUtil.modifyString(displayNameKey, args)));
        }

        // Item Name
        if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
            String itemNameKey = section.getString("item-name");
            if (itemNameKey != null) {
                if (itemNameKey.isEmpty()) {
                    meta.setItemName(" ");
                } else {
                    meta.setItemName(TextUtil.parse(player, CommonUtil.modifyString(itemNameKey, args)));
                }
            }
        }

        // Lore
        List<String> loreKey = section.getStringList("lore");
        if (!loreKey.isEmpty()) {
            meta.setLore(TextUtil.getListWithColorAndPAPI(player, CommonUtil.modifyList(loreKey, args)));
        }

        // Custom Model Data
        if (CommonUtil.getMajorVersion() >= 14) {
            int customModelDataKey = section.getInt("custom-model-data", section.getInt("cmd", -1));
            if (customModelDataKey > 0) {
                meta.setCustomModelData(customModelDataKey);
            }
        }

        // Max Stack
        if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
            int maxStackKey = section.getInt("max-stack", -1);
            if (maxStackKey > 0 && maxStackKey < 100) {
                meta.setMaxStackSize(maxStackKey);
            }
        }

        // Food
        if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
            ConfigurationSection foodKey = section.getConfigurationSection("food");
            FoodComponent foodComponent = meta.getFood();
            if (foodKey != null) {
                double eatSecond = foodKey.getDouble("eat-seconds", -1);
                if (eatSecond >= 0) {
                    foodComponent.setEatSeconds((float) eatSecond);
                }
                if (foodKey.contains("can-always-eat")) {
                    foodComponent.setCanAlwaysEat(foodKey.getBoolean("can-always-eat"));
                }
                int foodNutrition = foodKey.getInt("nutrition", -1);
                if (foodNutrition > 0) {
                    foodComponent.setNutrition(foodNutrition);
                }
                double foodSaturation = foodKey.getDouble("saturation", -1);
                if (foodSaturation > 0) {
                    foodComponent.setSaturation((float) foodSaturation);
                }
                for (String effects : section.getStringList("effects")) {
                    String[] effectParseResult = effects.replace(" ", "").split(",");
                    if (effectParseResult.length < 4) {
                        continue;
                    }
                    PotionEffectType potionEffectType = Registry.EFFECT.get(CommonUtil.parseNamespacedKey(effectParseResult[0]));
                    if (potionEffectType != null) {
                        PotionEffect potionEffect = new PotionEffect(potionEffectType,
                                Integer.parseInt(effectParseResult[1]),
                                Integer.parseInt(effectParseResult[2]),
                                effectParseResult.length < 5 || Boolean.parseBoolean(effectParseResult[3]),
                                effectParseResult.length < 6 || Boolean.parseBoolean(effectParseResult[4]),
                                effectParseResult.length < 7 || Boolean.parseBoolean(effectParseResult[5]));
                        foodComponent.addEffect(potionEffect, Float.parseFloat(effectParseResult[effectParseResult.length - 1]));
                    }
                }
            }
        }

        // Fire Resistant
        if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
            if (section.get("fire-resistant") != null) {
                meta.setFireResistant(section.getBoolean("fire-resistant"));
            }
        }

        // Hide Tooltip
        if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
            if (section.get("hide-tool-tip") != null) {
                meta.setHideTooltip(section.getBoolean("hide-tool-tip"));
            }
        }

        // Unbreakable
        if (section.get("unbreakable") != null) {
            meta.setUnbreakable(section.getBoolean("unbreakable"));
        }

        // Rarity
        if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
            String rarityKey = section.getString("rarity");
            if (rarityKey != null) {
                meta.setRarity(Enums.getIfPresent(ItemRarity.class, rarityKey).or(ItemRarity.COMMON));
            }
        }

        // Flag
        List<String> itemFlagKey = section.getStringList("flags");
        if (!itemFlagKey.isEmpty()) {
            for (String flag : itemFlagKey) {
                flag = flag.toUpperCase();
                ItemFlag itemFlag = Enums.getIfPresent(ItemFlag.class, flag).orNull();
                if (itemFlag != null) {
                    meta.addItemFlags(itemFlag);
                }
            }
        }

        // Enchantments
        ConfigurationSection enchantsKey = section.getConfigurationSection("enchants");
        if (enchantsKey != null) {
            for (String ench : enchantsKey.getKeys(false)) {
                Enchantment vanillaEnchant = Registry.ENCHANTMENT.get(CommonUtil.parseNamespacedKey(ench.toLowerCase()));
                if (vanillaEnchant != null) {
                    meta.addEnchant(vanillaEnchant, enchantsKey.getInt(ench), true);
                }
            }
        }

        // Attribute
        ConfigurationSection attributesKey = section.getConfigurationSection("attributes");
        if (attributesKey != null) {
            for (String attribute : attributesKey.getKeys(false)) {
                Attribute attributeInst = Enums.getIfPresent(Attribute.class, attribute.toUpperCase(Locale.ENGLISH)).orNull();
                if (attributeInst == null) {
                    continue;
                }
                ConfigurationSection subSection = attributesKey.getConfigurationSection(attribute);
                if (subSection == null) {
                    continue;
                }
                String attribId = subSection.getString("id");
                UUID id = attribId != null ? UUID.fromString(attribId) : UUID.randomUUID();

                String attribName = subSection.getString("name");
                double attribAmount = subSection.getDouble("amount");
                String attribOperation = subSection.getString("operation");

                if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
                    String attribSlot = subSection.getString("slot");

                    EquipmentSlotGroup slot = EquipmentSlotGroup.ANY;

                    if (attribSlot != null) {
                        EquipmentSlotGroup targetSlot = EquipmentSlotGroup.getByName(attribSlot);
                        if (targetSlot != null) {
                            slot = targetSlot;
                        }
                    }

                    if (attribName != null && attribOperation != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                id,
                                attribName,
                                attribAmount,
                                Enums.getIfPresent(AttributeModifier.Operation.class, attribOperation)
                                        .or(AttributeModifier.Operation.ADD_NUMBER),
                                slot);

                        meta.addAttributeModifier(attributeInst, modifier);
                    }
                } else {
                    String attribSlot = subSection.getString("slot");

                    EquipmentSlot slot = attribSlot != null ? Enums.getIfPresent(EquipmentSlot.class, attribSlot).or(EquipmentSlot.HAND) : null;

                    if (attribName != null && attribOperation != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                id,
                                attribName,
                                attribAmount,
                                Enums.getIfPresent(AttributeModifier.Operation.class, attribOperation)
                                        .or(AttributeModifier.Operation.ADD_NUMBER),
                                slot);

                        meta.addAttributeModifier(attributeInst, modifier);
                    }
                }
            }
        }

        // Damage
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int damageKey = section.getInt("damage", -1);
            damageable.setDamage(damageKey);
            if (CommonUtil.getMajorVersion() > 20 || (CommonUtil.getMajorVersion() == 20 && CommonUtil.getMinorVersion() >= 5)) {
                int maxDamageKey = section.getInt("max-damage", -1);
                if (maxDamageKey > 0) {
                    damageable.setMaxDamage(maxDamageKey);
                }
            }
        }

        // Stored Enchantments
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) meta;
            ConfigurationSection storedEnchantsKey = section.getConfigurationSection("stored-enchants");
            if (storedEnchantsKey != null) {
                for (String ench : storedEnchantsKey.getKeys(false)) {
                    Enchantment vanillaEnchant = Registry.ENCHANTMENT.get(CommonUtil.parseNamespacedKey(ench.toLowerCase()));
                    if (vanillaEnchant != null) {
                        enchantmentStorageMeta.addStoredEnchant(vanillaEnchant, storedEnchantsKey.getInt(ench), true);
                    }
                }
            }
        }

        // Banner
        if (meta instanceof BannerMeta) {
            BannerMeta banner = (BannerMeta) meta;
            ConfigurationSection bannerPatternsKey = section.getConfigurationSection("patterns");

            if (bannerPatternsKey != null) {
                for (String pattern : bannerPatternsKey.getKeys(false)) {
                    PatternType type = Enums.getIfPresent(PatternType.class, pattern.toUpperCase()).or(PatternType.BASE);
                    String bannerColor = bannerPatternsKey.getString(pattern);
                    if (bannerColor != null) {
                        DyeColor color = Enums.getIfPresent(DyeColor.class, bannerColor.toUpperCase()).or(DyeColor.WHITE);
                        banner.addPattern(new Pattern(color, type));
                    }
                }
            }
        }

        // Potion
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) meta;
            String basePotionType = section.getString("base-effect");
            if (basePotionType != null) {
                potionMeta.setBasePotionType(Enums.getIfPresent(PotionType.class, basePotionType.toUpperCase()).orNull());
            }
            for (String effects : section.getStringList("effects")) {
                String[] effectParseResult = effects.replace(" ", "").split(",");
                if (effectParseResult.length < 3) {
                    continue;
                }
                PotionEffectType potionEffectType = Registry.EFFECT.get(CommonUtil.parseNamespacedKey(effectParseResult[0]));
                if (potionEffectType != null) {
                    PotionEffect potionEffect = new PotionEffect(potionEffectType,
                    Integer.parseInt(effectParseResult[1]),
                            Integer.parseInt(effectParseResult[2]),
                            effectParseResult.length < 4 || Boolean.parseBoolean(effectParseResult[3]),
                            effectParseResult.length < 5 || Boolean.parseBoolean(effectParseResult[4]),
                            effectParseResult.length < 6 || Boolean.parseBoolean(effectParseResult[5]));
                    potionMeta.addCustomEffect(potionEffect, true);
                }
            }
            String potionColor = section.getString("color");
            if (potionColor != null) {
                potionMeta.setColor(CommonUtil.parseColor(potionColor));
            }
        }

        // Armor Trim
        if (CommonUtil.getMajorVersion() >= 20) {
            if (meta instanceof ArmorMeta) {
                ArmorMeta armorMeta = (ArmorMeta) meta;
                ConfigurationSection trim = section.getConfigurationSection("trim");
                if (trim != null) {
                    String trimMaterialKey = trim.getString("material");
                    String trimPatternKey = trim.getString("pattern");
                    if (trimMaterialKey != null && trimPatternKey != null) {
                        NamespacedKey trimMaterialNamespacedKey = CommonUtil.parseNamespacedKey(trimMaterialKey);
                        NamespacedKey trimPatternNamespacedKey = CommonUtil.parseNamespacedKey(trimPatternKey);
                        if (trimMaterialNamespacedKey != null && trimPatternNamespacedKey != null) {
                            TrimMaterial trimMaterial = Registry.TRIM_MATERIAL.get(trimMaterialNamespacedKey);
                            TrimPattern trimPattern = Registry.TRIM_PATTERN.get(trimPatternNamespacedKey);
                            if (trimMaterial != null && trimPattern != null) {
                                armorMeta.setTrim(new ArmorTrim(trimMaterial, trimPattern));
                            }
                        }
                    }
                }
            }
        }

        // Leather Armor Color
        if (meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leather = (LeatherArmorMeta) meta;
            String colorKey = section.getString("color");
            if (colorKey != null) {
                leather.setColor(CommonUtil.parseColor(colorKey));
            }
        }

        // Axolotl Bucket
        if (CommonUtil.getMajorVersion() >= 17) {
            if (meta instanceof AxolotlBucketMeta) {
                AxolotlBucketMeta bucket = (AxolotlBucketMeta) meta;
                String variantStr = section.getString("color");
                if (variantStr != null) {
                    Axolotl.Variant variant = Enums.getIfPresent(Axolotl.Variant.class, variantStr.toUpperCase()).orNull();
                    if (variant != null) {
                        bucket.setVariant(variant);
                    }
                }
            }
        }

        // Tropical Fish Bucket
        if (CommonUtil.getMajorVersion() >= 14) {
            if (meta instanceof TropicalFishBucketMeta) {
                TropicalFishBucketMeta tropical = (TropicalFishBucketMeta) meta;
                String colorKey = section.getString("color");
                String patternColorKey = section.getString("pattern-color");
                String patternKey = section.getString("pattern");
                if (colorKey != null && patternColorKey != null && patternKey != null) {
                    DyeColor color = Enums.getIfPresent(DyeColor.class, colorKey).or(DyeColor.WHITE);
                    DyeColor patternColor = Enums.getIfPresent(DyeColor.class, patternColorKey).or(DyeColor.WHITE);
                    TropicalFish.Pattern pattern = Enums.getIfPresent(TropicalFish.Pattern.class, patternKey).or(TropicalFish.Pattern.BETTY);

                    tropical.setBodyColor(color);
                    tropical.setPatternColor(patternColor);
                    tropical.setPattern(pattern);
                }
            }
        }

        // Skull
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            String skullTextureNameKey = section.getString("skull-meta", section.getString("skull"));
            if (skullTextureNameKey != null) {
                GameProfile profile = new GameProfile(UUID.randomUUID(), "");
                profile.getProperties().put("textures", new Property("textures", skullTextureNameKey));
                try {
                    Method mtd = skullMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                    mtd.setAccessible(true);
                    mtd.invoke(skullMeta, profile);
                } catch (Exception exception) {
                    ErrorManager.errorManager.sendErrorMessage("§x§9§8§F§B§9§8[ManyouItems] §cError: Can not parse skull texture in a item!");
                }
            }
        }

        // Firework
        if (meta instanceof FireworkMeta) {
            FireworkMeta firework = (FireworkMeta) meta;
            firework.setPower(section.getInt("power"));

            ConfigurationSection fireworkKey = section.getConfigurationSection("firework");
            if (fireworkKey != null) {
                FireworkEffect.Builder builder = FireworkEffect.builder();
                for (String fws : fireworkKey.getKeys(false)) {
                    ConfigurationSection fw = fireworkKey.getConfigurationSection(fws);

                    if (fw != null) {
                        builder.flicker(fw.getBoolean("flicker"));
                        builder.trail(fw.getBoolean("trail"));
                        String fireworkType = fw.getString("type");
                        if (fireworkType != null) {
                            builder.with(Enums.getIfPresent(FireworkEffect.Type.class, fireworkType.toUpperCase())
                                    .or(FireworkEffect.Type.STAR));
                        }

                        ConfigurationSection colorsSection = fw.getConfigurationSection("colors");
                        if (colorsSection != null) {
                            List<Color> colors = new ArrayList<>();
                            for (String colorStr : colorsSection.getStringList("base")) {
                                colors.add(CommonUtil.parseColor(colorStr));
                            }
                            builder.withColor(colors);

                            colors = new ArrayList<>();
                            for (String colorStr : colorsSection.getStringList("fade")) {
                                colors.add(CommonUtil.parseColor(colorStr));
                            }
                            builder.withFade(colors);
                        }
                    }

                    firework.addEffect(builder.build());
                }
            }
        }

        // Suspicious Stew
        if (CommonUtil.getMajorVersion() >= 14) {
            if (meta instanceof SuspiciousStewMeta) {
                SuspiciousStewMeta stewMeta = (SuspiciousStewMeta) meta;
                for (String effects : section.getStringList("effects")) {
                    String[] effectParseResult = effects.replace(" ", "").split(",");
                    if (effectParseResult.length < 3) {
                        continue;
                    }
                    PotionEffectType potionEffectType = Registry.EFFECT.get(CommonUtil.parseNamespacedKey(effectParseResult[0]));
                    if (potionEffectType != null) {
                        PotionEffect potionEffect = new PotionEffect(potionEffectType,
                                Integer.parseInt(effectParseResult[1]),
                                Integer.parseInt(effectParseResult[2]),
                                effectParseResult.length < 4 || Boolean.parseBoolean(effectParseResult[3]),
                                effectParseResult.length < 5 || Boolean.parseBoolean(effectParseResult[4]),
                                effectParseResult.length < 6 || Boolean.parseBoolean(effectParseResult[5]));
                        stewMeta.addCustomEffect(potionEffect, true);
                    }
                }
            }
        }

        // Bundle
        if (CommonUtil.getMajorVersion() >= 17) {
            if (meta instanceof BundleMeta) {
                BundleMeta bundleMeta = (BundleMeta) meta;
                ConfigurationSection bundleContentKey = section.getConfigurationSection("contents");

                if (bundleContentKey != null) {
                    for (String key : bundleContentKey.getKeys(false)) {
                        ConfigurationSection contentItemSection = bundleContentKey.getConfigurationSection(key);
                        if (contentItemSection != null) {
                            bundleMeta.addItem(buildItemStack(player,
                                    contentItemSection,
                                    contentItemSection.getInt("amount"),
                                    savedItemMap,
                                    args));
                        }
                    }
                }
            }
        }

        // Block
        if (meta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
            BlockState state = blockStateMeta.getBlockState();

            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                String spawnerKey = section.getString("spawner");
                if (spawnerKey != null) {
                    spawner.setSpawnedType(Enums.getIfPresent(EntityType.class, spawnerKey.toUpperCase()).orNull());
                    spawner.update(true);
                    blockStateMeta.setBlockState(spawner);
                }
            } else if (state instanceof ShulkerBox) {
                ConfigurationSection shulkerContentKey = section.getConfigurationSection("contents");
                if (shulkerContentKey != null) {
                    ShulkerBox box = (ShulkerBox) state;
                    for (String key : shulkerContentKey.getKeys(false)) {
                        ConfigurationSection contentItemSection = shulkerContentKey.getConfigurationSection(key);
                        if (contentItemSection != null) {
                            box.getInventory().setItem(Integer.parseInt(key), buildItemStack(player,
                                    contentItemSection,
                                    contentItemSection.getInt("amount"),
                                    savedItemMap,
                                    args));
                        }
                    }
                    box.update(true);
                    blockStateMeta.setBlockState(box);
                }
            } else if (CommonUtil.getMajorVersion() >= 20 && state instanceof BrushableBlock) {
                BrushableBlock brushableBlock = (BrushableBlock) state;
                ConfigurationSection brushableContentKey = section.getConfigurationSection("content");
                if (brushableContentKey != null) {
                    brushableBlock.setItem(buildItemStack(player, brushableContentKey, brushableContentKey.getInt("amount"), savedItemMap, args));
                }
                blockStateMeta.setBlockState(brushableBlock);
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}
