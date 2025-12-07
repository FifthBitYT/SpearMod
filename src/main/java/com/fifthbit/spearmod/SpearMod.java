package com.fifthbit.spearmod;

import com.fifthbit.spearmod.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpearMod implements ModInitializer {
	public static final String MOD_ID = "spearmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Spear Mod!");
		ModItems.registerItems();
		LOGGER.info("Spear Mod initialized successfully!");
	}
}