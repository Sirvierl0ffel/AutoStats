package com.lpn.autostats.render;

import static org.lwjgl.opengl.GL11.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.lpn.autostats.AutoStats;
import com.lpn.autostats.extension.Extension;
import com.lpn.autostats.extension.NormalExtension;
import com.mojang.authlib.GameProfile;

import net.labymod.api.permissions.Permissions;
import net.labymod.core.LabyModCore;
import net.labymod.core_implementation.mc18.gui.ModPlayerTabOverlay;
import net.labymod.ingamegui.Module;
import net.labymod.main.LabyMod;
import net.labymod.user.FamiliarManager;
import net.labymod.user.User;
import net.labymod.user.UserManager;
import net.labymod.user.group.LabyGroup;
import net.labymod.utils.DrawUtils;
import net.labymod.utils.ModColor;
import net.labymod.utils.manager.TagManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldSettings;

/**
 * {@link ModPlayerTabOverlay} with AutoStats rendering implemented
 * 
 * @author Sirvierl0ffel
 */
public class AutoStatsTabList extends ModPlayerTabOverlay {

	public static final Comparator<NetworkPlayerInfo> defaultComparator = (nwPlayer1, nwPlayer2) -> {
		if (nwPlayer1 == null) return 0;
		if (nwPlayer2 == null) return 0;

		ScorePlayerTeam team1 = nwPlayer1.getPlayerTeam();
		ScorePlayerTeam team2 = nwPlayer2.getPlayerTeam();
		boolean spec1 = nwPlayer1.getGameType() != WorldSettings.GameType.SPECTATOR;
		boolean spec2 = nwPlayer2.getGameType() != WorldSettings.GameType.SPECTATOR;
		return ComparisonChain.start().compareTrueFirst(spec1, spec2)
				.compare(team1 != null ? team1.getRegisteredName() : "", team2 != null ? team2.getRegisteredName() : "")
				.compare(nwPlayer1.getGameProfile().getName(), nwPlayer2.getGameProfile().getName()).result();
	};

	public static final Ordering<NetworkPlayerInfo> ordering = Ordering.from((nwPlayer1, nwPlayer2) -> {
		if (nwPlayer1 == null && nwPlayer2 == null) return 0;
		if (nwPlayer1 == null) return -1;
		if (nwPlayer2 == null) return 1;

		AutoStats autoStats = AutoStats.instance();
		if (autoStats.isEnabled() && autoStats.extensionManager.isSupported() && autoStats.isTabListEnabled()
				&& autoStats.isTabListSortingEnabled()) {
			return ComparisonChain.start().compare(nwPlayer1, nwPlayer2, (r, l) -> {
				String name1 = r.getGameProfile().getName();
				String name2 = l.getGameProfile().getName();
				return autoStats.extensionManager.comparePlayer(name1, name2);
			}).compare(nwPlayer1, nwPlayer2, defaultComparator).result();
		}

		return defaultComparator.compare(nwPlayer1, nwPlayer2);
	});

	private final Minecraft mc;
	private final GuiIngame guiIngame;
	private IChatComponent footer;
	private IChatComponent header;
	private long lastTimeOpened;
	private boolean isBeingRendered;

	/**
	 * Just creates {@link AutoStatsTabList} instance
	 * 
	 * @param mcIn
	 * @param guiIngameIn
	 */
	public AutoStatsTabList(Minecraft mcIn, GuiIngame guiIngameIn) {
		super(mcIn, guiIngameIn);
		this.mc = mcIn;
		this.guiIngame = guiIngameIn;
	}

	/** {@inheritDoc} */
	@Override
	public String getPlayerName(final NetworkPlayerInfo networkPlayerInfoIn) {
		String name = (networkPlayerInfoIn.getDisplayName() != null)
				? networkPlayerInfoIn.getDisplayName().getFormattedText()
				: ScorePlayerTeam.formatPlayerName((Team) networkPlayerInfoIn.getPlayerTeam(),
						networkPlayerInfoIn.getGameProfile().getName());
		String tagName = TagManager.getTaggedMessage(name);
		if (tagName != null) {
			return tagName;
		}
		return name;
	}

	/** {@inheritDoc} */
	@Override
	public void updatePlayerList(final boolean willBeRendered) {
		if (willBeRendered && !this.isBeingRendered) {
			this.lastTimeOpened = Minecraft.getSystemTime();
		}
		this.isBeingRendered = willBeRendered;
	}

