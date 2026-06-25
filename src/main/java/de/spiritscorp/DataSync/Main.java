package de.spiritscorp.DataSync;
/*
	DataSync
	Application to synchronize your data

	@author Tom Spirit
	@date 16.12.2021
	@version	1.1.0.0-alpha

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, @see <http://www.gnu.org/licenses/>.
*/

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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
public class Main {

	/**
	 * The current version of the application in semantic format,
	 * including the development stage (e.g., alpha).
	 */
	public static final String VERSION = "V 1.1.0.0-alpha";

	/**
	 * The timeout limit in milliseconds for regular, worker threads
	 * before a forced termination is triggered.
	 */
	public static final int THREAD_TIMEOUT = 15000;

	/**
	 * The timeout limit in milliseconds for asynchronous background processes
	 * (e.g., automated file or task scans).
	 */
	public static final int BACKGROUND_THREAD_TIMEOUT = 5000;

	/**
	 * The delay time in seconds used for displaying or fading out
	 * status and informational messages within the GUI.
	 */
	public static final int INFO_DELAY = 4;

	private static final String CONFIG_DIR_LONG = "--config-dir";
	private static final String CONFIG_DIR_SHORT = "-c";
	private static final String BOOT_DELAY_LONG = "--boot-delay";
	private static final String BOOT_DELAY_SHORT = "-b";
	private static final String DEBUG_LONG = "--debug";
	private static final String DEBUG_SHORT = "-d";
	private static final String DEBUG_TO_FILE_LONG = "--debug-to-file";
	private static final String DEBUG_TO_FILE_SHORT = "-f";

	private static boolean debug = false;
	private static boolean firstStart = false;

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
	public static void main( String[] args ) {
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
	static void parseArguments( String[] args ) {
		String configFolder = "";

		// Single-pass argument processing to minimize iteration overhead
		for( int i = 0; i < args.length; i++ ) {
			if( args[i] == null ) {
				continue;
			}
			final String arg = args[i].trim();
			final String generalArg = arg.toLowerCase();

			// Handle configuration directory configuration
			if( generalArg.startsWith( CONFIG_DIR_LONG ) || generalArg.startsWith( CONFIG_DIR_SHORT ) ) {
				if( generalArg.contains( "=" ) ) {
					configFolder = arg.substring( arg.indexOf( '=' ) + 1 );
				}else if( i + 1 < args.length && !args[i + 1].startsWith( "-" ) ) {
					// Safeguard: Only consume next argument if it's not another flag
					configFolder = args[++i].trim();
				}
				if( !configFolder.isBlank() ) {
					PreferenceManager.initGlobalRootConfigPath( Path.of( configFolder ) );
				}
			}

			// Handle boot delay initialization flag
			else if( generalArg.equals( BOOT_DELAY_LONG ) || generalArg.equals( BOOT_DELAY_SHORT ) )
				firstStart = true;

			// Handle standard console debug logging flag
			else if( generalArg.equals( DEBUG_LONG ) || generalArg.equals( DEBUG_SHORT ) )
				debug = true;

			// Handle file-based debug redirection flag
			else if( generalArg.equals( DEBUG_TO_FILE_LONG ) || generalArg.equals( DEBUG_TO_FILE_SHORT ) ) {
				debug = true;
				Debug.setDebugToFile();
			}
		}
		// Initialize debug diagnostics if debog is enabled
		if( debug ) {
			Debug.printDebugTimeless( "%nDEBUG BEGIN -> [%s]: %s",
					System.getProperty( "app.instance.name", "Standard Instance" ),
					LocalDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL, FormatStyle.SHORT ) ) );
			Debug.printDebug( "[Info] Data Sync Application initialized. Beginning system initialization." );
			Debug.printDebug( "[Setup] Set config root path -> %s", PreferenceManager.getInstance().getConfigPath().toString() );
		}
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
	static void resetForTesting() {
		debug = false;
		firstStart = false;
	}
}
