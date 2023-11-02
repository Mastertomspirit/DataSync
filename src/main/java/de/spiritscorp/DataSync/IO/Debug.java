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
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import de.spiritscorp.DataSync.Main;

public class Debug {

	/**
	 * A debug method to write a formatted string to this output stream using a timestamp, the specified format string and arguments. 
	 * 
	 * @param format	A format string as described in Format string syntax
	 * @param args		Arguments referenced by the format specifiers in the format string. If there are more arguments than format specifiers, 
	 * 					the extra arguments are ignored. The number of arguments is variable and may be zero. The maximum number of arguments is limited 
	 * 					by the maximum dimension of a Java array as defined by The Java Virtual Machine Specification.
	 */
	public static final void PRINT_DEBUG(String format,Object... args) {
		if(Main.debug) {
			String time = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSSSS"));
			System.out.printf(time + "  " + format + "%n", args);
		}
	}
	
	/**
	 * A debug method to write a formatted string to this output stream using the specified format string and arguments. 
	 * 
	 * @param format	A format string as described in Format string syntax
	 * @param args		Arguments referenced by the format specifiers in the format string. If there are more arguments than format specifiers, 
	 * 					the extra arguments are ignored. The number of arguments is variable and may be zero. The maximum number of arguments is limited 
	 * 					by the maximum dimension of a Java array as defined by The Java Virtual Machine Specification.
	 */
	public static final void PRINT_DEBUG_TIMELESS(String format,Object... args) {
		if(Main.debug) {
			System.out.printf(format + "%n", args);
		}
	}
	
	public static final void SET_DEBUG_TO_FILE() {
		try {
			if(!Files.exists(Preference.DEBUG_PATH)) Files.createDirectories(Preference.DEBUG_PATH.getParent());
			System.setOut(new PrintStream(Files.newOutputStream(Preference.DEBUG_PATH, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND), true, Charset.forName("UTF-8")));
			System.setErr(new PrintStream(Files.newOutputStream(Preference.ERROR_PATH, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND), true, Charset.forName("UTF-8")));
		} catch (IOException e) {e.printStackTrace();}
	}
}
