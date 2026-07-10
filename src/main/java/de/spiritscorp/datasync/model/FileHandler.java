package de.spiritscorp.datasync.model;

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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;

/**
 * Component responsible for handling low-level file operations such as listing,
 * copying, and deleting files across various storage backends.
 */
class FileHandler {

	/** The logger instance used for system diagnostics and error reporting. */
	private final Logger log;

	/**
	 * Constructs a new FileHandler with the specified logger.
	 *
	 * @param logger the logger instance to be used for diagnostic output
	 */
	FileHandler( final Logger logger ) {
		this.log = logger;
	}

	/**
	 * Scans the provided directory paths asynchronously to collect file attributes.
	 * <br>
	 * <br>
	 * The results are populated directly into the provided thread-safe result map.
	 * This method supports infinite processing loops to safely handle slow network shares.
	 *
	 * @param paths     the list of root directory paths to scan
	 * @param resultMap the shared map where resolved file paths and attributes are stored
	 * @param deepScan  the strategy determining the depth and thoroughness of the file analysis
	 * @param subDir    {@code true} to recursively traverse subdirectories, {@code false} otherwise
	 */
	void listFiles( final List<Path> paths, final Map<Path, FileAttributes> resultMap, final ScanType deepScan, final boolean subDir ) {
		try( ExecutorService executor = Executors.newSingleThreadExecutor() ) {
			for( final Path path : paths ) {
				// Guard: Check interruption context before entering the file tree walker system
				if( Thread.currentThread().isInterrupted() ) {
					Debug.printDebug( "[File Handler] Interrupt!  Interruption detected prior to walking directory path: %s", path.toString() );
					executor.shutdownNow();
					return;
				}
				if( Files.exists( path ) ) {
					walkTree( path.normalize(), executor, resultMap, deepScan, subDir );
				}
			}
			executor.shutdown();
			while( !executor.awaitTermination( 100, TimeUnit.MILLISECONDS ) ) {
				if( Thread.currentThread().isInterrupted() ) {
					executor.shutdownNow();
					return;
				}
			}
		}catch( InterruptedException _ ) {
			Debug.printDebug( "[File Handler] File processing walk subsystem was forcefully interrupted." );
			Thread.currentThread().interrupt();
		}
		for( final Path path : paths ) {
			Debug.printDebug( "[File Handler] ListFiles() -> ready  %s -> %s", Thread.currentThread().getName(), path.toString() );
		}
	}

	/**
	 * Deletes files from the specified map with optional trashbin backup.
	 *
	 * <p>For each file in the map:
	 * <ol>
	 * <li>Optionally copies file to trashbin directory before deletion</li>
	 * <li>Sets write permission if needed</li>
	 * <li>Deletes the file</li>
	 * <li>Logs the operation result</li>
	 * <ol>
	 * <p>
	 *
	 * @param map          Map of files to delete
	 * @param logOn        If true, prints status after completion
	 * @param trashbin     If true, copies files to trashbin before deletion
	 * @param trashbinPath Path to trashbin directory
	 */
	void deleteFiles( final Map<Path, FileAttributes> map, final boolean logOn, final boolean trashbin, final Path trashbinPath ) {
		for( final Map.Entry<Path, FileAttributes> entry : map.entrySet() ) {
			final FileAttributes fileAttr = entry.getValue();
			final Path path = entry.getKey();
			// Guard: Check thread interrupt status before executing file operations
			if( Thread.currentThread().isInterrupted() ) {
				Debug.printDebug( "[File Handler] Interrupt! Safe loop interruption caught within file deletion loop vector at: %s", path.toString() );
				break;
			}else if( fileAttr == null ) continue;
			processSingleDeletion( path, fileAttr, trashbin, trashbinPath );
		}
		map.clear();
		if( logOn ) log.printStatus();
	}

