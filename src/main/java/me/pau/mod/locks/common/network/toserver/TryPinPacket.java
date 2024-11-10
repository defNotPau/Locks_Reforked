package me.pau.mod.locks.common.network.toserver;

import java.util.Objects;
import java.util.function.Supplier;

import me.pau.mod.locks.common.container.LockPickingContainer;
import me.pau.mod.locks.common.init.LocksContainerTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TryPinPacket
{
	private final byte pin;

	public TryPinPacket(byte pin)
	{
		this.pin = pin;
	}

	public static TryPinPacket decode(FriendlyByteBuf buf) {
		return new TryPinPacket(buf.readByte());
	}

	public static void encode(TryPinPacket pkt, FriendlyByteBuf buf)
	{
		buf.writeByte(pkt.pin);
	}

	public static void handle(TryPinPacket pkt, Supplier<NetworkEvent.Context> ctx) {
		// Use runnable, lambda causes issues with class loading
		ctx.get().enqueueWork(new Runnable() {
			@Override
			public void run() {
				AbstractContainerMenu container = Objects.requireNonNull(ctx.get().getSender()).containerMenu;
				if(container.getType() == LocksContainerTypes.LOCK_PICKING.get())
					((LockPickingContainer) container).tryPin(pkt.pin);
			}
		});
		ctx.get().setPacketHandled(true);
	}
}