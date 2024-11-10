package me.pau.mod.locks.client.event;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import me.pau.mod.locks.Locks;
import me.pau.mod.locks.client.init.LocksRenderTypes;
import me.pau.mod.locks.client.util.LocksClientUtil;
import me.pau.mod.locks.common.capability.ISelection;
import me.pau.mod.locks.common.config.LocksServerConfig;
import me.pau.mod.locks.common.init.LocksCapabilities;
import me.pau.mod.locks.common.init.LocksItemTags;
import me.pau.mod.locks.common.util.Lockable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.Mth;
import com.mojang.math.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Locks.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LocksClientForgeEvents {
	public static Lockable tooltipLockable;

	private LocksClientForgeEvents() {}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent e) {
		Minecraft mc = Minecraft.getInstance();
		if(e.phase != TickEvent.Phase.START || mc.level == null || mc.isPaused())
			return;
		mc.level.getCapability(LocksCapabilities.LOCKABLE_HANDLER).orElse(null).getLoaded().values().forEach(lkb -> lkb.tick());
	}

	@SubscribeEvent
	public static void onRenderWorld(RenderLevelStageEvent e) {
		Minecraft mc = Minecraft.getInstance();
		PoseStack mtx = e.getPoseStack();
		MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

		// use mixin to avoid models disappearing  in water and when fabulous graphics are on
		// renderLocks(mtx, buf, LocksClientUtil.getClippingHelper(mtx, e.getProjectionMatrix()), e.getPartialTicks());
		renderSelection(mtx, buf);
	}

	public static boolean holdingPick(PlayerEntity player) {
		for(InteractionHand hand : InteractionHand.values())
			if(player.getItemInHand(hand).getItem().is(LocksItemTags.LOCK_PICKS))
				return true;
		return false;
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGameOverlayEvent.Pre e) {
		Minecraft mc = Minecraft.getInstance();
		if(e.getType() != RenderGameOverlayEvent.ElementType.ALL || tooltipLockable == null)
			return;
		if(holdingPick(mc.player)) {
			PoseStack mtx = e.getPoseStack();
			Vector3f vec = LocksClientUtil.worldToScreen(tooltipLockable.getLockState(mc.level).pos,e.getPartialTicks());
			if (vec.z() < 0d) {
				mtx.pushPose();
				mtx.translate(vec.x(), vec.y(), 0f);
				renderHudTooltip(mtx, Lists.transform(tooltipLockable.stack.getTooltipLines(mc.player, mc.options.advancedItemTooltips ? TooltipFlag.Default.NORMAL : TooltipFlag.Default.NORMAL), Component::getVisualOrderText), mc.font);
				mtx.popPose();
			}
		}
		tooltipLockable = null;
	}

	public static void renderLocks(PoseStack mtx, MultiBufferSource.BufferSource buf, ClippingHelper ch, float pt) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 o = LocksClientUtil.getCamera().getPosition();
		BlockPos.Mutable mut = new BlockPos.Mutable();

		double dMin = 0d;

		for(Lockable lkb : mc.level.getCapability(LocksCapabilities.LOCKABLE_HANDLER).orElse(null).getLoaded().values()) {
			Lockable.State state = lkb.getLockState(mc.level);
			if(state == null || !state.inRange(o) || !state.inView(ch))
				continue;

			double d = o.subtract(state.pos).lengthSqr();
			if(d <= 25d) {
				Vec3 look = o.add(mc.player.getViewVector(pt));
				double d1 = LocksClientUtil.distanceToLineSq(state.pos, o, look);
				if(d1 <= 4d && (dMin == 0d || d1 < dMin)) {
					tooltipLockable = lkb;
					dMin = d1;
				}
			}

			mtx.pushPose();
			// For some reason translating by negative player position and then the point coords causes jittering in very big z and x coords. Why? Thus we use 1 translation instead
			mtx.translate(state.pos.x - o.x, state.pos.y - o.y, state.pos.z - o.z);
			// FIXME 3 FUCKING QUATS PER FRAME !!! WHAT THE FUUUUUUCK!!!!!!!!!!!
			mtx.mulPose(Vector3f.YP.rotationDegrees(-state.tr.dir.toYRot() - 180f));
			if(state.tr.face != AttachFace.WALL)
				mtx.mulPose(Vector3f.XP.rotationDegrees(90f));
			mtx.translate(0d, 0.1d, 0d);
			mtx.mulPose(Vector3f.ZP.rotationDegrees(Mth.sin(LocksClientUtil.cubicBezier1d(1f, 1f, LocksClientUtil.lerp(lkb.maxSwingTicks - lkb.oldSwingTicks, lkb.maxSwingTicks - lkb.swingTicks, pt) / lkb.maxSwingTicks) * lkb.maxSwingTicks / 5f * 3.14f) * 10f));
			mtx.translate(0d, -0.1d, 0d);
			mtx.scale(0.5f, 0.5f, 0.5f);
			int light = WorldRenderer.getLightColor(mc.level, mut.set(state.pos.x, state.pos.y, state.pos.z));
			mc.getItemRenderer().renderStatic(lkb.stack, ItemCameraTransforms.TransformType.FIXED, light, OverlayTexture.NO_OVERLAY, mtx, buf);
			mtx.popPose();
		}
		buf.endBatch();
	}

	public static void renderSelection(PoseStack mtx, MultiBufferSource.BufferSource buf) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 o = LocksClientUtil.getCamera().getPosition();
		ISelection select = mc.player.getCapability(LocksCapabilities.SELECTION).orElse(null);
		if(select == null)
			return;
		BlockPos pos = select.get();
		if(pos == null)
			return;
		BlockPos pos1 = mc.hitResult instanceof BlockRayTraceResult ? ((BlockRayTraceResult) mc.hitResult).getBlockPos() : pos;
		boolean allow = Math.abs(pos.getX() - pos1.getX()) * Math.abs(pos.getY() - pos1.getY()) * Math.abs(pos.getZ() - pos1.getZ()) <= LocksServerConfig.MAX_LOCKABLE_VOLUME.get() && LocksServerConfig.canLock(mc.level, pos1);
		// Same as above
		WorldRenderer.renderLineBox(mtx, buf.getBuffer(LocksRenderTypes.OVERLAY_LINES), Math.min(pos.getX(), pos1.getX()) - o.x, Math.min(pos.getY(), pos1.getY()) - o.y, Math.min(pos.getZ(), pos1.getZ()) - o.z, Math.max(pos.getX(), pos1.getX()) + 1d - o.x, Math.max(pos.getY(), pos1.getY()) + 1d - o.y, Math.max(pos.getZ(), pos1.getZ()) + 1d - o.z, allow ? 0f : 1f, allow ? 1f : 0f, 0f, 0.5f);
		RenderSystem.disableDepthTest();
		buf.endBatch();
	}

	// Taken from Screen and modified to draw and fancy line and square and removed color recalculation
	public static void renderHudTooltip(PoseStack mtx, List<? extends IReorderingProcessor> lines, FontRenderer font) {
		if (lines.isEmpty())
			return;
		int width = 0;
		for (IReorderingProcessor line : lines) {
			int j = font.width(line);
			if (j > width)
				width = j;
		}

		int x = 36;
		int y = -36;
		int height = 8;
		if (lines.size() > 1)
			height += 2 + (lines.size() - 1) * 10;

		mtx.pushPose();

		BufferBuilder buf = Tessellator.getInstance().getBuilder();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		LocksClientUtil.square(buf, mtx, 0f, 0f, 4f, 0.05f, 0f, 0.3f, 0.8f);
		LocksClientUtil.line(buf, mtx, 1f, -1f, x / 3f + 0.6f, y / 2f, 2f, 0.05f, 0f, 0.3f, 0.8f);
		LocksClientUtil.line(buf, mtx, x / 3f, y / 2f, x - 3f, y / 2f, 2f, 0.05f, 0f, 0.3f, 0.8f);
		// line(buf, last, 1f, -1f, x - 3f, y / 2f, 2f, 0.05f, 0f, 0.3f, 0.8f);
		LocksClientUtil.vGradient(buf, mtx, x - 3, y - 4, x + width + 3, y - 3, 0.0627451f, 0f, 0.0627451f, 0.9411765f, 0.0627451f, 0f, 0.0627451f, 0.9411765f);
		LocksClientUtil.vGradient(buf, mtx, x - 3, y + height + 3, x + width + 3, y + height + 4, 0.0627451f, 0f, 0.0627451f, 0.9411765f, 0.0627451f, 0f, 0.0627451f, 0.9411765f);
		LocksClientUtil.vGradient(buf, mtx, x - 3, y - 3, x + width + 3, y + height + 3, 0.0627451f, 0f, 0.0627451f, 0.9411765f, 0.0627451f, 0f, 0.0627451f, 0.9411765f);
		LocksClientUtil.vGradient(buf, mtx, x - 4, y - 3, x - 3, y + height + 3, 0.0627451f, 0f, 0.0627451f, 0.9411765f, 0.0627451f, 0f, 0.0627451f, 0.9411765f);
		LocksClientUtil.vGradient(buf, mtx, x + width + 3, y - 3, x + width + 4, y + height + 3, 0.0627451f, 0f, 0.0627451f, 0.9411765f, 0.0627451f, 0f, 0.0627451f, 0.9411765f);
		LocksClientUtil.vGradient(buf, mtx, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, 0.3137255f, 0f, 1f, 0.3137255f, 0.15686275f, 0f, 0.49803922f, 0.3137255f);
		LocksClientUtil.vGradient(buf, mtx, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, 0.3137255f, 0f, 1f, 0.3137255f, 0.15686275f, 0f, 0.49803922f, 0.3137255f);
		LocksClientUtil.vGradient(buf, mtx, x - 3, y - 3, x + width + 3, y - 3 + 1, 0.3137255f, 0f, 1f, 0.3137255f, 0.3137255f, 0f, 1f, 0.3137255f);
		LocksClientUtil.vGradient(buf, mtx, x - 3, y + height + 2, x + width + 3, y + height + 3, 0.15686275f, 0f, 0.49803922f, 0.3137255f, 0.15686275f, 0f, 0.49803922f, 0.3137255f);
		RenderSystem.enableDepthTest();
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.shadeModel(7425);
		buf.end();
		WorldVertexBufferUploader.end(buf);
		RenderSystem.shadeModel(7424);
		RenderSystem.disableBlend();
		RenderSystem.enableTexture();
		MultiBufferSource.BufferSource buf1 = MultiBufferSource.immediate(Tessellator.getInstance().getBuilder());

		Matrix4f last = mtx.last().pose();
		for (int a = 0; a < lines.size(); ++a) {
			IReorderingProcessor line = lines.get(a);
			if (line != null)
				font.drawInBatch(line, (float) x, (float) y, -1, true, last, buf1, false, 0, 15728880);
			if (a == 0)
				y += 2;
			y += 10;
		}

		buf1.endBatch();

		mtx.popPose();
	}
}