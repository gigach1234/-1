package com.example.crazydiamond;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Vector3f;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

/** Server-side implementation of every ability. */
public final class StandActions {
	/** Set to false if you don't want the stand to smash blocks. */
	public static boolean BREAK_BLOCKS = true;

	private static final float MAX_BREAK_HARDNESS = 50.0f; // stone 1.5, obsidian 50, bedrock is unbreakable
	private static final double REACH = 4.5;

	private static final float PUNCH_DAMAGE = 9.0f;
	private static final float BARRAGE_DAMAGE = 1.6f;
	private static final float BARRAGE_FINISHER_DAMAGE = 7.0f;

	private static final int RETURN_RADIUS = 24;
	private static final int RETURN_BLOCKS_PER_TICK = 6;

	private static final float HEAL_AMOUNT = 1.5f;      // every 5 ticks
	private static final int REPAIR_PER_PULSE = 10;     // durability every 2 ticks

	private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1.0f, 0.78f, 0.12f), 1.0f);

	private StandActions() {
	}

	// ------------------------------------------------------------- helpers

	public static void gold(ServerWorld world, double x, double y, double z, int count, double spread) {
		world.spawnParticles(GOLD, x, y, z, count, spread, spread, spread, 0.02);
	}

	/** Living entities in front of the owner, closest first. */
	static List<LivingEntity> frontTargets(ServerWorld world, ServerPlayerEntity owner, double range) {
		Vec3d eye = owner.getEyePos();
		Vec3d look = owner.getRotationVec(1.0f);
		Box box = owner.getBoundingBox().stretch(look.multiply(range)).expand(1.5);

		List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box,
				e -> e.isAlive() && e != owner && !e.isSpectator());

		List<LivingEntity> result = new ArrayList<>();
		for (LivingEntity e : candidates) {
			Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
			double dist = to.length();
			if (dist > 0.001 && dist <= range + e.getWidth() * 0.5 && to.multiply(1.0 / dist).dotProduct(look) > 0.75) {
				result.add(e);
			}
		}
		result.sort(Comparator.comparingDouble((LivingEntity e) -> e.squaredDistanceTo(owner)));
		return result;
	}

	private static boolean hurt(ServerWorld world, ServerPlayerEntity owner, LivingEntity target, float damage) {
		target.timeUntilRegen = 0; // allow rapid hits
		return target.damage(world.getDamageSources().playerAttack(owner), damage);
	}

	private static void knockForward(LivingEntity target, ServerPlayerEntity owner, double strength) {
		float yawRad = owner.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		target.takeKnockback(strength, MathHelper.sin(yawRad), -MathHelper.cos(yawRad));
		target.velocityModified = true;
	}

	// --------------------------------------------------------------- blocks

	static void smashBlock(ServerWorld world, ServerPlayerEntity owner) {
		HitResult hit = owner.raycast(REACH, 1.0f, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		smashAt(world, owner, ((BlockHitResult) hit).getBlockPos(), MAX_BREAK_HARDNESS);
	}

	/** Breaks a block (with normal drops) and logs it so Return Block can restore it. */
	static boolean smashAt(ServerWorld world, ServerPlayerEntity owner, BlockPos pos, float maxHardness) {
		if (!BREAK_BLOCKS) {
			return false;
		}
		BlockState state = world.getBlockState(pos);
		if (state.isAir() || world.getBlockEntity(pos) != null) {
			return false;
		}
		float hardness = state.getHardness(world, pos);
		if (hardness < 0 || hardness > maxHardness) {
			return false;
		}
		if (!state.isFullCube(world, pos)) { // skips doors, plants, beds, etc.
			return false;
		}
		if (!world.canPlayerModifyAt(owner, pos)) {
			return false;
		}

		boolean dropItems = !owner.isCreative() && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS);
		List<ItemStack> drops = new ArrayList<>();
		if (dropItems) {
			for (ItemStack stack : Block.getDroppedStacks(state, world, pos, null, owner, ItemStack.EMPTY)) {
				if (!stack.isEmpty()) {
					drops.add(stack.copy());
				}
			}
		}

		world.breakBlock(pos, false, owner);
		for (ItemStack stack : drops) {
			Block.dropStack(world, pos, stack.copy());
		}
		BrokenBlockLog.record(world, pos, state, drops);
		gold(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.4);
		return true;
	}

	// ------------------------------------------------------------ abilities

	static void punch(ServerWorld world, ServerPlayerEntity owner) {
		List<LivingEntity> targets = frontTargets(world, owner, REACH);
		if (targets.isEmpty()) {
			smashBlock(world, owner);
			world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 0.8f);
			return;
		}
		LivingEntity target = targets.get(0);
		if (hurt(world, owner, target, PUNCH_DAMAGE)) {
			knockForward(target, owner, 1.0);
		}
		world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 0.9f);
		gold(world, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.3);
	}

	static void barrage(ServerWorld world, ServerPlayerEntity owner, int elapsed, int remaining) {
		if (elapsed % 2 != 0) {
			return;
		}
		boolean finisher = remaining <= 0;
		List<LivingEntity> targets = frontTargets(world, owner, REACH);

		if (targets.isEmpty()) {
			if (elapsed % 6 == 0) {
				smashBlock(world, owner);
			}
			return;
		}

		Vec3d look = owner.getRotationVec(1.0f);
		int count = Math.min(3, targets.size());
		for (int i = 0; i < count; i++) {
			LivingEntity target = targets.get(i);
			hurt(world, owner, target, finisher ? BARRAGE_FINISHER_DAMAGE : BARRAGE_DAMAGE);
			if (finisher) {
				knockForward(target, owner, 2.0);
				target.addVelocity(0.0, 0.4, 0.0);
			} else {
				target.addVelocity(look.x * 0.03, 0.01, look.z * 0.03);
			}
			target.velocityModified = true;
			gold(world, target.getX(), target.getBodyY(0.5), target.getZ(), 3, 0.3);
		}
		float pitch = 0.8f + world.random.nextFloat() * 0.4f;
		world.playSound(null, owner.getBlockPos(),
				finisher ? SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK : SoundEvents.ENTITY_PLAYER_ATTACK_WEAK,
				SoundCategory.PLAYERS, 1.0f, pitch);
	}

	static void returnBlock(ServerWorld world, ServerPlayerEntity owner, StandEntity stand, int elapsed) {
		int restored = BrokenBlockLog.restoreSome(world, owner, RETURN_RADIUS, RETURN_BLOCKS_PER_TICK);
		if (restored == 0 && elapsed <= 1) {
			owner.sendMessage(Text.translatable("message.crazydiamond.nothing_to_return"), true);
			stand.stop();
		} else if (restored == 0) {
			stand.stop(); // everything nearby has been restored
		}
	}

	static void heal(ServerWorld world, ServerPlayerEntity owner, StandEntity stand, int elapsed) {
		if (elapsed == 1) {
			List<LivingEntity> targets = frontTargets(world, owner, 6.0);
			LivingEntity chosen = targets.isEmpty() ? owner : targets.get(0);
			stand.setHealTarget(chosen.getUuid());
		}
		if (stand.getHealTarget() == null) {
			return;
		}
		Entity entity = world.getEntity(stand.getHealTarget());
		if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity.squaredDistanceTo(owner) > 144.0) {
			stand.stop();
			return;
		}
		LivingEntity target = (LivingEntity) entity;

		if (elapsed % 5 == 0) {
			target.heal(HEAL_AMOUNT);
			world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 1.4f);
		}
		if (elapsed % 2 == 0) {
			gold(world, target.getX(), target.getBodyY(0.5), target.getZ(), 5, 0.4);
		}
	}

	static void stoneShot(ServerWorld world, ServerPlayerEntity owner, StandEntity stand) {
		StoneShotEntity shot = new StoneShotEntity(ModEntities.STONE_SHOT, world);
		shot.setOwner(owner);
		Vec3d look = owner.getRotationVec(1.0f);
		shot.setPosition(stand.getX(), stand.getY() + 1.3, stand.getZ());
		shot.setVelocity(look.x, look.y, look.z, 2.6f, 0.0f);
		world.spawnEntity(shot);
		world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.7f);
	}

	/** Breaks the held item back into the ingredients of its crafting recipe. */
	static void disassemble(ServerWorld world, ServerPlayerEntity owner) {
		ItemStack held = owner.getMainHandStack();
		if (held.isEmpty()) {
			owner.sendMessage(Text.translatable("message.crazydiamond.no_item"), true);
			return;
		}

		DynamicRegistryManager registries = world.getRegistryManager();
		CraftingRecipe found = null;
		int outputCount = 1;
		for (Recipe<?> recipe : world.getRecipeManager().values()) {
			if (!(recipe instanceof CraftingRecipe) || recipe instanceof SpecialCraftingRecipe) {
				continue;
			}
			ItemStack output = recipe.getOutput(registries);
			if (output.isOf(held.getItem()) && output.getCount() <= held.getCount() && !recipe.getIngredients().isEmpty()) {
				found = (CraftingRecipe) recipe;
				outputCount = output.getCount();
				break;
			}
		}
		if (found == null) {
			owner.sendMessage(Text.translatable("message.crazydiamond.no_recipe"), true);
			return;
		}

		held.decrement(outputCount);
		for (Ingredient ingredient : found.getIngredients()) {
			if (ingredient.isEmpty()) {
				continue;
			}
			ItemStack[] options = ingredient.getMatchingStacks();
			if (options.length == 0) {
				continue;
			}
			ItemStack give = options[0].copy();
			give.setCount(1);
			ItemEntity drop = new ItemEntity(world, owner.getX(), owner.getY() + 1.0, owner.getZ(), give);
			drop.setVelocity(
					(world.random.nextDouble() - 0.5) * 0.2,
					0.2 + world.random.nextDouble() * 0.1,
					(world.random.nextDouble() - 0.5) * 0.2);
			drop.setPickupDelay(15);
			world.spawnEntity(drop);
		}
		gold(world, owner.getX(), owner.getY() + 1.0, owner.getZ(), 20, 0.5);
		world.playSound(null, owner.getBlockPos(), SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
	}

	static void repair(ServerWorld world, ServerPlayerEntity owner, StandEntity stand, int elapsed) {
		ItemStack held = owner.getMainHandStack();
		if (held.isEmpty() || !held.isDamageable() || !held.isDamaged()) {
			if (elapsed == 1) {
				owner.sendMessage(Text.translatable("message.crazydiamond.nothing_to_repair"), true);
			}
			stand.stop();
			return;
		}

		if (elapsed % 2 == 0) {
			held.setDamage(Math.max(0, held.getDamage() - REPAIR_PER_PULSE));
			gold(world, owner.getX(), owner.getY() + 1.1, owner.getZ(), 4, 0.4);
		}
		if (elapsed % 5 == 0) {
			int max = held.getMaxDamage();
			owner.sendMessage(Text.translatable("message.crazydiamond.durability", max - held.getDamage(), max), true);
		}
		if (!held.isDamaged()) {
			world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
			stand.stop();
		}
	}
}
