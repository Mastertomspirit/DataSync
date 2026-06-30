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
package de.spiritscorp.DataSync.IO;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import de.spiritscorp.DataSync.Model.FileAttributes;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;

/**
 * High-performance background logger using JSON Lines format (NDJSON).
 * Features an automated, size-based log rotation upon initialization.
 * Designed with Dependency Injection for optimal testability.
 */
public class Logger {

	/** Formatter for generating standardized German timestamp strings. */
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern( "dd.MM.yyyy  HH:mm:ss" );

	/** The absolute destination path of the active log file. */
	private final Path baseLogPath;

	/** The maximum threshold size in bytes before a file rotation is enforced. */
	private final long maxFileSize;

	/** The capacity limit of historical backup archives to keep on disk. */
	private final int maxBackupIndex;

	/** Thread-safe internal memory cache containing unwritten log entries. */
	private final List<JsonArray> logCache = new ArrayList<>();

	/** Concurrency lock ensuring atomic operations across background threads. */
	private final ReentrantLock threadLock = new ReentrantLock();

	/**
	 * Public default constructor utilizing production configurations.
	 * Fetches the default log path and enforces a 10 MB retention limit with 5 backups.
	 */
	public Logger() {
		this(
				PreferenceManager.getInstance().getLogPath(),
				10_485_760, // 10 MB
				5 );
	}

	/**
	 * Initializes the logger and executes an immediate size-based log rotation check.
	 *
	 * @param baseLogPath    The primary path to the active log file
	 * @param maxFileSize    The maximum allowed file size in bytes before rotation triggers
	 * @param maxBackupIndex The maximum number of archived log files to retain
	 * @throws NullPointerException if baseLogPath is null
	 */
	/* package */ Logger( final Path baseLogPath, final long maxFileSize, final int maxBackupIndex ) {
		this.baseLogPath = Objects.requireNonNull( baseLogPath, "baseLogPath must not be null" );
		this.maxFileSize = maxFileSize;
		this.maxBackupIndex = maxBackupIndex;

		executeLogRotationIfNeeded();
	}

	/**
	 * Sets a new log entry and queues it inside the volatile internal cache.
	 *
	 * @param filePath       The path where the file is/was located
	 * @param changeStatus   The status representing the change event
	 * @param fileAttributes The structural attributes of the file
	 */
	public void setEntry( final String filePath, final String changeStatus, final FileAttributes fileAttributes ) {
		threadLock.lock();
		try {
			final JsonObject jsonObject = Json.createObjectBuilder()
					.add( "Dateiname", fileAttributes.getFileName() )
					.add( "erstellt", fileAttributes.getCreateTimeString() )
					.add( "zuletzt modifiziert", fileAttributes.getModTimeString() )
					.add( "Größe", fileAttributes.getSize() )
					.add( "Fingerabdruck", ( fileAttributes.getFileHash() == null ) ? "null" : fileAttributes.getFileHash() )
					.build();

			final JsonArray jsonArray = Json.createArrayBuilder()
					.add( filePath )
					.add( LocalDateTime.now( ZoneId.systemDefault() ).format( DATE_FORMATTER ) )
					.add( changeStatus )
					.add( jsonObject )
					.build();

			logCache.add( jsonArray );
		}finally {
			threadLock.unlock();
		}
	}

	/**
	 * Writes the cached log entries to the file system using an efficient O(1) append strategy.
	 * Avoids loading existing files into memory, keeping the footprint static.
	 */
	public void printStatus() {
		threadLock.lock();
		try {
			if( logCache.isEmpty() ) { return; }

			try( BufferedWriter writer = Files.newBufferedWriter(
					baseLogPath,
					StandardOpenOption.CREATE,
					StandardOpenOption.APPEND ) ) {

				for( final JsonArray logEntry : logCache ) {
					writer.write( logEntry.toString() );
					writer.newLine();
				}

				logCache.clear();
			}catch( final IOException e ) {
				Debug.printDebug( "[Logger] can´t write log file at -> %s", baseLogPath );
				Debug.printException( this.getClass(), e );
			}
		}finally {
			threadLock.unlock();
		}
	}

	/**
	 * Reads the log file sequentially and parses the JSON lines.
	 * Returns the entries in reverse order, positioning the newest events at the top for UI representation.
	 *
	 * @return A list containing all logged structures ordered from newest to oldest
	 */
	public List<JsonArray> readLogForGui() {
		final List<JsonArray> invertedGuiList = new ArrayList<>();

		if( !Files.exists( baseLogPath ) ) { return invertedGuiList; }

		threadLock.lock();
		String currentLine = "";
		try {
			final List<String> lines = Files.readAllLines( baseLogPath );
			for( int i = lines.size() - 1; i >= 0; i-- ) {
				currentLine = lines.get( i ).trim();
				if( currentLine.isEmpty() ) continue;
				invertedGuiList.add( Json.createArrayBuilder().add( currentLine ).build() );
			}
		}catch( final JsonException e ) {
			Debug.printDebug( "[Logger] Invalid JSON in log line: %s", currentLine );
			Debug.printException( this.getClass(), e );
		}catch( final IOException e ) {
			Debug.printDebug( "[Logger] can´t read log file at -> %s", baseLogPath );
			Debug.printException( this.getClass(), e );
		}finally {
			threadLock.unlock();
		}
		return invertedGuiList;
	}

	/**
	 * Evaluates the size of the primary log file on startup and initiates a cascading shift
	 * of backup history files if the configured size threshold is exceeded.
	 */
	private void executeLogRotationIfNeeded() {
		if( !Files.exists( baseLogPath ) ) { return; }

		try {
			if( Files.size( baseLogPath ) < maxFileSize ) { return; }

			// Cascade existing backups downwards (e.g., log.4 -> log.5)
			for( int i = maxBackupIndex - 1; i >= 1; i-- ) {
				final Path sourceBackup = resolveBackupPath( i );
				if( Files.exists( sourceBackup ) ) {
					final Path targetBackup = resolveBackupPath( i + 1 );
					Files.move( sourceBackup, targetBackup, StandardCopyOption.REPLACE_EXISTING );
				}
			}

			// Move the current active log file to index 1 (e.g., log -> log.1)
			final Path firstBackup = resolveBackupPath( 1 );
			Files.move( baseLogPath, firstBackup, StandardCopyOption.REPLACE_EXISTING );

		}catch( final IOException e ) {
			Debug.printDebug( "[Logger] Execution of log rotation failed at -> %s", baseLogPath );
			Debug.printException( this.getClass(), e );
		}
	}

	/**
	 * Resolves the system path for an archived backup file based on its history index.
	 *
	 * @param index The history index marker
	 * @return Path representing the target location of the historical log file
	 */
	private Path resolveBackupPath( final int index ) {
		return baseLogPath.resolveSibling( baseLogPath.getFileName().toString() + "." + index );
	}
}