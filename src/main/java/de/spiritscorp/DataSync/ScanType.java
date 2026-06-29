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
 * Defines the available scanning modes and execution strategies for the file processing pipeline.
 * <p>
 * This enum maps specific processing types (such as standard synchronization, shallow scans,
 * deep cryptographic checksum validations, or duplicate identification) to their localized,
 * human-readable descriptions.
 *
 */
public enum ScanType {

	/** Standard synchronization mode comparing file metadata between targets. */
	SYNCHRONIZE( "Synchronisieren"),

	/** Shallow scan mode that only evaluates root level or basic file availability. */
	FLAT_SCAN( "flacher Scan"),

	/** Intensive scan mode processing deep file structures, modification timestamps, or checksums. */
	DEEP_SCAN( "tiefer Scan"),

	/** Dedicated analysis mode used exclusively to detect and aggregate identical file duplicates. */
	DUBLICATE_SCAN( "Dublikate suchen");

	/**
	 * The description about a scan type
	 */
	private final String description;

	/**
	 * Internal constructor to initialize the scan type with its display text.
	 *
	 * @param description the localized, human-readable description of the scanning strategy
	 */
	ScanType( final String description ) {
		this.description = description;
	}

	/**
	 * Extracts and retrieves the localized descriptions of all available scanning modes.
	 * <p>
	 * This array is typically utilized to populate UI components like combo boxes or selection menus.
	 *
	 * @return a String array containing the human-readable description texts in order of definition
	 */
	public static String[] getAllDescriptions() {
		final String[] ret = new String[values().length];
		int index = 0;
		for( final ScanType s : values() ) {
			ret[index] = s.getDescription();
			index++;
		}
		return ret;
	}

	/**
	 * Resolves a matching scanning configuration based on its localized description text.
	 *
	 * @param str the description string to look up (e.g., "tiefer Scan")
	 * @return the matching ScanType constant, or null if no matching description is found
	 */
	public static ScanType get( final String str ) {
		for( final ScanType st : values() ) {
			if( str.equals( st.getDescription() ) ) return st;
		}
		return null;
	}

	/**
	 * Gets the localized description text associated with this scanning mode.
	 *
	 * @return the human-readable scanning strategy text
	 */
	public String getDescription() { return description; }
}
