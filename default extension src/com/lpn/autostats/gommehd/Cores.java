package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of Cores on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class Cores extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7bCores\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public Cores() {
		super("Cores", HEAD);
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
		values.add(createValueCores(1));
		return new GommeHDStatistics(playerName, values);
	}

}
