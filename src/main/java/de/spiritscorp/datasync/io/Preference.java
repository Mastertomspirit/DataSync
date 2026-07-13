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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import de.spiritscorp.datasync.BgTime;
import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.model.FileAttributes;
import de.spiritscorp.datasync.model.Model;

/**
 * Isolated parameters state tracker mapped to a dedicated profile workspace scope.
 *
 * @author Tom Spirit
 */
public final class Preference {

	private String jobName;
	private final IOSyncMap ioSyncMap;
	private final Path jobScanTimePath;

	private ArrayList<Path> sourcePaths;
	private ArrayList<Path> destPaths;
	private final Map<Path, FileAttributes> syncMap;

	private static final String TRASHBIN_STRING = "Papierkorb";
	private Path trashbinPath;

	private ScanType scanMode = ScanType.FLAT_SCAN;
	private BgTime bgTime = BgTime.DAYLY;

	private boolean logOn = true;
	private boolean trashbin = true;
	private boolean subDir;
	private boolean autoDel;
	private boolean autoSync;
	private boolean bgSync;

	private Preference( final String jobName ) {
		this.jobName = jobName;

		syncMap = Model.createMap();
		destPaths = new ArrayList<>( List.of( PreferenceManager.DATASYNC_HOME ) );
		sourcePaths = new ArrayList<>( List.of( PreferenceManager.DATASYNC_HOME ) );
		trashbinPath = destPaths.get( 0 ).resolve( TRASHBIN_STRING );

		final String safeName = jobName.replaceAll( "[^a-zA-Z0-9-_]", "_" );
		this.ioSyncMap = new IOSyncMap( safeName );
		this.jobScanTimePath = PreferenceManager.getInstance().getRootPath().resolve( "lastScanTime_" + safeName );
	}

	/**
	 * Allocation factory generating tracking units separated by specific job profiles keys.
	 *
	 * @param jobName Unique target workspace identifier.
	 *
	 * @return Isolated configuration state scope.
	 */
	static Preference createSinglePreference( final String jobName ) {
		return new Preference( jobName );
	}

	/**
	 * Translates local fields variables entries directly into a JSON entity.
	 */
	JsonObject serialize() {
		final JsonObjectBuilder builder = Json.createObjectBuilder();

		final JsonArrayBuilder srcArr = Json.createArrayBuilder();
		for( final Path path : sourcePaths )
			srcArr.add( path.toString() );

		final JsonArrayBuilder destArr = Json.createArrayBuilder();
		for( final Path path : destPaths )
			destArr.add( path.toString() );

		builder.add( "sourcePaths", srcArr.build() )
				.add( "destPaths", destArr.build() )
				.add( "trashbinPath", trashbinPath.toString() )
				.add( "scanMode", scanMode.getDescription() )
				.add( "bgTime", bgTime.getName() )
				.add( "logOn", logOn )
				.add( "subDir", subDir )
				.add( "autoDel", autoDel )
				.add( "autoSync", autoSync )
				.add( "bgSync", bgSync )
				.add( "trashbin", trashbin );

		return builder.build();
	}

