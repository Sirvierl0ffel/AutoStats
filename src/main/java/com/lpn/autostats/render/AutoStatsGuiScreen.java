package com.lpn.autostats.render;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.lpn.autostats.AutoStats;
import com.lpn.autostats.extension.Extension;
import com.lpn.autostats.extension.ExtensionManager;

import net.labymod.gui.elements.Scrollbar;
import net.labymod.settings.elements.ColorPickerCheckBoxBulkElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.DropDownElement;
import net.labymod.settings.elements.SettingsElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * Manages the extension settings
 * 
 * @author Sirvierl0ffel
 */

public class AutoStatsGuiScreen extends GuiScreen {

	private static final ResourceLocation TEXTURE_STEVE = new ResourceLocation("textures/entity/steve.png");

	private final AutoStats autoStats;
	private final ExtensionManager extensionManager;
	private final GuiScreen parentScreen;

	private ExtensionList list;
	private Scrollbar scrollbar = new Scrollbar(1);
	private GuiButton blacklistButton;
	private GuiButton viewFolderButton;
	private GuiButton reloadButton;
	private GuiButton doneButton;
	private GuiButton discordButton;
	private GuiButton buttonBack;

	private List<SettingsElement> elementList = new ArrayList<>();
	private SettingsElement mouseOverElement;

	private long lastBlacklistHit;

	private Map<SettingsElement, Double> path = new LinkedHashMap<>();

