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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import de.spiritscorp.DataSync.Model.FileAttributes;
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
	IOSyncMap(String jobName) {
		this.jobSyncMapPath = PreferenceManager.getInstance().getConfigPath().getParent().resolve("syncMap_" + jobName + ".json");
	}

	boolean loadSyncMap(Map<Path, FileAttributes> syncMap) {
		if (!syncMap.isEmpty() || !Files.exists(jobSyncMapPath)) { return false; }
		try (FileReader reader = new FileReader(jobSyncMapPath.toFile(), StandardCharsets.UTF_8)) {
			final JsonReader jr = Json.createReader(reader);
			final JsonObject jo = jr.readObject();
			jr.close();

			for (final Map.Entry<String, JsonValue> entry : jo.entrySet()) {
				final JsonObject obj = entry.getValue().asJsonObject();
				final FileAttributes file = new FileAttributes(
						Paths.get(obj.getString("relativeFilePath")),
						obj.getString("createTimeString"),
						FileTime.fromMillis(Long.parseLong(obj.get("createTime").toString())),
						obj.getString("modTimeString"),
						FileTime.fromMillis(Long.parseLong(obj.get("modTime").toString())),
						Long.parseLong(obj.get("size").toString()),
						obj.getString("fileHash"));
				syncMap.put(Paths.get(obj.getString("relativeFilePath")), file);
			}
			return true;
		} catch (final Exception e) {
			Debug.printException(this.getClass(), e);
			return false;
		}
	}

	void writeSyncMap(Map<Path, FileAttributes> syncMap) {
		try (OutputStream os = Files.newOutputStream(jobSyncMapPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			final HashMap<String, Boolean> config = new HashMap<>();
			config.put(JsonGenerator.PRETTY_PRINTING, true);
			final JsonWriterFactory jwf = Json.createWriterFactory(config);

			final JsonObject jobObj = createSyncMap(syncMap);
			final JsonWriter jw = jwf.createWriter(os);
			jw.write(jobObj);
			jw.close();
		} catch (final IOException e) {
			Debug.printException(this.getClass(), e);
		}
	}

	private JsonObject createSyncMap(Map<Path, FileAttributes> syncMap) {
		final JsonObjectBuilder jo = Json.createObjectBuilder();
		for (final Map.Entry<Path, FileAttributes> entry : syncMap.entrySet()) {
			final JsonObject jo2 = Json.createObjectBuilder()
					.add("relativeFilePath", entry.getValue().getRelativeFilePath().toString())
					.add("fileHash", entry.getValue().getFileHash())
					.add("modTimeString", entry.getValue().getModTimeString())
					.add("createTime", entry.getValue().getCreateTime().toMillis())
					.add("modTime", entry.getValue().getModTime().toMillis())
					.add("size", entry.getValue().getSize())
					.add("createTimeString", entry.getValue().getCreateTimeString())
					.build();
			jo.add(entry.getValue().getRelativeFilePath().toString(), jo2);
		}
		return jo.build();
	}
}
