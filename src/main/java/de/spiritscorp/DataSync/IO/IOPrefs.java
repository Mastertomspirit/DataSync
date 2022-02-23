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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.ScanType;

class IOPrefs {

	private Path configDir, configPath;
	private JSONObject job;
	
	/**
	 * 
	 * @param configDir The directory where the config file should be saved
	 */
	IOPrefs(Path configDir){
		this.configDir = configDir;
		configPath = configDir.resolve("conf.json");
	}
	
/**
 * @return	<b>boolean</b> true if success
 */
	boolean readPreferences() {	
		if(Files.exists(configPath)) {
			try(FileReader reader = new FileReader(configPath.toFile(), Charset.forName("UTF-8"))){		
				JSONTokener jt = new JSONTokener(reader);
				job = new JSONObject(jt);	
				if(job != null) return true;
			}catch(JSONException | IOException e) {e.printStackTrace();}
		}
		job = new JSONObject();
		return false;
	}

/**
 * 	Write the Preferences at <i>"user-home"/DataSync/conf.json</i>
 */
	void writePreferences() {
		if(!Files.exists(configDir)) {
			configDir.toFile().mkdir();
		}
		try {			
			Files.writeString(configPath, job.toString(6), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}catch(IOException e) {e.printStackTrace();}	
	}
	
	/**
	 * Set the preferences as key value pairs
	 * 
	 * @param key
	 * @param value
	 */
	void setPreferences(String key, Object value) {
		job.put(key, value);
	}
	
	/**
	 * 
	 * @param value The value to be search
	 * @return <b>Path</> </br>The requested path
	 * @throws ConfigException If path is not valid
	 */
	Path getPath(String value) throws ConfigException{
		if(job.has(value)) {
			Path convPath = Paths.get(job.getString(value));
			if(Files.exists(convPath) || value.startsWith("destPath") || value.equals("trashbinPath"))	return convPath; 
		}
		throw new ConfigException("No valid Path found: " + value);
	}
	
	/**
	 * 
	 * @param value The value to be search
	 * @return <b>int</b> </br>The number of paths are set
	 * @throws ConfigException 
	 */
	int getMultiPath(String value) throws ConfigException {
		if(job.has(value)) {
			try {
				return job.getInt(value);
			}catch(JSONException e) {e.printStackTrace();}
		}
		throw new ConfigException("No valid Multipath number found");
	}

	/**
	 * 
	 * @param value The value to be search
	 * @return <b>boolean</b>
	 * @throws ConfigException
	 */
	boolean getBoolean(String value) throws ConfigException {
		if(job.has(value)) {
			try{
				return job.getBoolean(value);
			}catch(JSONException e) {e.printStackTrace();}
		}
		throw new ConfigException("No valid boolean found: " + value);
	}

	/**
	 * 
	 * @return <b>ScanType</b>
	 * @throws ConfigException
	 */
	ScanType getScanType() throws ConfigException {
		try {
			return job.getEnum(ScanType.class, "deepScan");
		}catch(JSONException e) {e.printStackTrace();}
		throw new ConfigException("No valid ScanType found");
	}

	/**
	 * 
	 * @return <b>BgTime</b>
	 * @throws ConfigException
	 */
	BgTime getBgTime() throws ConfigException {
		try {
			return job.getEnum(BgTime.class, "bgTime");
		}catch(Exception e) {e.printStackTrace();}
		throw new ConfigException("No valid BgTime found");
	}
}
