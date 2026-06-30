package de.spiritscorp.DataSync.Controller;
/*
Data Sync
	Application to synchronize your data

@author Tom Spirit
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
import java.util.Map;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.Model.FileAttributes;

/**
 * Formats raw synchronization metrics, file attributes, and processing durations
 * into localized, human-readable string representations tailored for the user interface.
 * <p>
 * This utility decouples presentation and text formatting logic from the core
 * background threading and data synchronization routines, ensuring a clean separation
 * of concerns.
 * </p>
 *
 * @author Tom Spirit
 */
@SuppressWarnings( "PMD.LongVariable" )
public class UiLogFormatter {

	// --- Conversion Constants for Byte Sizes ---
	/** The number of bytes required to reach a Gibibyte threshold. */
	private static final long BYTES_PER_GIB_THRESHOLD = 1_073_741_824L;
	/** The number of bytes required to reach a Mebibyte threshold. */
	private static final long BYTES_PER_MIB_THRESHOLD = 1_048_576L;
	/** The number of bytes required to reach a Kibibyte threshold. */
	private static final long BYTES_PER_KIB_THRESHOLD = 1_024L;
	/** Conversion divisor for calculating Mebibyte values. */
	private static final double CONVERSION_FACTOR_MIB = 1_048_576.0;
	/** Conversion divisor for calculating Kibibyte values. */
	private static final double CONVERSION_FACTOR_KIB = 1_024.0;
	/** Threshold above which the plural form "bytes" is used. */
	private static final long MINIMUM_BYTE_PLURAL_THRESHOLD = 1L;

	// --- Conversion Constants for Time Metrics ---
	/** Number of nanoseconds in a single standard second. */
	private static final double NANOSECONDS_PER_SECOND = 1_000_000_000.0;
	/** Total number of seconds representing a two-hour duration. */
	private static final int SECONDS_PER_TWO_HOURS = 7200;
	/** Total number of seconds representing a one-hour duration. */
	private static final int SECONDS_PER_ONE_HOUR = 3600;
	/** Total number of seconds representing a single minute. */
	private static final int SECONDS_PER_MINUTE = 60;

	// --- UI Layout and Processing Boundaries ---
	/** Maximum number of raw file entries displayed in the UI log text area. */
	private static final int DEFAULT_LOG_DISPLAY_LIMIT = 10_000;
	/** Initial number of character displayed in the UI log text area. */
	private static final int INITIAL_LOG_BUFFER_SIZE = 2_000;
	/** Divider used to calculate the display boundary loop limit. */
	private static final int DISPLAY_LIMIT_HALF_DIVIDER = 2;

	/**
	 * Converts a raw number of bytes into a human-readable string representation
	 * using binary prefixes (KiB, MiB, GiB).
	 *
	 * @param bytes the number of bytes to format
	 * @return a formatted string indicating the size with appropriate units
	 */
	String getReadableBytes( final long bytes ) {
		if( bytes > BYTES_PER_GIB_THRESHOLD ) {
			return String.format( "%.3f GiB", bytes / CONVERSION_FACTOR_MIB / CONVERSION_FACTOR_KIB );
		}else if( bytes > BYTES_PER_MIB_THRESHOLD ) {
			return ( bytes / BYTES_PER_MIB_THRESHOLD ) + " MiB";
		}else if( bytes > BYTES_PER_KIB_THRESHOLD ) {
			return ( bytes / BYTES_PER_KIB_THRESHOLD ) + " KiB";
		}else if( bytes > MINIMUM_BYTE_PLURAL_THRESHOLD ) {
			return bytes + " bytes";
		}else {
			return bytes + " byte";
		}
	}

	/**
	 * Formats a nanosecond duration into a human-readable runtime string
	 * partitioned into hours, minutes, and seconds.
	 *
	 * @param endTimeNano the elapsed duration in nanoseconds
	 * @return a localized string visualizing the total runtime
	 */
	String getEndTimeFormatted( final long endTimeNano ) {
		final double endTimeSec = endTimeNano / NANOSECONDS_PER_SECOND;
		if( endTimeSec >= SECONDS_PER_TWO_HOURS ) {
			return String.format( "%d Stunden %d Minuten %.3f Sekunden Laufzeit", ( (int) endTimeSec ) / SECONDS_PER_ONE_HOUR, ( (int) endTimeSec ) % SECONDS_PER_MINUTE,
					endTimeSec % SECONDS_PER_MINUTE );
		}else if( endTimeSec >= SECONDS_PER_ONE_HOUR ) {
			return String.format( "%d Stunde %d Minuten %.3f Sekunden Laufzeit", ( (int) endTimeSec ) / SECONDS_PER_ONE_HOUR, ( (int) endTimeSec ) % SECONDS_PER_MINUTE,
					endTimeSec % SECONDS_PER_MINUTE );
		}else if( endTimeSec >= SECONDS_PER_MINUTE ) {
			return String.format( "%d Minuten %.3f Sekunden Laufzeit", ( (int) endTimeSec ) / SECONDS_PER_MINUTE, endTimeSec % SECONDS_PER_MINUTE );
		}else {
			return String.format( "%.3f Sekunden Laufzeit", endTimeSec );
		}
	}

