/*
 		DataSync Application

		@author Tom Spirit

		This program is free software; you can redistribute it and/or modify
		it under the terms of the GNU General Public License as published by
		the Free Software Foundation; either version 3 of the License, or
		(at your option) any later version.

		This program is distributed in the hope that it will be useful,
		but WITHOUT ANY WARRANTY; without even the implied warranty of
		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
		GNU General Public License for more details.

		You should have received a copy of the GNU General Public License
		along with this program; if not, write to the Free Software Foundation,
		Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package de.spiritscorp.DataSync;

/**
 * Defines the background execution intervals and their corresponding internal
 * validation check timers for the synchronization tasks.
 * <p>
 * This enum maps human-readable interval names to explicit millisecond values,
 * allowing the background scheduler to determine when a synchronization run is due
 * and how frequently it should verify the execution state.
 *
 */
public enum BgTime {

	/** Execution interval of 1 minute. */
	MIN_1(60000L, "1 Minute", 5000L),

	/** Execution interval of 5 minutes. */
	MIN_5(300000L, "5 Minuten", 10000L),

	/** Execution interval of 30 minutes. */
	MIN_30(1800000L, "30 Minuten", 60000L),

	/** Execution interval set to occur hourly. */
	HOURLY(3600000L, "Stündlich", 300000L),

	/** Execution interval set to occur daily. */
	DAYLY(86400000L, "Täglich", 1800000L),

	/** Execution interval set to occur weekly. */
	WEEKLY(604800000L, "Wöchentlich", 1800000L),

	/** Execution interval set to occur monthly. */
	MONTHLY(2592000000L, "Monatlich", 1800000L);

	private final long time;
	private final String name;
	private final long checkTime;

	/**
	 * Internal constructor to initialize the background time configurations.
	 *
	 * @param time      the total duration of the interval represented in milliseconds
	 * @param name      the localized, human-readable display name of the interval
	 * @param checkTime the thread sleep or verification check cycle duration in milliseconds
	 */
	private BgTime(long time, String name, long checkTime) {
		this.time = time;
		this.name = name;
		this.checkTime = checkTime;
	}

	/**
	 * Extracts and retrieves the localized display names of all available background intervals.
	 * <p>
	 * This array is typically utilized to populate UI dropdown menus or configuration listings.
	 *
	 * @return a String array containing the human-readable names in the exact order of definition
	 */
	public static String[] getNames() {
		final String[] str = new String[BgTime.values().length];
		int i = 0;
		for (final BgTime bg : BgTime.values()) {
			str[i] = bg.getName();
			i++;
		}
		return str;
	}

	/**
	 * Resolves a matching background time configuration based on its localized display name.
	 *
	 * @param str the display name string to look up (e.g., "5 Minuten")
	 * @return the matching BgTime constant, or null if no matching name is found
	 */
	public static BgTime get(String str) {
		for (final BgTime st : BgTime.values()) {
			if (str.equals(st.getName())) return st;
		}
		return null;
	}

	/**
	 * Gets the total execution interval duration.
	 *
	 * @return the interval time value represented in milliseconds
	 */
	public long getTime() { return time; }

	/**
	 * Gets the localized display name of this interval configuration.
	 *
	 * @return the human-readable interval text
	 */
	public String getName() { return name; }

	/**
	 * Gets the specific loop cycle or validation check interval duration.
	 *
	 * @return the check cycle time value represented in milliseconds
	 */
	public long getCheckTime() { return checkTime; }
}
