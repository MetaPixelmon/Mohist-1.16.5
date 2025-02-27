/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2022.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.forge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.mohistmc.MohistMC;
import com.mohistmc.api.ServerAPI;
import com.mohistmc.entity.CraftCustomEntity;
import com.mohistmc.util.MohistEnumHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.item.PaintingType;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.item.Item;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import org.bukkit.Art;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.block.banner.PatternType;
import org.bukkit.craftbukkit.v1_16_R3.CraftParticle;
import org.bukkit.craftbukkit.v1_16_R3.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_16_R3.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_16_R3.potion.CraftPotionUtil;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftNamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.permissions.DefaultPermissions;

public class ForgeInjectBukkit {

    public static BiMap<DimensionType, World.Environment> environment =
            HashBiMap.create(ImmutableMap.<DimensionType, World.Environment>builder()
                    .put(DimensionType.DEFAULT_OVERWORLD, World.Environment.NORMAL)
                    .put(DimensionType.DEFAULT_NETHER, World.Environment.NETHER)
                    .put(DimensionType.DEFAULT_END, World.Environment.THE_END)
                    .build());

    public static BiMap<World.Environment, RegistryKey<Dimension>> environment0 =
            HashBiMap.create(ImmutableMap.<World.Environment, RegistryKey<Dimension>>builder()
                    .put(World.Environment.NORMAL, Dimension.OVERWORLD)
                    .put(World.Environment.NETHER, Dimension.NETHER)
                    .put(World.Environment.THE_END, Dimension.END)
                    .build());

    public static Map<Villager.Profession, ResourceLocation> profession = new HashMap<>();
    public static Map<org.bukkit.attribute.Attribute, ResourceLocation> attributemap = new HashMap<>();
    public static Map<PaintingType, Art> artMap = new HashMap<>();
    public static Map<org.bukkit.block.Biome, Biome> biomeMap = new HashMap<>();

    public static void init(){
        addEnumMaterialInItems();
        addEnumMaterialsInBlocks();
        addEnumBiome();
        addEnumEnchantment();
        addEnumEffectAndPotion();
        addEnumPattern();
        addEnumEntity();
        addEnumVillagerProfession();
        addEnumAttribute();
        addEnumArt();
        addEnumParticle();
    }


