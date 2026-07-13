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

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import de.spiritscorp.datasync.model.FileAttributes;

/**
 * Isolated binary mapping engine handling structural cache entries matching runtime properties.
 * * @author Tom Spirit
 */
class IOSyncMap {

	/** Individual path to the sync map */
	private final Path jobSyncMapPath;

	/**
	 * Binds tracking matrices to unique physical layout footprints.
	 */
	IOSyncMap( final String jobName ) {
		this.jobSyncMapPath = PreferenceManager.getInstance().getConfigPath().getParent().resolve( "syncMap_" + jobName + ".json" );
	}

	/**
	 * Deserializes the structural file-tracking registry from disk and hydrates the provided synchronization mapping.
	 * Enforces strict pre-conditions ensuring the destination container is empty and the source checkpoint file
	 * exists prior to execution. Intercepts and logs file-system access errors, JSON syntax malformations,
	 * and internal parser state conflicts gracefully.
	 *
	 * @param syncMap The target destination map to be populated with path keys and their associated tracking metadata.
	 * @return true if the persistence store was parsed successfully and the map state was fully hydrated;<br>
	 *         false if pre-conditions failed or an internal parsing/IO exception occurred.
	 */
	boolean loadSyncMap( final Map<Path, FileAttributes> syncMap ) {
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
			Debug.printDebug( "[IO Sync Map Error] File system or configuration failure while opening stream: %s", e.getMessage() );
			Debug.printException( this.getClass(), e );
			return false;

		}catch( final JsonException exception ) {
			// Block 2: JSON Processing (Deals with both syntax/parsing and structural creation errors)
			Debug.printDebug( "[IO Sync Map Error] JSON processing or syntax error: %s", exception.getMessage() );
			Debug.printException( this.getClass(), exception );
			return false;

		}catch( final IllegalStateException exception ) {
			// Block 3: Reader State Management (Reader already closed or multi-call violation)
			Debug.printDebug( "[IO Sync Map Error] Parser state conflict: %s", exception.getMessage() );
			Debug.printException( this.getClass(), exception );
			return false;
		}
	}

	/**
	 * Serializes the active in-memory file synchronization snapshot and flushes the state to disk.
	 * Compiles the tracking dataset into a formatted JSON structure utilizing pretty-printing layout rules,
	 * completely truncating any pre-existing target file to establish a clean persistence baseline.
	 *
	 * @param syncMap The source state map containing the path-to-attribute records to be persisted.
	 */
	void writeSyncMap( final Map<Path, FileAttributes> syncMap ) {
		try( OutputStream outputStream = Files.newOutputStream( jobSyncMapPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING ) ) {
			final HashMap<String, Boolean> config = new HashMap<>();
			config.put( JsonGenerator.PRETTY_PRINTING, true );
			final JsonWriterFactory jWriterFactory = Json.createWriterFactory( config );

			final JsonObject jsonObject = createJsonObject( syncMap );
			final JsonWriter jsonWriter = jWriterFactory.createWriter( outputStream );
			jsonWriter.write( jsonObject );
			jsonWriter.close();
		}catch( final IOException exception ) {
			Debug.printDebug( "[IO Sync Map Error] File system or configuration failure while writing file: %s", exception.getMessage() );
			Debug.printException( this.getClass(), exception );
		}
	}

	/**
	 * Deletes the persisted file synchronization snapshot from disk.
	 * Removes the tracking dataset file if it exists, effectively clearing the persistence
	 * baseline and forcing a full scan or re-initialization upon the next execution.
	 */
	void deleteSyncMap() {
		try {
			final boolean deleted = Files.deleteIfExists( jobSyncMapPath );
			if( deleted ) {
				Debug.printDebug( "[IO Sync Map] Successfully deleted sync map file: %s", jobSyncMapPath );
			}
		}catch( final IOException exception ) {
			Debug.printDebug( "[IO Sync Map Error] File system failure while deleting file: %s", exception.getMessage() );
			Debug.printException( this.getClass(), exception );
		}
	}

	/**
	 * Translates the raw in-memory domain mapping of file metadata into a structured JSON object tree.
	 * Iterates through the tracking entries, maps file primitives and specialized timestamps into a
	 * unified object blueprint, and indexes each sub-block by its relative file identifier path string.
	 *
	 * @param syncMap The raw file attribute domain dataset to convert.
	 * @return An immutable structural JSON representation ready for serialization to the persistence layer.
	 */
	private JsonObject createJsonObject( final Map<Path, FileAttributes> syncMap ) {
		final JsonObjectBuilder joBuilder = Json.createObjectBuilder();
		for( final Map.Entry<Path, FileAttributes> entry : syncMap.entrySet() ) {
			final JsonObject jsonObject = Json.createObjectBuilder()
					.add( "relativeFilePath", entry.getValue().getRelativeFilePath().toString() )
					.add( "fileHash", entry.getValue().getFileHash() )
					.add( "modTimeString", entry.getValue().getModTimeString() )
					.add( "createTime", entry.getValue().getCreateTime().toMillis() )
					.add( "modTime", entry.getValue().getModTime().toMillis() )
					.add( "size", entry.getValue().getSize() )
					.add( "createTimeString", entry.getValue().getCreateTimeString() )
					.build();
			joBuilder.add( entry.getValue().getRelativeFilePath().toString(), jsonObject );
		}
		return joBuilder.build();
	}
}
