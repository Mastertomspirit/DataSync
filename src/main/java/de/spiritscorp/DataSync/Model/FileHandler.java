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
package de.spiritscorp.DataSync.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;

class FileHandler {

	private final Logger log;
	private final int avgProc; // number of threads

	FileHandler( Logger log ) {
		this.log = log;
		avgProc = ( Runtime.getRuntime().availableProcessors() > 3 ) ? ( Runtime.getRuntime().availableProcessors() / 2 ) - 1 : 1;
	}

	/**
	 * List all files in the given directory and execute the filescan for attributes, and give back the results in a new Map
	 *
	 * @param paths
	 * @param deepScan
	 * @param subDir
	 * @return <b>Map</b> <br>
	 *         A map with FileAttributes
	 */
	void listFiles( ArrayList<Path> paths, Map<Path, FileAttributes> resultMap, ScanType deepScan, boolean subDir ) {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		for( final Path path : paths ) {
			// Guard: Check interruption context before entering the file tree walker system
			if( Thread.currentThread().isInterrupted() ) {
				Debug.printDebug( "[Engine] Interruption detected prior to walking directory path: %s", path.toString() );
				executor.shutdownNow();
				return;
			}
			if( Files.exists( path ) ) {
				try {
					final Path baseDir = subDir ? path.getParent() : path;
					Files.walkFileTree( path, new FileVisit( executor, baseDir, resultMap, deepScan ) );
				}catch( final IOException e ) {
					Debug.printException( this.getClass(), e );
				}
			}
		}
		executor.shutdown();
		try {
			while( !executor.awaitTermination( 100, TimeUnit.MILLISECONDS ) ) {
				if( Thread.currentThread().isInterrupted() ) {
					executor.shutdownNow();
					throw new InterruptedException();
				}
			}
		}catch( final InterruptedException e ) {
			Debug.printDebug( "[Engine] File processing walk subsystem was forcefully interrupted." );
			Thread.currentThread().interrupt();
		}
		Debug.printDebug( "listFiles() -> ready  %s -> %s", Thread.currentThread().getName(), paths.get( 0 ).toString() );
	}

	/**
	 * Searches the given map and returns a new map with the duplicates
	 *
	 * @param sourceMap The map to be checked
	 * @return <b>Map</b> <br>
	 *         The map with sorted duplicates
	 */
	Map<Path, FileAttributes> findDuplicates( Map<Path, FileAttributes> sourceMap ) {
		Debug.printDebug( "entryPaths -> %d", sourceMap.size() );
		final Map<Path, FileAttributes> duplicateMap = Model.createMap();

		final HashMap<Long, ArrayList<Path>> mapSize = new HashMap<>();
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
		Debug.printDebug( "duplicateList -> ready : size: %d", duplicateMap.size() );
		return duplicateMap;
	}

