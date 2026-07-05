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
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import de.spiritscorp.datasync.Main;

/**
 * Utility class providing centralized logging capabilities for application diagnostics,
 * errors, and exception tracking. All outputs are automatically enriched with a timestamp
 * and the active application instance name.
 */
public final class Debug {

	/**
	 * The unique name of this application instance, used as a prefix in log outputs.
	 * <p>
	 * The value is dynamically retrieved from the system property {@code app.instance.name} at startup.
	 * If this property is not defined, it falls back to the default value "Standard Instance".
	 * </p>
	 */
	private static final String INSTANCE_NAME = System.getProperty( "app.instance.name", "Standard Instance" );

	private Debug() {
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
	@SuppressWarnings( { "java:S3457", "java:S106" } )
	public static void printDebugTimeless( final String format, final Object... args ) {
		if( Main.isDebug() ) {
			System.out.printf( format + "%n", args ); // NOPMD
		}
	}

	/**
	 * Redirects diagnostic logging output streams to a local file.
	 * Once invoked, messages processed by this utility will be appended to the designated
	 * log file infrastructure instead of solely printing to the console.
	 */
	@SuppressWarnings( {
			"PMD.SystemPrintln", "PMD.AvoidPrintStackTrace", // PMD
			"java:S106", "java:S4507" // SonarLint
	} )
	public static void setDebugToFile() {
		try {
			if( !Files.exists( PreferenceManager.getInstance().getDebugPath() ) ) Files.createDirectories( PreferenceManager.getInstance().getDebugPath().getParent() );
			System.setOut( new PrintStream(
					Files.newOutputStream(
							PreferenceManager.getInstance().getDebugPath(),
							StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND ),
					true, StandardCharsets.UTF_8 ) );
			System.setErr( new PrintStream(
					Files.newOutputStream(
							PreferenceManager.getInstance().getErrorPath(),
							StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND ),
					true, StandardCharsets.UTF_8 ) );
		}catch( final IOException e ) {
			System.err.println( e.getMessage() );
			e.printStackTrace();
		}
	}

	private static void print( final PrintStream stream, final String format, final Object... args ) {
		if( Main.isDebug() ) {
			final String time = LocalDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "dd-MM-yyyy HH:mm:ss.SSSSS" ) );
			final String message = String.format( format, args );
			stream.printf( "%s [ %s ] %s%n", time, INSTANCE_NAME, message );
		}
	}
}
