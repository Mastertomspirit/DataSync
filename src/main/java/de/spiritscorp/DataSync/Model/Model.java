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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;

public class Model {

	private Map<Path, FileAttributes> sourceMap, destMap;
	private FileHandler handler;

	/**
	 * 
	 * @param logger
	 * @param sourceMap
	 * @param destMap
	 */
	public Model(Logger logger, Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap) {
		this.sourceMap = sourceMap;
		this.destMap = destMap;
		handler = new FileHandler(logger);
	}
	
	/**
	 * Create a consistent Map
	 * 
	 * @param <K>
	 * @param <V>
	 * @return Collections.synchronizedSortedMap(new TreeMap<>())
	 */
	public static final <K, V> Map<K, V> createMap(){
		return Collections.synchronizedSortedMap(new TreeMap<>());
	}
	
	/**
	 * 	List the files in both directories in two threads and read the attributes
	 * 
	 * @param sourcePathes
	 * @param destPathes
	 * @param stats An array with length 4, for statistics
	 * @param deepScan
	 * @param subDir
	 * @param trashbin
	 * @return 
	 */
	public Map<Path, FileAttributes>  scanSyncFiles(ArrayList<Path> sourcePathes, ArrayList<Path> destPathes, Long[] stats, ScanType deepScan, boolean subDir, boolean trashbin) {
		Debug.PRINT_DEBUG("list start");
		Thread t1 = new Thread(() -> handler.listFiles(sourcePathes, sourceMap, deepScan, subDir));
		Thread t2 = new Thread(() -> handler.listFiles(destPathes, destMap, deepScan, subDir));
		t1.start();
		t2.start();
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {e.printStackTrace();}
		stats[0] = (long) sourceMap.size();
		stats[1] = (long) destMap.size();
		stats[2] = getBytes(sourceMap);
		stats[3] = getBytes(destMap);
		Debug.PRINT_DEBUG("list ready");
		return getFailtures(sourceMap, destMap);
	}
	
	/**
	 * Equals the files and set unique in the maps
	 */
	public void getEqualsFiles(){
		Debug.PRINT_DEBUG("getEqualsFiles start");
		handler.equalsFiles(sourceMap, destMap);
		Debug.PRINT_DEBUG("getEqualsFiles ready");
	}
	
	/**
	 * Find out witch is the newest, or must delete and give back the result
	 * 
	 * @param syncMap
	 * @param sourcePath
	 * @param destPath
	 * @return <b>ArrayList<Map<Path,FileAttributes>></b> </br>Give back the copySourceHitList, the copyDestHitList and the delHitList
	 */
	public ArrayList<Map<Path,FileAttributes>> getSyncFiles(Map<Path, FileAttributes> syncMap, Path sourcePath, Path destPath){
		Debug.PRINT_DEBUG("getSyncFiles start");
		if(syncMap.isEmpty()) {
			Map<Path, FileAttributes> tempSyncMap = createMap(); 
			handler.listFiles(Preference.getInstance().getSourcePath(), tempSyncMap, ScanType.SYNCHRONIZE, false);
			for(Map.Entry<Path,FileAttributes>  entry : tempSyncMap.entrySet()) {
				syncMap.put(entry.getValue().getRelativeFilePath(), entry.getValue());
			}
		}
		ArrayList<Map<Path, FileAttributes>> result = handler.getSyncFiles(sourceMap, destMap, sourcePath, destPath, syncMap);
		Debug.PRINT_DEBUG("getSyncFiles ready");
		return result;
	}

	/**
	 * 	Delete files first and copy then the new files into the destination Path
	 * 
	 * @param del
	 * @param logOn
	 * @param destPath
	 * @param trashbin
	 * @param trashbinPath
	 * @return	<b>boolean</b> 	</br>false on an Error
	 */
	public boolean backupFiles(int del, boolean logOn, Path destPath, boolean trashbin, Path trashbinPath) {
		if (del == 0 && !destMap.isEmpty())		handler.deleteFiles(destMap, logOn, trashbin, trashbinPath);
		if(!sourceMap.isEmpty())					handler.copyFiles(sourceMap, logOn, destPath);
		return (sourceMap.isEmpty() && destMap.isEmpty());
	}
	
	/**
	 * Copy the newest and clear the deleted once
	 * 
	 * @param result
	 * @param syncMap
	 * @param sourcePath
	 * @param destPath
	 * @param testOn Only set on test cases
	 * @return <b>boolean</b> 	</br>false an Error
	 */
	public boolean syncFiles(ArrayList<Map<Path,FileAttributes>> result, Map<Path, FileAttributes> syncMap, Path sourcePath, Path destPath, boolean testOn) {
		if(!result.get(0).isEmpty())			handler.copyFiles(result.get(0), false, destPath);
		if(!result.get(1).isEmpty())			handler.copyFiles(result.get(1), false, sourcePath);
		if(!result.get(2).isEmpty())			handler.deleteFiles(result.get(2), false, false, null);
		
		sourceMap.clear();
		destMap.clear();
		syncMap.clear();
		if(!testOn) {
			Map<Path, FileAttributes> tempMap = createMap();
			handler.listFiles(Preference.getInstance().getSourcePath(), tempMap, ScanType.SYNCHRONIZE, false);
			for(Map.Entry<Path,FileAttributes>  entry : tempMap.entrySet()) {
				syncMap.put(entry.getValue().getRelativeFilePath(), entry.getValue());
			}
			Preference.getInstance().writeSyncMap();
		}
		return result.get(0).isEmpty() && result.get(1).isEmpty() && result.get(2).isEmpty();
	}
	
	/**
	 * 
	 * @param paths
	 * @return <b>Map</b> </br>Map with duplicates 
	 */
	public Map<Path, FileAttributes> scanDublicates(ArrayList<Path> paths) {
		handler.listFiles(paths, sourceMap, ScanType.DUBLICATE_SCAN, false);
		sourceMap = handler.findDuplicates(sourceMap);
		return getFailtures(sourceMap, destMap);
	}
	
	/**
	 * create the failMap and collect the failtures
	 * 
	 * @param sourceMap
	 * @param destMap
	 * @return failMap
	 */
	private Map<Path,FileAttributes> getFailtures(Map<Path,FileAttributes> sourceMap, Map<Path,FileAttributes> destMap){
		Map<Path,FileAttributes> failMap = Model.createMap();
		if(sourceMap != null) {
			for(Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet()) {
				if(entry.getValue().getFileHash().equals("Failed")) {
					failMap.put(entry.getKey(), entry.getValue());
				}
			}
		}
		if(destMap != null) {
			for(Map.Entry<Path, FileAttributes> entry : destMap.entrySet()) {
				if(entry.getValue().getFileHash().equals("Failed")) {
					failMap.put(entry.getKey(), entry.getValue());
				}
			}		
		}
		return failMap;
	}

	private Long getBytes(Map<Path, FileAttributes> map) {
		long allBytes = 0;
		for(Path p : map.keySet()) {
			allBytes += map.get(p).getSize();
		}
		return allBytes;
	}
}
