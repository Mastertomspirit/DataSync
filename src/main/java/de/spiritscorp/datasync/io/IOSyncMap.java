package de.spiritscorp.datasync.io;

/*-
 * 		Data Sync
 *
 * 		Copyright ©   2022    The Spirit
 * 		@email                        thespirit@spiritscorp.network
 *
 * 		This program is free software; you can redistribute it and/or modify
 * 		it under the terms of the GNU General Public License as published by
 * 		the Free Software Foundation; either version 3 of the License, or
 * 		(at your option) any later version.
 *
 * 		This program is distributed in the hope that it will be useful,
 * 		but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 		See the GNU General Public License for more details.
 *
 * 		You should have received a copy of the GNU General Public License
 * 		along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import de.spiritscorp.datasync.model.FileAttributes;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

/**
 * Isolated binary mapping engine handling structural cache entries matching runtime properties.
 * * @author Tom Spirit
 */
class IOSyncMap {

	private final Path jobSyncMapPath;

	/**
	 * Binds tracking matrices to unique physical layout footprints.
	 */
	IOSyncMap( String jobName ) {
		this.jobSyncMapPath = PreferenceManager.getInstance().getConfigPath().getParent().resolve( "syncMap_" + jobName + ".json" );
	}

	boolean loadSyncMap( Map<Path, FileAttributes> syncMap ) {
		if( !syncMap.isEmpty() || !Files.exists( jobSyncMapPath ) ) { return false; }
		try( JsonReader jsonReader = Json.createReader( Files.newInputStream( jobSyncMapPath, StandardOpenOption.READ ) ) ) {
			final JsonObject jsonObject = jsonReader.readObject();

			for( final Map.Entry<String, JsonValue> entry : jsonObject.entrySet() ) {
				final JsonObject objectEntry = entry.getValue().asJsonObject();
				final FileAttributes fileAttributes = new FileAttributes(
						Paths.get( objectEntry.getString( "relativeFilePath" ) ),
						objectEntry.getString( "createTimeString" ),
						FileTime.fromMillis( Long.parseLong( objectEntry.get( "createTime" ).toString() ) ),
						objectEntry.getString( "modTimeString" ),
						FileTime.fromMillis( Long.parseLong( objectEntry.get( "modTime" ).toString() ) ),
						Long.parseLong( objectEntry.get( "size" ).toString() ),
						objectEntry.getString( "fileHash" ) );
				syncMap.put( Paths.get( objectEntry.getString( "relativeFilePath" ) ), fileAttributes );
			}
			return true;
		}catch( final IllegalArgumentException | UnsupportedOperationException | SecurityException | IOException e ) {
			// Block 1: IO & File System Operations (Options, Permissions, Missing Files)
			Debug.printDebug( "[Error] File system or configuration failure while opening stream: %s", e.getMessage() );
			Debug.printException( this.getClass(), e );
			return false;

		}catch( final jakarta.json.JsonException e ) {
			// Block 2: JSON Processing (Deals with both syntax/parsing and structural creation errors)
			Debug.printDebug( "[Error] JSON processing or syntax error: %s", e.getMessage() );
			Debug.printException( this.getClass(), e );
			return false;

		}catch( final IllegalStateException e ) {
			// Block 3: Reader State Management (Reader already closed or multi-call violation)
			Debug.printDebug( "[Error] Parser state conflict: %s", e.getMessage() );
			Debug.printException( this.getClass(), e );
			return false;
		}
	}

	void writeSyncMap( Map<Path, FileAttributes> syncMap ) {
		try( OutputStream os = Files.newOutputStream( jobSyncMapPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING ) ) {
			final HashMap<String, Boolean> config = new HashMap<>();
			config.put( JsonGenerator.PRETTY_PRINTING, true );
			final JsonWriterFactory jwf = Json.createWriterFactory( config );

			final JsonObject jobObj = createSyncMap( syncMap );
			final JsonWriter jw = jwf.createWriter( os );
			jw.write( jobObj );
			jw.close();
		}catch( final IOException e ) {
			Debug.printException( this.getClass(), e );
		}
	}

	private JsonObject createSyncMap( Map<Path, FileAttributes> syncMap ) {
		final JsonObjectBuilder jo = Json.createObjectBuilder();
		for( final Map.Entry<Path, FileAttributes> entry : syncMap.entrySet() ) {
			final JsonObject jo2 = Json.createObjectBuilder()
					.add( "relativeFilePath", entry.getValue().getRelativeFilePath().toString() )
					.add( "fileHash", entry.getValue().getFileHash() )
					.add( "modTimeString", entry.getValue().getModTimeString() )
					.add( "createTime", entry.getValue().getCreateTime().toMillis() )
					.add( "modTime", entry.getValue().getModTime().toMillis() )
					.add( "size", entry.getValue().getSize() )
					.add( "createTimeString", entry.getValue().getCreateTimeString() )
					.build();
			jo.add( entry.getValue().getRelativeFilePath().toString(), jo2 );
		}
		return jo.build();
	}
}
