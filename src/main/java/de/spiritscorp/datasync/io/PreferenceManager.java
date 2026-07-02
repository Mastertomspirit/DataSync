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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.theme.AppTheme;
import de.spiritscorp.datasync.theme.DarkSlateTheme;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

/**
 * Thread-safe global configurations orchestrator managing persistent JSON configurations.
 * Coordinates multi-profile I/O read/write operations for independent replication synchronization tasks
 * and global application states. Implements the Singleton pattern to ensure centralized state control.
 *
 */
public final class PreferenceManager {

	/**
	 * Timeout duration in seconds for acquiring the profile lock.
	 */
	private static final long LOCK_TIME = 1;
	/**
	 * Root directory for application data storage.
	 */
	/*package*/ static final Path DATASYNC_HOME = Paths.get( System.getProperty( "user.home" ), "DataSync" );
	/**
	 * Root directory for application data storage.
	 */
	private Path rootPath = DATASYNC_HOME;
	/**
	 * Path to the JSON configuration file containing profiles and global settings.
	 */
	private Path configPath = rootPath.resolve( "conf.json" );
	/**
	 * Path to the standard JSON log file.
	 */
	private Path logPath = rootPath.resolve( "log.json" );
	/**
	 * Path to the standard debug log text file.
	 */
	private Path debugPath = rootPath.resolve( "debug.log" );
	/**
	 * Path to the error log text file.
	 */
	private Path errorPath = rootPath.resolve( "debug.err" );

	/**
	 * Thread-safe map storing the loaded automation profiles indexed by their job name.
	 */
	private final Map<String, Preference> loadedProfiles = new ConcurrentHashMap<>();

	/**
	 * The global Singleton instance of the PreferenceManager.
	 */
	private static final PreferenceManager INSTANCE = new PreferenceManager();

	/**
	 * Lock to ensure thread-safe operations on profile configurations.
	 */
	private final ReentrantLock profileLock = new ReentrantLock();

	/**
	 * Global flag indicating if application launch on systemboot.
	 */
	private boolean globalAutoStart;

	/**
	 * The currently active visual theme of the application.
	 */
	private AppTheme theme;

	/**
	 * Enforces non-instantiability outside the Singleton lifecycle context.
	 */
	private PreferenceManager() {
	}

	/**
	 * Gets the global Singleton instance.
	 *
	 * @return The singleton manager instance.
	 */
	public static PreferenceManager getInstance() { return INSTANCE; }