	/**
	 * Copies files from source to destination preserving file attributes.
	 *
	 * <p>For each file in the map:
	 * <ol>
	 * <li>Creates parent directories if needed</li>
	 * <li>Sets write permission on existing destination if needed</li>
	 * <li>Copies file with REPLACE_EXISTING and COPY_ATTRIBUTES options</li>
	 * <li>Restores original creation time</li>
	 * <li>Logs the operation result</li>
	 * <ol>
	 * <p>
	 *
	 * @param map      Map of files to copy (key=source path, value=file attributes)
	 * @param logOn    If true, prints status after completion
	 * @param destPath Destination directory path
	 */
	void copyFiles( final Map<Path, FileAttributes> map, final boolean logOn, final Path destPath ) {
		for( final Map.Entry<Path, FileAttributes> entry : map.entrySet() ) {
			// Guard: Check thread interrupt status before starting next copy transaction step
			if( Thread.currentThread().isInterrupted() ) {
				Debug.printDebug( "[File Handler] Interrupt! Safe loop interruption caught within file replication loop vector at: %s", entry.getKey().toString() );
				break;
			}
			final FileAttributes fileAttr = entry.getValue();
			if( fileAttr == null ) continue;
			final Path path = destPath.resolve( fileAttr.getRelativeFilePath() );

			if( ensureWritable( path ) ) {
				if( moveFile( entry.getKey(), path, fileAttr.getCreateTime() ) ) {
					log.setEntry( path.toString(), "kopiert", fileAttr );
				}else {
					log.setEntry( path.toString(), "FEHLER BEIM KOPIEREN", fileAttr );
					Debug.printDebug( "[File Handler Error] Copy failed: %s", path.toString() );
				}
			}else {
				log.setEntry( path.toString(), "SCHREIBSCHUTZ BEIM KOPIEREN", fileAttr );
				Debug.printDebug( "[File Handler Error] Copy failed, target file is not writable: %s", path.toString() );
			}
		}
		map.clear();
		if( logOn ) log.printStatus();
	}

	/**
	 * Orchestrates the deletion lifecycle for a single tracked file asset within the pipeline.
	 * <br>
	 * <br>
	 * If the staging flag is active, the method attempts to safely relocate the file to the designated trash bin
	 * architecture before purging it from the source node. It dynamically intercepts read-only locks by invoking
	 * permission elevation routines, ensuring that individual file transaction failures or hardware anomalies
	 * are captured defensively and recorded without halting the collective loop.
	 *
	 * @param path         the absolute file system path node of the file target to be deleted
	 * @param fileAttr     the architectural metadata container mapping the historical context of the file asset
	 * @param trashbin     controls whether the file should be moved into a backup staging area prior to removal
	 * @param trashbinPath the root destination directory layer representing the virtual trash bin storage pool
	 */
	private void processSingleDeletion( final Path path, final FileAttributes fileAttr, final boolean trashbin, final Path trashbinPath ) {
		try {
			if( trashbin && trashbinPath != null ) {
				final Path trashbinFile = trashbinPath.resolve( fileAttr.getRelativeFilePath() );
				if( !moveFile( path, trashbinFile, fileAttr.getCreateTime() ) ) {
					log.setEntry( path.toString(), "FEHLER BEIM VERSCHIEBEN IN DEN PAPIERKORB", fileAttr );
					Debug.printDebug( "[File Handler Error] Copy failed SourcePath :%s  DestPath: %s", path.toString(), trashbinFile.toString() );
				}
			}
			if( ensureWritable( path ) ) {
				Files.delete( path );
				log.setEntry( path.toString(), "gelöscht", fileAttr );
			}else {
				log.setEntry( path.toString(), "SCHREIBSCHUTZ BEIM LÖSCHEN", fileAttr );
				Debug.printDebug( "[File Handler Error] Delete failed, target file is not writable: %s", path.toString() );
			}
		}catch( final IOException exception ) {
			log.setEntry( path.toString(), "FEHLER BEIM LÖSCHEN", fileAttr );
			Debug.printDebug( "[File Handler Error] Delete failed: %s", path.toString() );
			Debug.printException( this.getClass(), exception );
		}
	}

