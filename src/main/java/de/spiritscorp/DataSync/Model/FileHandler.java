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
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;

class FileHandler {
	
	private Logger log;
	
	FileHandler(Logger log){
		this.log = log;
	}
	/**
	 * List all files in the given directory and execute the filescan for attributes, and give back the results in a new Map
	 * 
	 * @param  paths
	 * @param  deepScan
	 * @param  subDir
	 * @return <b>Map</b> </br>A map with FileAttributes
	 */
	Map<Path, FileAttributes> listFiles(ArrayList<Path> paths, ScanType deepScan, boolean subDir) {
		Map<Path, FileAttributes> map = createMap();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		for(Path path : paths) {
			if(Files.exists(path)) {
				try {	
					if(subDir)		Files.walkFileTree(path, new FileVisit(executor, path.getParent(), map, deepScan));
					else			Files.walkFileTree(path, new FileVisit(executor, path, map, deepScan));
				}catch(IOException e) {e.printStackTrace();}
			}
		}
		executor.shutdown();
		try {
			while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
		} catch (InterruptedException e) {e.printStackTrace();}	
		Debug.PRINT_DEBUG("listFiles() -> ready");
		return map;
	}

	/**
	 * Searches the given map and returns a new map with the duplicates
	 * 
	 * @param sourceMap The map to be checked
	 * @return <b>Map</b> </br>The map with sorted duplicates
	 */
	Map<Path, FileAttributes> findDuplicates(Map<Path, FileAttributes> sourceMap) {
		Debug.PRINT_DEBUG("entryPaths -> %d", sourceMap.size());
		Map<Path, FileAttributes> duplicateMap = createMap();

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
		if(sourceMap.size() != 0 && destMap.size() != 0) {		
			Set<Path> sourceHitList = Collections.synchronizedSet(new HashSet<>());
			Set<Path> destHitList = Collections.synchronizedSet(new HashSet<>());
			int avProc = (Runtime.getRuntime().availableProcessors() > 3) ? ((int) (Runtime.getRuntime().availableProcessors() / 2 )) - 1 : 1;
			if(sourceMap.size() > 30_000) {
				ExecutorService executor = Executors.newFixedThreadPool(avProc * 2);
				Map<Integer, Map<Path, FileAttributes>> splitSource = splitMap(sourceMap, avProc);
				Map<Integer, Map<Path, FileAttributes>> splitDest = splitMap(destMap, avProc);
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
			Debug.PRINT_DEBUG("full source path size: %d  && full destination path size: %d", sourceHitList.size(), destHitList.size());
		}
	}

	/**
	 * 
	 * @param map
	 * @param logOn
	 * @param trashbin
	 * @param trashbinPath
	 */
	void deleteFiles(Map<Path,FileAttributes> map, boolean logOn, boolean trashbin, Path trashbinPath) {
		try {
			for (Path path : map.keySet()) {
				if(trashbin) {
					Files.createDirectories(trashbinPath.resolve(map.get(path).getRelativeFilePath()));
					Files.copy(path, trashbinPath.resolve(map.get(path).getRelativeFilePath()), StandardCopyOption.REPLACE_EXISTING);
				}
				Files.delete(path);
				log.setEntry(path.toString(), "gelöscht", map.get(path));
			}
		}catch(IOException e) {e.printStackTrace();}
		map.clear();
		if(logOn) log.printStatus();	
	}

	/**
	 * 
	 * @param map
	 * @param logOn
	 * @param destPath
	 */
	void copyFiles(Map<Path, FileAttributes> map, boolean logOn, Path destPath) {
		for (Map.Entry<Path, FileAttributes> entry : map.entrySet()) {
			Path path = destPath.resolve(entry.getValue().getRelativeFilePath());
			try {
				if(!Files.exists(path.getParent()))	Files.createDirectories(path.getParent());
				Files.copy(
						entry.getKey(), 
						path,
						StandardCopyOption.REPLACE_EXISTING, 
						StandardCopyOption.COPY_ATTRIBUTES);				
				Files.setAttribute(path, "creationTime", entry.getValue().getCreateTimeFileTime());
				log.setEntry(path.toString(), "kopiert", entry.getValue());
			}catch(IOException e) {
				if(logOn) log.setEntry(path.toString(), "FEHLER BEIM KOPIEREN", entry.getValue());
				System.err.println("Fehler beim kopieren von " + path.toString());
				e.printStackTrace();
			}
		}
		map.clear();
		if(logOn) log.printStatus();	
	}
	
	/**
	 * compares the iterateMap entries with the fullMap and writes hits in the hitList
	 * 
	 * @param iterateMap The splitMap
	 * @param fullMap 
	 * @param hitList Set with the hits
	 * @param mapName
	 */
	private void equalsMap(Map<Path, FileAttributes> iterateMap, Map<Path, FileAttributes> fullMap, Set<Path> hitList) {
		Map<Path, FileAttributes> tempMap = Map.copyOf(fullMap);
		for (Map.Entry<Path, FileAttributes> entry : iterateMap.entrySet()) {
			if(tempMap.containsValue(entry.getValue())) {
				hitList.add(entry.getKey());
			}
		}		
	}
	
	/**
	 * splits the map depending on the processor cores
	 * 
	 * @param map
	 * @return  <b>Map</b> </br>The map with split maps
	 */
	private Map<Integer, Map<Path, FileAttributes>> splitMap(Map<Path, FileAttributes> map, int avProc) {
		Map<Integer, Map<Path, FileAttributes>> splitedMaps = createMap();
		for(int i = 0; i < avProc; i++) {
			splitedMaps.put(i, createMap());
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
	
	/**
	 * 
	 * @param <K>
	 * @param <V>
	 * @return Collections.synchronizedMap(new TreeMap<>())
	 */
	private <K, V> Map<K, V> createMap(){
		return Collections.synchronizedMap(new TreeMap<>());
	}
}
