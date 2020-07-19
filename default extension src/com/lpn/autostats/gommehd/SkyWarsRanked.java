package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of SkyWars on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class SkyWarsRanked extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7aRSKYWARS\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public SkyWarsRanked() {
		super("SkyWars", "Skywars Ranked", HEAD);
	}

	/** {@inheritDoc} */
	@Override
	protected Statistics createStatistics(String playerName) {
		List<Value<?>> values = new ArrayList<>(7);
		values.add(createValueKD(7));
		values.add(createValueKills(6));
		values.add(createValueDeaths(5));
		values.add(createValueWinRate(4));
		values.add(createValuePlayed(3));
		values.add(createValueWon(2));
		values.add(createValueEloPoints(1));
		return new GommeHDStatistics(playerName, values);
	}

}
