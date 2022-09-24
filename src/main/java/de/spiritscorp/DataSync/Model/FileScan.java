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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;

class FileScan  implements Runnable{

	private Path path, startPath;
	private Map<Path, FileAttributes> map;
	private ScanType scanType;
	private BasicFileAttributes bfa;
	
	/**
	 * Scan the attributes of one file
	 * 
	 * @param path
	 * @param startPath
	 * @param map
	 * @param scanType
	 */
	FileScan(Path path, Path startPath, Map<Path, FileAttributes> map, ScanType scanType, BasicFileAttributes bfa){
		this.path = path;
		this.startPath = startPath;
		this.map = map;
		this.scanType = scanType;
		this.bfa = bfa;
	}
	
	@Override
	public void run() {
			FileAttributes fa = new FileAttributes(
					relativePath(), 
					fileTimeToString(bfa.creationTime()), 
					bfa.creationTime(),
					fileTimeToString(bfa.lastModifiedTime()), 
					bfa.size(), 
					deepScan()
			);
			map.put(path, fa);
	}

	private String getSha256() {
		StringBuffer sb = new StringBuffer();
		try(BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ))){
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] input;
			while(bis.available() != 0) {
				input = bis.readNBytes(5120);
				messageDigest.update(input);
			}
			byte[] digestByte = messageDigest.digest();
			for(byte b : digestByte) {
				sb.append(Integer.toString((b&0xff) + 0x100,16).substring(1));
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			Debug.PRINT_DEBUG("Failed: %s", path);
		}
		return new String(sb);
	}
	
	private String fileTimeToString(FileTime fileTime) {
		return fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss"));
	}	
	private Path relativePath() {
		return startPath.relativize(path);
	}
	private String deepScan() {
		return (scanType == ScanType.FLAT_SCAN) ? null : getSha256();
	}
}

