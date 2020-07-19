package com.lpn.autostats;

import static org.lwjgl.opengl.GL11.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.lpn.autostats.extension.ExtensionManager;
import com.lpn.autostats.extension.StateInfo;
import com.lpn.autostats.render.AutoStatsGuiChat;
import com.lpn.autostats.render.AutoStatsRenderPlayerImplementation;
import com.lpn.autostats.render.AutoStatsTabList;

import net.labymod.api.LabyModAddon;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.core.CoreAdapter;
import net.labymod.core.ForgeAdapter;
import net.labymod.core.LabyModCore;
import net.labymod.core.MappingAdapter;
import net.labymod.core.MathAdapter;
import net.labymod.core.MinecraftAdapter;
import net.labymod.core.ProtocolAdapter;
import net.labymod.core.RenderAdapter;
import net.labymod.core.RenderPlayerAdapter;
import net.labymod.core.ServerPingerAdapter;
import net.labymod.core.SoundAdapter;
import net.labymod.core.WorldRendererAdapter;
import net.labymod.ingamechat.GuiChatCustom;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.SliderElement;
import net.labymod.utils.Material;
import net.labymod.utils.ServerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * AutoStats main class
 * 
 * @author Sirvierl0ffel
 */
public class AutoStats extends LabyModAddon {

	private static AutoStats instance;

	private final Logger logger = LogManager.getLogger("AutoStats");

	// Sets the instance at first
	{
		// Create AutoStats folder and fill it with default extensions, if needed
		File dir = new File("AutoStats");
		if (!dir.exists()) {
			dir.mkdir();
			try {
				List<Byte> byteList = new ArrayList<>();
				InputStream inputStream = getClass().getResourceAsStream("/GommeHD.jar");
				int b;
				while ((b = inputStream.read()) != -1) {
					byteList.add((byte) b);
				}
				byte[] bytes = new byte[byteList.size()];
				for (int i = 0; i < byteList.size(); i++) {
					bytes[i] = byteList.get(i);
				}
				File defFile = new File(dir, "GommeHD.jar");
				defFile.createNewFile();
				Files.write(defFile.toPath(), bytes);
			} catch (IOException e) {
				logger.error("Could not load default extensions!");
				e.printStackTrace();
			}
		}

		if (instance != null) throw new IllegalStateException("Can only create one instance of AutoStats?!");
		instance = this;
	}

	/** True, if the command button was pressed */
	public boolean opened;
	private boolean hinted = false;

	private boolean enabled = true;
	private boolean nameTags = true;
	private boolean colorEverything;
	private int gradientHeight = 10;
	private boolean tabList = true;
	private boolean tabListSorting = true;
	private boolean msgs = false;

	/** {@link ExtensionManager} */
	public final ExtensionManager extensionManager = new ExtensionManager();

	/** {@link StateInfo} */
	public final StateInfo stateInfo = new StateInfo();

	// The instance of the in game GUI, which got its tab list instance replaced
	private GuiIngame lastIngameGUI = null;

	/** Initializes AutoStats */
	@Override
	public void onEnable() {
		// Register events
		extensionManager.init();
		stateInfo.init();

		// Change RenderPlayerImplementation
		CoreAdapter old = LabyModCore.getCoreAdapter();
		LabyModCore.setCoreAdapter(new CoreAdapter() {
			public WorldRendererAdapter getWorldRendererImplementation() {
				return old.getWorldRendererImplementation();
			}

			public SoundAdapter getSoundImplementation() {
				return old.getSoundImplementation();
			}

			public ServerPingerAdapter getServerPingerImplementation() {
				return old.getServerPingerImplementation();
			}

			public RenderPlayerAdapter getRenderPlayerImplementation() {
				return new AutoStatsRenderPlayerImplementation();
			}

			public RenderAdapter getRenderImplementation() {
				return old.getRenderImplementation();
			}

			public MinecraftAdapter getMinecraftImplementation() {
				return old.getMinecraftImplementation();
			}

			public MathAdapter getMathImplementation() {
				return old.getMathImplementation();
			}

			public MappingAdapter getMappingAdapter() {
				return old.getMappingAdapter();
			}

			public ForgeAdapter getForgeImplementation() {
				return old.getForgeImplementation();
			}

			public ProtocolAdapter getProtocolAdapter() {
				return old.getProtocolAdapter();
			}
		});
		logger.info("Replaced LabyMod core adapter!");

		getApi().registerForgeListener(this);
		getApi().getEventManager().register((MessageSendEvent) this::onSend);
		getApi().getEventManager().registerOnJoin(this::onJoin);

		getApi().registerModule(new AutoStatsModule());

		loadConfig();
	}