	/**
	 * Creates new {@link AutoStatsGuiScreen}, which can be accessed via the
	 * command-block button in the chat
	 * 
	 * @param screen the parent screen
	 */
	public AutoStatsGuiScreen(GuiScreen screen) {
		parentScreen = screen;
		autoStats = AutoStats.instance();
		extensionManager = autoStats.extensionManager;
		autoStats.opened = true;
		JsonObject config = autoStats.getConfig();
		if (config != null) {
			config.addProperty("opened", true);
			autoStats.saveConfig();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void initGui() {
		buttonList.clear();

		int btnWidth = width / 4 - 5;
		int btnX = 4;
		buttonList.add(blacklistButton = new GuiButton(1, btnX, height - 26, btnWidth, 20, "Blacklist"));
		blacklistButton.enabled = false;
		btnX += btnWidth + 4;
		buttonList.add(viewFolderButton = new GuiButton(2, btnX, height - 26, btnWidth, 20, "View Folder"));
		btnX += btnWidth + 4;
		buttonList.add(reloadButton = new GuiButton(3, btnX, height - 26, btnWidth, 20, "Reload"));
		btnX += btnWidth + 4;
		buttonList.add(doneButton = new GuiButton(4, btnX, height - 26, btnWidth, 20, "Done"));
		buttonList
				.add(discordButton = new GuiButton(5, 4, height - 51, width / 2 - 8, 20, "AutoStats Discord for More"));
		buttonList.add(buttonBack = new GuiButton(6, width - 26, 4, 22, 20, "<"));

		scrollbar.setPosition(width - 6, 32, width - 2, height - 58);
		scrollbar.setSpeed(20);
		scrollbar.init();

		list = new ExtensionList();
		list.registerScrollButtons(7, 8);
	}

	@Override
	public void updateScreen() {
		for (SettingsElement e : elementList) {
			e.updateScreen();
		}
	}
	
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		autoStats.saveConfig();
	}

	/** {@inheritDoc} */
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		buttonBack.enabled = path.size() > 0;

		drawBackground(0);

		drawRect(width / 2 + 2, 32, width - 2, height - 58, 0x80000000);

		mouseOverElement = null;

		if (list.selected != null) {
			// The better method to make scroll panels
			AutoStats.scissorBox(width / 2 + 2, 32, width - 2, height - 58);
			glEnable(GL_SCISSOR_TEST);

			// Code I copied by myself from LabyModAddonsGui
			double totalEntryHeight = 0;
			double listY = 38 + scrollbar.getScrollY();
			boolean canRenderDescription = true;

			// Date format for the date of extensions
			SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy hh:mm");

			// Create info string
			String extensionInfo = "\u00A7n" + list.selected.name.replace("\u00A7r", "\u00A7r\u00A7n") + " by "
					+ list.selected.author.replace("\u00A7r", "\u00A7r\u00A7n") + "\u00A7r";
			if (extensionManager.getCurrent() == list.selected) extensionInfo += " \u00A7e[Current]";
			if (list.selected.isCrashed()) extensionInfo += " \u00A7c[Crashed]";
			if (extensionManager.isBlackListed(list.selected)) extensionInfo += " \u00A7c[Blacklisted]";
			extensionInfo += "\n\n\u00A77Date: \u00A78" + df.format(new Date(list.selected.date));
			extensionInfo += "\n\u00A77File: \u00A78" + list.selected.getFile();
			extensionInfo += "\n\u00A77Class: \u00A78" + list.selected.getClass().getName();
			extensionInfo += "\n";

			if (path.size() > 0) extensionInfo = "";

			// Draw info string
			fontRendererObj.drawSplitString(extensionInfo, width / 2 + 9, (int) listY, width / 2 - 10, 0xFFFFFFFF);
			int stringHeight = fontRendererObj.splitStringWidth(extensionInfo, width / 2 - 10) + 5;
			listY += stringHeight;

			// Code I copied by myself from LabyModAddonsGui to render SettingsElement list
			// from the selected extension
			for (int zLevel = 0; zLevel < 2; zLevel++) {
				totalEntryHeight = 0.0;
				elementList = Lists.reverse(elementList);
				for (SettingsElement settingsElement : elementList) {
					listY += settingsElement.getEntryHeight() + 2;
					totalEntryHeight += settingsElement.getEntryHeight() + 2;
				}
				for (SettingsElement settingsElement : elementList) {
					listY -= settingsElement.getEntryHeight() + 2;
					totalEntryHeight -= settingsElement.getEntryHeight() + 2;
					boolean dropDown = settingsElement instanceof DropDownElement;
					boolean colorPicker = settingsElement instanceof ColorPickerCheckBoxBulkElement;
					if (((!dropDown || colorPicker) && zLevel == 0) || ((dropDown || colorPicker) && zLevel == 1)) {
						int elementWidth = width / 2 - 14;
						int elementX = width / 2 + 4;
						settingsElement.draw(elementX, (int) listY, elementX + elementWidth,
								(int) (listY + settingsElement.getEntryHeight()), mouseX, mouseY);

						if (canRenderDescription && settingsElement instanceof DropDownElement) {
							canRenderDescription = !((DropDownElement) settingsElement).getDropDownMenu().isOpen();
						}

						if (!settingsElement.isMouseOver() || mouseY < 32 || mouseY > height - 58) continue;
						mouseOverElement = settingsElement;
					}
				}
				for (SettingsElement settingsElement : elementList) {
					listY += settingsElement.getEntryHeight() + 2;
					totalEntryHeight += settingsElement.getEntryHeight() + 2;
				}
				elementList = Lists.reverse(elementList);
			}

			glDisable(GL_SCISSOR_TEST);

			scrollbar.setEntryHeight(totalEntryHeight + stringHeight + 8);
			scrollbar.update(2);
			scrollbar.update(1);
			scrollbar.draw(mouseX, mouseY);

			// Draw description
			if (canRenderDescription) {
				Iterator elIt = elementList.iterator();
				while (elIt.hasNext()) {
					SettingsElement element = (SettingsElement) elIt.next();
					if (element.isMouseOver() && mouseY > 32 && mouseY < height - 58) {
						element.drawDescription(mouseX, mouseY, width);
					}
				}
			}

			// Draw tab example

			if (!list.selected.isCrashed()) {
				String tabString = "ApostrophPoint_Point";
				int tabX = width / 2 + 4 + 3;
				int tabY = height - 51 + 3;
				int tabWidth = width / 2 - 8 - 6;
				int tabHeight = 14;

				// Backy background
				drawRect(tabX - 3, tabY - 3, tabX + tabWidth + 3, tabY + tabHeight + 3, 0xA0000000);

				AutoStats.scissorBox(tabX, tabY, tabX + tabWidth, tabY + tabHeight);
				glEnable(GL_SCISSOR_TEST);

				// Background
				drawRect(tabX, tabY, tabX + tabWidth, tabY + tabHeight, 0x20FFFFFF);

				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				GlStateManager.enableAlpha();
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

				// Head
				mc.getTextureManager().bindTexture(TEXTURE_STEVE);
				Gui.drawScaledCustomSizeModalRect(tabX, tabY, 8, 8, 8, 8, 8, 8, 64, 64);

				// Name
				fontRendererObj.drawStringWithShadow(tabString, tabX + 9, tabY, 0xFFFFFFFF);

				int statsColor = extensionManager.getExampleColor(list.selected);

				// Font
				GlStateManager.pushMatrix();
				GlStateManager.translate(tabX, tabY + 8, 0);
				GlStateManager.scale(2d / 3, 2d / 3, 1);
				String text = autoStats.extensionManager.getExampleTabListText(list.selected);
				fontRendererObj.drawStringWithShadow(text, 1, 1, 0xFFFFFFFF);
				GlStateManager.popMatrix();

				// Gradient
				int left = tabX;
				int top = tabY + 8 + 3;
				int right = tabX + tabWidth;
				int bottom = tabY + tabHeight;
				AutoStats.drawGradientRect(0, left, top, right, bottom,
						(statsColor & 0x00FFFFFF) | 0x00000000 /* 0x00DFDFDF */,
						(statsColor & 0x00FFFFFF) | 0x80000000);

				glDisable(GL_SCISSOR_TEST);
			}
		}

		// Draw extension list
		list.drawScreen(mouseX, mouseY, partialTicks);

		String p = "";
		for (SettingsElement e : path.keySet()) {
			p = "\u00A7f/\u00A77" + e.getDisplayName();
		}

		// Draw title
		drawCenteredString(fontRendererObj, "AutoStats" + p, width / 2, 16, 0xFFFFFF);

		// Draw buttons
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	/** {@inheritDoc} */
	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		list.handleMouseInput();
		scrollbar.mouseInput();
	}

