package com.lpn.autostats.extension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lpn.autostats.AutoStats;

import net.labymod.api.EventManager;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.core.LabyModCore;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.SettingsElement;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

/**
 * Loads extensions and executes their methods safely
 * 
 * @author Sirvierl0ffel
 */

public class ExtensionManager {

	private List<Extension> addonExtensions = new ArrayList<Extension>();

	private final Logger logger = LogManager.getLogger("AutoStats");
	private final AutoStats autoStats;
	private final Map<String, Long> blacklist = new HashMap<>();
	private final List<Extension> extensionList = new ArrayList<>();

	private Extension current;
	private boolean stopped;

	/** Just creates {@link ExtensionManager} instance */
	public ExtensionManager() {
		autoStats = AutoStats.instance();
	}

	/** Registers event listeners */
	public void init() {
		autoStats.getApi().registerForgeListener(this);
		EventManager eventManager = autoStats.getApi().getEventManager();
		eventManager.register((MessageReceiveEvent) this::onReceive);
		eventManager.register((MessageSendEvent) this::onSend);
		reloadExtensions();
	}

	/**
	 * Loads the attributes of the extensions from the LabyModAddon addon
	 * configuration
	 * 
	 * @param config
	 */
	public void loadConfig() {
		JsonObject config = autoStats.getConfig();

		if (config == null) return;

		if (!config.has("extensionConfigs")) return;

		JsonArray extensionConfigs = config.get("extensionConfigs").getAsJsonArray();
		JsonArray newExtensionConfigs = new JsonArray();

		for (JsonElement element : extensionConfigs) {
			JsonObject extensionConfig = element.getAsJsonObject();
			String clazz = extensionConfig.get("class").getAsString();
			long date = extensionConfig.get("date").getAsLong();

			for (Extension extension : extensionList) {
				// Find and ensure up to date
				if (!clazz.equals(extension.getClass().getName())) continue;
				if (extension.incompatible.contains(date)) continue;

				// Load configuration
				try {
					extension.extensionConfig = extensionConfig;
					extension.loadConfig();
				} catch (Throwable t) {
					onCrash(extension, t);
				}

				newExtensionConfigs.add(element);
				break;
			}
		}

		// Only keep found up to date configurations
		config.add("extensionConfigs", newExtensionConfigs);
	}

	/**
	 * Gets executed every frame
	 * 
	 * @param tickEvent
	 */
	@SubscribeEvent
	public void onTick(ClientTickEvent tickEvent) {
		for (Extension extension : extensionList) {
			if (!extension.crashed) try {
				extension.onTick();
			} catch (Throwable t) {
				onCrash(extension, t);
			}
		}
	}

	// Executed, when the user sends a message
	private boolean onSend(String msg) {
		boolean cancel = false;
		for (Extension extension : extensionList) {
			if (!extension.crashed) try {
				cancel = cancel || extension.onSend(msg);
			} catch (Throwable t) {
				onCrash(extension, t);
			}
		}
		return cancel;
	}

	// Executed, when the server sends a message
	private boolean onReceive(String unformatted, String formatted) {
		boolean cancel = false;
		for (Extension extension : extensionList) {
			if (!extension.crashed) try {
				cancel = cancel || extension.onReceive(unformatted, formatted);
			} catch (Throwable t) {
				onCrash(extension, t);
			}
		}
		return cancel;
	}

	// Executed, when an extension causes an exception
	private void onCrash(Extension extension, Throwable t) {
		t.printStackTrace();
		extension.crashed = true;
		AutoStats.msg("\u00A7c" + extension.name + " crashed! See log for details!");
		if (extension == current) refresh();
	}

