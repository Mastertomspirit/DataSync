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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Unique Files:
 * 		schnurrend.txt, vorsicht.txt, eiskalt.txt, vernünftig.txt, elastisch.txt, zierlich.txt, fleißig.txt, wissend.txt, robust.txt, uralt.txt
 * 
 */
class SyncFiles {

	private ArrayList<Map<Path, FileAttributes>> expectedFileLists = new ArrayList<>(); 
	private Path sourcePath;
	private Path destPath;
	private JsonReader jr;
	private JsonObject jo;
	
	SyncFiles(Path rootPath) {
		sourcePath = rootPath.resolve("source");
		destPath = rootPath.resolve("dest");
	}
	
	ArrayList<Map<Path, FileAttributes>> createSyncFiles(Map<Path,FileAttributes> sourceMap, Map<Path, FileAttributes> destMap, Map<Path, FileAttributes> syncMap) {
		try(FileReader reader = new FileReader(SyncFiles.class.getResource("/syncFiles.json").getFile(), Charset.forName("UTF-8"))){		
			jr = Json.createReader(reader);	
			jo = jr.readObject();
			jr.close();
		}catch (IOException e) {e.printStackTrace();}
		expectedFileLists.add(Model.createMap());
		expectedFileLists.add(Model.createMap());
		expectedFileLists.add(Model.createMap());
		
		sourceMap.clear();
		destMap.clear();
		syncMap.clear();
		JsonValue entry;
		FileAttributes file;
//	Should do nothing
		entry = jo.get("schnurrend");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 entry.asJsonObject().getString("modTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("modTime").toString())), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);

//		Should do nothing
		entry = jo.get("vorsichtig");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641346622384L)), 
											 FileTime.fromMillis(1641346622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		
//	Should delete this
		entry = jo.get("eiskalt");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 entry.asJsonObject().getString("modTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("modTime").toString())), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(2).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		
		
//	Should delete this
		entry = jo.get("vernünftig");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 entry.asJsonObject().getString("modTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("modTime").toString())), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(2).put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

//	Should copy this to dest
		entry = jo.get("elastisch");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 entry.asJsonObject().getString("modTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("modTime").toString())), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(0).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

//	Should copy this
		entry = jo.get("zierlich");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 entry.asJsonObject().getString("modTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("modTime").toString())), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(1).put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

//	Should copy to dest
		entry = jo.get("fleißig");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641336622384L)), 
											 FileTime.fromMillis(1641336622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(0).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641335622384L)), 
											 FileTime.fromMillis(1641335622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);

//	Should copy to source --> testDir
		entry = jo.get("wissend");
		file = new FileAttributes(Paths.get("testDir", entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641335622384L)), 
											 FileTime.fromMillis(1641335622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve("testDir").resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get("testDir", entry.asJsonObject().getString("relativeFilePath")), file);
		file = new FileAttributes(Paths.get("testDir", entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641336622384L)), 
											 FileTime.fromMillis(1641336622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve("testDir").resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(1).put(destPath.resolve("testDir").resolve(entry.asJsonObject().getString("relativeFilePath")), file);

//	Should copy to dest
		entry = jo.get("robust");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641346622384L)), 
											 FileTime.fromMillis(1641346622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(0).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641336622384L)), 
											 FileTime.fromMillis(1641336622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

//	Should copy to source
		entry = jo.get("uralt");
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641336622384L)), 
											 FileTime.fromMillis(1641336622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		file = new FileAttributes(Paths.get(entry.asJsonObject().getString("relativeFilePath")),
											 entry.asJsonObject().getString("createTimeString"), 
											 FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
											 fileTimeToString(FileTime.fromMillis(1641346622384L)), 
											 FileTime.fromMillis(1641346622384L), 
											 Long.parseLong(entry.asJsonObject().get("size").toString()), 
											 entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(1).put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

		return expectedFileLists;

	}
	
	private String fileTimeToString(FileTime fileTime) {
		return fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss"));
	}
}
