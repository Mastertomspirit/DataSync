/*
 		DataSync Application

 	 	@author Tom Spirit
 	 	@date 16.12.2021
 		@version	0.9.6.0

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
package de.spiritscorp.DataSync;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;

import de.spiritscorp.DataSync.Controller.MainViewController;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Preference;

/**
 * Main application entry point responsible for runtime arguments parsing,
 * debug subsystems orchestration, and boots-strapping the JavaFX platform lifecycle.
 * * @author Tom Spirit
 */
public class Main {

	public final static String VERSION = "V 0.9.6.3";
	public static boolean debug = false;
	private static boolean firstStart = false;

	/**
	 * Launch the application framework execution matrix.
	 *
	 * @param args Command line runtime environment argument flags.
	 */
	public static void main(String[] args) {
		debug = Arrays.stream(args).anyMatch((s) -> s.equals("debug"));
		final boolean firstStart = Arrays.stream(args).anyMatch((s) -> s.equals("firstStart"));
		final String configFolder = Arrays
				.stream(args)
				.filter((s) -> s.toLowerCase().startsWith("--configfile="))
				.findFirst()
				.map((s) -> s.substring(s.indexOf('=') + 1, s.length()))
				.orElse("");
		if (!configFolder.isBlank()) {
			Preference.getInstance(Path.of(configFolder));
		}

		if (Arrays.stream(args).anyMatch((s) -> s.equals("debugToFile"))) {
			debug = true;
			Debug.setDebugToFile();
			Debug.printDebugTimeless("%nDEBUG BEGIN -> [%s]: %s%n",
					System.getProperty("app.instance.name", "Standard Instance"),
					LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)));
			Debug.printDebug("Set config root path -> %s", Preference.getRootPath().toString());
		}

		new MainViewController(args);
	}

	/**
	 * @return True if the application was executed with the clear initialization flag marker context.
	 */
	public static boolean isFirstStart() { return firstStart; }
}
