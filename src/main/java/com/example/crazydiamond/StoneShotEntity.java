package com.example.crazydiamond;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** The stone fired by the stand. Flies straight, leaves a golden trail. */
public class StoneShotEntity extends ThrownItemEntity {
	private static final float DAMAGE = 8.0f;
	private static final int MAX_LIFETIME = 40;

	private int life = 0;

	public StoneShotEntity(EntityType<? extends StoneShotEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COBBLESTONE;
	}

	@Override
	public boolean hasNoGravity() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			StandActions.gold(serverWorld, this.getX(), this.getY(), this.getZ(), 2, 0.05);
			if (++life > MAX_LIFETIME) {
				this.discard();
			}
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		if (this.getWorld().isClient) {
			return;
		}
		Entity target = hit.getEntity();
		Entity owner = this.getOwner();
		if (target == owner) {
			return;
		}
		target.damage(this.getWorld().getDamageSources().thrown(this, owner), DAMAGE);
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		super.onBlockHit(hit);
		if (this.getWorld() instanceof ServerWorld serverWorld && this.getOwner() instanceof ServerPlayerEntity player) {
			StandActions.smashAt(serverWorld, player, hit.getBlockPos(), 3.0f);
		}
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			StandActions.gold(serverWorld, this.getX(), this.getY(), this.getZ(), 14, 0.25);
			this.discard();
		}
	}
}