	/**
	 * Reconfigures and overrides the global operational ecosystem workspace root coordinates.
	 * Re-initializes all system-dependent structural path mappings dynamically.
	 * <p>
	 * Fallback at <b>(user.home)/DataSync</b> if the target path is not writeable or not valid
	 *
	 * @param customRoot The new target base directory path, or null to retain the home default context.
	 */
	public void initGlobalRootConfigPath( final Path customRoot ) {
		try {
			if( profileLock.tryLock( LOCK_TIME, TimeUnit.SECONDS ) ) {
				try {
					if( customRoot != null && Files.exists( customRoot, LinkOption.NOFOLLOW_LINKS ) ) {
						rootPath = customRoot.toAbsolutePath().normalize();
						configPath = customRoot.resolve( "conf.json" );
						logPath = customRoot.resolve( "log.json" );
						debugPath = customRoot.resolve( "debug.log" );
						errorPath = customRoot.resolve( "debug.err" );
					}
				}finally {
					profileLock.unlock();
				}
			}else {
				Debug.printError( "[Error] initGlobalRootConfigPath() -> Profiles are allready locked" );
			}
		}catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Allocates and provisions a new, distinct profile configuration data scope unit.
	 * Automatically appends the freshly constructed tracking unit block into active memory structures.
	 *
	 * @param jobName Unique target workspace identifier.
	 *
	 * @return An unpopulated, isolated configuration state segment tracker.
	 */
	public Preference createProfile( final String jobName ) {
		try {
			if( profileLock.tryLock( LOCK_TIME, TimeUnit.SECONDS ) ) {
				try {
					final Preference pref = Preference.createSinglePreference( jobName );
					loadedProfiles.put( pref.getJobName(), pref );
					if( saveAllPreferences() ) return pref;
				}finally {
					profileLock.unlock();
				}
			}else {
				Debug.printError( "[Error] createProfiles() -> Profiles are allready locked" );
			}
		}catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	/**
	 * Retrieves an allocated configuration tracking state context signature via its workspace identifier.
	 *
	 * @param jobName The unique profile registry lookup key.
	 * @return The matching configuration instance state, or null if no mapping tracks the parameter.
	 */
	public Preference getProfile( final String jobName ) {
		return loadedProfiles.get( jobName );
	}

	/**
	 * Atomically handles profile rename routines inside the synchronization runtime context.
	 * Mutates the tracking key structural state identifier map and flushes changes to disk immediately.
	 *
	 * @param oldName Original task profile identifier key.
	 * @param newName Target replacement unique identifier string.
	 * @param pref    Associated configuration parameters data segment instance.
	 * @return true if persistence succeeded; false if parameters were invalid or execution failed.
	 */
	public boolean renameProfile( final String oldName, final String newName, final Preference pref ) {
		boolean success = false;
		try {
			// Try to acquire the lock within a 1-second timeout to prevent deadlocks
			if( profileLock.tryLock( LOCK_TIME, TimeUnit.SECONDS ) ) {
				try {
					// Execute only when all inputs are valid
					if( oldName != null && newName != null && pref != null && !oldName.equals( newName ) ) {
						loadedProfiles.remove( oldName );
						pref.setJobNameFromManager( newName );
						loadedProfiles.put( newName, pref );
						success = saveAllPreferences();
					}
				}finally {
					// Always ensure the lock is released if it was successfully acquired
					profileLock.unlock();
				}
			}else {
				Debug.printError( "[Error] renameProfile() -> Profiles are allready locked" );
			}
		}catch( final InterruptedException e ) {
			// Restore interrupted status if the thread was interrupted while waiting for the lock
			Thread.currentThread().interrupt();
		}
		return success;
	}

	/**
	 * Atomically removes an active synchronization job context tracking assignment.
	 * Evicts cached properties from memory and updates the primary configuration storage file.
	 *
	 * @param job The high-level UI task context container targeted for decommissioning.
	 * @return true if structural extraction and serialization completed successfully.
	 */
	public boolean removeProfile( final SyncJobContext job ) {
		boolean success = false;
		try {
			if( profileLock.tryLock( LOCK_TIME, TimeUnit.SECONDS ) ) {
				try {
					if( job != null ) {
						loadedProfiles.remove( job.getJobName(), job.getPreference() );
						success = saveAllPreferences();
					}
				}finally {
					profileLock.unlock();
				}
			}else {
				Debug.printError( "[Error] removeProfile() -> Profiles are allready locked" );
			}
		}catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
		return success;
	}

	/**
	 * Exposes the active, in-memory configuration profile map registry.
	 * Wrapped in an unmodifiable view to preserve structural mutation thread safety bounds.
	 *
	 * @return An unmodifiable structural read-only view tracking live preference profiles.
	 */
	public Map<String, Preference> getLoadedProfiles() { return Collections.unmodifiableMap( loadedProfiles ); }

	/**
	 * Compiles all active in-memory profile matrices and flushes them into a single unified JSON structure.
	 * Truncates any existing configuration state assets dynamically during filesystem stream allocation.
	 *
	 * @return true if structural file flushing and underlying persistence executed without errors.
	 */
	public synchronized boolean saveAllPreferences() {
		try {
			if( profileLock.tryLock( LOCK_TIME, TimeUnit.SECONDS ) ) {
				try {
					if( !Files.exists( rootPath ) ) {
						Files.createDirectories( rootPath );
					}

					final JsonObjectBuilder rootBuilder = Json.createObjectBuilder();

					// Embed global properties
					final JsonObject globalDoc = Json.createObjectBuilder()
							.add( "autoStart", globalAutoStart )
							.add( "theme", theme.getClass().getName() )
							.build();
					rootBuilder.add( "globalSettings", globalDoc );

					// Append dynamic profile segments
					for( final Map.Entry<String, Preference> entry : loadedProfiles.entrySet() ) {
						rootBuilder.add( entry.getKey(), entry.getValue().serialize() );
					}

					final Map<String, Object> writerConfig = new HashMap<>();
					writerConfig.put( JsonGenerator.PRETTY_PRINTING, true );
					final JsonWriterFactory factory = Json.createWriterFactory( writerConfig );

					try( JsonWriter writer = factory.createWriter( Files.newOutputStream( configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING ) ) ) {
						writer.write( rootBuilder.build() );
						return true;
					}
				}catch( final IOException e ) {
					Debug.printDebug( "[Error] Critical: Failed to serialize active memory states to 'conf.json'. Reason: %s", e.getMessage() );
					Debug.printException( this.getClass(), e );
					return false;
				}finally {
					profileLock.unlock();
				}
			}else {
				Debug.printError( "[Error] removeProfile() -> Profiles are allready locked" );
			}
		}catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
		return false;
	}

	/**
	 * Hydrates the global storage matrix layer and multi-job profile cache records from the persistent disk token.
	 * Validates individual structural segments during structural ingestion parsing.
	 *
	 * @return true if filesystem parsing completed entirely; false if tracking token was absent or corrupt.
	 */
	public boolean loadAllPreferences() {
		try {
			if( profileLock.tryLock( LOCK_TIME, TimeUnit.SECONDS ) ) {
				try {

					try( JsonReader reader = Json.createReader( Files.newInputStream( configPath ) ) ) {
						final JsonObject rootObj = reader.readObject();
						if( rootObj.isEmpty() ) return false;

						// Extract global runtime parameters
						if( !extractGlobal( rootObj ) ) {
							Debug.printDebug( "[Warn] load globals incompleted" );
						}
						loadedProfiles.clear();

						// Extract distinct automation tasks profiles
						if( !extractProfiles( rootObj ) ) {
							Debug.printDebug( "[Warn] load profiles incompleted" );
						}
						return true;
					}catch( final ClassCastException | IOException e ) {
						Debug.printDebug( "[Error] Critical: Failed to load profiles. Reason: %s", e.getMessage() );
						Debug.printException( this.getClass(), e );
						return false;
					}
				}finally {
					profileLock.unlock();
				}
			}else {
				Debug.printError( "[Error] loadAllPreferences() -> Profiles are allready locked" );
			}
		}catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
		return false;
	}

	private boolean extractGlobal( final JsonObject rootObj ) {
		if( rootObj.containsKey( "globalSettings" ) ) {
			final JsonObject globalDoc = rootObj.getJsonObject( "globalSettings" );
			if( globalDoc == null ) return false;
			this.globalAutoStart = globalDoc.getBoolean( "autoStart", false );
			if( globalDoc.containsKey( "theme" ) ) {
				final String className = globalDoc.getString( "theme" );
				try {
					final Class<?> themeClass = Class.forName( className );
					this.theme = (AppTheme) themeClass.getDeclaredConstructor().newInstance();
					return true;
				}catch( final ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e ) {
					Debug.printDebug( "[Error] Falling back to default. Failed to instantiate theme class: %s", e.getMessage() );
					Debug.printException( getClass(), e );
				}
			}else {
				Debug.printDebug( "[Warn] No value for instantiate theme class. Falling back to default." );
			}
		}
		this.theme = new DarkSlateTheme();
		return false;
	}

	private boolean extractProfiles( final JsonObject rootObj ) {
		for( final String jobName : rootObj.keySet() ) {
			if( jobName.equals( "globalSettings" ) ) continue;
			final JsonObject jobData = rootObj.getJsonObject( jobName );
			final Preference pref = Preference.createSinglePreference( jobName );
			try {
				pref.deserialize( jobData );
				loadedProfiles.put( jobName, pref );
			}catch( final ConfigException e ) {
				Debug.printDebug( "[Error] Critical: Failed to load job profile '%s'. Skipping entry. Reason: %s", jobName, e.getMessage() );
				Debug.printException( this.getClass(), e );
				return false;
			}
		}
		return !loadedProfiles.isEmpty();
	}

	// --- Global Configuration Accessors ---

	/**
	 * Evaluates whether the application ecosystem is provisioned to launch automatically
	 * upon host operating system startup sequences.
	 *
	 * @return true if the global background autostart configuration sequence is enabled.
	 */
	public boolean isGlobalAutoStart() { return globalAutoStart; }

	/**
	 * Modifies the global automation startup property state parameter.
	 * This execution updates the structural configuration variable memory cache layers.
	 *
	 * @param globalAutoStart Target state flag to determine automated deployment behavior.
	 */
	public void setGlobalAutoStart( final boolean globalAutoStart ) { this.globalAutoStart = globalAutoStart; }

	// --- Instanced System Properties Accessors (Formerly Static) ---

	/**
	 * Retrieves the persistent dynamic configuration storage path target locator.
	 *
	 * @return The absolute filesystem path directing to the primary 'conf.json' token.
	 */
	public Path getConfigPath() { return configPath; }

	/**
	 * Retrieves the persistent operational event synchronization logging path locator.
	 *
	 * @return The absolute filesystem path directing to the structural 'log.json' entity.
	 */
	public Path getLogPath() { return logPath; }

	/**
	 * Retrieves the system console output debug log tracking path locator.
	 *
	 * @return The absolute filesystem path directing to the standard runtime 'debug.log' file.
	 */
	public Path getDebugPath() { return debugPath; }

	/**
	 * Retrieves the localized tracking error diagnostic path locator.
	 *
	 * @return The absolute filesystem path directing to the critical runtime 'debug.err' stream dump.
	 */
	public Path getErrorPath() { return errorPath; }

	/**
	 * Sets the visual theme of the application.
	 *
	 */
	public void setTheme( final AppTheme theme ) { this.theme = theme; }

	/**
	 * Gets the currently configured application theme.
	 *
	 * @return the active AppTheme
	 */
	public AppTheme getTheme() { return theme; }

	public boolean isCustomConfigDir() { return !DATASYNC_HOME.equals( rootPath ); }
}