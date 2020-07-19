package com.lpn.autostats.gommehd;

import java.util.List;

import com.google.common.collect.ComparisonChain;
import com.lpn.autostats.AutoStats;
import com.lpn.autostats.extension.Identifier;
import com.lpn.autostats.extension.NormalExtension;

import net.labymod.settings.elements.SettingsElement;

/**
 * The base class for all games on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public abstract class GommeHDExtension extends NormalExtension {

	// The tab foot in every game
	private static final String FOOT = "\u00A7r\u00A77Füge deine Freunde hinzu, erstelle Partys oder gründe\u00A7r\n\u00A7r\u00A77deinen eigenen Clan mit: \u00A7r\u00A7a/friend\u00A7r\u00A77, \u00A7r\u00A7d/party \u00A7r\u00A77und \u00A7r\u00A7e/clan\u00A7r";

	/** The name of this game */
	protected final String name;

	/** The currently processing statistics */
	protected GommeHDStatistics current;

	/** The color code for nicked players */
	public static final char NICK_COLOR = '5';

	/**
	 * Creates new gommehd.net game
	 * 
	 * @param name    the name of the game
	 * @param tabHead the tab heads on which this extension should get loaded
	 */
	public GommeHDExtension(String name, String tabHead) {
		super(1587169196969L, "GommeHD " + name, "Sirvierl0ffel",
				new Identifier.Or(new Identifier.IP("gommehd.net"), new Identifier.IP("mc.gommehd.net")),
				new Identifier.Tab(tabHead, FOOT, false));
		this.name = name;
	}

	/**
	 * Creates new gommehd.net game
	 * 
	 * @param name        the name of the game
	 * @param displayName the name of the game in AutoStats
	 * @param tabHead     the tab heads on which this extension should get loaded
	 */
	public GommeHDExtension(String name, String displayName, String tabHead) {
		super(1587169196969L, "GommeHD " + displayName, "Sirvierl0ffel",
				new Identifier.Or(new Identifier.IP("gommehd.net"), new Identifier.IP("mc.gommehd.net")),
				new Identifier.Tab(tabHead, FOOT, false));
		this.name = name;
	}

	/** {@inheritDoc} */
	@Override
	protected abstract Statistics createStatistics(String playerName);

	/** {@inheritDoc} */
	@Override
	protected boolean onReceive(String unformatted, String formatted) {
		try {
			// Ensure that this extension is in use
			if (!doUse()) return false;

			// Only need to parse while unprocessed statistics are left
			if (!isProcessing()) return false;

			// Ignore this message, it for some reason does not mean anything
			if (formatted.equals(
					"Du hast zu viele Statistiken abgerufen, bitte versuche es in einer anderen Runde erneut")) {
				return true;
			}

			// Blacklist first waiting statistics when these messages appears
			if (formatted.equals("[" + name + "] Die Statistiken von diesem Spieler sind versteckt")) {
				GommeHDStatistics stats = getFirstAwaiting();
				if (stats != null && !stats.isDone()) stats.setBlackListed(true);
				return !stats.print;
			}
			if (formatted.equals("[" + name + "] Dieser Spieler konnte nicht gefunden werden")) {
				GommeHDStatistics stats = getFirstAwaiting();
				if (stats != null && !stats.isDone()) {
					stats.setBlackListed(true);
					stats.nicked = true; // Mark player as nicked
				}
				return !stats.print;
			}

			// Start parsing
			if (formatted.startsWith("-= Statistiken von ") && formatted.endsWith(" (30 Tage) =-")) {
				String playerName = formatted.substring(19, formatted.length() - 13);
				current = getStats(playerName);
				if (current == null) return false;
				if (current.isDone()) {
					current = null;
					return false;
				}
				// May happen, when the client is very laggy
				if (current.isBlacklisted()) current.setBlackListed(false);
				return !current.print;
			}

			// Normal behavior, while not parsing
			if (current == null) return false;

			if (formatted.startsWith("---")) {
				// Stop parsing
				boolean print = current.print;
				current.print = false;
				current.setDone(true);
				current = null;
				return !print;
			} else {
				// Parse
				for (Value<?> v : current.list) {
					GommeHDValue<?> value = (GommeHDValue<?>) v;
					if (value.chatName == null) continue;
					if (formatted.startsWith(" " + value.chatName + ": ")) {
						String string = formatted.substring(value.chatName.length() + 3);
						value.setFrom(string.replace(".", "").replace(",", ""));
						break;
					}
				}
				return !current.print;
			}
		} catch (Throwable t) {
			// I have to do this because because of parties it can happen that you parse
			// wrong messages
			msg("\u00A7cIgnoring exception occurred while parsing \"" + formatted + "\": " + t.getClass().getName()
					+ ": " + t.getMessage());
			t.printStackTrace();
			return false;
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void fillSubSettings(Value<?> stat, List<SettingsElement> list, int statCount) {
		super.fillSubSettings(stat, list, statCount);
	}

	// General

	/**
	 * @param i the default importance of this value
	 * @return a value holding the rank of a player
	 */
	protected GommeHDValue<?> createValueRanking(int i) {
		return new GommeHDValue<Integer>("ranking", "Position im Ranking", "Ranking", "The position in the ranking.",
				1234, -1) {
			{
				importance = i;
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the kills of a player
	 */
	protected GommeHDValue<?> createValueKills(int i) {
		return new GommeHDValue<Integer>("kills", "Kills", "Kills", "The kills made in a month.", 123, -1) {
			{
				enabled = true;
				importance = i;
				numberFormat = "0000";
				redEnd = 1000;
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the deaths of a player
	 */
	protected GommeHDValue<?> createValueDeaths(int i) {
		return new GommeHDValue<Integer>("deaths", "Deaths", "Deaths", "The deats in a month.", 123, -1) {
			{
				enabled = true;
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}

			@Override
			public void set(Integer value) {
				super.set(value);

				GommeHDStatistics stats = getStatistics();
				if (!stats.has("kd") || stats.playerName == null) return;
				int kills = stats.get("kills");
				int deaths = stats.get("deaths");
				stats.set("kd", (float) kills / (float) deaths);
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the kill/death ratio of a player
	 */
	protected GommeHDValue<?> createValueKD(int i) {
		return new GommeHDValue<Float>("kd", null, "K/D", "The kill/death ratio in a month.", 1.23456789f, -1f) {
			{
				enabled = true;
				colored = true;
				importance = i;
				numberFormat = "0.00";
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the played games of a player
	 */
	protected GommeHDValue<?> createValuePlayed(int i) {
		return new GommeHDValue<Integer>("played", "Gespielte Spiele", "Played", "The games played in a month.", 123,
				-1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the wins of a player
	 */
	protected GommeHDValue<?> createValueWon(int i) {
		return new GommeHDValue<Integer>("won", "Gewonnene Spiele", "Won", "The games won in a month.", 123, -1) {
			{
				importance = i;
				redEnd = 300;
			}

			@Override
			public void set(Integer value) {
				super.set(value);

				GommeHDStatistics stats = getStatistics();
				if (!stats.has("winrate") || stats.playerName == null) return;
				int played = stats.get("played");
				int won = stats.get("won");
				int winrate = (int) ((float) won / (float) played * 100);
				if (winrate == 100) winrate = 99;
				stats.set("winrate", winrate);
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the win rate of a player
	 */
	protected GommeHDValue<?> createValueWinRate(int i) {
		return new GommeHDValue<Integer>("winrate", null, "Winrate", "The won/played ratio in a month.", 12, -1) {
			{
				importance = i;
				redEnd = 60f;
				numberFormat = "00%";
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the points of a player
	 */
	protected GommeHDValue<?> createValuePoints(int i) {
		return new GommeHDValue<Integer>("points", "Punkte", "Points", "The points made in a month.", 123, -1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	// BedWars

	/**
	 * @param i the default importance of this value
	 * @return a value holding the destroyed beds of a player
	 */
	protected GommeHDValue<?> createValueBeds(int i) {
		return new GommeHDValue<Integer>("beds", "Zerstörte Betten", "Beds", "The destroyed beds in a month.", 123,
				-1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	// Cookies

	/**
	 * @param i the default importance of this value
	 * @return a value holding the cookies of a player
	 */
	protected GommeHDValue<?> createValueCookies(int i) {
		return new GommeHDValue<Integer>("beds", "Cookies", "Cookies", "The cookies in a month.", 123, -1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	// Cores

	/**
	 * @param i the default importance of this value
	 * @return a value holding the destroyed cores of a player
	 */
	protected GommeHDValue<?> createValueCores(int i) {
		return new GommeHDValue<Integer>("cores", "Zerstörte Cores", "Cores", "The destroyed cores in a month.", 123,
				-1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	// EnderGames

	/**
	 * @param i the default importance of this value
	 * @return a value holding the opened chests of a player
	 */
	protected GommeHDValue<?> createValueChests(int i) {
		return new GommeHDValue<Integer>("chests", "Geöffnete Truhen", "Chests", "The opened chests in a month.", 123,
				-1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	// JumpLeague

	/**
	 * @param i the default importance of this value
	 * @return a value holding the goals of a player
	 */
	protected GommeHDValue<?> createValueGoals(int i) {
		return new GommeHDValue<Integer>("goals", "Erreichte Ziele", "Goals", "The goals reached in a month.", 123,
				-1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the flawless goals of a player
	 */
	protected GommeHDValue<?> createValueFlawlessGoals(int i) {
		return new GommeHDValue<Integer>("flawlessGoals", "Erreichte Ziele ohne Fail", "Flawless Goals",
				"The goals reached without fail in a month.", 123, -1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	// SkyWars ranked

	/**
	 * @param i the default importance of this value
	 * @return a value holding the elo points of a player
	 */
	protected GommeHDValue<?> createValueEloPoints(int i) {
		return new GommeHDValue<Integer>("eloPoints", "Elo-Punkte", "Elo Points", "The strength of this player.", 123,
				-1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	// TTT

	/**
	 * @param i the default importance of this value
	 * @return a value holding the karma of a player
	 */
	protected GommeHDValue<?> createValueKarma(int i) {
		return new GommeHDValue<Integer>("karma", "Karma", "Karma", "The karma of this player.", 123, -1) {
			{
				importance = i;
				redEnd = 1000;
				numberFormat = "0000";
			}
		};
	}

	/**
	 * @param i the default importance of this value
	 * @return a value holding the false positive rate of a player
	 */
	protected GommeHDValue<?> createValueFalsePositiveRate(int i) {
		return new GommeHDValue<Integer>("falsePositiveRate", "Falsch-Kill-Quote", "False Positive Rate",
				"The false kill rate of this player.", 12, -1) {
			{
				importance = i;
				redEnd = 60;
				numberFormat = "00%";
			}
			
			@Override
			public void setFrom(String string) {
				Integer i = Integer.valueOf(string.split(" ")[0]);
				if (i == 100) i = 99;
				set(i);
			}
		};
	}

	/** {@inheritDoc} */
	@Override
	protected boolean onSend(String msg) {
		if (!doUse()) return false;

		if (!isProcessing()) return false;

		// Clear message of double spaces
		while (msg.contains("  ")) {
			msg = msg.replace("  ", " ");
		}

		// Search for /stats command and the player in it
		String playerName = null;
		if (msg.equals("/stats") || msg.equals("/statsd 30")) {
			playerName = getPlayerName();
		} else if (msg.startsWith("/stats ")) {
			playerName = msg.substring(7);
		} else if (msg.startsWith("/statsd 30 ")) {
			playerName = msg.substring(11);
		} else {
			// When unsuccessful do nothing
			return false;
		}

		// Need to add player to processed list to avoid blacklisting wrong statistics

		GommeHDStatistics stats = getStats(playerName);

		// Create new statistics instance, if the statistics were not found
		if (stats == null) {
			statsList.add(stats = (GommeHDStatistics) createStatistics(playerName));
		}

		// Reset that
		stats.setBlackListed(false);
		stats.setDone(false);

		// Just handle it as it was just processed
		stats.setTries(stats.getTries() + 1);
		stats.setProcessed(System.currentTimeMillis());
		stats.print = true; // The user will see the statistics message
		processedList.add(stats);
		lastAction = System.currentTimeMillis() + 500; // Wait a bit more

		return false;
	}

	/** {@inheritDoc} */
	@Override
	public char getColorChar(String playerName) {
		GommeHDStatistics stats = getStats(playerName);
		if (stats == null) return '0';
		if (stats.nicked) return NICK_COLOR;
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
	public int comparePlayer(String playerName1, String playerName2) {
		GommeHDStatistics stats1 = getStats(playerName1);
		GommeHDStatistics stats2 = getStats(playerName2);
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
		return ComparisonChain.start().compareFalseFirst(stats1.nicked, stats2.nicked)
				.compareTrueFirst(stats1.isProcessed(), stats2.isProcessed())
				.compareFalseFirst(stats1.isBlacklisted(), stats2.isBlacklisted()).compareFalseFirst(nan1, nan2)
				.compare((Comparable<?>) value2.get(), (Comparable<?>) value1.get()).result();
	}

	/**
	 * {@link Statistics} with {@link #nicked} and {@link #print} booleans
	 * 
	 * @author Sirvierl0ffel
	 */

	class GommeHDStatistics extends Statistics {

		/** True, if the player could not be found */
		boolean nicked;

		/** True, if the response to this /stats execution should be printed */
		boolean print;

		/**
		 * Just creates {@link GommeHDExtension} instance
		 * 
		 * @param playerName
		 * @param map
		 */
		GommeHDStatistics(String playerName, List<Value<?>> list) {
			super(playerName, list);
		}

		/** {@inheritDoc} */
		@Override
		public String format() {
			return nicked ? "\u00A7" + NICK_COLOR + "[nick]" : super.format();
		}

		/**
		 * Needed, to update the tries, if the user checks these statistics
		 * 
		 * @param tries
		 */
		void setTries(int tries) {
			this.tries = tries;
		}

		/**
		 * Needed, to update the last processing, if the user checks these statistics
		 * 
		 * @param processed
		 */
		void setProcessed(long processed) {
			this.processed = processed;
		}

	}

	/**
	 * {@link Value} with chat name
	 *
	 * @param <T> the type of the value, which must implement {@link Comparable}
	 */

	class GommeHDValue<T> extends Value<T> {

		/** What the name of this statistic is in a message */
		final String chatName;

		/**
		 * Just creates {@link GommeHDValue} instance
		 * 
		 * @param chatName the name of the statistic value in the chat, can be
		 *                 {@code null}, if self calculated
		 * @param name     the name of the statistic value in AutoStats
		 * @param def      the initial value
		 */
		GommeHDValue(String key, String chatName, String name, String description, T example, T def) {
			super(key, name, description, example, def);
			this.chatName = chatName;
		}

	}

}
