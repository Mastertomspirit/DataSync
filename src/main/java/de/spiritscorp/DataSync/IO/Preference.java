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
package de.spiritscorp.DataSync.IO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.ScanType;
import jakarta.json.Json;
import jakarta.json.JsonValue;

public class Preference {

	private static final Path ROOT_PATH = Paths.get(System.getProperty("user.home"), "DataSync");
	public static final Path DEBUG_PATH = ROOT_PATH.resolve("debug.log");
	public static final Path ERROR_PATH = ROOT_PATH.resolve("debug.err");
	public static final Path LOG_PATH = ROOT_PATH.resolve("log.json");
	public static final Path CONFIG_PATH = ROOT_PATH.resolve("conf.json");
	public static final Path SCAN_TIME_PATH = ROOT_PATH.resolve("lastScanTime");
	
	private IOPrefs ioP;
	private ArrayList<Path> sourcePath = new ArrayList<>();
	private ArrayList<Path> destPath = new ArrayList<>();
	private Path startDestPath, trashbinPath;
	private String trashbinString = "Papierkorb";
	private ScanType deepScan = ScanType.FLAT_SCAN;
	private BgTime bgTime = BgTime.DAYLY;
	private int multiSourcePath, multiDestPath;
	private boolean isLoaded = false;
	private boolean logOn = true;
	private boolean subDir = false;
	private boolean autoDel, autoSync, bgSync, trashbin;
	private boolean autoBgDel = true;
	
	private final static Preference pref = new Preference();
	
	/**
	 * 
	 * @return <b>Preference</b>
	 */
	public static Preference getInstance() {
		return pref;
	}
	
	private Preference() {
		load();
	}
	
//	Check and load the preferences  
	private void load() {
		ioP = new IOPrefs();
		if(ioP.readPreferences()){
			try {
				multiSourcePath = ioP.getMultiPath("multiSourcePath");
				multiDestPath = ioP.getMultiPath("multiDestPath");
				deepScan = ioP.getScanType();
				bgTime = ioP.getBgTime();
				subDir = ioP.getBoolean("subDir");
				logOn = ioP.getBoolean("logOn");	
				autoDel = ioP.getBoolean("autoDel");
				autoBgDel = ioP.getBoolean("autoBgDel");
				autoSync = ioP.getBoolean("autoSync"); 
				bgSync = ioP.getBoolean("bgSync"); 
				trashbin = ioP.getBoolean("trashbin");
				startDestPath = ioP.getPath("startDestPath");
				trashbinPath = ioP.getPath("trashbinPath");
				for(int i = 1; i <= multiSourcePath; i++) {
					sourcePath.add(ioP.getPath("sourcePath" + i));
				}
				for(int i = 1; i <= multiDestPath; i++) {
					destPath.add(ioP.getPath("destPath" + i));
				}
			}catch(ConfigException e) {e.printStackTrace();}
		}
		if(!sourcePath.isEmpty() && !destPath.isEmpty()) {
			isLoaded = true;
		}else {
			multiSourcePath = 1;
			multiDestPath = 1;
			startDestPath = Paths.get(System.getProperty("user.home"));
			destPath.add(Paths.get(System.getProperty("user.home")));
			sourcePath.add(Paths.get(System.getProperty("user.home")));
			trashbinPath = startDestPath.resolve(trashbinString);
		}
	}

/**
 * 	Save the preferences
 * 
 * @return <b>boolean</b> true if success
 */
	public boolean savePrefs() {
		if(destPath != null && !sourcePath.isEmpty()) {
			for(int i = 1; i <= multiSourcePath; i++) {
				ioP.setPreferences("sourcePath" + i, Json.createValue(sourcePath.get(i-1).toString()));
			}
			for(int i = 1; i <= multiDestPath; i++) {
				ioP.setPreferences("destPath" + i, Json.createValue(destPath.get(i-1).toString()));
			}
			ioP.setPreferences("startDestPath", Json.createValue(startDestPath.toString()));
			ioP.setPreferences("trashbinPath", Json.createValue(trashbinPath.toString()));
			ioP.setPreferences("multiSourcePath", Json.createValue(multiSourcePath));
			ioP.setPreferences("multiDestPath", Json.createValue(multiDestPath));
			ioP.setPreferences("deepScan", Json.createValue(deepScan.getDescription()));
			ioP.setPreferences("subDir", (subDir) ? JsonValue.TRUE : JsonValue.FALSE);
			ioP.setPreferences("logOn", (logOn) ? JsonValue.TRUE : JsonValue.FALSE);
			ioP.setPreferences("autoDel", (autoDel) ? JsonValue.TRUE : JsonValue.FALSE);
			ioP.setPreferences("autoBgDel", (autoBgDel) ? JsonValue.TRUE : JsonValue.FALSE);
			ioP.setPreferences("autoSync", (autoSync) ? JsonValue.TRUE : JsonValue.FALSE); 
			ioP.setPreferences("bgSync", (bgSync) ? JsonValue.TRUE : JsonValue.FALSE); 
			ioP.setPreferences("trashbin", (trashbin) ? JsonValue.TRUE : JsonValue.FALSE);
			ioP.setPreferences("bgTime", Json.createValue(bgTime.getName()));
			ioP.writePreferences();
			return true;
		}
		return false;
	}
	
