package me.pau.mod.locks.mixin;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pau.mod.locks.Locks;
import me.pau.mod.locks.common.capability.ILockableHandler;
import me.pau.mod.locks.common.config.LocksConfig;
import me.pau.mod.locks.common.init.LocksCapabilities;
import me.pau.mod.locks.common.util.Cuboid6i;
import me.pau.mod.locks.common.util.Lock;
import me.pau.mod.locks.common.util.Lockable;
import me.pau.mod.locks.common.util.LockableInfo;
import me.pau.mod.locks.common.util.LocksUtil;
import me.pau.mod.locks.common.util.Transform;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.nbt.Tag;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {
	private final List<LockableInfo> lockableInfos = new ArrayList<>();

	@Inject(at = @At("HEAD"), method = "fillFromWorld")
	private void fillFromWorld(Level world, BlockPos start, Vec3i size, boolean takeEntities, @Nullable Block toIgnore, CallbackInfo ci) {
		if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1) {
			this.lockableInfos.clear();
			ILockableHandler handler = world.getCapability(LocksCapabilities.LOCKABLE_HANDLER).orElse(null);
			Cuboid6i bb = new Cuboid6i(start, start.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
			handler.getLoaded().values().stream()
				.filter(lkb -> lkb.bb.intersects(bb))
				.forEach(lkb -> {
					Cuboid6i newBB = bb.intersection(lkb.bb).offset(-start.getX(), -start.getY(), -start.getZ());
					this.lockableInfos.add(new LockableInfo(newBB, lkb.lock, lkb.tr, lkb.stack, lkb.id));
				});
		}
	}

	// Second return
	@Inject(at = @At(value = "RETURN", ordinal = 1), method = "placeInWorld")
	private void placeInWorld(ServerLevelAccessor world, BlockPos start, BlockPos size, StructurePlaceSettings settings, RandomSource rng, int i, CallbackInfoReturnable<Boolean> cir) {
		Level level;
		try {
			level = world.getLevel();
		}
		catch(Exception e) {
			Locks.LOGGER.warn(world + "#getLevel threw an error! Skipping lockable placement for this template ");
			return;
		}
		ILockableHandler handler = level.getCapability(LocksCapabilities.LOCKABLE_HANDLER).orElse(null);
		for(LockableInfo lkb : this.lockableInfos) {
			BlockPos pos1 = LocksUtil.transform(lkb.bb.x1, lkb.bb.y1, lkb.bb.z1, settings);
			BlockPos pos2 = LocksUtil.transform(lkb.bb.x2, lkb.bb.y2, lkb.bb.z2, settings);
			Cuboid6i bb = new Cuboid6i(pos1.getX() + start.getX(), pos1.getY() + start.getY(), pos1.getZ() + start.getZ(), pos2.getX() + start.getX(), pos2.getY() + start.getY(), pos2.getZ() + start.getZ());
			ItemStack stack = LocksConfig.RANDOMIZE_LOADED_LOCKS.get() ? LocksConfig.getRandomLock(rng) : lkb.stack;
			Lock lock = LocksConfig.RANDOMIZE_LOADED_LOCKS.get() ? Lock.from(stack) : lkb.lock;
			Transform tr = Transform.fromDirectionAndFace(settings.getRotation().rotate(settings.getMirror().getRotation(lkb.tr.dir).rotate(lkb.tr.dir)), lkb.tr.face, Direction.NORTH);
			handler.add(new Lockable(bb, lock, tr, stack, level));
		}
	}

	private static final String KEY_LOCKABLES = "Lockables";

	@Inject(at = @At("HEAD"), method = "save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;")
	private void save(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
		ListTag list = new ListTag();
		for(LockableInfo lkb : this.lockableInfos)
			list.add(LockableInfo.toNbt(lkb));
		nbt.put(KEY_LOCKABLES, list);
	}

	@Inject(at = @At("HEAD"), method = "load(Lnet/minecraft/nbt/CompoundTag;)V")
	private void read(CompoundTag nbt, CallbackInfo ci) {
		this.lockableInfos.clear();
		ListTag list = nbt.getList(KEY_LOCKABLES, Tag.TAG_COMPOUND);
		for(int a = 0, b = list.size(); a < b; ++a)
			this.lockableInfos.add(LockableInfo.fromNbt(list.getCompound(a)));
	}
}