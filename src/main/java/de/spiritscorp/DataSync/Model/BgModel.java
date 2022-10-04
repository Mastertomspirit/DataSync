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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;

public class BgModel {

	private final Preference pref;
	private final FileHandler handler;
	private Map<Path,FileAttributes> sourceMap, destMap;
	
	public BgModel(Preference pref, Logger logger) {
		this.pref = pref;
		handler = new FileHandler(logger);
	}

	/**
	 * List all files, equals them and make the prefer sync 
	 * 
	 * @return <b>boolean</b> </br>true if the process ran and both maps are empty
	 */
	public boolean runBgJob(){
		boolean logOn = pref.isLogOn();
		Path startDestPath = pref.getStartDestPath();
		Path trashbinPath = pref.getTrashbinPath();
		boolean trashbin = pref.isTrashbin();
		boolean autoBgDel = pref.isAutoBgDel();
		Debug.PRINT_DEBUG("time since last scan: %d", System.currentTimeMillis() - pref.getLastScanTime());
		if(Files.exists(pref.getStartDestPath())) {
			if(System.currentTimeMillis() - pref.getLastScanTime()  > pref.getBgTime().getTime()){
				Debug.PRINT_DEBUG("bgJob started");
				Thread t1 = new Thread(() -> sourceMap = handler.listFiles(pref.getSourcePath(), ScanType.FLAT_SCAN, pref.isSubDir()));
				Thread t2 = new Thread(() -> destMap = handler.listFiles(pref.getDestPath(), ScanType.FLAT_SCAN, pref.isSubDir()));
				t1.start();
				t2.start();
				try {
					t1.join();
					t2.join();
				} catch (InterruptedException e) {e.printStackTrace();}
				handler.equalsFiles(sourceMap, destMap);
				Debug.PRINT_DEBUG("unique source path size: %d  && unique destination path size: %d", sourceMap.size(), destMap.size());
				if(autoBgDel && !destMap.isEmpty())		handler.deleteFiles(destMap, logOn, trashbin, trashbinPath);
				if(!sourceMap.isEmpty())							handler.copyFiles(sourceMap, logOn, startDestPath);
				pref.saveLastScanTime();
				
				return (sourceMap.isEmpty() && destMap.isEmpty());
			}
		}
		return false;
	}
}