	/**
	 * Hydrates system state metrics back out from serialized profile blocks.
	 * Validates path existence, structural availability, and sanitizes incoming
	 * data types during structural parsing to prevent parsing exceptions.
	 *
	 * @param jsonObject The serialized JSON object payload mapping the profile configuration.
	 * @throws ConfigException If the structural state context is completely unrecoverable or malformed.
	 */
	void deserialize( final JsonObject jsonObject ) throws ConfigException {
		if( jsonObject == null ) { throw new ConfigException( "JSON payload context is null." ); }

		try {
			// --- SOURCE PATHS VALIDATION ---
			this.sourcePaths.clear();
			final JsonArray srcArr = jsonObject.getJsonArray( "sourcePaths" );
			if( srcArr != null ) {
				for( final JsonValue jsonValue : srcArr ) {
					// Strip literal quotes from stringification boundaries
					final Path path = Paths.get( jsonValue.toString().replace( "\"", "" ) );
					// Rigid validation requirement: Source directories MUST physically exist
					if( Files.exists( path ) && Files.isDirectory( path ) ) {
						this.sourcePaths.add( path );
					}else {
						Debug.printDebug( "[Preference] Warning: Source path no longer available on filesystem: " + path );
					}
				}
			}

			// --- DESTINATION PATHS VALIDATION ---
			this.destPaths.clear();
			final JsonArray destArr = jsonObject.getJsonArray( "destPaths" );
			if( destArr != null ) {
				for( final JsonValue jsonValue : destArr ) {
					final Path path = Paths.get( jsonValue.toString().replace( "\"", "" ) );
					// Target paths are allowed to be offline temporarily (e.g., disconnected network mounts)
					this.destPaths.add( path );
					// Fallback
					this.trashbinPath = path;
				}
			}

			if( jsonObject.containsKey( "trashbinPath" ) ) {
				this.trashbinPath = Paths.get( jsonObject.getString( "trashbinPath" ) );
			}

			// --- PATH FALLBACK LOGIC ---
			// Resilient protection layer: If structural filters left arrays empty, fall back to datasync home coordinates
			if( sourcePaths.isEmpty() || destPaths.isEmpty() ) {
				final Path userHome = PreferenceManager.DATASYNC_HOME;
				if( sourcePaths.isEmpty() ) sourcePaths.add( userHome );
				if( destPaths.isEmpty() ) destPaths.add( userHome );
				this.trashbinPath = userHome.resolve( TRASHBIN_STRING );
			}

			// --- PRIMITIVE ENUM CONFIGS (With Type-Checking & Fallbacks) ---
			if( jsonObject.containsKey( "scanMode" ) ) {
				final ScanType parsed = ScanType.get( jsonObject.getString( "scanMode" ) );
				this.scanMode = ( parsed != null ) ? parsed : ScanType.FLAT_SCAN;
			}
			if( jsonObject.containsKey( "bgTime" ) ) {
				final BgTime parsed = BgTime.get( jsonObject.getString( "bgTime" ) );
				this.bgTime = ( parsed != null ) ? parsed : BgTime.DAYLY;
			}

			// --- RESILIENT BOOLEAN INJECTION LAYER ---
			// Intercepts structural ClassCastExceptions if human operators modified primitive types illegally
			this.logOn = getSafeBoolean( jsonObject, "logOn", true );
			this.trashbin = getSafeBoolean( jsonObject, "trashbin", true );
			this.subDir = getSafeBoolean( jsonObject, "subDir", false );
			this.autoDel = getSafeBoolean( jsonObject, "autoDel", false );
			this.autoSync = getSafeBoolean( jsonObject, "autoSync", false );
			this.bgSync = getSafeBoolean( jsonObject, "bgSync", false );

			// Trigger downstream initialization matrix cache tracking parsing
			ioSyncMap.loadSyncMap( syncMap );

		}catch( final Exception exception ) {
			// Wrap any nested unhandled parsing or type violations into a predictable lifecycle runtime exception
			throw new ConfigException( "JSON payload structure is corrupt or mismatched.", exception );
		}
	}

	/**
	 * Extracts a primitive boolean property from a JSON node while guaranteeing type safety bounds.
	 * Prevents application or thread failure sequences if string litterals or garbage attributes
	 * occupy the specified property target location.
	 *
	 * @param json         The serialized source JSON payload structural map.
	 * @param key          The expected parameter identifier token key.
	 * @param defaultValue The architectural primitive value fallback state if the verification sequence fails.
	 * @return The evaluated value assigned to the target property key, or the predefined default.
	 */
	private boolean getSafeBoolean( final JsonObject json, final String key, final boolean defaultValue ) {
		if( !json.containsKey( key ) ) { return defaultValue; }
		try {
			return json.getBoolean( key );
		}catch( ClassCastException _ ) {
			Debug.printDebug( "[Preference] Warning: Invalid data type mapping tracking key '%s'. Falling back to default: %s", key, defaultValue );
			return defaultValue;
		}
	}

