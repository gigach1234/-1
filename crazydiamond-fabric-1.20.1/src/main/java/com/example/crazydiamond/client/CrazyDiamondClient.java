package com.example.crazydiamond.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.example.crazydiamond.Ability;
import com.example.crazydiamond.ModEntities;
import com.example.crazydiamond.StandEntity;
import com.example.crazydiamond.StandNetworking;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;

public class CrazyDiamondClient implements ClientModInitializer {
	private static final String CATEGORY = "category.crazydiamond";

	private record AbilityKey(KeyBinding key, Ability ability) {
	}

	private static KeyBinding summonKey;
	private static final List<AbilityKey> ABILITY_KEYS = new ArrayList<>();

	@Override
	public void onInitializeClient() {
		summonKey = register("key.crazydiamond.summon", GLFW.GLFW_KEY_V);

		// Punch is on left click; every other ability has its own key.
		bind(Ability.BARRAGE, GLFW.GLFW_KEY_R);
		bind(Ability.RETURN_BLOCK, GLFW.GLFW_KEY_Z);
		bind(Ability.HEAL_MODE, GLFW.GLFW_KEY_X);
		bind(Ability.STONE_SHOT, GLFW.GLFW_KEY_C);
		bind(Ability.DISASSEMBLE, GLFW.GLFW_KEY_G);
		bind(Ability.REPAIR_ITEM, GLFW.GLFW_KEY_B);

		EntityModelLayerRegistry.registerModelLayer(StandModel.LAYER, StandModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.STAND, StandRenderer::new);
		EntityRendererRegistry.register(ModEntities.STONE_SHOT, FlyingItemEntityRenderer::new);

		// Left click (click or hold) = stand punch while the stand is summoned.
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (client.currentScreen == null && hasStand(client)) {
				send(StandNetworking.actionFor(Ability.PUNCH));
				return true; // cancel the normal attack / block breaking
			}
			return false;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}
			while (summonKey.wasPressed()) {
				send(StandNetworking.SUMMON);
			}
			for (AbilityKey entry : ABILITY_KEYS) {
				while (entry.key().wasPressed()) {
					send(StandNetworking.actionFor(entry.ability()));
				}
			}
		});
	}

	private static KeyBinding register(String translationKey, int glfwKey) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, CATEGORY));
	}

	private static void bind(Ability ability, int glfwKey) {
		ABILITY_KEYS.add(new AbilityKey(register(ability.keyTranslation(), glfwKey), ability));
	}

	private static boolean hasStand(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return false;
		}
		UUID id = player.getUuid();
		return !client.world.getEntitiesByClass(StandEntity.class,
				player.getBoundingBox().expand(16.0), stand -> id.equals(stand.getOwnerUuid())).isEmpty();
	}

	private static void send(int action) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(action);
		ClientPlayNetworking.send(StandNetworking.ACTION, buf);
	}
}
