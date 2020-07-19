package com.lpn.autostats.gommehd;

import java.util.ArrayList;
import java.util.List;

import com.lpn.autostats.extension.ExtensionManager;

/**
 * Manages the statistics of GunGame on gommehd.net
 * 
 * @author Sirvierl0ffel
 */

public class GunGame extends GommeHDExtension {

	private static final String HEAD = "\u00A7r\u00A7eGommeHD.net\u00A7r\u00A7e \u00A7r\u00A7aGUNGAME\u00A7r\n\u00A7r\u00A77Du m\u00F6chtest einen Spieler melden? \u00A7r\u00A7c/report \u00A7r\u00A77<Spieler>\u00A7r";

	/**
	 * Constructor to get invoked from {@link ExtensionManager#reloadExtensions()}
	 */
	public GunGame() {
		super("GunGame", HEAD);
	}

	/** {@inheritDoc} */
	@Override
	protected Statistics createStatistics(String playerName) {
		List<Value<?>> list = new ArrayList<>(1);
		list.add(createValueKills(1));
		return new GommeHDStatistics(playerName, list);
	}

}
