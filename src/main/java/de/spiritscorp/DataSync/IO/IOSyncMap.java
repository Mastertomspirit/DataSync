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

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.spiritscorp.DataSync.Model.FileAttributes;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

class IOSyncMap {
	
/**
 * read the json syncMap
 * 
 * @param syncMap
 * @return <b>boolean</b> 	</br>true if the map is loaded
 */
	boolean loadSyncMap(Map<Path, FileAttributes> syncMap) {
		if(syncMap.isEmpty()) {
			if(Files.exists(Preference.SYNCMAP_PATH)) {
				try(FileReader reader = new FileReader(Preference.SYNCMAP_PATH.toFile(), Charset.forName("UTF-8"))){		
					JsonReader jr = Json.createReader(reader);	
					JsonObject jo = jr.readObject();
					jr.close();
					for(Entry<String, JsonValue> entry : jo.entrySet()) {
						FileAttributes file = new FileAttributes(Paths.get(entry.getValue().asJsonObject().getString("relativeFilePath")),
																 entry.getValue().asJsonObject().getString("createTimeString"), 
																 FileTime.fromMillis(Long.parseLong(entry.getValue().asJsonObject().get("createTime").toString())), 
																 entry.getValue().asJsonObject().getString("modTimeString"), 
																 FileTime.fromMillis(Long.parseLong(entry.getValue().asJsonObject().get("modTime").toString())), 
																 Long.parseLong(entry.getValue().asJsonObject().get("size").toString()), 
																 entry.getValue().asJsonObject().getString("fileHash"));
						syncMap.put(Paths.get(entry.getValue().asJsonObject().getString("relativeFilePath")), file);
					}
				}catch (IOException e) {
					Debug.PRINT_DEBUG("%nFehler beim laden der SyncMap: %s", e.getMessage());
					e.printStackTrace();
					return false;
				}
			}else {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * write the sync map as json
	 * 
	 * @param syncMap
	 */
	void writeSyncMap(Map<Path, FileAttributes> syncMap) {
		try(OutputStream os = Files.newOutputStream(Preference.SYNCMAP_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {	
			HashMap<String,Boolean> config = new HashMap<>();
			JsonObject job;
			config.put(JsonGenerator.PRETTY_PRINTING, true);
			JsonWriterFactory jwf = Json.createWriterFactory(config);
			job = Json.createObjectBuilder(createSyncMap(syncMap)).build();
			JsonWriter jw = jwf.createWriter(os);
			jw.write(job);
			jw.close();
		}catch(IOException e) {
			e.printStackTrace();
			Debug.PRINT_DEBUG("%nFehler beim schreiben der SyncMap: %s", e.getMessage());
		}	
	}

	/**
	 * Build a jsonObject from the sync map
	 * 
	 * @param syncMap
	 * @return <b>JsonObject</b> 	</br>
	 */
	private JsonObject createSyncMap(Map<Path, FileAttributes> syncMap) {
		JsonObjectBuilder jo = Json.createObjectBuilder();
		for(Map.Entry<Path, FileAttributes> entry : syncMap.entrySet()) {
			JsonObject jo2 = Json.createObjectBuilder().add("relativeFilePath", entry.getValue().getRelativeFilePath().toString())
													   .add("fileHash", entry.getValue().getFileHash())
													   .add("modTimeString", entry.getValue().getModTimeString())
													   .add("createTime", entry.getValue().getCreateTime().toMillis())
													   .add("modTime", entry.getValue().getModTime().toMillis())
													   .add("size", entry.getValue().getSize())
													   .add("createTimeString", entry.getValue().getCreateTimeString())
													   .build();
			jo.add(entry.getValue().getRelativeFilePath().toString(), jo2);
		}
		return jo.build();
	}	
}
