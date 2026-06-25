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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.Model.FileAttributes;
import de.spiritscorp.DataSync.Model.Model;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Isolated parameters state tracker mapped to a dedicated profile workspace scope.
 *
 * @author Tom Spirit
 */
public class Preference {

	private String jobName;
	private final IOSyncMap ioSyncMap;
	private final Path jobScanTimePath;

	private ArrayList<Path> sourcePaths = new ArrayList<>();
	private ArrayList<Path> destPaths = new ArrayList<>();
	private final Map<Path, FileAttributes> syncMap = Model.createMap();

	private final String trashbinString = "Papierkorb";
	private Path startSourcePath = Paths.get( System.getProperty( "user.home" ) );
	private Path startDestPath = Paths.get( System.getProperty( "user.home" ) );
	private Path trashbinPath = startDestPath.resolve( trashbinString );

	private ScanType scanMode = ScanType.FLAT_SCAN;
	private BgTime bgTime = BgTime.DAYLY;

	private boolean logOn = true;
	private boolean trashbin = true;
	private boolean subDir, autoDel, autoSync, bgSync, autoBgDel;

	private Preference( String jobName ) {
		this.jobName = jobName;
		sourcePaths.add( startSourcePath );
		destPaths.add( startDestPath );
		final String safeName = jobName.replaceAll( "[^a-zA-Z0-9-_]", "_" );
		this.ioSyncMap = new IOSyncMap( safeName );
		this.jobScanTimePath = PreferenceManager.getInstance().getConfigPath().getParent().resolve( "lastScanTime_" + safeName );
	}

	/**
	 * Allocation factory generating tracking units separated by specific job profiles keys.
	 *
	 * @param jobName Unique target workspace identifier.
	 *
	 * @return Isolated configuration state scope.
	 */
	static Preference createSinglePreference( String jobName ) {
		return new Preference( jobName );
	}

