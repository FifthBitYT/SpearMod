package com.fifthbit.spearmod.item;

import com.fifthbit.spearmod.SpearMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    // Create the registry key first
    public static final RegistryKey<Item> DIAMOND_SPEAR_KEY = RegistryKey.of(RegistryKeys.ITEM,
            Identifier.of(SpearMod.MOD_ID, "diamond_spear"));

    public static final RegistryKey<Item> DIAMOND_SPEAR_HEAD_KEY = RegistryKey.of(RegistryKeys.ITEM,
            Identifier.of(SpearMod.MOD_ID, "diamond_spear_head"));

    // Then register the items with the key
    public static final Item DIAMOND_SPEAR = Registry.register(
            Registries.ITEM,
            DIAMOND_SPEAR_KEY,
            new DiamondSpearItem(new Item.Settings().registryKey(DIAMOND_SPEAR_KEY).maxDamage(1561))
    );

    public static final Item DIAMOND_SPEAR_HEAD = Registry.register(
            Registries.ITEM,
            DIAMOND_SPEAR_HEAD_KEY,
            new Item(new Item.Settings().registryKey(DIAMOND_SPEAR_HEAD_KEY))
    );

    public static void registerItems() {
        SpearMod.LOGGER.info("Registering items for " + SpearMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(DIAMOND_SPEAR);
            entries.add(DIAMOND_SPEAR_HEAD);
        });

        SpearMod.LOGGER.info("Items registered successfully!");
    }
}