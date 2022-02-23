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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import de.spiritscorp.DataSync.Model.FileAttributes;

public class Logger {
	
	private JSONArray ja = new JSONArray();
	private Path logPath = Paths.get(System.getProperty("user.home"), "DataSync", "log.json");
	
	/**
	 * Set a new log entry
	 * 
	 * @param filePath The path where the file is/was located
	 * @param changeStatus	The status, what is happen
	 * @param fa The attributes of the file
	 */
	public void setEntry(String filePath, String changeStatus, FileAttributes fa) {
		JSONArray ja2 = new JSONArray();
		ja2.put(filePath);
		ja2.put(LocalDateTime.now());
		ja2.put(changeStatus);
		JSONObject jo = new JSONObject(fa);
		ja2.put(jo);
		ja.put(ja2);
	}
	
//	TODO	ineffizient
	/**
	 * Write the cached entries to the file system, append on the top of the file
	 */
	public void printStatus() {
		readLog();
		try {
			Files.writeString(logPath, ja.toString(2), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			ja.clear();
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read the logfile as JsonArray
	 */
	public void readLog() {
		if(Files.exists(logPath)) {
			try(FileReader fr = new FileReader(logPath.toFile(), Charset.forName("UTF-8"))) {
				JSONTokener jt = new JSONTokener(fr);
				JSONArray jaTemp = (JSONArray) jt.nextValue();
				jaTemp.forEach(p -> ja.put(p));				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
