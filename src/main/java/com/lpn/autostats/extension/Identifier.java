package com.lpn.autostats.extension;

import com.lpn.autostats.AutoStats;

/**
 * Accesses {@link StateInfo} to check if a extension is supported
 * 
 * @author Sirvierl0ffel
 */

public abstract class Identifier {

	/** AutoStats instance */
	protected final AutoStats autoStats;

	/** StateInfo instance */
	protected final StateInfo stateInfo;

	/** Just creates {@link Identifier} instance */
	public Identifier() {
		autoStats = AutoStats.instance();
		stateInfo = autoStats.stateInfo;
	}

	/** @return true, if the current state matches */
	public abstract boolean test();

	/** Makes a chain of identifiers, all of them has to be true */
	public static class And extends Identifier {

		private final Identifier[] identifiers;

		/**
		 * Makes a chain of identifiers, all of them has to be true
		 * 
		 * @param identifiers
		 */
		public And(Identifier... identifiers) {
			this.identifiers = identifiers;
		}

		/** {@inheritDoc} */
		@Override
		public boolean test() {
			for (Identifier identifier : identifiers) {
				if (!identifier.test()) return false;
			}
			return true;
		}

	}

	/**
	 * Makes a chain of identifiers, one of them has to be true
	 */
	public static class Or extends Identifier {

		private final Identifier[] identifiers;

		/**
		 * Makes a chain of identifiers, one of them has to be true
		 * 
		 * @param identifiers
		 */
		public Or(Identifier... identifiers) {
			this.identifiers = identifiers;
		}

		/** {@inheritDoc} */
		@Override
		public boolean test() {
			for (Identifier identifier : identifiers) {
				if (identifier.test()) return true;
			}
			return false;
		}

	}

	/**
	 * Identifies the IPs of a server
	 */
	public static class IP extends Identifier {

		private final String ip;

		/**
		 * Identifies the IPs of a server
		 * 
		 * @param ips the IP of the server to identify
		 */
		public IP(String ip) {
			this.ip = ip;
		}

		/** {@inheritDoc} */
		@Override
		public boolean test() {
			String currentIP = stateInfo.getIP();
			return ip.equalsIgnoreCase(currentIP);
		}

	}

	/**
	 * Identifies the tab list of a server
	 * 
	 * @author Sirvierl0ffel
	 */
	public static class Tab extends Identifier {

		private final String head, foot;
		private final boolean regex;

		/**
		 * Identifies the tab list of a server
		 * 
		 * @param head  the head the tab list has to be
		 * @param foot  the foot the tab list has to be
		 * @param regex if these strings are <a href=
		 *              "https://en.wikipedia.org/wiki/Regular_expression">regular
		 *              expressions</a>
		 */
		public Tab(String head, String foot, boolean regex) {
			this.head = head;
			this.foot = foot;
			this.regex = regex;
		}

		/** {@inheritDoc} */
		@Override
		public boolean test() {
			String currentHead = stateInfo.getTabHead();
			String currentFoot = stateInfo.getTabFoot();

			if (regex ? !currentHead.matches(head) : !currentHead.equals(head)) return false;
			if (regex ? !currentFoot.matches(foot) : !currentFoot.equals(foot)) return false;

			return true;
		}

	}

}
