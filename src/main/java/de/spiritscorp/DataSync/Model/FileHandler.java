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
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Logger;

class FileHandler {
	
	private Logger log = new Logger();
	
	/**
	 * List all files in the given directory and execute the filescan for attributes, and give back the results in a new Map
	 * 
	 * @param paths
	 * @param deepScan
	 * @param subDir
	 * @return <b>Map</b> </br>A map with FileAttributes
	 */
	Map<Path, FileAttributes> listFiles(ArrayList<Path> paths, ScanType deepScan, boolean subDir) {
		Map<Path, FileAttributes> map = Collections.synchronizedSortedMap(new TreeMap<>());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		for(Path path : paths) {
			if(Files.exists(path)) {
				if(subDir)		listStream(path, executor, deepScan, map, path.getParent());
				else			listStream(path, executor, deepScan, map, path);
			}
		}
		executor.shutdown();
		try {
			while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {}
		} catch (InterruptedException e) {e.printStackTrace();}		
		return map;
	}
	
	/**
	 * Find all duplicates at the given Path and give back all duplicates
	 * 
	 * @param map The map to be checked
	 * @return <b>Map</b> </br>The map with sorted duplicates
	 */
	Map<Path, FileAttributes> findDuplicates(Map<Path, FileAttributes> map) {
		LinkedList<Path> duplicateList = new LinkedList<>();
		Path[] entryPaths = new Path[map.size()];
		map.keySet().toArray(entryPaths);
		
		for(int i = 0; i < entryPaths.length; i++) {
			for(int j = i + 1; j < entryPaths.length; j++) {
				if(map.get(entryPaths[i]).getFileHash().equals(map.get(entryPaths[j]).getFileHash())) {
					duplicateList.add(entryPaths[i]);
					duplicateList.add(entryPaths[j]);			}
			}
		}
		
		LinkedList<Path> uniqueList =  new LinkedList<>();
		for(Map.Entry<Path, FileAttributes> entry : map.entrySet()) {
			uniqueList.add(entry.getKey());
			for(Path path: duplicateList) {
				if(entry.getKey().equals(path)) {
					uniqueList.remove(path);
					break; 
				}
			}
		}
		
		for(Path path : uniqueList) {
			map.remove(path);
		}
		return map;
	}
	
	/**
	 * Equals the source map with the destination map and remove same files on both maps
	 * 
	 * @param sourceMap
	 * @param destMap
	 */
	void equalsFiles(Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap) {
		LinkedList<Path> sourceList = new LinkedList<>();
		LinkedList<Path> destList = new LinkedList<>();
		for (Map.Entry<Path, FileAttributes> source : sourceMap.entrySet()) {
			if(destMap.containsKey(source.getKey())) {
				if (source.getValue().equals(destMap.get(source.getKey()))) {
					sourceList.add(source.getKey());
					destList.add(source.getKey());
					break;
				}
			}
		}
		for (Path p : sourceList) {
			sourceMap.remove(p);
		}
		for (Path p : destList) {
			destMap.remove(p);
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
//		TODO		rename existing file names with different attributes
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
	
// TODO empty dirs in an extra list, or so... 
	private void listStream(Path dir, ExecutorService executor, ScanType deepScan, Map<Path, FileAttributes> map, Path path) {
		try {
			Files.walk(dir, Integer.MAX_VALUE)
				.filter((p) -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
				.filter((p) -> Files.isReadable(p))
				.filter((p) -> Files.isRegularFile(p))			
				.filter((p) -> !path.relativize(p).startsWith("Papierkorb"))
				.forEach((p) -> executor.execute(new FileScan(p, path, map, deepScan)));
		} catch (AccessDeniedException ee) {}
		  catch (IOException e) {e.printStackTrace();}
	}
}