	/**
	 * Generates a fully structured and titled overview log text from the scanned file maps,
	 * tailoring headers and categories based on the given synchronization execution type.
	 *
	 * @param scanType  the synchronization or backup operational mode archetype
	 * @param sourceMap map of pending allocations determined on the source environment
	 * @param destMap   map of pending allocations determined on the target environment
	 * @param failMap   map of files that encountered access violations or are marked for deletion
	 * @return a comprehensive structured text summary report ready for text area visualization
	 */
	String formatMaps( final ScanType scanType, final Map<Path, FileAttributes> sourceMap, final Map<Path, FileAttributes> destMap, final Map<Path, FileAttributes> failMap ) {
		final String line = System.lineSeparator();
		final String scanFinish = "Scan abgeschlossen!\n";
		final String tableSep = "------------------------------\n";
		final StringBuilder stringBuilder = new StringBuilder( INITIAL_LOG_BUFFER_SIZE );

		if( scanType == ScanType.SYNCHRONIZE ) {
			stringBuilder.append( scanFinish ).append( "Zu kopieren in das Sourceverzeichnis:\n" ).append( tableSep );
			if( sourceMap != null ) {
				buildEntries( stringBuilder, sourceMap, DEFAULT_LOG_DISPLAY_LIMIT );
			}
			stringBuilder.append( line ).append( "Zu kopieren in das Zielverzeichnis:\n" ).append( tableSep );
			if( destMap != null ) {
				buildEntries( stringBuilder, destMap, DEFAULT_LOG_DISPLAY_LIMIT );
			}
			if( failMap != null && !failMap.isEmpty() ) {
				stringBuilder.append( line ).append( "Zu löschende Dateien:\n" ).append( tableSep );
				buildEntries( stringBuilder, failMap, DEFAULT_LOG_DISPLAY_LIMIT );
			}
		}else {
			stringBuilder.append( scanFinish ).append( "Zu kopierende Dateien:\n" ).append( tableSep );
			if( sourceMap != null ) {
				buildEntries( stringBuilder, sourceMap, DEFAULT_LOG_DISPLAY_LIMIT );
			}
			stringBuilder.append( line ).append( "Zu löschende Dateien:\n" ).append( tableSep );
			if( destMap != null ) {
				buildEntries( stringBuilder, destMap, DEFAULT_LOG_DISPLAY_LIMIT );
			}
			if( failMap != null && !failMap.isEmpty() ) {
				stringBuilder.append( line ).append( "Fehlerhafter Zugriff:\n" ).append( tableSep );
				buildEntries( stringBuilder, failMap, DEFAULT_LOG_DISPLAY_LIMIT );
			}
		}
		return stringBuilder.toString();
	}

	/**
	 * Iterates over a map of file attributes and appends a detailed structural text summary
	 * for each entry to the provided {@code StringBuilder} up to a designated threshold.
	 *
	 * @param stringBuilder the buffer to append the formatted lines to
	 * @param printableMap  the map containing file paths and their associated attributes
	 * @param displayLimit  the absolute boundary to prevent UI thread lockups during rendering
	 * @return the updated {@code StringBuilder} containing the appended logs
	 */
	private StringBuilder buildEntries( final StringBuilder stringBuilder, final Map<Path, FileAttributes> printableMap, final int displayLimit ) {
		final String valueSeparator = " , ";
		int index = 0;
		for( final Map.Entry<Path, FileAttributes> entry : printableMap.entrySet() ) {
			final FileAttributes value = entry.getValue();
			stringBuilder.append( value.getFileName() ).append( valueSeparator )
					.append( getReadableBytes( value.getSize() ) ).append( valueSeparator )
					.append( value.getModTimeString() ).append( valueSeparator )
					.append( value.getCreateTimeString() ).append( valueSeparator )
					.append( value.getFileHash() ).append( "     " )
					.append( entry.getKey().toString() )
					.append( '\n' );

			index++;
			if( index > ( displayLimit / DISPLAY_LIMIT_HALF_DIVIDER ) ) {
				break;
			}
		}
		return stringBuilder;
	}

}
