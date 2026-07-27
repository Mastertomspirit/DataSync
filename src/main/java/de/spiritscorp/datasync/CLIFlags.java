package de.spiritscorp.datasync;

/*-
 * 		Data Sync
 *
 * 		Copyright ©   2022    The Spirit
 * 		@email                        thespirit@spiritscorp.network
 *
 * 		This program is free software; you can redistribute it and/or modify
 * 		it under the terms of the GNU General Public License as published by
 * 		the Free Software Foundation; either version 3 of the License, or
 * 		(at your option) any later version.
 *
 * 		This program is distributed in the hope that it will be useful,
 * 		but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 		See the GNU General Public License for more details.
 *
 * 		You should have received a copy of the GNU General Public License
 * 		along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Locale;

/**
 * Defines the supported command-line interface (CLI) switches and arguments
 * utilized to configure the application's runtime environment during the bootstrapping phase.
 *
 * @author Tom Spirit
 * @version 1.0.0
 */
public enum CLIFlags {

	/** Command-line argument to specify a custom directory path hosting configuration files, profiles, and sync maps. */
	CONFIG_DIR( "--config-dir", "-c" ),
	/** Command-line flag to enforce an extended initialization pause during system boot sequences to prevent resource contention. */
	BOOT_DELAY( "--boot-delay", "-b" ),
	/** Command-line flag to activate verbose diagnostic logging streams to the standard console output. */
	DEBUG( "--debug", "-d" ),
	/** Command-line flag to mirror or redirect diagnostic debug streams into a local file system log repository. */
	DEBUG_TO_FILE( "--debug-to-file", "-f" ),
	/** Fallback flag used when no matching command-line argument is detected to prevent NullPointerExceptions. */
	NONE( "", "" );

	/** The verbose, multi-character command-line argument identifier. */
	private final String longFlag;
	/** The single-character mnemonic shortcut alias for rapid terminal execution. */
	private final String shortFlag;

	/**
	 * Constructs a new command-line flag definition mapped to its corresponding terminal input literals.
	 *
	 * @param longFlag  The verbose argument identifier starting with a double dash
	 * @param shortFlag The single-character mnemonic alias starting with a single dash
	 */
	CLIFlags( final String longFlag, final String shortFlag ) {
		this.longFlag = longFlag;
		this.shortFlag = shortFlag;
	}

	/**
	 * Resolves a raw command-line argument string to its corresponding strongly-typed {@link CLIFlags} representation.
	 * <p>
	 * This method serves as the central factory for command-line argument mapping and employs a defensive,
	 * null-safe lookup strategy. It supports exact match evaluation for standardized flags as well as
	 * prefix-based evaluation to handle parameterized switches (such as directory paths attached directly
	 * to the configuration argument).
	 * </p>
	 * <p>
	 * If the input string is {@code null}, blank, or fails to match any registered application switch,
	 * this method returns {@link #NONE} to ensure safe downstream processing without the risk of
	 * encountering a {@link NullPointerException}.
	 * </p>
	 *
	 * @param arg the raw terminal argument to evaluate (can be {@code null} or blank)
	 * @return the resolved {@link CLIFlags} enum constant, or {@link #NONE} as a robust fallback if unrecognized
	 */
	public static CLIFlags fromArgument( final String arg ) {
		if( arg == null || arg.isBlank() ) return NONE;
		for( final CLIFlags flag : values() ) {
			if( flag == NONE ) continue;
			final String lowArg = arg.toLowerCase( Locale.ROOT );
			if( flag.isExactMatch( lowArg ) || flag.isPrefixMatch( lowArg ) ) { return flag; }
		}
		return NONE;
	}

	/**
	 * Retrieves the verbose argument identifier prefix.
	 *
	 * @return The long flag string
	 */
	public String getLongFlag() { return longFlag; }

	/**
	 * Retrieves the single-character mnemonic alias prefix.
	 *
	 * @return The short flag string
	 */
	public String getShortFlag() { return shortFlag; }

	/**
	 * Checks for a standard, exact command-line switch match.
	 */
	private boolean isExactMatch( final String arg ) {
		return longFlag.equals( arg ) || shortFlag.equals( arg );
	}

	/**
	 * Checks for prefix-based argument structures (specifically restricted to the configuration directory path).
	 */
	private boolean isPrefixMatch( final String arg ) {
		return this == CONFIG_DIR && ( arg.startsWith( longFlag ) || arg.startsWith( shortFlag ) );
	}
}
