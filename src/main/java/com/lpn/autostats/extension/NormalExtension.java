package com.lpn.autostats.extension;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ComparisonChain;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.lpn.autostats.AutoStats;
import com.lpn.autostats.render.AutoStatsTabList;

import net.labymod.core.LabyModCore;
import net.labymod.gui.elements.ModTextField;
import net.labymod.settings.Settings;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.Material;
import net.labymod.utils.ModColor;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;

/**
 * A normal {@link Extension} with a statistics list getting worked through
 * 
 * @author Sirvierl0ffel
 */

public abstract class NormalExtension extends Extension {

	/** The list keeping the {@link Statistics} instance of all players */
	protected final List<Statistics> statsList = new ArrayList<>();
	protected final List<Statistics> processedList = new ArrayList<>();

	/** The color code for blacklisted statistics */
	public static final char NAH_COLOR = 'd';

	/** The color code for processing statistics */
	public static final char MHH_COLOR = 'b';

	private int maxTries = 2;
	private int intervalMS = 1000;
	private int timeoutMS = 15000;

	protected long lastAction;

	/** Attributes of statistic values */
	protected JsonObject statsAttribs = new JsonObject();

	private final Statistics exampleStatistics;

	/**
	 * Just calls {@link Extension#Extension(long, String, String, Identifier...)}
	 * 
	 * @param date
	 * @param name
	 * @param author
	 * @param identifiers
	 */
	public NormalExtension(long date, String name, String author, Identifier... identifiers) {
		super(date, name, author, identifiers);

		exampleStatistics = createStatistics(null);
	}

	/** {@inheritDoc} */
	@Override
	protected void loadConfig() {
		if (extensionConfig.has("maxTries")) maxTries = extensionConfig.get("maxTries").getAsInt();
		if (extensionConfig.has("interval")) intervalMS = extensionConfig.get("interval").getAsInt();
		if (extensionConfig.has("timeout")) timeoutMS = extensionConfig.get("timeout").getAsInt();
		if (extensionConfig.has("statsAttribs")) statsAttribs = extensionConfig.get("statsAttribs").getAsJsonObject();
	}
	
	/** {@inheritDoc} */
	@Override
	public void fillSettings(List<SettingsElement> list) {
		list.add(createSlider("Max Tries", "maxTries", "The max attempts to make at getting the stats of a player.",
				new IconData(Material.DIODE), maxTries, 1, 10));

		list.add(createSlider("Interval", "interval",
				"The milliseconds to wait after getting the next stats of a player.", new IconData(Material.DIODE),
				intervalMS, 500, 5000));

		list.add(createSlider("Timeout", "timeout",
				"The milliseconds to wait before retrying to get the stats of a player.", new IconData(Material.DIODE),
				timeoutMS, 5000, 60000));

		List<Value<?>> sorted = new ArrayList<>(exampleStatistics.list);
		sorted.sort(null);
		for (Value<?> value : sorted) {
			BooleanElement enabled = createBoolean(value.name, "enabled", value.description,
					new IconData(Material.LEVER), value.getAttrib("enabled", value.enabled), "statsAttribs", value.key);
			list.add(enabled);

			List<SettingsElement> subSettings = new ArrayList<>();
			fillSubSettings(value, subSettings, sorted.size());
			Settings settings = new Settings(subSettings.toArray(new SettingsElement[0]));
			enabled.setSubSettings(settings);
		}
	}

