package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of EnderGames on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class EnderGames extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A75EnderGames\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public EnderGames() {
		super("EnderGames", HEAD);
	}

	/** {@inheritDoc} */
	@Override
	protected Statistics createStatistics(String playerName) {
		List<Value<?>> values = new ArrayList<>(9);
		values.add(createValueKD(9));
		values.add(createValueKills(8));
		values.add(createValueDeaths(7));
		values.add(createValueWinRate(6));
		values.add(createValuePlayed(5));
		values.add(createValueWon(4));
		values.add(createValueRanking(3));
		values.add(createValuePoints(2));
		values.add(createValueChests(1));
		return new GommeHDStatistics(playerName, values);
	}

}
