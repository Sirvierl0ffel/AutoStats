package com.lpn.autostats.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import net.labymod.api.events.MessageSendEvent;
import net.labymod.api.permissions.Permissions;
import net.labymod.core.LabyModCore;
import net.labymod.ingamechat.GuiChatCustom;
import net.labymod.ingamechat.IngameChatManager;
import net.labymod.ingamechat.namehistory.NameHistory;
import net.labymod.ingamechat.namehistory.NameHistoryUtil;
import net.labymod.ingamechat.renderer.EnumMouseAction;
import net.labymod.ingamechat.tabs.GuiChatAutoText;
import net.labymod.ingamechat.tabs.GuiChatFilter;
import net.labymod.ingamechat.tabs.GuiChatNameHistory;
import net.labymod.ingamechat.tabs.GuiChatPlayerMenu;
import net.labymod.ingamechat.tabs.GuiChatShortcuts;
import net.labymod.ingamechat.tabs.GuiChatSymbols;
import net.labymod.ingamechat.tools.shortcuts.Shortcuts;
import net.labymod.ingamegui.Module;
import net.labymod.ingamegui.ModuleGui;
import net.labymod.main.LabyMod;
import net.labymod.main.ModTextures;
import net.labymod.main.lang.LanguageManager;
import net.labymod.settings.LabyModModuleEditorGui;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.utils.Material;
import net.labymod.utils.ModColor;
import net.labymod.utils.ModUtils;
import net.labymod.utils.UUIDFetcher;
import net.labymod.utils.manager.TooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

/**
 * Replaces the chat GUI entirely, because I am lazy
 * 
 * @author Sirvierl0ffel
 */

public class AutoStatsGuiChat extends GuiChat {

	private static final ModuleGui moduleGui = GuiChatCustom.getModuleGui();
	private ChatButton[] chatButtons;
	private String defaultText;
	private IngameChatManager ingameChatManager;

	/**
	 * Just creates {@link AutoStatsGuiChat} instance
	 * 
	 * @param defaultText
	 */
	public AutoStatsGuiChat(final String defaultText) {
		super(defaultText);
		this.ingameChatManager = LabyMod.getInstance().getIngameChatManager();
		this.defaultText = defaultText;
	}

	/** {@inheritDoc} */
	@Override
	public void initGui() {
		super.initGui();

		if (this.mc.currentScreen != null && this.mc.currentScreen.getClass() == AutoStatsGuiChat.class) {
			GuiChatCustom.activeTab = -1;
		}

		final boolean chatFeaturesAllowed = LabyMod.getInstance().getServerManager()
				.isAllowed(Permissions.Permission.CHAT);
		Module.setCurrentModuleGui(AutoStatsGuiChat.moduleGui);
		AutoStatsGuiChat.moduleGui.initGui();
		final List<ChatButton> chatButtonList = new ArrayList<ChatButton>();

		if (LabyMod.getSettings().chatSymbols) {
			chatButtonList
					.add(new ChatButton(0, "symbols", new IconData(ModTextures.CHAT_TAB_SYMBOLS), chatFeaturesAllowed));
		}
		if (LabyMod.getSettings().autoText) {
			chatButtonList.add(
					new ChatButton(1, "autotext", new IconData(ModTextures.CHAT_TAB_AUTOTEXT), chatFeaturesAllowed));
		}
		if (LabyMod.getSettings().chatShortcuts) {
			chatButtonList.add(
					new ChatButton(2, "shortcut", new IconData(ModTextures.CHAT_TAB_SHORTCUT), chatFeaturesAllowed));
		}
		if (LabyMod.getSettings().playerMenu && LabyMod.getSettings().playerMenuEditor) {
			chatButtonList.add(new ChatButton(3, "playermenu", new IconData(ModTextures.CHAT_TAB_PLAYERMENU),
					chatFeaturesAllowed));
		}
		if (LabyMod.getSettings().chatFilter) {
			chatButtonList.add(new ChatButton(4, "filter", new IconData(ModTextures.CHAT_TAB_FILTER), true));
		}
		if (LabyMod.getSettings().nameHistory) {
			chatButtonList.add(new ChatButton(5, "namehistory", new IconData(Material.BOOK_AND_QUILL), true));
		}
		if (LabyMod.getSettings().showModuleEditorShortcut) {
			chatButtonList.add(new ChatButton(6, "module_editor", new IconData(ModTextures.CHAT_TAB_GUI_EDITOR), true));
		}

		ChatButton autoStatsButton = new ChatButton(69, "", new IconData("textures/blocks/command_block.png"), true);
		autoStatsButton.displayName = "AutoStats";
		chatButtonList.add(autoStatsButton);

		chatButtonList.toArray(this.chatButtons = new ChatButton[chatButtonList.size()]);
		this.inputField.setText((this.defaultText == null) ? "" : this.defaultText);
	}