	/**
	 * Save the last scan time 
	 */
	public void saveLastScanTime() {
		try(BufferedWriter bw = Files.newBufferedWriter(Preference.SCAN_TIME_PATH, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)){
			bw.write(String.valueOf(System.currentTimeMillis()));
		}catch(IOException e) {	}
	}
	
	/**
	 * Get the last scan time
	 * 
	 * @return <b>long</b> lastScanTime
	 */
	public long getLastScanTime() {
		try(BufferedReader br = Files.newBufferedReader(Preference.SCAN_TIME_PATH)){
			String str = br.readLine();
			try { 
				return Long.valueOf(str);
			}catch(NullPointerException e) {
				return 0;
			}
		}catch(IOException e) {	}
		return 0;
	}
	
	public void setStartDestPath(Path startDestPath) {
		this.startDestPath = startDestPath;
		this.trashbinPath = startDestPath.resolve(trashbinString);
	}
	public void setDestPath(ArrayList<Path> paths) {
		multiDestPath = paths.size();
		destPath = paths;		
	}
	public void setSourcePath(ArrayList<Path> paths) {
		multiSourcePath = paths.size();
		sourcePath = paths;
	}
	public void setTrashbin(boolean trashbin) {
		this.trashbin = trashbin;
	}
	public void setScanMode(ScanType deepScan) {
		this.deepScan = deepScan;
	}
	public void setBgTime(BgTime bgTime) {
		this.bgTime = bgTime;
	}
	public void setBgSync(boolean bgSync) {
		this.bgSync = bgSync;
	}
	public void setLogOn(boolean logOn) {
		this.logOn = logOn;
	}
	public void setSubDir(boolean subDir) {
		this.subDir = subDir;
	}
	public void setAutoBgDel(boolean autoBgDel) {
		this.autoBgDel = autoBgDel;
	}
	public void setAutoDel(boolean autoDel) {
		this.autoDel = autoDel;
	}
	public void setAutoSync(boolean autoSync) {
		this.autoSync = autoSync;
	}
	public ArrayList<Path> getSourcePath() {
		return sourcePath;
	}
	public ArrayList<Path> getDestPath() {
		return destPath;
	}
	public ScanType getDeepScan() {
		return deepScan;
	}
	public Path getStartDestPath() {
		return startDestPath;
	}
	public Path getTrashbinPath() {
		return trashbinPath;
	}
	public boolean isLoaded() {
		return isLoaded;
	}
	public boolean isLogOn() {
		return logOn;
	}
	public boolean isSubDir() {
		return subDir;
	}
	public boolean isBgSync() {
		return bgSync;
	}
	public boolean isAutoDel() {
		return autoDel;
	}
	public boolean isAutoBgDel() {
		return autoBgDel;
	}
	public boolean isAutoSync() {
		return autoSync;
	}
	public boolean isTrashbin() {
		return trashbin;
	}
	public BgTime getBgTime() {
		return bgTime;
	}
}
