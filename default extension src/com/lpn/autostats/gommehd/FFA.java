package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of FFA on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class FFA extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7aHARDCORE\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public FFA() {
		super("FFA", HEAD);
	}

	/** {@inheritDoc} */
	@Override
	protected Statistics createStatistics(String playerName) {
		List<Value<?>> list = new ArrayList<>(3);
		list.add(createValueKills(3));
		list.add(createValueDeaths(2));
		list.add(createValueKD(1));
		return new GommeHDStatistics(playerName, list);
	}

}
