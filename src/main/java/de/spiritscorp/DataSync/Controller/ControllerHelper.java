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
package de.spiritscorp.DataSync.Controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import de.spiritscorp.DataSync.Main;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.Gui.FileChooser;
import de.spiritscorp.DataSync.Gui.View;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Model.FileAttributes;
import de.spiritscorp.DataSync.Model.Model;

class ControllerHelper {
	
	private final Model model;
	private final Preference pref;
	private Map<Path,FileAttributes> sourceMap, destMap;
	private boolean scanRun;
	
	/**
	 * Helper methods for controller
	 * 
	 * @param model	The model
	 * @param pref The preferences
	 */
	ControllerHelper(final Model model, final Preference pref){
		this.model = model;
		this.pref = pref;
		scanRun = false;
	}

	/**
	 * Choose one or more source directories and one destination Path
	 * 
	 * @param view The view
	 */
	void selectButton(View view) {
		ArrayList<Path> sourcePaths = new ArrayList<Path>();
		ArrayList<Path> destPaths = new ArrayList<>();
		int i = 0;
		int sourcePathSize = pref.getSourcePath().size();

		do{
			Path preferendPath;
			if(sourcePathSize - i > 0) {
				preferendPath = pref.getSourcePath().get(i);
			}else {
				preferendPath = sourcePaths.get(i-1);
			}
			try{
				sourcePaths.add(new FileChooser("Quell", preferendPath).getSelectedFile().toPath());
				i++;
			}catch(NullPointerException ne) {}
		}while(JOptionPane.showConfirmDialog(view, "Einen weiteren Ordner hinzufügen?", "Multiauswahl bestätigen", 0,3) == 0);		
		try {
			if(sourcePaths.isEmpty()) throw new NullPointerException();
			if(sourcePaths.size() > 1 && (JOptionPane.showConfirmDialog(view, "Unterordner im Zielverzeichnis erstellen?", "Unterordner Erstellung", 0, 3) == 0)) {
				pref.setSubDir(true);
			}else {
				pref.setSubDir(false);
			}
			Path path = new FileChooser("Ziel", pref.getStartDestPath()).getSelectedFile().toPath();

			for(Path sopath : sourcePaths) {
				view.setTextArea("Quell-Ordner gewählt:   " + sopath.toAbsolutePath().toString());
			}
			view.setTextArea("Ziel-Ordner gewählt:   " + path.toAbsolutePath().toString());
			if(sourcePaths.size() > 1 && pref.isSubDir()){
				view.setTextArea("Unterordner für Zielverzeichnis erstellt");
				for(Path soPath : sourcePaths) {
					destPaths.add(path.resolve(soPath.subpath( soPath.getNameCount() -1, soPath.getNameCount())));
				}
			}else {
				destPaths.add(path);
			}
			view.setSourceLabel(sourcePaths);
			pref.setSourcePath(sourcePaths);
			pref.setDestPath(destPaths);
			view.setDestLabel(path.toAbsolutePath().toString());
			pref.setStartDestPath(path);
			view.getTrashbinCheck().setToolTipText(pref.getTrashbinPath().toString());
		}catch(NullPointerException ne) {
			view.setTextArea("Auswählen abgebrochen");
		}
	}
	
	/**
	 * Scan the directories and synchronize it
	 * 
	 * @param view The view
	 */
	void startSync(View view) {
				scanRun = true;
				view.setScanRun(true);

				ScanType deepScan = pref.getDeepScan();
				Path startDestPath = pref.getStartDestPath();
				Path trashbinPath = pref.getTrashbinPath();
				boolean trashbin = pref.isTrashbin();
				boolean logOn = pref.isLogOn();
				Long[] stats = new Long[4];				
				long startTime = System.nanoTime();
				
				HashMap<String, Map<Path, FileAttributes>> hashMap  = model.scanSyncFiles(pref.getSourcePath(), pref.getDestPath(), stats, pref.getDeepScan(), pref.isSubDir(), pref.isTrashbin());
				String endTimeFormatted = getEndTimeFormatted(System.nanoTime() - startTime);
				sourceMap = hashMap.get("sourceMap");
				destMap = hashMap.get("destMap");
				view.setTextArea(formatMaps(deepScan));
				view.setTextArea(String.format("Quelldateien: %d Stück und Zieldateien: %d Stück", stats[0], stats[1]));
				view.setTextArea(String.format("Größe aller Quelldateien: %s      Größe aller Zieldateien: %s", getReadableBytes(stats[2]), getReadableBytes(stats[3])));
				view.setTextArea(endTimeFormatted);

				int del, ok;
				if(pref.isAutoDel()) {
					del  = 0;
				}else {
					del = JOptionPane.showConfirmDialog(view, "Löschen bestätigen?", "Gelöschte Dateien entfernen", 0, 0);
				}
				if(pref.isAutoSync()) ok = 0;
				else ok = JOptionPane.showConfirmDialog(view, "Dateien Syncronisieren?", "Syncronisations Bestätigung", 0, 2);
				if(ok == 0) {
					if(model.syncFiles(del, logOn, startDestPath, trashbin, trashbinPath)) {
						view.setTextArea("Syncronisation erfolgreich");
					}else {
						view.setTextArea("Syncronisation fehlgeschlagen");
					}
				}else {
					view.setTextArea("Syncronisation abgebrochen!");
				}
				scanRun = false;
				view.setScanRun(false);
	}
	
