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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.spiritscorp.DataSync.Controller.SyncJobContext;
import de.spiritscorp.DataSync.Theme.AppTheme;
import de.spiritscorp.DataSync.Theme.DarkSlateTheme;
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
public class PreferenceManager {

	private static Path rootPath = Paths.get(System.getProperty("user.home"), "DataSync");
	private static Path configPath = rootPath.resolve("conf.json");
	private static Path logPath = rootPath.resolve("log.json");
	private static Path debugPath = rootPath.resolve("debug.log");
	private static Path errorPath = rootPath.resolve("debug.err");

	private final Map<String, Preference> loadedProfiles = new HashMap<>();
	private static final PreferenceManager INSTANCE = new PreferenceManager();

	private boolean globalAutoStart = false;
	private AppTheme theme = new DarkSlateTheme();

	/**
	 * Enforces non-instantiability outside the Singleton lifecycle context.
	 */
	private PreferenceManager() {
	}

	/**
	 * Retrieves the centralized operational instance boundary coordinator.
	 *
	 * @return The singleton manager instance.
	 */
	public static PreferenceManager getInstance() { return INSTANCE; }

	/**
	 * Reconfigures and overrides the global operational ecosystem workspace root coordinates.
	 * Re-initializes all system-dependent structural path mappings dynamically.
	 *
	 * @param customRoot The new target base directory path, or null to retain the home default context.
	 */
	public static synchronized void initGlobalRoot(Path customRoot) {
		if (customRoot != null && Files.exists(customRoot, LinkOption.NOFOLLOW_LINKS)) {
			rootPath = customRoot;
			configPath = rootPath.resolve("conf.json");
			logPath = rootPath.resolve("log.json");
			debugPath = rootPath.resolve("debug.log");
			errorPath = rootPath.resolve("debug.err");
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
	public synchronized Preference createProfile(String jobName) {
		final Preference pref = Preference.createSinglePreference(jobName);
		loadedProfiles.put(pref.getJobName(), pref);
		return pref;
	}

	/**
	 * Retrieves an allocated configuration tracking state context signature via its workspace identifier.
	 *
	 * @param jobName The unique profile registry lookup key.
	 * @return The matching configuration instance state, or null if no mapping tracks the parameter.
	 */
	public synchronized Preference getProfile(String jobName) {
		return loadedProfiles.get(jobName);
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
	public synchronized boolean renameProfile(String oldName, String newName, Preference pref) {
		if (oldName == null || newName == null || pref == null || oldName.equals(newName)) { return false; }
		loadedProfiles.remove(oldName);
		pref.setJobNameFromManager(newName);
		loadedProfiles.put(newName, pref);
		return saveAllPreferences();
	}

	/**
	 * Atomically removes an active synchronization job context tracking assignment.
	 * Evicts cached properties from memory and updates the primary configuration storage file.
	 *
	 * @param job The high-level UI task context container targeted for decommissioning.
	 * @return true if structural extraction and serialization completed successfully.
	 */
	public synchronized boolean removeProfile(SyncJobContext job) {
		if (job == null) return false;
		loadedProfiles.remove(job.getJobName(), job.getPreference());
		return saveAllPreferences();
	}

	/**
	 * Exposes the active, in-memory configuration profile map registry.
	 * Wrapped in an unmodifiable view to preserve structural mutation thread safety bounds.
	 *
	 * @return An unmodifiable structural read-only view tracking live preference profiles.
	 */
	public synchronized Map<String, Preference> getLoadedProfiles() { return Collections.unmodifiableMap(loadedProfiles); }

	/**
	 * Compiles all active in-memory profile matrices and flushes them into a single unified JSON structure.
	 * Truncates any existing configuration state assets dynamically during filesystem stream allocation.
	 *
	 * @return true if structural file flushing and underlying persistence executed without errors.
	 */
	public synchronized boolean saveAllPreferences() {
		try {
			if (!Files.exists(rootPath)) {
				Files.createDirectories(rootPath);
			}

			final JsonObjectBuilder rootBuilder = Json.createObjectBuilder();

			// Embed global properties
			final JsonObject globalDoc = Json.createObjectBuilder()
					.add("autoStart", globalAutoStart)
					.add("theme", theme.getClass().getName())
					.build();
			rootBuilder.add("globalSettings", globalDoc);

			// Append dynamic profile segments
			for (final Map.Entry<String, Preference> entry : loadedProfiles.entrySet()) {
				rootBuilder.add(entry.getKey(), entry.getValue().serialize());
			}

			final Map<String, Object> writerConfig = new HashMap<>();
			writerConfig.put(JsonGenerator.PRETTY_PRINTING, true);
			final JsonWriterFactory factory = Json.createWriterFactory(writerConfig);

			try (OutputStream os = Files.newOutputStream(configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				final JsonWriter writer = factory.createWriter(os);
				writer.write(rootBuilder.build());
				writer.close();
				return true;
			}
		} catch (final IOException e) {
			Debug.printDebug("[Error] Critical: Failed to serialize active memory states to 'conf.json'. Reason: %s", e.getMessage());
			Debug.printException(this.getClass(), e);
			return false;
		}
	}

	/**
	 * Hydrates the global storage matrix layer and multi-job profile cache records from the persistent disk token.
	 * Validates individual structural segments during structural ingestion parsing.
	 *
	 * @return true if filesystem parsing completed entirely; false if tracking token was absent or corrupt.
	 */
	public synchronized boolean loadAllPreferences() {
		if (!Files.exists(configPath)) { return false; }

		try (InputStream is = Files.newInputStream(configPath)) {
			final JsonReader reader = Json.createReader(is);
			final JsonObject rootObj = reader.readObject();
			reader.close();
			if (rootObj.isEmpty()) return false;

			// Extract global runtime parameters
			if (rootObj.containsKey("globalSettings")) {
				final JsonObject globalDoc = rootObj.getJsonObject("globalSettings");
				this.globalAutoStart = globalDoc.getBoolean("autoStart", false);
				if (globalDoc.containsKey("theme")) {
					try {
						final String className = globalDoc.getString("theme");
						final Class<?> themeClass = Class.forName(className);

						this.theme = (AppTheme) themeClass.getDeclaredConstructor().newInstance();

					} catch (final Exception e) {
						Debug.printDebug("[Error] Failed to instantiate theme class. Falling back to default.");
						Debug.printException(getClass(), e);
						this.theme = new DarkSlateTheme();
					}
				} else {
					Debug.printDebug("[Warn] No value for instantiate theme class. Falling back to default.");
				}
			}
			loadedProfiles.clear();

			// Extract distinct automation tasks profiles
			for (final String jobName : rootObj.keySet()) {
				if (jobName.equals("globalSettings")) continue;
				final JsonObject jobData = rootObj.getJsonObject(jobName);
				final Preference pref = Preference.createSinglePreference(jobName);
				try {
					pref.deserialize(jobData);
					loadedProfiles.put(jobName, pref);
				} catch (final ConfigException e) {
					Debug.printDebug("[Error] Critical: Failed to load job profile '%s'. Skipping entry. Reason: %s", jobName, e.getMessage());
					Debug.printException(this.getClass(), e);
				}
			}
			return true;
		} catch (final ClassCastException | IOException e) {
			Debug.printDebug("[Error] Critical: Failed to load profiles. Reason: %s", e.getMessage());
			Debug.printException(this.getClass(), e);
			return false;
		}
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
	public void setGlobalAutoStart(boolean globalAutoStart) { this.globalAutoStart = globalAutoStart; }

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

	public void setTheme(AppTheme theme) { this.theme = theme; }

	public AppTheme getTheme() { return theme; }
}