	/**
	 * Persists the current timestamp as the last successful scan execution time.
	 */
	public void saveLastScanTime() {
		try( BufferedWriter writer = Files.newBufferedWriter( jobScanTimePath,
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING ) ) {
			writer.write( String.valueOf( System.currentTimeMillis() ) );
		}catch( final IOException exception ) {
			Debug.printDebug( "[Preference Error] IO write failed at '%s' message: %s", jobScanTimePath, exception.getMessage() );
			Debug.printException( this.getClass(), exception );
		}
	}

	/**
	 * Retrieves the last recorded scan execution timestamp.
	 *
	 * @return Unix epoch millis, or 0 if no timer token exists yet.
	 */
	public long getLastScanTime() {
		if( !Files.exists( jobScanTimePath ) ) { return 0; }
		try( BufferedReader reader = Files.newBufferedReader( jobScanTimePath ) ) {
			final String line = reader.readLine();
			return ( line != null ) ? Long.parseLong( line.trim() ) : 0;
		}catch( final IOException | NumberFormatException exception ) {
			Debug.printDebug( "[Preference Error] IO read failed at '%s' message: %s", jobScanTimePath, exception.getMessage() );
			Debug.printException( this.getClass(), exception );
			return 0;
		}
	}

	public void writeSyncMap() {
		ioSyncMap.writeSyncMap( syncMap );
	}

	// --- Setters and Getters ---
	public String getJobName() { return jobName; }

	void setJobNameFromManager( final String newName ) { this.jobName = newName; }

	public ArrayList<Path> getSourcePaths() { return sourcePaths; }

	public void setSourcePaths( final ArrayList<Path> sourcePaths ) {
		this.sourcePaths = sourcePaths;
		ioSyncMap.deleteSyncMap();
	}

	public void setSourcePath( final Path sourcePath ) {
		this.sourcePaths.add( sourcePath );
	}

	public void removeSourcePath( final Path sourcePath ) {
		this.sourcePaths.remove( sourcePath );
	}

	public ArrayList<Path> getDestPaths() { return destPaths; }

	public void setDestPaths( final ArrayList<Path> destPaths ) {
		this.destPaths = destPaths;
		this.trashbinPath = destPaths.get( 0 ).resolve( TRASHBIN_STRING );
		ioSyncMap.deleteSyncMap();
	}

	public Map<Path, FileAttributes> getSyncMap() { return syncMap; }

	public ScanType getScanMode() { return scanMode; }

	public void setScanMode( final ScanType scanMode ) { this.scanMode = scanMode; }

	public BgTime getBgTime() { return bgTime; }

	public void setBgTime( final BgTime time ) { this.bgTime = time; }

	public boolean isLogOn() { return logOn; }

	public void setLogOn( final boolean logOn ) { this.logOn = logOn; }

	public boolean isSubDir() { return subDir; }

	public void setSubDir( final boolean subDir ) { this.subDir = subDir; }

	public boolean isAutoDel() { return autoDel; }

	public void setAutoDel( final boolean autoDel ) { this.autoDel = autoDel; }

	public boolean isAutoSync() { return autoSync; }

	public void setAutoSync( final boolean autoSync ) { this.autoSync = autoSync; }

	public boolean isBgSync() { return bgSync; }

	public void setBgSync( final boolean bgSync ) { this.bgSync = bgSync; }

	public boolean isTrashbin() { return trashbin; }

	public void setTrashbin( final boolean trashbin ) { this.trashbin = trashbin; }

	public Path getTrashbinPath() { return trashbinPath; }
}