	private boolean onSend(String msg) {
		// Copy state info map to clipboard
		if (msg.toLowerCase().startsWith("/copysas")) {
			String text = "";
			for (Entry<String, Object> state : stateInfo.map.entrySet()) {
				String key = state.getKey();
				String value = StringEscapeUtils.escapeJava(state.getValue().toString());
				text += key + ": " + value + System.lineSeparator();
			}
			GuiScreen.setClipboardString(text);
			msg("Copied state!");
			return true;
		} else if (msg.toLowerCase().startsWith("/continueas")) {
			if (!isEnabled()) {
				msg("\u00A7cAutoStats is not enabled!");
			} else if (!extensionManager.isStopped()) {
				msg("\u00A7cAutoStats is not stopped!");
			} else {
				extensionManager.setStopped(false);
				msg("Continueing!");
			}
			return true;
		}

		return false;
	}

	private void onJoin(ServerData server) {
		// Print new install message
		if (!opened) {
			msg("Thank you for downloading this addon. Click the command block button in your chat"
					+ (hinted ? ", or AutoStats will delete System32." : "."));
			hinted = true;
		}
	}

	/**
	 * Fires, when a {@link GuiScreen} gets displayed, edits the in game GUI, to use
	 * the AutoStats tab list and replaces the default chat GUI, to show the
	 * AutoStats button opening the AutoStats GUI
	 * 
	 * @param evt
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onOpen(GuiOpenEvent evt) {
		Minecraft mc = Minecraft.getMinecraft();

		if (evt.gui == null) return;

		// Replace chat GUI with own to add button (AutoStats button only displays when
		// no other chat tab is open, because ... difficulties with class transformers)
		if (evt.gui.getClass() == GuiChatCustom.class) {
			String text = "";
			try {
				Field defTextField = GuiChatCustom.class.getDeclaredField("defaultText");
				defTextField.setAccessible(true);
				text = (String) defTextField.get(evt.gui);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			evt.gui = new AutoStatsGuiChat(text);
			return;
		}

		// Set tab list field to AutoStatsTabList
		if (mc.ingameGUI == lastIngameGUI) return;
		lastIngameGUI = mc.ingameGUI;
		try {
			Class<?> clazz = lastIngameGUI.getClass();
			Field tabField = null;
			for (Field field : clazz.getDeclaredFields()) {
				if (GuiPlayerTabOverlay.class.isAssignableFrom(field.getType())) {
					tabField = field;
					break;
				}
			}
			tabField.setAccessible(true);
			Field modField = Field.class.getDeclaredField("modifiers");
			modField.setAccessible(true);
			modField.setInt(tabField, tabField.getModifiers() & ~Modifier.FINAL);
			tabField.set(lastIngameGUI, new AutoStatsTabList(mc, lastIngameGUI));
		} catch (Throwable t) {
			logger.error("Failed to replace tab list!");
			t.printStackTrace();
		}
	}

	/** Loads configuration */
	@Override
	public void loadConfig() {
		JsonObject config = getConfig();
		if (config == null) return;

		if (config.has("enabled")) enabled = config.get("enabled").getAsBoolean();
		if (config.has("nameTags")) nameTags = config.get("nameTags").getAsBoolean();
		if (config.has("colorEverything")) colorEverything = config.get("colorEverything").getAsBoolean();
		if (config.has("gradientHeight")) gradientHeight = config.get("gradientHeight").getAsInt();
		if (config.has("tabList")) tabList = config.get("tabList").getAsBoolean();
		if (config.has("tabListSorting")) tabListSorting = config.get("tabListSorting").getAsBoolean();
		if (config.has("opened")) opened = true;
		if (config.has("msgs")) msgs = config.get("msgs").getAsBoolean();

		extensionManager.loadConfig();
	}

	/** Fills settings GUI */
	@Override
	protected void fillSettings(List<SettingsElement> settings) {
		settings.add(new BooleanElement("Enabled", this, new IconData(Material.LEVER), "enabled", enabled));
		settings.add(new BooleanElement("Name Tags", this, new IconData(Material.LEVER), "nameTags", nameTags));
		settings.add(new BooleanElement("Color Everything", this, new IconData(Material.LEVER), "colorEverything",
				colorEverything));
		settings.add(new SliderElement("Gradient Height", this, new IconData(Material.GLASS), "gradientHeight",
				gradientHeight).setRange(0, 15));
		settings.add(new BooleanElement("Tab List", this, new IconData(Material.LEVER), "tabList", tabList));
		settings.add(new BooleanElement("Tab List Sorting", this, new IconData(Material.LEVER), "tabListSorting",
				tabListSorting));
		settings.add(new BooleanElement("Msgs", this, new IconData(Material.LEVER), "msgs", msgs));
		settings.get(1).setDescriptionText("When enabled the name tags are colored in the players skill level.");
		settings.get(3).setDescriptionText("The height of the gradient over the name tags.");
		settings.get(2).setDescriptionText("When enabled the whole nametag is colored, otherwise it is a gradient.");
		settings.get(4).setDescriptionText("The tab list is only available for the 1.8 tab list.");
		settings.get(5).setDescriptionText("Sorts the tab list after stats.");
		settings.get(6).setDescriptionText("Updates you via chat which extension is in use!");
	}