	/**
	 * Cleans the maps of identical hits
	 *
	 * @param sourceMap
	 * @param destMap
	 */
	void equalsFiles( Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap ) {
		Debug.printDebug( "max mem: %d, free mem: %d, total mem: %d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory() );
		if( sourceMap.size() != 0 && destMap.size() != 0 ) {
			final Set<Path> sourceHitList = Collections.synchronizedSet( new HashSet<>() );
			final Set<Path> destHitList = Collections.synchronizedSet( new HashSet<>() );
			if( sourceMap.size() > 30_000 ) {
				final ExecutorService executor = Executors.newFixedThreadPool( avgProc * 2 );
				final Map<Integer, Map<Path, FileAttributes>> splitSource = splitMap( sourceMap, avgProc );
				final Map<Integer, Map<Path, FileAttributes>> splitDest = splitMap( destMap, avgProc );
				for( final Map.Entry<Integer, Map<Path, FileAttributes>> source : splitSource.entrySet() ) {
					executor.execute( () -> equalsMap( source.getValue(), destMap, sourceHitList ) );
				}
				for( final Map.Entry<Integer, Map<Path, FileAttributes>> dest : splitDest.entrySet() ) {
					executor.execute( () -> equalsMap( dest.getValue(), sourceMap, destHitList ) );
				}
				executor.shutdown();
				try {
					while( !executor.awaitTermination( 100, TimeUnit.MILLISECONDS ) ) {
						if( Thread.currentThread().isInterrupted() ) {
							executor.shutdownNow();
							return;
						}
					}
				}catch( final InterruptedException e ) {
					executor.shutdownNow();
					Thread.currentThread().interrupt();
					return;
				}
			}else {
				final Thread t1 = new Thread( () -> equalsMap( sourceMap, destMap, sourceHitList ) );
				final Thread t2 = new Thread( () -> equalsMap( destMap, sourceMap, destHitList ) );
				t1.start();
				t2.start();
				try {
					t1.join();
					t2.join();
				}catch( final InterruptedException e ) {
					t1.interrupt();
					t2.interrupt();
					Thread.currentThread().interrupt();
					return;
				}
			}
			// Post-processing guard
			if( Thread.currentThread().isInterrupted() ) return;
			for( final Path p : sourceHitList ) {
				sourceMap.remove( p );
			}
			for( final Path p : destHitList ) {
				destMap.remove( p );
			}
			Debug.printDebug( "full source hitList size: %d  && full destination hitList size: %d", sourceMap.size(), destMap.size() );
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
	ArrayList<Map<Path, FileAttributes>> getSyncFiles( Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap, Path startSourcePath, Path startDestPath,
			Map<Path, FileAttributes> syncMap ) {
		Debug.printDebug( "max mem: %d, free mem: %d, total mem: %d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory() );
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
			if( sourceMap.size() > 30_000 || destMap.size() > 30_000 ) {
				final ExecutorService executor = Executors.newFixedThreadPool( avgProc * 2 );
				final Map<Integer, Map<Path, FileAttributes>> splitSource = splitMap( sourceMap, avgProc );
				final Map<Integer, Map<Path, FileAttributes>> splitDest = splitMap( destMap, avgProc );

				for( final Map.Entry<Integer, Map<Path, FileAttributes>> source : splitSource.entrySet() ) {
					executor.execute( () -> syncMaps( source.getValue(), destMap, resultValue, startDestPath, syncMap ) );
				}
				for( final Map.Entry<Integer, Map<Path, FileAttributes>> dest : splitDest.entrySet() ) {
					executor.execute( () -> syncMaps( dest.getValue(), sourceMap, destValue, startSourcePath, syncMap ) );
				}
				executor.shutdown();
				try {
					while( !executor.awaitTermination( 100, TimeUnit.MILLISECONDS ) ) {
						if( Thread.currentThread().isInterrupted() ) {
							executor.shutdownNow();
							return resultValue;
						}
					}
				}catch( final InterruptedException e ) {
					executor.shutdownNow();
					Thread.currentThread().interrupt();
					return resultValue;
				}
			}else {
				syncMaps( sourceMap, destMap, resultValue, startDestPath, syncMap );
				if( Thread.currentThread().isInterrupted() ) return resultValue;
				syncMaps( destMap, sourceMap, destValue, startSourcePath, syncMap );
			}
		}
		Debug.printDebug( "full copySourceHitList size: %d  && full copyDestHitList size: %d  && full delHitList size: %d", copySourceHitList.size(), copyDestHitList.size(), delHitList.size() );
		return resultValue;
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
	void deleteFiles( Map<Path, FileAttributes> map, boolean logOn, boolean trashbin, Path trashbinPath ) {
		for( final Map.Entry<Path, FileAttributes> entry : map.entrySet() ) {
			// Guard: Check thread interrupt status before executing file operations
			if( Thread.currentThread().isInterrupted() ) {
				Debug.printDebug( "[Engine] Safe loop interruption caught within file deletion loop vector." );
				break;
			}

			final FileAttributes fileAttr = entry.getValue();
			final Path path = entry.getKey();
			if( fileAttr == null ) continue;
			try {
				if( trashbin && trashbinPath != null ) {
					Files.createDirectories( trashbinPath.resolve( fileAttr.getRelativeFilePath() ) );
					Files.copy( path, trashbinPath.resolve( fileAttr.getRelativeFilePath() ), StandardCopyOption.REPLACE_EXISTING );
				}
				if( !path.toFile().canWrite() ) path.toFile().setWritable( true );
				Files.delete( path );
				log.setEntry( path.toString(), "gelöscht", fileAttr );
			}catch( final IOException e ) {
				log.setEntry( path.toString(), "FEHLER BEIM LÖSCHEN", fileAttr );
				Debug.printDebug( "delete failed: %s", path.toString() );
				Debug.printException( this.getClass(), e );
			}
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
	void copyFiles( Map<Path, FileAttributes> map, boolean logOn, Path destPath ) {
		for( final Map.Entry<Path, FileAttributes> entry : map.entrySet() ) {
			// Guard: Check thread interrupt status before starting next copy transaction step
			if( Thread.currentThread().isInterrupted() ) {
				Debug.printDebug( "[Engine] Safe loop interruption caught within file replication loop vector." );
				break;
			}
			final FileAttributes fileAttr = entry.getValue();
			if( fileAttr == null ) continue;
			final Path path = destPath.resolve( fileAttr.getRelativeFilePath() );
			final Path parentPath = path.getParent();
			try {
				if( parentPath != null && !Files.exists( parentPath ) )
					Files.createDirectories( parentPath );
				else if( Files.exists( path ) && !path.toFile().canWrite() ) path.toFile().setWritable( true );
				Files.copy(
						entry.getKey(),
						path,
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES );
				Files.setAttribute( path, "creationTime", fileAttr.getCreateTime() );
				log.setEntry( path.toString(), "kopiert", fileAttr );
			}catch( final IOException e ) {
				log.setEntry( path.toString(), "FEHLER BEIM KOPIEREN", fileAttr );
				Debug.printDebug( "copy failed: %s", path.toString() );
				Debug.printException( this.getClass(), e );
			}
		}
		map.clear();
		if( logOn ) log.printStatus();
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
	private void equalsMap( Map<Path, FileAttributes> iterateMap, Map<Path, FileAttributes> fullMap, Set<Path> hitList ) {
		for( final Map.Entry<Path, FileAttributes> entry : iterateMap.entrySet() ) {
			if( Thread.currentThread().isInterrupted() ) return;
			if( fullMap.containsValue( entry.getValue() ) ) {
				hitList.add( entry.getKey() );
			}
		}
	}

	/**
	 * Determines synchronization actions for all files contained in sourceMap.
	 *
	 * <p>
	 * The synchronization decision is based on:
	 * <p>
	 *
	 * <ul>
	 * <li>Current source file state</li>
	 * <li>Current destination file state</li>
	 * <li>Last known synchronization state</li>
	 * <ul>
	 *
	 * <p>
	 * Rules:
	 * <p>
	 *
	 * <ol>
	 * <li>File only exists in source -> copy to destination</li>
	 * <li>File existed previously but is missing in destination -> delete source</li>
	 * <li>File exists in source and destination but not in sync state
	 * -> initial sync conflict, newest version wins</li>
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
	private void syncMaps( Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap, ArrayList<Map<Path, FileAttributes>> resultValue, Path startDestPath,
			Map<Path, FileAttributes> syncMap ) {

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
	private boolean isNewer(
			FileAttributes source,
			FileAttributes dest ) {

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
	private Map<Integer, Map<Path, FileAttributes>> splitMap( Map<Path, FileAttributes> map, int avProc ) {
		final Map<Integer, Map<Path, FileAttributes>> splitedMaps = Model.createMap();
		for( int i = 0; i < avProc; i++ ) {
			splitedMaps.put( i, Model.createMap() );
		}
		final int split = ( map.size() / avProc ) + 20;
		int i = 0;
		int j = 0;
		for( final Map.Entry<Path, FileAttributes> entry : map.entrySet() ) {
			if( i <= split ) {
				splitedMaps.get( j ).put( entry.getKey(), entry.getValue() );
				i++;
			}else {
				++j;
				i = 0;
				splitedMaps.get( j ).put( entry.getKey(), entry.getValue() );
			}
		}
		return splitedMaps;
	}
}