	/** {@inheritDoc} */
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (!button.enabled) return;

		if (button.id == 1 && list.selected != null) {
			// Blacklist
			long currentMS = System.currentTimeMillis();
			if (blacklistButton.displayString.equals("\u00A7c!Blacklist!")) {
				if (currentMS - lastBlacklistHit > 2000) {
					extensionManager.blacklist(list.selected);
					blacklistButton.displayString = "Blacklist";
				}
			} else {
				blacklistButton.displayString = "\u00A7c!Blacklist!";
			}
			lastBlacklistHit = currentMS;
		} else if (button.id == 2) {
			// View folder
			File dir = new File("AutoStats");
			if (dir.exists()) Desktop.getDesktop().open(dir);
		} else if (button.id == 3) {
			// Reload
			extensionManager.reloadExtensions();
			extensionManager.refresh();
			list.extensions = listExtensions();
			list.selected = null;
			blacklistButton.displayString = "Blacklist";
			blacklistButton.enabled = false;
		} else if (button.id == 4) {
			// Done
			mc.displayGuiScreen(parentScreen);
		} else if (button.id == 5) {
			// AutoStats discord
			try {
				Desktop.getDesktop().browse(new URL("https://discord.gg/UDGV5mh").toURI());
			} catch (Exception e) {
			}
		} else if (button.id == 6) {
			if (path.size() > 0) {
				SettingsElement first = path.keySet().toArray(new SettingsElement[0])[path.size() - 1];
				double scrollY = path.get(first);
				path.remove(first);
				if (path.size() > 0) {
					elementList.clear();
					elementList.addAll(path.keySet().toArray(new SettingsElement[0])[path.size() - 1].getSubSettings()
							.getElements());
				} else {
					elementList.clear();
					extensionManager.fillSettings(list.selected, elementList);
				}
				scrollbar.setScrollY(scrollY);
				return;
			}
		} else if (button.id == 7 || button.id == 8) {
			list.actionPerformed(button);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) {
			if (path.size() > 0) {
				SettingsElement first = path.keySet().toArray(new SettingsElement[0])[path.size() - 1];
				double scrollY = path.get(first);
				path.remove(first);
				if (path.size() == 0) {
					elementList.clear();
					extensionManager.fillSettings(list.selected, elementList);
				} else {
					elementList.clear();
					elementList.addAll(path.keySet().toArray(new SettingsElement[0])[path.size() - 1].getSubSettings()
							.getElements());
				}
				scrollbar.setScrollY(scrollY);
				return;
			}

			// Escape
			mc.displayGuiScreen(parentScreen);
		}

