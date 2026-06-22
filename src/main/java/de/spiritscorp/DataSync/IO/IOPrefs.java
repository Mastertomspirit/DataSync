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

class IOPrefs {
	/*
		private final Path configDir;
		private JsonObject job;
		private final HashMap<String, JsonValue> prefTempMap;
	
		IOPrefs() {
			this.configDir = Preference.getConfigPath().getParent();
			prefTempMap = new HashMap<>();
		}
	*/
	/**
	 * @return <b>boolean</b> true if success
	 * 
	 *         boolean readPreferences() {
	 *         if (Files.exists(Preference.getConfigPath())) {
	 *         try (FileReader reader = new FileReader(Preference.getConfigPath().toFile(), Charset.forName("UTF-8"))) {
	 *         final JsonReader jr = Json.createReader(reader);
	 *         job = jr.readObject();
	 *         jr.close();
	 *         if (job != null) return true;
	 *         } catch (final IOException e) {
	 *         Debug.printException(this.getClass(), e);
	 *         }
	 *         }
	 *         return false;
	 *         }
	 */
	/**
	 * Write the Preferences at <i>"user-home"/DataSync/conf.json</i>
	 * 
	 * boolean writePreferences() {
	 * if (!Files.exists(configDir)) {
	 * configDir.toFile().mkdir();
	 * }
	 * try (OutputStream os = Files.newOutputStream(Preference.getConfigPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
	 * final HashMap<String, Boolean> config = new HashMap<>();
	 * config.put(JsonGenerator.PRETTY_PRINTING, true);
	 * final JsonWriterFactory jwf = Json.createWriterFactory(config);
	 * job = Json.createObjectBuilder(prefTempMap).build();
	 * final JsonWriter jw = jwf.createWriter(os);
	 * jw.write(job);
	 * jw.close();
	 * prefTempMap.clear();
	 * return true;
	 * } catch (final IOException e) {
	 * Debug.printException(this.getClass(), e);
	 * }
	 * return false;
	 * }
	 */
	/**
	 * Set the preferences as key value pairs
	 *
	 * @param key
	 * @param value
	 * 
	 *              void setPreferences(String key, JsonValue value) {
	 *              prefTempMap.put(key, value);
	 *              }
	 */
	/**
	 *
	 * @param key The key to be search
	 * @return <b>Path</> </br>
	 *         The requested path
	 * @throws ConfigException If path is not valid
	 * 
	 *                         Path getPath(String key) throws ConfigException {
	 *                         if (job.containsKey(key)) {
	 *                         final Path convPath = Paths.get(job.getString(key));
	 *                         if (Files.exists(convPath) || key.startsWith("destPath") || key.startsWith("startDestPath") || key.equals("trashbinPath")) return convPath;
	 *                         }
	 *                         throw new ConfigException("No valid Path found: " + key);
	 *                         }
	 */

	/**
	 *
	 * @param key The key to be search
	 * @return <b>int</b> </br>
	 *         The number of paths are set
	 * @throws ConfigException
	 * 
	 *                         int getMultiPath(String key) throws ConfigException {
	 *                         if (job.containsKey(key)) { return job.getInt(key); }
	 *                         throw new ConfigException("No valid Multipath number found");
	 *                         }
	 */
	/**
	 *
	 * @param key The key to be search
	 * @return <b>boolean</b>
	 * @throws ConfigException
	 * 
	 *                         boolean getBoolean(String key) throws ConfigException {
	 *                         if (job.containsKey(key)) { return job.getBoolean(key); }
	 *                         throw new ConfigException("No valid boolean found: " + key);
	 *                         }
	 */
	/**
	 *
	 * @return <b>ScanType</b>
	 * @throws ConfigException
	 * 
	 *                         ScanType getScanType() throws ConfigException {
	 *                         try {
	 *                         return ScanType.get(job.getString("deepScan"));
	 *                         } catch (final Exception e) {}
	 *                         throw new ConfigException("No valid ScanType found");
	 *                         }
	 */
	/**
	 *
	 * @return <b>BgTime</b>
	 * @throws ConfigException
	 * 
	 *                         BgTime getBgTime() throws ConfigException {
	 *                         try {
	 *                         return BgTime.get(job.getString("bgTime"));
	 *                         } catch (final Exception e) {}
	 *                         throw new ConfigException("No valid BgTime found");
	 *                         }
	 */
}