	/** Loads extensions from AutoStats folder */
	public void reloadExtensions() {
		// Load blacklist
		blacklist.clear();
		File file = new File("AutoStats/blacklist.txt");
		if (file.exists()) try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(" ");
				blacklist.put(split[0], Long.valueOf(split[1]));
			}
			br.close();
		} catch (Throwable t) {
			file.delete();
			logger.error("Could not load AutoStats blacklist!");
			t.printStackTrace();
		}

		// Reset
		addonExtensions.forEach(e -> e.crashed = false);
		current = null;
		extensionList.clear();
		extensionList.addAll(addonExtensions);

		// Load extensions from AutoStats folder

		File dir = new File("AutoStats");
		if (!dir.exists()) dir.mkdir();

		logger.info("Searching for extensions in \"" + dir.getAbsolutePath() + "\"...");

		File[] jarFiles = dir.listFiles(p -> p.getName().endsWith(".jar"));

		for (int i = 0; i < jarFiles.length; i++) {
			// Found extensions in this jar
			ArrayList<Class<Extension>> list = new ArrayList<>();

			String jarFilePath = jarFiles[i].getAbsolutePath();

			try {
				// Create class loader and jar file
				URL[] urls = { jarFiles[i].toURI().toURL() };
				URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
				JarFile jarFile = new JarFile(jarFiles[i]);
				Enumeration<JarEntry> jarEntrys = jarFile.entries();

				// Loop through files in jar
				jarLoop: while (jarEntrys.hasMoreElements()) {
					// Load current file if it is a class
					JarEntry je = jarEntrys.nextElement();
					if (je.isDirectory() || !je.getName().endsWith(".class")) continue;
					String className = je.getName().substring(0, je.getName().length() - 6);
					className = className.replace('/', '.');
					Class<?> loaded = classLoader.loadClass(className);

					// Add class if it is an extension
					if (Extension.class.isAssignableFrom(loaded) && !Modifier.isAbstract(loaded.getModifiers())) {
						// Find constructor
						List<Constructor> all = new ArrayList<>();
						all.addAll(Arrays.asList(loaded.getConstructors()));
						for (Constructor constructor : all) {
							boolean zeroParameter = constructor.getParameterCount() == 0;
							if (zeroParameter) {
								list.add((Class<Extension>) loaded);
								continue jarLoop;
							}
						}

						logger.error(loaded.getName() + " in \"" + jarFilePath
								+ "\" has no zero-parameter and accessible constructor.");
					}
				}

				// Create every extension instance
				for (Class<Extension> clazz : list) {
					// Try to find existing extension
					Extension existing = null;
					for (Extension e : extensionList) {
						if (e.getClass().getName().equalsIgnoreCase(clazz.getName())) {
							existing = e;
							continue;
						}
					}

					// Create extension
					Extension adding = clazz.newInstance();

					// Check if blacklisted
					Long outdatedVersion = blacklist.get(adding.getClass().getName());
					if (outdatedVersion == null) outdatedVersion = 0L;
					if (adding.date == outdatedVersion) {
						continue;
					}

					adding.file = jarFilePath;

					if (existing != null) {
						// Remove existing class, if out-dated, don't add new one if not
						if (existing.date < adding.date) {
							extensionList.remove(existing);
						} else {
							continue;
						}
					}

					extensionList.add(adding);
					logger.info("Added " + clazz.getName() + " to extensions!");
				}

				if (list.isEmpty()) logger.warn("\"" + jarFilePath + "\" has no registered extensions.");

				classLoader.close();
				jarFile.close();
			} catch (Throwable t) {
				logger.error("Error processing \"" + jarFilePath + "\"!");
				t.printStackTrace();
			}
		}

		// Sort by date so the most recently coded extensions get picked first if two
		// extensions have the same target
		Collections.sort(extensionList);

		loadConfig();
	}

	/**
	 * Adds an extension from another LabyMod addon
	 * 
	 * @param addon
	 * @param extension
	 */
	public void addExtension(LabyModAddon addon, Extension extension) {
		extensionList.add(extension);
		addonExtensions.add(extension);
		extension.file = "From Addon " + addon.about.name;
	}

	/** Searches for the newest extension supporting the current server */
	public void refresh() {
		stopped = false;
		for (Extension extension : extensionList) {
			if (!extension.isSupported() || extension.crashed || isBlackListed(extension)) continue;
			if (current == extension) return;
			// refresh() gets still called, when AutoStats is not enabled
			if (autoStats.isEnabled() && autoStats.isMsgsEnabled())
				AutoStats.msg("Now using " + extension.name + " by " + extension.author + "!");
			if (current != null) current.onDeselect();
			current = extension;
			extension.onSelect();
			return;
		}
		if (current != null && autoStats.isEnabled() && autoStats.isMsgsEnabled()) AutoStats.msg("Not supported!");
		current = null;
	}

	/**
	 * Adds this extension to the blacklist, refreshes and saves the blacklist
	 * 
	 * @param extension
	 */
	public void blacklist(Extension extension) {
		blacklist.put(extension.getClass().getName(), extension.date);

		refresh();

		// Save blacklist
		File dir = new File("AutoStats");
		if (!dir.exists()) dir.mkdir();
		File file = new File(dir, "blacklist.txt");
		try {
			if (!file.exists()) file.createNewFile();
			PrintWriter pw = new PrintWriter(new FileOutputStream(file), true);
			for (Entry<String, Long> entry : blacklist.entrySet()) {
				pw.println(entry.getKey() + " " + entry.getValue());
			}
			pw.close();
		} catch (IOException e) {
			logger.error("Could not save extension blacklist!");
			e.printStackTrace();
		}
	}

	/**
	 * @param extension
	 * @return true, if this extension is blacklisted
	 */
	public boolean isBlackListed(Extension extension) {
		return blacklist.containsKey(extension.getClass().getName());
	}

	/**
	 * Adds the settings shown in the AutoStats screen
	 * 
	 * @param elementList
	 */
	public void fillSettings(Extension extension, List<SettingsElement> list) {
		if (extension == null) return;

		if (!extension.crashed) try {
			extension.fillSettings(list);
			list.forEach(this::enableSubSettings);
		} catch (Throwable t) {
			onCrash(extension, t);
		}
	}

	private void enableSubSettings(SettingsElement element) {
		if (element instanceof ControlElement) ((ControlElement) element).setSettingEnabled(true);
		element.getSubSettings().getElements().forEach(this::enableSubSettings);
	}

	/**
	 * @param playerName
	 * @return the color representing the skill level of this player
	 */
	public int getColor(String playerName) {
		if (!isSupported()) return 0x00000000;

		try {
			return LabyModCore.getMinecraft().getFontRenderer().getColorCode(current.getColorChar(playerName));
		} catch (Throwable t) {
			onCrash(current, t);
			return 0x00000000;
		}
	}

	/**
	 * Compares two players for the tab list sorting
	 * 
	 * @param playerName1
	 * @param playerName2
	 * @return the comparing result
	 */
	public int comparePlayer(String playerName1, String playerName2) {
		if (!isSupported()) return 0;

		try {
			return current.comparePlayer(playerName1, playerName2);
		} catch (Throwable t) {
			onCrash(current, t);
			return 0;
		}
	}

	/**
	 * @param playerName
	 * @return the string to render in the tab list under the name of this player
	 */
	public String getTabListText(String playerName) {
		if (!isSupported()) return "";

		try {
			return current.getTabListText(playerName);
		} catch (Throwable t) {
			onCrash(current, t);
			return "";
		}
	}

	/** @return the number of seconds until all statistics are done */
	public float getFinishedIn() {
		if (!isSupported()) return 0;

		try {
			return current.getFinishedIn();
		} catch (Throwable t) {
			onCrash(current, t);
			return 0;
		}
	}

	/** @return the example gradient color shown in the AutoStats GUI */
	public int getExampleColor(Extension extension) {
		try {
			return LabyModCore.getMinecraft().getFontRenderer().getColorCode(extension.getExampleColorChar());
		} catch (Throwable t) {
			onCrash(extension, t);
			return 0;
		}
	}

	/**
	 * @return the example tab list text for a player, shown in the AutoStats GUI
	 */
	public String getExampleTabListText(Extension extension) {
		try {
			return extension.getExampleTabListText();
		} catch (Throwable t) {
			onCrash(extension, t);
			return "";
		}
	}

	/** @return true, if an extension for the current server could be found */
	public boolean isSupported() {
		return current != null;
	}

	/** Stops the AutoStats process */
	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}

	/** @return true, if the current extension failed */
	public boolean isStopped() {
		return stopped;
	}

	/** @return all loaded extensions */
	public List<Extension> getExtensionList() {
		return extensionList;
	}

	/** @return the currently used extension */
	public Extension getCurrent() {
		return current;
	}

}
