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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.spiritscorp.datasync.io.Debug;

/**
 * Component responsible for high-level file analysis, conflict detection,
 * and synchronization state logic.
 */
class FileAnalyzer {

	/** The calculated optimal number of parallel threads to use for heavy I/O operations. */
	private final int avgProc;
	/** The threshold size at which a file list workload is split into sub-tasks for parallel processing. */
	private static final int THREAD_SPLIT_SIZE = 30_000;

	/**
	 * Constructs a new FileAnalyzer and dynamically calculates the optimal thread pool size
	 * based on the available system CPU cores.
	 */
	FileAnalyzer() {
		// Allocates roughly half of the available cores minus one if the system has more than 3 cores,
		// ensuring the application remains responsive during intense background scanning.
		avgProc = ( Runtime.getRuntime().availableProcessors() > 3 ) ? ( Runtime.getRuntime().availableProcessors() / 2 ) - 1 : 1;
	}

	/**
	 * Searches the given map and returns a new map with the duplicates
	 *
	 * @param sourceMap The map to be checked
	 * @return <b>Map</b> <br>
	 *         The map with sorted duplicates
	 */
	Map<Path, FileAttributes> findDuplicates( final Map<Path, FileAttributes> sourceMap ) {
		Debug.printDebug( "[File Analyzer]  entryPaths -> %d", sourceMap.size() );
		final Map<Path, FileAttributes> duplicateMap = Model.createMap();

		final Map<Long, ArrayList<Path>> mapSize = new HashMap<>();
		for( final Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet() ) {
			if( Thread.currentThread().isInterrupted() ) return duplicateMap;
			final long size = entry.getValue().getSize();
			if( mapSize.containsKey( size ) ) {
				mapSize.get( size ).add( entry.getKey() );
			}else {
				mapSize.put( size, new ArrayList<>() );
				mapSize.get( size ).add( entry.getKey() );
			}
		}

		for( final Map.Entry<Long, ArrayList<Path>> entry : mapSize.entrySet() ) {
			if( Thread.currentThread().isInterrupted() ) return duplicateMap;
			final ArrayList<Path> paths = entry.getValue();
			if( paths.size() > 1 ) {
				for( int i = 0; i < paths.size(); i++ ) {
					final String firstPath = sourceMap.get( paths.get( i ) ).getFileHash();
					for( int j = i + 1; j < paths.size(); j++ ) {
						if( firstPath.equals( sourceMap.get( paths.get( j ) ).getFileHash() ) ) {
							duplicateMap.put( paths.get( i ), sourceMap.get( paths.get( i ) ) );
							duplicateMap.put( paths.get( j ), sourceMap.get( paths.get( j ) ) );
						}
					}
				}
			}
		}
		Debug.printDebug( "[File Analyzer] DuplicateList -> ready : size: %d", duplicateMap.size() );
		return duplicateMap;
	}

