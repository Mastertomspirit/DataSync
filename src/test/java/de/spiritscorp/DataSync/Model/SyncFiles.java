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
 * Complex synchronization scenario generator for testing file sync operations.
 * 
 * <p>Generates predefined synchronization scenarios with 10 unique test files:
 * <ul>
 *   <li>schnurrend.txt: Identical in source, dest, and sync (no action)</li>
 *   <li>vorsichtig.txt: Modified in dest after initial sync (keep dest)</li>
 *   <li>eiskalt.txt: Deleted from dest, exists in sync (delete from source)</li>
 *   <li>vernünftig.txt: Deleted from source, exists in sync (delete from dest)</li>
 *   <li>elastisch.txt: New in source (copy to dest)</li>
 *   <li>zierlich.txt: New in dest (copy to source)</li>
 *   <li>fleißig.txt: Newer in source than sync (copy to dest)</li>
 *   <li>wissend.txt: Newer in dest than sync (copy to source)</li>
 *   <li>robust.txt: Newer in source (copy to dest)</li>
 *   <li>uralt.txt: Newer in dest (copy to source)</li>
 * </ul>
 * </p>
 * 
 * @author Tom Spirit
 * @version 2.0
 */
class SyncFiles {

	/** List of expected sync result maps */
	private ArrayList<Map<Path, FileAttributes>> expectedFileLists;
	
	/** Root path for source directory */
	private Path sourcePath;
	
	/** Root path for destination directory */
	private Path destPath;
	
	/** JSON reader for test data */
	private JsonReader jr;
	
	/** JSON object containing test file specifications */
	private JsonObject jo;
	
	/**
	 * Constructs a SyncFiles instance with the specified root test path.
	 * 
	 * @param rootPath The root path for test operations, containing 'source' and 'dest' subdirectories
	 */
	SyncFiles(Path rootPath) {
		sourcePath = rootPath.resolve("source");
		destPath = rootPath.resolve("dest");
	}
	
