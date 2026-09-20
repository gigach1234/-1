package com.example.crazydiamond;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * The stand itself. It follows its owner around and runs one ability at a time.
 * All gameplay logic runs on the server; the client only receives the position,
 * the owner, the current ability id and how long it has been running.
 */
public class StandEntity extends Entity {
	private static final TrackedData<Integer> ANIM =
			DataTracker.registerData(StandEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> ANIM_TIME =
			DataTracker.registerData(StandEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Optional<UUID>> OWNER =
			DataTracker.registerData(StandEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

	// server-side ability state
	private Ability active;
	private int abilityTicks;
	private int cooldown;
	private UUID healTarget;

	public StandEntity(EntityType<? extends StandEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(ANIM, 0);
		this.dataTracker.startTracking(ANIM_TIME, 0);
		this.dataTracker.startTracking(OWNER, Optional.empty());
	}

	// ---------------------------------------------------------------- owner

	public void setOwner(PlayerEntity owner) {
		this.dataTracker.set(OWNER, Optional.of(owner.getUuid()));
	}

	public UUID getOwnerUuid() {
		return this.dataTracker.get(OWNER).orElse(null);
	}

	// ------------------------------------------------------------ animation

	/** 0 = idle, otherwise ability ordinal + 1. Synced to clients. */
	public int getAnim() {
		return this.dataTracker.get(ANIM);
	}

	/** Ticks since the current ability started. Synced to clients. */
	public int getAnimTime() {
		return this.dataTracker.get(ANIM_TIME);
	}

	public Ability getClientAbility() {
		int a = getAnim();
		if (a <= 0 || a > Ability.values().length) {
			return null;
		}
		return Ability.values()[a - 1];
	}

	// -------------------------------------------------------------- abilities

	public boolean tryStart(Ability ability) {
		if (active != null || cooldown > 0) {
			return false;
		}
		active = ability;
		abilityTicks = ability.duration;
		healTarget = null;
		this.dataTracker.set(ANIM_TIME, 0);
		this.dataTracker.set(ANIM, ability.ordinal() + 1);
		return true;
	}

	/** Ends the current ability at the end of this tick. */
	public void stop() {
		abilityTicks = 0;
	}

	public UUID getHealTarget() {
		return healTarget;
	}

	public void setHealTarget(UUID uuid) {
		this.healTarget = uuid;
	}

	private void endAbility() {
		if (active != null) {
			cooldown = active.cooldown;
		}
		active = null;
		healTarget = null;
		this.dataTracker.set(ANIM, 0);
		this.dataTracker.set(ANIM_TIME, 0);
	}

	// ------------------------------------------------------------------ tick

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld().isClient) {
			return;
		}

		UUID ownerId = getOwnerUuid();
		ServerWorld world = (ServerWorld) this.getWorld();
		PlayerEntity found = ownerId == null ? null : world.getPlayerByUuid(ownerId);
		if (!(found instanceof ServerPlayerEntity) || !found.isAlive() || found.isSpectator()) {
			this.discard();
			return;
		}
		ServerPlayerEntity owner = (ServerPlayerEntity) found;

		follow(owner);

		if (cooldown > 0) {
			cooldown--;
		}
		if (active != null) {
			abilityTicks--;
			int elapsed = active.duration - abilityTicks; // 1 .. duration
			this.dataTracker.set(ANIM_TIME, elapsed);
			runAbility(world, owner, elapsed, abilityTicks);
			if (abilityTicks <= 0) {
				endAbility();
			}
		}
	}

	private void runAbility(ServerWorld world, ServerPlayerEntity owner, int elapsed, int remaining) {
		switch (active) {
			case PUNCH -> {
				if (elapsed == 3) {
					StandActions.punch(world, owner);
				}
			}
			case BARRAGE -> StandActions.barrage(world, owner, elapsed, remaining);
			case RETURN_BLOCK -> StandActions.returnBlock(world, owner, this, elapsed);
			case HEAL_MODE -> StandActions.heal(world, owner, this, elapsed);
			case STONE_SHOT -> {
				if (elapsed == 6) {
					StandActions.stoneShot(world, owner, this);
				}
			}
			case DISASSEMBLE -> {
				if (elapsed == 10) {
					StandActions.disassemble(world, owner);
				}
			}
			case REPAIR_ITEM -> StandActions.repair(world, owner, this, elapsed);
		}
	}

	/** Hovers behind the owner, or steps in front while an ability is running. */
	private void follow(ServerPlayerEntity owner) {
		float yawRad = owner.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		double forwardX = -MathHelper.sin(yawRad);
		double forwardZ = MathHelper.cos(yawRad);
		double rightX = -MathHelper.cos(yawRad);
		double rightZ = -MathHelper.sin(yawRad);

		boolean fighting = active != null;
		double forward = fighting ? 1.3 : -0.9;
		double right = fighting ? 0.35 : 0.85;
		double bob = Math.sin(this.age * 0.12) * 0.05;

		double x = owner.getX() + forwardX * forward + rightX * right;
		double y = owner.getY() + bob;
		double z = owner.getZ() + forwardZ * forward + rightZ * right;

		this.setPosition(x, y, z);
		this.setYaw(owner.getYaw());
		this.setPitch(0.0f);
	}

	// ------------------------------------------------------------ misc rules

	@Override
	public boolean isInvulnerableTo(DamageSource damageSource) {
		return true;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		if (nbt.containsUuid("Owner")) {
			this.dataTracker.set(OWNER, Optional.of(nbt.getUuid("Owner")));
		}
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		UUID id = getOwnerUuid();
		if (id != null) {
			nbt.putUuid("Owner", id);
		}
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return new EntitySpawnS2CPacket(this);
	}
}
