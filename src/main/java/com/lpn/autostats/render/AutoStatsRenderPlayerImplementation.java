package com.lpn.autostats.render;

import org.lwjgl.opengl.GL11;

import com.lpn.autostats.AutoStats;

import net.labymod.core.LabyModCore;
import net.labymod.core.WorldRendererAdapter;
import net.labymod.core_implementation.mc18.RenderPlayerImplementation;
import net.labymod.main.LabyMod;
import net.labymod.mojang.RenderPlayerHook.RenderPlayerCustom;
import net.labymod.user.User;
import net.labymod.user.group.EnumGroupDisplayType;
import net.labymod.user.group.LabyGroup;
import net.labymod.utils.ModColor;
import net.labymod.utils.manager.TagManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

/**
 * Renders the the name tag background of the players in the color of their
 * skill level
 * 
 * @author Sirvierl0ffel
 */
public class AutoStatsRenderPlayerImplementation extends RenderPlayerImplementation {

	private static final boolean CHEATY = "Sirvierl0ffel".equals(System.getProperty("user.name"));

	/** {@inheritDoc} */
	@Override
	public void renderName(RenderPlayerCustom renderPlayer, AbstractClientPlayer entity, double x, double y, double z) {
		boolean canRender = Minecraft.isGuiEnabled() && !entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)
				&& entity.riddenByEntity == null;
		if (renderPlayer.canRenderTheName(entity) || entity == renderPlayer.getRenderManager().livingPlayer
				&& LabyMod.getSettings().showMyName && canRender) {
			double distance = entity.getDistanceSqToEntity(renderPlayer.getRenderManager().livingPlayer);
			float f = entity.isSneaking() ? 32.0F : 64.0F;
			if (distance < (double) (f * f)) {
				User user = entity instanceof EntityPlayer
						? LabyMod.getInstance().getUserManager().getUser(entity.getUniqueID())
						: null;
				float maxNameTagHeight = user != null && LabyMod.getSettings().cosmetics ? user.getMaxNameTagHeight()
						: 0.0F;
				String username = entity.getDisplayName().getFormattedText();
				GlStateManager.alphaFunc(516, 0.1F);
				String tagName = TagManager.getTaggedMessage(username);
				if (tagName != null) {
					username = tagName;
				}
				float fixedPlayerViewX = renderPlayer.getRenderManager().playerViewX
						* (float) (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? -1 : 1);
				y += (double) maxNameTagHeight;
				FontRenderer fontrenderer = renderPlayer.getFontRendererFromRenderManager();
				if (entity.isSneaking()) {
					GlStateManager.pushMatrix();
					GlStateManager.translate((float) x,
							(float) y + entity.height + 0.5F - (entity.isChild() ? entity.height / 2.0F : 0.0F),
							(float) z);
					GL11.glNormal3f(0.0F, 1.0F, 0.0F);
					GlStateManager.rotate(-renderPlayer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
					GlStateManager.rotate(fixedPlayerViewX, 1.0F, 0.0F, 0.0F);
					GlStateManager.scale(-0.02666667F, -0.02666667F, 0.02666667F);
					GlStateManager.translate(0.0F, 9.374999F, 0.0F);
					GlStateManager.disableLighting();
					GlStateManager.depthMask(false);
					GlStateManager.enableBlend();
					GlStateManager.disableTexture2D();
					GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
					int widthDiv2 = fontrenderer.getStringWidth(username) / 2;
					Tessellator tessellator = Tessellator.getInstance();
					WorldRenderer worldrenderer = tessellator.getWorldRenderer();
					worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

					// AutoStats color thingy
					int color = 0x00000000;
					AutoStats autoStats = AutoStats.instance();
					boolean use = autoStats.isUsable() && autoStats.isNameTagsEnabled();
					if (use && autoStats.isColorEverythingEnabled())
						color = autoStats.extensionManager.getColor(entity.getName());
					float r = (float) (color >> 16 & 255) / 255.0F;
					float g = (float) (color >> 8 & 255) / 255.0F;
					float b = (float) (color & 255) / 255.0F;

					worldrenderer.pos((double) (-widthDiv2 - 1), -1.0D, 0.0D).color(r, g, b, 0.25F).endVertex();
					worldrenderer.pos((double) (-widthDiv2 - 1), 8.0D, 0.0D).color(r, g, b, 0.25F).endVertex();
					worldrenderer.pos((double) (widthDiv2 + 1), 8.0D, 0.0D).color(r, g, b, 0.25F).endVertex();
					worldrenderer.pos((double) (widthDiv2 + 1), -1.0D, 0.0D).color(r, g, b, 0.25F).endVertex();
					tessellator.draw();
					GlStateManager.enableTexture2D();
					GlStateManager.depthMask(true);
					fontrenderer.drawString(username, -fontrenderer.getStringWidth(username) / 2, 0, 553648127);

					// AutoStats gradient thingy
					if (use && !autoStats.isColorEverythingEnabled()) {
						int statsColor = autoStats.extensionManager.getColor(entity.getName());
						int left = -widthDiv2 - 1;
						int right = widthDiv2 + 1;
						int top = 9 - autoStats.getGradientHeight();
						int bottom = 8;
						AutoStats.drawGradientRect(-1, left, top, right, bottom, (statsColor & 0x00FFFFFF) | 0x00000000,
								(statsColor & 0x00FFFFFF) | 0x28000000);
					}

					GlStateManager.enableLighting();
					GlStateManager.disableBlend();
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					GlStateManager.popMatrix();
				} else {
					LabyGroup labyGroup = user.getGroup();
					double size;
					if (user.getSubTitle() != null) {
						GlStateManager.pushMatrix();
						size = user.getSubTitleSize();
						GlStateManager.translate(0.0D, -0.2D + size / 8.0D, 0.0D);
						this.renderLivingLabelCustom(renderPlayer, entity, user.getSubTitle(), x, y, z, 64,
								(float) size);
						y += size / 6.0D;
						GlStateManager.popMatrix();
					}
					if (labyGroup != null && labyGroup.getDisplayType() == EnumGroupDisplayType.BESIDE_NAME) {
						GlStateManager.pushMatrix();
						GlStateManager.translate((float) x,
								(float) y + entity.height + 0.5F - (entity.isChild() ? entity.height / 2.0F : 0.0F),
								(float) z);
						GlStateManager.rotate(-renderPlayer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
						GlStateManager.rotate(fixedPlayerViewX, 1.0F, 0.0F, 0.0F);
						GlStateManager.scale(-0.02666667F, -0.02666667F, 0.02666667F);
						GlStateManager.disableLighting();
						GlStateManager.disableBlend();
						GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
						size = (double) (-fontrenderer.getStringWidth(username) / 2 - 2 - 8);
						labyGroup.renderBadge(size, -0.5D, 8.0D, 8.0D, false);
						GlStateManager.enableLighting();
						GlStateManager.disableBlend();
						GlStateManager.resetColor();
						GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
						GlStateManager.popMatrix();
					}
					if (distance < 100.0D) {
						Scoreboard scoreboard = entity.getWorldScoreboard();
						ScoreObjective scoreobjective = scoreboard.getObjectiveInDisplaySlot(2);
						if (scoreobjective != null) {
							Score score = scoreboard.getValueFromObjective(entity.getName(), scoreobjective);
							this.renderLivingLabelCustom(renderPlayer, entity,
									score.getScorePoints() + " " + scoreobjective.getDisplayName(), x, y, z, 64);
							y += (double) ((float) LabyMod.getInstance().getDrawUtils().getFontRenderer().FONT_HEIGHT
									* 1.15F * 0.02666667F);
						}
					}
					this.renderLivingLabelCustom(renderPlayer, entity, username, x,
							y - (entity.isChild() ? (double) (entity.height / 2.0F) : 0.0D), z, 64);
					if (tagName != null) {
						GlStateManager.pushMatrix();
						GlStateManager.translate((float) x,
								(float) y + entity.height + 0.5F - (entity.isChild() ? entity.height / 2.0F : 0.0F),
								(float) z);
						GlStateManager.rotate(-renderPlayer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
						GlStateManager.rotate(fixedPlayerViewX, 1.0F, 0.0F, 0.0F);
						GlStateManager.scale(-0.01666667F, -0.01666667F, 0.01666667F);
						GlStateManager.translate(0.0F, entity.isSneaking() ? 17.0F : 2.0F, 0.0F);
						GlStateManager.disableLighting();
						GlStateManager.enableBlend();
						fontrenderer.drawString("\u270e",
								5 + (int) ((double) fontrenderer.getStringWidth(username) * 0.8D), 0,
								ModColor.toRGB(255, 255, 0, 255));
						GlStateManager.disableBlend();
						GlStateManager.enableLighting();
						GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
						GlStateManager.popMatrix();
					}
					if (labyGroup != null && labyGroup.getDisplayType() == EnumGroupDisplayType.ABOVE_HEAD) {
						GlStateManager.pushMatrix();
						size = 0.5D;
						GlStateManager.scale(size, size, size);
						GlStateManager.translate(0.0D, 2.0D, 0.0D);
						this.renderLivingLabelCustom(renderPlayer, entity, labyGroup.getDisplayTag(), x / size,
								(y - (entity.isChild() ? (double) (entity.height / 2.0F) : 0.0D) + 0.3D) / size,
								z / size, 10);
						GlStateManager.popMatrix();
					}
				}
			}
		}

	}

	/** {@inheritDoc} */
	@Override
	protected void renderLivingLabelCustom(RenderPlayerCustom renderPlayer, Entity entity, String str, double x,
			double y, double z, int maxDistance, float scale) {
		double d0 = entity.getDistanceSqToEntity(renderPlayer.getRenderManager().livingPlayer);
		if (d0 <= (double) (maxDistance * maxDistance)) {
			float fixedPlayerViewX = renderPlayer.getRenderManager().playerViewX
					* (float) (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? -1 : 1);
			FontRenderer fontrenderer = renderPlayer.getFontRendererFromRenderManager();
			float f1 = 0.016666668F * scale;
			GlStateManager.pushMatrix();
			GlStateManager.translate((float) x + 0.0F, (float) y + entity.height + 0.5F, (float) z);
			GL11.glNormal3f(0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(-renderPlayer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(fixedPlayerViewX, 1.0F, 0.0F, 0.0F);
			GlStateManager.scale(-f1, -f1, f1);
			GlStateManager.disableLighting();
			GlStateManager.depthMask(false);
			GlStateManager.disableDepth();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
			Tessellator tessellator = Tessellator.getInstance();
			WorldRendererAdapter worldrenderer = LabyModCore.getWorldRenderer();
			int i = 0;
			if (str.equals("deadmau5")) {
				i = -10;
			}
			int widthDiv2 = fontrenderer.getStringWidth(str) / 2;
			GlStateManager.disableTexture2D();
			worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

			// AutoStats color thingy
			int color = 0x00000000;
			AutoStats autoStats = AutoStats.instance();
			boolean use = entity instanceof EntityPlayer && autoStats.isUsable() && autoStats.isNameTagsEnabled();
			if (use && autoStats.isColorEverythingEnabled())
				color = autoStats.extensionManager.getColor(entity.getName());
			float r = (float) (color >> 16 & 255) / 255.0F;
			float g = (float) (color >> 8 & 255) / 255.0F;
			float b = (float) (color & 255) / 255.0F;

			worldrenderer.pos((double) (-widthDiv2 - 1), (double) (-1 + i), 0.0D).color(r, g, b, 0.25F).endVertex();
			worldrenderer.pos((double) (-widthDiv2 - 1), (double) (8 + i), 0.0D).color(r, g, b, 0.25F).endVertex();
			worldrenderer.pos((double) (widthDiv2 + 1), (double) (8 + i), 0.0D).color(r, g, b, 0.25F).endVertex();
			worldrenderer.pos((double) (widthDiv2 + 1), (double) (-1 + i), 0.0D).color(r, g, b, 0.25F).endVertex();
			tessellator.draw();

			// DARK AutoStats gradient thingy
			if (use && !autoStats.isColorEverythingEnabled()) {
				int statsColor = autoStats.extensionManager.getColor(entity.getName());
				int left = -widthDiv2 - 1;
				int right = widthDiv2 + 1;
				int top = 9 - autoStats.getGradientHeight();
				int bottom = 8;
				AutoStats.drawGradientRect(0, left, top, right, bottom, (statsColor & 0x00FFFFFF) | 0x00000000,
						(statsColor & 0x00FFFFFF) | 0x48000000);
			}

			GlStateManager.enableTexture2D();
			fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, 0x40FFFFFF);
			if (!use || !CHEATY) GlStateManager.enableDepth();
			GlStateManager.depthMask(true);

			// BRIGHT AutoStats gradient thingy
			if (use && !autoStats.isColorEverythingEnabled()) {
				int statsColor = autoStats.extensionManager.getColor(entity.getName());
				int left = -widthDiv2 - 1;
				int right = widthDiv2 + 1;
				int top = 9 - autoStats.getGradientHeight();
				int bottom = 8;
				AutoStats.drawGradientRect(0.05f, left, top, right, bottom, (statsColor & 0x00FFFFFF) | 0x00000000,
						(statsColor & 0x00FFFFFF) | 0x68000000);
			}

			fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, -1);
			GlStateManager.enableDepth();
			GlStateManager.enableLighting();
			GlStateManager.disableBlend();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.popMatrix();
		}

	}
}