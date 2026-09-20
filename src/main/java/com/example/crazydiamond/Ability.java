package com.example.crazydiamond;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum Ability {
	PUNCH("punch", 6, 2),
	BARRAGE("barrage", 60, 20),
	RETURN_BLOCK("return_block", 40, 20),
	HEAL_MODE("heal_mode", 100, 20),
	STONE_SHOT("stone_shot", 12, 10),
	DISASSEMBLE("disassemble", 20, 20),
	REPAIR_ITEM("repair_item", 100, 20);

	public final String id;
	/** How long the ability runs, in ticks. */
	public final int duration;
	/** Cooldown after the ability ends, in ticks. */
	public final int cooldown;

	Ability(String id, int duration, int cooldown) {
		this.id = id;
		this.duration = duration;
		this.cooldown = cooldown;
	}

	public MutableText displayName() {
		return Text.translatable("ability." + CrazyDiamondMod.MOD_ID + "." + id).formatted(Formatting.LIGHT_PURPLE);
	}

	public String keyTranslation() {
		return "key." + CrazyDiamondMod.MOD_ID + "." + id;
	}
}