	/**
	 * Evaluates and dynamically elevates file system privileges to ensure the target path is writable.
	 * <br>
	 * <br>
	 * This method utilizes a robust feature-detection pattern to achieve pure platform independence, bypassing
	 * fragile operating system string checks. If the file is not natively writable, it sequentially polls the
	 * layout for {@link DosFileAttributeView} (Windows/NTFS) or {@link PosixFileAttributeView} (macOS/Linux/Jimfs).
	 * POSIX permission modifications are designed to be additive, appending the owner write bit to a copy of the
	 * existing permission mask to guarantee that file readability states remain entirely intact.
	 *
	 * @param path the target path node evaluated and cleared for structural write operations
	 * @return {@code true} if the file is verified as writable or was successfully elevated to a writable state;
	 *         {@code false} if permissions could not be altered or the underlying file system capabilities are unsupported
	 */
	private boolean ensureWritable( final Path path ) {
		if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS ) ) return true;
		if( Files.isWritable( path ) ) return true;
		try {

			// 1. Check for DOS capability (Windows, FAT32/NTFS external drives)
			final DosFileAttributeView dosView = Files.getFileAttributeView( path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS );
			if( dosView != null ) {
				dosView.setReadOnly( false );
				return true;
			}

			// 2. Check for POSIX capability (UNIX, macOS, Jimfs Unix environment)
			final PosixFileAttributeView posixView = Files.getFileAttributeView( path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS );
			if( posixView != null ) {
				final Set<PosixFilePermission> permissions = EnumSet.noneOf( PosixFilePermission.class );
				permissions.addAll( posixView.readAttributes().permissions() );
				if( permissions.add( PosixFilePermission.OWNER_WRITE ) ) {
					posixView.setPermissions( permissions );
					return true;
				}
			}
			// 3. Fallback if the filesystem supports neither view
			Debug.printDebug( "[File Handler] File system operations are not supported for this specific target track: %s", path.toString() );
		}catch( final IOException exception ) {
			Debug.printDebug( "[File Handler Error] I/O error, file permissions could not be verified or modified: %s", exception.getLocalizedMessage() );
			Debug.printException( this.getClass(), exception );
		}
		return false;
	}

	/**
	 * Replicates a file asset across system tracking layouts while synchronizing underlying temporal attributes.
	 * <br>
	 * <br>
	 * This private helper encapsulates defensive verification steps. It guarantees idempotent execution by
	 * automatically generating any missing parent directory trees on the fly. Upon successful stream replication
	 * using standard replacement options, it forcefully overwrites the target's metadata cluster to ensure the
	 * origin file creation timestamp is structurally preserved.
	 *
	 * @param sourceFilePath the absolute source path node providing the active data payload stream
	 * @param destFilePath   the targeted destination path node location where the synchronized file asset will materialize
	 * @param createTime     the original creation timestamp token used to override the final destination file attributes
	 * @return {@code true} if the file transfer and attribute injection sequences finish successfully;
	 *         {@code false} if blocked by an internal I/O failure, null references, or file-locking collision states
	 */
	private boolean moveFile( final Path sourceFilePath, final Path destFilePath, final FileTime createTime ) {
		if( destFilePath == null || sourceFilePath == null ) return false;
		try {
			if( !Files.exists( destFilePath ) ) Files.createDirectories( destFilePath.getParent() );
			Files.copy( sourceFilePath, destFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES );
			Files.setAttribute( destFilePath, "creationTime", createTime );
			return true;
		}catch( IOException exception ) {
			Debug.printException( this.getClass(), exception );
			return false;
		}
	}

	/**
	 * Initiates a synchronous, deep file tree traversal starting at the given path.
	 * <br>
	 * <br>
	 * Any structural I/O errors encountered during the walk are caught locally
	 * and sent to the debug subsystem to prevent the entire batch scan from failing.
	 *
	 * @param path      the root directory path where the traversal begins
	 * @param executor  the executor service processing asynchronous file task attributes
	 * @param resultMap the shared map where discovered paths and attributes are registered
	 * @param deepScan  the strategy determining the thoroughness of the file analysis
	 * @param subDir    {@code true} to use the parent directory as the base context,
	 *                  {@code false} to use the path itself
	 */
	private void walkTree( final Path path, final ExecutorService executor, final Map<Path, FileAttributes> resultMap, final ScanType deepScan, final boolean subDir ) {
		try {
			final Path baseDir = subDir ? path.getParent() : path;
			Files.walkFileTree( path, new FileVisit( executor, baseDir, resultMap, deepScan ) );
		}catch( final IOException exception ) {
			Debug.printDebug( "[File Handler Error] Error on walking directory path: %s with message: %s", path.toString(), exception.getMessage() );
			Debug.printException( this.getClass(), exception );
		}
	}

}
