package com.lpn.autostats.extension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.lpn.autostats.AutoStats;

import net.labymod.gui.elements.ModTextField;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.NumberElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.SliderElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.ModColor;
import net.minecraft.client.Minecraft;

/**
 * Base AutoStats extension class that manages the statistics of a game. If you
 * create a newer version only it will be loaded, thanks to the all-mighty date
 * parameter in the constructor. You can keep the same class name for every
 * version. If two different classes can manage a game, the one with the latest
 * date will get chosen. The event methods are even called, if {@link #doUse()}
 * returns false
 * 
 * @author Sirvierl0ffel
 */

public abstract class Extension implements Comparable<Extension> {

	/** The date this version of the extension was created */
	public final long date;

	/** The name of the game this extension is meant for */
	public final String name;

	/** The author of this extension */
	public final String author;

	/**
	 * If a date is contained in this list, AutoStats will delete the extension
	 * configuration marked with it
	 */
	protected final List<Long> incompatible = new ArrayList<>();

	/** The AutoStats instance */
	protected final AutoStats autoStats;

	/** The ExtensionManager instance */
	protected final ExtensionManager extensionManager;

	/** The AutoStats logger instance */
	protected final Logger logger = LogManager.getLogger("AutoStats");

	/**
	 * The configuration of this extension, saved in the addon configuration, if
	 * updated {@link #loadConfig()} will be called
	 */
	protected JsonObject extensionConfig = new JsonObject();

	/** The jar file this extension is stored in */
	String file;

	/**
	 * When true this extension wont be considered for a server until
	 * {@link ExtensionManager#reloadExtensions()} gets called, will be set when
	 * this extension causes an exception
	 */
	boolean crashed;

	private final Identifier[] identifierList;

	/**
	 * Needs to be called in a constructor with no arguments, so
	 * {@link ExtensionManager#reloadExtensions()} can invoke it, or in an abstract
	 * class
	 * 
	 * @param date        the date this version of the extension was created
	 * @param name        the name of the game this extension is made for
	 * @param author      the author of this extension
	 * @param identifiers the identifiers that indicate, this extension supports
	 *                    this server, all have to be true
	 */
	public Extension(long date, String name, String author, Identifier... identifiers) {
		this.autoStats = AutoStats.instance();
		this.extensionManager = autoStats.extensionManager;

		this.date = date;
		this.name = name;
		this.author = author;
		identifierList = identifiers;

		if (identifiers.length == 0) logger.info(getClass().getName() + " has no server identifiers!");
	}

	/**
	 * Loads {@link #extensionConfig}
	 */
	protected abstract void loadConfig();

	/**
	 * Adds the settings shown in the AutoStats GUI
	 * 
	 * @param list the list to add the settings to
	 */
	protected abstract void fillSettings(List<SettingsElement> list);

	/**
	 * @param playerName
	 * @return the color code representing the skill level of this player
	 */
	protected abstract char getColorChar(String playerName);

	/**
	 * @param playerName
	 * @return the string to render in the tab list under the name of this player
	 */
	protected abstract String getTabListText(String playerName);

	/**
	 * Compares two players for the tab list sorting
	 * 
	 * @param playerName1
	 * @param playerName2
	 * @return the comparing result
	 */
	protected abstract int comparePlayer(String playerName1, String playerName2);

	/** @return the number of seconds until all statistics are viewable */
	protected abstract float getFinishedIn();

	/** @return the example gradient color, shown in the AutoStats GUI */
	protected abstract char getExampleColorChar();

	/**
	 * @return the example tab list text for a player, shown in the AutoStats GUI
	 */
	protected abstract String getExampleTabListText();

	/**
	 * Gets called, when this extension gets selected after a
	 * {@link ExtensionManager#refresh()} call
	 */
	protected void onSelect() {
	}

	/**
	 * Gets called, when another extension gets selected after a
	 * {@link ExtensionManager#refresh()} call
	 */
	protected void onDeselect() {
	}

	/** Gets called every frame */
	protected void onTick() {
	}

	/**
	 * Gets called when a chat line is received
	 * 
	 * @param unformatted with color codes
	 * @param formatted   without color codes
	 * @return true, if the line should not be displayed
	 */
	protected boolean onReceive(String unformatted, String formatted) {
		return false;
	}

	/**
	 * Gets called when the user sends a message to the server, not if the
	 * {@link AutoStats#sendMsg(String)} method gets called
	 * 
	 * @param msg
	 * @return true, if the line should not be sent
	 */
	protected boolean onSend(String msg) {
		return false;
	}