	/**
	 * Translates local fields variables entries directly into a JSON entity.
	 */
	JsonObject serialize() {
		final JsonObjectBuilder builder = Json.createObjectBuilder();

		final JsonArrayBuilder srcArr = Json.createArrayBuilder();
		for( final Path p : sourcePaths )
			srcArr.add( p.toString() );

		final JsonArrayBuilder destArr = Json.createArrayBuilder();
		for( final Path p : destPaths )
			destArr.add( p.toString() );

		builder.add( "sourcePaths", srcArr.build() )
				.add( "destPaths", destArr.build() )
				.add( "startSourcePath", startSourcePath.toString() )
				.add( "startDestPath", startDestPath.toString() )
				.add( "trashbinPath", trashbinPath.toString() )
				.add( "scanMode", scanMode.getDescription() )
				.add( "bgTime", bgTime.getName() )
				.add( "logOn", logOn )
				.add( "subDir", subDir )
				.add( "autoDel", autoDel )
				.add( "autoBgDel", autoBgDel )
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
	 * @param json The serialized JSON object payload mapping the profile configuration.
	 * @throws ConfigException If the structural state context is completely unrecoverable or malformed.
	 */
	void deserialize( JsonObject json ) throws ConfigException {
		if( json == null ) { throw new ConfigException( "JSON payload context is null." ); }

		try {
			// --- SOURCE PATHS VALIDATION ---
			sourcePaths.clear();
			final JsonArray srcArr = json.getJsonArray( "sourcePaths" );
			if( srcArr != null ) {
				for( final JsonValue v : srcArr ) {
					// Strip literal quotes from stringification boundaries
					final Path p = Paths.get( v.toString().replace( "\"", "" ) );
					// Rigid validation requirement: Source directories MUST physically exist
					if( Files.exists( p ) && Files.isDirectory( p ) ) {
						sourcePaths.add( p );
					}else {
						Debug.printDebug( "[Preference] Warning: Source path no longer available on filesystem: " + p );
					}
				}
			}

			// --- DESTINATION PATHS VALIDATION ---
			destPaths.clear();
			final JsonArray destArr = json.getJsonArray( "destPaths" );
			if( destArr != null ) {
				for( final JsonValue v : destArr ) {
					final Path p = Paths.get( v.toString().replace( "\"", "" ) );
					// Target paths are allowed to be offline temporarily (e.g., disconnected network mounts)
					destPaths.add( p );
				}
			}

			// --- STARTING & SYSTEM PATHS VALIDATION ---
			if( json.containsKey( "startSourcePath" ) ) {
				final Path p = Paths.get( json.getString( "startSourcePath" ) );
				this.startSourcePath = ( Files.exists( p ) ) ? p : Paths.get( System.getProperty( "user.home" ) );
			}

			if( json.containsKey( "startDestPath" ) ) {
				final Path p = Paths.get( json.getString( "startDestPath" ) );
				setStartDestPath( p ); // Internally forces tracking updates for trashbinPath coordinates
			}

			if( json.containsKey( "trashbinPath" ) ) {
				this.trashbinPath = Paths.get( json.getString( "trashbinPath" ) );
			}

			// --- PATH FALLBACK LOGIC ---
			// Resilient protection layer: If structural filters left arrays empty, fall back to user home coordinates
			if( sourcePaths.isEmpty() || destPaths.isEmpty() ) {
				final Path userHome = Paths.get( System.getProperty( "user.home" ) );
				if( sourcePaths.isEmpty() ) sourcePaths.add( userHome );
				if( destPaths.isEmpty() ) destPaths.add( userHome );
				this.startSourcePath = userHome;
				this.startDestPath = userHome;
				this.trashbinPath = userHome.resolve( trashbinString );
			}

			// --- PRIMITIVE ENUM CONFIGS (With Type-Checking & Fallbacks) ---
			if( json.containsKey( "scanMode" ) ) {
				final ScanType parsed = ScanType.get( json.getString( "scanMode" ) );
				this.scanMode = ( parsed != null ) ? parsed : ScanType.FLAT_SCAN;
			}
			if( json.containsKey( "bgTime" ) ) {
				final BgTime parsed = BgTime.get( json.getString( "bgTime" ) );
				this.bgTime = ( parsed != null ) ? parsed : BgTime.DAYLY;
			}

			// --- RESILIENT BOOLEAN INJECTION LAYER ---
			// Intercepts structural ClassCastExceptions if human operators modified primitive types illegally
			this.logOn = getSafeBoolean( json, "logOn", true );
			this.trashbin = getSafeBoolean( json, "trashbin", true );
			this.subDir = getSafeBoolean( json, "subDir", false );
			this.autoDel = getSafeBoolean( json, "autoDel", false );
			this.autoBgDel = getSafeBoolean( json, "autoBgDel", false );
			this.autoSync = getSafeBoolean( json, "autoSync", false );
			this.bgSync = getSafeBoolean( json, "bgSync", false );

			// Trigger downstream initialization matrix cache tracking parsing
			ioSyncMap.loadSyncMap( syncMap );

		}catch( final Exception e ) {
			// Wrap any nested unhandled parsing or type violations into a predictable lifecycle runtime exception
			throw new ConfigException( "JSON payload structure is corrupt or mismatched.", e );
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
	private boolean getSafeBoolean( JsonObject json, String key, boolean defaultValue ) {
		if( !json.containsKey( key ) ) { return defaultValue; }
		try {
			return json.getBoolean( key );
		}catch( final ClassCastException e ) {
			Debug.printDebug( "[Preference] Warning: Invalid data type mapping tracking key '" + key + "'. Falling back to default: " + defaultValue );
			return defaultValue;
		}
	}

	/**
	 * Persists the current timestamp as the last successful scan execution time.
	 */
	public void saveLastScanTime() {
		try( BufferedWriter bw = Files.newBufferedWriter( jobScanTimePath,
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING ) ) {
			bw.write( String.valueOf( System.currentTimeMillis() ) );
		}catch( final IOException e ) {
			Debug.printException( this.getClass(), e );
		}
	}

	/**
	 * Retrieves the last recorded scan execution timestamp.
	 *
	 * @return Unix epoch millis, or 0 if no timer token exists yet.
	 */
	public long getLastScanTime() {
		if( !Files.exists( jobScanTimePath ) ) { return 0; }
		try( BufferedReader br = Files.newBufferedReader( jobScanTimePath ) ) {
			final String str = br.readLine();
			return ( str != null ) ? Long.parseLong( str.trim() ) : 0;
		}catch( final IOException | NumberFormatException e ) {
			return 0;
		}
	}

	public void writeSyncMap() {
		ioSyncMap.writeSyncMap( syncMap );
	}

	// --- Setters and Getters ---
	public String getJobName() { return jobName; }

	void setJobNameFromManager( String newName ) { this.jobName = newName; }

	public ArrayList<Path> getSourcePath() { return sourcePaths; }

	public void setSourcePath( ArrayList<Path> paths ) { this.sourcePaths = paths; }

	public ArrayList<Path> getDestPath() { return destPaths; }

	public void setDestPath( ArrayList<Path> paths ) { this.destPaths = paths; }

	public Path getStartSourcePath() { return startSourcePath; }

	public void setStartSourcePath( Path p ) { this.startSourcePath = p; }

	public Path getStartDestPath() { return startDestPath; }

	public void setStartDestPath( Path p ) {
		this.startDestPath = p;
		this.trashbinPath = p.resolve( trashbinString );
	}

	public Map<Path, FileAttributes> getSyncMap() { return syncMap; }

	public ScanType getScanMode() { return scanMode; }

	public void setScanMode( ScanType mode ) { this.scanMode = mode; }

	public BgTime getBgTime() { return bgTime; }

	public void setBgTime( BgTime time ) { this.bgTime = time; }

	public boolean isLogOn() { return logOn; }

	public void setLogOn( boolean logOn ) { this.logOn = logOn; }

	public boolean isSubDir() { return subDir; }

	public void setSubDir( boolean subDir ) { this.subDir = subDir; }

	public boolean isAutoDel() { return autoDel; }

	public void setAutoDel( boolean autoDel ) { this.autoDel = autoDel; }

	public boolean isAutoBgDel() { return autoBgDel; }

	public void setAutoBgDel( boolean autoBgDel ) { this.autoBgDel = autoBgDel; }

	public boolean isAutoSync() { return autoSync; }

	public void setAutoSync( boolean autoSync ) { this.autoSync = autoSync; }

	public boolean isBgSync() { return bgSync; }

	public void setBgSync( boolean bgSync ) { this.bgSync = bgSync; }

	public boolean isTrashbin() { return trashbin; }

	public void setTrashbin( boolean trashbin ) { this.trashbin = trashbin; }

	public Path getTrashbinPath() { return trashbinPath; }
}