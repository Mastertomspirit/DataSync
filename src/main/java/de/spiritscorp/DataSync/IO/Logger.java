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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

	private final LinkedList<JsonValue> logList = new LinkedList<>();

	/**
	 * Set a new log entry
	 *
	 * @param filePath     The path where the file is/was located
	 * @param changeStatus The status, what is happen
	 * @param fa           The attributes of the file
	 */
	public void setEntry( String filePath, String changeStatus, FileAttributes fa ) {
		final JsonObject jo = Json.createObjectBuilder()
				.add( "Dateiname", fa.getFileName() )
				.add( "erstellt", fa.getCreateTimeString() )
				.add( "zuletzt modifiziert", fa.getModTimeString() )
				.add( "Größe", fa.getSize() )
				.add( "Fingerabdruck", ( fa.getFileHash() == null ) ? "null" : fa.getFileHash() )
				.build();
		final JsonArray ja = Json.createArrayBuilder()
				.add( filePath )
				.add( LocalDateTime.now().format( DateTimeFormatter.ofPattern( "dd.MM.yyyy  HH:mm:ss" ) ) )
				.add( changeStatus )
				.add( jo )
				.build();
		logList.addFirst( ja );
	}

//	TODO	ineffizient
	/**
	 * Write the cached entries to the file system, append on the top of the file
	 */
	public void printStatus() {
		readLog();
		final JsonArray jsonArray = Json.createArrayBuilder( logList ).build();
		final HashMap<String, Boolean> config = new HashMap<>();
		config.put( JsonGenerator.PRETTY_PRINTING, true );
		final JsonWriterFactory jwf = Json.createWriterFactory( config );
		try( JsonWriter jsonWriter = jwf.createWriter( Files.newOutputStream( PreferenceManager.getInstance().getLogPath(), StandardOpenOption.WRITE ) ) ) {
			jsonWriter.write( jsonArray );
			logList.clear();
		}catch( final IOException e ) {
			Debug.printException( this.getClass(), e );
		}
	}

	/**
	 * Read the logfile as JsonArray
	 */
	private void readLog() {
		final Path logPath = PreferenceManager.getInstance().getLogPath();
		if( Files.exists( logPath ) ) {
			try( JsonReader reader = Json.createReader( Files.newInputStream( logPath ) ) ) {
				reader.readArray()
						.forEach( logList::add );
			}catch( final IOException e ) {
				Debug.printException( this.getClass(), e );
			}
		}
	}
}
