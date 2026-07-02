package de.spiritscorp.DataSync;
/*
	Data Sync
		Application to synchronize your data

	@author Tom Spirit
	@date 16.12.2021
	@version	1.1.0.0-alpha
	@email tomspirit@spiritscorp.network

	Copyright ©

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.PreferenceManager;
import javafx.application.Application;

/**
 * Main application entry point responsible for runtime arguments parsing,
 * debug subsystems orchestration, and boots-strapping the JavaFX platform lifecycle.
 *
 * @author Tom Spirit
 */
@SuppressWarnings( { "PMD.LongVariable" } )
public final class Main { // NOPMD ShortClassName

	/**
	 * The current version of the application in semantic format,
	 * including the development stage (e.g., alpha).
	 */
	public static final String VERSION = "V 1.1.0.0-alpha";

	/**
	 * The timeout limit in milliseconds for regular, worker threads
	 * before a forced termination is triggered.
	 */
	public static final int THREAD_TIMEOUT = 15_000;

	/**
	 * The timeout limit in milliseconds for asynchronous background processes
	 * (e.g., automated file or task scans).
	 */
	public static final int BACKGROUND_THREAD_TIMEOUT = 5_000;

	/**
	 * The delay time in seconds used for displaying or fading out
	 * status and informational messages within the GUI.
	 */
	public static final int INFO_DELAY = 4;

	/** Config folder flag long */
	public static final String CONFIG_DIR_LONG = "--config-dir";
	/** Config folder flag short */
	public static final String CONFIG_DIR_SHORT = "-c";
	/** Boot delay flag long */
	public static final String BOOT_DELAY_LONG = "--boot-delay";
	/** Boot Delay flag short */
	public static final String BOOT_DELAY_SHORT = "-b";
	/** Debug flag long */
	public static final String DEBUG_LONG = "--debug";
	/** Debug flag short */
	public static final String DEBUG_SHORT = "-d";
	/** Debug to file flag long */
	public static final String DEBUG_TO_FILE_LONG = "--debug-to-file";
	/** Debug to file flag short */
	public static final String DEBUG_TO_FILE_SHORT = "-f";

	/** Debug mode */
	private static boolean debug;
	/** Boot delay mode */
	private static boolean firstStart;
	/** Helper to jump over the next argument if config dir needs 2 */
	private static boolean folderJumpArg;
	/** Will be set debug to file */
	private static boolean toFile;

	/**
	 * Application entry point. Orchestrates the initial boot sequence by parsing
	 * command-line options and bootstrapping the underlying JavaFX application subsystem.
	 * <p>
	 * This method delegates argument parsing to {@link #parseArguments(String[])} before
	 * handing over control to the JavaFX application lifecycle via {@link Application#launch(Class, String...)}.
	 * </p>
	 *
	 * @param args Runtime command-line execution flags and configuration parameters.
	 */
	public static void main( final String[] args ) {
		parseArguments( args );
		Application.launch( Gui.class, args );
	}

	/**
	 * Checks whether the application was launched automatically by the operating system's
	 * startup/autostart routine.
	 * <p>
	 * When {@code true}, a timer delay is initialized to reduce system resource contention
	 * during OS boot, and the application is instructed to start minimized in the background.
	 * This flag is managed and set automatically during the autostart registration process.
	 *
	 * @return {@code true} if the application was triggered via OS autostart;
	 *         {@code false} if it was started manually by the user.
	 */
	public static boolean isFirstStart() { return firstStart; }

	/**
	 * Checks whether the debug mode is active for extended verbose and additional runtime diagnostic outputs.
	 *
	 * @return {@code true} if advanced diagnostic information should be emitted;
	 *         {@code false} otherwise.
	 */
	public static boolean isDebug() { return debug; }

	/**
	 * Checks whether the debug mode is active for extended verbose and additional runtime diagnostic outputs.
	 *
	 * @return {@code true} if advanced diagnostic information should be emitted;
	 *         {@code false} otherwise.
	 */
	public static boolean isDebugToFile() { return toFile; }

