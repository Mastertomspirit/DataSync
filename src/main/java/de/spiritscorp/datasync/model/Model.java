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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;

/**
 * Core controller class responsible for managing high-performance file synchronization,
 * directory scanning, and backup operations.
 * <p>
 * This class orchestrates the synchronization pipeline by utilizing multi-threaded
 * file traversals to process source and destination structures simultaneously. It tracks
 * file attributes, evaluates state deltas to isolate unique changes, detects duplicate
 * files based on sizes or checksum configurations, and handles safe file transfers.
 * <p>
 * To ensure data integrity, structural backups are handled via a strict two-phase execution
 * model: clearing obsolete files first (with optional local trashbin staging) before transferring
 * new payloads. Internal storage maps are wrapped in synchronized structures to maintain
 * thread safety during parallel operations.
 *
 */
public class Model {

	private final Map<Path, FileAttributes> sourceMap;
	private final Map<Path, FileAttributes> destMap;
	private final FileHandler handler;

	/**
	 * Constructs a new Model controller instance and sets up the central tracking components.
	 * <p>
	 * Initializes the internal system logger for transaction auditing and binds the
	 * reference maps used for storing file attribute states on the source and destination sides.
	 *
	 * @param logger    the active system logger instance used for operational and debug tracking
	 * @param sourceMap the tracking map used to store and evaluate source file attributes
	 * @param destMap   the tracking map used to store and evaluate destination file attributes
	 */
	public Model( Logger logger, Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap ) {
		this.sourceMap = sourceMap;
		this.destMap = destMap;
		handler = new FileHandler( logger );
	}

	/**
	 * Creates a thread-safe, synchronized sorted map backed by a standard TreeMap.
	 * <p>
	 * This helper method is crucial for concurrent environments where multiple threads
	 * need to read and write to the file mapping without risking memory corruption.
	 *
	 * @param <K> the type of keys maintained by this map (typically java.nio.file.Path)
	 * @param <V> the type of mapped values (typically FileAttributes)
	 * @return a synchronized, thread-safe view of a newly instantiated TreeMap
	 */
	public static final <K, V> Map<K, V> createMap() {
		return Collections.synchronizedSortedMap( new TreeMap<>() );
	}

	/**
	 * Lists all files in both source and destination directories concurrently using dedicated threads.
	 * <p>
	 * To maximize performance on multi-core systems, this method spawns two parallel threads:
	 * One for the source path scanning and one for the destination path scanning.
	 * <p>
	 * After both threads have finished execution, the provided statistics array is populated
	 * with the size and byte metrics of both maps.
	 *
	 * @param sourcePathes an ArrayList containing the base directories of the source side
	 * @param destPathes   an ArrayList containing the base directories of the destination side
	 * @param stats        a Long array with a minimum length of 4 used to store the tracking results:
	 *                     index 0: Total number of files found in source,
	 *                     index 1: Total number of files found in destination,
	 *                     index 2: Total aggregated size of source files in bytes,
	 *                     index 3: Total aggregated size of destination files in bytes
	 * @param deepScan     the configuration determining the type and depth of the file parsing
	 * @param subDir       true to recursively scan all subdirectories, false to only process the root level
	 * @param trashbin     true to enable trashbin retention logic, false to bypass it
	 * @return a Map containing all paths where failures, permission issues, or structural conflicts occurred
	 */
	public Map<Path, FileAttributes> scanSyncFiles( ArrayList<Path> sourcePathes, ArrayList<Path> destPathes, Long[] stats, ScanType deepScan, boolean subDir, boolean trashbin ) {
		Debug.printDebug( "[Model] list start" );
		final Thread t1 = new Thread( () -> handler.listFiles( sourcePathes, sourceMap, deepScan, subDir ) );
		final Thread t2 = new Thread( () -> handler.listFiles( destPathes, destMap, deepScan, subDir ) );
		t1.start();
		t2.start();
		try {
			t1.join();
			t2.join();
		}catch( final InterruptedException e ) {
			Debug.printException( this.getClass(), e );
		}
		stats[0] = (long) sourceMap.size();
		stats[1] = (long) destMap.size();
		stats[2] = getBytes( sourceMap );
		stats[3] = getBytes( destMap );
		Debug.printDebug( "[Model] list ready" );
		return getFailtures( sourceMap, destMap );
	}

