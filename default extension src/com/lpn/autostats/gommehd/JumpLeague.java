package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of JumpLeague on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class JumpLeague extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7aJL\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public JumpLeague() {
		super("JumpLeague", HEAD);
	}

	/** {@inheritDoc} */
	@Override
	protected Statistics createStatistics(String playerName) {
		List<Value<?>> values = new ArrayList<>(11);
		values.add(createValueKD(11));
		values.add(createValueKills(10));
		values.add(createValueDeaths(8));
		values.add(createValueWinRate(7));
		values.add(createValuePlayed(6));
		values.add(createValueWon(5));
		values.add(createValueRanking(4));
		values.add(createValuePoints(3));
		values.add(createValueGoals(2));
		values.add(createValueFlawlessGoals(1));
		return new GommeHDStatistics(playerName, values);
	}

}