	/**
	 * Scan the source directories and show duplicates
	 * 
	 * @param view The view
	 */
	void startDuplicateScan(View view) {
				scanRun = true;
				view.setScanRun(true);
				long startTime = System.nanoTime();
				sourceMap  = model.scanDublicates(pref.getSourcePath(), pref.isSubDir());
				String endTimeFormatted = getEndTimeFormatted(System.nanoTime() - startTime);
				long space = 0;
				for(Path p : sourceMap.keySet()) {
					space += sourceMap.get(p).getSize();
				}
				space /= 2;
				view.setTextArea(formatMaps(ScanType.DUBLICATE_SCAN));
				view.setTextArea("Duplicate gefunden: " + (sourceMap.size() / 2) + " Stück");
				view.setTextArea("Doppelt belegter Speicherplatz: " + getReadableBytes(space));
				view.setTextArea(endTimeFormatted);
				scanRun = false;
				view.setScanRun(false);
	}
	
/**
 * Set and delete the system autostart for Windows and Linux </br></br>
 * 
 * <b><i>NO OSX SUPPORT PLANED</i></b>
 * 
 * @param set When set is true, entry will delete
 */
	public void setOSAutostart(boolean set) {
		String javaPath = System.getProperty("sun.boot.library.path");
		String exePath = System.getProperty("jpackage.app-path");
		String datei = System.getProperty("sun.java.command");
		String fullPath = Paths.get("").toAbsolutePath().toString() + System.getProperty("file.separator") + datei;
		String os = System.getProperty("os.name").toLowerCase();

		if(os.contains("win")) {
//			First value is for compile only the jar
			String value;
			if(exePath == null) {
				value = "/t  REG_SZ /d \"\\\"" + javaPath + "\\javaw.exe\\\" -Xmx200m -jar \\\"" + datei + "\\\" firstStart\"";
			}else{
				value = "/t REG_SZ /d \"" + exePath + " firstStart\"";
			}
			String cmd = "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v DataSync /f ";
			try {
				if(set)		Runtime.getRuntime().exec("cmd /c reg add " + cmd + value);
				else		Runtime.getRuntime().exec("cmd /c reg delete " + cmd);
			} catch (IOException e) {e.printStackTrace();}
			
		}else if (os.contains("nix") || os.contains("aix") || os.contains("nux")){
//			TODO		Not working -> EOF not set  -> crontab can't close the stream
//			Problem		Files.writeString()
//						Workaround move the file directly on the linux system
//			First value is for compile only the jar
			String value;
			if(exePath == null) {
				value = String.format("@reboot %s/java -jar \"%s\" firstStart", javaPath, fullPath);	
			}else {
				value = String.format("@reboot %s firstStart", exePath);
			}
			Path pathTemp = Paths.get("linx1.txt").toAbsolutePath();
			Path path = Paths.get("linx.txt").toAbsolutePath();
			try {
				Files.writeString(pathTemp, value, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
				Runtime.getRuntime().exec("mv " + pathTemp + " " + path);
				if(set)	{
					Runtime.getRuntime().exec("crontab " + path.toString());
					if(Main.debug) {
						System.out.println("crontab " + path.toString());
					}
				}
				else	{
					Runtime.getRuntime().exec("crontab -r " + path.toString());
					if(Main.debug) {
						System.out.println("crontab -r " + path.toString());
					}
				}
			} catch (IOException e) {e.printStackTrace();}
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {e.printStackTrace();}
		}else if (os.contains("osx")){}
		    // Apple OSX
	}

	boolean isScanRun() {
		return scanRun;
	}
	
	void setScanRun(boolean scanRun) {
		this.scanRun = scanRun;
	}
	
	/**
	 * 	Give back a formatted string for visualizing at the textArea
	 * 
	 * @param	deepScan Witch ScanType
	 * @return <b>String</b> </br>The formatted string
	 */
	private String formatMaps(ScanType deepScan) {
		String line = System.lineSeparator();
		StringBuffer sb = new StringBuffer();
		if(deepScan == ScanType.DUBLICATE_SCAN) {
			sb.append("Scan abgeschlossen!" + line);
			sb.append("Doppelte Dateien:" + line);
			sb.append("----------------------" + line);
			String[] str = new String[sourceMap.size()];
			int i = 0;
			for (Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet()) {
				FileAttributes value = entry.getValue();

				str[i] = value.getFileName() + " , " + 
							getReadableBytes(value.getSize()) + " , " +
							value.getModTime() + " , " + 
							value.getCreateTime() + " , " + 
							value.getFileHash() + "         " + entry.getKey().toString() + line;
				i++;
			}			
			Arrays.sort(str);
			for(String s : str) {
				sb.append(s);
			}
		}else {
			sb.append("Scan abgeschlossen!" + line);
			sb.append("Zu kopierende Dateien:" + line);
			sb.append("----------------------" + line);
			int displayLimit = 0;
			for (Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet()) {
				FileAttributes value = entry.getValue();
				sb.append(value.getFileName() + " , " +
							getReadableBytes(value.getSize()) + " , " +
							value.getModTime() + " , " +
							value.getCreateTime() + " , " +
							value.getFileHash() + "  " +
							"   " + entry.getKey().toString() +
							line);
				displayLimit++;
				if (displayLimit > 5000) break;
			}
			displayLimit = 0;
			sb.append(line);
			sb.append("Zu löschende Dateien:" + line);
			sb.append("---------------------" + line);
			for (Map.Entry<Path, FileAttributes> entry : destMap.entrySet()) {
				FileAttributes value = entry.getValue();
				sb.append(value.getFileName() + " , " +
						getReadableBytes(value.getSize()) + " , " +
						value.getModTime() + " , " +
						value.getCreateTime() + " , " +
						value.getFileHash() + "  " +
						"   " + entry.getKey().toString() +
						line);
				displayLimit++;
				if (displayLimit > 5000) break;
			}
		}
		return new String(sb);
	}
	
	/**
	 * Formatting nanoseconds to hour, minutes, or seconds
	 * 
	 * @param endTimeNano
	 * @return <b>String</b> </br>The formatted string
	 */
	private String getEndTimeFormatted(long endTimeNano) {
		double endTimeSec = ((double) endTimeNano) / 1000000000;		
		if(endTimeSec >= 7200)			return String.format("%d Stunden  %d Minuten  %.3f Sekunden Laufzeit", ((int) endTimeSec) / 3600, ((int) endTimeSec) % 60, endTimeSec % 60 );
		else if(endTimeSec >= 3600)		return String.format("%d Stunde  %d Minuten  %.3f Sekunden Laufzeit", ((int) endTimeSec) / 3600, ((int) endTimeSec) % 60, endTimeSec % 60 );
		else if(endTimeSec >= 60) 		return String.format("%d Minuten  %.3f Sekunden Laufzeit", ((int) endTimeSec) / 60, endTimeSec % 60 );
		else							return String.valueOf(endTimeSec) + " Sekunden Laufzeit"; 

	}
	
	/**
	 * Formatting bytes to GiB, MiB, KiB, bytes
	 * 
	 * @param bytes The bytes
	 * @return <b>String</b> </br>The formatted string
	 */
	private String getReadableBytes(long bytes) {
		if (bytes > 1073741824)		return String.format("%.3f GiB", ((double)(bytes / 1048576)) / 1024);
		else if(bytes > 1048576)	return (bytes / 1048576) + " MiB";
		else if(bytes > 1024)		return (bytes / 1024) + " KiB";
		else if(bytes > 1)			return bytes + " bytes";
		else						return bytes + " byte";
	}	
}