	protected void fillSubSettings(Value<?> value, List<SettingsElement> list, int statCount) {
		list.add(createString("Label", "label", "The label in the tab list.", new IconData(Material.SIGN),
				value.getAttrib("label", value.label), "statsAttribs", value.key));
		list.add(createNumber("Importance", "importance", "The higher the number the more to the lef in the tab list.",
				new IconData(Material.REDSTONE_TORCH_ON), value.getAttrib("importance", value.importance).intValue(), 1,
				statCount, "statsAttribs", value.key));

		if (value.isNumber()) {
			String def = value.getAttrib("numberFormat", value.numberFormat);
			StringElement nfElement = createString("Number Format", "numberFormat",
					"The java.text.DecimalFormat pattern.", new IconData(Material.COMMAND), def, "statsAttribs",
					value.key);

			// Ensure the string is a valid number format
			nfElement.addCallback(new Consumer<String>() {
				String prev = def;

				@Override
				public void accept(String val) {
					try {
						// Create instance to check pattern
						new DecimalFormat(val, new DecimalFormatSymbols(Locale.ENGLISH));

						// Save valid value
						prev = val;
						setAttrib("numberFormat", new JsonPrimitive(val), "statsAttribs", value.key);
					} catch (Throwable t) {
						// Forcefully set the text back to the previous value
						try {
							Field fieldField = nfElement.getClass().getDeclaredField("textField");
							fieldField.setAccessible(true);
							ModTextField field = (ModTextField) fieldField.get(nfElement);
							field.setText(String.valueOf(prev));
						} catch (Throwable th) {
							assert false;
						}
					}
				}
			});

			list.add(nfElement);

			list.add(createBoolean("Colored", "colored", "Colors the value to show how good it is.",
					new IconData(Material.LEVER), value.getAttrib("colored", value.colored), "statsAttribs",
					value.key));

			list.add(createFloat("Green Start", "greenStart", "Where to start mapping this value as green.",
					new IconData(Material.REDSTONE_TORCH_ON),
					value.getAttrib("greenStart", value.greenStart).intValue(), "statsAttribs", value.key));

			list.add(createFloat("Red End", "redEnd", "Where this value is full red.",
					new IconData(Material.REDSTONE_TORCH_ON), value.getAttrib("redEnd", value.redEnd).intValue(),
					"statsAttribs", value.key));
		}
	}

	/** {@inheritDoc} */
	@Override
	public char getColorChar(String playerName) {
		Statistics stats = getStats(playerName);
		if (stats == null) return '0';
		if (stats.isBlacklisted()) return NAH_COLOR;
		if (!stats.isDone()) return MHH_COLOR;
		Value<?> value = stats.getMostImportant();
		float f = value.asFloat();
		float greenStart = value.getAttrib("greenStart", value.greenStart).floatValue();
		float redEnd = value.getAttrib("redEnd", value.redEnd).floatValue();
		return AutoStats.mapToGreenRed(f, greenStart, redEnd);
	}