	/**
	 * Maps number to a color code
	 * 
	 * @param number
	 * @param minNumber
	 * @param maxNumber
	 * @return a color char mapped between 6 green red codes
	 */
	public static char mapToGreenRed(float number, float minNumber, float maxNumber) {
		char[] greenRed = new char[] { '2', 'a', 'e', '6', 'c', '4' };
		if (number == -1) return '7';
		if (Float.isNaN(number)) return '3';
		float index = greenRed.length * (number - minNumber) / (maxNumber - minNumber);
		return greenRed[Math.max(0, Math.min(greenRed.length - 1, (int) index))];
	}

	/**
	 * Method from {@link GuiUtils}, which is not supported in vanilla
	 * 
	 * @param zLevel
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @param startColor
	 * @param endColor
	 */
	public static void drawGradientRect(float zLevel, int left, int top, int right, int bottom, int startColor,
			int endColor) {
		float startAlpha = (float) (startColor >> 24 & 255) / 255.0F;
		float startRed = (float) (startColor >> 16 & 255) / 255.0F;
		float startGreen = (float) (startColor >> 8 & 255) / 255.0F;
		float startBlue = (float) (startColor & 255) / 255.0F;
		float endAlpha = (float) (endColor >> 24 & 255) / 255.0F;
		float endRed = (float) (endColor >> 16 & 255) / 255.0F;
		float endGreen = (float) (endColor >> 8 & 255) / 255.0F;
		float endBlue = (float) (endColor & 255) / 255.0F;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.shadeModel(7425);
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		worldrenderer.pos(right, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		worldrenderer.pos(left, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		worldrenderer.pos(left, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();
		worldrenderer.pos(right, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();
		tessellator.draw();
		GlStateManager.shadeModel(7424);
//		GlStateManager.disableBlend();
//		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
	}

	/**
	 * Scissors method I copied by myself from Wurst
	 * 
	 * @param x
	 * @param y
	 * @param xend
	 * @param yend
	 */
	public static void scissorBox(int x, int y, int xend, int yend) {
		int width = xend - x;
		int height = yend - y;
		ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
		int factor = sr.getScaleFactor();
		int bottomY = sr.getScaledHeight() - yend;
		glScissor(x * factor, bottomY * factor, width * factor, height * factor);
	}

	// Debug method to load GUI screen
//	public static GuiScreen loadTest(GuiScreen prev) {
//		File dir = new File("AutoStats");
//		if (!dir.exists()) dir.mkdir();
//		File[] jarFiles = dir.listFiles(p -> p.getName().endsWith(".jar"));
//		for (int i = 0; i < jarFiles.length; i++) {
//			ArrayList<Class<Extension>> list = new ArrayList<>();
//			String jarFilePath = jarFiles[i].getAbsolutePath();
//			try {
//				URL[] urls = { jarFiles[i].toURI().toURL() };
//				URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
//				JarFile jarFile = new JarFile(jarFiles[i]);
//				Enumeration<JarEntry> jarEntrys = jarFile.entries();
//				while (jarEntrys.hasMoreElements()) {
//					JarEntry je = jarEntrys.nextElement();
//					if (je.isDirectory() || !je.getName().endsWith(".class")) continue;
//					String className = je.getName().substring(0, je.getName().length() - 6);
//					className = className.replace('/', '.');
//					Class<?> loaded = classLoader.loadClass(className);
//					if (loaded.getName().equals("com.lpn.autostats.AutoStatsTestGuiScreen")) {
//						return (GuiScreen) loaded.getConstructor(GuiScreen.class).newInstance(prev);
//					}
//				}
//				classLoader.close();
//				jarFile.close();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		return null;
//	}

	/**
	 * Sends the player a chat message with the AutoStats prefix
	 * 
	 * @param msg
	 */
	public static void msg(String msg) {
		LabyMod.getInstance().displayMessageInChat("\u00A72[\u00A7aAutoStats\u00A72]\u00A7e " + msg);
	}

	/**
	 * @throws IllegalStateException if AutoStats was not enabled yet
	 * 
	 * @return the AutoStats instance
	 */
	public static AutoStats instance() {
		if (instance == null) throw new IllegalStateException("Not initialized!");
		return instance;
	}

	/** @return true, if an extension can and should be used */
	public boolean isUsable() {
		return isEnabled() && extensionManager.isSupported() && !extensionManager.isStopped();
	}

	/** @return true, if AutoStats is enabled */
	public boolean isEnabled() {
		return enabled;
	}

	/** @return true, if AutoStats name tags are enabled */
	public boolean isNameTagsEnabled() {
		return nameTags;
	}

	/** @return true, if AutoStats tab list is enabled */
	public boolean isTabListEnabled() {
		return tabList;
	}

	/** @return true, if AutoStats tab list sorting is enabled */
	public boolean isTabListSortingEnabled() {
		return tabListSorting;
	}

	/** @return the height of the gradient on the name tags */
	public int getGradientHeight() {
		return gradientHeight;
	}

	/** @return if the hole name tag should be colored **/
	public boolean isColorEverythingEnabled() {
		return colorEverything;
	}

	/**
	 * @return true if someone wants to be updated via chat which extension is used
	 */
	public boolean isMsgsEnabled() {
		return msgs;
	}

}