	/**
	 * Compares the pre-loaded source and destination maps to isolate identical files.
	 * <p>
	 * This method triggers the internal handlers to filter out matching files from both
	 * maps. After execution, both maps will only contain unique entries that require
	 * synchronization actions like copy, update, or delete.
	 */
	public void getEqualsFiles() {
		Debug.printDebug( "[Model] getEqualsFiles start" );
		handler.equalsFiles( sourceMap, destMap );
		Debug.printDebug( "[Model] getEqualsFiles ready" );
	}

	/**
	 * Analyzes the file state differentials to categorize synchronization requirements.
	 * <p>
	 * Evaluates file modification dates, sizes, or checksums between the source and destination
	 * targets. The results are split into three structural hitlists returned as an indexed list.
	 *
	 * @param syncMap    the map tracking current synchronization history states
	 * @param sourcePath the absolute base path of the source directory
	 * @param destPath   the absolute base path of the destination directory
	 * @return an ArrayList containing exactly three separate maps:
	 *         index 0 (copySourceHitList): Files to be copied from source to destination,
	 *         index 1 (copyDestHitList): Files to be copied back from destination to source,
	 *         index 2 (delHitList): Files marked for deletion from the target directory
	 */
	public ArrayList<Map<Path, FileAttributes>> getSyncFiles( Map<Path, FileAttributes> syncMap, Path sourcePath, Path destPath ) {
		Debug.printDebug( "[Model] getSyncFiles start" );
		final ArrayList<Map<Path, FileAttributes>> result = handler.getSyncFiles( sourceMap, destMap, sourcePath, destPath, syncMap );
		Debug.printDebug( "[Model] getSyncFiles ready" );
		return result;
	}

	/**
	 * Executes the physical file backup sequence on the local storage system.
	 * <p>
	 * To ensure a clean and predictable operation, this method enforces a strict two-phase execution order:
	 * Phase 1 (Purge) clears obsolete files from the target directory first, and
	 * Phase 2 (Transfer) physically copies new or updated files into the destination path.
	 *
	 * @param del          the mode flag determining deletions (processed exclusively if set to 0)
	 * @param logOn        true to output detailed file paths and transaction logs to the system logger
	 * @param destPath     the absolute path to the target directory where files will be transferred to
	 * @param trashbin     true to move deleted files safely into a local trash bin structure
	 * @param trashbinPath the absolute directory path representing the safe retention folder
	 * @return true if all file entries inside the tracking maps were successfully processed and cleared,
	 *         false if unprocessed files remain due to operational faults or file system errors
	 */
	public boolean backupFiles( int del, boolean logOn, Path destPath, boolean trashbin, Path trashbinPath ) {
		if( del == 0 && !destMap.isEmpty() ) handler.deleteFiles( destMap, logOn, trashbin, trashbinPath );
		if( !sourceMap.isEmpty() ) handler.copyFiles( sourceMap, logOn, destPath );
		return sourceMap.isEmpty() && destMap.isEmpty();
	}

