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
import java.util.HashMap;
import java.util.Map;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;

public class Model {

	private Map<Path, FileAttributes> sourceMap, destMap;
	private FileHandler handler;

	public Model(Logger logger) {
		handler = new FileHandler(logger);
	}
	
/**
 * 	Read the file attributes in both directories in a few threads, equals with each other and filter them. The remaining will give back
 * 
 * @param sourcePathes
 * @param destPathes
 * @param stats An array with length 4, for statistics
 * @param deepScan
 * @param subDir
 * @param trashbin
 * @return <b>HashMap</b> with source Map and destination Map
 */
	public HashMap<String, Map<Path,FileAttributes>> scanSyncFiles(ArrayList<Path> sourcePathes, ArrayList<Path> destPathes, Long[] stats, ScanType deepScan, boolean subDir, boolean trashbin) {
		Thread t1 = new Thread(() -> sourceMap = handler.listFiles(sourcePathes, deepScan, subDir));
		Thread t2 = new Thread(() -> destMap = handler.listFiles(destPathes, deepScan, subDir));
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
		Debug.PRINT_DEBUG("equalsFiles start");
		Debug.PRINT_DEBUG("sourceMap.size = %d, destMap.size = %d", sourceMap.size(), destMap.size());
		handler.equalsFiles(sourceMap, destMap);
		HashMap<String, Map<Path,FileAttributes>> hashMap = new HashMap<>();
		hashMap.put("destMap", destMap);
		hashMap.put("sourceMap", sourceMap);
		return hashMap;
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
	public boolean syncFiles(int del, boolean logOn, Path destPath, boolean trashbin, Path trashbinPath) {
		if (del == 0 && !destMap.isEmpty())		handler.deleteFiles(destMap, logOn, trashbin, trashbinPath);
		if(!sourceMap.isEmpty())		handler.copyFiles(sourceMap, logOn, destPath);
		return (sourceMap.isEmpty() && destMap.isEmpty());
	}
	
	/**
	 * 
	 * @param paths
	 * @param subDir
	 * @return <b>Map</b> </br>Map with duplicates 
	 */
	public Map<Path, FileAttributes> scanDublicates(ArrayList<Path> paths) {
		sourceMap = handler.listFiles(paths, ScanType.DUBLICATE_SCAN, false);
		sourceMap = handler.findDuplicates(sourceMap);
//		TODO View needs a way to change the entrys
		return sourceMap;
	}
	
	private Long getBytes(Map<Path, FileAttributes> map) {
		long allBytes = 0;
		for(Path p : map.keySet()) {
			allBytes += map.get(p).getSize();
		}
		return allBytes;
	}
}