	/** {@inheritDoc} */
	@Override
	public void renderPlayerlist(final int width, final Scoreboard scoreboardIn,
			final ScoreObjective scoreObjectiveIn) {
		Module.lastTablistRendered = System.currentTimeMillis();
		if (LabyMod.getSettings().oldTablist && Permissions.isAllowed(Permissions.Permission.ANIMATIONS)) {
			this.oldTabOverlay(width, scoreboardIn, scoreObjectiveIn);
		} else {
			this.newTabOverlay(width, scoreboardIn, scoreObjectiveIn);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void newTabOverlay(final int width, final Scoreboard scoreboardIn, final ScoreObjective scoreObjectiveIn) {
		AutoStats autoStats = AutoStats.instance();
		boolean useAutoStats = autoStats.isEnabled() && autoStats.extensionManager.isSupported()
				&& autoStats.isTabListEnabled();
		Extension current = autoStats.extensionManager.getCurrent();
		if (current instanceof NormalExtension) {
			((NormalExtension) current).fillStatsList();
		}
		int extraSpace = useAutoStats ? 6 : 0;

		FontRenderer fontRenderer = LabyModCore.getMinecraft().getFontRenderer();

		final UserManager userManager = LabyMod.getInstance().getUserManager();
		final FamiliarManager familiarManager = userManager.getFamiliarManager();
		int familiarCount = 0;
		int totalCount = 0;
		final NetHandlerPlayClient nethandlerplayclient = LabyModCore.getMinecraft().getPlayer().sendQueue;
		List<NetworkPlayerInfo> sortedNWInfo = AutoStatsTabList.ordering
				.sortedCopy(nethandlerplayclient.getPlayerInfoMap());
		int maxPlayerWidth = useAutoStats ? 112 : 0;
		int scorePlayerWidth = 0;
		for (final NetworkPlayerInfo networkplayerinfo : sortedNWInfo) {
			int playerWidth = fontRenderer.getStringWidth(this.getPlayerName(networkplayerinfo));
			if (LabyMod.getSettings().revealFamiliarUsers) {
				final UUID uuid = networkplayerinfo.getGameProfile().getId();
				if (familiarManager.isFamiliar(uuid)) {
					playerWidth += 10;
					++familiarCount;
				}
				++totalCount;
			}
			maxPlayerWidth = Math.max(maxPlayerWidth, playerWidth);
			if (scoreObjectiveIn != null
					&& scoreObjectiveIn.getRenderType() != IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
				playerWidth = fontRenderer.getStringWidth(" " + scoreboardIn
						.getValueFromObjective(networkplayerinfo.getGameProfile().getName(), scoreObjectiveIn)
						.getScorePoints());
				scorePlayerWidth = Math.max(scorePlayerWidth, playerWidth);
			}
		}

		sortedNWInfo = sortedNWInfo.subList(0, Math.min(sortedNWInfo.size(), 57));
		int columns;
		int playerCount;
		int rows;
		for (playerCount = (columns = sortedNWInfo.size()), rows = 1; columns > (useAutoStats ? 16
				: 20); columns = (playerCount + rows - 1) / rows) {
			++rows;
		}

		final boolean server = this.mc.isIntegratedServerRunning()
				|| LabyModCore.getMinecraft().getConnection().getNetworkManager().getIsencrypted();

		int scoreWidth;
		if (scoreObjectiveIn != null) {
			if (scoreObjectiveIn.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
				scoreWidth = 90;
			} else {
				scoreWidth = scorePlayerWidth;
			}
		} else {
			scoreWidth = 0;
		}
		final int columnWidth = Math.min(rows * ((server ? 9 : 0) + maxPlayerWidth + scoreWidth + 13), width - 50)
				/ rows;
		final int x = width / 2 - (columnWidth * rows + (rows - 1) * 5) / 2;
		int y = 10;

		int maxWidth = columnWidth * rows + (rows - 1 + 9) * 5;
		List<String> headerLines = null;
		List<String> footerLines = null;
		if (this.header != null) {
			headerLines = (List<String>) fontRenderer.listFormattedStringToWidth(this.header.getFormattedText(),
					width - 50);
			for (final String headerLine : headerLines) {
				maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(headerLine));
			}
		}
		if (this.footer != null) {
			footerLines = (List<String>) fontRenderer.listFormattedStringToWidth(this.footer.getFormattedText(),
					width - 50);
			for (final String footerLine : footerLines) {
				maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(footerLine));
			}
		}
		if (headerLines != null) {
			drawRect(width / 2 - maxWidth / 2 - 1, y - 1, width / 2 + maxWidth / 2 + 2,
					y + headerLines.size() * fontRenderer.FONT_HEIGHT, Integer.MIN_VALUE);
			for (final String s3 : headerLines) {
				final int i2 = fontRenderer.getStringWidth(s3);
				fontRenderer.drawStringWithShadow(s3, (float) (width / 2 - i2 / 2), (float) y, -1);
				y += fontRenderer.FONT_HEIGHT;
			}
			++y;
		}
		drawRect(width / 2 - maxWidth / 2 - 1, y - 1, width / 2 + maxWidth / 2 + 2, y + columns * (9 + extraSpace),
				Integer.MIN_VALUE);
		for (int i = 0; i < playerCount; ++i) {
			final int currentColumn = i / columns;
			final int currentRow = i % columns;
			int currentX = x + currentColumn * columnWidth + currentColumn * 5;
			final int currentY = y + currentRow * (9 + extraSpace);

			// Save gradient bounds
			int left = currentX;
			int top = currentY + 8 + extraSpace / 2;
			int right = currentX + columnWidth;
			int bottom = currentY + 8 + extraSpace;

			AutoStats.scissorBox(currentX, currentY, currentX + columnWidth, currentY + 8 + extraSpace);
			GL11.glEnable(GL_SCISSOR_TEST);
			drawRect(currentX, currentY, currentX + columnWidth, currentY + 8 + extraSpace, 0x20FFFFFF);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
			if (i < sortedNWInfo.size()) {
				final NetworkPlayerInfo nwPlayer = sortedNWInfo.get(i);
				String s4 = this.getPlayerName(nwPlayer);
				final GameProfile gameprofile = nwPlayer.getGameProfile();
				if (server) {
					final EntityPlayer entityplayer = LabyModCore.getMinecraft().getWorld()
							.getPlayerEntityByUUID(gameprofile.getId());
					final boolean premium = entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.CAPE)
							&& (gameprofile.getName().equals("Dinnerbone") || gameprofile.getName().equals("Grumm"));
					this.mc.getTextureManager().bindTexture(nwPlayer.getLocationSkin());
					final int l2 = 8 + (premium ? 8 : 0);
					final int i3 = 8 * (premium ? -1 : 1);
					Gui.drawScaledCustomSizeModalRect(currentX, currentY, 8.0f, (float) l2, 8, i3, 8, 8, 64.0f, 64.0f);
					if (entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.HAT)) {
						final int j5 = 8 + (premium ? 8 : 0);
						final int k2 = 8 * (premium ? -1 : 1);
						Gui.drawScaledCustomSizeModalRect(currentX, currentY, 40.0f, (float) j5, 8, k2, 8, 8, 64.0f,
								64.0f);
					}
					currentX += 9;
				}
				if (nwPlayer.getGameType() == WorldSettings.GameType.SPECTATOR) {
					s4 = EnumChatFormatting.ITALIC + s4;
					fontRenderer.drawStringWithShadow(s4, (float) currentX, (float) currentY, -1862270977);
				} else {
					boolean badgeVisible = false;
					if (LabyMod.getSettings().revealFamiliarUsers) {
						final User user = userManager.getUser(nwPlayer.getGameProfile().getId());
						if (user.isFamiliar()) {
							final LabyGroup group = user.getGroup();
							if (group != null) {
								group.renderBadge((double) (currentX - 1), (double) currentY, 8.0, 8.0, true);
							}
							badgeVisible = true;
						}
					}
					fontRenderer.drawStringWithShadow(s4, (float) (currentX + (badgeVisible ? 8 : 0)), (float) currentY,
							-1);
				}

				if (useAutoStats) {
					String name = nwPlayer.getGameProfile().getName();

					int statsColor = autoStats.extensionManager.getColor(name);

					GlStateManager.pushMatrix();
					GlStateManager.translate(currentX - 8, currentY + 8, 0);
					GlStateManager.scale(2d / 3, 2d / 3, 1);
					String text = autoStats.extensionManager.getTabListText(name);
					fontRenderer.drawStringWithShadow(text, 1, 1, 0xFFFFFFFF);
					GlStateManager.popMatrix();

					AutoStats.drawGradientRect(0, left, top, right, bottom,
							(statsColor & 0x00FFFFFF) | 0x00000000 /* 0x00DFDFDF */,
							(statsColor & 0x00FFFFFF) | 0x80000000);
				}

				if (scoreObjectiveIn != null && nwPlayer.getGameType() != WorldSettings.GameType.SPECTATOR) {
					final int var23 = currentX + maxPlayerWidth + 1;
					final int var24 = var23 + scoreWidth;
					if (var24 - var23 > 5) {
						this.drawScoreboardValues(scoreObjectiveIn, currentY, gameprofile.getName(), var23, var24,
								nwPlayer);
					}
				}
				this.drawPing(columnWidth, currentX - (server ? 9 : 0), currentY, nwPlayer);

				GL11.glDisable(GL_SCISSOR_TEST);
			}
		}
		if (footerLines != null) {
			y = y + columns * (9 + extraSpace) + 1;
			drawRect(width / 2 - maxWidth / 2 - 1, y - 1, width / 2 + maxWidth / 2 + 2,
					y + footerLines.size() * fontRenderer.FONT_HEIGHT, Integer.MIN_VALUE);
			for (final String s5 : footerLines) {
				final int j6 = fontRenderer.getStringWidth(s5);
				fontRenderer.drawStringWithShadow(s5, (float) (width / 2 - j6 / 2), (float) y, -1);
				y += fontRenderer.FONT_HEIGHT;
			}
		}
		if (LabyMod.getSettings().revealFamiliarUsers && LabyMod.getSettings().revealFamiliarUsersPercentage) {
			final int percent = (int) ((totalCount == 0) ? 0L : Math.round(100.0 / totalCount * familiarCount));
			final String displayString = ModColor.cl('7') + familiarCount + ModColor.cl('8') + "/" + ModColor.cl('7')
					+ totalCount + " " + ModColor.cl('a') + percent + "%";
			LabyMod.getInstance().getDrawUtils().drawRightString(displayString, (double) (width / 2 + maxWidth / 2),
					3.0, 0.7);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void oldTabOverlay(final int width, final Scoreboard scoreboardIn, final ScoreObjective scoreObjectiveIn) {
		final UserManager userManager = LabyMod.getInstance().getUserManager();
		final FamiliarManager familiarManager = userManager.getFamiliarManager();
		int familiarCount = 0;
		int totalCount = 0;
		try {
			final NetHandlerPlayClient var4 = LabyModCore.getMinecraft().getPlayer().sendQueue;
			final List<?> var5 = (List<?>) AutoStatsTabList.ordering.sortedCopy((Iterable) var4.getPlayerInfoMap());
			int var7;
			final int var6 = var7 = LabyModCore.getMinecraft().getPlayer().sendQueue.currentServerMaxPlayers;
			final ScaledResolution var8 = new ScaledResolution(Minecraft.getMinecraft());
			int var9 = 0;
			final int var10 = var8.getScaledWidth();
			int var11 = 0;
			int var12 = 0;
			int var13 = 0;
			for (var9 = 1; var7 > 20; var7 = (var6 + var9 - 1) / var9) {
				++var9;
			}
			int var14 = 300 / var9;
			if (var14 > 150) {
				var14 = 150;
			}
			final int var15 = (var10 - var9 * var14) / 2;
			final byte var16 = 10;
			drawRect(var15 - 1, var16 - 1, var15 + var14 * var9, var16 + 9 * var7, Integer.MIN_VALUE);
			for (var11 = 0; var11 < var6; ++var11) {
				var12 = var15 + var11 % var9 * var14;
				var13 = var16 + var11 / var9 * 9;
				drawRect(var12, var13, var12 + var14 - 1, var13 + 8, 553648127);
				GlStateManager.enableAlpha();
				if (var11 < var5.size()) {
					final NetworkPlayerInfo nwPlayerInfo = (NetworkPlayerInfo) var5.get(var11);
					final String name = nwPlayerInfo.getGameProfile().getName();
					final ScorePlayerTeam team = LabyModCore.getMinecraft().getWorld().getScoreboard()
							.getPlayersTeam(name);
					final String playerName = this.getPlayerName(nwPlayerInfo);
					final boolean badgeVisible = false;
					if (LabyMod.getSettings().revealFamiliarUsers) {
						final User user = userManager.getUser(nwPlayerInfo.getGameProfile().getId());
						if (user.isFamiliar()) {
							final LabyGroup group = user.getGroup();
							if (group != null) {
								group.renderBadge((double) var12, (double) var13, 8.0, 8.0, true);
							}
							++familiarCount;
						}
					}
					++totalCount;
					LabyMod.getInstance().getDrawUtils().drawString(playerName,
							(double) (var12 + (badgeVisible ? 9 : 0)), (double) var13);
					if (scoreObjectiveIn != null) {
						final int scoreX = var12 + LabyMod.getInstance().getDrawUtils().getStringWidth(playerName) + 5;
						final int var21 = var12 + var14 - 12 - 5;
						if (var21 - scoreX > 5) {
							final Score score = scoreboardIn.getValueFromObjective(name, scoreObjectiveIn);
							final String points = EnumChatFormatting.YELLOW + "" + score.getScorePoints();
							LabyMod.getInstance().getDrawUtils().drawString(points,
									(double) (var21 - LabyMod.getInstance().getDrawUtils().getStringWidth(points)),
									(double) var13, 1.6777215E7);
						}
					}
					this.drawPing(50, var12 + var14 - 52, var13, nwPlayerInfo);
				}
			}
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.disableLighting();
			GlStateManager.enableAlpha();
			if (LabyMod.getSettings().revealFamiliarUsers && LabyMod.getSettings().revealFamiliarUsersPercentage) {
				final int percent = (int) ((totalCount == 0) ? 0L : Math.round(100.0 / totalCount * familiarCount));
				final String displayString = ModColor.cl('7') + familiarCount + ModColor.cl('8') + "/"
						+ ModColor.cl('7') + totalCount + " " + ModColor.cl('a') + percent + "%";
				LabyMod.getInstance().getDrawUtils().drawRightString(displayString, (double) (var15 + var14 * var9),
						3.0, 0.7);
			}
		} catch (Exception ex) {
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void drawPing(final int p_175245_1_, final int p_175245_2_, final int p_175245_3_,
			final NetworkPlayerInfo networkPlayerInfoIn) {
		if (!LabyMod.getSettings().tabPing) {
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			this.mc.getTextureManager().bindTexture(ModPlayerTabOverlay.icons);
			final byte var5 = 0;
			byte var6;
			if (networkPlayerInfoIn.getResponseTime() < 0) {
				var6 = 5;
			} else if (networkPlayerInfoIn.getResponseTime() < 150) {
				var6 = 0;
			} else if (networkPlayerInfoIn.getResponseTime() < 300) {
				var6 = 1;
			} else if (networkPlayerInfoIn.getResponseTime() < 600) {
				var6 = 2;
			} else if (networkPlayerInfoIn.getResponseTime() < 1000) {
				var6 = 3;
			} else {
				var6 = 4;
			}
			this.zLevel += 100.0f;
			this.drawTexturedModalRect(p_175245_2_ + p_175245_1_ - 11, p_175245_3_, 0 + var5 * 10, 176 + var6 * 8, 10,
					8);
		} else {
			this.zLevel += 100.0f;
		}
		final DrawUtils draw = LabyMod.getInstance().getDrawUtils();
		GL11.glPushMatrix();
		GlStateManager.scale(0.5, 0.5, 0.5);
		int ping = networkPlayerInfoIn.getResponseTime();
		if (ping >= 1000) {
			ping = 999;
		}
		if (ping < 0) {
			ping = 0;
		}
		final boolean useColors = LabyMod.getSettings().tabPing_colored;
		String c = useColors ? "a" : "f";
		if (useColors) {
			if (ping > 150) {
				c = "2";
			}
			if (ping > 300) {
				c = "c";
			}
			if (ping > 600) {
				c = "4";
			}
		}
		if (LabyMod.getSettings().tabPing) {
			draw.drawCenteredString(ModColor.cl(c) + ((ping == 0) ? "?" : ping),
					(double) ((p_175245_2_ + p_175245_1_) * 2 - 12), (double) (p_175245_3_ * 2 + 5));
		}
		GL11.glPopMatrix();
		this.zLevel -= 100.0f;
	}

	private void drawScoreboardValues(final ScoreObjective objective, final int p_175247_2_, final String p_175247_3_,
			final int p_175247_4_, final int p_175247_5_, final NetworkPlayerInfo p_175247_6_) {
		FontRenderer fontRenderer = LabyModCore.getMinecraft().getFontRenderer();
		final int i = objective.getScoreboard().getValueFromObjective(p_175247_3_, objective).getScorePoints();
		if (objective.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
			this.mc.getTextureManager().bindTexture(ModPlayerTabOverlay.icons);
			if (this.lastTimeOpened == p_175247_6_.func_178855_p()) {
				if (i < p_175247_6_.func_178835_l()) {
					p_175247_6_.func_178846_a(Minecraft.getSystemTime());
					p_175247_6_.func_178844_b((long) (this.guiIngame.getUpdateCounter() + 20));
				} else if (i > p_175247_6_.func_178835_l()) {
					p_175247_6_.func_178846_a(Minecraft.getSystemTime());
					p_175247_6_.func_178844_b((long) (this.guiIngame.getUpdateCounter() + 10));
				}
			}
			if (Minecraft.getSystemTime() - p_175247_6_.func_178847_n() > 1000L
					|| this.lastTimeOpened != p_175247_6_.func_178855_p()) {
				p_175247_6_.func_178836_b(i);
				p_175247_6_.func_178857_c(i);
				p_175247_6_.func_178846_a(Minecraft.getSystemTime());
			}
			p_175247_6_.func_178843_c(this.lastTimeOpened);
			p_175247_6_.func_178836_b(i);
			final int j = LabyModCore.getMath().ceiling_float_int(Math.max(i, p_175247_6_.func_178860_m()) / 2.0f);
			final int k = Math.max(LabyModCore.getMath().ceiling_float_int((float) (i / 2)),
					Math.max(LabyModCore.getMath().ceiling_float_int((float) (p_175247_6_.func_178860_m() / 2)), 10));
			final boolean flag = p_175247_6_.func_178858_o() > this.guiIngame.getUpdateCounter()
					&& (p_175247_6_.func_178858_o() - this.guiIngame.getUpdateCounter()) / 3L % 2L == 1L;
			if (j > 0) {
				final float f = Math.min((p_175247_5_ - p_175247_4_ - 4) / k, 9.0f);
				if (f > 3.0f) {
					for (int l = j; l < k; ++l) {
						this.drawTexturedModalRect(p_175247_4_ + l * f, (float) p_175247_2_, flag ? 25 : 16, 0, 9, 9);
					}
					for (int j2 = 0; j2 < j; ++j2) {
						this.drawTexturedModalRect(p_175247_4_ + j2 * f, (float) p_175247_2_, flag ? 25 : 16, 0, 9, 9);
						if (flag) {
							if (j2 * 2 + 1 < p_175247_6_.func_178860_m()) {
								this.drawTexturedModalRect(p_175247_4_ + j2 * f, (float) p_175247_2_, 70, 0, 9, 9);
							}
							if (j2 * 2 + 1 == p_175247_6_.func_178860_m()) {
								this.drawTexturedModalRect(p_175247_4_ + j2 * f, (float) p_175247_2_, 79, 0, 9, 9);
							}
						}
						if (j2 * 2 + 1 < i) {
							this.drawTexturedModalRect(p_175247_4_ + j2 * f, (float) p_175247_2_, (j2 >= 10) ? 160 : 52,
									0, 9, 9);
						}
						if (j2 * 2 + 1 == i) {
							this.drawTexturedModalRect(p_175247_4_ + j2 * f, (float) p_175247_2_, (j2 >= 10) ? 169 : 61,
									0, 9, 9);
						}
					}
				} else {
					final float f2 = LabyModCore.getMath().clamp_float(i / 20.0f, 0.0f, 1.0f);
					final int i2 = (int) ((1.0f - f2) * 255.0f) << 16 | (int) (f2 * 255.0f) << 8;
					String s = "" + i / 2.0f;
					if (p_175247_5_ - fontRenderer.getStringWidth(s + "hp") >= p_175247_4_) {
						s += "hp";
					}
					fontRenderer.drawStringWithShadow(s,
							(float) ((p_175247_5_ + p_175247_4_) / 2 - fontRenderer.getStringWidth(s) / 2),
							(float) p_175247_2_, i2);
				}
			}
		} else {
			final String s2 = EnumChatFormatting.YELLOW + "" + i;
			fontRenderer.drawStringWithShadow(s2, (float) (p_175247_5_ - fontRenderer.getStringWidth(s2)),
					(float) p_175247_2_, 16777215);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void setFooter(final IChatComponent footerIn) {
		this.footer = footerIn;

		LabyMod.getInstance().getEventManager()
				.callAllFooter(LabyModCore.getMinecraft().getChatComponent((Object) footerIn));
	}

	/** {@inheritDoc} */
	@Override
	public void setHeader(final IChatComponent headerIn) {
		this.header = headerIn;

		LabyMod.getInstance().getEventManager()
				.callAllHeader(LabyModCore.getMinecraft().getChatComponent((Object) headerIn));
	}

	/** {@inheritDoc} */
	@Override
	public void func_181030_a() {
		this.header = null;
		this.footer = null;
	}

}