	/** {@inheritDoc} */
	@Override
	protected void actionPerformed(final GuiButton button) throws IOException {
		super.actionPerformed(button);
		switch (button.id) {
		case 4: {
			this.mc.displayGuiScreen((GuiScreen) new GuiChatNameHistory(this.inputField.getText()));
			break;
		}
		}
	}

	/** {@inheritDoc} */
	public void drawButtons(final int mouseX, final int mouseY, final float partialTicks) {
		int slot = 0;
		for (final ChatButton chatButton : this.chatButtons) {
			final boolean enabled = chatButton.isEnabled();
			final int x = this.width - 2 - 13 - slot * 14;
			final int y = this.height - 14;
			if (slot == GuiChatCustom.activeTab) {
				drawRect(x, y - 2, x + 13, y, Integer.MIN_VALUE);
			}
			drawRect(x, y, x + 13, y + 12, Integer.MIN_VALUE);
			final boolean hoverSymbols = mouseX >= x && mouseX < x + 13 && mouseY > y && mouseY < y + 12;
			if (chatButton.getIconData().hasMaterialIcon()) {
				GlStateManager.pushMatrix();
				final double scale = hoverSymbols ? 0.7 : 0.6;
				GlStateManager.scale(scale, scale, 1.0);
				LabyMod.getInstance().getDrawUtils().renderItemIntoGUI(chatButton.getItem(),
						(x + 5.5 - scale * 6.0) / scale, (y + 5 - scale * 6.0) / scale);
				GlStateManager.popMatrix();
			} else if (chatButton.getIconData().hasTextureIcon()) {
				Minecraft.getMinecraft().getTextureManager().bindTexture(chatButton.getIconData().getTextureIcon());
				LabyMod.getInstance().getDrawUtils().drawTexture((double) (x + 2 - (hoverSymbols ? 1 : 0)),
						(double) (y + 2 - (hoverSymbols ? 1 : 0)), 255.0, 255.0, hoverSymbols ? 11.0 : 9.0,
						hoverSymbols ? 11.0 : 9.0);
			}
			if (hoverSymbols) {
				TooltipHelper.getHelper().pointTooltip(mouseX, mouseY, 0L,
						enabled ? chatButton.getDisplayName()
								: LanguageManager.translate("ingame_chat_feature_not_allowed",
										new Object[] { chatButton.getDisplayName() }));
			}
			++slot;
		}
	}