	/**
	 * Creates and populates the three result maps representing expected synchronization outcomes.
	 * 
	 * <p>Implements complex synchronization logic testing scenarios:
	 * <ol>
	 *   <li>Map 0: Files to copy from source to destination</li>
	 *   <li>Map 1: Files to copy from destination to source</li>
	 *   <li>Map 2: Files to delete</li>
	 * </ol>
	 * </p>
	 * 
	 * <p><strong>Key Test Scenarios:</strong>
	 * <ul>
	 *   <li><strong>Latest Version Detection:</strong> Compares modification times to ensure
	 *       the newest version is always copied, never overwritten</li>
	 *   <li><strong>Orphaned Files:</strong> Files that existed in sync but are now missing
	 *       from one location are marked for deletion</li>
	 *   <li><strong>New Files:</strong> Files only in source or dest are copied to the other location</li>
	 *   <li><strong>File Consistency:</strong> Tests that file attributes (size, hash, timestamp) remain consistent</li>
	 * </ul>
	 * </p>
	 * 
	 * @param sourceMap The source file map (will be cleared and repopulated with test data)
	 * @param destMap The destination file map (will be cleared and repopulated with test data)
	 * @param syncMap The synchronization state map (will be cleared and repopulated with test data)
	 * @return ArrayList containing three maps: [copySource, copyDest, delete]
	 */
	ArrayList<Map<Path, FileAttributes>> createSyncFiles(
		Map<Path,FileAttributes> sourceMap, 
		Map<Path, FileAttributes> destMap, 
		Map<Path, FileAttributes> syncMap) {
		
		try (FileReader reader = new FileReader(
			SyncFiles.class.getResource("/syncFiles.json").getFile(), 
			Charset.forName("UTF-8"))) {
			
			jr = Json.createReader(reader);	
			jo = jr.readObject();
			jr.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		expectedFileLists = new ArrayList<>();
		expectedFileLists.add(Model.createMap());  // Files to copy from source to dest
		expectedFileLists.add(Model.createMap());  // Files to copy from dest to source
		expectedFileLists.add(Model.createMap());  // Files to delete
		
		// Clear all input maps to ensure clean state
		sourceMap.clear();
		destMap.clear();
		syncMap.clear();
		
		JsonValue entry;
		FileAttributes file;
		
		// ============================================================
		// Scenario 1: schnurrend.txt - IDENTICAL FILE
		// Should do nothing: file is identical in all three locations
		// ============================================================
		entry = jo.get("schnurrend");
		file = createFileAttributesFromJson(entry);
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);

		// ============================================================
		// Scenario 2: vorsichtig.txt - MODIFIED IN DESTINATION
		// Should do nothing: destination is newer than sync, skip update
		// ============================================================
		entry = jo.get("vorsichtig");
		file = new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641346622384L)),  // NEWER timestamp
			FileTime.fromMillis(1641346622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		
		// ============================================================
		// Scenario 3: eiskalt.txt - DELETED FROM DESTINATION
		// Should delete: file in sync and source but missing from dest
		// ============================================================
		entry = jo.get("eiskalt");
		file = createFileAttributesFromJson(entry);
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(2).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		
		// ============================================================
		// Scenario 4: vernünftig.txt - DELETED FROM SOURCE
		// Should delete: file in sync and dest but missing from source
		// ============================================================
		entry = jo.get("vernünftig");
		file = createFileAttributesFromJson(entry);
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(2).put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

		// ============================================================
		// Scenario 5: elastisch.txt - NEW FILE IN SOURCE
		// Should copy to dest: only in source, not in sync
		// ============================================================
		entry = jo.get("elastisch");
		file = createFileAttributesFromJson(entry);
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(0).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

		// ============================================================
		// Scenario 6: zierlich.txt - NEW FILE IN DESTINATION
		// Should copy to source: only in dest, not in sync
		// ============================================================
		entry = jo.get("zierlich");
		file = createFileAttributesFromJson(entry);
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(1).put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

		// ============================================================
		// Scenario 7: fleißig.txt - NEWER VERSION IN SOURCE
		// Should copy to dest: source timestamp is NEWER than sync
		// ============================================================
		entry = jo.get("fleißig");
		// Source version - NEWER (1641336622384L > 1641335622384L)
		file = new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641336622384L)),  // NEWER
			FileTime.fromMillis(1641336622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(0).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		
		// Destination version - OLDER (1641335622384L < 1641336622384L)
		file = new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641335622384L)),  // OLDER
			FileTime.fromMillis(1641335622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);

		// ============================================================
		// Scenario 8: wissend.txt - NEWER VERSION IN DESTINATION (testDir)
		// Should copy to source: dest timestamp is NEWER than sync
		// ============================================================
		entry = jo.get("wissend");
		// Source version - OLDER (1641335622384L < 1641336622384L)
		file = new FileAttributes(
			Paths.get("testDir", entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641335622384L)),  // OLDER
			FileTime.fromMillis(1641335622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve("testDir").resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get("testDir", entry.asJsonObject().getString("relativeFilePath")), file);
		
		// Destination version - NEWER (1641336622384L > 1641335622384L)
		file = new FileAttributes(
			Paths.get("testDir", entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641336622384L)),  // NEWER
			FileTime.fromMillis(1641336622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve("testDir").resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(1).put(destPath.resolve("testDir").resolve(entry.asJsonObject().getString("relativeFilePath")), file);

		// ============================================================
		// Scenario 9: robust.txt - EXTREME CASE: MUCH NEWER IN SOURCE
		// Should copy to dest: source is significantly newer (1641346622384L)
		// ============================================================
		entry = jo.get("robust");
		// Source version - MUCH NEWER (1641346622384L >> 1641336622384L)
		file = new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641346622384L)),  // MUCH NEWER
			FileTime.fromMillis(1641346622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(0).put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		
		// Destination version - OLDER (1641336622384L < 1641346622384L)
		file = new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641336622384L)),  // OLDER
			FileTime.fromMillis(1641336622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

		// ============================================================
		// Scenario 10: uralt.txt - EXTREME CASE: MUCH NEWER IN DESTINATION
		// Should copy to source: dest is significantly newer (1641346622384L)
		// ============================================================
		entry = jo.get("uralt");
		// Source version - OLDER (1641336622384L < 1641346622384L)
		file = new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641336622384L)),  // OLDER
			FileTime.fromMillis(1641336622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		sourceMap.put(sourcePath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		
		// Destination version - MUCH NEWER (1641346622384L >> 1641336622384L)
		file = new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			fileTimeToString(FileTime.fromMillis(1641346622384L)),  // MUCH NEWER
			FileTime.fromMillis(1641346622384L), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
		destMap.put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);
		syncMap.put(Paths.get(entry.asJsonObject().getString("relativeFilePath")), file);
		expectedFileLists.get(1).put(destPath.resolve(entry.asJsonObject().getString("relativeFilePath")), file);

		return expectedFileLists;
	}
	
	/**
	 * Helper method to create FileAttributes from JSON entry data.
	 * Extracts file metadata from JSON and constructs a FileAttributes object.
	 * 
	 * @param entry The JSON value containing file metadata
	 * @return FileAttributes object with extracted data
	 */
	private FileAttributes createFileAttributesFromJson(JsonValue entry) {
		return new FileAttributes(
			Paths.get(entry.asJsonObject().getString("relativeFilePath")),
			entry.asJsonObject().getString("createTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("createTime").toString())), 
			entry.asJsonObject().getString("modTimeString"), 
			FileTime.fromMillis(Long.parseLong(entry.asJsonObject().get("modTime").toString())), 
			Long.parseLong(entry.asJsonObject().get("size").toString()), 
			entry.asJsonObject().getString("fileHash"));
	}
	
	/**
	 * Converts a FileTime instance to a human-readable string representation.
	 * Uses the system default timezone and "dd.MM.yyyy  HH:mm:ss" pattern.
	 * 
	 * @param fileTime The FileTime to convert
	 * @return Formatted time string (e.g., "04.01.2022  20:33:38")
	 */
	private String fileTimeToString(FileTime fileTime) {
		return fileTime.toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime()
			.format(DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss"));
	}
}