		for (SettingsElement e : elementList) {
			e.keyTyped(typedChar, keyCode);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		scrollbar.mouseAction(mouseX, mouseY, Scrollbar.EnumMouseAction.CLICKED);
		for (SettingsElement el : elementList) {
			el.mouseClicked(mouseX, mouseY, mouseButton);
		}
		if (mouseOverElement != null) {
			if (mouseOverElement instanceof ControlElement) {
				ControlElement element = (ControlElement) mouseOverElement;
				if (element.hasSubList() && element.getButtonAdvanced().isMouseOver()
						&& element.getButtonAdvanced().enabled) {
					element.getButtonAdvanced().playPressSound(mc.getSoundHandler());
					path.put(element, scrollbar.getScrollY());
					elementList.clear();
					elementList.addAll(element.getSubSettings().getElements());
					scrollbar.setScrollY(0);
				}
			}
		}

	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
		scrollbar.mouseAction(mouseX, mouseY, Scrollbar.EnumMouseAction.DRAGGING);
		for (SettingsElement el : elementList) {
			el.mouseClickMove(mouseX, mouseY, clickedMouseButton);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		scrollbar.mouseAction(mouseX, mouseY, Scrollbar.EnumMouseAction.RELEASED);
		for (SettingsElement el : elementList) {
			el.mouseRelease(mouseX, mouseY, state);
		}
	}

	/** The list of extensions */
	class ExtensionList extends GuiSlot {

		List<Extension> extensions = listExtensions();
		Extension selected;

		/** Sets its size itself */
		ExtensionList() {
			super(Minecraft.getMinecraft(), AutoStatsGuiScreen.this.width / 2 - 4 - 2, AutoStatsGuiScreen.this.height,
					32, AutoStatsGuiScreen.this.height - 58, 18);
			this.left = 4;
		}

		/** {@inheritDoc} */
		@Override
		public void setDimensions(int widthIn, int heightIn, int topIn, int bottomIn) {
			super.setDimensions(widthIn, heightIn, topIn, bottomIn);
			this.left = 4;
		}

		/** {@inheritDoc} */
		@Override
		protected void drawSlot(int index, int p_180791_2_, int y, int p_180791_4_, int mouseXIn, int mouseYIn) {
			Extension e = extensions.get(index);
			drawCenteredString(fontRendererObj,
					(e == extensionManager.getCurrent() ? "\u00A7e" : "") + e.name, width / 2,
					y + 1, 0xC0C0C0);
		}

		/** {@inheritDoc} */
		@Override
		protected void drawBackground() {
		}

		/** {@inheritDoc} */
		@Override
		protected void elementClicked(int index, boolean isDoubleClick, int mouseX, int mouseY) {
			path.clear();

			Extension extension = extensions.get(index);
			blacklistButton.displayString = "Blacklist";
			blacklistButton.enabled = extension != null && !extensionManager.isBlackListed(extension);
			elementList.clear();
			extensionManager.fillSettings(extension, elementList);
			selected = extension;
		}

		/** {@inheritDoc} */
		@Override
		protected int getSize() {
			return extensions.size();
		}

		/** {@inheritDoc} */
		@Override
		protected int getContentHeight() {
			return getSize() * 18;
		}

		/** {@inheritDoc} */
		@Override
		protected boolean isSelected(int index) {
			Extension extension = extensions.get(index);
			return selected == extension;
		}
	}

	// Sorts after name
	private List<Extension> listExtensions() {
		List<Extension> extensionList = extensionManager.getExtensionList();
		extensionList = new ArrayList<Extension>(extensionList);
		extensionList.sort((e1, e2) -> e1.name.compareTo(e2.name));
		return extensionList;
	}

}