	/**
	 * Synchronizes files bi-directionally between the configured directories.
	 * <p>
	 * This function consumes the pre-calculated multi-hitlist results, transfers the newest file states,
	 * and structurally synchronizes both directories to reach an identical file state.
	 *
	 * @param result     the calculated synchronization tracking lists containing the hit maps
	 * @param syncMap    the map tracking current synchronization history states
	 * @param sourcePath the absolute base path of the source directory
	 * @param destPath   the absolute base path of the destination directory
	 * @param testOn     true to run a dry-run simulation which skips real I/O operations
	 * @return true if the entire synchronization pipeline completed without unexpected exceptions,
	 *         false if errors occurred during file interaction
	 */
	public boolean syncFiles( SyncJobContext ctx, ArrayList<Map<Path, FileAttributes>> result, Map<Path, FileAttributes> syncMap, Path sourcePath, Path destPath, boolean testOn ) {
		if( !result.get( 0 ).isEmpty() ) handler.copyFiles( result.get( 0 ), false, destPath );
		if( !result.get( 1 ).isEmpty() ) handler.copyFiles( result.get( 1 ), false, sourcePath );
		if( !result.get( 2 ).isEmpty() ) handler.deleteFiles( result.get( 2 ), false, false, null );

		sourceMap.clear();
		destMap.clear();
		syncMap.clear();
		if( !testOn ) {
			final Map<Path, FileAttributes> tempMap = createMap();
			handler.listFiles( ctx.getPreference().getSourcePath(), tempMap, ScanType.SYNCHRONIZE, false );
			for( final Map.Entry<Path, FileAttributes> entry : tempMap.entrySet() ) {
				syncMap.put( entry.getValue().getRelativeFilePath(), entry.getValue() );
			}
			ctx.getPreference().writeSyncMap();
		}
		return result.get( 0 ).isEmpty() && result.get( 1 ).isEmpty() && result.get( 2 ).isEmpty();
	}

	/**
	 * Scans the selected target paths to locate and extract duplicate file structures.
	 * <p>
	 * Utilizes a specialized duplicate scan handler that matches files based on identical
	 * parameters like sizing blocks or checksums. Any errors encountered during the filesystem
	 * traversal are collected and merged into the final state mapping.
	 *
	 * @param paths an ArrayList containing the directory paths that should be inspected for file duplicates
	 * @return a Map containing the duplicate paths mapped to their attributes, combined with failure reports
	 */
	public Map<Path, FileAttributes> scanDublicates( final ArrayList<Path> paths, final Long... stats ) {
		handler.listFiles( paths, sourceMap, ScanType.DUBLICATE_SCAN, false );
		final Map<Path, FileAttributes> duplicateMap = handler.findDuplicates( sourceMap );
		stats[0] = (long) sourceMap.size();
		stats[1] = (long) getFailtures( sourceMap, destMap ).size();
		stats[2] = 0L;
		stats[3] = 0L;
		return duplicateMap;
	}

	/**
	 * Aggregates processing errors, missing file attributes, or permission blocks into a dedicated failure tracking map.
	 * <p>
	 * This private utility evaluates the unresolved differences between the source and destination maps
	 * after an operation has completed, isolating paths that caused structural system errors.
	 *
	 * @param sourceMap the tracking map containing the current source file information
	 * @param destMap   the tracking map containing the current destination file information
	 * @return a filtered Map detailing all elements that failed to process correctly
	 */
	private Map<Path, FileAttributes> getFailtures( Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap ) {
		final Map<Path, FileAttributes> failMap = createMap();
		if( sourceMap != null ) {
			for( final Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet() ) {
				if( entry.getValue().getFileHash().equals( "Failed" ) ) {
					failMap.put( entry.getKey(), entry.getValue() );
				}
			}
		}
		if( destMap != null ) {
			for( final Map.Entry<Path, FileAttributes> entry : destMap.entrySet() ) {
				if( entry.getValue().getFileHash().equals( "Failed" ) ) {
					failMap.put( entry.getKey(), entry.getValue() );
				}
			}
		}
		return failMap;
	}

	/**
	 * Calculates the total aggregated file size of all entries within the provided map.
	 * <p>
	 * Loops through the key set of paths and sums up the individual byte sizes
	 * extracted from the respective file attributes.
	 *
	 * @param map the tracking map containing the file paths and their associated attributes
	 * @return the total size of all files combined, represented in bytes
	 */
	private Long getBytes( Map<Path, FileAttributes> map ) {
		long allBytes = 0;
		for( final FileAttributes p : map.values() ) {
			allBytes += p.getSize();
		}
		return allBytes;
	}
}