    public static void addEnumMaterialInItems(){
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation resourceLocation = item.getRegistryName();
            if(!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                // inject item materials into Bukkit for FML
                String materialName = normalizeName(resourceLocation.toString());
                int id = Item.getId(item);
                Material material = Material.addMaterial(materialName, id, item.getMaxStackSize(), false, true, resourceLocation);
                CraftMagicNumbers.ITEM_MATERIAL.put(item, material);
                CraftMagicNumbers.MATERIAL_ITEM.put(material, item);
                if (material != null) {
                    MohistMC.LOGGER.debug("Save-ITEM: " + material.name() + " - " + materialName);
                }
            }
        }
    }


    public static void addEnumMaterialsInBlocks(){
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation resourceLocation = block.getRegistryName();
            if(!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                // inject block materials into Bukkit for FML
                String materialName = normalizeName(resourceLocation.toString());
                int id = Item.getId(block.asItem());
                Item item = Item.byId(id);
                Material material = Material.addMaterial(materialName, id, item.getMaxStackSize(), true, false, resourceLocation);
                CraftMagicNumbers.BLOCK_MATERIAL.put(block, material);
                CraftMagicNumbers.MATERIAL_BLOCK.put(material, block);
                if (material != null) {
                    MohistMC.LOGGER.debug("Save-BLOCK:" + material.name() + " - " + materialName);
                }
            }
        }
    }


    public static void addEnumEnchantment() {
        // Enchantment
        for (Map.Entry<RegistryKey<Enchantment>, Enchantment> entry : ForgeRegistries.ENCHANTMENTS.getEntries()) {
            org.bukkit.enchantments.Enchantment.registerEnchantment(new CraftEnchantment(entry.getValue()));
        }
        org.bukkit.enchantments.Enchantment.stopAcceptingRegistrations();
    }

    public static void addEnumEffectAndPotion() {
        // Points
        for (Effect effect : ForgeRegistries.POTIONS) {
            PotionEffectType pet = new CraftPotionEffectType(effect);
            PotionEffectType.registerPotionEffectType(pet);
        }
        PotionEffectType.stopAcceptingRegistrations();

        for (Potion potion : ForgeRegistries.POTION_TYPES) {
            if (CraftPotionUtil.toBukkit(potion.getRegistryName().toString()).getType() == PotionType.UNCRAFTABLE && potion != Potions.EMPTY) {
                String name = normalizeName(potion.getRegistryName().toString());
                EffectInstance effectInstance = potion.getEffects().isEmpty() ? null : potion.getEffects().get(0);
                PotionType potionType = MohistEnumHelper.addEnum0(PotionType.class, name, new Class[]{PotionEffectType.class, Boolean.TYPE, Boolean.TYPE}, effectInstance == null ? null : PotionEffectType.getById(Effect.getId(effectInstance.getEffect())), false, false);
                if (potionType != null) {
                    MohistMC.LOGGER.debug("Save-PotionType:" + name + " - " + potionType.name());
                }
            }
        }
    }

    public static void addEnumParticle() {
        for (ParticleType particleType : ForgeRegistries.PARTICLE_TYPES) {
            ResourceLocation resourceLocation = particleType.getRegistryName();
            String name = normalizeName(particleType.getRegistryName().toString());
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                Particle particle = MohistEnumHelper.addEnum0(Particle.class, name, new Class[0]);
                if (particle != null) {
                    CraftParticle.putParticles(particle, resourceLocation);
                    MohistMC.LOGGER.debug("Save-ParticleType:" + name + " - " + particle.name());
                }
            }
        }
    }

    public static void addEnumBiome() {
        List<String> map = new ArrayList<>();
        for (net.minecraft.world.biome.Biome biome : ForgeRegistries.BIOMES) {
            ResourceLocation resourceLocation = biome.getRegistryName();
            String biomeName = normalizeName(resourceLocation.toString());
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT) && !map.contains(biomeName)) {
                map.add(biomeName);
                org.bukkit.block.Biome biomeCB = MohistEnumHelper.addEnum0(org.bukkit.block.Biome.class, biomeName, new Class[0]);
                biomeMap.put(biomeCB, biome);
                MohistMC.LOGGER.debug("Save-BIOME:" + biomeCB.name() + " - " + biomeName);
            }
        }
        map.clear();
    }

    public static void addEnumPattern(){
        Map<String, PatternType> PATTERN_MAP = ObfuscationReflectionHelper.getPrivateValue(PatternType.class, null, "byString");
        for (BannerPattern bannerpattern : BannerPattern.values()) {
            String p_i47246_3_ = bannerpattern.name();
            String hashname = bannerpattern.getHashname();
            if (PatternType.getByIdentifier(hashname) == null) {
                PatternType patternType = MohistEnumHelper.addEnum0(PatternType.class, p_i47246_3_, new Class[]{String.class}, hashname);
                PATTERN_MAP.put(hashname, patternType);
            }
        }
    }

    public static void addEnumEnvironment(Registry<Dimension> registry) {
        int i = World.Environment.values().length;
        for (Map.Entry<RegistryKey<Dimension>, Dimension> entry : registry.entrySet()) {
            RegistryKey<Dimension> key = entry.getKey();
            DimensionType dimensionType = entry.getValue().type();
            World.Environment environment1 = environment.get(dimensionType);
            if (environment1 == null) {
                String name = normalizeName(key.location().toString());
                int id = i - 1;
                environment1 = MohistEnumHelper.addEnum(World.Environment.class, name, new Class[]{Integer.TYPE}, new Object[]{id});
                environment.put(dimensionType, environment1);
                environment0.put(environment1, key);
                MohistMC.LOGGER.debug("Registered forge DimensionType as environment {}", environment1);
                i++;
            }
        }
    }

    public static WorldType addEnumWorldType(String name)
    {
        WorldType worldType = MohistEnumHelper.addEnum0(WorldType.class, name, new Class [] { String.class }, name);
        Map<String, WorldType> BY_NAME = ObfuscationReflectionHelper.getPrivateValue(WorldType.class, null, "BY_NAME");
        BY_NAME.put(name.toUpperCase(), worldType);
        return worldType;
    }

    public static void addEnumEntity() {
        for (net.minecraft.entity.EntityType<?> entity : ForgeRegistries.ENTITIES) {
            ResourceLocation resourceLocation = ForgeRegistries.ENTITIES.getKey(entity);
            NamespacedKey key = CraftNamespacedKey.fromMinecraft(resourceLocation);
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                String entityType = normalizeName(resourceLocation.toString());
                int typeId = entityType.hashCode();
                EntityType bukkitType = MohistEnumHelper.addEnum0(EntityType.class, entityType, new Class[]{String.class, Class.class, Integer.TYPE, Boolean.TYPE}, entityType.toLowerCase(), CraftCustomEntity.class, typeId, false);
                bukkitType.key = key;
                EntityType.NAME_MAP.put(entityType.toLowerCase(), bukkitType);
                EntityType.ID_MAP.put((short) typeId, bukkitType);
                ServerAPI.entityTypeMap.put(entity, entityType);
            }
        }
    }

    public static void registerDefaultPermission(String name, DefaultPermissionLevel level, String desc) {
        PermissionDefault permissionDefault;
        if (level == DefaultPermissionLevel.ALL) {
            permissionDefault = PermissionDefault.TRUE;
        } else if (level == DefaultPermissionLevel.OP) {
            permissionDefault = PermissionDefault.OP;
        } else {
            permissionDefault = PermissionDefault.FALSE;
        }
        DefaultPermissions.registerPermission(name, desc, permissionDefault);
    }

    public static void addEnumVillagerProfession() {
        for (VillagerProfession villagerProfession : ForgeRegistries.PROFESSIONS) {
            ResourceLocation resourceLocation = villagerProfession.getRegistryName();
            String name = normalizeName(resourceLocation.toString());
            if(!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                Villager.Profession vp = MohistEnumHelper.addEnum0(Villager.Profession.class, name, new Class[0]);
                profession.put(vp, resourceLocation);
                MohistMC.LOGGER.debug("Registered forge VillagerProfession as Profession {}", vp.name());
            }
        }
    }

    public static void addEnumAttribute() {
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES) {
            ResourceLocation resourceLocation = attribute.getRegistryName();
            String name = normalizeName(resourceLocation.getPath());
            if(!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                org.bukkit.attribute.Attribute ab = MohistEnumHelper.addEnum0(org.bukkit.attribute.Attribute.class, name, new Class[]{String.class}, resourceLocation.getPath());
                attributemap.put(ab, resourceLocation);
                MohistMC.LOGGER.debug("Registered forge Attribute as Attribute(Bukkit) {}", ab.name());
            }
        }
    }

    public static void addEnumArt() {
        int i = Art.values().length;
        HashMap<String, Art> BY_NAME = ObfuscationReflectionHelper.getPrivateValue(Art.class, null, "BY_NAME");
        HashMap<Integer, Art> BY_ID = ObfuscationReflectionHelper.getPrivateValue(Art.class, null, "BY_ID");
        for (PaintingType entry : ForgeRegistries.PAINTING_TYPES) {
            int width = entry.getWidth();
            int height = entry.getHeight();
            ResourceLocation resourceLocation = entry.getRegistryName();
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                String name = normalizeName(resourceLocation.toString());
                int id = i - 1;
                Art art = MohistEnumHelper.addEnum(Art.class, name, new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE}, new Object[]{id, width, height});
                artMap.put(entry, art);
                BY_NAME.put(name, art);
                BY_ID.put(id, art);
                MohistMC.LOGGER.debug("Registered forge PaintingType as Art {}", art);
                i++;
            }
        }
    }

    public static String normalizeName(String name) {
        return name.toUpperCase(java.util.Locale.ENGLISH).replaceAll("(:|\\s)", "_").replaceAll("\\W", "");
    }
}
