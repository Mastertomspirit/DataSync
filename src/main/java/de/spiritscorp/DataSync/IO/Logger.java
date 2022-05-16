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

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;

import de.spiritscorp.DataSync.Model.FileAttributes;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

public class Logger {
	
	private LinkedList<JsonValue> logList = new LinkedList<>();
	private Path logPath = Paths.get(System.getProperty("user.home"), "DataSync", "log.json");
	
	/**
	 * Set a new log entry
	 * 
	 * @param filePath The path where the file is/was located
	 * @param changeStatus	The status, what is happen
	 * @param fa The attributes of the file
	 */
	public void setEntry(String filePath, String changeStatus, FileAttributes fa) {
		JsonObject jo = Json.createObjectBuilder()
				.add("Dateiname", fa.getFileName())
				.add("erstellt", fa.getCreateTime())
				.add("zuletzt modifiziert", fa.getModTime())
				.add("Größe", fa.getSize())
				.add("Fingerabdruck" , (fa.getFileHash() == null) ? "null" : fa.getFileHash())
				.build();	
		JsonArray ja = Json.createArrayBuilder()
				.add(filePath)
				.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss")))
				.add(changeStatus)
				.add(jo)
				.build();
		logList.addFirst(ja);
	}
	
//	TODO	ineffizient
	/**
	 * Write the cached entries to the file system, append on the top of the file
	 */
	public void printStatus() {
		readLog();
		JsonArray ja = Json.createArrayBuilder(logList).build();
		try(FileOutputStream fos = new FileOutputStream(logPath.toFile(), false)) {
			HashMap<String,Boolean> config = new HashMap<>();
			config.put(JsonGenerator.PRETTY_PRINTING, true);
			JsonWriterFactory jwf = Json.createWriterFactory(config);
			JsonWriter jw = jwf.createWriter(fos);
			jw.write(ja);
			jw.close();
			logList.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read the logfile as JsonArray
	 */
	private void readLog() {
		if(Files.exists(logPath)) {
			try(FileReader reader = new FileReader(logPath.toFile(), Charset.forName("UTF-8"))) {
				JsonReader jr = Json.createReader(reader);
				jr.readArray()
					.stream()
					.forEach((v) -> logList.add(v));
				jr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
