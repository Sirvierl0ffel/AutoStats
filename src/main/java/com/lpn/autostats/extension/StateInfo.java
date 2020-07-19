package com.lpn.autostats.extension;

import java.util.LinkedHashMap;
import java.util.Map;

import com.lpn.autostats.AutoStats;

import net.labymod.api.EventManager;
import net.labymod.api.events.TabListEvent;
import net.labymod.utils.ServerData;

/**
 * Holds information for identifiers, the map can be used to hold informations
 * after reloading extensions
 * 
 * @author Sirvierl0ffel
 */

public class StateInfo {

	/** Can be used to hold info after reloading extensions */
	public final Map<String, Object> map = new LinkedHashMap<>();

	private final AutoStats autoStats;

	private String ip = "";
	private int port = -1;
	private String tabHead = "";
	private String tabFoot = "";

	/** Just creates {@link StateInfo} instance */
	public StateInfo() {
		autoStats = AutoStats.instance();
		updateMap();
	}

	/** Registers event listeners */
	public void init() {
		EventManager eventManager = autoStats.getApi().getEventManager();
		eventManager.register(this::onTabchange);
		eventManager.registerOnJoin(this::onJoin);
		eventManager.registerOnQuit(this::onQuit);
	}

	private void onTabchange(TabListEvent.Type type, String unformatted, String formatted) {
		if (type == TabListEvent.Type.HEADER) tabHead = unformatted;
		if (type == TabListEvent.Type.FOOTER) tabFoot = unformatted;
		updateMap();
		autoStats.extensionManager.refresh();
	}

	private void onJoin(ServerData data) {
		ip = data.getIp();
		port = data.getPort();
		updateMap();
		autoStats.extensionManager.refresh();
	}

	private void onQuit(ServerData data) {
		ip = "";
		port = -1;
		tabHead = "";
		tabFoot = "";
		updateMap();
		autoStats.extensionManager.refresh();
	}

	private void updateMap() {
		map.put("ip", ip);
		map.put("port", port);
		map.put("tabHead", tabHead);
		map.put("tabFoot", tabFoot);
	}

	/** @return the IP of the current server, empty, if not connected */
	public String getIP() {
		return ip;
	}

	/** @return the port of the current server, -1, if not connected */
	public int getPort() {
		return port;
	}

	/** @return the tab head string */
	public String getTabHead() {
		return tabHead;
	}

	/** @return the tab foot string */
	public String getTabFoot() {
		return tabFoot;
	}

}