	/**
	 * Cleans the maps of identical hits
	 *
	 * @param sourceMap
	 * @param destMap
	 */
	void equalsFiles( final Map<Path, FileAttributes> sourceMap, final Map<Path, FileAttributes> destMap ) {
		Debug.printDebug( "[File Analyzer] max mem: %d, free mem: %d, total mem: %d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory() );
		if( sourceMap.size() > 0 && destMap.size() > 0 ) {
			final Set<Path> sourceHitList = Collections.synchronizedSet( new HashSet<>() );
			final Set<Path> destHitList = Collections.synchronizedSet( new HashSet<>() );
			if( sourceMap.size() > THREAD_SPLIT_SIZE ) {
				try( ExecutorService executor = Executors.newFixedThreadPool( avgProc * 2 ) ) {
					final Map<Integer, Map<Path, FileAttributes>> splitSource = splitMap( sourceMap, avgProc );
					final Map<Integer, Map<Path, FileAttributes>> splitDest = splitMap( destMap, avgProc );
					for( final Map.Entry<Integer, Map<Path, FileAttributes>> source : splitSource.entrySet() ) {
						executor.execute( () -> equalsMap( source.getValue(), destMap, sourceHitList ) );
					}
					for( final Map.Entry<Integer, Map<Path, FileAttributes>> dest : splitDest.entrySet() ) {
						executor.execute( () -> equalsMap( dest.getValue(), sourceMap, destHitList ) );
					}
					executor.shutdown();
					while( !executor.awaitTermination( 10, TimeUnit.MINUTES ) ) {
						if( Thread.currentThread().isInterrupted() ) {
							executor.shutdownNow();
							return;
						}
					}
				}catch( InterruptedException _ ) {
					Thread.currentThread().interrupt();
					return;
				}
			}else {
				final Thread thread1 = new Thread( () -> equalsMap( sourceMap, destMap, sourceHitList ) );
				final Thread thread2 = new Thread( () -> equalsMap( destMap, sourceMap, destHitList ) );
				thread1.start();
				thread2.start();
				try {
					thread1.join();
					thread2.join();
				}catch( InterruptedException _ ) {
					thread1.interrupt();
					thread2.interrupt();
					Thread.currentThread().interrupt();
					return;
				}
			}
			for( final Path p : sourceHitList ) {
				sourceMap.remove( p );
			}
			for( final Path p : destHitList ) {
				destMap.remove( p );
			}
			Debug.printDebug( "[File Analyzer] Full source hitList size: %d  && Full destination hitList size: %d", sourceMap.size(), destMap.size() );
		}
	}

	/**
	 * Find out which file is the newest version or must be deleted and return the result.
	 * Determines synchronization actions by comparing file modification timestamps.
	 * <p>
	 *
	 * @param sourceMap       Map of files from source directory
	 * @param destMap         Map of files from destination directory
	 * @param startSourcePath Root path of source directory
	 * @param startDestPath   Root path of destination directory
	 * @param syncMap         Map containing last known synchronization state
	 * @return ArrayList containing three maps: [copySource, copyDest, delete]
	 */
	ArrayList<Map<Path, FileAttributes>> getSyncFiles( final Map<Path, FileAttributes> sourceMap, final Map<Path, FileAttributes> destMap, final Path startSourcePath, final Path startDestPath,
			final Map<Path, FileAttributes> syncMap ) {
		Debug.printDebug( "[FileAnalyzer] max mem: %d, free mem: %d, total mem: %d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory() );
		final ArrayList<Map<Path, FileAttributes>> resultValue = new ArrayList<>();
		final ArrayList<Map<Path, FileAttributes>> destValue = new ArrayList<>();
		final Map<Path, FileAttributes> copySourceHitList = Model.createMap();
		final Map<Path, FileAttributes> copyDestHitList = Model.createMap();
		final Map<Path, FileAttributes> delHitList = Model.createMap();

		resultValue.add( copySourceHitList );
		resultValue.add( copyDestHitList );
		resultValue.add( delHitList );
		destValue.add( copyDestHitList );
		destValue.add( copySourceHitList );
		destValue.add( delHitList );
		if( sourceMap.size() > 0 || destMap.size() > 0 ) {
			if( sourceMap.size() > THREAD_SPLIT_SIZE || destMap.size() > THREAD_SPLIT_SIZE ) {
				try( ExecutorService executor = Executors.newFixedThreadPool( avgProc * 2 ) ) {
					final Map<Integer, Map<Path, FileAttributes>> splitSource = splitMap( sourceMap, avgProc );
					final Map<Integer, Map<Path, FileAttributes>> splitDest = splitMap( destMap, avgProc );

					for( final Map.Entry<Integer, Map<Path, FileAttributes>> source : splitSource.entrySet() ) {
						executor.execute( () -> syncMaps( source.getValue(), destMap, resultValue, startDestPath, syncMap ) );
					}
					for( final Map.Entry<Integer, Map<Path, FileAttributes>> dest : splitDest.entrySet() ) {
						executor.execute( () -> syncMaps( dest.getValue(), sourceMap, destValue, startSourcePath, syncMap ) );
					}
					executor.shutdown();
					while( !executor.awaitTermination( 10, TimeUnit.MINUTES ) ) {
						if( Thread.currentThread().isInterrupted() ) {
							executor.shutdownNow();
							return resultValue;
						}
					}
				}catch( InterruptedException _ ) {
					Thread.currentThread().interrupt();
					return resultValue;
				}
			}else {
				syncMaps( sourceMap, destMap, resultValue, startDestPath, syncMap );
				if( Thread.currentThread().isInterrupted() ) return resultValue;
				syncMaps( destMap, sourceMap, destValue, startSourcePath, syncMap );
			}
		}
		Debug.printDebug( "[File Analyzer] Full copySourceHitList size: %d  && Full copyDestHitList size: %d  && Full delHitList size: %d",
				copySourceHitList.size(), copyDestHitList.size(), delHitList.size() );
		return resultValue;
	}

	/**
	 * Compares files in the iterate map with files in the full map and records matches.
	 * Files are considered equal if their FileAttributes objects are equal
	 * (same hash, size, modification time, and name).
	 *
	 * @param iterateMap The map to iterate through (typically a split/partial map)
	 * @param fullMap    The complete map to compare against
	 * @param hitList    Set to accumulate matching file paths
	 */
	private void equalsMap( final Map<Path, FileAttributes> iterateMap, final Map<Path, FileAttributes> fullMap, final Set<Path> hitList ) {
		for( final Map.Entry<Path, FileAttributes> entry : iterateMap.entrySet() ) {
			if( Thread.currentThread().isInterrupted() ) return;
			if( fullMap.containsValue( entry.getValue() ) ) {
				hitList.add( entry.getKey() );
			}
		}
	}

	/**
	 * Determines synchronization actions for all files contained in sourceMap.<br>
	 * The synchronization decision is based on:<br>
	 * <ul>
	 * <li>Current source file state</li>
	 * <li>Current destination file state</li>
	 * <li>Last known synchronization state</li>
	 * <ul><br>
	 * Rules:<br>
	 * <ol>
	 * <li>File only exists in source -> copy to destination</li>
	 * <li>File existed previously but is missing in destination -> delete source</li>
	 * <li>File exists in source and destination but not in sync state -> initial sync conflict, newest version wins</li>
	 * <li>File exists in all locations and differs from sync state -> newest version wins</li>
	 * <li>Identical files -> no action</li>
	 * <ol>
	 *
	 * @param sourceMap     Current source files
	 * @param destMap       Current destination files
	 * @param resultValue   Synchronization result:
	 *                      [0] copy source -> destination
	 *                      [1] copy destination -> source
	 *                      [2] delete files
	 * @param startDestPath Destination root path
	 * @param syncMap       Last synchronization snapshot
	 */
	private void syncMaps( final Map<Path, FileAttributes> sourceMap, final Map<Path, FileAttributes> destMap, final List<Map<Path, FileAttributes>> resultValue, final Path startDestPath,
			final Map<Path, FileAttributes> syncMap ) {

		final Map<Path, FileAttributes> copySourceHitList = resultValue.get( 0 );
		final Map<Path, FileAttributes> copyDestHitList = resultValue.get( 1 );
		final Map<Path, FileAttributes> delHitList = resultValue.get( 2 );

		for( final Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet() ) {
			if( Thread.currentThread().isInterrupted() ) return;

			final Path relativePath = entry.getValue().getRelativeFilePath();
			final Path destPath = startDestPath.resolve( relativePath );

			final FileAttributes sourceAttributes = entry.getValue();
			final FileAttributes destAttributes = destMap.get( destPath );
			final FileAttributes syncAttributes = syncMap.get( relativePath );

			/*
			 * -----------------------------------------------------------------
			 * CASE 1
			 * File exists only in source.
			 * -----------------------------------------------------------------
			 */
			if( destAttributes == null ) {
				if( syncAttributes == null ) {
					// New file
					copySourceHitList.put( entry.getKey(), sourceAttributes );
				}else {
					// File existed before but was deleted on destination
					delHitList.put( entry.getKey(), sourceAttributes );
				}
				continue;
			}

			/*
			 * -----------------------------------------------------------------
			 * CASE 2
			 * File exists in source and destination.
			 * -----------------------------------------------------------------
			 */
			if( sourceAttributes.equals( destAttributes ) ) {
				continue;
			}

			/*
			 * -----------------------------------------------------------------
			 * CASE 3
			 * Initial synchronization conflict.
			 *
			 * File exists on both sides but there is no sync history.
			 * Newest file wins.
			 * -----------------------------------------------------------------
			 */
			if( isNewer( sourceAttributes, destAttributes ) ) {
				copySourceHitList.put( entry.getKey(), sourceAttributes );
			}else {
				copyDestHitList.put( destPath, destAttributes );
			}
		}
	}

	/**
	 * Returns true if source is newer than destination.
	 *
	 * @param source Source file attributes
	 * @param dest   Destination file attributes
	 * @return true if source modification time is newer
	 */
	private boolean isNewer( final FileAttributes source, final FileAttributes dest ) {

		return source.getModTime().toMillis() > dest.getModTime().toMillis();
	}

	/**
	 * Splits a map into smaller chunks for parallel processing.
	 * Used to optimize performance when dealing with large file sets.
	 *
	 * <p>Maps are split based on the number of available processors.
	 * Each chunk receives approximately map.{@code size() / avgProc} entries.
	 * <p>
	 *
	 * @param map    The map to split
	 * @param avProc Number of threads/chunks to create
	 * @return Map of split maps indexed by integer keys (0 to avProc-1)
	 */
	private Map<Integer, Map<Path, FileAttributes>> splitMap( final Map<Path, FileAttributes> map, final int avProc ) {
		final Map<Integer, Map<Path, FileAttributes>> splitedMaps = Model.createMap();
		for( int i = 0; i < avProc; i++ ) {
			splitedMaps.put( i, Model.createMap() );
		}
		final int split = ( map.size() / avProc ) + 20;
		int innerMap = 0;
		int outerMap = 0;
		for( final Map.Entry<Path, FileAttributes> entry : map.entrySet() ) {
			if( innerMap <= split ) {
				splitedMaps.get( outerMap ).put( entry.getKey(), entry.getValue() );
				innerMap++;
			}else {
				++outerMap;
				innerMap = 0;
				splitedMaps.get( outerMap ).put( entry.getKey(), entry.getValue() );
			}
		}
		return splitedMaps;
	}
}
