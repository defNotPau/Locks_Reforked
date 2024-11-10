package me.pau.mod.locks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pau.mod.locks.common.util.LocksUtil;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

@Mixin(LockableTileEntity.class)
public class LockableTileEntityMixin
{
	@Inject(at = @At("HEAD"), method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/Direction;)Lnet/minecraftforge/common/util/LazyOptional;", cancellable = true, remap = false)
	private void getCapability(Capability cap, Direction side, CallbackInfoReturnable<LazyOptional> cir)
	{
		TileEntity te = (TileEntity) (Object) this;
		if(!te.isRemoved() && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && te.hasLevel() && LocksUtil.locked(te.getLevel(), te.getBlockPos()))
			cir.setReturnValue(LazyOptional.empty());
	}
}