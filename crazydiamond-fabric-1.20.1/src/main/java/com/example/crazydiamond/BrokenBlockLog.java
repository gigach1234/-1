package com.example.crazydiamond;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Remembers blocks smashed by the stand (and the items they dropped) so "Return Block"
 * can put them back. When a block is restored, the items it dropped are taken back
 * (from the ground nearby first, then from the owner's inventory), so nothing is duplicated.
 * The log lives in memory only (it is cleared when the server stops).
 */
public final class BrokenBlockLog {
	private static final int MAX_ENTRIES = 8192;
	private static final Map<Identifier, LinkedHashMap<BlockPos, Entry>> LOG = new HashMap<>();

	private record Entry(BlockState state, List<ItemStack> drops) {
	}

	private BrokenBlockLog() {
	}

	public static void record(ServerWorld world, BlockPos pos, BlockState state, List<ItemStack> drops) {
		LinkedHashMap<BlockPos, Entry> map =
				LOG.computeIfAbsent(world.getRegistryKey().getValue(), k -> new LinkedHashMap<>());
		map.put(pos.toImmutable(), new Entry(state, drops));
		if (map.size() > MAX_ENTRIES) {
			Iterator<BlockPos> it = map.keySet().iterator();
			it.next();
			it.remove();
		}
	}

	/** Restores up to {@code max} logged blocks within {@code radius} of the owner, nearest first. */
	public static int restoreSome(ServerWorld world, ServerPlayerEntity owner, int radius, int max) {
		Map<BlockPos, Entry> map = LOG.get(world.getRegistryKey().getValue());
		if (map == null || map.isEmpty()) {
			return 0;
		}

		BlockPos center = owner.getBlockPos();
		double radiusSq = (double) radius * radius;
		List<BlockPos> near = new ArrayList<>();
		for (BlockPos pos : map.keySet()) {
			if (pos.getSquaredDistance(center) <= radiusSq) {
				near.add(pos);
			}
		}
		near.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(center)));

		int restored = 0;
		boolean soundPlayed = false;
		for (BlockPos pos : near) {
			if (restored >= max) {
				break;
			}
			if (!world.isChunkLoaded(pos)) {
				continue;
			}
			Entry entry = map.get(pos);
			BlockState current = world.getBlockState(pos);

			if (current.isAir() || current.isReplaceable()) {
				world.setBlockState(pos, entry.state());
				for (ItemStack drop : entry.drops()) {
					takeBack(world, owner, pos, drop);
				}
				StandActions.gold(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.35);
				if (!soundPlayed) {
					world.playSound(null, pos, entry.state().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 0.8f, 1.0f);
					soundPlayed = true;
				}
				restored++;
			}
			// Either restored, or the spot was taken by something else: forget the entry.
			map.remove(pos);
		}
		return restored;
	}

	/** Removes the given dropped item again: from the ground nearby first, then from the owner's inventory. */
	private static void takeBack(ServerWorld world, ServerPlayerEntity owner, BlockPos pos, ItemStack wanted) {
		int remaining = wanted.getCount();

		Box box = new Box(pos).expand(6.0);
		List<ItemEntity> onGround = world.getEntitiesByClass(ItemEntity.class, box,
				e -> e.isAlive() && ItemStack.canCombine(e.getStack(), wanted));
		for (ItemEntity itemEntity : onGround) {
			if (remaining <= 0) {
				return;
			}
			ItemStack copy = itemEntity.getStack().copy();
			int take = Math.min(remaining, copy.getCount());
			copy.decrement(take);
			remaining -= take;
			if (copy.isEmpty()) {
				itemEntity.discard();
			} else {
				itemEntity.setStack(copy);
			}
		}

		PlayerInventory inventory = owner.getInventory();
		for (int i = 0; i < inventory.size() && remaining > 0; i++) {
			ItemStack slot = inventory.getStack(i);
			if (!slot.isEmpty() && ItemStack.canCombine(slot, wanted)) {
				int take = Math.min(remaining, slot.getCount());
				slot.decrement(take);
				remaining -= take;
			}
		}
	}

	public static void clear() {
		LOG.clear();
	}
}