	/** {@inheritDoc} */
	public void onButtonClick(final int mouseX, final int mouseY, final int mouseButton) {
		for (int slot = 0; slot < this.chatButtons.length; ++slot) {
			final ChatButton chatButton = this.chatButtons[slot];
			final boolean hoverSymbols = mouseX > this.width - 2 - 13 - slot * 14 && mouseX < this.width - 2 - slot * 14
					&& mouseY > this.height - 14 && mouseY < this.height - 2;
			if (hoverSymbols && chatButton.isEnabled()) {
				switch (chatButton.getId()) {
				case 0: {
					GuiChatCustom.activeTab = slot;
					this.mc.displayGuiScreen((GuiScreen) ((this.mc.currentScreen instanceof GuiChatSymbols)
							? new AutoStatsGuiChat(this.inputField.getText())
							: new GuiChatSymbols(this.inputField.getText())));
					break;
				}
				case 1: {
					GuiChatCustom.activeTab = slot;
					this.mc.displayGuiScreen((GuiScreen) ((this.mc.currentScreen instanceof GuiChatAutoText)
							? new AutoStatsGuiChat(this.inputField.getText())
							: new GuiChatAutoText(this.inputField.getText())));
					break;
				}
				case 2: {
					GuiChatCustom.activeTab = slot;
					this.mc.displayGuiScreen((GuiScreen) ((this.mc.currentScreen instanceof GuiChatShortcuts)
							? new AutoStatsGuiChat(this.inputField.getText())
							: new GuiChatShortcuts(this.inputField.getText())));
					break;
				}
				case 3: {
					GuiChatCustom.activeTab = slot;
					this.mc.displayGuiScreen((GuiScreen) ((this.mc.currentScreen instanceof GuiChatPlayerMenu)
							? new AutoStatsGuiChat(this.inputField.getText())
							: new GuiChatPlayerMenu(this.inputField.getText())));
					break;
				}
				case 4: {
					GuiChatCustom.activeTab = slot;
					this.mc.displayGuiScreen((GuiScreen) ((this.mc.currentScreen instanceof GuiChatFilter)
							? new AutoStatsGuiChat(this.inputField.getText())
							: new GuiChatFilter(this.inputField.getText())));
					break;
				}
				case 5: {
					GuiChatCustom.activeTab = slot;
					this.mc.displayGuiScreen((GuiScreen) ((this.mc.currentScreen instanceof GuiChatNameHistory)
							? new AutoStatsGuiChat(this.inputField.getText())
							: new GuiChatNameHistory(this.inputField.getText())));
					break;
				}
				case 6: {
					GuiChatCustom.activeTab = slot;
					this.mc.displayGuiScreen((GuiScreen) new LabyModModuleEditorGui(
							(GuiScreen) new AutoStatsGuiChat(this.inputField.getText())));
					break;
				}
				case 69: {
					this.mc.displayGuiScreen(new AutoStatsGuiScreen(new AutoStatsGuiChat(this.inputField.getText())));
					break;
				}
				}
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
		this.ingameChatManager.handleMouse(mouseX, mouseY, mouseButton, EnumMouseAction.CLICKED);
		super.mouseClicked(mouseX, mouseY, mouseButton);
		AutoStatsGuiChat.moduleGui.mouseClicked(mouseX, mouseY, mouseButton);
		this.onButtonClick(mouseX, mouseY, mouseButton);
		if (mouseButton == 1) {
			final String value = LabyModCore.getMinecraft().getClickEventValue(Mouse.getX(), Mouse.getY());
			if (value != null && value.startsWith("/msg ")) {
				final String name = value.replace("/msg ", "").replace(" ", "");
				if (!NameHistoryUtil.isInCache(name)) {
					NameHistoryUtil.getNameHistory(name);
				}
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void mouseReleased(final int mouseX, final int mouseY, final int state) {
		this.ingameChatManager.handleMouse(mouseX, mouseY, state, EnumMouseAction.RELEASED);
		super.mouseReleased(mouseX, mouseY, state);
		AutoStatsGuiChat.moduleGui.mouseReleased(mouseX, mouseY, state);
	}

	/** {@inheritDoc} */
	@Override
	protected void mouseClickMove(final int mouseX, final int mouseY, final int clickedMouseButton,
			final long timeSinceLastClick) {
		this.ingameChatManager.handleMouse(mouseX, mouseY, clickedMouseButton, EnumMouseAction.DRAGGING);
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
		AutoStatsGuiChat.moduleGui.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	/** {@inheritDoc} */
	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		AutoStatsGuiChat.moduleGui.handleMouseInput();
	}

	/** {@inheritDoc} */
	@Override
	protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
		AutoStatsGuiChat.moduleGui.keyTyped(typedChar, keyCode);
	}

	/** {@inheritDoc} */
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		AutoStatsGuiChat.moduleGui.onGuiClosed();
		Module.setCurrentModuleGui((ModuleGui) null);
	}

	/** {@inheritDoc} */
	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
		this.drawModifiedSuperScreen(mouseX, mouseY, partialTicks);
		this.ingameChatManager.handleMouse(mouseX, mouseY, -1, EnumMouseAction.RENDER);
		AutoStatsGuiChat.moduleGui.drawScreen(mouseX, mouseY, partialTicks);
		this.drawButtons(mouseX, mouseY, partialTicks);
		final String value = LabyModCore.getMinecraft().getClickEventValue(Mouse.getX(), Mouse.getY());
		if (LabyMod.getSettings().hoverNameHistory && value != null && value.startsWith("/msg ")) {
			final String name = value.replace("/msg ", "").replace(" ", "");
			if (NameHistoryUtil.isInCache(name)) {
				final NameHistory history = NameHistoryUtil.getNameHistory(name);
				final ArrayList<String> lines = new ArrayList<String>();
				boolean currentName = true;
				for (final UUIDFetcher change : history.getChanges()) {
					if (change.changedToAt != 0L) {
						final String date = ModUtils.getTimeDiff(change.changedToAt);
						String c = "7";
						if (currentName) {
							c = "6";
						}
						currentName = false;
						lines.add(ModColor.cl(c) + change.name + ModColor.cl("8") + " - " + ModColor.cl("8") + date);
					} else {
						lines.add(ModColor.cl("a") + change.name);
					}
				}
				this.drawHoveringText((List) lines, mouseX, mouseY);
				GlStateManager.disableLighting();
			} else {
				final ArrayList<String> lines2 = new ArrayList<String>();
				lines2.add(LanguageManager.translate("ingame_chat_rightclick_for_namechanges"));
				this.drawHoveringText((List) lines2, mouseX, mouseY);
				GlStateManager.disableLighting();
			}
		}
	}

	private void drawModifiedSuperScreen(final int mouseX, final int mouseY, final float partialTicks) {
		drawRect(2, this.height - 14, this.width - 2 - this.chatButtons.length * 14, this.height - 2,
				Integer.MIN_VALUE);
		this.inputField.drawTextBox();
		this.handleComponentHover(this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY()), mouseX,
				mouseY);
		for (int i = 0; i < this.buttonList.size(); ++i) {
			LabyModCore.getMinecraft().drawButton((GuiButton) this.buttonList.get(i), mouseX, mouseY);
		}
		for (int j = 0; j < this.labelList.size(); ++j) {
			this.labelList.get(j).drawLabel(this.mc, mouseX, mouseY);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void sendChatMessage(String msg, final boolean addToChat) {
		boolean cancelled = false;
		for (final Shortcuts.Shortcut shortcut : LabyMod.getInstance().getChatToolManager().getShortcuts()) {
			msg = msg.replace(shortcut.getShortcut(),
					String.format(shortcut.getReplacement(), LabyMod.getInstance().getPlayerName()));
		}
		for (final MessageSendEvent messageSend : LabyMod.getInstance().getEventManager().getMessageSend()) {
			if (messageSend.onSend(msg) && !cancelled) {
				cancelled = true;
			}
		}
		if (cancelled) {
			if (addToChat) {
				this.mc.ingameGUI.getChatGUI().addToSentMessages(msg);
			}
			return;
		}
		super.sendChatMessage(msg, addToChat);
	}

	private class ChatButton {

		private int id;
		private String displayName;
		private boolean enabled;
		private ItemStack item;
		private IconData iconData;

		public ChatButton(final int id, final String languageKey, final IconData iconData, final boolean enabled) {
			this.id = id;
			this.displayName = LanguageManager.translate("ingame_chat_tab_" + languageKey);
			this.item = (iconData.hasMaterialIcon() ? iconData.getMaterialIcon().createItemStack() : null);
			this.iconData = iconData;
			this.enabled = enabled;
		}

		public int getId() {
			return this.id;
		}

		public String getDisplayName() {
			return this.displayName;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public ItemStack getItem() {
			return this.item;
		}

		public IconData getIconData() {
			return this.iconData;
		}
	}
}