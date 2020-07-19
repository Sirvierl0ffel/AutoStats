package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of Cookies on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class Cookies extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7aCOOKIES\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public Cookies() {
		super("Cookies", HEAD);
	}

	/** {@inheritDoc} */
	@Override
	protected Statistics createStatistics(String playerName) {
		List<Value<?>> list = new ArrayList<>(5);
		list.add(createValueWinRate(5));
		list.add(createValuePlayed(4));
		list.add(createValueWon(3));
		list.add(createValueRanking(2));
		list.add(createValueCookies(1));
		return new GommeHDStatistics(playerName, list);
	}

}