	/** {@inheritDoc} */
	@Override
	public String getTabListText(String playerName) {
		Statistics stats = getStats(playerName);
		if (stats == null) return "";
		return stats.format();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int comparePlayer(String playerName1, String playerName2) {
		Statistics stats1 = getStats(playerName1);
		Statistics stats2 = getStats(playerName2);
		if (stats1 == null) return 0;
		if (stats2 == null) return 0;
		Value<?> value1 = stats1.getMostImportant();
		Value<?> value2 = stats2.getMostImportant();
		boolean nan1 = false;
		boolean nan2 = false;
		if (stats1.getMostImportant().isNumber()) {
			nan1 = Float.isNaN(value1.asFloat());
			nan2 = Float.isNaN(value2.asFloat());
		}
		return ComparisonChain.start().compareTrueFirst(stats1.isProcessed(), stats2.isProcessed())
				.compareFalseFirst(stats1.isBlacklisted(), stats2.isBlacklisted()).compareFalseFirst(nan1, nan2)
				.compare((Comparable<?>) value2.get(), (Comparable<?>) value1.get()).result();
	}

	/** {@inheritDoc} */
	@Override
	public float getFinishedIn() {
		float undone = 0;
		for (Statistics stats : statsList) {
			if (!stats.isProcessed()) undone++;
		}
		if (undone == 0) return 0;
		long currentMS = System.currentTimeMillis();
		int msSinceLast = (int) (currentMS - lastAction);
		float seconds = undone * intervalMS / 1000f - msSinceLast / 1000f;
		return seconds;
	}

	/** {@inheritDoc} */
	@Override
	protected char getExampleColorChar() {
		return getColorChar(null);
	}

	/** {@inheritDoc} */
	@Override
	protected String getExampleTabListText() {
		return getTabListText(null);
	}

	/** {@inheritDoc} */
	@Override
	protected void onDeselect() {
		// Clear memory of all statistics
		statsList.clear();
		processedList.clear();
	}

	/** {@inheritDoc} */
	@Override
	public void onTick() {
		// Check for use
		if (!doUse()) return;

		// Break, if no new action can be made
		long currentMS = System.currentTimeMillis();
		if (lastAction + intervalMS > currentMS) return;

		fillStatsList();

		for (Statistics s : statsList) {
			Statistics stats = (Statistics) s;

			// Only process not done / blacklisted statistics
			if (stats.isDone() || stats.isBlacklisted()) continue;

			if (!stats.isProcessed()) {
				// Process if unprocessed
				stats.tries++;
				stats.processed = currentMS;
				processedList.add(stats);
				process(stats.playerName);
			} else {
				// Reprocess only after timeout
				if (!stats.isTimedout()) continue;
				if (stats.tries < maxTries) {
					stats.tries++;
					stats.processed = currentMS;
					processedList.add(stats);
					process(stats.playerName);
				} else {
					// Blacklist after timeout of final try
					stats.setBlackListed(true);
				}
			}

			// Exit loop after first action
			break;
		}
		lastAction = currentMS;
	}

	/** @return the statistics this extension is waiting for the longest */
	public <T extends Statistics> T getFirstAwaiting() {
		for (Statistics stats : new ArrayList<>(processedList)) {
			if (!stats.isDone() && !stats.isBlacklisted()) {
				return (T) stats;
			} else {
				processedList.remove(stats);
			}
		}
		return null;
	}

	/** @return true, if this extension is still processing statistics */
	public boolean isProcessing() {
		for (Statistics stats : statsList) {
			if (!stats.isDone() && !stats.isBlacklisted()) return true;
		}
		return false;
	}

	/**
	 * Finds the statistics of a player
	 * 
	 * @param playerName
	 * @return the statistics instance bound to this player
	 */
	public <T extends Statistics> T getStats(String playerName) {
		if (playerName == null) return (T) exampleStatistics;

		for (Statistics stats : statsList) {
			if (stats.playerName.equalsIgnoreCase(playerName)) {
				return (T) stats;
			}
		}
		return null;
	}

	/** Fills the statistics list with the name of the players in the tab list */
	public void fillStatsList() {
		// Create sorted list of players
		EntityPlayerSP player = LabyModCore.getMinecraft().getPlayer();
		Collection<NetworkPlayerInfo> players = player.sendQueue.getPlayerInfoMap();
		Map<Statistics, NetworkPlayerInfo> map = new HashMap<>();

		boolean processing = false;
		boolean added = false;
		for (NetworkPlayerInfo nwPlayer : players) {
			String playerName = nwPlayer.getGameProfile().getName();
			Statistics stats = getStats(playerName);
			if (stats != null) {
				if (!stats.isDone()) processing = true;
				map.put(stats, nwPlayer);
				continue;
			}
			stats = createStatistics(playerName);
			statsList.add(stats);
			map.put(stats, nwPlayer);
			added = true;
		}

		statsList.sort((s1, s2) -> AutoStatsTabList.ordering.compare(map.get(s1), map.get(s2)));

		// If a new player joined and all others are processed make AutoStats wait the
		// delay again
		if (!processing && added) {
			// Or not ...
//			lastAction = System.currentTimeMillis();
		}
	}

	/**
	 * Creates {@link Statistics} instance for the tab list.
	 * 
	 * @param playerName the player, to create the statistics for, may be null
	 * @return a new statistics instance for the player
	 */
	protected abstract Statistics createStatistics(String playerName);

	/**
	 * Requests the statistics of this player, executes {@code /stats <name>} by
	 * default
	 * 
	 * @param playerName
	 */
	public void process(String playerName) {
		sendMsg("/stats " + playerName);
	}

	/**
	 * Holds the statistics of one player
	 */
	public class Statistics {

		/** The name of the player that has these statistics */
		public final String playerName;

		/** List of all statistic values */
		public final List<Value<?>> list;

		/** The time these statistics where processed, -1 if not processed */
		protected long processed = -1;

		/** The amount of tries to get these statistics */
		protected int tries;

		private boolean blacklisted;
		private boolean done;

		private final boolean example;

		/**
		 * Creates new {@link Statistics} instance
		 * 
		 * @param playerName the name of the player that has this statistics
		 * @param map        the map of values to fill in
		 */
		public Statistics(String playerName, List<Value<?>> list) {
			if (list.isEmpty()) throw new IllegalStateException("Can not initialize with empty list!");

			this.playerName = playerName;
			this.list = Collections.unmodifiableList(list);
			this.list.forEach(value -> value.statistics = this);
			this.example = playerName == null;
		}

		/** Marks these statistics as (un)processable */
		public void setBlackListed(boolean blacklisted) {
			this.blacklisted = blacklisted;
		}

		/**
		 * @return true, if the time this statistics where processed was
		 *         {@link NormalExtension#intervalMS} ago
		 */
		public boolean isTimedout() {
			return processed + timeoutMS < System.currentTimeMillis();
		}

		/** @return the time these statistics where processed */
		public long getProcessed() {
			return processed;
		}

		/** @return true, if an attempt at getting these statistics was made */
		public boolean isProcessed() {
			return tries > 0 || example;
		}

		/** @return the amount of times these statistics where processed */
		public int getTries() {
			return tries;
		}

		/** @return true, if all attempts at getting this extension failed */
		public boolean isBlacklisted() {
			return blacklisted && !example;
		}

		/**
		 * @param <T>  the type of value
		 * @param name the name of the value
		 * @return the value by this name
		 */
		public <T> T get(String name) {
			return (T) getValue(name).value;
		}

		/**
		 * Sets this value
		 * 
		 * @param <T>
		 * @param name  the name of the value
		 * @param value the new value
		 */
		public <T> void set(String name, T value) {
			((Value<T>) getValue(name)).set(value);
		}

		/**
		 * @param name the name of the value
		 * @return true, if these statistics contain a value by this name
		 */
		public boolean has(String name) {
			return getValue(name) != null;
		}

		/**
		 * @param name the name of the value
		 * @return the value by this name
		 */
		public Value<?> getValue(String name) {
			for (Value<?> value : list) {
				if (value.key.equals(name)) {
					return value;
				}
			}
			return null;
		}

		/**
		 * Set to true, if the statistics are done
		 * 
		 * @param done
		 */
		public void setDone(boolean done) {
			this.done = done;
		}

		/** @return true, when one value is set */
		public boolean isDone() {
			return done || example;
		}

		/** @return the string to represent these statistics in the tab list */
		public String format() {
			if (!isProcessed()) {
				if (AutoStats.instance().extensionManager.isStopped()) return "\u00A7" + NAH_COLOR + "[nah]";

				return "\u00A7e...";
			}

			if (!isDone()) {
				if (isBlacklisted()) return "\u00A7" + NAH_COLOR + "[nah]";

				NormalExtension current = (NormalExtension) AutoStats.instance().extensionManager.getCurrent();
				long processed = getProcessed();
				long currentMS = System.currentTimeMillis();
				float seconds = (current.timeoutMS - (currentMS - processed)) / 1000f;
				if (seconds < 0) seconds = 0;
				DecimalFormat df = new DecimalFormat("00.0", new DecimalFormatSymbols(Locale.ENGLISH));
				return "\u00A7" + MHH_COLOR + df.format(seconds) + "s ...";
			}

			String string = "";
			List<Value<?>> sorted = new ArrayList<>(list);
			sorted.sort(null);
			for (Value<?> value : sorted) {
				if (!value.getAttrib("enabled", value.enabled)) continue;
				string += (string.equals("") ? "" : " \u00A7r") + value.format();
			}
			return string;
		}

		/** @return the most important statistics value */
		public Value<?> getMostImportant() {
			List<Value<?>> values = new ArrayList<>(list);
			values.sort(null);
			return values.get(0);
		}

	}

	/**
	 * Represents a single value of the {@link Statistics} of a player
	 * 
	 * @param <T> the type of the value, which has to implement {@link Comparable}
	 */
	public class Value<T> implements Comparable<Value<?>> {

		private Statistics statistics;

		/** The intern name of this statistic value */
		public final String key;

		/** The name shown in the AutoStats GUI */
		public final String name;

		/** Description of this statistics value */
		public final String description;

		/** Example value */
		public final T example;

		// def parameters for getBool(), getNumber(), getString()
		/** The default enabled attribute */
		public boolean enabled;
		/** The default label attribute */
		public String label;
		/** The default colored attribute */
		public boolean colored;
		/** The default importance attribute */
		public int importance = 1;
		/** The default color mapping attributes */
		public float greenStart, redEnd = 6;
		/** The default number format attribute */
		public String numberFormat = "#";

		private T value;
		private boolean set;

		/**
		 * Creates {@link Value} instance
		 * 
		 * @param label the label of the value shown in the tab list
		 * @param def   the default value
		 */
		public Value(String key, String name, String description, T example, T def) {
			this.key = key;
			this.name = name;
			this.description = description;
			this.example = example;

			value = def;
			label = "&7" + name + ":&2";
		}

		/**
		 * Sets the value, if the type of the value is a primitive one, otherwise throws
		 * {@link IllegalArgumentException}
		 * 
		 * @param string
		 */
		public void setFrom(String string) {
			if (value instanceof Boolean) {
				set((T) Boolean.valueOf(string));
			} else if (value instanceof Byte) {
				set((T) Byte.valueOf(string));
			} else if (value instanceof Short) {
				set((T) Short.valueOf(string));
			} else if (value instanceof Character) {
				set((T) Character.valueOf(string.charAt(0)));
			} else if (value instanceof Integer) {
				set((T) Integer.valueOf(string));
			} else if (value instanceof Long) {
				set((T) Long.valueOf(string));
			} else if (value instanceof Float) {
				set((T) Float.valueOf(string));
			} else if (value instanceof Double) {
				set((T) Double.valueOf(string));
			} else if (value instanceof String) {
				set((T) string);
			} else throw new IllegalArgumentException(value.getClass().getName() + " is not a parsable type!");
		}

		/** @return true, if the value is a number, e.g. an integer or a float */
		public boolean isNumber() {
			return value instanceof Number;
		}

		/**
		 * Sets the statistic value
		 * 
		 * @param value
		 */
		public void set(T value) {
			this.value = value;
			set = true;
		}

		/** @return the value cast to a float */
		public float asFloat() {
			return ((Number) value).floatValue();
		}

		/** @return the statistic value */
		public T get() {
			return value;
		}

		/** @return true, if the {@link #set(Object)} method was called */
		public boolean isSet() {
			return set;
		}

		/** @return the string to represent this value in the tab list */
		public String format() {
			if (statistics.example && !set) {
				set(example);
			}

			boolean colored = getAttrib("colored", this.colored);
			float greenStart = getAttrib("greenStart", this.greenStart).floatValue();
			float redEnd = getAttrib("redEnd", this.redEnd).floatValue();
			String label = getAttrib("label", this.label);
			String numberFormat = getAttrib("numberFormat", this.numberFormat);
			DecimalFormat decimalFormat = new DecimalFormat(numberFormat, new DecimalFormatSymbols(Locale.ENGLISH));
			decimalFormat.setMultiplier(1); // Don't use normalized percentages
			
			boolean map = colored && isNumber();
			String color = (map ? "\u00A7" + autoStats.mapToGreenRed(asFloat(), greenStart, redEnd) : "");
			String v = isNumber() ? decimalFormat.format(Float.isNaN(asFloat()) ? 0 : asFloat())
					: String.valueOf(value);
			
			String value = (isSet() ? "" + v
					: ((extensionManager.isStopped() && !statistics.example) ? "\u00A7c[stp]" : "\u00A7c-"));

			return ModColor.createColors(label) + color + value;
		}

		/**
		 * @param name the name of the attribute of this statistics value
		 * @param def  the default value
		 * @return the value of this attribute
		 */
		public boolean getAttrib(String name, boolean def) {
			if (!statsAttribs.has(key)) return def;
			JsonObject attribs = statsAttribs.get(key).getAsJsonObject();
			if (!attribs.has(name)) return def;
			return attribs.get(name).getAsBoolean();
		}

		/**
		 * @param name the name of the attribute of this statistics value
		 * @param def  the default value
		 * @return the value of this attribute
		 */
		public Number getAttrib(String name, Number def) {
			if (!statsAttribs.has(key)) return def;
			JsonObject attribs = statsAttribs.get(key).getAsJsonObject();
			if (!attribs.has(name)) return def;
			return attribs.get(name).getAsNumber();
		}

		/**
		 * @param name the name of the attribute of this statistics value
		 * @param def  the default value
		 * @return the value of this attribute
		 */
		public String getAttrib(String name, String def) {
			if (!statsAttribs.has(key)) return def;
			JsonObject attribs = statsAttribs.get(key).getAsJsonObject();
			if (!attribs.has(name)) return def;
			return attribs.get(name).getAsString();
		}

		/**
		 * @param <U> the type of statistics
		 * @return the statistics containing this value
		 */
		public <U extends Statistics> U getStatistics() {
			return (U) statistics;
		}

		/**
		 * Comparable to sort by importance
		 * 
		 * @param value the {@link Value} to compare to
		 * @return the comparing result
		 */
		@Override
		public int compareTo(Value value) {
			int importance = this.getAttrib("importance", this.importance).intValue();
			int oImportance = value.getAttrib("importance", value.importance).intValue();
			return Integer.compare(oImportance, importance);
		}

	}

}
