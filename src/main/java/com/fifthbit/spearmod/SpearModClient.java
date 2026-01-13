package com.fifthbit.spearmod;

import net.fabricmc.api.ClientModInitializer;

public class SpearModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpearMod.LOGGER.info("Spear Mod Client Initialized!");
        // Model predicates work automatically through the model JSON
    }
}