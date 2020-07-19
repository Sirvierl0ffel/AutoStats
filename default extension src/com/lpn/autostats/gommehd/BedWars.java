package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of BedWars on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class BedWars extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7bBedWars\u00A7r\n\u00A7r\u00A77Du m�chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public BedWars() {
		super("BedWars", HEAD);
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
		values.add(createValueBeds(1));
		return new GommeHDStatistics(playerName, values);
	}

}
