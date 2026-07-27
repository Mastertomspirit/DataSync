package de.spiritscorp.datasync.io;

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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import de.spiritscorp.datasync.Main;

/**
 * Utility class providing centralized logging capabilities for application diagnostics, errors, and exception tracking.
 * All outputs are automatically enriched with a timestamp and the active application instance name.
 *
 * @author Tom Spirit
 * @version 1.2.0
 */
@SuppressWarnings( { "java:S106" } )
public final class Debug {

	/**
	 * The unique name of this application instance, used as a prefix in log outputs.
	 * <p>
	 * The value is dynamically retrieved from the system property {@code app.instance.name} at startup.
	 * If this property is not defined, it falls back to the default value "Standard Instance".
	 * </p>
	 */
	private static final String INSTANCE_NAME = System.getProperty( "app.instance.name", "Standard Instance" );

	/** Reference to the preference MANAGER configuration singleton. */
	private static final PreferenceManager MANAGER = PreferenceManager.getInstance();

	/** Temporary bootstrap buffer holding early standard output diagnostic messages captured during the log rotation phase. */
	private static final List<String> DEBUG_BUFFER = new ArrayList<>();

	/** Temporary bootstrap buffer holding early standard error diagnostic messages captured during the log rotation phase. */
	private static final List<String> ERROR_BUFFER = new ArrayList<>();

	/** State flag indicating whether diagnostic messages should be staged in memory buffers rather than written directly to the active streams. */
	private static boolean isBuffering;

	/** State flag indicating whether the logging environment and log rotation have been initialized. */
	private static boolean isInitialized;

	/** Private constructor to prevent instantiation of this static utility class. */
	private Debug() {
	}

	/**
	 * Initializes the logging subsystem by executing log rotation on the provided paths.
	 * <p>
	 * This method ensures idempotent execution; if the framework is already initialized,
	 * subsequent calls will return immediately without repeating the rotation.
	 *
	 * @param debugPath the path to the diagnostic debug log file
	 * @param errorPath the path to the error log file
	 */
	public static void initDebugToFile( final Path debugPath, final Path errorPath ) {
		if( !isInitialized ) {
			isBuffering = true; // NOPMD
			final Logrotater logrotater = new Logrotater( MANAGER.getMaxLogSize(), MANAGER.getMaxLogCount() );
			logrotater.executeLogRotationIfNeeded( debugPath );
			logrotater.executeLogRotationIfNeeded( errorPath );
			isBuffering = false;
			isInitialized = true;
		}
	}

	/**
	 * Writes a formatted debugging message to the standard output stream (stdout).
	 * The output is automatically prefixed with a current timestamp and the running instance name.
	 *
	 * @param format A format string as described in {@link java.util.Formatter} syntax
	 * @param args   Arguments referenced by the format specifiers in the format string. If there are more arguments than format specifiers,
	 *               the extra arguments are ignored. The number of arguments is variable and may be zero. The maximum number of arguments is limited
	 *               by the maximum dimension of a Java array as defined by The Java Virtual Machine Specification.
	 */
	public static void printDebug( final String format, final Object... args ) {
		print( System.out, format, args );
	}

	/**
	 * Writes a formatted error message to the standard error stream (stderr).
	 * The output is automatically prefixed with a current timestamp and the running instance name.
	 *
	 * @param format A format string as described in {@link java.util.Formatter} syntax
	 * @param args   Arguments referenced by the format specifiers in the format string. If there are more arguments than format specifiers,
	 *               the extra arguments are ignored. The number of arguments is variable and may be zero. The maximum number of arguments is limited
	 *               by the maximum dimension of a Java array as defined by The Java Virtual Machine Specification.
	 */
	public static void printError( final String format, final Object... args ) {
		print( System.err, format, args );
	}

	/**
	 * Logs or prints detailed diagnostic information about a caught exception.
	 * <p>
	 * This utility method formats and outputs the primary exception message,
	 * inspects and logs the root cause if present to prevent {@link NullPointerException},
	 * and extracts the complete formatted stack trace for deep debugging.
	 * <p>
	 * *
	 * <b>Example Output Structure:</b>
	 *
	 * <pre>
	 * Exception in Class: [com.example.MyService]:Message -> Connection failed
	 * ↳ Cause: java.net.ConnectException -> Message: Connection refused
	 * Full Info:
	 * java.lang.RuntimeException: Connection failed
	 * at com.example.MyService.start(MyService.java:24)
	 * ...
	 * <p>
	 *
	 * @param clazz     the {@link Class} context where the exception was caught, used for identification
	 * @param exception the {@link Exception} instance containing the error details and stack trace
	 */
	public static void printException( final Class<?> clazz, final Exception exception ) {
		printError( "Exception in Class: [%s]: Message -> %s", clazz.getName(), exception.getMessage() );
		if( exception.getCause() != null ) {
			printError( "  ↳ Cause: %s -> Message: %s", exception.getCause().getClass().getName(), exception.getCause().getMessage() );
		}
		final StringWriter writer = new StringWriter();
		exception.printStackTrace( new PrintWriter( writer ) );
		printError( "Full Info:%n%s", writer.toString() );
	}

