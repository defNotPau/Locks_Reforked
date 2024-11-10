package me.pau.mod.locks.common.capability;

import java.util.Observer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.pau.mod.locks.common.util.Lockable;
import net.minecraft.nbt.IntTag;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;

public interface ILockableHandler extends INBTSerializable<IntTag>, Observer {
	int nextId();

	Int2ObjectMap<Lockable> getLoaded();

	Int2ObjectMap<Lockable> getInChunk(BlockPos pos);

	boolean add(Lockable lkb);

	boolean remove(int id);
}