	/**
	 * Creates an element for the extension settings
	 * 
	 * @param displayName the name shown in the extension settings
	 * @param attrib      the name in the configuration, where this boolean is saved
	 * @param description the description to display, if this element is hovered
	 * @param icon        the icon of this element
	 * @param def         the value, if not contained in {@link #extensionConfig}
	 * @param objectPath  the path of JSON objects, that leads to the attribute to
	 *                    change
	 * @return the element updating the configuration
	 */
	protected BooleanElement createBoolean(String displayName, String attrib, String description, IconData icon,
			boolean def, String... objectPath) {
		boolean current = def;
		if (extensionConfig.has(attrib)) current = extensionConfig.get(attrib).getAsBoolean();
		Consumer<Boolean> callback = value -> setAttrib(attrib, new JsonPrimitive(value), objectPath);
		BooleanElement element = new BooleanElement(ModColor.createColors(displayName), icon, callback, current);
		element.setDescriptionText(description);
		return element;
	}

	/**
	 * Creates an element for the extension settings
	 * 
	 * @param displayName the name shown in the extension settings
	 * @param attrib      the name in the configuration, where this string is saved
	 * @param description the description to display, if this element is hovered
	 * @param icon        the icon of this element
	 * @param def         the value, if not contained in {@link #extensionConfig}
	 * @param objectPath  the path of JSON objects, that leads to the attribute to
	 *                    change
	 * @return the element updating the configuration
	 */
	protected StringElement createString(String displayName, String attrib, String description, IconData icon,
			String def, String... objectPath) {
		String current = def;
		if (extensionConfig.has(attrib)) current = extensionConfig.get(attrib).getAsString();
		Consumer<String> callback = value -> setAttrib(attrib, new JsonPrimitive(value), objectPath);
		StringElement element = new StringElement(ModColor.createColors(displayName), icon, current, callback);
		element.setDescriptionText(description);
		return element;
	}

	/**
	 * Creates an element for the extension settings
	 * 
	 * @param displayName the name shown in the extension settings
	 * @param attrib      the name in the configuration, where this float is saved
	 * @param description the description to display, if this element is hovered
	 * @param icon        the icon of this element
	 * @param def         the value, if not contained in {@link #extensionConfig}
	 * @param objectPath  the path of JSON objects, that leads to the attribute to
	 *                    change
	 * @return the element updating the configuration
	 */
	protected StringElement createFloat(String displayName, String attrib, String description, IconData icon, float def,
			String... objectPath) {
		float current = def;
		if (extensionConfig.has(attrib)) current = extensionConfig.get(attrib).getAsFloat();
		StringElement[] el = new StringElement[1];
		Consumer<String> callback = new Consumer<String>() {
			float prev = def;

			@Override
			public void accept(String val) {
				try {
					// Ensure the string is a float
					float f = Float.valueOf(val);

					// Save value
					prev = f;
					setAttrib(attrib, new JsonPrimitive(f), objectPath);
				} catch (Throwable t) {
					// Forcefully set the text back to the previous value
					try {
						Field fieldField = el[0].getClass().getDeclaredField("textField");
						fieldField.setAccessible(true);
						ModTextField field = (ModTextField) fieldField.get(el[0]);
						field.setText(String.valueOf(prev));
					} catch (Throwable th) {
						assert false;
					}
				}
			}
		};
		StringElement element = el[0] = new StringElement(ModColor.createColors(displayName), icon,
				String.valueOf(current), callback);
		element.setDescriptionText(description);
		return element;
	}

	/**
	 * Creates an element for the extension settings
	 * 
	 * @param displayName the name shown in the extension settings
	 * @param attrib      the name in the configuration, where this integer is saved
	 * @param description the description to display, if this element is hovered
	 * @param icon        the icon of this element
	 * @param def         the value, if not contained in {@link #extensionConfig}
	 * @param min         the minimum this number can be
	 * @param max         the maximum this number can be
	 * @param objectPath  the path of JSON objects, that leads to the attribute to
	 *                    change
	 * @return the element updating the configuration
	 */
	protected SliderElement createSlider(String displayName, String attrib, String description, IconData icon, int def,
			int min, int max, String... objectPath) {
		int current = def;
		if (extensionConfig.has(attrib)) current = extensionConfig.get(attrib).getAsInt();
		SliderElement element = new SliderElement(ModColor.createColors(displayName), icon, current);
		element.addCallback(value -> setAttrib(attrib, new JsonPrimitive(value), objectPath));
		element.setRange(min, max);
		element.setDescriptionText(description);
		return element;
	}

