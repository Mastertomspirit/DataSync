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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;

import de.spiritscorp.DataSync.Controller.Controller;
import de.spiritscorp.DataSync.IO.Debug;

public class Main {
	
	public final static String VERSION = "V 0.9.6.0";
	public static boolean debug = false;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		debug = Arrays.stream(args).anyMatch((s) -> s.equals("debug"));
		boolean firstStart = Arrays.stream(args).anyMatch((s) -> s.equals("firstStart"));
		
		if(Arrays.stream(args).anyMatch((s) -> s.equals("debugToFile")))	{
			debug = true;
			Debug.SET_DEBUG_TO_FILE();
			Debug.PRINT_DEBUG_TIMELESS("%nDEBUG BEGIN: %s%n", LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)));
		}

		new Controller(firstStart);
	}
}
