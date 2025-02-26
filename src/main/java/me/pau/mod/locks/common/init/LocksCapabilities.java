package me.pau.mod.locks.common.init;

import me.pau.mod.locks.common.capability.CapabilityProvider;
import me.pau.mod.locks.common.capability.ILockableHandler;
import me.pau.mod.locks.common.capability.ILockableStorage;
import me.pau.mod.locks.common.capability.ISelection;
import me.pau.mod.locks.common.capability.LockableHandler;
import me.pau.mod.locks.common.capability.LockableStorage;
import me.pau.mod.locks.common.capability.Selection;
import me.pau.mod.locks.common.capability.SerializableCapabilityProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public final class LocksCapabilities {
	public static final Capability<ILockableHandler> LOCKABLE_HANDLER = CapabilityManager.get(new CapabilityToken<>(){});
	public static final Capability<ILockableStorage> LOCKABLE_STORAGE = CapabilityManager.get(new CapabilityToken<>(){});

	public static final Capability<ISelection> SELECTION = CapabilityManager.get(new CapabilityToken<>(){});

	private LocksCapabilities() {}

	/*
	public static void register() {
		CapabilityManager.INSTANCE.register(ILockableHandler.class, new CapabilityStorage(), () -> null);
		CapabilityManager.INSTANCE.register(ILockableStorage.class, new CapabilityStorage(), () -> null);
		CapabilityManager.INSTANCE.register(ISelection.class, new EmptyCapabilityStorage(), Selection::new);
	}
	 */

	public static void attachToWorld(AttachCapabilitiesEvent<Level> e) {
		e.addCapability(LockableHandler.ID, new SerializableCapabilityProvider<>(LOCKABLE_HANDLER, new LockableHandler(e.getObject())));
	}

	public static void attachToChunk(AttachCapabilitiesEvent<LevelChunk> e) {
		e.addCapability(LockableStorage.ID, new SerializableCapabilityProvider<>(LOCKABLE_STORAGE, new LockableStorage(e.getObject())));
	}

	public static void attachToEntity(AttachCapabilitiesEvent<Entity> e) {
		if(e.getObject() instanceof Player)
			e.addCapability(Selection.ID, new CapabilityProvider<>(SELECTION, new Selection()));
	}
}