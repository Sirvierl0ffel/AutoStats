package com.lpn.autostats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import net.labymod.ingamegui.moduletypes.SimpleModule;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.ModColor;

/**
 * LabyMod module, which displays the current state of AutoStats
 * 
 * @author Sirvierl0ffel
 */

public class AutoStatsModule extends SimpleModule {

	private final AutoStats autoStats = AutoStats.instance();

	private String label;
	private String disabledValue;
	private String unsupportedValue;
	private String stoppedValue;
	private String finishedInValue;
	private String finishedValue;

	/** {@inheritDoc} */
	@Override
	public void loadSettings() {
		label = ModColor.createColors(getAttribute("label", "AutoStats"));
		disabledValue = ModColor.createColors(getAttribute("disabledValue", "&7Disabled"));
		unsupportedValue = ModColor.createColors(getAttribute("unsupportedValue", "&7Unsupported"));
		stoppedValue = ModColor.createColors(getAttribute("stoppedValue", "&7Stopped"));
		finishedInValue = ModColor.createColors(getAttribute("finishedInValue", "&7Finished: &8"));
		finishedValue = ModColor.createColors(getAttribute("finishedValue", "&7Finished: &8--:--.-"));
	}

	/** {@inheritDoc} */
	@Override
	public void fillSubSettings(List<SettingsElement> settingsElements) {
		settingsElements.add(new StringElement(this, new IconData(), "Label", "label"));
		settingsElements.add(new StringElement(this, new IconData(), "Disabled Value", "disabledValue"));
		settingsElements.add(new StringElement(this, new IconData(), "Unsupported Value", "unsupportedValue"));
		settingsElements.add(new StringElement(this, new IconData(), "Stopped Value", "stoppedValue"));
		settingsElements.add(new StringElement(this, new IconData(), "Finished in Value", "finishedInValue"));
		settingsElements.add(new StringElement(this, new IconData(), "Finished Value", "finishedValue"));
	}

	/** {@inheritDoc} */
	@Override
	public String getDisplayValue() {
		if (!autoStats.isEnabled()) return this.disabledValue;
		if (!autoStats.extensionManager.isSupported()) return unsupportedValue;
		if (autoStats.extensionManager.isStopped()) return stoppedValue;

		float seconds = autoStats.extensionManager.getFinishedIn();
		if (seconds <= 0) return finishedValue;

		int minutes = (int) seconds / 60;
		seconds %= 60;

		String min = String.format("%02d", minutes);
		DecimalFormat df = new DecimalFormat("00.0", new DecimalFormatSymbols(Locale.ENGLISH));
		String sec = df.format(seconds);

		return finishedInValue + min + ":" + sec;
	}

	/** {@inheritDoc} */
	@Override
	public IconData getIconData() {
		return new IconData("textures/blocks/command_block.png");
	}

	/** {@inheritDoc} */
	@Override
	public String getDescription() {
		return "Shows you what AutoStats is doing.";
	}

	/** {@inheritDoc} */
	@Override
	public String getSettingName() {
		return "AutoStats State";
	}

	/** {@inheritDoc} */
	@Override
	public String getDisplayName() {
		return label;
	}

	/** {@inheritDoc} */
	@Override
	public String getDefaultValue() {
		return getDisplayValue();
	}

	/** {@inheritDoc} */
	@Override
	public String getControlName() {
		return getSettingName();
	}

	/** {@inheritDoc} */
	@Override
	public int getSortingId() {
		return 0;
	}

}