	/**
	 * Evaluates and processes runtime command-line arguments in a single pass to configure
	 * global application states and subsystem parameters.
	 * <p>
	 * The parser evaluates standard flags for debugging, diagnostic routing, execution delays,
	 * and configuration root directory adjustments. For key-value configurations, it supports
	 * both standard inline assignment (e.g., {@code --config-dir=/path}) and safe whitespace
	 * lookahead token isolation (e.g., {@code -c /path}), ensuring subsequent flags are not
	 * accidentally consumed as paths.
	 * </p>
	 *
	 * @param args An array of string arguments passed to the application upon startup.
	 *             Null elements within the array are safely ignored.
	 */
	/*package*/ static void parseArguments( final String... args ) {

		final PreferenceManager manager = PreferenceManager.getInstance();

		// Single-pass argument processing to minimize iteration overhead
		for( int i = 0; i < args.length; i++ ) {
			if( args[i] == null || folderJumpArg ) {
				folderJumpArg = false;
				continue;
			}
			evaluateArgumentFlags( args, i, manager );
		}
		if( toFile ) Debug.setDebugToFile();
		// Initialize debug diagnostics if debog is enabled
		if( debug ) initializeDebugDiagnostics( manager.getConfigPath() );
	}

	/**
	 * Resets the global execution states and diagnostic tracking flags to their
	 * initial default values.
	 * <p>
	 * This helper method is intended exclusively for test isolation purposes (e.g., within
	 * {@code @BeforeEach} setup methods) to clear out internal static modifications
	 * between consecutive test executions and guarantee a deterministic environment.
	 * </p>
	 */
	/*package*/ static void resetForTesting() {
		debug = false;
		firstStart = false;
	}

	/**
	 * Evaluates individual argument flags and updates global execution variables.
	 */
	private static void evaluateArgumentFlags( final String[] args, final int currentIndex, final PreferenceManager manager ) {
		final String arg = args[currentIndex].trim();
		final String generalArg = arg.toLowerCase( Locale.ROOT );
		switch( generalArg ) {
			// Modern multi-labels: replaces the logical OR (||) chains
			case BOOT_DELAY_LONG, BOOT_DELAY_SHORT ->
				firstStart = true;

			case DEBUG_LONG, DEBUG_SHORT ->
				debug = true;

			case DEBUG_TO_FILE_LONG, DEBUG_TO_FILE_SHORT -> {
				toFile = true;
				debug = true;
			}

			// Pattern Matching with a 'when' guard to handle the .startsWith() logic
			case final String genArg when genArg.startsWith( CONFIG_DIR_LONG ) || genArg.startsWith( CONFIG_DIR_SHORT ) ->
				handleConfigDirectoryArgument( args, currentIndex, manager );

			default -> {
				// No match found, skip silently
			}
		}
	}

	/**
	 * Handles the logic for extracting and setting the configuration directory path.
	 *
	 * @param args         The command-line arguments.
	 * @param currentIndex The current index in the arguments array.
	 * @param manager      The preference manager instance.
	 */
	private static void handleConfigDirectoryArgument( final String[] args, final int currentIndex, final PreferenceManager manager ) {
		final String arg = args[currentIndex].trim();
		final String generalArg = arg.toLowerCase( Locale.ROOT );
		String configFolder = "";
		if( generalArg.contains( "=" ) ) {
			configFolder = arg.substring( arg.indexOf( '=' ) + 1 );
		}else if( currentIndex + 1 < args.length && !args[currentIndex + 1].startsWith( "-" ) ) {
			// Safeguard: Only consume next argument if it's not another flag
			configFolder = args[currentIndex + 1].trim();
			folderJumpArg = true;
		}
		if( !configFolder.isBlank() ) {
			manager.initGlobalRootConfigPath( Path.of( configFolder ) );
		}
	}

	/**
	 * Initializes debug diagnostics with application information.
	 *
	 * @param manager The preference manager instance.
	 */
	private static void initializeDebugDiagnostics( final Path configPath ) {
		Debug.printDebugTimeless( "%nDEBUG BEGIN -> [%s]: %s",
				System.getProperty( "app.instance.name", "Standard Instance" ),
				LocalDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL, FormatStyle.SHORT ) ) );
		Debug.printDebug( "[Info] Data Sync Application initialized. Beginning system initialization." );
		Debug.printDebug( "[Setup] Set config root path -> %s", configPath.toString() );
	}

}
