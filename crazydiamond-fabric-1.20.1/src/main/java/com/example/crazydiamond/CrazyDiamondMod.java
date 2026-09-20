package com.example.crazydiamond;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrazyDiamondMod implements ModInitializer {
	public static final String MOD_ID = "crazydiamond";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		StandNetworking.register();
		LOGGER.info("Crazy Diamond loaded");
	}
}
