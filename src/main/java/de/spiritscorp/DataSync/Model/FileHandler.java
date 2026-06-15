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
	
	private Logger log;
	private int avgProc;		//	number of threads
	
	FileHandler(Logger log){
		this.log = log;
		avgProc = (Runtime.getRuntime().availableProcessors() > 3) ? ((int) (Runtime.getRuntime().availableProcessors() / 2 )) - 1 : 1;
	}
	
	/**
	 * List all files in the given directory and execute the filescan for attributes, and give back the results in a new Map
	 * 
	 * @param  paths
	 * @param  deepScan
	 * @param  subDir
	 * @return <b>Map</b> </br>A map with FileAttributes
	 */
	void listFiles(ArrayList<Path> paths, Map<Path, FileAttributes> resultMap, ScanType deepScan, boolean subDir) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		for(Path path : paths) {
			if(Files.exists(path)) {
				try {	
					if(subDir)		Files.walkFileTree(path, new FileVisit(executor, path.getParent(), resultMap, deepScan));
					else			Files.walkFileTree(path, new FileVisit(executor, path, resultMap, deepScan));
				}catch(IOException e) {e.printStackTrace();}
			}
		}
		executor.shutdown();
		try {
			while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
		} catch (InterruptedException e) {e.printStackTrace();}	
		Debug.PRINT_DEBUG("listFiles() -> ready  %s -> %s", Thread.currentThread().getName(), paths.get(0).toString());
	}

	/**
	 * Searches the given map and returns a new map with the duplicates
	 * 
	 * @param sourceMap The map to be checked
	 * @return <b>Map</b> </br>The map with sorted duplicates
	 */
	Map<Path, FileAttributes> findDuplicates(Map<Path, FileAttributes> sourceMap) {
		Debug.PRINT_DEBUG("entryPaths -> %d", sourceMap.size());
		Map<Path, FileAttributes> duplicateMap = Model.createMap();

		HashMap<Long, ArrayList<Path>> mapSize = new HashMap<>();
		for(Map.Entry<Path, FileAttributes> entry: sourceMap.entrySet()) {
			long size = entry.getValue().getSize();
			if(mapSize.containsKey(size)) {
				mapSize.get(size).add(entry.getKey());
			}else {
				mapSize.put(size, new ArrayList<>());
				mapSize.get(size).add(entry.getKey());
			}
		}
		
		for(Map.Entry<Long, ArrayList<Path>> entry : mapSize.entrySet()) {
			ArrayList<Path> paths = entry.getValue();
			if (paths.size() > 1) {
				for(int i = 0; i < paths.size(); i++) {
					final String firstPath = sourceMap.get(paths.get(i)).getFileHash();
					for(int j = i + 1; j < paths.size(); j++) {
						if(firstPath.equals(sourceMap.get(paths.get(j)).getFileHash())) {	
							duplicateMap.put(paths.get(i), sourceMap.get(paths.get(i)));
							duplicateMap.put(paths.get(j), sourceMap.get(paths.get(j)));
						}
					}
				}
			}
		}
		Debug.PRINT_DEBUG("duplicateList -> ready : size: %d", duplicateMap.size());
		return duplicateMap;
	}
	
	/**
	 * Cleans the maps of identical hits
	 * 
	 * @param sourceMap
	 * @param destMap
	 */
	void equalsFiles(Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap) {
		Debug.PRINT_DEBUG("max mem: %d, free mem: %d, total mem: %d", Runtime.getRuntime().maxMemory(),Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());
		if(sourceMap.size() != 0 && destMap.size() != 0) {		
			Set<Path> sourceHitList = Collections.synchronizedSet(new HashSet<>());
			Set<Path> destHitList = Collections.synchronizedSet(new HashSet<>());
			if(sourceMap.size() > 30_000) {
				ExecutorService executor = Executors.newFixedThreadPool(avgProc * 2);
				Map<Integer, Map<Path, FileAttributes>> splitSource = splitMap(sourceMap, avgProc);
				Map<Integer, Map<Path, FileAttributes>> splitDest = splitMap(destMap, avgProc);
				for(Map.Entry<Integer, Map<Path, FileAttributes>> source : splitSource.entrySet()) {
					executor.execute(new Thread(() -> equalsMap(source.getValue(), destMap, sourceHitList)));
				}
				for(Map.Entry<Integer, Map<Path, FileAttributes>> dest : splitDest.entrySet()) {
					executor.execute(new Thread(() -> equalsMap(dest.getValue(), sourceMap, destHitList)));
				}
				executor.shutdown();
				try {
					while(!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
				} catch (InterruptedException e) {e.printStackTrace();}
			}else {
				Thread t1 = new Thread(() -> 	equalsMap(sourceMap, destMap, sourceHitList));
				Thread t2 = new Thread(() -> 	equalsMap(destMap, sourceMap, destHitList));
				t1.start();
				t2.start();
				try {
					t1.join();
					t2.join();
				} catch (InterruptedException e) {e.printStackTrace();}
			}
			for (Path p : sourceHitList) {
				sourceMap.remove(p);
			}
			for (Path p : destHitList) {
				destMap.remove(p);
			}	
			Debug.PRINT_DEBUG("full source hitList size: %d  && full destination hitList size: %d",sourceMap.size(), destMap.size());
		}
	}
	
	/**
	 * Find out which file is the newest version or must be deleted and return the result.
	 * Determines synchronization actions by comparing file modification timestamps.
	 * 
	 * <p><strong>CRITICAL FIX (2026-06-13):</strong>
	 * Fixed bug where older file versions could overwrite newer versions.
	 * The algorithm now correctly compares modification times to ensure
	 * the latest version is always preserved.
	 * </p>
	 * 
	 * @param sourceMap Map of files from source directory
	 * @param destMap Map of files from destination directory
	 * @param startSourcePath Root path of source directory
	 * @param startDestPath Root path of destination directory
	 * @param syncMap Map containing last known synchronization state
	 * @return ArrayList containing three maps: [copySource, copyDest, delete]
	 */
	ArrayList<Map<Path,FileAttributes>> getSyncFiles(Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap, Path startSourcePath, Path startDestPath, Map<Path,FileAttributes> syncMap) {
		Debug.PRINT_DEBUG("max mem: %d, free mem: %d, total mem: %d", Runtime.getRuntime().maxMemory(),Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());
		ArrayList<Map<Path,FileAttributes>> resultValue = new ArrayList<>();
		ArrayList<Map<Path,FileAttributes>> destValue = new ArrayList<>();
		Map<Path, FileAttributes> copySourceHitList = Model.createMap();
		Map<Path, FileAttributes> copyDestHitList = Model.createMap();
		Map<Path, FileAttributes> delHitList = Model.createMap();

		resultValue.add(copySourceHitList);
		resultValue.add(copyDestHitList);
		resultValue.add(delHitList);
		destValue.add(copyDestHitList);
		destValue.add(copySourceHitList);
		destValue.add(delHitList);
		if(sourceMap.size() > 0 || destMap.size() > 0) {
			if(sourceMap.size() > 30_000 || destMap.size() > 30_000) {
				ExecutorService executor = Executors.newFixedThreadPool(avgProc * 2);
				Map<Integer, Map<Path, FileAttributes>> splitSource = splitMap(sourceMap, avgProc);
				Map<Integer, Map<Path, FileAttributes>> splitDest = splitMap(destMap, avgProc);
			
				for(Map.Entry<Integer, Map<Path, FileAttributes>> source : splitSource.entrySet()) {
					executor.execute(new Thread(() -> syncMaps(source.getValue(), destMap, resultValue, startDestPath, syncMap)));
				}
				for(Map.Entry<Integer, Map<Path, FileAttributes>> dest : splitDest.entrySet()) {
					executor.execute(new Thread(() -> syncMaps(dest.getValue(), sourceMap, destValue, startSourcePath, syncMap)));
				}
				executor.shutdown();
				try {
					while(!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
				} catch (InterruptedException e) {e.printStackTrace();}
				
			}else {
				syncMaps(sourceMap, destMap, resultValue, startDestPath, syncMap);
				syncMaps(destMap, sourceMap, destValue, startSourcePath, syncMap);			
			}
		}
		Debug.PRINT_DEBUG("full copySourceHitList size: %d  && full copyDestHitList size: %d  && full delHitList size: %d", copySourceHitList.size(), copyDestHitList.size(), delHitList.size());
		return resultValue;
	}

	/**
	 * Deletes files from the specified map with optional trashbin backup.
	 * 
	 * <p>For each file in the map:
	 * <ol>
	 *   <li>Optionally copies file to trashbin directory before deletion</li>
	 *   <li>Sets write permission if needed</li>
	 *   <li>Deletes the file</li>
	 *   <li>Logs the operation result</li>
	 * </ol>
	 * </p>
	 * 
	 * @param map Map of files to delete
	 * @param logOn If true, prints status after completion
	 * @param trashbin If true, copies files to trashbin before deletion
	 * @param trashbinPath Path to trashbin directory
	 */
	void deleteFiles(Map<Path,FileAttributes> map, boolean logOn, boolean trashbin, Path trashbinPath) {
		for (Path path : map.keySet()) {
			try {
				if(trashbin) {
					Files.createDirectories(trashbinPath.resolve(map.get(path).getRelativeFilePath()));
					Files.copy(path, trashbinPath.resolve(map.get(path).getRelativeFilePath()), StandardCopyOption.REPLACE_EXISTING);
				}
				if(!path.toFile().canWrite())		path.toFile().setWritable(true);
				Files.delete(path);
				log.setEntry(path.toString(), "gelöscht", map.get(path));
			}catch(IOException e) {
				log.setEntry(path.toString(), "FEHLER BEIM LÖSCHEN", map.get(path));
				Debug.PRINT_DEBUG("delete failed: %s", path.toString());
				e.printStackTrace();
			}
		}
		map.clear();
		if(logOn) log.printStatus();	
	}

	/**
	 * Copies files from source to destination preserving file attributes.
	 * 
	 * <p>For each file in the map:
	 * <ol>
	 *   <li>Creates parent directories if needed</li>
	 *   <li>Sets write permission on existing destination if needed</li>
	 *   <li>Copies file with REPLACE_EXISTING and COPY_ATTRIBUTES options</li>
	 *   <li>Restores original creation time</li>
	 *   <li>Logs the operation result</li>
	 * </ol>
	 * </p>
	 * 
	 * @param map Map of files to copy (key=source path, value=file attributes)
	 * @param logOn If true, prints status after completion
	 * @param destPath Destination directory path
	 */
	void copyFiles(Map<Path, FileAttributes> map, boolean logOn, Path destPath) {
		for (Map.Entry<Path, FileAttributes> entry : map.entrySet()) {
			Path path = destPath.resolve(entry.getValue().getRelativeFilePath());
			try {
				if(!Files.exists(path.getParent())) 									Files.createDirectories(path.getParent());
				else 
					if(Files.exists(path) && !path.toFile().canWrite())  		path.toFile().setWritable(true);	
				Files.copy(
						entry.getKey(), 
						path,
						StandardCopyOption.REPLACE_EXISTING, 
						StandardCopyOption.COPY_ATTRIBUTES);				
				Files.setAttribute(path, "creationTime", entry.getValue().getCreateTime());
				log.setEntry(path.toString(), "kopiert", entry.getValue());
			}catch(IOException e) {
				log.setEntry(path.toString(), "FEHLER BEIM KOPIEREN", entry.getValue());
				Debug.PRINT_DEBUG("copy failed: %s", path.toString());
				e.printStackTrace();
			}
		}
		map.clear();
		if(logOn) log.printStatus();	
	}
	
	/**
	 * Compares files in the iterate map with files in the full map and records matches.
	 * Files are considered equal if their FileAttributes objects are equal
	 * (same hash, size, modification time, and name).
	 * 
	 * @param iterateMap The map to iterate through (typically a split/partial map)
	 * @param fullMap The complete map to compare against
	 * @param hitList Set to accumulate matching file paths
	 */
	private void equalsMap(Map<Path, FileAttributes> iterateMap, Map<Path, FileAttributes> fullMap, Set<Path> hitList) {
		for (Map.Entry<Path, FileAttributes> entry : iterateMap.entrySet()) {
			if(fullMap.containsValue(entry.getValue())) {
				hitList.add(entry.getKey());
			}
		}		
	}
	
	/**
	 * Determines synchronization actions for all files contained in sourceMap.
	 *
	 * <p>
	 * The synchronization decision is based on:
	 * </p>
	 *
	 * <ul>
	 *   <li>Current source file state</li>
	 *   <li>Current destination file state</li>
	 *   <li>Last known synchronization state</li>
	 * </ul>
	 *
	 * <p>
	 * Rules:
	 * </p>
	 *
	 * <ol>
	 *   <li>File only exists in source -> copy to destination</li>
	 *   <li>File existed previously but is missing in destination -> delete source</li>
	 *   <li>File exists in source and destination but not in sync state
	 *       -> initial sync conflict, newest version wins</li>
	 *   <li>File exists in all locations and differs from sync state
	 *       -> newest version wins</li>
	 *   <li>Identical files -> no action</li>
	 * </ol>
	 *
	 * @param sourceMap Current source files
	 * @param destMap Current destination files
	 * @param resultValue Synchronization result:
	 *        [0] copy source -> destination
	 *        [1] copy destination -> source
	 *        [2] delete files
	 * @param startDestPath Destination root path
	 * @param syncMap Last synchronization snapshot
	 */
	private void syncMaps(Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap, ArrayList<Map<Path, FileAttributes>> resultValue, Path startDestPath, Map<Path, FileAttributes> syncMap) {
	
	    Map<Path, FileAttributes> copySourceHitList = resultValue.get(0);
	    Map<Path, FileAttributes> copyDestHitList = resultValue.get(1);
	    Map<Path, FileAttributes> delHitList = resultValue.get(2);
	
	    for (Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet()) {
	
	        Path relativePath = entry.getValue().getRelativeFilePath();
	        Path destPath = startDestPath.resolve(relativePath);
	
	        FileAttributes sourceAttributes = entry.getValue();
	        FileAttributes destAttributes = destMap.get(destPath);
	        FileAttributes syncAttributes = syncMap.get(relativePath);
	
	        /*
	         * -----------------------------------------------------------------
	         * CASE 1
	         * File exists only in source.
	         * -----------------------------------------------------------------
	         */
	        if (destAttributes == null) {
	            if (syncAttributes == null) {
	                // New file
	                copySourceHitList.put(entry.getKey(), sourceAttributes);
	            } else {
	                // File existed before but was deleted on destination
	                delHitList.put(entry.getKey(), sourceAttributes);
	            }
	            continue;
	        }
	
	        /*
	         * -----------------------------------------------------------------
	         * CASE 2
	         * File exists in source and destination.
	         * -----------------------------------------------------------------
	         */
	
	        if (sourceAttributes.equals(destAttributes)) {
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
	        if (syncAttributes == null) {
	            if (isNewer(sourceAttributes, destAttributes)) {
	                copySourceHitList.put(entry.getKey(), sourceAttributes);
	            } else {
	                copyDestHitList.put(destPath, destAttributes);
	            }
	            continue;
	        }
	
	        /*
	         * -----------------------------------------------------------------
	         * CASE 4
	         * File exists in sync history.
	         * -----------------------------------------------------------------
	         */
	
	        boolean sourceChanged = !sourceAttributes.equals(syncAttributes);
	        boolean destChanged = !destAttributes.equals(syncAttributes);
	
	        /*
	         * Source changed, destination unchanged.
	         */
	        if (sourceChanged && !destChanged) {
	            copySourceHitList.put(entry.getKey(), sourceAttributes);
	            continue;
	        }
	
	        /*
	         * Destination changed, source unchanged.
	         */
	        if (!sourceChanged && destChanged) {
	            copyDestHitList.put(destPath, destAttributes);
	            continue;
	        }
	
	        /*
	         * Conflict:
	         * both sides changed since last sync.
	         *
	         * Newest version wins.
	         */
	//	TODO Conflict Handling
			if (sourceChanged && destChanged) {
	            if (isNewer(sourceAttributes, destAttributes)) {
	                copySourceHitList.put(entry.getKey(), sourceAttributes);
	            } else {
	                copyDestHitList.put(destPath, destAttributes);
	            }
	        }
	    }
	}
	
	/**
	 * Returns true if source is newer than destination.
	 *
	 * @param source Source file attributes
	 * @param dest Destination file attributes
	 * @return true if source modification time is newer
	 */
	private boolean isNewer(
	        FileAttributes source,
	        FileAttributes dest) {
	
	    return source.getModTime().toMillis()
	            > dest.getModTime().toMillis();
	}
		
	/**
	 * Splits a map into smaller chunks for parallel processing.
	 * Used to optimize performance when dealing with large file sets.
	 * 
	 * <p>Maps are split based on the number of available processors.
	 * Each chunk receives approximately map.size() / avgProc entries.
	 * </p>
	 * 
	 * @param map The map to split
	 * @param avProc Number of threads/chunks to create
	 * @return Map of split maps indexed by integer keys (0 to avProc-1)
	 */
	private Map<Integer, Map<Path, FileAttributes>> splitMap(Map<Path, FileAttributes> map, int avProc) {
		Map<Integer, Map<Path, FileAttributes>> splitedMaps = Model.createMap();
		for(int i = 0; i < avProc; i++) {
			splitedMaps.put(i, Model.createMap());
		}
		int split = ((int) map.size() / avProc) + 20;
		int i = 0;
		int j = 0;
		for(Map.Entry<Path, FileAttributes> entry : map.entrySet()) {
			if ( i <= split) {
				splitedMaps.get(j).put(entry.getKey(), entry.getValue());
				i++;
			}else {
				++j;
				i = 0;
				splitedMaps.get(j).put(entry.getKey(), entry.getValue());
			}
		}
		return splitedMaps;
	}
}
