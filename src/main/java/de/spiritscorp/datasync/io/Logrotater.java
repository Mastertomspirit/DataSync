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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Orchestrates sequential log file rotation and archival retention policies.
 * Enforces size-based bounds on active logging outputs and automatically manages the
 * lifecycle of historical backup archives by shifting indices and purging expired segments.
 */
class Logrotater {

	/** The maximum threshold size in bytes before a file rotation is enforced. */
	private final long maxFileSize;

	/** The capacity limit of historical backup archives to keep on disk. */
	private final int maxBackupIndex;

	/**
	 * Initializes the log rotation engine with strict performance thresholds and archival limits.
	 *
	 * @param maxFileSize    The upper boundary capacity in bytes an active log file can reach before triggering a rotation cycle.
	 * @param maxBackupIndex The maximum index depth of historical backup files to retain on the file system before oldest logs are purged.
	 */
	Logrotater( final long maxFileSize, final int maxBackupIndex ) {
		this.maxFileSize = maxFileSize;
		this.maxBackupIndex = maxBackupIndex;
	}

	/**
	 * Evaluates the size of the primary log file on startup and initiates a cascading shift
	 * of backup history files if the configured size threshold is exceeded.
	 *
	 * @param baseLogPath The full comfiguration file path
	 */
	void executeLogRotationIfNeeded( final Path baseLogPath ) {
		if( !Files.exists( baseLogPath ) ) { return; }

		try {
			long actualSize = Files.size( baseLogPath );
			if( actualSize < maxFileSize ) { return; }
			Debug.printDebug( "[Logrotater] Rotation starting at file size: %.2f Mb", actualSize / ( 1024 * 1024 ) );

			// Cascade existing backups downwards (e.g., log.4 -> log.5)
			for( int i = maxBackupIndex - 1; i >= 1; i-- ) {
				final Path sourceBackup = resolveBackupPath( baseLogPath, i );
				if( Files.exists( sourceBackup ) ) {
					final Path targetBackup = resolveBackupPath( baseLogPath, i + 1 );
					Files.move( sourceBackup, targetBackup, StandardCopyOption.REPLACE_EXISTING );
					Debug.printDebug( "[Logrotater] Rotate file %s -> %s ", sourceBackup.toString(), targetBackup.toString() );
				}
			}

			// Move the current active log file to index 1 (e.g., log -> log.1)
			final Path firstBackup = resolveBackupPath( baseLogPath, 1 );
			Files.move( baseLogPath, firstBackup, StandardCopyOption.REPLACE_EXISTING );
			Debug.printDebug( "[Logrotater] Rotate file %s -> %s ", baseLogPath.toString(), firstBackup.toString() );
			Debug.printDebug( "[Logrotater] Finished successfully " );
		}catch( final IOException exception ) {
			Debug.printDebug( "[Logrotater Error] Execution of log rotation failed at -> %s with message -> %s", baseLogPath, exception.getMessage() );
			Debug.printException( this.getClass(), exception );
		}
	}

	/**
	 * Resolves the system path for an archived backup file based on its history index.
	 *
	 * @param baseLogPath The full comfiguration file path
	 * @param index       The history index marker
	 * @return Path representing the target location of the historical log file
	 */
	private Path resolveBackupPath( final Path baseLogPath, final int index ) {
		return baseLogPath.resolveSibling( baseLogPath.getFileName().toString() + "." + index );
	}
}