	/**
	 * Creates an element for the extension settings
	 * 
	 * @param displayName the name shown in the extension settings
	 * @param attrib      the name in the configuration, where this integer is saved
	 * @param description the description to display, if this element is hovered
	 * @param icon        the icon of this element
	 * @param def         the value, if not contained in {@link #extensionConfig}
	 * @param min         the minimum this number can be
	 * @param max         the maximum this number can be
	 * @param objectPath  the path of JSON objects, that leads to the attribute to
	 *                    change
	 * @return the element updating the configuration
	 */
	protected NumberElement createNumber(String displayName, String attrib, String description, IconData icon, int def,
			int min, int max, String... objectPath) {
		int current = def;
		if (extensionConfig.has(attrib)) current = extensionConfig.get(attrib).getAsInt();
		NumberElement element = new NumberElement(ModColor.createColors(displayName), icon, current);
		element.setRange(min, max);
		element.addCallback(value -> setAttrib(attrib, new JsonPrimitive(value), objectPath));
		element.setDescriptionText(description);
		return element;
	}

	/**
	 * Sets a value in the extension configuration
	 * 
	 * @param attrib     the name of the attribute in the configuration
	 * @param value      the value to set the attribute
	 * @param objectPath the path of JSON objects, that leads to the value changed
	 */
	protected final void setAttrib(String attrib, JsonElement value, String... objectPath) {
		// Don't allow to set class
		if (attrib.equals("class") && objectPath.length == 0) throw new RuntimeException("no");

		JsonObject config = autoStats.getConfig();
		if (config == null) return;

		// Get all extension configurations
		if (!config.has("extensionConfigs")) config.add("extensionConfigs", new JsonArray());
		JsonArray extensionConfigs = config.get("extensionConfigs").getAsJsonArray();

		// Find configuration of this class
		for (int i = extensionConfigs.size(); i > 0;) {
			JsonObject extensionConfig = extensionConfigs.get(--i).getAsJsonObject();
			if (extensionConfig.get("class").getAsString().equals(getClass().getName())) {
				// Generate objects from path
				JsonObject objToAdd = extensionConfig;
				for (String name : objectPath) {
					if (!objToAdd.has(name)) objToAdd.add(name, new JsonObject());
					objToAdd = objToAdd.get(name).getAsJsonObject();
				}

				// Add object, load configuration and exit
				objToAdd.add(attrib, value);
				this.extensionConfig = extensionConfig;
				loadConfig();
				return;
			}
		}

		// Create new configuration
		JsonObject extensionConfig = new JsonObject();
		extensionConfig.addProperty("class", getClass().getName());
		extensionConfig.addProperty("date", date);

		// Generate objects from path
		JsonObject objToAdd = extensionConfig;
		for (String name : objectPath) {
			if (!objToAdd.has(name)) objToAdd.add(name, new JsonObject());
			objToAdd = objToAdd.get(name).getAsJsonObject();
		}

		// Add object and load configuration
		objToAdd.add(attrib, value);
		extensionConfigs.add(extensionConfig);
		this.extensionConfig = extensionConfig;
		loadConfig();
	}

	/**
	 * Sends the user a message with the AutoStats prefix
	 * 
	 * @param msg
	 */
	protected void msg(String msg) {
		AutoStats.msg(msg);
	}

	/**
	 * Sends a message to the server
	 * 
	 * @param msg
	 */
	protected void sendMsg(String msg) {
		Minecraft.getMinecraft().thePlayer.sendChatMessage(msg);
	}

	/** @return the username of the currently logged in player */
	protected String getPlayerName() {
		return autoStats.getApi().getPlayerUsername();
	}

	/** @return true, if this extension supports the current server */
	public boolean isSupported() {
		for (Identifier identifier : identifierList) {
			if (!identifier.test()) {
				return false;
			}
		}
		return true;
	}

	/** @return true, if this extension should be used */
	public boolean doUse() {
		return autoStats.isUsable() && extensionManager.getCurrent() == this;
	}

	/**
	 * @return true, when this extension caused an exception and can not be used
	 *         anymore
	 */
	public boolean isCrashed() {
		return crashed;
	}

	/** @return the file this extension was loaded from */
	public String getFile() {
		return file;
	}

	/**
	 * Compares the date of this extension to another
	 * 
	 * @return the comparing result
	 */
	@Override
	public final int compareTo(Extension o) {
		return Long.compare(o.date, date);
	}

}
