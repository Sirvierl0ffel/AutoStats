package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of SurvivalGames on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class SurvivalGames extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7aSurvivalGames\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";
//	private static final String HEAD_QUICK = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7aQuick SurvivalGames\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public SurvivalGames() {
		super("SurvivalGames", HEAD);
	}

	/** {@inheritDoc} */
	@Override
	protected Statistics createStatistics(String playerName) {
		List<Value<?>> values = new ArrayList<>(8);
		values.add(createValueKD(8));
		values.add(createValueKills(7));
		values.add(createValueDeaths(6));
		values.add(createValueWinRate(5));
		values.add(createValuePlayed(4));
		values.add(createValueWon(3));
		values.add(createValueRanking(2));
		values.add(createValuePoints(1));
		return new GommeHDStatistics(playerName, values);
	}

}
