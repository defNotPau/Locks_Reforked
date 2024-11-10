package me.pau.mod.locks.client.event;

import me.pau.mod.locks.Locks;
import me.pau.mod.locks.client.init.LocksItemModelsProperties;
import me.pau.mod.locks.client.init.LocksScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Locks.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LocksClientModEvents
{
	private LocksClientModEvents() {}

	@SubscribeEvent
	public static void onSetup(FMLClientSetupEvent e)
	{
		LocksScreens.register();
		LocksItemModelsProperties.register();
	}
}