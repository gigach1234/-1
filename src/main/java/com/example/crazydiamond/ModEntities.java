package com.example.crazydiamond;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
	public static final EntityType<StandEntity> STAND = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(CrazyDiamondMod.MOD_ID, "crazy_diamond"),
			EntityType.Builder.<StandEntity>create(StandEntity::new, SpawnGroup.MISC)
					.setDimensions(0.6f, 1.9f)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.build("crazy_diamond"));

	public static final EntityType<StoneShotEntity> STONE_SHOT = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(CrazyDiamondMod.MOD_ID, "stone_shot"),
			EntityType.Builder.<StoneShotEntity>create(StoneShotEntity::new, SpawnGroup.MISC)
					.setDimensions(0.25f, 0.25f)
					.maxTrackingRange(4)
					.trackingTickInterval(10)
					.build("stone_shot"));

	private ModEntities() {
	}

	/** Called from the mod initializer just to force this class to load. */
	public static void register() {
	}
}