	/**
	 * A debug method to write a formatted string to this output stream using the specified format string and arguments.
	 *
	 * @param format A format string as described in {@link java.util.Formatter} syntax
	 * @param args   Arguments referenced by the format specifiers in the format string. If there are more arguments than format specifiers,
	 *               the extra arguments are ignored. The number of arguments is variable and may be zero. The maximum number of arguments is limited
	 *               by the maximum dimension of a Java array as defined by The Java Virtual Machine Specification.
	 */
	@SuppressWarnings( { "java:S3457" } )
	public static void printDebugTimeless( final String format, final Object... args ) {
		if( Main.isDebug() ) {
			System.out.printf( format + "%n", args ); // NOPMD
		}
	}

	/**
	 * Redirects standard output ({@code System.out}) and standard error ({@code System.err})
	 * streams to their respective designated local log files.
	 * <p>
	 * If the logging system has not been initialized yet, this method automatically triggers
	 * the log rotation sequence first. Once both file streams are successfully established,
	 * any diagnostic messages captured in the memory buffers during the rotation phase are
	 * immediately flushed to their corresponding destination files.
	 */
	@SuppressWarnings( {
			"PMD.SystemPrintln", "PMD.AvoidPrintStackTrace", "PMD.CloseResource", // PMD
			"java:S4507", "java:S2095" // SonarLint
	} )
	public static void setDebugToFile() {
		final Path debugPath = MANAGER.getDebugPath();
		final Path errorPath = MANAGER.getErrorPath();
		if( !isInitialized ) {
			initDebugToFile( debugPath, errorPath );
		}
		try {
			if( !Files.exists( debugPath ) ) Files.createDirectories( debugPath.getParent() );
			final PrintStream outStream = new PrintStream(
					Files.newOutputStream(
							debugPath,
							StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND ),
					true, StandardCharsets.UTF_8 );
			final PrintStream errStream = new PrintStream(
					Files.newOutputStream(
							errorPath,
							StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND ),
					true, StandardCharsets.UTF_8 );
			System.setOut( outStream );
			System.setErr( errStream );

			if( !DEBUG_BUFFER.isEmpty() ) {
				for( final String log : DEBUG_BUFFER ) {
					outStream.print( log );
				}
				DEBUG_BUFFER.clear();
			}
			if( !ERROR_BUFFER.isEmpty() ) {
				for( final String log : ERROR_BUFFER ) {
					errStream.print( log );
				}
				ERROR_BUFFER.clear();
			}
		}catch( final IOException exception ) {
			System.err.println( exception.getMessage() );
			exception.printStackTrace();
		}
	}

	/**
	 * Formats and dispatches a diagnostic message to the specified target stream.
	 * <p>
	 * If diagnostic mode is enabled, the message is automatically prefixed with the
	 * current system timestamp and the running instance name. While the framework is
	 * in its initialization phase ({@code isBuffering == true}), the fully formatted
	 * line is diverted into the appropriate memory buffer based on the target stream's
	 * identity to prevent file access conflicts during log rotation.
	 *
	 * @param stream the target {@link PrintStream} (typically {@code System.out} or {@code System.err})
	 * @param format a format string conforming to {@link java.util.Formatter} syntax
	 * @param args   arguments referenced by the format specifiers in the format string
	 */
	private static void print( final PrintStream stream, final String format, final Object... args ) {
		if( Main.isDebug() ) {
			final String time = LocalDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "dd-MM-yyyy HH:mm:ss.SSSSS" ) );
			final String message = String.format( format, args );
			final String finalMessage = String.format( "%s [ %s ] %s%n", time, INSTANCE_NAME, message );
			if( isBuffering ) {
				if( stream == System.out ) { // NOPMD Intentional reference comparison
					DEBUG_BUFFER.add( finalMessage );
				}else if( stream == System.err ) { // NOPMD Intentional reference comparison
					ERROR_BUFFER.add( finalMessage );
				}
			}else {
				stream.print( finalMessage );
			}
		}
	}
}
